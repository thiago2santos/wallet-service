#!/bin/bash

# Performance Monitoring Script for Wallet Service
# Usage: ./monitor-metrics.sh [duration_in_seconds]

DURATION=${1:-300}  # Default 5 minutes
PROMETHEUS_URL="http://localhost:9090"
OUTPUT_DIR="docs/performance/results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

echo "🔍 Starting Performance Monitoring for ${DURATION} seconds..."
echo "📊 Results will be saved to: ${OUTPUT_DIR}/metrics_${TIMESTAMP}.txt"

# Create output directory
mkdir -p "$OUTPUT_DIR"
OUTPUT_FILE="${OUTPUT_DIR}/metrics_${TIMESTAMP}.txt"

# Function to query Prometheus
query_prometheus() {
    local query="$1"
    local label="$2"
    echo "=== $label ===" >> "$OUTPUT_FILE"
    echo "Query: $query" >> "$OUTPUT_FILE"
    curl -s "${PROMETHEUS_URL}/api/v1/query?query=${query}" | jq -r '.data.result[] | "\(.metric | to_entries | map("\(.key)=\(.value)") | join(" ")) value=\(.value[1])"' >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"
}

# Function to monitor metrics
monitor_metrics() {
    echo "$(date): Collecting metrics..." | tee -a "$OUTPUT_FILE"
    
    # Application Health
    query_prometheus "up" "Application Health"
    
    # Response Times (95th percentile)
    query_prometheus "histogram_quantile(0.95, rate(wallet_operations_creation_duration_seconds_bucket[1m]))" "Wallet Creation - 95th Percentile Response Time"
    query_prometheus "histogram_quantile(0.95, rate(wallet_operations_deposit_duration_seconds_bucket[1m]))" "Deposit - 95th Percentile Response Time"
    query_prometheus "histogram_quantile(0.95, rate(wallet_operations_withdrawal_duration_seconds_bucket[1m]))" "Withdrawal - 95th Percentile Response Time"
    query_prometheus "histogram_quantile(0.95, rate(wallet_operations_transfer_duration_seconds_bucket[1m]))" "Transfer - 95th Percentile Response Time"
    query_prometheus "histogram_quantile(0.95, rate(wallet_operations_query_duration_seconds_bucket[1m]))" "Query - 95th Percentile Response Time"
    
    # Throughput (requests per second)
    query_prometheus "rate(wallet_operations_created_total[1m])" "Wallet Creation Rate (RPS)"
    query_prometheus "rate(wallet_operations_deposits_total[1m])" "Deposit Rate (RPS)"
    query_prometheus "rate(wallet_operations_withdrawals_total[1m])" "Withdrawal Rate (RPS)"
    query_prometheus "rate(wallet_operations_transfers_total[1m])" "Transfer Rate (RPS)"
    query_prometheus "rate(wallet_operations_queries_total[1m])" "Query Rate (RPS)"
    
    # Error Rates
    query_prometheus "rate(wallet_operations_failed_total[1m])" "Failed Operations Rate"
    
    # Business Metrics
    query_prometheus "wallet_operations_created_total" "Total Wallets Created"
    query_prometheus "rate(wallet_money_deposited_total[1m])" "Money Deposit Rate (BRL/sec)"
    query_prometheus "rate(wallet_money_withdrawn_total[1m])" "Money Withdrawal Rate (BRL/sec)"
    query_prometheus "rate(wallet_money_transferred_total[1m])" "Money Transfer Rate (BRL/sec)"
    
    # System Metrics (if available)
    query_prometheus "jvm_memory_used_bytes{area=\"heap\"}" "JVM Heap Usage"
    query_prometheus "jvm_threads_current" "JVM Thread Count"
    query_prometheus "process_cpu_usage" "CPU Usage"
    
    echo "----------------------------------------" >> "$OUTPUT_FILE"
}

# Initial metrics collection
echo "📊 Performance Monitoring Started at $(date)" > "$OUTPUT_FILE"
echo "Duration: ${DURATION} seconds" >> "$OUTPUT_FILE"
echo "Prometheus URL: ${PROMETHEUS_URL}" >> "$OUTPUT_FILE"
echo "========================================" >> "$OUTPUT_FILE"

# Monitor metrics every 30 seconds
INTERVAL=30
ITERATIONS=$((DURATION / INTERVAL))

for i in $(seq 1 $ITERATIONS); do
    echo "📈 Collecting metrics (${i}/${ITERATIONS})..."
    monitor_metrics
    
    if [ $i -lt $ITERATIONS ]; then
        echo "⏳ Waiting ${INTERVAL} seconds..."
        sleep $INTERVAL
    fi
done

echo "✅ Monitoring completed!"
echo "📄 Results saved to: $OUTPUT_FILE"

# Generate summary
echo ""
echo "📊 SUMMARY"
echo "=========="
echo "Duration: ${DURATION} seconds"
echo "Samples: ${ITERATIONS}"
echo "Output: $OUTPUT_FILE"
echo ""
echo "🔍 To view results:"
echo "cat $OUTPUT_FILE"
echo ""
echo "📈 To analyze with Prometheus directly:"
echo "curl '${PROMETHEUS_URL}/api/v1/query?query=rate(wallet_operations_deposits_total[5m])'"
