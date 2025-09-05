// Continuous Wallet Stress Test
// Creates wallets every 2 seconds and immediately uses them for transactions
// Scales up until system breaks

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
const walletCreationRate = new Rate('wallet_creation_success');
const transactionRate = new Rate('transaction_success');
const systemThroughput = new Counter('total_operations');
const responseTime = new Trend('operation_response_time');

export const options = {
  // Start small and scale up every 2 minutes
  stages: [
    { duration: '2m', target: 5 },    // 5 users creating wallets
    { duration: '2m', target: 10 },   // 10 users
    { duration: '2m', target: 20 },   // 20 users
    { duration: '2m', target: 35 },   // 35 users
    { duration: '2m', target: 50 },   // 50 users
    { duration: '2m', target: 75 },   // 75 users
    { duration: '2m', target: 100 },  // 100 users - should start showing stress
    { duration: '2m', target: 150 },  // 150 users - heavy load
    { duration: '2m', target: 200 },  // 200 users - very heavy
    { duration: '2m', target: 300 },  // 300 users - breaking point?
    { duration: '2m', target: 500 },  // 500 users - system should break
    { duration: '1m', target: 0 },    // Ramp down
  ],
  
  thresholds: {
    // Relaxed thresholds - we expect some failures at high load
    http_req_duration: ['p(95)<2000'],  // 2 second P95
    http_req_failed: ['rate<0.50'],     // Allow 50% failures
    wallet_creation_success: ['rate>0.30'], // At least 30% wallet creation success
  },
};

const BASE_URL = 'http://localhost:8080/api/v1';

// Shared wallet pool - thread-safe in k6
let walletPool = [];
let walletCounter = 0;

export default function() {
  const userId = `stress-${__VU}-${Date.now()}-${Math.random().toString(36).substring(7)}`;
  
  // Step 1: Create a new wallet every iteration
  const newWallet = createWallet(userId);
  
  if (newWallet) {
    // Add to pool for other operations
    walletPool.push(newWallet);
    
    // Step 2: Immediately deposit some money
    depositMoney(newWallet, 1000);
    
    // Step 3: If we have multiple wallets, do some transfers
    if (walletPool.length > 1) {
      performRandomTransaction();
    }
  }
  
  // Sleep for 2 seconds as requested
  sleep(2);
}

function createWallet(userId) {
  const startTime = Date.now();
  
  try {
    const response = http.post(`${BASE_URL}/wallets`, JSON.stringify({
      userId: userId
    }), {
      headers: { 'Content-Type': 'application/json' },
      timeout: '10s',
    });
    
    const duration = Date.now() - startTime;
    responseTime.add(duration);
    systemThroughput.add(1);
    
    const success = check(response, {
      'wallet creation status is 201': (r) => r.status === 201,
    });
    
    walletCreationRate.add(success ? 1 : 0);
    
    if (success) {
      // Extract wallet ID from Location header
      const locationHeader = response.headers['Location'] || response.headers['location'];
      if (locationHeader) {
        const walletId = locationHeader.split('/').pop();
        const wallet = { id: walletId, userId: userId };
        console.log(`✅ Created wallet ${++walletCounter}: ${wallet.id} (${duration}ms)`);
        return wallet;
      } else {
        console.log(`❌ No Location header in response (${duration}ms)`);
        return null;
      }
    } else {
      console.log(`❌ Failed to create wallet: HTTP ${response.status} (${duration}ms)`);
      return null;
    }
    
  } catch (error) {
    const duration = Date.now() - startTime;
    responseTime.add(duration);
    systemThroughput.add(1);
    walletCreationRate.add(0);
    console.log(`💥 Wallet creation exception: ${error} (${duration}ms)`);
    return null;
  }
}

function depositMoney(wallet, amount) {
  const startTime = Date.now();
  
  try {
    const response = http.post(`${BASE_URL}/wallets/${wallet.id}/deposit`, JSON.stringify({
      amount: amount
    }), {
      headers: { 'Content-Type': 'application/json' },
      timeout: '10s',
    });
    
    const duration = Date.now() - startTime;
    responseTime.add(duration);
    systemThroughput.add(1);
    
    const success = check(response, {
      'deposit status is 200': (r) => r.status === 200,
    });
    
    transactionRate.add(success ? 1 : 0);
    
    if (success) {
      console.log(`💰 Deposited $${amount} to ${wallet.id} (${duration}ms)`);
    } else {
      console.log(`❌ Deposit failed: HTTP ${response.status} (${duration}ms)`);
    }
    
    return success;
    
  } catch (error) {
    const duration = Date.now() - startTime;
    responseTime.add(duration);
    systemThroughput.add(1);
    transactionRate.add(0);
    console.log(`💥 Deposit exception: ${error} (${duration}ms)`);
    return false;
  }
}

function performRandomTransaction() {
  if (walletPool.length < 2) return;
  
  const operations = ['withdraw', 'transfer', 'query'];
  const operation = operations[Math.floor(Math.random() * operations.length)];
  
  switch (operation) {
    case 'withdraw':
      performWithdrawal();
      break;
    case 'transfer':
      performTransfer();
      break;
    case 'query':
      performQuery();
      break;
  }
}

function performWithdrawal() {
  const wallet = walletPool[Math.floor(Math.random() * walletPool.length)];
  const amount = Math.random() * 100 + 10; // $10-$110
  const startTime = Date.now();
  
  try {
    const response = http.post(`${BASE_URL}/wallets/${wallet.id}/withdraw`, JSON.stringify({
      amount: amount
    }), {
      headers: { 'Content-Type': 'application/json' },
      timeout: '10s',
    });
    
    const duration = Date.now() - startTime;
    responseTime.add(duration);
    systemThroughput.add(1);
    
    const success = check(response, {
      'withdrawal status is 200 or 400': (r) => r.status === 200 || r.status === 400,
    });
    
    transactionRate.add(success ? 1 : 0);
    
    if (response.status === 200) {
      console.log(`💸 Withdrew $${amount.toFixed(2)} from ${wallet.id} (${duration}ms)`);
    } else if (response.status === 400) {
      console.log(`⚠️ Insufficient funds for withdrawal from ${wallet.id} (${duration}ms)`);
    } else {
      console.log(`❌ Withdrawal failed: HTTP ${response.status} (${duration}ms)`);
    }
    
  } catch (error) {
    const duration = Date.now() - startTime;
    responseTime.add(duration);
    systemThroughput.add(1);
    transactionRate.add(0);
    console.log(`💥 Withdrawal exception: ${error} (${duration}ms)`);
  }
}

function performTransfer() {
  if (walletPool.length < 2) return;
  
  const fromWallet = walletPool[Math.floor(Math.random() * walletPool.length)];
  let toWallet = walletPool[Math.floor(Math.random() * walletPool.length)];
  
  // Ensure different wallets
  while (toWallet.id === fromWallet.id && walletPool.length > 1) {
    toWallet = walletPool[Math.floor(Math.random() * walletPool.length)];
  }
  
  const amount = Math.random() * 50 + 5; // $5-$55
  const startTime = Date.now();
  
  try {
    const response = http.post(`${BASE_URL}/wallets/${fromWallet.id}/transfer`, JSON.stringify({
      toWalletId: toWallet.id,
      amount: amount
    }), {
      headers: { 'Content-Type': 'application/json' },
      timeout: '10s',
    });
    
    const duration = Date.now() - startTime;
    responseTime.add(duration);
    systemThroughput.add(1);
    
    const success = check(response, {
      'transfer status is 200 or 400': (r) => r.status === 200 || r.status === 400,
    });
    
    transactionRate.add(success ? 1 : 0);
    
    if (response.status === 200) {
      console.log(`🔄 Transferred $${amount.toFixed(2)} from ${fromWallet.id} to ${toWallet.id} (${duration}ms)`);
    } else if (response.status === 400) {
      console.log(`⚠️ Transfer failed - insufficient funds (${duration}ms)`);
    } else {
      console.log(`❌ Transfer failed: HTTP ${response.status} (${duration}ms)`);
    }
    
  } catch (error) {
    const duration = Date.now() - startTime;
    responseTime.add(duration);
    systemThroughput.add(1);
    transactionRate.add(0);
    console.log(`💥 Transfer exception: ${error} (${duration}ms)`);
  }
}

function performQuery() {
  const wallet = walletPool[Math.floor(Math.random() * walletPool.length)];
  const startTime = Date.now();
  
  try {
    const response = http.get(`${BASE_URL}/wallets/${wallet.id}`, {
      timeout: '10s',
    });
    
    const duration = Date.now() - startTime;
    responseTime.add(duration);
    systemThroughput.add(1);
    
    const success = check(response, {
      'query status is 200': (r) => r.status === 200,
    });
    
    transactionRate.add(success ? 1 : 0);
    
    if (success) {
      const walletData = JSON.parse(response.body);
      console.log(`📊 Queried ${wallet.id}: $${walletData.balance} (${duration}ms)`);
    } else {
      console.log(`❌ Query failed: HTTP ${response.status} (${duration}ms)`);
    }
    
  } catch (error) {
    const duration = Date.now() - startTime;
    responseTime.add(duration);
    systemThroughput.add(1);
    transactionRate.add(0);
    console.log(`💥 Query exception: ${error} (${duration}ms)`);
  }
}

export function handleSummary(data) {
  console.log('\n🏁 CONTINUOUS WALLET STRESS TEST COMPLETED!');
  console.log('==============================================');
  console.log(`📊 Total Operations: ${data.metrics.total_operations?.values?.count || 0}`);
  console.log(`🏦 Wallets Created: ${walletCounter}`);
  console.log(`📈 Avg Response Time: ${(data.metrics.operation_response_time?.values?.avg || 0).toFixed(2)}ms`);
  console.log(`🎯 P95 Response Time: ${(data.metrics.http_req_duration?.values?.['p(95)'] || 0).toFixed(2)}ms`);
  console.log(`✅ Wallet Creation Success Rate: ${((data.metrics.wallet_creation_success?.values?.rate || 0) * 100).toFixed(1)}%`);
  console.log(`💰 Transaction Success Rate: ${((data.metrics.transaction_success?.values?.rate || 0) * 100).toFixed(1)}%`);
  console.log(`❌ HTTP Error Rate: ${((data.metrics.http_req_failed?.values?.rate || 0) * 100).toFixed(1)}%`);
  console.log('\n📊 Check your Golden Metrics dashboard: http://localhost:3000/d/wallet-golden-metrics');
  
  return {
    'stdout': '', // Don't output the default summary
  };
}
