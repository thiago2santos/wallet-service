# 🔧 Wallet Service - Distributed Tracing Troubleshooting Guide

## 🎯 Overview

This guide provides step-by-step troubleshooting procedures using **distributed tracing** (OpenTelemetry + Jaeger) combined with **Grafana dashboards** for comprehensive observability.

## 🛠️ Troubleshooting Tools

### 1. **Jaeger UI** - Distributed Tracing
- **URL**: `http://localhost:16686`
- **Purpose**: Trace request flows, identify bottlenecks, debug errors
- **Service Name**: `wallet-service`

### 2. **Grafana Dashboards** - Metrics & Alerts
- **URL**: `http://localhost:3000` (admin/admin)
- **Distributed Tracing Dashboard**: "Wallet Service - Distributed Tracing & Troubleshooting"
- **Other Dashboards**: Golden Metrics, Business Metrics, Technical Metrics

### 3. **Prometheus** - Raw Metrics
- **URL**: `http://localhost:9090`
- **Purpose**: Query raw metrics, create custom alerts

---

## 🚨 Common Issues & Solutions

### Issue 1: **Slow Wallet Operations**

#### **Symptoms**
- High response times (>2s)
- User complaints about slow deposits/withdrawals
- Timeouts in application logs

#### **Troubleshooting Steps**

1. **Check Grafana Dashboard**
   ```
   Dashboard: "Distributed Tracing & Troubleshooting"
   Panel: "Response Time Percentiles"
   Look for: 95th percentile > 2000ms
   ```

2. **Analyze Traces in Jaeger**
   ```
   1. Open http://localhost:16686
   2. Service: wallet-service
   3. Operation: api.wallet.deposit OR wallet.deposit
   4. Lookback: Last 1 hour
   5. Click "Find Traces"
   ```

3. **Identify Bottlenecks**
   - **Database Slow**: Check "Database & Cache Latency" panel
   - **Cache Miss**: Look for Redis spans in traces
   - **Circuit Breaker**: Check "Resilience Patterns" panel

#### **Solutions**
- **Database**: Optimize queries, check connection pool
- **Cache**: Verify Redis connectivity, check cache hit ratio
- **Circuit Breaker**: Check if services are degraded

---

### Issue 2: **High Error Rate**

#### **Symptoms**
- HTTP 4xx/5xx errors increasing
- Failed transactions
- Circuit breakers opening

#### **Troubleshooting Steps**

1. **Check Error Rate Dashboard**
   ```
   Panel: "Error Rate"
   Look for: Spikes > 5%
   Time Range: Adjust to incident period
   ```

2. **Find Error Traces**
   ```
   Jaeger UI:
   1. Service: wallet-service
   2. Tags: error=true
   3. Min Duration: 0ms
   4. Max Duration: Leave empty
   ```

3. **Analyze Error Patterns**
   - **Validation Errors**: Check request spans for invalid data
   - **Database Errors**: Look for SQL exception spans
   - **Timeout Errors**: Check span duration vs timeout settings

#### **Solutions**
- **Validation**: Update input validation rules
- **Database**: Check connection health, query performance
- **Timeouts**: Adjust timeout configurations

---

### Issue 3: **Memory Leaks / Resource Issues**

#### **Symptoms**
- Increasing memory usage
- Connection pool exhaustion
- Slow garbage collection

#### **Troubleshooting Steps**

1. **Check Resource Metrics**
   ```
   Dashboard: "Technical Metrics"
   Panels: JVM Memory, Connection Pools, GC Activity
   ```

2. **Trace Resource Usage**
   ```
   Jaeger UI:
   1. Look for long-running spans
   2. Check database connection spans
   3. Identify resource cleanup patterns
   ```

3. **Analyze Patterns**
   - **Connection Leaks**: Spans without proper closure
   - **Memory Leaks**: Increasing span creation without cleanup
   - **GC Pressure**: High allocation rates in spans

---

### Issue 4: **Business Logic Errors**

#### **Symptoms**
- Incorrect balance calculations
- Failed transfers
- Inconsistent wallet states

#### **Troubleshooting Steps**

1. **Check Business Metrics**
   ```
   Dashboard: "Business Metrics"
   Panels: Transaction Success Rate, Balance Accuracy
   ```

2. **Trace Business Operations**
   ```
   Jaeger UI:
   1. Operation: wallet.deposit, wallet.withdraw, wallet.transfer
   2. Look for failed spans
   3. Check span tags for business context
   ```

3. **Analyze Transaction Flow**
   - **CQRS Flow**: Command → Handler → Event → Update
   - **Database Consistency**: Check read/write separation
   - **Event Sourcing**: Verify event ordering

---

## 🔍 Advanced Troubleshooting Techniques

### 1. **Correlation Analysis**

**Correlate metrics with traces:**
```
1. Identify spike in Grafana dashboard
2. Note exact timestamp
3. Search Jaeger for traces in that time window
4. Analyze common patterns in failed traces
```

### 2. **Performance Profiling**

**Use trace data for optimization:**
```
1. Find slowest operations in Jaeger
2. Identify bottleneck spans
3. Correlate with resource usage in Grafana
4. Optimize identified components
```

### 3. **Dependency Analysis**

**Map service dependencies:**
```
1. Use Jaeger's dependency graph
2. Identify critical path components
3. Check health of each dependency
4. Plan resilience improvements
```

---

## 📊 Custom Queries & Alerts

### Prometheus Queries for Troubleshooting

```promql
# High error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1

# Slow database operations
histogram_quantile(0.95, rate(wallet_database_operation_duration_seconds_bucket[5m])) > 2

# Circuit breaker open
resilience4j_circuitbreaker_state{state="open"} > 0

# High memory usage
jvm_memory_used_bytes / jvm_memory_max_bytes > 0.8
```

### Jaeger Search Tips

```
# Find errors
tags: error=true

# Find slow operations
minDuration: 2s

# Find specific user operations
tags: user.id=user123

# Find database issues
tags: db.statement=*SELECT*
```

---

## 🚀 Proactive Monitoring

### 1. **Set Up Alerts**
- Configure Grafana alerts for key metrics
- Use Prometheus alerting rules
- Set up notification channels (Slack, email)

### 2. **Regular Health Checks**
- Monitor dashboard trends daily
- Review trace samples weekly
- Analyze performance patterns monthly

### 3. **Capacity Planning**
- Use historical trace data
- Identify scaling bottlenecks
- Plan infrastructure upgrades

---

## 📚 Additional Resources

- **OpenTelemetry Docs**: https://opentelemetry.io/docs/
- **Jaeger Documentation**: https://www.jaegertracing.io/docs/
- **Grafana Dashboards**: https://grafana.com/docs/grafana/latest/dashboards/
- **Prometheus Queries**: https://prometheus.io/docs/prometheus/latest/querying/

---

## 🆘 Emergency Procedures

### Critical System Failure

1. **Immediate Actions**
   - Check "Wallet Service Overview" dashboard
   - Look for red alerts in Grafana
   - Check Jaeger for error traces in last 15 minutes

2. **Escalation Path**
   - Gather trace IDs from failed operations
   - Export relevant dashboard screenshots
   - Document timeline of events

3. **Recovery Steps**
   - Use traces to identify root cause
   - Apply targeted fixes based on trace analysis
   - Monitor recovery using dashboards

Remember: **Distributed tracing provides the "why" behind the "what" shown in metrics!** 🎯
