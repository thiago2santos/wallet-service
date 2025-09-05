#!/bin/bash

# Real-time monitoring script for stress test
# Monitors Golden Metrics during extreme load testing

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
PROMETHEUS_URL="http://localhost:9090"
GRAFANA_URL="http://localhost:3000"
WALLET_SERVICE_URL="http://localhost:8080"
MONITOR_INTERVAL=5
LOG_FILE="stress-test-monitor-$(date +%Y%m%d_%H%M%S).log"

print_header() {
    clear
    echo -e "${PURPLE}╔══════════════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${PURPLE}║                    🔥 WALLET SERVICE STRESS TEST MONITOR 🔥                  ║${NC}"
    echo -e "${PURPLE}║                          Golden Metrics Real-Time View                        ║${NC}"
    echo -e "${PURPLE}╚══════════════════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${CYAN}📊 Grafana Golden Metrics: ${GRAFANA_URL}/d/wallet-golden-metrics${NC}"
    echo -e "${CYAN}📈 Prometheus: ${PROMETHEUS_URL}${NC}"
    echo -e "${CYAN}🎯 Wallet Service: ${WALLET_SERVICE_URL}${NC}"
    echo -e "${CYAN}📝 Log File: ${LOG_FILE}${NC}"
    echo ""
    echo -e "${YELLOW}Press Ctrl+C to stop monitoring${NC}"
    echo ""
}

query_prometheus() {
    local query="$1"
    local result=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=${query}" | jq -r '.data.result[0].value[1] // "N/A"' 2>/dev/null)
    echo "$result"
}

format_number() {
    local num="$1"
    if [[ "$num" == "N/A" ]] || [[ "$num" == "" ]]; then
        echo "N/A"
    elif [[ "$num" =~ ^[0-9]+\.?[0-9]*$ ]]; then
        printf "%.2f" "$num"
    else
        echo "$num"
    fi
}

format_percentage() {
    local num="$1"
    if [[ "$num" == "N/A" ]] || [[ "$num" == "" ]]; then
        echo "N/A"
    elif [[ "$num" =~ ^[0-9]+\.?[0-9]*$ ]]; then
        printf "%.1f%%" "$(echo "$num * 100" | bc -l 2>/dev/null || echo "0")"
    else
        echo "$num"
    fi
}

get_color_for_latency() {
    local latency="$1"
    if [[ "$latency" == "N/A" ]]; then
        echo "$YELLOW"
    elif (( $(echo "$latency < 0.05" | bc -l 2>/dev/null || echo "0") )); then
        echo "$GREEN"
    elif (( $(echo "$latency < 0.1" | bc -l 2>/dev/null || echo "0") )); then
        echo "$YELLOW"
    else
        echo "$RED"
    fi
}

get_color_for_error_rate() {
    local rate="$1"
    if [[ "$rate" == "N/A" ]]; then
        echo "$YELLOW"
    elif (( $(echo "$rate < 0.01" | bc -l 2>/dev/null || echo "0") )); then
        echo "$GREEN"
    elif (( $(echo "$rate < 0.05" | bc -l 2>/dev/null || echo "0") )); then
        echo "$YELLOW"
    else
        echo "$RED"
    fi
}

get_color_for_saturation() {
    local sat="$1"
    if [[ "$sat" == "N/A" ]]; then
        echo "$YELLOW"
    elif (( $(echo "$sat < 0.7" | bc -l 2>/dev/null || echo "0") )); then
        echo "$GREEN"
    elif (( $(echo "$sat < 0.9" | bc -l 2>/dev/null || echo "0") )); then
        echo "$YELLOW"
    else
        echo "$RED"
    fi
}

check_service_health() {
    local health_status=$(curl -s -o /dev/null -w "%{http_code}" "${WALLET_SERVICE_URL}/q/health" 2>/dev/null || echo "000")
    if [[ "$health_status" == "200" ]]; then
        echo -e "${GREEN}🟢 HEALTHY${NC}"
    else
        echo -e "${RED}🔴 DOWN (HTTP $health_status)${NC}"
    fi
}

monitor_golden_metrics() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    # 🚀 LATENCY - Golden Signal #1
    local p50_latency=$(query_prometheus 'histogram_quantile(0.50, rate(wallet_operations_creation_duration_seconds_bucket[1m]))')
    local p95_latency=$(query_prometheus 'histogram_quantile(0.95, rate(wallet_operations_creation_duration_seconds_bucket[1m]))')
    local p99_latency=$(query_prometheus 'histogram_quantile(0.99, rate(wallet_operations_creation_duration_seconds_bucket[1m]))')
    
    # 📈 TRAFFIC - Golden Signal #2
    local total_rps=$(query_prometheus 'sum(rate(wallet_operations_created_total[1m]) + rate(wallet_operations_deposits_total[1m]) + rate(wallet_operations_withdrawals_total[1m]) + rate(wallet_operations_transfers_total[1m]) + rate(wallet_operations_queries_total[1m]))')
    local create_rps=$(query_prometheus 'rate(wallet_operations_created_total[1m])')
    local deposit_rps=$(query_prometheus 'rate(wallet_operations_deposits_total[1m])')
    local transfer_rps=$(query_prometheus 'rate(wallet_operations_transfers_total[1m])')
    
    # ❌ ERRORS - Golden Signal #3
    local http_error_rate=$(query_prometheus '(rate(http_server_requests_seconds_count{job="wallet-service", status=~"[45].."}[1m]) / rate(http_server_requests_seconds_count{job="wallet-service"}[1m]))')
    local business_error_rate=$(query_prometheus '(rate(wallet_operations_failed_total[1m]) / (rate(wallet_operations_created_total[1m]) + rate(wallet_operations_deposits_total[1m]) + rate(wallet_operations_withdrawals_total[1m]) + rate(wallet_operations_transfers_total[1m]) + rate(wallet_operations_queries_total[1m])))')
    
    # ⚡ SATURATION - Golden Signal #4
    local cpu_usage=$(query_prometheus 'process_cpu_usage{job="wallet-service"}')
    local memory_usage=$(query_prometheus '(jvm_memory_used_bytes{job="wallet-service", area="heap"} / jvm_memory_max_bytes{job="wallet-service", area="heap"})')
    local db_pool_usage=$(query_prometheus '(hikaricp_connections_active{job="wallet-service"} / hikaricp_connections_max{job="wallet-service"})')
    
    # Additional system metrics
    local jvm_threads=$(query_prometheus 'jvm_threads_current{job="wallet-service"}')
    local gc_rate=$(query_prometheus 'rate(jvm_gc_collection_seconds_count{job="wallet-service"}[1m])')
    
    # Service health
    local service_health=$(check_service_health)
    
    # Display metrics
    print_header
    
    echo -e "${BLUE}🚀 GOLDEN SIGNAL #1: LATENCY${NC}"
    echo -e "$(get_color_for_latency "$p50_latency")   P50: $(format_number "$p50_latency")s${NC}  |  $(get_color_for_latency "$p95_latency")P95: $(format_number "$p95_latency")s${NC}  |  $(get_color_for_latency "$p99_latency")P99: $(format_number "$p99_latency")s${NC}"
    echo ""
    
    echo -e "${BLUE}📈 GOLDEN SIGNAL #2: TRAFFIC${NC}"
    echo -e "   Total RPS: ${CYAN}$(format_number "$total_rps")${NC}  |  Create: ${CYAN}$(format_number "$create_rps")${NC}  |  Deposit: ${CYAN}$(format_number "$deposit_rps")${NC}  |  Transfer: ${CYAN}$(format_number "$transfer_rps")${NC}"
    echo ""
    
    echo -e "${BLUE}❌ GOLDEN SIGNAL #3: ERRORS${NC}"
    echo -e "$(get_color_for_error_rate "$http_error_rate")   HTTP Errors: $(format_percentage "$http_error_rate")${NC}  |  $(get_color_for_error_rate "$business_error_rate")Business Errors: $(format_percentage "$business_error_rate")${NC}"
    echo ""
    
    echo -e "${BLUE}⚡ GOLDEN SIGNAL #4: SATURATION${NC}"
    echo -e "$(get_color_for_saturation "$cpu_usage")   CPU: $(format_percentage "$cpu_usage")${NC}  |  $(get_color_for_saturation "$memory_usage")Memory: $(format_percentage "$memory_usage")${NC}  |  $(get_color_for_saturation "$db_pool_usage")DB Pool: $(format_percentage "$db_pool_usage")${NC}"
    echo ""
    
    echo -e "${BLUE}🔧 SYSTEM METRICS${NC}"
    echo -e "   JVM Threads: ${CYAN}$(format_number "$jvm_threads")${NC}  |  GC Rate: ${CYAN}$(format_number "$gc_rate")/s${NC}  |  Service: $service_health"
    echo ""
    
    echo -e "${BLUE}📊 SLO COMPLIANCE${NC}"
    local latency_slo="❌"
    local error_slo="❌"
    local availability_slo="❌"
    
    if (( $(echo "$p95_latency < 0.1" | bc -l 2>/dev/null || echo "0") )); then
        latency_slo="${GREEN}✅${NC}"
    fi
    
    if (( $(echo "$http_error_rate < 0.01" | bc -l 2>/dev/null || echo "0") )); then
        error_slo="${GREEN}✅${NC}"
    fi
    
    if [[ "$service_health" == *"HEALTHY"* ]]; then
        availability_slo="${GREEN}✅${NC}"
    fi
    
    echo -e "   Latency SLO (P95<100ms): $latency_slo  |  Error SLO (<1%): $error_slo  |  Availability SLO: $availability_slo"
    echo ""
    
    echo -e "${YELLOW}⏰ Last Updated: $timestamp${NC}"
    
    # Log to file
    echo "$timestamp,$(format_number "$p50_latency"),$(format_number "$p95_latency"),$(format_number "$p99_latency"),$(format_number "$total_rps"),$(format_percentage "$http_error_rate"),$(format_percentage "$business_error_rate"),$(format_percentage "$cpu_usage"),$(format_percentage "$memory_usage"),$(format_percentage "$db_pool_usage")" >> "$LOG_FILE"
}

# Create log file header
echo "timestamp,p50_latency,p95_latency,p99_latency,total_rps,http_error_rate,business_error_rate,cpu_usage,memory_usage,db_pool_usage" > "$LOG_FILE"

# Main monitoring loop
echo -e "${GREEN}🚀 Starting Golden Metrics monitoring...${NC}"
echo -e "${YELLOW}💡 Open Grafana dashboard: ${GRAFANA_URL}/d/wallet-golden-metrics${NC}"
echo ""

trap 'echo -e "\n${GREEN}📊 Monitoring stopped. Log saved to: $LOG_FILE${NC}"; exit 0' INT

while true; do
    monitor_golden_metrics
    sleep $MONITOR_INTERVAL
done
