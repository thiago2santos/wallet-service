// Extreme Stress Test - Load until system failure
// This test progressively increases load until the system breaks
// Run with: k6 run scripts/stress-test-to-failure.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics for detailed analysis
const errorRate = new Rate('wallet_errors');
const responseTime = new Trend('wallet_response_time');
const throughput = new Counter('wallet_operations_total');
const failuresByType = new Counter('failures_by_type');

export const options = {
  // Aggressive scaling stages - ramp up until failure
  stages: [
    // Phase 1: Baseline (should work fine)
    { duration: '1m', target: 10 },    // Warm up
    { duration: '2m', target: 25 },    // Light load
    
    // Phase 2: Moderate stress
    { duration: '2m', target: 50 },    // Moderate load
    { duration: '3m', target: 100 },   // Heavy load
    
    // Phase 3: High stress
    { duration: '2m', target: 200 },   // Very heavy load
    { duration: '3m', target: 300 },   // Extreme load
    
    // Phase 4: Breaking point attempts
    { duration: '2m', target: 500 },   // Should start showing issues
    { duration: '3m', target: 750 },   // Likely breaking point
    { duration: '2m', target: 1000 },  // Almost certainly broken
    
    // Phase 5: Nuclear option
    { duration: '2m', target: 1500 },  // System should be dead
    { duration: '1m', target: 2000 },  // Just to be sure
    
    // Cooldown (if system survives)
    { duration: '2m', target: 0 },     // Ramp down
  ],
  
  // Relaxed thresholds - we expect failures
  thresholds: {
    // We expect high response times and errors at breaking point
    http_req_duration: ['p(50)<5000', 'p(95)<10000'], // Very relaxed
    http_req_failed: ['rate<0.95'],    // Allow up to 95% failures
    wallet_errors: ['rate<0.95'],      // Track our custom errors
  },
  
  // Don't abort on threshold failures - we want to see the crash
  noConnectionReuse: true,
  userAgent: 'WalletStressTest/1.0',
};

const BASE_URL = 'http://localhost:8080/api/v1';
let createdWallets = [];

export function setup() {
  console.log('🔥 EXTREME STRESS TEST - Loading until system failure');
  console.log('📊 Monitor Golden Metrics at: http://localhost:3000/d/wallet-golden-metrics');
  console.log('⚠️  This test WILL attempt to crash the system!');
  
  // Create initial test wallets
  const wallets = [];
  console.log('Creating initial test wallets...');
  
  for (let i = 1; i <= 50; i++) {
    try {
      const response = http.post(`${BASE_URL}/wallets`, JSON.stringify({
        userId: `stress-user-${i}`
      }), {
        headers: { 'Content-Type': 'application/json' },
        timeout: '10s',
      });
      
      if (response.status === 201) {
        const wallet = JSON.parse(response.body);
        wallets.push(wallet.id);
        
        // Add some initial balance for testing
        http.post(`${BASE_URL}/wallets/${wallet.id}/deposit`, JSON.stringify({
          amount: 1000.00
        }), {
          headers: { 'Content-Type': 'application/json' },
          timeout: '10s',
        });
      }
    } catch (error) {
      console.log(`Failed to create wallet ${i}: ${error}`);
    }
    
    if (i % 10 === 0) {
      console.log(`Created ${i}/50 wallets...`);
    }
  }
  
  console.log(`✅ Setup complete. Created ${wallets.length} wallets for stress testing.`);
  return { wallets: wallets };
}

export default function(data) {
  const wallets = data.wallets || [];
  
  if (wallets.length === 0) {
    console.log('❌ No wallets available for testing');
    return;
  }
  
  // Random operation selection with weights
  const operations = [
    { name: 'query_balance', weight: 40 },      // Most common - 40%
    { name: 'deposit', weight: 25 },            // 25%
    { name: 'transfer', weight: 20 },           // 20%
    { name: 'withdrawal', weight: 10 },         // 10%
    { name: 'create_wallet', weight: 5 },       // 5%
  ];
  
  const totalWeight = operations.reduce((sum, op) => sum + op.weight, 0);
  const random = Math.random() * totalWeight;
  let currentWeight = 0;
  let selectedOperation = operations[0];
  
  for (const op of operations) {
    currentWeight += op.weight;
    if (random <= currentWeight) {
      selectedOperation = op;
      break;
    }
  }
  
  const startTime = Date.now();
  let response;
  let success = false;
  
  try {
    switch (selectedOperation.name) {
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
    
  } catch (error) {
    console.log(`Operation ${selectedOperation.name} failed: ${error}`);
    failuresByType.add(1, { operation: selectedOperation.name, error: 'exception' });
  }
  
  const duration = Date.now() - startTime;
  responseTime.add(duration);
  throughput.add(1);
  
  if (!success) {
    errorRate.add(1);
    if (response) {
      failuresByType.add(1, { 
        operation: selectedOperation.name, 
        status: response.status,
        error: 'http_error'
      });
    }
  } else {
    errorRate.add(0);
  }
  
  // Very short sleep to maximize load
  sleep(Math.random() * 0.1); // 0-100ms random sleep
}

function queryBalance(wallets) {
  const walletId = wallets[Math.floor(Math.random() * wallets.length)];
  const response = http.get(`${BASE_URL}/wallets/${walletId}`, {
    timeout: '5s',
  });
  
  check(response, {
    'balance query status ok': (r) => r.status === 200,
  });
  
  return response;
}

function makeDeposit(wallets) {
  const walletId = wallets[Math.floor(Math.random() * wallets.length)];
  const amount = Math.random() * 100 + 1; // $1-$100
  
  const response = http.post(`${BASE_URL}/wallets/${walletId}/deposit`, JSON.stringify({
    amount: amount
  }), {
    headers: { 'Content-Type': 'application/json' },
    timeout: '5s',
  });
  
  check(response, {
    'deposit status ok': (r) => r.status === 200,
  });
  
  return response;
}

function makeWithdrawal(wallets) {
  const walletId = wallets[Math.floor(Math.random() * wallets.length)];
  const amount = Math.random() * 50 + 1; // $1-$50 (smaller to avoid insufficient funds)
  
  const response = http.post(`${BASE_URL}/wallets/${walletId}/withdraw`, JSON.stringify({
    amount: amount
  }), {
    headers: { 'Content-Type': 'application/json' },
    timeout: '5s',
  });
  
  check(response, {
    'withdrawal status ok': (r) => r.status === 200 || r.status === 400, // 400 = insufficient funds is ok
  });
  
  return response;
}

function makeTransfer(wallets) {
  if (wallets.length < 2) return null;
  
  const fromWallet = wallets[Math.floor(Math.random() * wallets.length)];
  let toWallet = wallets[Math.floor(Math.random() * wallets.length)];
  
  // Ensure different wallets
  while (toWallet === fromWallet && wallets.length > 1) {
    toWallet = wallets[Math.floor(Math.random() * wallets.length)];
  }
  
  const amount = Math.random() * 25 + 1; // $1-$25
  
  const response = http.post(`${BASE_URL}/wallets/${fromWallet}/transfer`, JSON.stringify({
    toWalletId: toWallet,
    amount: amount
  }), {
    headers: { 'Content-Type': 'application/json' },
    timeout: '5s',
  });
  
  check(response, {
    'transfer status ok': (r) => r.status === 200 || r.status === 400, // 400 = insufficient funds is ok
  });
  
  return response;
}

function createWallet() {
  const userId = `stress-runtime-${Math.random().toString(36).substring(7)}`;
  
  const response = http.post(`${BASE_URL}/wallets`, JSON.stringify({
    userId: userId
  }), {
    headers: { 'Content-Type': 'application/json' },
    timeout: '5s',
  });
  
  check(response, {
    'wallet creation status ok': (r) => r.status === 201,
  });
  
  return response;
}

export function teardown(data) {
  console.log('🏁 Stress test completed!');
  console.log('📊 Check Golden Metrics dashboard for the carnage: http://localhost:3000/d/wallet-golden-metrics');
  console.log('💀 If the system survived, increase the load even more next time!');
}
