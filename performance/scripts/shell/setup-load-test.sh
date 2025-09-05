#!/bin/bash

# Setup script for load testing the Wallet Service
# This script prepares the environment and installs necessary tools

set -e

echo "🚀 Setting up load testing environment for Wallet Service"
echo "========================================================"

# Check if running on macOS or Linux
if [[ "$OSTYPE" == "darwin"* ]]; then
    PLATFORM="macos"
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    PLATFORM="linux"
else
    echo "❌ Unsupported platform: $OSTYPE"
    exit 1
fi

echo "📋 Platform detected: $PLATFORM"

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Install K6 if not present
echo ""
echo "🔧 Checking K6 installation..."
if command_exists k6; then
    echo "✅ K6 is already installed: $(k6 version)"
else
    echo "📦 Installing K6..."
    if [[ "$PLATFORM" == "macos" ]]; then
        if command_exists brew; then
            brew install k6
        else
            echo "❌ Homebrew not found. Please install Homebrew first or install K6 manually."
            echo "   Visit: https://k6.io/docs/get-started/installation/"
            exit 1
        fi
    elif [[ "$PLATFORM" == "linux" ]]; then
        # Add K6 repository and install
        sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
        echo "deb https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
        sudo apt-get update
        sudo apt-get install k6
    fi
    echo "✅ K6 installed successfully"
fi

# Check Docker and Docker Compose
echo ""
echo "🐳 Checking Docker installation..."
if command_exists docker; then
    echo "✅ Docker is installed: $(docker --version)"
else
    echo "❌ Docker not found. Please install Docker first."
    echo "   Visit: https://docs.docker.com/get-docker/"
    exit 1
fi

if command_exists docker-compose; then
    echo "✅ Docker Compose is installed: $(docker-compose --version)"
else
    echo "❌ Docker Compose not found. Please install Docker Compose first."
    exit 1
fi

# Create results directory
echo ""
echo "📁 Creating results directory..."
mkdir -p results
echo "✅ Results directory created: ./results"

# Check if application is running
echo ""
echo "🔍 Checking if Wallet Service is running..."
if curl -s http://localhost:8080/q/health >/dev/null 2>&1; then
    echo "✅ Wallet Service is running at http://localhost:8080"
else
    echo "⚠️  Wallet Service is not running at http://localhost:8080"
    echo "   Please start the service with: ./mvnw quarkus:dev"
    echo "   Or with Docker: docker-compose up -d"
fi

# Create a simple test script
echo ""
echo "📝 Creating quick test script..."
cat > scripts/quick-test.js << 'EOF'
// Quick test to verify the service is working
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 1,
  duration: '10s',
};

export default function() {
  // Test health endpoint
  const healthResponse = http.get('http://localhost:8080/q/health');
  check(healthResponse, {
    'health check status is 200': (r) => r.status === 200,
  });
  
  // Test wallet creation
  const createResponse = http.post('http://localhost:8080/api/v1/wallets', 
    JSON.stringify({
      userId: 'quick-test-user',
      currency: 'USD'
    }), {
      headers: { 'Content-Type': 'application/json' },
    }
  );
  
  check(createResponse, {
    'wallet creation status is 201': (r) => r.status === 201,
  });
}
EOF

echo "✅ Quick test script created: scripts/quick-test.js"

# Create monitoring setup script
echo ""
echo "📊 Creating monitoring setup..."
cat > scripts/start-monitoring.sh << 'EOF'
#!/bin/bash
echo "🔍 Starting monitoring stack..."
docker-compose -f docker-compose.yml up -d prometheus grafana
echo "✅ Monitoring started:"
echo "   Prometheus: http://localhost:9090"
echo "   Grafana: http://localhost:3000 (admin/admin)"
EOF

chmod +x scripts/start-monitoring.sh
echo "✅ Monitoring setup script created: scripts/start-monitoring.sh"

# Create load test runner script
echo ""
echo "🏃 Creating load test runner..."
cat > scripts/run-load-test.sh << 'EOF'
#!/bin/bash

echo "🚀 Starting Wallet Service Load Test"
echo "===================================="

# Check if service is running
if ! curl -s http://localhost:8080/q/health >/dev/null 2>&1; then
    echo "❌ Wallet Service is not running. Please start it first."
    exit 1
fi

# Create results directory with timestamp
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RESULTS_DIR="../../results/current/loadtest_$TIMESTAMP"
mkdir -p "$RESULTS_DIR"

echo "📁 Results will be saved to: $RESULTS_DIR"

# Run the load test
echo "🏃 Running load test..."
k6 run --out json="$RESULTS_DIR/results.json" ../k6/load-test-basic.js

echo "✅ Load test completed!"
echo "📊 Results saved to: $RESULTS_DIR"
echo ""
echo "📈 To view results:"
echo "   - JSON data: $RESULTS_DIR/results.json"
echo "   - Summary was displayed above"
EOF

chmod +x scripts/run-load-test.sh
echo "✅ Load test runner created: scripts/run-load-test.sh"

echo ""
echo "🎉 Load testing setup complete!"
echo ""
echo "📋 Next steps:"
echo "1. Start your Wallet Service: ./mvnw quarkus:dev"
echo "2. Run quick test: k6 run scripts/quick-test.js"
echo "3. Run full load test: ./scripts/run-load-test.sh"
echo "4. Start monitoring (optional): ./scripts/start-monitoring.sh"
echo ""
echo "📚 Documentation: Check docs/load-testing-plan.md for detailed scenarios"
echo ""
echo "🎯 Happy load testing!"
