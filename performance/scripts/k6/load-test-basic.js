// Basic Load Test for Wallet Service
// Run with: k6 run scripts/load-test-basic.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const balanceQueryTime = new Trend('balance_query_duration');
const depositTime = new Trend('deposit_duration');

export const options = {
  stages: [
    { duration: '2m', target: 20 },   // Ramp up to 20 users
    { duration: '5m', target: 20 },   // Stay at 20 users
    { duration: '2m', target: 50 },   // Ramp up to 50 users  
    { duration: '5m', target: 50 },   // Stay at 50 users
    { duration: '2m', target: 0 },    // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'], // 95% under 1 second
    http_req_failed: ['rate<0.05'],    // Error rate under 5%
    errors: ['rate<0.05'],
  },
};

const BASE_URL = 'http://localhost:8080/api/v1';

// Test wallets (you'll need to create these first)
const TEST_WALLETS = [
  'wallet-test-1',
  'wallet-test-2', 
  'wallet-test-3',
  'wallet-test-4',
  'wallet-test-5'
];

export function setup() {
  console.log('🚀 Starting load test setup...');
  
  // Create test wallets
  const createdWallets = [];
  
  for (let i = 1; i <= 10; i++) {
    const response = http.post(`${BASE_URL}/wallets`, JSON.stringify({
      userId: `loadtest-user-${i}`
    }), {
      headers: { 'Content-Type': 'application/json' },
    });
    
    if (response.status === 201) {
      // Extract wallet ID from Location header
      const locationHeader = response.headers['Location'] || response.headers['location'];
      if (locationHeader) {
        const walletId = locationHeader.split('/').pop();
        createdWallets.push(walletId);
        console.log(`✅ Created wallet: ${walletId}`);
      }
    } else {
      console.log(`❌ Failed to create wallet for user ${i}: ${response.status} - ${response.body}`);
    }
  }
  
  console.log(`📊 Setup complete. Created ${createdWallets.length} wallets`);
  return { wallets: createdWallets };
}

export default function(data) {
  if (!data.wallets || data.wallets.length === 0) {
    console.log('❌ No wallets available for testing');
    return;
  }
  
  const walletId = data.wallets[Math.floor(Math.random() * data.wallets.length)];
  
  // Simulate user behavior with weighted operations
  const operation = Math.random();
  
  if (operation < 0.5) {
    // 50% - Balance queries (most common)
    const response = http.get(`${BASE_URL}/wallets/${walletId}`);
    
    const success = check(response, {
      'balance query status is 200': (r) => r.status === 200,
      'balance query has id': (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.id !== undefined && body.balance !== undefined;
        } catch (e) {
          return false;
        }
      },
    });
    
    balanceQueryTime.add(response.timings.duration);
    errorRate.add(!success);
    
  } else if (operation < 0.8) {
    // 30% - Deposits
    const amount = (Math.random() * 100 + 10).toFixed(2); // $10-$110
    const response = http.post(`${BASE_URL}/wallets/${walletId}/deposit`, 
      JSON.stringify({
        amount: amount,
        referenceId: `loadtest-dep-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
        description: `Load test deposit of $${amount}`
      }), {
        headers: { 'Content-Type': 'application/json' },
      }
    );
    
    const success = check(response, {
      'deposit status is 200': (r) => r.status === 200,
      'deposit has location header': (r) => {
        const locationHeader = r.headers['Location'] || r.headers['location'];
        return locationHeader !== undefined && locationHeader.includes('/transactions/');
      },
    });
    
    depositTime.add(response.timings.duration);
    errorRate.add(!success);
    
  } else {
    // 20% - Withdrawals (might fail due to insufficient funds)
    const amount = (Math.random() * 50 + 5).toFixed(2); // $5-$55
    const response = http.post(`${BASE_URL}/wallets/${walletId}/withdraw`,
      JSON.stringify({
        amount: amount,
        referenceId: `loadtest-wit-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
        description: `Load test withdrawal of $${amount}`
      }), {
        headers: { 'Content-Type': 'application/json' },
      }
    );
    
    // Withdrawal can legitimately fail with 400 (insufficient funds)
    const success = check(response, {
      'withdrawal status is 200 or 400': (r) => r.status === 200 || r.status === 400,
    });
    
    errorRate.add(!success);
  }
  
  // Think time - simulate user reading/deciding
  sleep(Math.random() * 3 + 1); // 1-4 seconds
}

export function teardown(data) {
  console.log('🏁 Load test completed');
  console.log(`📊 Tested with ${data.wallets ? data.wallets.length : 0} wallets`);
}

export function handleSummary(data) {
  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
    'load-test-results.json': JSON.stringify(data),
  };
}

function textSummary(data, options) {
  const indent = options.indent || '';
  const enableColors = options.enableColors || false;
  
  let summary = '\n' + indent + '📊 WALLET SERVICE LOAD TEST RESULTS\n';
  summary += indent + '=====================================\n\n';
  
  // Test duration
  const testDuration = Math.round(data.state.testRunDurationMs / 1000);
  summary += indent + `⏱️  Test Duration: ${testDuration}s\n\n`;
  
  // HTTP metrics
  const httpReqs = data.metrics.http_reqs;
  const httpReqDuration = data.metrics.http_req_duration;
  const httpReqFailed = data.metrics.http_req_failed;
  
  if (httpReqs) {
    summary += indent + `🚀 Total Requests: ${httpReqs.values.count}\n`;
    summary += indent + `📈 Requests/sec: ${httpReqs.values.rate.toFixed(2)}\n\n`;
  }
  
  if (httpReqDuration) {
    summary += indent + `⚡ Response Times:\n`;
    summary += indent + `   Average: ${httpReqDuration.values.avg.toFixed(2)}ms\n`;
    summary += indent + `   95th percentile: ${httpReqDuration.values['p(95)'].toFixed(2)}ms\n`;
    summary += indent + `   99th percentile: ${httpReqDuration.values['p(99)'].toFixed(2)}ms\n\n`;
  }
  
  if (httpReqFailed) {
    const errorPercentage = (httpReqFailed.values.rate * 100).toFixed(2);
    summary += indent + `❌ Error Rate: ${errorPercentage}%\n\n`;
  }
  
  // Custom metrics
  if (data.metrics.balance_query_duration) {
    summary += indent + `💰 Balance Query Average: ${data.metrics.balance_query_duration.values.avg.toFixed(2)}ms\n`;
  }
  
  if (data.metrics.deposit_duration) {
    summary += indent + `💵 Deposit Average: ${data.metrics.deposit_duration.values.avg.toFixed(2)}ms\n`;
  }
  
  summary += indent + '\n🎯 Performance Assessment:\n';
  
  // Performance assessment
  if (httpReqDuration && httpReqDuration.values['p(95)'] < 500) {
    summary += indent + '✅ Response times are good (95th percentile < 500ms)\n';
  } else if (httpReqDuration) {
    summary += indent + '⚠️  Response times need improvement (95th percentile > 500ms)\n';
  }
  
  if (httpReqFailed && httpReqFailed.values.rate < 0.01) {
    summary += indent + '✅ Error rate is acceptable (< 1%)\n';
  } else if (httpReqFailed) {
    summary += indent + '⚠️  Error rate is high (> 1%)\n';
  }
  
  return summary;
}
