#!/bin/bash

# Real-time CPU and Memory Monitor for Load Testing
# Usage: ./realtime-monitor.sh [interval_seconds]

INTERVAL=${1:-3}  # Default 3 seconds
APP_PID=$(pgrep -f "wallet-service-dev.jar" || echo "")

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${GREEN}🚀 Real-time Wallet Service Monitor${NC}"
echo -e "${BLUE}📊 Updating every ${INTERVAL} seconds - Press Ctrl+C to stop${NC}"
echo ""

# Function to get JVM heap metrics
get_jvm_metrics() {
    local heap_used=$(curl -s 'http://localhost:9090/api/v1/query?query=jvm_memory_used_bytes{area="heap"}' 2>/dev/null | \
                     jq -r '.data.result | map(.value[1] | tonumber) | add // 0' 2>/dev/null || echo "0")
    local heap_max=$(curl -s 'http://localhost:9090/api/v1/query?query=jvm_memory_max_bytes{area="heap"}' 2>/dev/null | \
                    jq -r '.data.result | map(.value[1] | tonumber) | add // 0' 2>/dev/null || echo "0")
    local threads=$(curl -s 'http://localhost:9090/api/v1/query?query=jvm_threads_current' 2>/dev/null | \
                   jq -r '.data.result[0].value[1] // "0"' 2>/dev/null || echo "0")
    
    echo "$heap_used $heap_max $threads"
}

# Function to get load test metrics
get_load_metrics() {
    # Get recent request rates (if available)
    local http_requests=$(curl -s 'http://localhost:9090/api/v1/query?query=rate(http_requests_total[1m])' 2>/dev/null | \
                         jq -r '.data.result[0].value[1] // "0"' 2>/dev/null || echo "0")
    
    # Check if load test is running (k6 process)
    local k6_running=$(pgrep k6 >/dev/null && echo "YES" || echo "NO")
    
    echo "$http_requests $k6_running"
}

# Trap Ctrl+C
trap 'echo -e "\n${GREEN}✅ Monitoring stopped${NC}"; exit 0' INT

# Main monitoring loop
while true; do
    timestamp=$(date '+%H:%M:%S')
    
    # Get system metrics
    cpu_usage=$(top -l 1 -n 0 | grep "CPU usage" | awk '{print $3}' | sed 's/%//')
    
    # Get memory info
    memory_info=$(vm_stat | awk '
        /Pages free/ { free = $3 }
        /Pages active/ { active = $3 }
        /Pages inactive/ { inactive = $3 }
        /Pages wired down/ { wired = $4 }
        END {
            gsub(/[^0-9]/, "", free)
            gsub(/[^0-9]/, "", active) 
            gsub(/[^0-9]/, "", inactive)
            gsub(/[^0-9]/, "", wired)
            
            total_pages = free + active + inactive + wired
            used_pages = active + inactive + wired
            
            total_gb = total_pages * 4096 / 1024 / 1024 / 1024
            used_gb = used_pages * 4096 / 1024 / 1024 / 1024
            
            printf "%.1f %.1f %.1f", total_gb, used_gb, (used_gb/total_gb)*100
        }')
    
    # Get application process metrics
    if [ -n "$APP_PID" ]; then
        app_stats=$(ps -p "$APP_PID" -o pcpu,pmem,rss 2>/dev/null | tail -n 1 || echo "0 0 0")
    else
        app_stats="0 0 0"
    fi
    
    # Get JVM metrics
    jvm_stats=$(get_jvm_metrics)
    read -r heap_used heap_max threads <<< "$jvm_stats"
    
    # Get load test metrics
    load_stats=$(get_load_metrics)
    read -r http_rate k6_status <<< "$load_stats"
    
    # Parse values
    read -r sys_mem_total sys_mem_used sys_mem_percent <<< "$memory_info"
    read -r app_cpu app_mem app_rss <<< "$app_stats"
    
    # Convert heap to MB
    if [[ "$heap_used" =~ ^[0-9]+$ ]] && [ "$heap_used" -gt 0 ]; then
        heap_used_mb=$(echo "scale=0; $heap_used / 1024 / 1024" | bc 2>/dev/null || echo "0")
        heap_max_mb=$(echo "scale=0; $heap_max / 1024 / 1024" | bc 2>/dev/null || echo "0")
        if [ "$heap_max" -gt 0 ]; then
            heap_percent=$(echo "scale=1; $heap_used * 100 / $heap_max" | bc 2>/dev/null || echo "0")
        else
            heap_percent="0"
        fi
    else
        heap_used_mb="N/A"
        heap_max_mb="N/A" 
        heap_percent="N/A"
    fi
    
    # Convert RSS to MB
    if [[ "$app_rss" =~ ^[0-9]+$ ]]; then
        app_rss_mb=$(echo "scale=0; $app_rss / 1024" | bc 2>/dev/null || echo "0")
    else
        app_rss_mb="0"
    fi
    
    # Clear screen and display
    clear
    echo -e "${GREEN}🔍 Wallet Service Real-time Monitor${NC} - ${timestamp}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    
    # Load Test Status
    if [ "$k6_status" = "YES" ]; then
        echo -e "${YELLOW}🔥 LOAD TEST STATUS: ${GREEN}RUNNING${NC}"
    else
        echo -e "${YELLOW}🔥 LOAD TEST STATUS: ${RED}NOT RUNNING${NC}"
    fi
    echo ""
    
    # System Resources
    echo -e "${CYAN}🖥️  SYSTEM RESOURCES${NC}"
    printf "   CPU Usage:      %6s%%\n" "${cpu_usage:-N/A}"
    printf "   Memory:         %5.1f GB / %5.1f GB (%s%%)\n" "${sys_mem_used:-0}" "${sys_mem_total:-0}" "${sys_mem_percent:-0}"
    echo ""
    
    # Application Process
    echo -e "${CYAN}☕ APPLICATION PROCESS${NC} ${BLUE}(PID: ${APP_PID:-N/A})${NC}"
    printf "   CPU Usage:      %6s%%\n" "${app_cpu:-N/A}"
    printf "   Memory Usage:   %6s%% (RSS: %s MB)\n" "${app_mem:-N/A}" "${app_rss_mb:-0}"
    echo ""
    
    # JVM Metrics
    echo -e "${CYAN}🚀 JVM HEAP MEMORY${NC}"
    if [ "$heap_used_mb" != "N/A" ]; then
        printf "   Heap Used:      %6s MB / %s MB (%s%%)\n" "${heap_used_mb}" "${heap_max_mb}" "${heap_percent}"
        printf "   Threads:        %6s\n" "${threads:-N/A}"
    else
        printf "   Status:         Metrics not available\n"
    fi
    echo ""
    
    # Performance Indicators
    echo -e "${CYAN}📊 PERFORMANCE INDICATORS${NC}"
    printf "   HTTP Req/sec:   %6s\n" "${http_rate:-N/A}"
    echo ""
    
    # Quick Actions
    echo -e "${BLUE}🔗 Quick Links:${NC}"
    echo "   • Grafana Dashboard: http://localhost:3000"
    echo "   • Prometheus: http://localhost:9090" 
    echo "   • Application Health: http://localhost:8080/q/health"
    echo ""
    echo -e "${YELLOW}Press Ctrl+C to stop monitoring${NC}"
    
    sleep "$INTERVAL"
done
