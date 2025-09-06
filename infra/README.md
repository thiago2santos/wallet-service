# 🏗️ Infrastructure Organization

This directory contains all infrastructure-related configurations, scripts, and documentation for the Wallet Service project.

## 📁 Structure Overview

```
infra/
├── local-dev/              # Local development infrastructure
│   ├── docker-compose.yml  # Main services (MySQL, Redis, Kafka, Grafana, Prometheus)
│   ├── docker-compose.loadtest.yml  # Load testing environment
│   ├── grafana/            # Monitoring dashboards and provisioning
│   ├── kafka/              # Message broker schemas and configuration
│   ├── mysql/              # Database initialization scripts
│   └── prometheus.yml      # Metrics collection configuration
├── performance/            # Load testing and performance monitoring
├── scripts/               # Deployment and operational scripts
├── environments/          # Cloud infrastructure definitions
│   ├── staging/           # Staging environment Terraform
│   └── production/        # Production environment Terraform
├── modules/               # Reusable Terraform modules
└── docs/                  # Infrastructure documentation
```

## 🚀 Quick Start

### Local Development
```bash
# Start the complete local infrastructure
cd infra/local-dev
docker-compose up -d

# For load testing environment
docker-compose -f docker-compose.loadtest.yml up -d
```

### Performance Testing
```bash
# Run comprehensive load tests
cd infra/performance
./scripts/shell/setup-load-test.sh
```

### AWS Deployment (Coming Soon)
```bash
# Deploy to staging
cd infra/environments/staging
terraform init && terraform apply

# Deploy to production
cd infra/environments/production
terraform init && terraform apply
```

## 🎯 Infrastructure Components

### Local Development Stack
- **MySQL 8.0**: Primary/Replica database setup with initialization scripts
- **Redis 7**: Caching layer for high-performance operations
- **Apache Kafka**: Event streaming and audit trail
- **Prometheus**: Metrics collection and monitoring
- **Grafana**: Observability dashboards and alerting

### AWS Production Architecture (Planned)
- **Aurora MySQL Serverless v2**: Managed database with auto-scaling
- **ElastiCache Redis**: Managed caching layer
- **MSK (Managed Kafka)**: Event streaming service
- **ECS Fargate**: Containerized application hosting
- **Application Load Balancer**: Traffic distribution
- **CloudWatch**: Monitoring and logging

## 📊 Monitoring & Observability

Access the monitoring stack:
- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090

Available dashboards:
- Business Metrics | Infrastructure Metrics | Technical Metrics | Service Overview

## 🔧 Performance Testing

The performance testing suite includes:
- **Load Testing**: Sustained traffic simulation
- **Stress Testing**: Breaking point identification  
- **Containerized Testing**: Isolated test environments
- **Real-time Monitoring**: Live performance metrics

## 📚 Documentation

For detailed information, see:
- `docs/DEPLOYMENT_GUIDE.md` - Step-by-step deployment instructions
- `performance/README.md` - Performance testing guide
- `local-dev/grafana/README.md` - Monitoring setup guide

## 🏛️ Architecture Principles

This infrastructure follows:
- **AWS Well-Architected Framework**: Security, reliability, performance, cost optimization
- **12-Factor App**: Cloud-native application principles
- **Infrastructure as Code**: Terraform for reproducible deployments
- **Observability First**: Comprehensive monitoring and alerting
- **Financial Grade**: High availability and consistency for financial operations

---

*Built for scale, designed for reliability, optimized for performance* 🚀
