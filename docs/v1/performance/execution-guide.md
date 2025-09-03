# Performance Testing Execution Guide

## 🚀 Quick Start

### Prerequisites
- ✅ Wallet Service running on `http://localhost:8080`
- ✅ Prometheus running on `http://localhost:9090`  
- ✅ All infrastructure services up (`docker-compose up -d`)

### 1-Minute Health Check
```bash
# Check application
curl http://localhost:8080/q/health

# Check Prometheus
curl http://localhost:9090/api/v1/query?query=up

# Check metrics endpoint
curl http://localhost:8080/metrics | grep wallet_operations
```

## 🧪 Test Execution

### Baseline Performance Test
```bash
# Run comprehensive baseline test (5 minutes)
./docs/performance/scripts/simple-load-test.sh baseline 300

# Quick baseline test (1 minute)  
./docs/performance/scripts/simple-load-test.sh baseline 60
```

### Load Testing
```bash
# Light load test (10 minutes)
./docs/performance/scripts/simple-load-test.sh load 600

# Stress test (30 minutes)
./docs/performance/scripts/simple-load-test.sh load 1800
```

### Individual Component Tests
```bash
# Test wallet creation only
./docs/performance/scripts/simple-load-test.sh create

# Test wallet queries only  
./docs/performance/scripts/simple-load-test.sh query

# Test deposits only
./docs/performance/scripts/simple-load-test.sh deposit
```

## 📊 Monitoring During Tests

### Real-time Prometheus Queries

#### Response Times (95th Percentile)
```bash
# Wallet Creation
curl -s "http://localhost:9090/api/v1/query?query=histogram_quantile(0.95, rate(wallet_operations_creation_duration_seconds_bucket[1m]))"

# Deposits
curl -s "http://localhost:9090/api/v1/query?query=histogram_quantile(0.95, rate(wallet_operations_deposit_duration_seconds_bucket[1m]))"

# Queries
curl -s "http://localhost:9090/api/v1/query?query=histogram_quantile(0.95, rate(wallet_operations_query_duration_seconds_bucket[1m]))"
```

#### Throughput (Requests Per Second)
```bash
# Total operations per second
curl -s "http://localhost:9090/api/v1/query?query=rate(wallet_operations_created_total[1m]) + rate(wallet_operations_deposits_total[1m]) + rate(wallet_operations_queries_total[1m])"

# Individual operation rates
curl -s "http://localhost:9090/api/v1/query?query=rate(wallet_operations_deposits_total[1m])"
```

#### Error Rates
```bash
# Failed operations per second
curl -s "http://localhost:9090/api/v1/query?query=rate(wallet_operations_failed_total[1m])"

# Error percentage
curl -s "http://localhost:9090/api/v1/query?query=rate(wallet_operations_failed_total[1m]) / (rate(wallet_operations_created_total[1m]) + rate(wallet_operations_deposits_total[1m]) + rate(wallet_operations_queries_total[1m])) * 100"
```

### Continuous Monitoring
```bash
# Monitor metrics every 30 seconds for 10 minutes
./docs/performance/scripts/monitor-metrics.sh 600

# Quick 2-minute monitoring
./docs/performance/scripts/monitor-metrics.sh 120
```

## 📈 Performance Targets

### Response Time Targets
| Operation | Target (95th percentile) | Acceptable | Critical |
|-----------|-------------------------|------------|----------|
| Create Wallet | < 100ms | < 200ms | > 500ms |
| Get Wallet | < 50ms | < 100ms | > 300ms |
| Deposit | < 100ms | < 200ms | > 500ms |
| Withdraw | < 100ms | < 200ms | > 500ms |
| Transfer | < 150ms | < 300ms | > 750ms |
| Historical Balance | < 200ms | < 400ms | > 1000ms |

### Throughput Targets
| Scenario | Target RPS | Acceptable RPS | Notes |
|----------|------------|----------------|-------|
| Read-Heavy (80/20) | 1000 | 500 | Cached reads |
| Write-Heavy (20/80) | 500 | 250 | Database writes |
| Mixed Operations | 750 | 375 | Realistic load |

### System Resource Limits
- **CPU Usage**: < 80% sustained
- **Memory (Heap)**: < 2GB
- **Database Connections**: < 80% of pool
- **Error Rate**: < 0.1%

## 🔍 Results Analysis

### Automated Analysis
```bash
# Generate performance report
./docs/performance/scripts/analyze-results.sh docs/performance/results/

# Compare two test runs
./docs/performance/scripts/compare-results.sh run1_timestamp run2_timestamp
```

### Manual Analysis Checklist

#### ✅ Response Times
- [ ] 95th percentile within targets
- [ ] No significant outliers (>2x target)
- [ ] Consistent performance across test duration
- [ ] No degradation over time

#### ✅ Throughput  
- [ ] Sustained target RPS achieved
- [ ] No significant drops during test
- [ ] Linear scaling with load increase
- [ ] Graceful degradation at limits

#### ✅ Error Rates
- [ ] Error rate < 0.1% under normal load
- [ ] No 5xx errors under normal conditions
- [ ] Proper error responses (4xx for business logic)
- [ ] Error rate increases predictably at limits

#### ✅ Resource Usage
- [ ] CPU usage reasonable and stable
- [ ] Memory usage within limits
- [ ] No memory leaks detected
- [ ] Database connections properly managed

## 🚨 Troubleshooting

### Common Issues

#### Application Not Responding
```bash
# Check if application is running
ps aux | grep quarkus

# Check application logs
tail -f quarkus.log

# Restart application
pkill -f "quarkus:dev"
./mvnw quarkus:dev
```

#### Prometheus Not Scraping
```bash
# Check Prometheus targets
curl http://localhost:9090/api/v1/targets

# Check metrics endpoint
curl http://localhost:8080/metrics | head -20

# Verify Prometheus config
cat prometheus.yml
```

#### High Error Rates
```bash
# Check application logs for errors
grep -i error quarkus.log | tail -10

# Check database connectivity
docker logs wallet-mysql-primary | tail -10

# Check Redis connectivity  
docker logs wallet-redis | tail -10
```

#### Poor Performance
```bash
# Check system resources
top -p $(pgrep -f quarkus)

# Check database performance
docker exec wallet-mysql-primary mysql -u wallet -pwallet -e "SHOW PROCESSLIST;"

# Check for GC issues
grep -i "gc" quarkus.log | tail -10
```

## 📋 Test Execution Checklist

### Pre-Test Setup
- [ ] All Docker services running (`docker-compose ps`)
- [ ] Application healthy (`curl http://localhost:8080/q/health`)
- [ ] Prometheus scraping (`curl http://localhost:9090/api/v1/targets`)
- [ ] Metrics endpoint working (`curl http://localhost:8080/metrics`)
- [ ] Clear previous test data (`rm -rf docs/performance/results/*`)

### During Test
- [ ] Monitor application logs (`tail -f quarkus.log`)
- [ ] Watch system resources (`top`, `htop`)
- [ ] Check error rates in real-time
- [ ] Note any anomalies or issues

### Post-Test Analysis
- [ ] Export Prometheus data
- [ ] Analyze response time distributions
- [ ] Check for performance degradation
- [ ] Document findings and recommendations
- [ ] Archive results with timestamp

## 🎯 Next Steps

### After Baseline Testing
1. **Analyze Results** - Compare against targets
2. **Identify Bottlenecks** - Find limiting factors
3. **Optimize Performance** - Address identified issues
4. **Re-test** - Validate improvements

### Performance Optimization Areas
- **Database Query Optimization** - Slow queries
- **Connection Pool Tuning** - Database connections
- **Cache Configuration** - Redis hit rates
- **JVM Tuning** - Heap size, GC settings
- **Application Code** - Async processing, batching

### Advanced Testing
- **Soak Testing** - 24+ hour endurance tests
- **Spike Testing** - Sudden load increases
- **Volume Testing** - Large data sets
- **Chaos Testing** - Failure scenarios
