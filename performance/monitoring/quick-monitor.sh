#!/bin/bash

# Quick CPU and Memory Monitor for Wallet Service
# Usage: ./quick-monitor.sh

APP_PID=$(pgrep -f "wallet-service-dev.jar" || echo "")
PROMETHEUS_URL="http://localhost:9090"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${GREEN}🔍 Quick Wallet Service Monitor${NC}"
echo -e "${BLUE}📅 $(date)${NC}"
echo ""

# System CPU and Memory
echo -e "${YELLOW}🖥️  SYSTEM RESOURCES${NC}"
echo "CPU Usage: $(top -l 1 -n 0 | grep "CPU usage" | awk '{print $3}')"

# Memory info
vm_stat | awk '
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
    free_gb = free * 4096 / 1024 / 1024 / 1024
    
    printf "Memory: %.1f GB used / %.1f GB total (%.1f%% used)\n", used_gb, total_gb, (used_gb/total_gb)*100
}'

echo ""

# Application Process
if [ -n "$APP_PID" ]; then
    echo -e "${YELLOW}☕ APPLICATION PROCESS (PID: $APP_PID)${NC}"
    ps -p "$APP_PID" -o pid,pcpu,pmem,rss,vsz | tail -n 1 | awk '{
        printf "CPU: %s%%, Memory: %s%%, RSS: %.1f MB, VSZ: %.1f MB\n", $2, $3, $4/1024, $5/1024
    }'
    echo ""
fi

# JVM Metrics from Prometheus
echo -e "${YELLOW}🚀 JVM METRICS${NC}"
heap_used=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=sum(jvm_memory_used_bytes{area=\"heap\"})" 2>/dev/null | jq -r '.data.result[0].value[1] // "N/A"' 2>/dev/null || echo "N/A")
heap_max=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=sum(jvm_memory_max_bytes{area=\"heap\"})" 2>/dev/null | jq -r '.data.result[0].value[1] // "N/A"' 2>/dev/null || echo "N/A")
threads=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=jvm_threads_current" 2>/dev/null | jq -r '.data.result[0].value[1] // "N/A"' 2>/dev/null || echo "N/A")

if [[ "$heap_used" != "N/A" && "$heap_used" =~ ^[0-9]+$ ]]; then
    heap_used_mb=$(echo "scale=1; $heap_used / 1024 / 1024" | bc 2>/dev/null || echo "N/A")
    heap_max_mb=$(echo "scale=1; $heap_max / 1024 / 1024" | bc 2>/dev/null || echo "N/A")
    heap_percent=$(echo "scale=1; $heap_used * 100 / $heap_max" | bc 2>/dev/null || echo "N/A")
    echo "Heap Memory: ${heap_used_mb} MB / ${heap_max_mb} MB (${heap_percent}%)"
else
    echo "Heap Memory: Unable to fetch from Prometheus"
fi

echo "JVM Threads: $threads"
echo ""

# Application Health
echo -e "${YELLOW}🏥 APPLICATION HEALTH${NC}"
health_status=$(curl -s http://localhost:8080/q/health 2>/dev/null | jq -r '.status // "UNKNOWN"' 2>/dev/null || echo "UNREACHABLE")
echo "Health Status: $health_status"

echo ""
echo -e "${BLUE}📊 Monitoring URLs:${NC}"
echo "• Grafana: http://localhost:3000"
echo "• Prometheus: http://localhost:9090"
echo "• Application: http://localhost:8080"
