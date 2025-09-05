# =============================================================================
# PRODUCTION ENVIRONMENT OUTPUTS
# =============================================================================

# =============================================================================
# NETWORKING OUTPUTS
# =============================================================================

output "vpc_id" {
  description = "ID of the VPC"
  value       = module.networking.vpc_id
}

output "vpc_cidr_block" {
  description = "CIDR block of the VPC"
  value       = module.networking.vpc_cidr_block
}

output "public_subnet_ids" {
  description = "IDs of the public subnets"
  value       = module.networking.public_subnet_ids
}

output "private_subnet_ids" {
  description = "IDs of the private subnets"
  value       = module.networking.private_subnet_ids
}

output "database_subnet_ids" {
  description = "IDs of the database subnets"
  value       = module.networking.database_subnet_ids
}

# =============================================================================
# SECURITY OUTPUTS
# =============================================================================

output "certificate_arn" {
  description = "ARN of the SSL certificate"
  value       = module.security.certificate_arn
}

output "waf_web_acl_arn" {
  description = "ARN of the WAF Web ACL"
  value       = module.security.waf_web_acl_arn
}

# =============================================================================
# COMPUTE OUTPUTS (EKS)
# =============================================================================

output "cluster_name" {
  description = "Name of the EKS cluster"
  value       = module.compute.cluster_name
}

output "cluster_endpoint" {
  description = "Endpoint for EKS control plane"
  value       = module.compute.cluster_endpoint
}

output "cluster_security_group_id" {
  description = "Security group ID attached to the EKS cluster"
  value       = module.compute.cluster_security_group_id
}

output "cluster_iam_role_arn" {
  description = "IAM role ARN associated with EKS cluster"
  value       = module.compute.cluster_iam_role_arn
}

output "node_groups" {
  description = "EKS node groups information"
  value       = module.compute.node_groups
  sensitive   = true
}

# =============================================================================
# DATABASE OUTPUTS (Aurora)
# =============================================================================

output "aurora_cluster_id" {
  description = "Aurora cluster identifier"
  value       = module.database.cluster_id
}

output "aurora_cluster_endpoint" {
  description = "Aurora cluster endpoint"
  value       = module.database.cluster_endpoint
  sensitive   = true
}

output "aurora_reader_endpoint" {
  description = "Aurora reader endpoint"
  value       = module.database.reader_endpoint
  sensitive   = true
}

output "aurora_cluster_master_username" {
  description = "Aurora cluster master username"
  value       = module.database.cluster_master_username
  sensitive   = true
}

output "aurora_cluster_port" {
  description = "Aurora cluster port"
  value       = module.database.cluster_port
}

output "aurora_security_group_id" {
  description = "Security group ID for Aurora cluster"
  value       = module.database.security_group_id
}

# =============================================================================
# CACHE OUTPUTS (ElastiCache)
# =============================================================================

output "redis_cluster_id" {
  description = "ElastiCache Redis cluster identifier"
  value       = module.cache.cluster_id
}

output "redis_primary_endpoint" {
  description = "ElastiCache Redis primary endpoint"
  value       = module.cache.primary_endpoint
  sensitive   = true
}

output "redis_configuration_endpoint" {
  description = "ElastiCache Redis configuration endpoint"
  value       = module.cache.configuration_endpoint
  sensitive   = true
}

output "redis_port" {
  description = "ElastiCache Redis port"
  value       = module.cache.port
}

# =============================================================================
# MESSAGING OUTPUTS (MSK)
# =============================================================================

output "kafka_cluster_arn" {
  description = "MSK cluster ARN"
  value       = module.messaging.cluster_arn
}

output "kafka_bootstrap_brokers" {
  description = "MSK bootstrap brokers"
  value       = module.messaging.bootstrap_brokers
  sensitive   = true
}

output "kafka_bootstrap_brokers_tls" {
  description = "MSK bootstrap brokers (TLS)"
  value       = module.messaging.bootstrap_brokers_tls
  sensitive   = true
}

output "kafka_zookeeper_connect_string" {
  description = "MSK Zookeeper connection string"
  value       = module.messaging.zookeeper_connect_string
  sensitive   = true
}

# =============================================================================
# MONITORING OUTPUTS
# =============================================================================

output "cloudwatch_log_group_names" {
  description = "CloudWatch log group names"
  value       = module.monitoring.log_group_names
}

output "prometheus_endpoint" {
  description = "Prometheus endpoint URL"
  value       = module.monitoring.prometheus_endpoint
  sensitive   = true
}

output "grafana_endpoint" {
  description = "Grafana endpoint URL"
  value       = module.monitoring.grafana_endpoint
  sensitive   = true
}

# =============================================================================
# APPLICATION CONFIGURATION OUTPUTS
# =============================================================================

output "application_config" {
  description = "Configuration values for the wallet service application"
  value = {
    # Database configuration
    database = {
      host     = module.database.cluster_endpoint
      port     = module.database.cluster_port
      username = module.database.cluster_master_username
      name     = "wallet_db"
    }
    
    # Cache configuration
    cache = {
      host = module.cache.primary_endpoint
      port = module.cache.port
    }
    
    # Messaging configuration
    messaging = {
      bootstrap_servers = module.messaging.bootstrap_brokers_tls
    }
    
    # Kubernetes configuration
    kubernetes = {
      cluster_name = module.compute.cluster_name
      endpoint     = module.compute.cluster_endpoint
    }
  }
  sensitive = true
}

# =============================================================================
# COST ESTIMATION OUTPUTS
# =============================================================================

output "estimated_monthly_cost" {
  description = "Estimated monthly cost breakdown (USD)"
  value = {
    compute = {
      description = "EKS nodes (20 x c5.2xlarge)"
      cost_usd    = 3600
    }
    database = {
      description = "Aurora Serverless v2 (avg 16 ACUs)"
      cost_usd    = 1200
    }
    cache = {
      description = "ElastiCache Redis (3 x r6g.2xlarge)"
      cost_usd    = 900
    }
    messaging = {
      description = "MSK Kafka (6 x m5.2xlarge)"
      cost_usd    = 1800
    }
    networking = {
      description = "ALB, NAT Gateway, Data Transfer"
      cost_usd    = 300
    }
    monitoring = {
      description = "CloudWatch, Logs, Metrics"
      cost_usd    = 200
    }
    total = {
      description = "Total estimated monthly cost"
      cost_usd    = 8000
    }
  }
}

# =============================================================================
# CAPACITY PLANNING OUTPUTS
# =============================================================================

output "capacity_planning" {
  description = "Capacity planning information"
  value = {
    target_tps           = var.target_tps
    expected_users       = var.expected_users
    current_pod_capacity = var.min_nodes * 10 * 250  # nodes * pods_per_node * tps_per_pod
    max_pod_capacity     = var.max_nodes * 10 * 250
    
    scaling_metrics = {
      pods_per_node = 10
      tps_per_pod   = 250
      min_nodes     = var.min_nodes
      max_nodes     = var.max_nodes
    }
  }
}

# =============================================================================
# DEPLOYMENT INFORMATION
# =============================================================================

output "deployment_info" {
  description = "Information needed for application deployment"
  value = {
    region           = var.aws_region
    environment      = "production"
    cluster_name     = module.compute.cluster_name
    namespace        = "wallet-service"
    
    kubectl_config_command = "aws eks update-kubeconfig --region ${var.aws_region} --name ${module.compute.cluster_name}"
    
    next_steps = [
      "1. Configure kubectl: aws eks update-kubeconfig --region ${var.aws_region} --name ${module.compute.cluster_name}",
      "2. Deploy application: kubectl apply -f k8s/",
      "3. Configure monitoring: helm install prometheus prometheus-community/kube-prometheus-stack",
      "4. Set up ingress: kubectl apply -f ingress/",
      "5. Configure autoscaling: kubectl apply -f hpa/"
    ]
  }
}
