#!/bin/bash

# Extreme Stress Test Orchestrator
# This script coordinates the entire stress testing process

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
GRAFANA_URL="http://localhost:3000/d/wallet-golden-metrics"
PROMETHEUS_URL="http://localhost:9090"
WALLET_SERVICE_URL="http://localhost:8080"
TEST_RESULTS_DIR="../../results/current/stress-test-$(date +%Y%m%d_%H%M%S)"

print_banner() {
    echo -e "${RED}"
    echo "╔══════════════════════════════════════════════════════════════════════════════╗"
    echo "║                                                                              ║"
    echo "║                    🔥 EXTREME WALLET SERVICE STRESS TEST 🔥                 ║"
    echo "║                                                                              ║"
    echo "║                        ⚠️  WARNING: DESTRUCTIVE TEST ⚠️                     ║"
    echo "║                                                                              ║"
    echo "║              This test will attempt to CRASH the wallet service             ║"
    echo "║                                                                              ║"
    echo "╚══════════════════════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

check_prerequisites() {
    echo -e "${BLUE}🔍 Checking prerequisites...${NC}"
    
    # Check if k6 is installed
    if ! command -v k6 &> /dev/null; then
        echo -e "${RED}❌ k6 is not installed. Install it first:${NC}"
        echo "   macOS: brew install k6"
        echo "   Linux: https://k6.io/docs/getting-started/installation/"
        exit 1
    fi
    
    # Check if jq is installed (for JSON parsing)
    if ! command -v jq &> /dev/null; then
        echo -e "${RED}❌ jq is not installed. Install it first:${NC}"
        echo "   macOS: brew install jq"
        echo "   Linux: sudo apt-get install jq"
        exit 1
    fi
    
    # Check if bc is installed (for calculations)
    if ! command -v bc &> /dev/null; then
        echo -e "${RED}❌ bc is not installed. Install it first:${NC}"
        echo "   macOS: brew install bc"
        echo "   Linux: sudo apt-get install bc"
        exit 1
    fi
    
    # Check if services are running
    echo -e "${YELLOW}🔍 Checking service availability...${NC}"
    
    if ! curl -s -o /dev/null -w "%{http_code}" "$WALLET_SERVICE_URL/q/health" | grep -q "200"; then
        echo -e "${RED}❌ Wallet service is not running at $WALLET_SERVICE_URL${NC}"
        echo "   Start it with: docker-compose up -d or mvn quarkus:dev"
        exit 1
    fi
    
    if ! curl -s -o /dev/null -w "%{http_code}" "$PROMETHEUS_URL/api/v1/status/config" | grep -q "200"; then
        echo -e "${RED}❌ Prometheus is not running at $PROMETHEUS_URL${NC}"
        echo "   Start it with: docker-compose up -d prometheus"
        exit 1
    fi
    
    if ! curl -s -o /dev/null -w "%{http_code}" "$GRAFANA_URL" | grep -q "200"; then
        echo -e "${YELLOW}⚠️  Grafana may not be accessible at $GRAFANA_URL${NC}"
        echo "   Start it with: docker-compose up -d grafana"
    fi
    
    echo -e "${GREEN}✅ All prerequisites met!${NC}"
}

setup_test_environment() {
    echo -e "${BLUE}🛠️  Setting up test environment...${NC}"
    
    # Create results directory
    mkdir -p "$TEST_RESULTS_DIR"
    
    # Get baseline metrics
    echo -e "${YELLOW}📊 Capturing baseline metrics...${NC}"
    curl -s "${PROMETHEUS_URL}/api/v1/query?query=process_cpu_usage{job=\"wallet-service\"}" > "$TEST_RESULTS_DIR/baseline-cpu.json"
    curl -s "${PROMETHEUS_URL}/api/v1/query?query=jvm_memory_used_bytes{job=\"wallet-service\",area=\"heap\"}" > "$TEST_RESULTS_DIR/baseline-memory.json"
    curl -s "${PROMETHEUS_URL}/api/v1/query?query=hikaricp_connections_active{job=\"wallet-service\"}" > "$TEST_RESULTS_DIR/baseline-db.json"
    
    echo -e "${GREEN}✅ Test environment ready!${NC}"
    echo -e "${CYAN}📁 Results will be saved to: $TEST_RESULTS_DIR${NC}"
}

start_monitoring() {
    echo -e "${BLUE}📊 Starting real-time monitoring...${NC}"
    
    # Start the monitoring script in background
    ../../monitoring/monitor-stress-test.sh > "$TEST_RESULTS_DIR/monitor-output.log" 2>&1 &
    MONITOR_PID=$!
    
    echo -e "${GREEN}✅ Monitoring started (PID: $MONITOR_PID)${NC}"
    echo -e "${CYAN}📊 Golden Metrics Dashboard: $GRAFANA_URL${NC}"
    
    return $MONITOR_PID
}

run_stress_test() {
    echo -e "${BLUE}🔥 Starting extreme stress test...${NC}"
    echo -e "${YELLOW}⚠️  This will progressively increase load until system failure!${NC}"
    echo ""
    
    # Show test phases
    echo -e "${CYAN}📋 Test Phases:${NC}"
    echo "   Phase 1: Baseline (10-25 users) - Should work fine"
    echo "   Phase 2: Moderate (50-100 users) - Some stress"
    echo "   Phase 3: High stress (200-300 users) - Heavy load"
    echo "   Phase 4: Breaking point (500-1000 users) - Expect failures"
    echo "   Phase 5: Nuclear option (1500-2000 users) - System death"
    echo ""
    
    # Ask for confirmation
    echo -e "${RED}⚠️  Are you sure you want to proceed? This WILL attempt to crash the system!${NC}"
    read -p "Type 'CRASH' to confirm: " confirmation
    
    if [[ "$confirmation" != "CRASH" ]]; then
        echo -e "${YELLOW}❌ Test cancelled by user${NC}"
        return 1
    fi
    
    echo -e "${RED}🚀 LAUNCHING EXTREME STRESS TEST!${NC}"
    echo ""
    
    # Run the k6 stress test
    k6 run --out json="$TEST_RESULTS_DIR/k6-results.json" ../k6/stress-test-to-failure.js 2>&1 | tee "$TEST_RESULTS_DIR/k6-output.log"
    
    local exit_code=$?
    
    if [[ $exit_code -eq 0 ]]; then
        echo -e "${GREEN}🎉 Test completed successfully! System survived the stress test!${NC}"
    else
        echo -e "${RED}💀 Test failed or system crashed! Exit code: $exit_code${NC}"
    fi
    
    return $exit_code
}

analyze_results() {
    echo -e "${BLUE}📈 Analyzing test results...${NC}"
    
    # Get post-test metrics
    echo -e "${YELLOW}📊 Capturing post-test metrics...${NC}"
    curl -s "${PROMETHEUS_URL}/api/v1/query?query=process_cpu_usage{job=\"wallet-service\"}" > "$TEST_RESULTS_DIR/post-test-cpu.json"
    curl -s "${PROMETHEUS_URL}/api/v1/query?query=jvm_memory_used_bytes{job=\"wallet-service\",area=\"heap\"}" > "$TEST_RESULTS_DIR/post-test-memory.json"
    curl -s "${PROMETHEUS_URL}/api/v1/query?query=hikaricp_connections_active{job=\"wallet-service\"}" > "$TEST_RESULTS_DIR/post-test-db.json"
    
    # Check if service is still alive
    local service_status=$(curl -s -o /dev/null -w "%{http_code}" "$WALLET_SERVICE_URL/q/health" 2>/dev/null || echo "000")
    
    echo -e "${CYAN}📊 Test Results Summary:${NC}"
    echo "=================================="
    
    if [[ "$service_status" == "200" ]]; then
        echo -e "${GREEN}✅ Service Status: ALIVE (HTTP 200)${NC}"
    else
        echo -e "${RED}💀 Service Status: DEAD (HTTP $service_status)${NC}"
    fi
    
    # Parse k6 results if available
    if [[ -f "$TEST_RESULTS_DIR/k6-results.json" ]]; then
        echo ""
        echo -e "${CYAN}📈 K6 Test Metrics:${NC}"
        
        # Extract key metrics from k6 JSON output
        local max_vus=$(jq -r '.metrics.vus.values.max // "N/A"' "$TEST_RESULTS_DIR/k6-results.json")
        local total_requests=$(jq -r '.metrics.http_reqs.values.count // "N/A"' "$TEST_RESULTS_DIR/k6-results.json")
        local avg_response_time=$(jq -r '.metrics.http_req_duration.values.avg // "N/A"' "$TEST_RESULTS_DIR/k6-results.json")
        local p95_response_time=$(jq -r '.metrics.http_req_duration.values["p(95)"] // "N/A"' "$TEST_RESULTS_DIR/k6-results.json")
        local error_rate=$(jq -r '.metrics.http_req_failed.values.rate // "N/A"' "$TEST_RESULTS_DIR/k6-results.json")
        
        echo "   Max Virtual Users: $max_vus"
        echo "   Total Requests: $total_requests"
        echo "   Avg Response Time: ${avg_response_time}ms"
        echo "   P95 Response Time: ${p95_response_time}ms"
        echo "   Error Rate: $(echo "$error_rate * 100" | bc -l 2>/dev/null || echo "N/A")%"
    fi
    
    echo ""
    echo -e "${CYAN}📁 All results saved to: $TEST_RESULTS_DIR${NC}"
    echo -e "${CYAN}📊 View Golden Metrics: $GRAFANA_URL${NC}"
}

cleanup() {
    echo -e "${BLUE}🧹 Cleaning up...${NC}"
    
    # Kill monitoring process if it's still running
    if [[ -n "$MONITOR_PID" ]] && kill -0 "$MONITOR_PID" 2>/dev/null; then
        echo -e "${YELLOW}🔄 Stopping monitoring process...${NC}"
        kill "$MONITOR_PID" 2>/dev/null || true
        wait "$MONITOR_PID" 2>/dev/null || true
    fi
    
    echo -e "${GREEN}✅ Cleanup completed${NC}"
}

main() {
    print_banner
    
    # Set up trap for cleanup
    trap cleanup EXIT INT TERM
    
    check_prerequisites
    setup_test_environment
    
    # Start monitoring
    start_monitoring
    MONITOR_PID=$!
    
    # Give monitoring a moment to start
    sleep 3
    
    # Run the stress test
    if run_stress_test; then
        echo -e "${GREEN}🎉 Stress test completed!${NC}"
    else
        echo -e "${RED}💀 Stress test resulted in system failure or was cancelled${NC}"
    fi
    
    # Stop monitoring
    if [[ -n "$MONITOR_PID" ]] && kill -0 "$MONITOR_PID" 2>/dev/null; then
        kill "$MONITOR_PID" 2>/dev/null || true
        wait "$MONITOR_PID" 2>/dev/null || true
    fi
    
    analyze_results
    
    echo ""
    echo -e "${PURPLE}🏁 EXTREME STRESS TEST COMPLETED!${NC}"
    echo -e "${CYAN}📊 Check your Golden Metrics dashboard for the full story: $GRAFANA_URL${NC}"
    echo ""
}

# Run the main function
main "$@"
