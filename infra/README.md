# 🏗️ Wallet Service Infrastructure

Enterprise-grade AWS infrastructure for the Wallet Service using Terraform.

## 📊 **Scale Requirements**
- **Users**: 10 million
- **TPS**: 5,000+ transactions per second
- **Region**: Brazil (sa-east-1)
- **Environments**: Staging + Production

## 📁 **Structure**

```
infra/
├── environments/          # Environment-specific configurations
│   ├── staging/          # Minimal cost, full functionality
│   └── production/       # Full scale, high availability
├── modules/              # Reusable Terraform modules
│   ├── networking/       # VPC, subnets, security groups
│   ├── security/         # IAM, WAF, certificates
│   ├── compute/          # EKS cluster and node groups
│   ├── database/         # Aurora MySQL Serverless v2
│   ├── cache/            # ElastiCache Redis
│   ├── messaging/        # MSK Kafka
│   └── monitoring/       # CloudWatch, Prometheus, Grafana
├── scripts/              # Deployment and management scripts
└── docs/                 # Architecture documentation
```

## 🚀 **Quick Start**

### Prerequisites
- AWS CLI configured
- Terraform >= 1.5
- kubectl
- helm

### Deploy Staging
```bash
cd environments/staging
terraform init
terraform plan
terraform apply
```

### Deploy Production
```bash
cd environments/production
terraform init
terraform plan
terraform apply
```

## 💰 **Cost Estimates**

| Environment | Monthly Cost (USD) | Description |
|-------------|-------------------|-------------|
| **Staging** | ~$365 | Minimal resources, all components |
| **Production** | ~$8,000 | Full scale, 5000+ TPS capacity |

## 🏗️ **Architecture Overview**

### Production Infrastructure
- **EKS**: 10-50 nodes (c5.2xlarge)
- **Aurora**: 8-64 ACUs (Serverless v2)
- **ElastiCache**: 3 nodes (r6g.2xlarge)
- **MSK**: 6 brokers (m5.2xlarge)

### Staging Infrastructure  
- **EKS**: 2 nodes (t3.large)
- **Aurora**: 0.5-2 ACUs (Serverless v2)
- **ElastiCache**: 1 node (t3.micro)
- **MSK**: 3 brokers (t3.small)

## 📋 **Deployment Checklist**

- [ ] AWS credentials configured
- [ ] Domain and SSL certificates ready
- [ ] Monitoring alerts configured
- [ ] Backup strategies implemented
- [ ] Disaster recovery tested
- [ ] Security review completed
- [ ] Load testing performed
- [ ] Documentation updated
