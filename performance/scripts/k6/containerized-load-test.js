// CONTAINERIZED LOAD TEST - Test against Docker deployment
// This test targets the containerized wallet service for realistic performance testing
// Run with: k6 run performance/scripts/k6/containerized-load-test.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('wallet_errors');
const responseTime = new Trend('wallet_response_time');
const throughput = new Counter('wallet_operations_total');
const successfulOps = new Counter('successful_operations');

// Configuration for containerized deployment
const BASE_URL = 'http://localhost:8080/api/v1';

export const options = {
  // Progressive load testing against containerized service
  stages: [
    // Phase 1: Warm-up
    { duration: '2m', target: 50 },      // Warm up containers
    { duration: '3m', target: 100 },     // Light load
    
    // Phase 2: Moderate load
    { duration: '3m', target: 250 },     // Moderate load
    { duration: '5m', target: 500 },     // Sustained moderate load
    
    // Phase 3: High load
    { duration: '3m', target: 1000 },    // High load
    { duration: '5m', target: 1500 },    // Sustained high load
    
    // Phase 4: Extreme load
    { duration: '2m', target: 2000 },    // Very high load
    { duration: '3m', target: 2500 },    // Extreme load
    { duration: '2m', target: 3000 },    // Peak load
    
    // Phase 5: Breaking point
    { duration: '2m', target: 4000 },    // Push to breaking point
    { duration: '2m', target: 5000 },    // Maximum load
    
    // Phase 6: Cool down
    { duration: '2m', target: 1000 },    // Cool down
    { duration: '2m', target: 0 },       // Complete stop
  ],
  
  // Thresholds for containerized environment
  thresholds: {
    'http_req_duration': [
      'p(95)<2000',  // 95% of requests under 2s (more lenient for containers)
      'p(99)<5000',  // 99% of requests under 5s
    ],
    'http_req_failed': ['rate<0.15'],  // Less than 15% errors (containers may have more overhead)
    'wallet_errors': ['rate<0.15'],
    'checks': ['rate>0.80'],  // 80% of checks should pass
  },
  
  // Extended timeouts for containerized environment
  httpDebug: 'full',
  insecureSkipTLSVerify: true,
  noConnectionReuse: false,
  userAgent: 'K6-LoadTest-Containerized/1.0',
};

// Global variables for test data
let testWallets = [];

export function setup() {
  console.log('🐳 CONTAINERIZED LOAD TEST SETUP');
  console.log('==================================');
  console.log('Target: ' + BASE_URL);
  console.log('');
  
  // Wait for container to be fully ready
  console.log('⏳ Waiting for containerized service to be ready...');
  let retries = 0;
  const maxRetries = 30;
  
  while (retries < maxRetries) {
    try {
      const healthCheck = http.get(`${BASE_URL.replace('/api/v1', '')}/q/health`, {
        timeout: '10s',
      });
      
      if (healthCheck.status === 200) {
        console.log('✅ Container health check passed');
        break;
      }
    } catch (e) {
      console.log(`⏳ Waiting for container... (${retries + 1}/${maxRetries})`);
    }
    
    retries++;
    if (retries >= maxRetries) {
      throw new Error('❌ Container failed to become ready within timeout');
    }
    
    sleep(2);
  }
  
  console.log('🏗️  Creating initial wallets for containerized testing...');
  const wallets = [];
  
  // Create fewer initial wallets for container testing
  for (let i = 1; i <= 20; i++) {
    try {
      const response = http.post(`${BASE_URL}/wallets`, JSON.stringify({
        userId: `container-user-${i}-${Date.now()}`
      }), {
        headers: { 'Content-Type': 'application/json' },
        timeout: '15s',  // Longer timeout for containers
      });
      
      if (response.status === 201) {
        // Extract wallet ID from Location header
        const locationHeader = response.headers['Location'] || response.headers['location'];
        if (locationHeader) {
          const walletId = locationHeader.split('/').pop();
          wallets.push(walletId);
          
          // Add substantial balance for testing
          const depositResponse = http.post(`${BASE_URL}/wallets/${walletId}/deposit`, JSON.stringify({
            amount: 5000.00,  // $5,000 per wallet
            referenceId: `container-setup-deposit-${walletId}-${Date.now()}`
          }), {
            headers: { 'Content-Type': 'application/json' },
            timeout: '15s',
          });
          
          if (depositResponse.status === 200) {
            console.log(`✅ Wallet ${i}/20 created and funded: ${walletId}`);
          } else {
            console.log(`⚠️  Wallet ${i} created but funding failed: HTTP ${depositResponse.status}`);
          }
        }
      } else {
        console.log(`❌ Failed to create wallet ${i}: HTTP ${response.status}`);
      }
    } catch (error) {
      console.log(`❌ Error creating wallet ${i}: ${error.message}`);
    }
    
    // Small delay between wallet creations for container stability
    sleep(0.5);
  }
  
  if (wallets.length === 0) {
    throw new Error('❌ No wallets available for containerized testing');
  }
  
  console.log(`✅ Setup complete: ${wallets.length} wallets ready for containerized load testing`);
  console.log('🚀 Starting containerized load test...');
  console.log('');
  
  return { wallets: wallets };
}

export default function(data) {
  const wallets = data.wallets;
  
  if (!wallets || wallets.length === 0) {
    console.log('❌ No wallets available for testing');
    return;
  }
  
  // Weighted operation distribution for realistic load
  const operations = [
    { name: 'balance', weight: 40 },    // 40% - Most common operation
    { name: 'deposit', weight: 25 },    // 25% - Frequent
    { name: 'withdraw', weight: 20 },   // 20% - Common
    { name: 'transfer', weight: 10 },   // 10% - Less frequent
    { name: 'create', weight: 5 },      // 5% - Least frequent
  ];
  
  // Select operation based on weights
  const rand = Math.random() * 100;
  let cumulative = 0;
  let selectedOp = 'balance';
  
  for (const op of operations) {
    cumulative += op.weight;
    if (rand <= cumulative) {
      selectedOp = op.name;
      break;
    }
  }
  
  // Execute selected operation
  let response;
  switch (selectedOp) {
    case 'balance':
      response = checkBalance(wallets);
      break;
    case 'deposit':
      response = makeDeposit(wallets);
      break;
    case 'withdraw':
      response = makeWithdrawal(wallets);
      break;
    case 'transfer':
      response = makeTransfer(wallets);
      break;
    case 'create':
      response = createWallet();
      break;
    default:
      response = checkBalance(wallets);
  }
  
  // Record metrics
  if (response) {
    throughput.add(1);
    responseTime.add(response.timings.duration);
    
    const success = response.status >= 200 && response.status < 400;
    if (success) {
      successfulOps.add(1);
    }
    errorRate.add(!success);
  }
  
  // Shorter sleep for containerized testing (containers handle concurrency better)
  sleep(Math.random() * 0.5 + 0.1); // 0.1-0.6 seconds
}

function checkBalance(wallets) {
  const walletId = wallets[Math.floor(Math.random() * wallets.length)];
  
  const response = http.get(`${BASE_URL}/wallets/${walletId}`, {
    timeout: '5s',
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
    referenceId: `container-deposit-${Math.random().toString(36).substring(7)}-${Date.now()}`
  }), {
    headers: { 'Content-Type': 'application/json' },
    timeout: '5s',
  });
  
  check(response, {
    'deposit success': (r) => r.status === 200,
    'deposit fast': (r) => r.timings.duration < 1500,
  });
  
  return response;
}

function makeWithdrawal(wallets) {
  const walletId = wallets[Math.floor(Math.random() * wallets.length)];
  const amount = Math.random() * 100 + 1; // $1-$100
  
  const response = http.post(`${BASE_URL}/wallets/${walletId}/withdraw`, JSON.stringify({
    amount: amount,
    referenceId: `container-withdraw-${Math.random().toString(36).substring(7)}-${Date.now()}`
  }), {
    headers: { 'Content-Type': 'application/json' },
    timeout: '5s',
  });
  
  check(response, {
    'withdrawal processed': (r) => r.status === 200 || r.status === 400, // 400 for insufficient funds is OK
    'withdrawal fast': (r) => r.timings.duration < 1500,
  });
  
  return response;
}

function makeTransfer(wallets) {
  if (wallets.length < 2) return checkBalance(wallets);
  
  const fromWallet = wallets[Math.floor(Math.random() * wallets.length)];
  let toWallet = wallets[Math.floor(Math.random() * wallets.length)];
  
  // Ensure different wallets
  while (toWallet === fromWallet && wallets.length > 1) {
    toWallet = wallets[Math.floor(Math.random() * wallets.length)];
  }
  
  const amount = Math.random() * 50 + 1; // $1-$50
  
  const response = http.post(`${BASE_URL}/wallets/${fromWallet}/transfer`, JSON.stringify({
    toWalletId: toWallet,
    amount: amount,
    referenceId: `container-transfer-${Math.random().toString(36).substring(7)}-${Date.now()}`
  }), {
    headers: { 'Content-Type': 'application/json' },
    timeout: '5s',
  });
  
  check(response, {
    'transfer processed': (r) => r.status === 200 || r.status === 400, // 400 for insufficient funds is OK
    'transfer fast': (r) => r.timings.duration < 2000,
  });
  
  return response;
}

function createWallet() {
  const response = http.post(`${BASE_URL}/wallets`, JSON.stringify({
    userId: `container-load-user-${Math.random().toString(36).substring(7)}-${Date.now()}`
  }), {
    headers: { 'Content-Type': 'application/json' },
    timeout: '5s',
  });
  
  check(response, {
    'wallet creation success': (r) => r.status === 201,
    'wallet creation fast': (r) => r.timings.duration < 1500,
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
  console.log('');
  console.log('🏁 CONTAINERIZED LOAD TEST COMPLETED!');
  console.log('📊 Check the Grafana dashboard for detailed container metrics');
  console.log('🐳 Container performance analysis complete');
  console.log('📈 Review response times, error rates, and resource utilization');
  console.log('');
}
