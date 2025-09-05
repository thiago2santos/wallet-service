// EXTREME LOAD TEST - Find the TRUE breaking point
// This test will aggressively push the system until it actually breaks
// Run with: k6 run performance/scripts/k6/extreme-load-test.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('wallet_errors');
const responseTime = new Trend('wallet_response_time');
const throughput = new Counter('wallet_operations_total');
const successfulOps = new Counter('successful_operations');

export const options = {
  // MUCH MORE AGGRESSIVE scaling - start high and go higher
  stages: [
    // Phase 1: Quick ramp to moderate load
    { duration: '30s', target: 50 },     // Quick start
    { duration: '1m', target: 100 },     // Moderate load
    
    // Phase 2: Aggressive scaling
    { duration: '1m', target: 250 },     // Heavy load
    { duration: '1m', target: 500 },     // Very heavy load
    { duration: '2m', target: 750 },     // Extreme load
    
    // Phase 3: Breaking point attempts
    { duration: '1m', target: 1000 },    // Should show stress
    { duration: '2m', target: 1500 },    // Likely breaking point
    { duration: '1m', target: 2000 },    // Almost certainly broken
    
    // Phase 4: Nuclear option
    { duration: '1m', target: 3000 },    // System should be struggling
    { duration: '1m', target: 5000 },    // Nuclear load
    
    // Cooldown
    { duration: '30s', target: 0 },      // Ramp down quickly
  ],
  
  // More realistic thresholds that will actually detect problems
  thresholds: {
    http_req_duration: ['p(95)<1000'],    // 95% under 1 second
    http_req_failed: ['rate<0.10'],       // Less than 10% failures
    wallet_errors: ['rate<0.05'],         // Less than 5% custom errors
  },
  
  // Optimize for maximum throughput
  noConnectionReuse: false,  // Reuse connections for better performance
  userAgent: 'WalletExtremeLoadTest/1.0',
  
  // Don't abort on threshold failures - we want to see the breaking point
  noThresholds: false,
};

const BASE_URL = 'http://localhost:8080/api/v1';
let testWallets = [];

export function setup() {
  console.log('🚀 EXTREME LOAD TEST - Finding the TRUE breaking point');
  console.log('📊 Monitor at: http://localhost:3000/d/wallet-golden-metrics');
  console.log('⚠️  This test WILL push the system to its limits!');
  
  // Create test wallets for operations
  const wallets = [];
  console.log('Creating test wallets for load testing...');
  
  for (let i = 1; i <= 100; i++) {
    try {
      const response = http.post(`${BASE_URL}/wallets`, JSON.stringify({
        userId: `extreme-load-user-${i}-${Date.now()}`
      }), {
        headers: { 'Content-Type': 'application/json' },
        timeout: '10s',
      });
      
      if (response.status === 201) {
        // Extract wallet ID from Location header
        const locationHeader = response.headers['Location'] || response.headers['location'];
        if (locationHeader) {
          const walletId = locationHeader.split('/').pop();
          wallets.push(walletId);
          
          // Add substantial balance for testing
          const depositResponse = http.post(`${BASE_URL}/wallets/${walletId}/deposit`, JSON.stringify({
            amount: 10000.00,  // $10,000 per wallet
            referenceId: `setup-deposit-${walletId}-${Date.now()}`
          }), {
            headers: { 'Content-Type': 'application/json' },
            timeout: '10s',
          });
          
          if (depositResponse.status !== 200) {
            console.log(`Failed to deposit to wallet ${walletId}: ${depositResponse.status}`);
          }
        } else {
          console.log(`Wallet created but no Location header found`);
        }
      } else {
        console.log(`Failed to create wallet ${i}: HTTP ${response.status} - ${response.body}`);
      }
    } catch (error) {
      console.log(`Failed to create wallet ${i}: ${error}`);
    }
    
    if (i % 20 === 0) {
      console.log(`Created ${i}/100 wallets...`);
    }
  }
  
  console.log(`✅ Setup complete. Created ${wallets.length} wallets for extreme load testing.`);
  return { wallets: wallets };
}

export default function(data) {
  const wallets = data.wallets || [];
  
  if (wallets.length === 0) {
    console.log('❌ No wallets available for testing');
    return;
  }
  
  // Weighted operation selection - more realistic mix
  const operations = [
    { name: 'query_balance', weight: 50 },      // 50% - most common
    { name: 'deposit', weight: 20 },            // 20%
    { name: 'transfer', weight: 15 },           // 15%
    { name: 'withdrawal', weight: 10 },         // 10%
    { name: 'create_wallet', weight: 5 },       // 5%
  ];
  
  const selectedOp = selectWeightedOperation(operations);
  const startTime = Date.now();
  let response;
  let success = false;
  
  try {
    switch (selectedOp) {
      case 'query_balance':
        response = queryBalance(wallets);
        break;
      case 'deposit':
        response = makeDeposit(wallets);
        break;
      case 'transfer':
        response = makeTransfer(wallets);
        break;
      case 'withdrawal':
        response = makeWithdrawal(wallets);
        break;
      case 'create_wallet':
        response = createWallet();
        break;
    }
    
    success = response && response.status >= 200 && response.status < 400;
    
    if (success) {
      successfulOps.add(1);
    }
    
  } catch (error) {
    console.log(`❌ Operation ${selectedOp} failed: ${error}`);
    errorRate.add(1);
  }
  
  const duration = Date.now() - startTime;
  responseTime.add(duration);
  throughput.add(1);
  
  if (!success) {
    errorRate.add(1);
  } else {
    errorRate.add(0);
  }
  
  // MINIMAL sleep for maximum throughput
  sleep(0.01); // Only 10ms sleep - much more aggressive!
}

function selectWeightedOperation(operations) {
  const totalWeight = operations.reduce((sum, op) => sum + op.weight, 0);
  const random = Math.random() * totalWeight;
  let currentWeight = 0;
  
  for (const op of operations) {
    currentWeight += op.weight;
    if (random <= currentWeight) {
      return op.name;
    }
  }
  return operations[0].name;
}

function queryBalance(wallets) {
  const walletId = wallets[Math.floor(Math.random() * wallets.length)];
  const response = http.get(`${BASE_URL}/wallets/${walletId}`, {
    timeout: '3s', // Shorter timeout
  });
  
  check(response, {
    'balance query success': (r) => r.status === 200,
    'balance query fast': (r) => r.timings.duration < 1000,
  });
  
  return response;
}

function makeDeposit(wallets) {
  const walletId = wallets[Math.floor(Math.random() * wallets.length)];
  const amount = Math.random() * 500 + 1; // $1-$500
  
  const response = http.post(`${BASE_URL}/wallets/${walletId}/deposit`, JSON.stringify({
    amount: amount,
    referenceId: `load-deposit-${Math.random().toString(36).substring(7)}-${Date.now()}`
  }), {
    headers: { 'Content-Type': 'application/json' },
    timeout: '3s',
  });
  
  check(response, {
    'deposit success': (r) => r.status === 200,
    'deposit fast': (r) => r.timings.duration < 1000,
  });
  
  return response;
}

function makeWithdrawal(wallets) {
  const walletId = wallets[Math.floor(Math.random() * wallets.length)];
  const amount = Math.random() * 100 + 1; // $1-$100
  
  const response = http.post(`${BASE_URL}/wallets/${walletId}/withdraw`, JSON.stringify({
    amount: amount,
    referenceId: `load-withdraw-${Math.random().toString(36).substring(7)}-${Date.now()}`
  }), {
    headers: { 'Content-Type': 'application/json' },
    timeout: '3s',
  });
  
  check(response, {
    'withdrawal processed': (r) => r.status === 200 || r.status === 400,
    'withdrawal fast': (r) => r.timings.duration < 1000,
  });
  
  return response;
}

function makeTransfer(wallets) {
  if (wallets.length < 2) return null;
  
  const fromWallet = wallets[Math.floor(Math.random() * wallets.length)];
  let toWallet = wallets[Math.floor(Math.random() * wallets.length)];
  
  while (toWallet === fromWallet && wallets.length > 1) {
    toWallet = wallets[Math.floor(Math.random() * wallets.length)];
  }
  
  const amount = Math.random() * 50 + 1; // $1-$50
  
  const response = http.post(`${BASE_URL}/wallets/${fromWallet}/transfer`, JSON.stringify({
    toWalletId: toWallet,
    amount: amount,
    referenceId: `load-transfer-${Math.random().toString(36).substring(7)}-${Date.now()}`
  }), {
    headers: { 'Content-Type': 'application/json' },
    timeout: '3s',
  });
  
  check(response, {
    'transfer processed': (r) => r.status === 200 || r.status === 400,
    'transfer fast': (r) => r.timings.duration < 1000,
  });
  
  return response;
}

function createWallet() {
  const userId = `extreme-load-${Math.random().toString(36).substring(7)}-${Date.now()}`;
  
  const response = http.post(`${BASE_URL}/wallets`, JSON.stringify({
    userId: userId
  }), {
    headers: { 'Content-Type': 'application/json' },
    timeout: '3s',
  });
  
  check(response, {
    'wallet creation success': (r) => r.status === 201,
    'wallet creation fast': (r) => r.timings.duration < 1000,
  });
  
  // Store new wallet for future operations
  if (response.status === 201) {
    try {
      const locationHeader = response.headers['Location'] || response.headers['location'];
      if (locationHeader) {
        const walletId = locationHeader.split('/').pop();
        testWallets.push(walletId);
      }
    } catch (e) {
      // Ignore parsing errors during high load
    }
  }
  
  return response;
}

export function teardown(data) {
  console.log('🏁 EXTREME LOAD TEST COMPLETED!');
  console.log('📊 Check the Golden Metrics dashboard for results');
  console.log('💥 If the system survived this, it\'s truly robust!');
  console.log('📈 Review response times, error rates, and throughput metrics');
}
