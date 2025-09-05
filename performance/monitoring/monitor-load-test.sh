#!/bin/bash

# Real-time Load Test Monitoring Script for Wallet Service
# Usage: ./monitor-load-test.sh [duration_in_seconds]
# This script monitors CPU, memory, and application metrics during load testing

set -e

# Configuration
DURATION=${1:-600}  # Default 10 minutes
INTERVAL=5          # Update every 5 seconds
APP_PID=$(pgrep -f "wallet-service-dev.jar" || echo "")
PROMETHEUS_URL="http://localhost:9090"
OUTPUT_DIR="results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Create output directory
mkdir -p "$OUTPUT_DIR"
LOG_FILE="${OUTPUT_DIR}/monitoring_${TIMESTAMP}.log"

echo -e "${GREEN}🔍 Starting Real-time Load Test Monitoring${NC}"
echo -e "${BLUE}📊 Duration: ${DURATION} seconds (${INTERVAL}s intervals)${NC}"
echo -e "${BLUE}📝 Log file: ${LOG_FILE}${NC}"
echo -e "${BLUE}🎯 Application PID: ${APP_PID:-'Not found'}${NC}"
echo ""

# Initialize log file
cat > "$LOG_FILE" << EOF
Load Test Monitoring Session
============================
Start Time: $(date)
Duration: ${DURATION} seconds
Interval: ${INTERVAL} seconds
Application PID: ${APP_PID:-'Not found'}
Prometheus URL: ${PROMETHEUS_URL}
============================

EOF

# Function to get application CPU and Memory usage
get_app_metrics() {
    if [ -n "$APP_PID" ]; then
        # Get process stats using ps
        local ps_output=$(ps -p "$APP_PID" -o pid,pcpu,pmem,rss,vsz 2>/dev/null || echo "")
        if [ -n "$ps_output" ]; then
            echo "$ps_output" | tail -n 1
        else
            echo "Process not found"
        fi
    else
        echo "PID not available"
    fi
}

# Function to get system metrics
get_system_metrics() {
    # CPU usage
    local cpu_usage=$(top -l 1 -n 0 | grep "CPU usage" | awk '{print $3}' | sed 's/%//')
    
    # Memory usage
    local memory_info=$(vm_stat | awk '
        /Pages free/ { free = $3 }
        /Pages active/ { active = $3 }
        /Pages inactive/ { inactive = $3 }
        /Pages speculative/ { speculative = $3 }
        /Pages wired down/ { wired = $4 }
        END {
            gsub(/[^0-9]/, "", free)
            gsub(/[^0-9]/, "", active) 
            gsub(/[^0-9]/, "", inactive)
            gsub(/[^0-9]/, "", speculative)
            gsub(/[^0-9]/, "", wired)
            
            total_pages = free + active + inactive + speculative + wired
            used_pages = active + inactive + speculative + wired
            
            total_mb = total_pages * 4096 / 1024 / 1024
            used_mb = used_pages * 4096 / 1024 / 1024
            free_mb = free * 4096 / 1024 / 1024
            
            printf "%.0f %.0f %.1f", total_mb, used_mb, (used_mb/total_mb)*100
        }')
    
    echo "$cpu_usage $memory_info"
}

# Function to query Prometheus metrics
query_prometheus() {
    local query="$1"
    local result=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=${query}" 2>/dev/null | \
                  jq -r '.data.result[0].value[1] // "N/A"' 2>/dev/null || echo "N/A")
    echo "$result"
}

# Function to get application metrics from Prometheus
get_prometheus_metrics() {
    local heap_used=$(query_prometheus "sum(jvm_memory_used_bytes{area=\"heap\"})")
    local heap_max=$(query_prometheus "sum(jvm_memory_max_bytes{area=\"heap\"})")
    local cpu_process=$(query_prometheus "system_cpu_usage")
    local threads=$(query_prometheus "jvm_threads_current")
    local gc_time=$(query_prometheus "rate(jvm_gc_collection_seconds_sum[1m])")
    
    # Convert bytes to MB
    if [[ "$heap_used" != "N/A" && "$heap_used" =~ ^[0-9]+$ ]]; then
        heap_used_mb=$(echo "scale=1; $heap_used / 1024 / 1024" | bc 2>/dev/null || echo "N/A")
    else
        heap_used_mb="N/A"
    fi
    
    if [[ "$heap_max" != "N/A" && "$heap_max" =~ ^[0-9]+$ ]]; then
        heap_max_mb=$(echo "scale=1; $heap_max / 1024 / 1024" | bc 2>/dev/null || echo "N/A")
    else
        heap_max_mb="N/A"
    fi
    
    echo "$heap_used_mb $heap_max_mb $cpu_process $threads $gc_time"
}

# Function to display metrics
display_metrics() {
    local timestamp="$1"
    local app_stats="$2"
    local system_stats="$3" 
    local prometheus_stats="$4"
    
    # Parse stats
    read -r app_pid app_cpu app_mem app_rss app_vsz <<< "$app_stats"
    read -r sys_cpu sys_mem_total sys_mem_used sys_mem_percent <<< "$system_stats"
    read -r heap_used heap_max process_cpu threads gc_time <<< "$prometheus_stats"
    
    # Clear screen and show header
    clear
    echo -e "${GREEN}🔍 Wallet Service Load Test Monitoring${NC}"
    echo -e "${BLUE}📅 $(date) | ⏱️  Running for: $(($(date +%s) - START_TIME))s / ${DURATION}s${NC}"
    echo ""
    
    # System Resources
    echo -e "${YELLOW}🖥️  SYSTEM RESOURCES${NC}"
    echo "┌─────────────────────────────────────────────────────────────────┐"
    printf "│ CPU Usage:     %6s%%                                        │\n" "${sys_cpu:-N/A}"
    printf "│ Memory Total:  %8s MB                                    │\n" "${sys_mem_total:-N/A}"
    printf "│ Memory Used:   %8s MB (%s%%)                            │\n" "${sys_mem_used:-N/A}" "${sys_mem_percent:-N/A}"
    echo "└─────────────────────────────────────────────────────────────────┘"
    echo ""
    
    # Application Resources (Process Level)
    echo -e "${YELLOW}☕ APPLICATION PROCESS (PID: ${APP_PID:-N/A})${NC}"
    echo "┌─────────────────────────────────────────────────────────────────┐"
    if [ "$app_pid" != "Process" ] && [ "$app_pid" != "PID" ]; then
        printf "│ CPU Usage:     %6s%%                                        │\n" "${app_cpu:-N/A}"
        printf "│ Memory Usage:  %6s%% (RSS: %s KB, VSZ: %s KB)          │\n" "${app_mem:-N/A}" "${app_rss:-N/A}" "${app_vsz:-N/A}"
    else
        printf "│ Status:        Application process not found                    │\n"
    fi
    echo "└─────────────────────────────────────────────────────────────────┘"
    echo ""
    
    # JVM Metrics (Prometheus)
    echo -e "${YELLOW}🚀 JVM METRICS (via Prometheus)${NC}"
    echo "┌─────────────────────────────────────────────────────────────────┐"
    printf "│ Heap Used:     %8s MB                                    │\n" "${heap_used:-N/A}"
    printf "│ Heap Max:      %8s MB                                    │\n" "${heap_max:-N/A}"
    printf "│ Process CPU:   %8s                                       │\n" "${process_cpu:-N/A}"
    printf "│ Threads:       %8s                                       │\n" "${threads:-N/A}"
    printf "│ GC Time/sec:   %8s                                       │\n" "${gc_time:-N/A}"
    echo "└─────────────────────────────────────────────────────────────────┘"
    echo ""
    
    # Quick stats
    echo -e "${BLUE}📊 Quick Actions:${NC}"
    echo "• View Grafana Dashboard: http://localhost:3000 (admin/admin)"
    echo "• View Prometheus: http://localhost:9090"
    echo "• Application Health: http://localhost:8080/q/health"
    echo "• Press Ctrl+C to stop monitoring"
    echo ""
}

# Function to log metrics
log_metrics() {
    local timestamp="$1"
    local app_stats="$2"
    local system_stats="$3"
    local prometheus_stats="$4"
    
    echo "[$timestamp]" >> "$LOG_FILE"
    echo "App Stats: $app_stats" >> "$LOG_FILE"
    echo "System Stats: $system_stats" >> "$LOG_FILE"
    echo "Prometheus Stats: $prometheus_stats" >> "$LOG_FILE"
    echo "---" >> "$LOG_FILE"
}

# Trap Ctrl+C
trap 'echo -e "\n${GREEN}✅ Monitoring stopped by user${NC}"; exit 0' INT

# Main monitoring loop
START_TIME=$(date +%s)
ITERATIONS=$((DURATION / INTERVAL))

echo -e "${GREEN}🚀 Starting monitoring loop...${NC}"
echo "Press Ctrl+C to stop monitoring early"
echo ""

for i in $(seq 1 $ITERATIONS); do
    timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    # Collect metrics
    app_stats=$(get_app_metrics)
    system_stats=$(get_system_metrics)
    prometheus_stats=$(get_prometheus_metrics)
    
    # Display and log
    display_metrics "$timestamp" "$app_stats" "$system_stats" "$prometheus_stats"
    log_metrics "$timestamp" "$app_stats" "$system_stats" "$prometheus_stats"
    
    # Sleep if not the last iteration
    if [ $i -lt $ITERATIONS ]; then
        sleep $INTERVAL
    fi
done

echo -e "\n${GREEN}✅ Monitoring completed!${NC}"
echo -e "${BLUE}📄 Detailed logs saved to: ${LOG_FILE}${NC}"
echo ""
echo -e "${YELLOW}📊 SUMMARY${NC}"
echo "Duration: ${DURATION} seconds"
echo "Samples: ${ITERATIONS}"
echo "Log file: ${LOG_FILE}"
echo ""
echo -e "${BLUE}🔍 To analyze the results:${NC}"
echo "cat ${LOG_FILE}"
echo ""
echo -e "${BLUE}📈 Grafana Dashboard: http://localhost:3000${NC}"
echo -e "${BLUE}📊 Prometheus: http://localhost:9090${NC}"
