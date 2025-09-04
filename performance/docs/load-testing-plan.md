# Load Testing Plan

> Comprehensive performance testing strategy for the Wallet Service

## 🎯 **Testing Objectives**

### **Primary Goals**
- **Validate performance claims** - Test our "sub-100ms" response time claims
- **Find breaking points** - Determine maximum throughput before failures
- **Identify bottlenecks** - Database, cache, application layer limits
- **Establish baselines** - Real performance metrics for documentation
- **Ensure stability** - System behavior under sustained load

### **Success Criteria**
- **Response Times**: 95th percentile under defined thresholds
- **Throughput**: Minimum requests per second without errors
- **Error Rate**: Less than 1% errors under normal load
- **Resource Usage**: CPU/Memory within acceptable limits
- **Recovery**: System recovers gracefully after load spikes

---

## 📊 **Test Scenarios**

### **Scenario 1: Normal Business Load**
**Purpose**: Simulate typical daily usage patterns

**Profile**:
- **Users**: 100 concurrent users
- **Duration**: 30 minutes
- **Pattern**: Gradual ramp-up over 5 minutes

**Operations Mix**:
- 40% Balance queries (`GET /wallets/{id}/balance`)
- 25% Deposits (`POST /wallets/{id}/deposit`)
- 20% Withdrawals (`POST /wallets/{id}/withdraw`)
- 10% Transfers (`POST /wallets/{sourceId}/transfer`)
- 5% Historical queries (`GET /wallets/{id}/balance/historical`)

**Expected Results**:
- Response time: < 200ms (95th percentile)
- Throughput: > 500 RPS
- Error rate: < 0.5%

### **Scenario 2: Peak Load**
**Purpose**: Test system under high but realistic load

**Profile**:
- **Users**: 500 concurrent users
- **Duration**: 20 minutes
- **Pattern**: Quick ramp-up over 2 minutes

**Operations Mix**:
- 50% Balance queries (read-heavy during peak)
- 20% Deposits
- 15% Withdrawals
- 10% Transfers
- 5% Historical queries

**Expected Results**:
- Response time: < 500ms (95th percentile)
- Throughput: > 1000 RPS
- Error rate: < 1%

### **Scenario 3: Stress Test**
**Purpose**: Find the breaking point

**Profile**:
- **Users**: Start at 100, increase by 100 every 2 minutes
- **Duration**: Until system breaks or 2000 users
- **Pattern**: Aggressive ramp-up

**Operations Mix**:
- 60% Balance queries (most common operation)
- 40% Write operations (deposits/withdrawals/transfers)

**Expected Results**:
- Find maximum sustainable load
- Identify failure modes
- Measure recovery time

### **Scenario 4: Spike Test**
**Purpose**: Test system resilience to sudden load spikes

**Profile**:
- **Base Load**: 50 users
- **Spike**: Jump to 1000 users for 5 minutes
- **Recovery**: Back to 50 users
- **Duration**: 30 minutes total

**Expected Results**:
- System handles spike without crashing
- Performance degrades gracefully
- Quick recovery to normal performance

### **Scenario 5: Endurance Test**
**Purpose**: Test system stability over extended periods

**Profile**:
- **Users**: 200 concurrent users (moderate load)
- **Duration**: 4 hours
- **Pattern**: Steady load with minor variations

**Expected Results**:
- No memory leaks
- Stable performance over time
- No connection pool exhaustion

### **Scenario 6: Database-Heavy Test**
**Purpose**: Test database and cache performance

**Profile**:
- **Users**: 300 concurrent users
- **Duration**: 15 minutes
- **Pattern**: Focus on data-intensive operations

**Operations Mix**:
- 30% Historical balance queries (complex DB queries)
- 30% Balance queries (cache hits/misses)
- 40% Write operations (database writes)

**Expected Results**:
- Database connection pool stability
- Cache effectiveness
- Query performance under load

---

## 🛠️ **Testing Tools**

### **Primary Tool: K6** ⭐ (Recommended)

**Why K6:**
- ✅ **JavaScript-based** - Easy to write and maintain
- ✅ **High performance** - Written in Go, handles high load
- ✅ **Great reporting** - Built-in metrics and dashboards
- ✅ **CI/CD friendly** - Easy integration with pipelines
- ✅ **Cloud support** - Can run in K6 Cloud for scale

**Installation**:
```bash
# macOS
brew install k6

# Linux
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6

# Docker
docker run --rm -i grafana/k6:latest run - <script.js
```

### **Secondary Tool: JMeter** (Alternative)

**Why JMeter:**
- ✅ **GUI interface** - Visual test plan creation
- ✅ **Mature ecosystem** - Lots of plugins and examples
- ✅ **Detailed reporting** - Comprehensive reports
- ✅ **Protocol support** - HTTP, HTTPS, WebSocket, etc.

**Use Case**: When you need GUI-based test creation or complex scenarios

### **Monitoring Tools**

#### **Application Metrics**
- **Prometheus + Grafana** - Real-time metrics during tests
- **JVM metrics** - Heap, GC, thread pools
- **Custom metrics** - Business logic performance

#### **Infrastructure Metrics**
- **Docker stats** - Container resource usage
- **System metrics** - CPU, memory, disk I/O
- **Network metrics** - Bandwidth, connections

#### **Database Metrics**
- **MySQL metrics** - Connections, query time, locks
- **Redis metrics** - Memory usage, hit rates, connections

---

## 📋 **Test Environment Setup**

### **Isolated Test Environment**

```yaml
# docker-compose.loadtest.yml
version: '3.8'

services:
  wallet-service:
    build: .
    ports:
      - "8080:8080"
    environment:
      - QUARKUS_PROFILE=loadtest
      - JAVA_OPTS=-Xmx1g -Xms512m
    depends_on:
      - mysql-primary
      - redis
      - kafka

  mysql-primary:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: loadtest
      MYSQL_DATABASE: wallet_loadtest
    ports:
      - "3306:3306"
    command: >
      --max-connections=1000
      --innodb-buffer-pool-size=512M
      --innodb-log-file-size=256M

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru

  kafka:
    image: confluentinc/cp-kafka:latest
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
    ports:
      - "9092:9092"
    depends_on:
      - zookeeper

  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  # Monitoring stack
  prometheus:
    image: prom/prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml

  grafana:
    image: grafana/grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=loadtest
```

### **Load Test Configuration**

```properties
# application-loadtest.properties
quarkus.profile=loadtest

# Database optimizations
quarkus.datasource.write.reactive.max-size=50
quarkus.datasource.read.reactive.max-size=100
quarkus.hibernate-orm.log.sql=false

# Redis optimizations
quarkus.redis.max-pool-size=50
quarkus.redis.max-pool-waiting=50

# JVM optimizations
quarkus.thread-pool.max-threads=200
quarkus.vertx.event-loops-pool-size=8

# Disable dev features
quarkus.devservices.enabled=false
quarkus.log.level=WARN
```

---

## 📝 **Test Scripts**

### **K6 Test Script Example**

```javascript
// load-test-scenarios.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const balanceQueryTime = new Trend('balance_query_duration');
const depositTime = new Trend('deposit_duration');

export const options = {
  scenarios: {
    normal_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '5m', target: 100 },  // Ramp up
        { duration: '30m', target: 100 }, // Stay at 100
        { duration: '5m', target: 0 },    // Ramp down
      ],
    },
    peak_load: {
      executor: 'ramping-vus',
      startTime: '45m',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 500 },  // Quick ramp up
        { duration: '20m', target: 500 }, // Peak load
        { duration: '3m', target: 0 },    // Ramp down
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% under 500ms
    http_req_failed: ['rate<0.01'],   // Error rate under 1%
    errors: ['rate<0.01'],
  },
};

const BASE_URL = 'http://localhost:8080/api/v1';

// Test data
const wallets = [];
for (let i = 0; i < 1000; i++) {
  wallets.push(`wallet-${i}`);
}

export function setup() {
  // Create test wallets
  console.log('Creating test wallets...');
  for (let i = 0; i < 100; i++) {
    const response = http.post(`${BASE_URL}/wallets`, JSON.stringify({
      userId: `user-${i}`,
      // Single currency: BRL
    }), {
      headers: { 'Content-Type': 'application/json' },
    });
    
    if (response.status === 201) {
      const wallet = JSON.parse(response.body);
      wallets.push(wallet.walletId);
    }
  }
  
  return { wallets };
}

export default function(data) {
  const walletId = data.wallets[Math.floor(Math.random() * data.wallets.length)];
  
  // Weighted operation selection
  const operation = Math.random();
  
  if (operation < 0.4) {
    // 40% - Balance queries
    const response = http.get(`${BASE_URL}/wallets/${walletId}/balance`);
    
    const success = check(response, {
      'balance query status is 200': (r) => r.status === 200,
      'balance query response time < 200ms': (r) => r.timings.duration < 200,
    });
    
    balanceQueryTime.add(response.timings.duration);
    errorRate.add(!success);
    
  } else if (operation < 0.65) {
    // 25% - Deposits
    const response = http.post(`${BASE_URL}/wallets/${walletId}/deposit`, 
      JSON.stringify({
        amount: (Math.random() * 1000).toFixed(2),
        referenceId: `dep-${Date.now()}-${Math.random()}`,
        description: 'Load test deposit'
      }), {
        headers: { 'Content-Type': 'application/json' },
      }
    );
    
    const success = check(response, {
      'deposit status is 200': (r) => r.status === 200,
      'deposit response time < 300ms': (r) => r.timings.duration < 300,
    });
    
    depositTime.add(response.timings.duration);
    errorRate.add(!success);
    
  } else if (operation < 0.85) {
    // 20% - Withdrawals
    const response = http.post(`${BASE_URL}/wallets/${walletId}/withdraw`,
      JSON.stringify({
        amount: (Math.random() * 100).toFixed(2),
        referenceId: `wit-${Date.now()}-${Math.random()}`,
        description: 'Load test withdrawal'
      }), {
        headers: { 'Content-Type': 'application/json' },
      }
    );
    
    check(response, {
      'withdrawal status is 200 or 400': (r) => r.status === 200 || r.status === 400,
    });
    
  } else if (operation < 0.95) {
    // 10% - Transfers
    const targetWallet = data.wallets[Math.floor(Math.random() * data.wallets.length)];
    if (targetWallet !== walletId) {
      const response = http.post(`${BASE_URL}/wallets/${walletId}/transfer`,
        JSON.stringify({
          destinationWalletId: targetWallet,
          amount: (Math.random() * 50).toFixed(2),
          referenceId: `tra-${Date.now()}-${Math.random()}`,
          description: 'Load test transfer'
        }), {
          headers: { 'Content-Type': 'application/json' },
        }
      );
      
      check(response, {
        'transfer status is 200 or 400': (r) => r.status === 200 || r.status === 400,
      });
    }
    
  } else {
    // 5% - Historical queries
    const timestamp = new Date(Date.now() - Math.random() * 86400000).toISOString().slice(0, -1);
    const response = http.get(`${BASE_URL}/wallets/${walletId}/balance/historical?timestamp=${timestamp}`);
    
    check(response, {
      'historical query status is 200': (r) => r.status === 200,
      'historical query response time < 1000ms': (r) => r.timings.duration < 1000,
    });
  }
  
  sleep(Math.random() * 2); // Random think time 0-2 seconds
}

export function teardown(data) {
  console.log('Load test completed');
}
```

---

## 📊 **Performance Baselines**

### **Target Metrics**

| Scenario | Concurrent Users | Response Time (95th) | Throughput (RPS) | Error Rate |
|----------|------------------|---------------------|------------------|------------|
| Normal Load | 100 | < 200ms | > 500 | < 0.5% |
| Peak Load | 500 | < 500ms | > 1000 | < 1% |
| Stress Test | Variable | < 1000ms | Find limit | < 5% |

### **Resource Limits**

| Component | CPU | Memory | Connections |
|-----------|-----|--------|-------------|
| Application | < 80% | < 1GB | N/A |
| MySQL | < 70% | < 2GB | < 500 |
| Redis | < 50% | < 512MB | < 1000 |

### **Business Metrics**

| Operation | Target Response Time | Acceptable Error Rate |
|-----------|---------------------|----------------------|
| Balance Query | < 50ms | < 0.1% |
| Deposit | < 200ms | < 0.5% |
| Withdrawal | < 200ms | < 1% (due to validation) |
| Transfer | < 300ms | < 1% (complex operation) |
| Historical Query | < 500ms | < 2% (complex query) |

---

## 🚀 **Execution Plan**

### **Phase 1: Environment Setup** (1 day)
1. Create isolated test environment
2. Configure monitoring stack
3. Prepare test data
4. Validate test setup

### **Phase 2: Baseline Testing** (1 day)
1. Run normal load scenario
2. Establish performance baselines
3. Identify obvious bottlenecks
4. Document initial findings

### **Phase 3: Comprehensive Testing** (2 days)
1. Execute all test scenarios
2. Monitor system behavior
3. Collect detailed metrics
4. Analyze results

### **Phase 4: Optimization** (As needed)
1. Address identified bottlenecks
2. Re-run tests to validate improvements
3. Update performance documentation
4. Create performance regression tests

---

## 📈 **Expected Outcomes**

### **Documentation Updates**
- Replace performance claims with real data
- Update implementation status with test results
- Add performance section to README
- Create performance monitoring guide

### **System Improvements**
- Database query optimizations
- Connection pool tuning
- Cache configuration improvements
- JVM parameter optimization

### **Continuous Testing**
- Automated performance tests in CI/CD
- Performance regression detection
- Regular performance monitoring
- Performance budgets for new features

---

**Let's find out what this system can really do! 🎯**

*No more guessing about performance - let's get real data!*
