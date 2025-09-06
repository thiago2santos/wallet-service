# Containerized Load Testing Guide

This guide explains how to deploy and test the Wallet Service in a containerized environment for realistic performance testing.

## 🎯 Why Containerized Testing?

Testing against a containerized deployment provides:

- **Realistic Performance**: Closer to production environment
- **Resource Isolation**: Controlled CPU/memory limits
- **Network Overhead**: Real container networking latency
- **JVM Optimization**: Production-like JVM settings
- **Database Tuning**: Optimized MySQL configuration
- **Monitoring**: Full observability stack

## 🚀 Quick Start

### 1. Deploy the Containerized Environment

```bash
# Build and deploy the containerized wallet service
./scripts/deploy-for-loadtest.sh
```

This script will:
- ✅ Build the optimized application
- ✅ Create Docker image with JVM tuning
- ✅ Start MySQL with performance optimizations
- ✅ Start Redis with high-performance config
- ✅ Deploy wallet service with resource limits
- ✅ Start Prometheus and Grafana monitoring
- ✅ Wait for all services to be healthy

### 2. Run the Containerized Load Test

```bash
# Run the containerized-specific load test
k6 run performance/scripts/k6/containerized-load-test.js
```

Or use the extreme load test against containers:
```bash
# Update the BASE_URL in extreme-load-test.js to target containers
k6 run performance/scripts/k6/extreme-load-test.js
```

## 📊 Service URLs

Once deployed, access these services:

| Service | URL | Purpose |
|---------|-----|---------|
| **Wallet API** | http://localhost:8080 | Main application |
| **Swagger UI** | http://localhost:8080/q/swagger-ui | API documentation |
| **Health Check** | http://localhost:8080/q/health | Service health |
| **Metrics** | http://localhost:8080/metrics | Prometheus metrics |
| **Grafana** | http://localhost:3000 | Dashboards (admin/admin) |
| **Prometheus** | http://localhost:9090 | Metrics collection |

## 🔧 Container Optimizations

### Application Optimizations

- **JVM Settings**: G1GC, 4GB heap, optimized GC pauses
- **Connection Pools**: 50 connections per datasource
- **HTTP Server**: 8 IO threads, 200 worker threads
- **Logging**: Reduced to WARN level for performance
- **Event Sourcing**: Disabled for pure performance testing

### Database Optimizations

- **InnoDB Buffer Pool**: 1GB
- **Max Connections**: 1000
- **Query Cache**: 256MB
- **Thread Cache**: 50 threads
- **Optimized I/O**: 8 read/write threads

### Redis Optimizations

- **Max Memory**: 512MB with LRU eviction
- **TCP Keepalive**: Enabled
- **Persistence**: Disabled for performance

## 📈 Expected Performance Characteristics

Based on containerized testing, expect:

### Sustainable Load
- **1,000-1,500 VUs**: Good performance
- **Response Times**: <500ms p95
- **Error Rate**: <1%

### High Load
- **1,500-2,500 VUs**: Acceptable performance
- **Response Times**: <1s p95
- **Error Rate**: <5%

### Breaking Point
- **2,500-3,500 VUs**: Performance degradation
- **Response Times**: <2s p95
- **Error Rate**: <15%

### System Limits
- **3,500+ VUs**: System stress
- **Response Times**: >2s p95
- **Error Rate**: >15%

## 🔍 Monitoring During Tests

### Real-time Monitoring

1. **Grafana Dashboards**: http://localhost:3000
   - Golden Metrics Dashboard
   - Business Metrics Dashboard
   - Infrastructure Metrics Dashboard

2. **Container Stats**:
   ```bash
   # Monitor container resource usage
   docker stats
   
   # Monitor specific service
   docker stats wallet-service-loadtest
   ```

3. **Application Logs**:
   ```bash
   # Follow all logs
   docker-compose -f docker-compose.loadtest.yml logs -f
   
   # Follow wallet service only
   docker-compose -f docker-compose.loadtest.yml logs -f wallet-service
   ```

### Key Metrics to Watch

- **Response Times**: p95, p99 latencies
- **Error Rates**: HTTP 4xx/5xx responses
- **Throughput**: Requests per second
- **CPU Usage**: Container CPU utilization
- **Memory Usage**: JVM heap and container memory
- **Database Connections**: Active/idle connections
- **GC Activity**: Garbage collection frequency/duration

## 🛑 Stopping the Environment

```bash
# Stop all containers
docker-compose -f docker-compose.loadtest.yml down

# Stop and remove volumes (clean slate)
docker-compose -f docker-compose.loadtest.yml down -v

# Clean up everything
docker system prune -f
```

## 🔄 Iterative Testing

### Test Different Scenarios

1. **Baseline Test**: Current configuration
2. **Tuned JVM**: Adjust heap sizes
3. **More Connections**: Increase pool sizes
4. **Different GC**: Try different garbage collectors
5. **Resource Limits**: Test with different CPU/memory limits

### Configuration Changes

Edit `docker-compose.loadtest.yml` to modify:
- Container resource limits
- JVM options
- Database settings
- Connection pool sizes

Then redeploy:
```bash
./scripts/deploy-for-loadtest.sh
```

## 🎯 Comparison with Development Mode

| Aspect | Development Mode | Containerized |
|--------|------------------|---------------|
| **JVM** | Default settings | Optimized G1GC |
| **Resources** | Unlimited | Limited (4 CPU, 6GB RAM) |
| **Database** | Default MySQL | Tuned MySQL |
| **Networking** | Localhost | Container network |
| **Monitoring** | Basic | Full observability |
| **Realistic** | ❌ | ✅ |

## 📝 Test Results Analysis

After running tests:

1. **Export Grafana Dashboards**: Save screenshots/data
2. **Collect K6 Results**: JSON output with detailed metrics
3. **Container Logs**: Save for error analysis
4. **Resource Usage**: Document peak CPU/memory usage
5. **Compare Results**: Against development mode testing

## 🚨 Troubleshooting

### Common Issues

1. **Container Won't Start**:
   ```bash
   docker-compose -f docker-compose.loadtest.yml logs wallet-service
   ```

2. **Database Connection Issues**:
   ```bash
   docker-compose -f docker-compose.loadtest.yml logs mysql-loadtest
   ```

3. **Out of Memory**:
   - Reduce JVM heap size in docker-compose.loadtest.yml
   - Increase container memory limits

4. **High Response Times**:
   - Check container resource usage
   - Monitor database connection pool
   - Review GC logs

### Performance Debugging

```bash
# Check container resource usage
docker stats --no-stream

# Check database performance
docker exec -it wallet-mysql-loadtest mysql -u wallet -pwallet -e "SHOW PROCESSLIST;"

# Check application metrics
curl http://localhost:8080/metrics | grep -E "(http_|db_|jvm_)"
```

## 🎉 Success Criteria

A successful containerized load test should demonstrate:

- ✅ **Realistic Performance**: Better understanding of production limits
- ✅ **Resource Utilization**: Clear CPU/memory usage patterns
- ✅ **Bottleneck Identification**: Specific performance constraints
- ✅ **Monitoring Validation**: Dashboards working during load
- ✅ **Baseline Documentation**: Performance characteristics recorded

This containerized approach gives you the most realistic performance testing environment possible before production deployment!
