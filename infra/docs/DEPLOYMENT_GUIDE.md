# 🚀 Wallet Service AWS Deployment Guide

Complete guide for deploying the Wallet Service to AWS using Terraform.

## 📋 **Prerequisites**

### **Required Tools**
```bash
# AWS CLI
aws --version  # >= 2.0

# Terraform
terraform --version  # >= 1.5

# kubectl
kubectl version --client  # >= 1.28

# Helm
helm version  # >= 3.12
```

### **AWS Configuration**
```bash
# Configure AWS credentials
aws configure

# Verify access
aws sts get-caller-identity
```

## 🏗️ **Infrastructure Overview**

### **🎯 Production Environment**
- **Scale**: 10M users, 5000+ TPS
- **Cost**: ~$8,000/month
- **HA**: Multi-AZ deployment
- **Capacity**: Auto-scaling 10-50 nodes

### **🧪 Staging Environment**  
- **Scale**: 1K users, 100 TPS
- **Cost**: ~$365/month
- **HA**: Basic redundancy
- **Capacity**: Fixed 2-4 nodes

## 📁 **Project Structure**

```
infra/
├── environments/
│   ├── staging/          # Cost-optimized environment
│   └── production/       # Full-scale environment
├── modules/              # Reusable Terraform modules
│   ├── networking/       # VPC, subnets, security groups
│   ├── security/         # IAM, WAF, certificates
│   ├── compute/          # EKS cluster and nodes
│   ├── database/         # Aurora MySQL Serverless v2
│   ├── cache/            # ElastiCache Redis
│   ├── messaging/        # MSK Kafka
│   └── monitoring/       # CloudWatch, Prometheus
└── scripts/              # Deployment automation
```

## 🚀 **Deployment Steps**

### **Step 1: Prepare Configuration**

#### **For Staging:**
```bash
cd infra/environments/staging

# Copy example configuration
cp terraform.tfvars.example terraform.tfvars

# Edit configuration
vim terraform.tfvars
```

#### **For Production:**
```bash
cd infra/environments/production

# Copy example configuration  
cp terraform.tfvars.example terraform.tfvars

# Edit configuration (IMPORTANT: Update domain_name!)
vim terraform.tfvars
```

### **Step 2: Initialize Terraform**

```bash
# Initialize Terraform
terraform init

# Validate configuration
terraform validate

# Plan deployment
terraform plan
```

### **Step 3: Deploy Infrastructure**

```bash
# Apply infrastructure (review plan first!)
terraform apply

# Confirm with 'yes' when prompted
```

**⏱️ Deployment Time:**
- **Staging**: ~15-20 minutes
- **Production**: ~25-30 minutes

### **Step 4: Configure kubectl**

```bash
# Get cluster name from output
CLUSTER_NAME=$(terraform output -raw cluster_name)

# Configure kubectl
aws eks update-kubeconfig --region sa-east-1 --name $CLUSTER_NAME

# Verify connection
kubectl get nodes
```

### **Step 5: Deploy Application**

```bash
# Create namespace
kubectl create namespace wallet-service

# Deploy application (from project root)
cd ../../../
kubectl apply -f k8s/ -n wallet-service

# Verify deployment
kubectl get pods -n wallet-service
```

## 📊 **Post-Deployment Verification**

### **Infrastructure Health Check**
```bash
# Check EKS cluster
kubectl get nodes
kubectl get pods --all-namespaces

# Check Aurora cluster
aws rds describe-db-clusters --region sa-east-1

# Check ElastiCache
aws elasticache describe-cache-clusters --region sa-east-1

# Check MSK cluster
aws kafka list-clusters --region sa-east-1
```

### **Application Health Check**
```bash
# Get load balancer URL
kubectl get ingress -n wallet-service

# Test health endpoint
curl https://your-domain.com/q/health

# Test wallet creation
curl -X POST https://your-domain.com/api/v1/wallets \
  -H "Content-Type: application/json" \
  -d '{"userId": "test-user", "currency": "USD"}'
```

## 📈 **Monitoring Setup**

### **Access Grafana**
```bash
# Get Grafana URL from Terraform output
terraform output grafana_endpoint

# Default credentials: admin/admin (change immediately!)
```

### **Configure Alerts**
```bash
# Deploy alerting rules
kubectl apply -f monitoring/alerts/ -n wallet-service

# Configure notification channels in Grafana
```

## 💰 **Cost Monitoring**

### **Set Up Cost Alerts**
```bash
# Create cost budget
aws budgets create-budget --account-id $(aws sts get-caller-identity --query Account --output text) \
  --budget file://scripts/cost-budget.json
```

### **Daily Cost Check**
```bash
# Check current month costs
aws ce get-cost-and-usage \
  --time-period Start=2024-01-01,End=2024-01-31 \
  --granularity MONTHLY \
  --metrics BlendedCost
```

## 🔧 **Maintenance Operations**

### **Scaling Operations**
```bash
# Scale EKS nodes
kubectl scale deployment wallet-service --replicas=50 -n wallet-service

# Scale Aurora (automatic based on load)
# Scale ElastiCache (requires Terraform change)
```

### **Backup Operations**
```bash
# Aurora backups are automatic
# Manual snapshot
aws rds create-db-cluster-snapshot \
  --db-cluster-identifier wallet-prod-aurora \
  --db-cluster-snapshot-identifier manual-snapshot-$(date +%Y%m%d)
```

### **Update Operations**
```bash
# Update infrastructure
terraform plan
terraform apply

# Update application
kubectl set image deployment/wallet-service wallet-service=new-image:tag -n wallet-service
```

## 🚨 **Disaster Recovery**

### **Backup Strategy**
- **Aurora**: 35-day automated backups + manual snapshots
- **Redis**: No persistence (cache only)
- **Kafka**: 7-day retention + cross-region replication
- **Application**: GitOps + container registry

### **Recovery Procedures**
```bash
# Restore Aurora from snapshot
aws rds restore-db-cluster-from-snapshot \
  --db-cluster-identifier wallet-prod-aurora-restored \
  --snapshot-identifier snapshot-id

# Redeploy application
kubectl apply -f k8s/ -n wallet-service
```

## 🔒 **Security Checklist**

- [ ] **IAM**: Least privilege access
- [ ] **VPC**: Private subnets for all services
- [ ] **Encryption**: At rest and in transit
- [ ] **WAF**: DDoS and application protection
- [ ] **Secrets**: AWS Secrets Manager
- [ ] **Monitoring**: CloudTrail and GuardDuty
- [ ] **Compliance**: PCI DSS considerations

## 🐛 **Troubleshooting**

### **Common Issues**

#### **EKS Nodes Not Ready**
```bash
# Check node status
kubectl describe nodes

# Check IAM permissions
aws iam get-role --role-name eks-node-group-role
```

#### **Aurora Connection Issues**
```bash
# Check security groups
aws ec2 describe-security-groups --group-ids sg-xxxxx

# Test connectivity from EKS
kubectl run test-pod --image=mysql:8.0 --rm -it -- mysql -h aurora-endpoint -u username -p
```

#### **Application Pods Failing**
```bash
# Check pod logs
kubectl logs -f deployment/wallet-service -n wallet-service

# Check events
kubectl get events -n wallet-service --sort-by='.lastTimestamp'
```

### **Performance Issues**

#### **High Latency**
```bash
# Check Aurora Performance Insights
# Check Redis hit ratio
# Check Kafka consumer lag
```

#### **Resource Constraints**
```bash
# Check node resources
kubectl top nodes

# Check pod resources
kubectl top pods -n wallet-service
```

## 📞 **Support Contacts**

- **Platform Team**: platform-team@company.com
- **On-Call**: +55-11-xxxx-xxxx
- **Slack**: #wallet-service-ops

## 📚 **Additional Resources**

- [AWS Well-Architected Framework](https://aws.amazon.com/architecture/well-architected/)
- [EKS Best Practices](https://aws.github.io/aws-eks-best-practices/)
- [Aurora Performance Tuning](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/Aurora.BestPractices.html)
- [MSK Best Practices](https://docs.aws.amazon.com/msk/latest/developerguide/bestpractices.html)
