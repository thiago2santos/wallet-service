# Outputs for staging environment

# Networking Outputs
output "vpc_id" {
  description = "ID of the VPC"
  value       = module.networking.vpc_id
}

output "public_subnet_ids" {
  description = "IDs of the public subnets"
  value       = module.networking.public_subnet_ids
}

output "private_subnet_ids" {
  description = "IDs of the private subnets"
  value       = module.networking.private_subnet_ids
}

# Database Outputs
output "database_endpoint" {
  description = "RDS cluster endpoint (writer)"
  value       = module.database.cluster_endpoint
}

output "database_reader_endpoint" {
  description = "RDS cluster reader endpoint"
  value       = module.database.cluster_reader_endpoint
}

output "database_port" {
  description = "RDS cluster port"
  value       = module.database.cluster_port
}

output "database_name" {
  description = "Database name"
  value       = module.database.database_name
}

output "database_secret_arn" {
  description = "ARN of the secret containing database credentials"
  value       = module.database.secret_arn
}

# Cache Outputs
output "redis_endpoint" {
  description = "Redis primary endpoint"
  value       = module.cache.primary_endpoint
}

output "redis_port" {
  description = "Redis port"
  value       = module.cache.port
}

# Messaging Outputs
output "kafka_bootstrap_brokers" {
  description = "Kafka bootstrap brokers"
  value       = module.messaging.bootstrap_brokers
}

output "kafka_zookeeper_connect_string" {
  description = "Kafka Zookeeper connection string"
  value       = module.messaging.zookeeper_connect_string
}

# Application Outputs
output "application_url" {
  description = "Application Load Balancer URL"
  value       = "http://${module.compute.alb_dns_name}"
}

output "alb_dns_name" {
  description = "Application Load Balancer DNS name"
  value       = module.compute.alb_dns_name
}

output "alb_zone_id" {
  description = "Application Load Balancer zone ID"
  value       = module.compute.alb_zone_id
}

output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = module.compute.cluster_name
}

output "ecs_service_name" {
  description = "ECS service name"
  value       = module.compute.service_name
}

# Monitoring Outputs
output "grafana_workspace_id" {
  description = "Grafana workspace ID"
  value       = module.monitoring.grafana_workspace_id
}

output "grafana_workspace_endpoint" {
  description = "Grafana workspace endpoint"
  value       = module.monitoring.grafana_workspace_endpoint
}

output "cloudwatch_dashboard_url" {
  description = "CloudWatch dashboard URL"
  value       = "https://${var.aws_region}.console.aws.amazon.com/cloudwatch/home?region=${var.aws_region}#dashboards:name=${aws_cloudwatch_dashboard.main.dashboard_name}"
}

# Security Outputs
output "sns_alerts_topic_arn" {
  description = "SNS topic ARN for alerts"
  value       = aws_sns_topic.alerts.arn
}

# Connection Information
output "connection_info" {
  description = "Connection information for the wallet service"
  value = {
    application_url = "http://${module.compute.alb_dns_name}"
    api_base_url   = "http://${module.compute.alb_dns_name}/api/v1"
    health_check   = "http://${module.compute.alb_dns_name}/q/health"
    metrics        = "http://${module.compute.alb_dns_name}/metrics"
    swagger_ui     = "http://${module.compute.alb_dns_name}/q/swagger-ui"
  }
}

# Environment Summary
output "environment_summary" {
  description = "Summary of the deployed environment"
  value = {
    environment     = var.environment
    region         = var.aws_region
    vpc_id         = module.networking.vpc_id
    database_type  = "Aurora MySQL"
    cache_type     = "ElastiCache Redis"
    messaging_type = "MSK Kafka"
    compute_type   = "ECS Fargate"
    monitoring     = "CloudWatch + Grafana"
    cost_optimized = true
  }
}
