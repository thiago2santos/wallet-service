#!/bin/bash

# Simple Load Testing Script for Wallet Service
# Usage: ./simple-load-test.sh [test_type] [duration]

TEST_TYPE=${1:-baseline}
DURATION=${2:-60}
BASE_URL="http://localhost:8080"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RESULTS_DIR="docs/performance/results"

echo "🚀 Starting Load Test: $TEST_TYPE"
echo "⏱️  Duration: ${DURATION} seconds"
echo "🎯 Target: $BASE_URL"

# Create results directory
mkdir -p "$RESULTS_DIR"

# Function to test wallet creation
test_wallet_creation() {
    echo "📝 Testing Wallet Creation..."
    
    for i in $(seq 1 10); do
        USER_ID="load-test-user-${TIMESTAMP}-${i}"
        
        RESPONSE=$(curl -s -w "%{http_code}:%{time_total}" \
            -X POST "$BASE_URL/api/v1/wallets" \
            -H "Content-Type: application/json" \
            -d "{\"userId\": \"$USER_ID\"}")
        
        HTTP_CODE=$(echo "$RESPONSE" | cut -d: -f1)
        RESPONSE_TIME=$(echo "$RESPONSE" | cut -d: -f2)
        
        echo "Wallet Creation - HTTP: $HTTP_CODE, Time: ${RESPONSE_TIME}s"
        
        # Extract wallet ID for further tests
        if [ "$HTTP_CODE" = "201" ]; then
            WALLET_ID=$(echo "$RESPONSE" | grep -o '"Location":"[^"]*"' | sed 's/.*wallets\///g' | sed 's/"//g')
            echo "Created wallet: $WALLET_ID"
            echo "$WALLET_ID" >> "${RESULTS_DIR}/wallets_${TIMESTAMP}.txt"
        fi
        
        sleep 1
    done
}

# Function to test wallet queries
test_wallet_queries() {
    echo "🔍 Testing Wallet Queries..."
    
    WALLET_FILE="${RESULTS_DIR}/wallets_${TIMESTAMP}.txt"
    
    if [ ! -f "$WALLET_FILE" ]; then
        echo "❌ No wallets found. Run wallet creation test first."
        return 1
    fi
    
    while IFS= read -r WALLET_ID; do
        RESPONSE=$(curl -s -w "%{http_code}:%{time_total}" \
            "$BASE_URL/api/v1/wallets/$WALLET_ID")
        
        HTTP_CODE=$(echo "$RESPONSE" | cut -d: -f1)
        RESPONSE_TIME=$(echo "$RESPONSE" | cut -d: -f2)
        
        echo "Wallet Query - HTTP: $HTTP_CODE, Time: ${RESPONSE_TIME}s"
        sleep 0.5
    done < "$WALLET_FILE"
}

# Function to test deposits
test_deposits() {
    echo "💰 Testing Deposits..."
    
    WALLET_FILE="${RESULTS_DIR}/wallets_${TIMESTAMP}.txt"
    
    if [ ! -f "$WALLET_FILE" ]; then
        echo "❌ No wallets found. Run wallet creation test first."
        return 1
    fi
    
    COUNTER=1
    while IFS= read -r WALLET_ID; do
        AMOUNT="100.$(printf "%02d" $COUNTER)"
        REF_ID="load-test-deposit-${TIMESTAMP}-${COUNTER}"
        
        RESPONSE=$(curl -s -w "%{http_code}:%{time_total}" \
            -X POST "$BASE_URL/api/v1/wallets/$WALLET_ID/deposit" \
            -H "Content-Type: application/json" \
            -d "{\"amount\": $AMOUNT, \"referenceId\": \"$REF_ID\", \"description\": \"Load test deposit\"}")
        
        HTTP_CODE=$(echo "$RESPONSE" | cut -d: -f1)
        RESPONSE_TIME=$(echo "$RESPONSE" | cut -d: -f2)
        
        echo "Deposit - HTTP: $HTTP_CODE, Time: ${RESPONSE_TIME}s, Amount: $AMOUNT"
        
        COUNTER=$((COUNTER + 1))
        sleep 1
    done < "$WALLET_FILE"
}

# Function to run baseline test
run_baseline_test() {
    echo "📊 Running Baseline Performance Test..."
    
    # Start monitoring in background
    echo "🔍 Starting metrics monitoring..."
    ./docs/performance/scripts/monitor-metrics.sh $DURATION > "${RESULTS_DIR}/monitoring_${TIMESTAMP}.log" 2>&1 &
    MONITOR_PID=$!
    
    # Run tests
    test_wallet_creation
    sleep 5
    test_wallet_queries  
    sleep 5
    test_deposits
    
    # Wait for monitoring to complete
    echo "⏳ Waiting for monitoring to complete..."
    wait $MONITOR_PID
    
    echo "✅ Baseline test completed!"
}

# Function to run continuous load test
run_load_test() {
    echo "🔥 Running Continuous Load Test for ${DURATION} seconds..."
    
    # Start monitoring in background
    ./docs/performance/scripts/monitor-metrics.sh $DURATION > "${RESULTS_DIR}/monitoring_${TIMESTAMP}.log" 2>&1 &
    MONITOR_PID=$!
    
    END_TIME=$(($(date +%s) + DURATION))
    COUNTER=1
    
    while [ $(date +%s) -lt $END_TIME ]; do
        # Create wallet
        USER_ID="load-test-${TIMESTAMP}-${COUNTER}"
        WALLET_RESPONSE=$(curl -s -w "%{http_code}" \
            -X POST "$BASE_URL/api/v1/wallets" \
            -H "Content-Type: application/json" \
            -d "{\"userId\": \"$USER_ID\"}")
        
        echo "[$COUNTER] Wallet Creation: $(echo "$WALLET_RESPONSE" | tail -c 4)"
        
        # Quick deposit if wallet created successfully
        if [[ "$WALLET_RESPONSE" == *"201" ]]; then
            WALLET_ID=$(echo "$WALLET_RESPONSE" | grep -o '/wallets/[^"]*' | sed 's|/wallets/||')
            if [ ! -z "$WALLET_ID" ]; then
                DEPOSIT_RESPONSE=$(curl -s -w "%{http_code}" \
                    -X POST "$BASE_URL/api/v1/wallets/$WALLET_ID/deposit" \
                    -H "Content-Type: application/json" \
                    -d "{\"amount\": 50.00, \"referenceId\": \"load-${COUNTER}\", \"description\": \"Load test\"}")
                
                echo "[$COUNTER] Deposit: $(echo "$DEPOSIT_RESPONSE" | tail -c 4)"
            fi
        fi
        
        COUNTER=$((COUNTER + 1))
        sleep 2
    done
    
    # Wait for monitoring to complete
    wait $MONITOR_PID
    
    echo "✅ Load test completed! Processed $COUNTER operations."
}

# Check if application is running
echo "🔍 Checking application health..."
HEALTH_CHECK=$(curl -s -w "%{http_code}" "$BASE_URL/q/health" || echo "000")

if [[ "$HEALTH_CHECK" != *"200" ]]; then
    echo "❌ Application is not running or not healthy!"
    echo "   Please start the application first: ./mvnw quarkus:dev"
    exit 1
fi

echo "✅ Application is healthy!"

# Run the specified test
case $TEST_TYPE in
    "baseline")
        run_baseline_test
        ;;
    "load")
        run_load_test
        ;;
    "create")
        test_wallet_creation
        ;;
    "query")
        test_wallet_queries
        ;;
    "deposit")
        test_deposits
        ;;
    *)
        echo "❌ Unknown test type: $TEST_TYPE"
        echo "Available types: baseline, load, create, query, deposit"
        exit 1
        ;;
esac

echo ""
echo "📊 Test Results Summary"
echo "======================"
echo "Test Type: $TEST_TYPE"
echo "Duration: ${DURATION}s"
echo "Timestamp: $TIMESTAMP"
echo "Results Directory: $RESULTS_DIR"
echo ""
echo "📁 Generated Files:"
ls -la "${RESULTS_DIR}/"*"${TIMESTAMP}"* 2>/dev/null || echo "No result files generated"
echo ""
echo "🔍 To view Prometheus metrics:"
echo "curl 'http://localhost:9090/api/v1/query?query=rate(wallet_operations_deposits_total[5m])'"
