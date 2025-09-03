# Baseline Performance Test Plan

## 🎯 Objectives

### Primary Goals
- **Establish performance baseline** - Current system capabilities
- **Validate performance claims** - Test "sub-100ms" and "10,000 RPS" claims  
- **Identify bottlenecks** - Find system limitations
- **Document actual limits** - Real-world performance metrics

### Success Criteria
- **Response Time**: 95th percentile < 200ms under normal load
- **Throughput**: Sustained > 1,000 RPS without errors
- **Error Rate**: < 0.1% under normal load  
- **Resource Usage**: CPU < 80%, Memory < 2GB heap

## 🧪 Test Scenarios

### Scenario 1: Single User Baseline
**Objective**: Establish baseline response times for each endpoint

| Endpoint | Expected Response Time | Test Duration |
|----------|----------------------|---------------|
| POST /api/v1/wallets | < 100ms | 60s |
| GET /api/v1/wallets/{id} | < 50ms | 60s |
| POST /api/v1/wallets/{id}/deposit | < 100ms | 60s |
| POST /api/v1/wallets/{id}/withdraw | < 100ms | 60s |
| POST /api/v1/wallets/{id}/transfer | < 150ms | 60s |
| GET /api/v1/wallets/{id}/balance/historical | < 200ms | 60s |

### Scenario 2: Read-Heavy Load (80/20)
**Objective**: Test read performance with caching

- **Load Pattern**: 80% GET operations, 20% write operations
- **Ramp-up**: 1 → 10 → 50 → 100 → 500 RPS over 10 minutes
- **Duration**: 15 minutes sustained load
- **Target**: Maintain < 100ms 95th percentile

### Scenario 3: Write-Heavy Load (20/80)  
**Objective**: Test write performance and database limits

- **Load Pattern**: 20% GET operations, 80% write operations
- **Ramp-up**: 1 → 10 → 50 → 100 → 200 RPS over 10 minutes
- **Duration**: 15 minutes sustained load
- **Target**: Maintain < 200ms 95th percentile

### Scenario 4: Stress Testing
**Objective**: Find breaking point

- **Load Pattern**: Mixed operations
- **Ramp-up**: Increase by 50 RPS every 2 minutes until failure
- **Stop Condition**: Error rate > 1% OR 95th percentile > 1000ms
- **Target**: Identify maximum sustainable throughput

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

### Performance Claims to Validate
| Claim | Test Method | Expected Result |
|-------|-------------|-----------------|
| "Sub-100ms response times" | Single user baseline | 95th percentile < 100ms |
| "10,000 RPS" | Stress testing | Sustained 10k RPS |
| "Sub-second startup" | Application restart | < 1s to ready |
| "Low memory footprint" | Resource monitoring | < 100MB heap |

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
