# AWS Cloud Infrastructure

> Cloud-native, scalable infrastructure for the Wallet Service supporting 10M users and 5000+ TPS

## 🎯 Architecture Overview

This infrastructure supports:
- **10 million users** with room for growth
- **5,000+ TPS** sustained throughput (15,000 TPS peak)
- **Multi-AZ deployment** for high availability
- **Separate staging and production** environments
- **Brazil-focused deployment** (São Paulo region)

## 📁 Directory Structure

```
infra/aws/
├── README.md                    # This file
├── environments/
│   ├── staging/                 # Staging environment (minimal but representative)
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   └── terraform.tfvars.example
│   └── production/              # Production environment (full scale)
│       ├── main.tf
│       ├── variables.tf
│       ├── outputs.tf
│       └── terraform.tfvars.example
├── modules/                     # Reusable Terraform modules
│   ├── networking/              # VPC, subnets, security groups
│   ├── database/                # RDS Aurora MySQL cluster
│   ├── cache/                   # ElastiCache Redis cluster
│   ├── messaging/               # MSK (Kafka) cluster
│   ├── compute/                 # ECS Fargate services
│   ├── monitoring/              # CloudWatch, Grafana, Prometheus
│   ├── security/                # IAM roles, KMS keys
│   └── storage/                 # S3 buckets for backups/logs
├── scripts/
│   ├── deploy.sh                # Deployment automation
│   ├── destroy.sh               # Environment cleanup
│   └── migrate.sh               # Database migration runner
└── docs/
    ├── DEPLOYMENT.md            # Deployment guide
    ├── MONITORING.md            # Monitoring setup
    ├── SECURITY.md              # Security considerations
    └── COST-OPTIMIZATION.md     # Cost management strategies
```

## 🌎 Regional Strategy

**Primary Region**: `sa-east-1` (São Paulo)
- **Rationale**: Lowest latency for Brazilian users
- **Compliance**: Data residency requirements
- **Availability**: 3 AZs for high availability

**Backup Region**: `us-east-1` (N. Virginia)
- **Purpose**: Disaster recovery and backup storage
- **Cost**: Lower storage costs for long-term backups

## 🏛️ Architecture Components

### Production Environment

| Component | Service | Instance Type | Quantity | Purpose |
|-----------|---------|---------------|----------|---------|
| **Application** | ECS Fargate | 4 vCPU, 8GB RAM | 6-20 tasks | Auto-scaling application containers |
| **Database** | RDS Aurora MySQL | r6g.2xlarge | 1 writer + 2 readers | Primary data store with read replicas |
| **Cache** | ElastiCache Redis | r6g.large | 3 nodes | Session cache and query acceleration |
| **Message Queue** | MSK (Kafka) | kafka.m5.large | 3 brokers | Event streaming and async processing |
| **Load Balancer** | ALB | - | 1 | Traffic distribution and SSL termination |
| **Monitoring** | Managed Grafana | - | 1 workspace | Observability and alerting |

### Staging Environment

| Component | Service | Instance Type | Quantity | Purpose |
|-----------|---------|---------------|----------|---------|
| **Application** | ECS Fargate | 2 vCPU, 4GB RAM | 2-4 tasks | Scaled-down application |
| **Database** | RDS Aurora MySQL | r6g.large | 1 writer + 1 reader | Smaller but representative setup |
| **Cache** | ElastiCache Redis | r6g.medium | 1 node | Basic caching functionality |
| **Message Queue** | MSK (Kafka) | kafka.t3.small | 1 broker | Event streaming testing |
| **Load Balancer** | ALB | - | 1 | Same configuration as prod |
| **Monitoring** | Managed Grafana | - | 1 workspace | Full monitoring capabilities |

## 💰 Cost Estimates

### Production Environment (Monthly)
- **Compute (ECS)**: ~$800-2,400 (auto-scaling)
- **Database (Aurora)**: ~$1,200
- **Cache (Redis)**: ~$400
- **Messaging (MSK)**: ~$600
- **Networking (ALB, NAT)**: ~$200
- **Monitoring**: ~$300
- **Storage & Backup**: ~$200
- **Total**: ~$3,700-5,300/month

### Staging Environment (Monthly)
- **Total**: ~$800-1,200/month (20-25% of production)

## 🚀 Quick Start

1. **Prerequisites**
   ```bash
   # Install required tools
   brew install terraform awscli
   
   # Configure AWS credentials
   aws configure
   ```

2. **Deploy Staging**
   ```bash
   cd infra/aws/environments/staging
   terraform init
   terraform plan
   terraform apply
   ```

3. **Deploy Production**
   ```bash
   cd infra/aws/environments/production
   terraform init
   terraform plan
   terraform apply
   ```

## 🔒 Security Features

- **Network Isolation**: Private subnets for all backend services
- **Encryption**: At-rest and in-transit encryption for all data
- **IAM**: Least-privilege access with service-specific roles
- **Secrets Management**: AWS Secrets Manager for credentials
- **WAF**: Web Application Firewall for API protection
- **VPC Flow Logs**: Network traffic monitoring

## 📊 Monitoring & Observability

- **Application Metrics**: Custom metrics via Micrometer/Prometheus
- **Infrastructure Metrics**: CloudWatch for AWS resources
- **Distributed Tracing**: AWS X-Ray integration
- **Log Aggregation**: CloudWatch Logs with structured logging
- **Alerting**: CloudWatch Alarms + SNS notifications
- **Dashboards**: Grafana dashboards for business and technical metrics

## 🔄 CI/CD Integration

- **GitHub Actions**: Automated deployments
- **ECR**: Container image registry
- **Blue/Green Deployments**: Zero-downtime deployments
- **Database Migrations**: Automated schema updates
- **Rollback Capability**: Quick rollback on deployment issues

## 📈 Scaling Strategy

### Horizontal Scaling
- **Application**: ECS auto-scaling based on CPU/memory/request count
- **Database**: Aurora read replicas (up to 15)
- **Cache**: Redis cluster mode for data sharding

### Vertical Scaling
- **Database**: Upgrade instance types during maintenance windows
- **Application**: Increase task CPU/memory allocation

### Geographic Scaling
- **Multi-Region**: Future expansion to other AWS regions
- **CDN**: CloudFront for static content delivery

## 🛡️ Disaster Recovery

- **RTO**: 30 minutes (Recovery Time Objective)
- **RPO**: 5 minutes (Recovery Point Objective)
- **Backup Strategy**: 
  - Automated daily snapshots
  - Point-in-time recovery (35 days)
  - Cross-region backup replication
- **Failover**: Automated failover for Aurora clusters

## 📚 Next Steps

1. Review and customize `terraform.tfvars.example` files
2. Set up AWS credentials and permissions
3. Deploy staging environment first
4. Run integration tests against staging
5. Deploy production environment
6. Configure monitoring and alerting
7. Set up CI/CD pipelines

## 🤝 Support

For questions or issues:
- Check the `docs/` directory for detailed guides
- Review Terraform module documentation
- Consult AWS best practices documentation
