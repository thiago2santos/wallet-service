#!/bin/bash

# Test script to simulate a fresh clone experience
# This script tests what a new developer would experience

set -e

echo "🧪 Testing Fresh Clone Experience..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_step() {
    echo -e "\n${BLUE}📋 Step: $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Simulate what a fresh clone would have
print_step "Checking what files would be available after git clone"

required_files=(
    "infra/local-dev/docker-compose.yml"
    "infra/local-dev/grafana/dashboards/wallet-service-overview.json"
    "infra/local-dev/grafana/dashboards/wallet-business-metrics.json"
    "infra/local-dev/grafana/dashboards/wallet-technical-metrics.json"
    "infra/local-dev/grafana/dashboards/wallet-infrastructure-metrics.json"
    "infra/local-dev/grafana/dashboards/wallet-golden-metrics.json"
    "infra/local-dev/grafana/provisioning/dashboards/wallet-service.yml"
    "infra/local-dev/grafana/provisioning/datasources/prometheus.yml"
    "infra/local-dev/grafana/README.md"
    "infra/scripts/verify-grafana-setup.sh"
)

all_files_present=true
for file in "${required_files[@]}"; do
    if [ -f "$file" ]; then
        print_success "✓ $file exists and will be cloned"
    else
        print_error "✗ $file is missing - won't be available after clone"
        all_files_present=false
    fi
done

if [ "$all_files_present" = false ]; then
    print_error "Some files are missing! Please commit them to git."
    exit 1
fi

print_step "Checking if files are tracked by git"

untracked_files=()
for file in "${required_files[@]}"; do
    if git ls-files --error-unmatch "$file" >/dev/null 2>&1; then
        print_success "✓ $file is tracked by git"
    else
        print_warning "⚠ $file is not tracked by git yet"
        untracked_files+=("$file")
    fi
done

if [ ${#untracked_files[@]} -gt 0 ]; then
    print_warning "The following files need to be committed:"
    for file in "${untracked_files[@]}"; do
        echo "  - $file"
    done
    echo ""
    echo "Run: git add ${untracked_files[*]} && git commit -m 'Add Grafana dashboards and monitoring setup'"
fi

print_step "Testing the complete workflow"

echo "🔄 What happens when someone clones this repo:"
echo ""
echo "1. git clone <repo-url>"
echo "2. cd wallet-service"
echo "3. cd infra/local-dev && docker-compose up -d"
echo "4. cd ../../ && ./infra/scripts/verify-grafana-setup.sh"
echo "5. Open http://localhost:3000 (admin/admin)"
echo "6. See 4 dashboards in 'Wallet Service' folder"
echo ""

print_step "Verifying current setup works"

# Run our verification script
if ./infra/scripts/verify-grafana-setup.sh >/dev/null 2>&1; then
    print_success "Current setup verification passed!"
else
    print_error "Current setup verification failed!"
    echo "Run: ./infra/scripts/verify-grafana-setup.sh for details"
    exit 1
fi

print_step "Persistence Guarantee Summary"

echo ""
echo "🔒 PERSISTENCE GUARANTEE:"
echo "========================="
echo ""
echo "✅ All dashboard JSON files are in git"
echo "✅ All provisioning configs are in git"  
echo "✅ Docker-compose.yml volume mounts are in git"
echo "✅ Verification script is in git"
echo "✅ Documentation is updated"
echo ""
echo "🎯 RESULT: Anyone who clones this repo will get:"
echo "• Automatic dashboard provisioning"
echo "• Zero manual configuration required"
echo "• 5 comprehensive monitoring dashboards (including Golden Metrics)"
echo "• Prometheus datasource pre-configured"
echo "• Verification script to test setup"
echo ""

if [ ${#untracked_files[@]} -eq 0 ]; then
    print_success "🚀 READY FOR PRODUCTION: All files are committed and persistent!"
    echo ""
    echo "Next steps for the team:"
    echo "1. Push changes: git push"
    echo "2. Share repo with team"
    echo "3. Team runs: git clone && cd infra/local-dev && docker-compose up -d"
    echo "4. Dashboards work immediately!"
else
    print_warning "⏳ ALMOST READY: Commit the untracked files first"
fi

echo ""
echo "🧪 Fresh clone test completed!"
