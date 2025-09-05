# 🚀 Performance Testing Framework

This directory contains all performance and load testing tools, scripts, and results for the Wallet Service.

## 📁 Directory Structure

```
performance/
├── scripts/           # Test execution scripts
│   ├── k6/           # K6 load testing scripts
│   └── shell/        # Shell orchestration scripts
├── monitoring/       # Real-time monitoring tools
├── results/          # Test results and reports
│   ├── current/      # Latest test results
│   └── archive/      # Historical results and legacy tests
├── docs/            # Performance testing documentation
└── README.md        # This file
```

## 🎯 Quick Start

### Prerequisites
- Docker Compose running (`docker-compose up -d`)
- K6 installed (`brew install k6` or [download](https://k6.io/docs/getting-started/installation/))
- Grafana dashboards available at http://localhost:3000

### Run Basic Load Test
```bash
# Basic load test (5 VUs for 30s)
k6 run performance/scripts/k6/load-test-basic.js

# Continuous wallet stress test (creates wallets + transactions)
k6 run --duration 60s --vus 5 performance/scripts/k6/continuous-wallet-stress.js

# Find system breaking point
./performance/scripts/shell/find-breaking-point.sh
```

### Monitor in Real-time
```bash
# Quick metrics snapshot
./performance/monitoring/quick-monitor.sh

# Real-time monitoring during tests
./performance/monitoring/realtime-monitor.sh

# Monitor specific stress test
./performance/monitoring/monitor-stress-test.sh
```

## 📊 Test Scripts

### K6 Scripts (`performance/scripts/k6/`)

| Script | Purpose | Duration | Load Pattern |
|--------|---------|----------|--------------|
| `load-test-basic.js` | Basic API validation | 30s | 5 VUs steady |
| `continuous-wallet-stress.js` | Wallet creation + transactions | Configurable | Creates wallets every 2s + random transactions |
| `stress-test-to-failure.js` | Progressive load increase | Variable | Ramps up until failure |

### Shell Scripts (`performance/scripts/shell/`)

| Script | Purpose | Features |
|--------|---------|----------|
| `find-breaking-point.sh` | Launch continuous stress test | Auto-start, Golden Metrics integration |
| `run-extreme-stress-test.sh` | Orchestrated stress testing | Prerequisites check, monitoring, cleanup |
| `setup-load-test.sh` | Test environment setup | Dependencies, validation |

## 📈 Monitoring Tools (`performance/monitoring/`)

| Tool | Purpose | Output |
|------|---------|---------|
| `quick-monitor.sh` | Instant metrics snapshot | Console summary |
| `realtime-monitor.sh` | Live metrics feed | Continuous updates |
| `monitor-stress-test.sh` | Golden Metrics monitoring | SRE-focused metrics |
| `monitor-load-test.sh` | General load test monitoring | Comprehensive metrics |

## 🎛️ Golden Metrics Dashboard

Access the **Golden Metrics (SRE)** dashboard for real-time monitoring:
- **URL**: http://localhost:3000/d/wallet-golden-metrics
- **Metrics**: Latency, Traffic, Errors, Saturation
- **SLI/SLO**: Real-time compliance monitoring

## 📋 Test Scenarios

### 1. **Basic Health Check**
```bash
k6 run performance/scripts/k6/load-test-basic.js
```
- **Purpose**: Validate all endpoints work
- **Load**: 5 VUs for 30 seconds
- **Expected**: <100ms P95, 0% errors

### 2. **Continuous Wallet Operations**
```bash
k6 run --duration 60s --vus 5 performance/scripts/k6/continuous-wallet-stress.js
```
- **Purpose**: Real-world usage simulation
- **Pattern**: Creates wallets every 2s + random transactions
- **Expected**: 100% wallet creation success, <50ms P95

### 3. **Breaking Point Detection**
```bash
./performance/scripts/shell/find-breaking-point.sh
```
- **Purpose**: Find system limits
- **Pattern**: Progressive load increase
- **Monitor**: Golden Metrics dashboard for real-time impact

## 📊 Results Analysis

### Current Results (`performance/results/current/`)
- Latest test outputs in JSON format
- Real-time logs and metrics
- Performance snapshots

### Historical Data (`performance/results/archive/`)
- Legacy performance reports
- Baseline measurements
- Trend analysis data

## 🔧 Configuration

### Environment Variables
```bash
# Test target (default: localhost:8080)
export WALLET_SERVICE_URL="http://localhost:8080"

# Test duration (default: 30s)
export TEST_DURATION="60s"

# Virtual users (default: 5)
export TEST_VUS="10"
```

### K6 Options
All K6 scripts support standard options:
```bash
k6 run --duration 2m --vus 10 --iterations 1000 script.js
```

## 🎯 Performance Targets

### SLI/SLO Targets
- **Latency**: P95 < 100ms, P99 < 200ms
- **Availability**: > 99.9% uptime
- **Error Rate**: < 0.1% for business operations
- **Throughput**: > 100 RPS sustained

### Resource Limits
- **CPU**: < 80% utilization
- **Memory**: < 2GB heap usage
- **Database**: < 80% connection pool usage
- **Response Time**: < 50ms average

## 🚨 Troubleshooting

### Common Issues

**High Error Rates (HTTP 400)**
- Check request payload validation
- Verify wallet IDs exist before transactions
- Review business logic constraints

**Performance Degradation**
- Monitor JVM GC activity
- Check database connection pool
- Review Golden Metrics dashboard

**Test Script Failures**
- Verify service is running (`docker-compose ps`)
- Check network connectivity
- Review K6 logs for specific errors

### Debug Commands
```bash
# Check service health
curl http://localhost:8080/q/health

# View live metrics
curl http://localhost:8080/metrics

# Check container status
docker-compose ps

# View service logs
docker-compose logs wallet-service
```

## 📚 Documentation

- `docs/load-testing-plan.md` - Comprehensive testing strategy
- `../grafana/README.md` - Dashboard documentation
- `../README.md` - Main project setup

## 🤝 Contributing

When adding new performance tests:
1. Place K6 scripts in `scripts/k6/`
2. Place shell scripts in `scripts/shell/`
3. Update this README with new test descriptions
4. Include expected performance targets
5. Add monitoring integration where applicable

---

**🎯 Happy Load Testing!** Use the Golden Metrics dashboard to monitor your tests in real-time.
