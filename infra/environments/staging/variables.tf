# =============================================================================
# STAGING ENVIRONMENT VARIABLES
# =============================================================================

# =============================================================================
# GENERAL CONFIGURATION
# =============================================================================

variable "aws_region" {
  description = "AWS region for deployment"
  type        = string
  default     = "sa-east-1"  # São Paulo, Brazil
}

variable "domain_name" {
  description = "Domain name for the wallet service"
  type        = string
  default     = "staging-wallet.example.com"
}

# =============================================================================
# SCALING CONFIGURATION (MINIMAL)
# =============================================================================

variable "target_tps" {
  description = "Target transactions per second (staging)"
  type        = number
  default     = 100
}

variable "expected_users" {
  description = "Expected number of users (staging)"
  type        = number
  default     = 1000
}

# =============================================================================
# COMPUTE CONFIGURATION (COST OPTIMIZED)
# =============================================================================

variable "eks_node_instance_types" {
  description = "Instance types for EKS nodes (cost optimized)"
  type        = list(string)
  default     = ["t3.large"]
}

variable "min_nodes" {
  description = "Minimum number of EKS nodes"
  type        = number
  default     = 2
}

variable "max_nodes" {
  description = "Maximum number of EKS nodes"
  type        = number
  default     = 4
}

# =============================================================================
# DATABASE CONFIGURATION (MINIMAL)
# =============================================================================

variable "aurora_min_capacity" {
  description = "Minimum Aurora Serverless v2 capacity (ACUs)"
  type        = number
  default     = 0.5
}

variable "aurora_max_capacity" {
  description = "Maximum Aurora Serverless v2 capacity (ACUs)"
  type        = number
  default     = 2
}

variable "aurora_backup_retention_days" {
  description = "Number of days to retain Aurora backups"
  type        = number
  default     = 7
}

# =============================================================================
# CACHE CONFIGURATION (MINIMAL)
# =============================================================================

variable "redis_node_type" {
  description = "ElastiCache Redis node type"
  type        = string
  default     = "t3.micro"
}

variable "redis_num_cache_nodes" {
  description = "Number of Redis cache nodes"
  type        = number
  default     = 1
}

# =============================================================================
# MESSAGING CONFIGURATION (MINIMAL)
# =============================================================================

variable "kafka_instance_type" {
  description = "MSK Kafka instance type"
  type        = string
  default     = "t3.small"
}

variable "kafka_number_of_broker_nodes" {
  description = "Number of Kafka broker nodes"
  type        = number
  default     = 2
}

variable "kafka_ebs_volume_size" {
  description = "EBS volume size for Kafka brokers (GB)"
  type        = number
  default     = 100
}

# =============================================================================
# MONITORING CONFIGURATION (BASIC)
# =============================================================================

variable "log_retention_days" {
  description = "CloudWatch log retention period in days"
  type        = number
  default     = 7
}

variable "enable_performance_insights" {
  description = "Enable Performance Insights for Aurora"
  type        = bool
  default     = false  # Disabled for cost
}

variable "enable_enhanced_monitoring" {
  description = "Enable enhanced monitoring for RDS"
  type        = bool
  default     = false  # Disabled for cost
}

# =============================================================================
# SECURITY CONFIGURATION
# =============================================================================

variable "enable_encryption_at_rest" {
  description = "Enable encryption at rest for all services"
  type        = bool
  default     = true  # Still enabled for consistency
}

variable "enable_encryption_in_transit" {
  description = "Enable encryption in transit for all services"
  type        = bool
  default     = true  # Still enabled for consistency
}

# =============================================================================
# COST OPTIMIZATION (AGGRESSIVE)
# =============================================================================

variable "enable_spot_instances" {
  description = "Enable spot instances for cost savings"
  type        = bool
  default     = true  # Enabled for staging cost savings
}

variable "enable_scheduled_scaling" {
  description = "Enable scheduled scaling based on business hours"
  type        = bool
  default     = true
}

# =============================================================================
# TAGS
# =============================================================================

variable "additional_tags" {
  description = "Additional tags to apply to all resources"
  type        = map(string)
  default = {
    CostOptimized = "true"
    AutoShutdown  = "enabled"
  }
}
