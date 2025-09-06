# Baseline Performance Test Plan

## 🎯 Objectives

### Primary Goals
- **Establish performance baseline** - Current system capabilities
- **Measure actual performance** - Document real-world response times
- **Identify bottlenecks** - Find system limitations
- **Document actual limits** - Real-world performance metrics

### Success Criteria
- **Response Time**: Measure 95th percentile under normal load
- **Throughput**: Determine maximum sustainable RPS
- **Error Rate**: Monitor error rates under load
- **Resource Usage**: Monitor CPU and memory consumption

## 🧪 Test Scenarios

### Scenario 1: Single User Baseline
**Objective**: Establish baseline response times for each endpoint

| Endpoint | Test Duration | Measurement Goal |
|----------|---------------|------------------|
| POST /api/v1/wallets | 60s | Measure creation latency |
| GET /api/v1/wallets/{id} | 60s | Measure query latency |
| POST /api/v1/wallets/{id}/deposit | 60s | Measure deposit latency |
| POST /api/v1/wallets/{id}/withdraw | 60s | Measure withdrawal latency |
| POST /api/v1/wallets/{id}/transfer | 60s | Measure transfer latency |
| GET /api/v1/wallets/{id}/balance/historical | 60s | Measure historical query latency |

### Scenario 2: Read-Heavy Load (80/20)
**Objective**: Test read performance with caching

- **Load Pattern**: 80% GET operations, 20% write operations
- **Ramp-up**: 1 → 10 → 50 → 100 → 500 RPS over 10 minutes
- **Duration**: 15 minutes sustained load
- **Goal**: Measure 95th percentile response times

### Scenario 3: Write-Heavy Load (20/80)  
**Objective**: Test write performance and database limits

- **Load Pattern**: 20% GET operations, 80% write operations
- **Ramp-up**: 1 → 10 → 50 → 100 → 200 RPS over 10 minutes
- **Duration**: 15 minutes sustained load
- **Goal**: Measure 95th percentile response times under write load

### Scenario 4: Stress Testing
**Objective**: Find breaking point

- **Load Pattern**: Mixed operations
- **Ramp-up**: Increase by 50 RPS every 2 minutes until failure
- **Stop Condition**: Error rate > 1% OR 95th percentile > 1000ms
- **Goal**: Identify maximum sustainable throughput

## 📊 Metrics to Monitor

### Application Metrics (Prometheus)
```promql
# Response Times
histogram_quantile(0.95, wallet_operations_creation_duration_seconds)
histogram_quantile(0.95, wallet_operations_deposit_duration_seconds)
histogram_quantile(0.95, wallet_operations_withdrawal_duration_seconds)
histogram_quantile(0.95, wallet_operations_transfer_duration_seconds)
histogram_quantile(0.95, wallet_operations_query_duration_seconds)

# Throughput
rate(wallet_operations_created_total[1m])
rate(wallet_operations_deposits_total[1m])
rate(wallet_operations_withdrawals_total[1m])
rate(wallet_operations_transfers_total[1m])
rate(wallet_operations_queries_total[1m])

# Error Rates
rate(wallet_operations_failed_total[1m])

# Business Metrics
rate(wallet_money_deposited_total[1m])
rate(wallet_money_withdrawn_total[1m])
rate(wallet_money_transferred_total[1m])
```

### System Metrics
- **JVM Heap Usage**: `jvm_memory_used_bytes{area="heap"}`
- **GC Activity**: `jvm_gc_collection_seconds`
- **Thread Count**: `jvm_threads_current`
- **Database Connections**: `hikaricp_connections_active`

## 🛠️ Test Tools

### Primary: Artillery.js
```yaml
config:
  target: 'http://localhost:8080'
  phases:
    - duration: 60
      arrivalRate: 1
      name: "Baseline"
    - duration: 300  
      arrivalRate: 10
      name: "Light Load"
    - duration: 300
      arrivalRate: 50
      name: "Medium Load"
```

### Secondary: k6
```javascript
import http from 'k6/http';
import { check } from 'k6';

export let options = {
  stages: [
    { duration: '2m', target: 10 },
    { duration: '5m', target: 50 },
    { duration: '2m', target: 0 },
  ],
};
```

## 📋 Test Environment

### Infrastructure
- **Application**: Quarkus (JVM mode)
- **Database**: MySQL 8.0 (Primary + Replica)
- **Cache**: Redis 7.0
- **Messaging**: Apache Kafka
- **Monitoring**: Prometheus + Custom Metrics

### Hardware Specs
- **CPU**: Document actual specs
- **Memory**: Document actual specs  
- **Storage**: Document actual specs
- **Network**: Local (Docker Compose)

## 📈 Expected Results

### Performance Measurements to Capture
| Metric | Test Method | Measurement Goal |
|--------|-------------|------------------|
| Response times | Single user baseline | Measure actual 95th percentile |
| Maximum throughput | Stress testing | Find sustainable RPS limit |
| Startup time | Application restart | Measure actual startup time |
| Memory usage | Resource monitoring | Monitor heap usage patterns |

### Bottleneck Predictions
1. **Database connections** - Likely first bottleneck
2. **Redis cache** - Memory limitations
3. **JVM GC** - Heap pressure under load
4. **Network I/O** - Docker networking limits

## 📝 Test Execution Checklist

### Pre-Test
- [ ] Start all infrastructure services
- [ ] Verify application health
- [ ] Clear metrics/logs
- [ ] Document system state

### During Test  
- [ ] Monitor Prometheus metrics
- [ ] Watch application logs
- [ ] Track system resources
- [ ] Note any anomalies

### Post-Test
- [ ] Export metrics data
- [ ] Analyze results
- [ ] Document findings
- [ ] Create performance report

## 🎯 Next Steps

1. **Execute baseline tests** - Establish current performance
2. **Create detailed reports** - Document findings
3. **Identify optimizations** - Performance improvements
4. **Validate improvements** - Re-test after changes
