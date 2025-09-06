# AWS Deployment Guide

> Complete guide for deploying the Wallet Service to AWS cloud infrastructure

## 🎯 Overview

This guide covers the deployment of a production-ready wallet service infrastructure on AWS, designed to support:
- **10 million users**
- **5,000+ TPS** sustained throughput
- **High availability** across multiple AZs
- **Auto-scaling** based on demand
- **Comprehensive monitoring** and alerting

## 📋 Prerequisites

### Required Tools
```bash
# Install AWS CLI
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# Install Terraform
wget https://releases.hashicorp.com/terraform/1.6.0/terraform_1.6.0_linux_amd64.zip
unzip terraform_1.6.0_linux_amd64.zip
sudo mv terraform /usr/local/bin/

# Verify installations
aws --version
terraform --version
```

### AWS Account Setup
1. **AWS Account** with appropriate permissions
2. **IAM User** with programmatic access
3. **Required IAM Permissions**:
   - EC2, VPC, RDS, ElastiCache, MSK, ECS, ALB
   - IAM, KMS, Secrets Manager, CloudWatch
   - S3, SNS for logging and alerts

### AWS CLI Configuration
```bash
# Configure AWS credentials
aws configure

# Verify access
aws sts get-caller-identity
```

## 🏗️ Infrastructure Components

### Production Environment
| Component | Service | Configuration | Purpose |
|-----------|---------|---------------|---------|
| **Compute** | ECS Fargate | 6-20 tasks, 4 vCPU, 8GB RAM | Application hosting |
| **Database** | Aurora MySQL | r6g.2xlarge, 1+2 instances | Primary data store |
| **Cache** | ElastiCache Redis | r6g.large, 3 nodes | Performance optimization |
| **Messaging** | MSK Kafka | m5.large, 3 brokers | Event streaming |
| **Load Balancer** | ALB | Multi-AZ | Traffic distribution |
| **Monitoring** | CloudWatch + Grafana | Full observability | Monitoring & alerting |

### Staging Environment
- **25% of production scale** for cost optimization
- **Same architecture** for realistic testing
- **Simplified security** for easier development

## 🚀 Deployment Steps

### Step 1: Prepare Configuration

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd wallet-service/infra/aws
   ```

2. **Choose environment** (staging or production):
   ```bash
   cd environments/staging  # or environments/production
   ```

3. **Configure variables**:
   ```bash
   cp terraform.tfvars.example terraform.tfvars
   # Edit terraform.tfvars with your specific values
   ```

### Step 2: Deploy Infrastructure

#### Option A: Using the Deployment Script (Recommended)
```bash
# Deploy staging environment
../../../scripts/deploy.sh --environment staging

# Deploy production environment
../../../scripts/deploy.sh --environment production --auto-approve
```

#### Option B: Manual Terraform Commands
```bash
# Initialize Terraform
terraform init

# Plan deployment
terraform plan -out=tfplan

# Apply changes
terraform apply tfplan
```

### Step 3: Verify Deployment

1. **Check application health**:
   ```bash
   curl $(terraform output -raw application_url)/q/health
   ```

2. **Access Swagger UI**:
   ```bash
   open $(terraform output -raw application_url)/q/swagger-ui
   ```

3. **Monitor infrastructure**:
   ```bash
   # CloudWatch Dashboard
   open $(terraform output -raw cloudwatch_dashboard_url)
   
   # Grafana Workspace
   open $(terraform output -raw grafana_workspace_endpoint)
   ```

## 🔧 Configuration Details

### Environment Variables

#### Required Configuration
```bash
# In terraform.tfvars
aws_region = "sa-east-1"
environment = "production"
owner = "wallet-team"

# Alert emails
alert_email_addresses = [
  "devops@yourcompany.com",
  "wallet-team@yourcompany.com"
]

# Container image
container_image = "your-account.dkr.ecr.sa-east-1.amazonaws.com/wallet-service:latest"
```

#### Database Configuration
```bash
# Production settings
database_writer_instance_class = "db.r6g.2xlarge"
database_reader_instance_class = "db.r6g.2xlarge"
database_reader_count = 2
database_backup_retention_period = 35
database_max_connections = "2000"
```

#### Application Configuration
```bash
# Production scaling
app_cpu = 4096                    # 4 vCPU
app_memory = 8192                 # 8 GB RAM
app_desired_count = 6             # Initial instances
app_min_capacity = 3              # Minimum instances
app_max_capacity = 20             # Maximum instances
```

### Network Configuration

#### VPC and Subnets
```bash
# Production network layout
vpc_cidr = "10.0.0.0/16"

# Public subnets (ALB, NAT Gateways)
public_subnet_cidrs = [
  "10.0.1.0/24",   # sa-east-1a
  "10.0.2.0/24",   # sa-east-1b
  "10.0.3.0/24"    # sa-east-1c
]

# Private subnets (Application, Cache, Kafka)
private_subnet_cidrs = [
  "10.0.11.0/24",  # sa-east-1a
  "10.0.12.0/24",  # sa-east-1b
  "10.0.13.0/24"   # sa-east-1c
]

# Database subnets (Isolated)
database_subnet_cidrs = [
  "10.0.21.0/24",  # sa-east-1a
  "10.0.22.0/24",  # sa-east-1b
  "10.0.23.0/24"   # sa-east-1c
]
```

## 📊 Monitoring and Observability

### CloudWatch Dashboards
- **Application Metrics**: Request count, response times, error rates
- **Infrastructure Metrics**: CPU, memory, network utilization
- **Database Metrics**: Connection count, query performance, replication lag
- **Business Metrics**: Transaction volume, wallet operations

### Alerting Rules
- **High Error Rate**: >1% 4xx/5xx responses
- **High Latency**: >500ms P95 response time
- **Database Issues**: High CPU, connection exhaustion
- **Infrastructure**: High CPU/memory utilization

### Log Aggregation
- **Application Logs**: Structured JSON logs in CloudWatch
- **Infrastructure Logs**: VPC Flow Logs, ALB access logs
- **Database Logs**: Slow query logs, error logs
- **Kafka Logs**: Broker logs, topic metrics

## 🔒 Security Considerations

### Network Security
- **Private Subnets**: All backend services in private subnets
- **Security Groups**: Least-privilege access rules
- **NACLs**: Additional network-level protection
- **VPC Flow Logs**: Network traffic monitoring

### Data Protection
- **Encryption at Rest**: All data encrypted using KMS
- **Encryption in Transit**: TLS for all communications
- **Secrets Management**: AWS Secrets Manager for credentials
- **Key Rotation**: Automated key rotation policies

### Access Control
- **IAM Roles**: Service-specific roles with minimal permissions
- **Resource Tags**: Consistent tagging for access control
- **CloudTrail**: API call logging and monitoring

## 💰 Cost Optimization

### Production Costs (Monthly)
- **Compute**: $800-2,400 (auto-scaling)
- **Database**: $1,200
- **Cache**: $400
- **Messaging**: $600
- **Networking**: $200
- **Monitoring**: $300
- **Storage**: $200
- **Total**: $3,700-5,300

### Cost Optimization Strategies
1. **Reserved Instances**: 30-60% savings for predictable workloads
2. **Spot Instances**: For non-critical batch processing
3. **Scheduled Scaling**: Scale down during low-traffic periods
4. **Storage Optimization**: Lifecycle policies for logs and backups
5. **Regular Reviews**: Monthly cost analysis and optimization

### Staging Costs (Monthly)
- **Total**: $800-1,200 (20-25% of production)
- **Cost Savings**: Smaller instances, shorter retention, simplified setup

## 🔄 CI/CD Integration

### Container Registry
```bash
# Build and push container image
docker build -t wallet-service .
docker tag wallet-service:latest $ECR_REGISTRY/wallet-service:latest
docker push $ECR_REGISTRY/wallet-service:latest
```

### Deployment Pipeline
1. **Build**: Compile application and create container image
2. **Test**: Run unit tests and integration tests
3. **Deploy Staging**: Deploy to staging environment
4. **Integration Tests**: Run tests against staging
5. **Deploy Production**: Deploy to production with blue/green strategy

### GitHub Actions Example
```yaml
name: Deploy to AWS
on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Deploy to Staging
        run: |
          cd infra/aws
          ./scripts/deploy.sh --environment staging --auto-approve
      - name: Run Integration Tests
        run: |
          # Run integration tests against staging
      - name: Deploy to Production
        if: success()
        run: |
          cd infra/aws
          ./scripts/deploy.sh --environment production --auto-approve
```

## 🛠️ Troubleshooting

### Common Issues

#### Terraform Errors
```bash
# State lock issues
terraform force-unlock <lock-id>

# State corruption
terraform refresh
terraform plan

# Module issues
terraform init -upgrade
```

#### Application Issues
```bash
# Check ECS service status
aws ecs describe-services --cluster production-wallet-cluster --services production-wallet-service

# Check application logs
aws logs tail /aws/ecs/wallet-service-production --follow

# Check health endpoint
curl https://your-alb-url/q/health
```

#### Database Issues
```bash
# Check RDS cluster status
aws rds describe-db-clusters --db-cluster-identifier production-wallet-cluster

# Check database connections
aws rds describe-db-cluster-endpoints --db-cluster-identifier production-wallet-cluster

# Monitor slow queries
aws logs filter-log-events --log-group-name /aws/rds/cluster/production-wallet-cluster/slowquery
```

### Recovery Procedures

#### Application Recovery
1. **Check auto-scaling**: Verify ECS service is scaling properly
2. **Manual scaling**: Increase desired count if needed
3. **Rollback**: Deploy previous container image version
4. **Database failover**: Trigger Aurora failover if needed

#### Infrastructure Recovery
1. **Multi-AZ**: Services automatically failover to healthy AZs
2. **Database**: Aurora automatic failover (30-120 seconds)
3. **Cache**: ElastiCache automatic failover
4. **Load Balancer**: Health checks route traffic to healthy instances

## 📚 Additional Resources

### Documentation
- [AWS Well-Architected Framework](https://aws.amazon.com/architecture/well-architected/)
- [Aurora MySQL Best Practices](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/Aurora.BestPractices.html)
- [ECS Best Practices](https://docs.aws.amazon.com/AmazonECS/latest/bestpracticesguide/)

### Monitoring
- [CloudWatch Best Practices](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cloudwatch_architecture.html)
- [Grafana Documentation](https://grafana.com/docs/)

### Security
- [AWS Security Best Practices](https://aws.amazon.com/architecture/security-identity-compliance/)
- [VPC Security Best Practices](https://docs.aws.amazon.com/vpc/latest/userguide/vpc-security-best-practices.html)

## 🤝 Support

For deployment issues or questions:
1. Check the troubleshooting section above
2. Review AWS CloudWatch logs and metrics
3. Consult the team documentation
4. Contact the DevOps team for infrastructure issues
