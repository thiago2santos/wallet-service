# Grafana Dashboards for Wallet Service

This directory contains comprehensive Grafana dashboards for monitoring the Wallet Service application.

## Dashboards Overview

### 1. Wallet Service - Overview (`wallet-service-overview.json`)
- **Purpose**: High-level overview of the service health and performance
- **Key Metrics**:
  - Total wallets and transactions
  - Operations rate (per minute)
  - Average response times
  - Failed operations rate
  - HTTP request metrics

### 2. Wallet Service - Business Metrics (`wallet-business-metrics.json`)
- **Purpose**: Business-focused metrics for understanding money flow and operations
- **Key Metrics**:
  - Total money deposited, withdrawn, and transferred
  - Money flow rates over time
  - Operations distribution (pie chart)
  - Hourly operations volume
  - Net money flow

### 3. Wallet Service - Technical Metrics (`wallet-technical-metrics.json`)
- **Purpose**: Technical implementation details (CQRS, Outbox pattern, performance)
- **Key Metrics**:
  - CQRS bus metrics (commands, queries, errors)
  - Outbox pattern metrics (events created, published, failed)
  - Response time percentiles
  - HTTP status codes distribution
  - Throughput vs latency correlation

### 4. Wallet Service - Infrastructure (`wallet-infrastructure-metrics.json`)
- **Purpose**: JVM and system-level monitoring
- **Key Metrics**:
  - JVM heap and non-heap memory usage
  - Garbage collection metrics
  - Thread count and states
  - Database connection pool metrics
  - CPU usage and system load
  - File descriptors usage

### 5. Wallet Service - Golden Metrics (`wallet-golden-metrics.json`) ⭐
- **Purpose**: The Four Golden Signals for SRE monitoring
- **Key Metrics**:
  - 🚀 **Latency** - P50/P95/P99 response times with SLI thresholds
  - 📈 **Traffic** - Requests per second across all operations
  - ❌ **Errors** - Error rates (HTTP 4xx/5xx + business logic errors)
  - ⚡ **Saturation** - Resource utilization (CPU, memory, DB pool)
  - 🎯 **SLO Compliance** - Real-time SLI vs SLO monitoring

## Setup Instructions

### Option 1: Manual Import
1. Access Grafana at `http://localhost:3000` (admin/admin)
2. Go to "+" → "Import"
3. Copy and paste the JSON content from each dashboard file
4. Configure the Prometheus datasource (should be auto-detected)

### Option 2: Automatic Provisioning (Recommended)

1. **Update docker-compose.yml** to include dashboard provisioning:

```yaml
grafana:
  image: grafana/grafana:10.0.3
  container_name: wallet-grafana
  restart: unless-stopped
  ports:
    - "3000:3000"
  volumes:
    - grafana-data:/var/lib/grafana
    - ./grafana/dashboards:/var/lib/grafana/dashboards/wallet-service:ro
    - ./grafana/provisioning:/etc/grafana/provisioning:ro
  networks:
    - wallet-network
  depends_on:
    - prometheus
  environment:
    - GF_SECURITY_ADMIN_PASSWORD=admin
```

2. **Restart the Grafana container**:
```bash
docker-compose restart grafana
```

3. **Access Grafana**: The dashboards will be automatically loaded in the "Wallet Service" folder.

## Metrics Dependencies

These dashboards expect the following metrics to be available from your Wallet Service:

### Business Metrics
- `wallet_operations_created_total`
- `wallet_operations_deposits_total`
- `wallet_operations_withdrawals_total`
- `wallet_operations_transfers_total`
- `wallet_operations_queries_total`
- `wallet_operations_failed_total`
- `wallet_money_deposited_total`
- `wallet_money_withdrawn_total`
- `wallet_money_transferred_total`

### Technical Metrics
- `wallet_cqrs_commands_dispatched_total`
- `wallet_cqrs_queries_dispatched_total`
- `wallet_cqrs_bus_errors_total`
- `wallet_outbox_events_created_total`
- `wallet_outbox_events_published_total`
- `wallet_outbox_events_failed_total`

### Performance Metrics
- `wallet_operations_*_duration_seconds` (Timer metrics)
- `http_server_requests_seconds_*` (HTTP metrics)

### Infrastructure Metrics
- `jvm_memory_*` (JVM memory metrics)
- `jvm_gc_*` (Garbage collection metrics)
- `jvm_threads_*` (Thread metrics)
- `hikaricp_connections_*` (Database connection pool)
- `process_cpu_usage` (CPU usage)
- `system_load_average_1m` (System load)

## Customization

### Time Ranges
- Overview: Last 1 hour (5s refresh)
- Business Metrics: Last 6 hours (5s refresh)
- Technical Metrics: Last 1 hour (5s refresh)
- Infrastructure: Last 1 hour (5s refresh)

### Alerts
You can add alerts to any of these dashboards by:
1. Editing a panel
2. Going to the "Alert" tab
3. Setting up alert conditions
4. Configuring notification channels

### Variables
Consider adding template variables for:
- Environment (dev, staging, prod)
- Instance (if running multiple instances)
- Time range selection

## Troubleshooting

### Dashboard Not Loading
1. Check that Prometheus is running and accessible
2. Verify the datasource configuration
3. Ensure the wallet service is exposing metrics at `/metrics`

### Missing Metrics
1. Verify that the `WalletMetrics` class is properly injected
2. Check that metrics are being recorded in the application code
3. Confirm Prometheus is scraping the correct endpoint

### Performance Issues
1. Reduce the refresh rate if needed
2. Adjust time ranges for better performance
3. Consider using recording rules in Prometheus for complex queries

## Dashboard URLs
Once imported, you can access the dashboards at:
- Overview: `http://localhost:3000/d/wallet-service-overview`
- Business: `http://localhost:3000/d/wallet-business-metrics`
- Technical: `http://localhost:3000/d/wallet-technical-metrics`
- Infrastructure: `http://localhost:3000/d/wallet-infrastructure-metrics`
- **Golden Metrics (SRE)**: `http://localhost:3000/d/wallet-golden-metrics` ⭐
