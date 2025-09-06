#!/bin/bash

# Extreme Load Test Runner
# This script runs the extreme load test with proper monitoring and result collection

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
RESULTS_DIR="$PROJECT_ROOT/results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
TEST_NAME="extreme-load-test-$TIMESTAMP"
RESULTS_PATH="$RESULTS_DIR/$TEST_NAME"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 EXTREME LOAD TEST RUNNER${NC}"
echo -e "${BLUE}================================${NC}"
echo ""

# Check if k6 is installed
if ! command -v k6 &> /dev/null; then
    echo -e "${RED}❌ k6 is not installed. Please install it first:${NC}"
    echo "   brew install k6"
    echo "   or visit: https://k6.io/docs/getting-started/installation/"
    exit 1
fi

# Check if wallet service is running
echo -e "${YELLOW}🔍 Checking if wallet service is running...${NC}"
if ! curl -s http://localhost:8080/health > /dev/null 2>&1; then
    echo -e "${RED}❌ Wallet service is not running on localhost:8080${NC}"
    echo -e "${YELLOW}💡 Start it with: ./mvnw quarkus:dev${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Wallet service is running${NC}"

# Create results directory
mkdir -p "$RESULTS_PATH"
echo -e "${YELLOW}📁 Results will be saved to: $RESULTS_PATH${NC}"

# Check if monitoring is available
if curl -s http://localhost:3000 > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Grafana monitoring available at http://localhost:3000${NC}"
    echo -e "${BLUE}📊 Golden Metrics: http://localhost:3000/d/wallet-golden-metrics${NC}"
else
    echo -e "${YELLOW}⚠️  Grafana monitoring not available (optional)${NC}"
fi

echo ""
echo -e "${RED}⚠️  WARNING: This test will push your system to its limits!${NC}"
echo -e "${RED}⚠️  It may cause high CPU, memory usage, and potential system stress.${NC}"
echo ""
read -p "Do you want to continue? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Test cancelled.${NC}"
    exit 0
fi

echo ""
echo -e "${GREEN}🏁 Starting extreme load test...${NC}"
echo -e "${BLUE}Test configuration:${NC}"
echo "  - Max VUs: 5000"
echo "  - Duration: ~15 minutes"
echo "  - Operations: Balance queries, deposits, withdrawals, transfers, wallet creation"
echo "  - Target: Find the TRUE breaking point"
echo ""

# Start system monitoring in background
echo -e "${YELLOW}📊 Starting system monitoring...${NC}"
{
    echo "timestamp,cpu_percent,memory_percent,load_avg"
    while true; do
        timestamp=$(date '+%Y-%m-%d %H:%M:%S')
        cpu=$(top -l 1 -n 0 | grep "CPU usage" | awk '{print $3}' | sed 's/%//')
        memory=$(top -l 1 -n 0 | grep "PhysMem" | awk '{print $2}' | sed 's/M//')
        load=$(uptime | awk -F'load averages:' '{print $2}' | awk '{print $1}' | sed 's/,//')
        echo "$timestamp,$cpu,$memory,$load"
        sleep 5
    done
} > "$RESULTS_PATH/system-metrics.csv" &
MONITOR_PID=$!

# Function to cleanup on exit
cleanup() {
    echo ""
    echo -e "${YELLOW}🧹 Cleaning up...${NC}"
    if kill -0 $MONITOR_PID 2>/dev/null; then
        kill $MONITOR_PID
    fi
    echo -e "${GREEN}✅ Cleanup complete${NC}"
}
trap cleanup EXIT

# Run the extreme load test
echo -e "${GREEN}🚀 Executing extreme load test...${NC}"
cd "$PROJECT_ROOT"

k6 run \
    --out json="$RESULTS_PATH/results.json" \
    --summary-export="$RESULTS_PATH/summary.json" \
    performance/scripts/k6/extreme-load-test.js \
    2>&1 | tee "$RESULTS_PATH/test-output.log"

TEST_EXIT_CODE=${PIPESTATUS[0]}

echo ""
echo -e "${BLUE}📊 TEST COMPLETED${NC}"
echo -e "${BLUE}=================${NC}"

if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✅ Test completed successfully${NC}"
else
    echo -e "${YELLOW}⚠️  Test completed with issues (exit code: $TEST_EXIT_CODE)${NC}"
fi

echo ""
echo -e "${BLUE}📁 Results saved to:${NC}"
echo "  - Full results: $RESULTS_PATH/results.json"
echo "  - Summary: $RESULTS_PATH/summary.json"
echo "  - Test log: $RESULTS_PATH/test-output.log"
echo "  - System metrics: $RESULTS_PATH/system-metrics.csv"

echo ""
echo -e "${BLUE}📈 Quick Analysis:${NC}"

# Extract key metrics from summary if available
if [ -f "$RESULTS_PATH/summary.json" ]; then
    echo -e "${YELLOW}Analyzing results...${NC}"
    
    # Use jq if available, otherwise basic grep
    if command -v jq &> /dev/null; then
        echo "  - Total requests: $(jq -r '.metrics.http_reqs.count // "N/A"' "$RESULTS_PATH/summary.json")"
        echo "  - Failed requests: $(jq -r '.metrics.http_req_failed.count // "N/A"' "$RESULTS_PATH/summary.json")"
        echo "  - Avg response time: $(jq -r '.metrics.http_req_duration.avg // "N/A"' "$RESULTS_PATH/summary.json")ms"
        echo "  - 95th percentile: $(jq -r '.metrics.http_req_duration.p95 // "N/A"' "$RESULTS_PATH/summary.json")ms"
        echo "  - Max response time: $(jq -r '.metrics.http_req_duration.max // "N/A"' "$RESULTS_PATH/summary.json")ms"
    else
        echo "  - Install jq for detailed analysis: brew install jq"
    fi
fi

echo ""
echo -e "${GREEN}🎯 Next Steps:${NC}"
echo "1. Review the test output above for error patterns"
echo "2. Check system metrics in: $RESULTS_PATH/system-metrics.csv"
echo "3. If monitoring is available, review Grafana dashboards"
echo "4. Look for the point where response times spiked or errors increased"
echo "5. That's your system's breaking point!"

echo ""
echo -e "${BLUE}💡 Tips for Analysis:${NC}"
echo "- Look for when response times went above 1000ms"
echo "- Check when error rates exceeded 5-10%"
echo "- Monitor CPU/memory usage during peak load"
echo "- The breaking point is where performance degrades significantly"

if curl -s http://localhost:3000 > /dev/null 2>&1; then
    echo ""
    echo -e "${GREEN}📊 View detailed metrics at: http://localhost:3000/d/wallet-golden-metrics${NC}"
fi
