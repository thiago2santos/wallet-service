#!/bin/bash

# Grafana Setup Verification Script
# This script verifies that Grafana dashboards are properly provisioned

set -e

echo "🔍 Verifying Grafana Dashboard Setup..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    local status=$1
    local message=$2
    if [ "$status" = "OK" ]; then
        echo -e "${GREEN}✅ $message${NC}"
    elif [ "$status" = "WARN" ]; then
        echo -e "${YELLOW}⚠️  $message${NC}"
    else
        echo -e "${RED}❌ $message${NC}"
    fi
}

# Check if required files exist
echo "📁 Checking required files..."

required_files=(
    "grafana/dashboards/wallet-service-overview.json"
    "grafana/dashboards/wallet-business-metrics.json"
    "grafana/dashboards/wallet-technical-metrics.json"
    "grafana/dashboards/wallet-infrastructure-metrics.json"
    "grafana/dashboards/wallet-golden-metrics.json"
    "grafana/provisioning/dashboards/wallet-service.yml"
    "grafana/provisioning/datasources/prometheus.yml"
    "docker-compose.yml"
)

all_files_exist=true
for file in "${required_files[@]}"; do
    if [ -f "$file" ]; then
        print_status "OK" "Found $file"
    else
        print_status "ERROR" "Missing $file"
        all_files_exist=false
    fi
done

if [ "$all_files_exist" = false ]; then
    echo -e "\n${RED}❌ Some required files are missing. Please ensure all Grafana configuration files are present.${NC}"
    exit 1
fi

# Check docker-compose.yml for correct volume mounts
echo -e "\n🐳 Checking Docker Compose configuration..."

if grep -q "./grafana/dashboards:/var/lib/grafana/dashboards/wallet-service:ro" docker-compose.yml; then
    print_status "OK" "Dashboard volume mount configured"
else
    print_status "ERROR" "Dashboard volume mount missing in docker-compose.yml"
    exit 1
fi

if grep -q "./grafana/provisioning/dashboards:/etc/grafana/provisioning/dashboards:ro" docker-compose.yml; then
    print_status "OK" "Dashboard provisioning volume mount configured"
else
    print_status "ERROR" "Dashboard provisioning volume mount missing in docker-compose.yml"
    exit 1
fi

if grep -q "./grafana/provisioning/datasources:/etc/grafana/provisioning/datasources:ro" docker-compose.yml; then
    print_status "OK" "Datasource provisioning volume mount configured"
else
    print_status "ERROR" "Datasource provisioning volume mount missing in docker-compose.yml"
    exit 1
fi

# Check if containers are running
echo -e "\n🚀 Checking container status..."

if docker-compose ps | grep -q "wallet-grafana.*Up"; then
    print_status "OK" "Grafana container is running"
    
    # Wait a moment for Grafana to fully start
    echo "⏳ Waiting for Grafana to be ready..."
    sleep 5
    
    # Check if Grafana is responding
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/api/health | grep -q "200"; then
        print_status "OK" "Grafana is responding on port 3000"
    else
        print_status "WARN" "Grafana may not be fully ready yet"
    fi
    
    # Check if files are mounted correctly in container
    echo -e "\n📂 Checking file mounts in container..."
    
    if docker exec wallet-grafana test -f /etc/grafana/provisioning/dashboards/wallet-service.yml; then
        print_status "OK" "Dashboard provisioning config mounted"
    else
        print_status "ERROR" "Dashboard provisioning config not mounted"
    fi
    
    if docker exec wallet-grafana test -f /etc/grafana/provisioning/datasources/prometheus.yml; then
        print_status "OK" "Datasource provisioning config mounted"
    else
        print_status "ERROR" "Datasource provisioning config not mounted"
    fi
    
    dashboard_count=$(docker exec wallet-grafana find /var/lib/grafana/dashboards/wallet-service -name "*.json" | wc -l)
    if [ "$dashboard_count" -eq 5 ]; then
        print_status "OK" "All 5 dashboard files mounted ($dashboard_count found)"
    else
        print_status "ERROR" "Expected 5 dashboard files, found $dashboard_count"
    fi
    
else
    print_status "WARN" "Grafana container is not running. Start it with: docker-compose up -d grafana"
fi

# Check if Prometheus is running (required for dashboards to work)
echo -e "\n📊 Checking Prometheus..."

if docker-compose ps | grep -q "wallet-prometheus.*Up"; then
    print_status "OK" "Prometheus container is running"
    
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:9090/api/v1/status/config | grep -q "200"; then
        print_status "OK" "Prometheus is responding on port 9090"
    else
        print_status "WARN" "Prometheus may not be fully ready yet"
    fi
else
    print_status "WARN" "Prometheus container is not running. Start it with: docker-compose up -d prometheus"
fi

echo -e "\n🎯 Setup Summary:"
echo "===================="
echo "• Grafana URL: http://localhost:3000"
echo "• Default credentials: admin/admin"
echo "• Dashboards should appear in 'Wallet Service' folder"
echo "• Prometheus URL: http://localhost:9090"
echo ""
echo "📋 Expected Dashboards:"
echo "• Wallet Service - Overview"
echo "• Wallet Service - Business Metrics"
echo "• Wallet Service - Technical Metrics"
echo "• Wallet Service - Infrastructure"
echo "• Wallet Service - Golden Metrics (SRE) ⭐"
echo ""

if [ "$all_files_exist" = true ]; then
    print_status "OK" "Grafana dashboard setup verification completed successfully!"
    echo -e "\n${GREEN}🚀 Ready to go! Anyone who clones this repo can run 'docker-compose up -d' and get the dashboards automatically.${NC}"
else
    print_status "ERROR" "Setup verification failed. Please fix the issues above."
    exit 1
fi
