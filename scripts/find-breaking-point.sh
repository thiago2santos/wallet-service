#!/bin/bash

# Find System Breaking Point
# Runs the continuous wallet stress test while monitoring Golden Metrics

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

echo -e "${PURPLE}"
echo "╔══════════════════════════════════════════════════════════════════════════════╗"
echo "║                                                                              ║"
echo "║                    🎯 FIND SYSTEM BREAKING POINT 🎯                         ║"
echo "║                                                                              ║"
echo "║              Smart approach: Create wallets + use them immediately          ║"
echo "║                                                                              ║"
echo "╚══════════════════════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

echo -e "${BLUE}🎯 Test Strategy:${NC}"
echo "• Create new wallets every 2 seconds"
echo "• Immediately deposit money into new wallets"
echo "• Perform random transactions (withdraw, transfer, query)"
echo "• Scale from 5 → 500 users over 22 minutes"
echo "• Monitor everything in Golden Metrics dashboard"
echo ""

echo -e "${YELLOW}📊 Golden Metrics Dashboard: http://localhost:3000/d/wallet-golden-metrics${NC}"
echo ""

# Check if service is running
if ! curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/q/health" | grep -q "200"; then
    echo -e "${RED}❌ Wallet service is not running!${NC}"
    echo "Start it with: docker-compose up -d"
    exit 1
fi

echo -e "${GREEN}✅ Wallet service is running${NC}"

# Auto-start without confirmation
echo -e "${YELLOW}🚀 Auto-starting breaking point test in 3 seconds...${NC}"
sleep 3
echo ""
echo -e "${GREEN}🚀 Starting continuous wallet stress test...${NC}"
echo -e "${CYAN}📊 Watch the Golden Metrics dashboard to see the system under stress!${NC}"
echo ""

# Create results directory
RESULTS_DIR="results/breaking-point-$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RESULTS_DIR"

# Run the test and save results
k6 run --out json="$RESULTS_DIR/results.json" scripts/continuous-wallet-stress.js 2>&1 | tee "$RESULTS_DIR/test-output.log"

echo ""
echo -e "${BLUE}📈 Test completed! Results saved to: $RESULTS_DIR${NC}"
echo -e "${CYAN}📊 Check Golden Metrics for the full story: http://localhost:3000/d/wallet-golden-metrics${NC}"
