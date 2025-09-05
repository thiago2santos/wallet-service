# =============================================================================
# PRODUCTION ENVIRONMENT VARIABLES
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
  default     = "wallet.example.com"
  
  validation {
    condition     = can(regex("^[a-z0-9.-]+\\.[a-z]{2,}$", var.domain_name))
    error_message = "Domain name must be a valid FQDN."
  }
}

# =============================================================================
# SCALING CONFIGURATION
# =============================================================================

variable "target_tps" {
  description = "Target transactions per second"
  type        = number
  default     = 5000
  
  validation {
    condition     = var.target_tps >= 1000 && var.target_tps <= 50000
    error_message = "Target TPS must be between 1000 and 50000."
  }
}

variable "expected_users" {
  description = "Expected number of users"
  type        = number
  default     = 10000000
  
  validation {
    condition     = var.expected_users >= 100000
    error_message = "Expected users must be at least 100,000."
  }
}

# =============================================================================
# COMPUTE CONFIGURATION
# =============================================================================

variable "eks_node_instance_types" {
  description = "Instance types for EKS nodes"
  type        = list(string)
  default     = ["c5.2xlarge"]
  
  validation {
    condition = alltrue([
      for instance_type in var.eks_node_instance_types :
      can(regex("^[a-z][0-9][a-z]?\\.(nano|micro|small|medium|large|xlarge|[0-9]+xlarge)$", instance_type))
    ])
    error_message = "All instance types must be valid AWS instance types."
  }
}

variable "min_nodes" {
  description = "Minimum number of EKS nodes"
  type        = number
  default     = 10
  
  validation {
    condition     = var.min_nodes >= 2 && var.min_nodes <= 100
    error_message = "Minimum nodes must be between 2 and 100."
  }
}

variable "max_nodes" {
  description = "Maximum number of EKS nodes"
  type        = number
  default     = 50
  
  validation {
    condition     = var.max_nodes >= 5 && var.max_nodes <= 200
    error_message = "Maximum nodes must be between 5 and 200."
  }
}

# =============================================================================
# DATABASE CONFIGURATION
# =============================================================================

variable "aurora_min_capacity" {
  description = "Minimum Aurora Serverless v2 capacity (ACUs)"
  type        = number
  default     = 8
  
  validation {
    condition     = var.aurora_min_capacity >= 0.5 && var.aurora_min_capacity <= 128
    error_message = "Aurora min capacity must be between 0.5 and 128 ACUs."
  }
}

variable "aurora_max_capacity" {
  description = "Maximum Aurora Serverless v2 capacity (ACUs)"
  type        = number
  default     = 64
  
  validation {
    condition     = var.aurora_max_capacity >= 1 && var.aurora_max_capacity <= 128
    error_message = "Aurora max capacity must be between 1 and 128 ACUs."
  }
}

variable "aurora_backup_retention_days" {
  description = "Number of days to retain Aurora backups"
  type        = number
  default     = 35
  
  validation {
    condition     = var.aurora_backup_retention_days >= 7 && var.aurora_backup_retention_days <= 35
    error_message = "Backup retention must be between 7 and 35 days."
  }
}

# =============================================================================
# CACHE CONFIGURATION
# =============================================================================

variable "redis_node_type" {
  description = "ElastiCache Redis node type"
  type        = string
  default     = "r6g.2xlarge"
  
  validation {
    condition     = can(regex("^[a-z][0-9][a-z]?\\.(nano|micro|small|medium|large|xlarge|[0-9]+xlarge)$", var.redis_node_type))
    error_message = "Redis node type must be a valid AWS instance type."
  }
}

variable "redis_num_cache_nodes" {
  description = "Number of Redis cache nodes"
  type        = number
  default     = 3
  
  validation {
    condition     = var.redis_num_cache_nodes >= 1 && var.redis_num_cache_nodes <= 20
    error_message = "Number of Redis nodes must be between 1 and 20."
  }
}

# =============================================================================
# MESSAGING CONFIGURATION
# =============================================================================

variable "kafka_instance_type" {
  description = "MSK Kafka instance type"
  type        = string
  default     = "m5.2xlarge"
  
  validation {
    condition     = can(regex("^[a-z][0-9][a-z]?\\.(large|xlarge|[0-9]+xlarge)$", var.kafka_instance_type))
    error_message = "Kafka instance type must be large or xlarge variants."
  }
}

variable "kafka_number_of_broker_nodes" {
  description = "Number of Kafka broker nodes"
  type        = number
  default     = 6
  
  validation {
    condition     = var.kafka_number_of_broker_nodes >= 3 && var.kafka_number_of_broker_nodes % 3 == 0
    error_message = "Number of Kafka brokers must be a multiple of 3 (for 3 AZs)."
  }
}

variable "kafka_ebs_volume_size" {
  description = "EBS volume size for Kafka brokers (GB)"
  type        = number
  default     = 1000
  
  validation {
    condition     = var.kafka_ebs_volume_size >= 100 && var.kafka_ebs_volume_size <= 16384
    error_message = "Kafka EBS volume size must be between 100 GB and 16 TB."
  }
}

# =============================================================================
# MONITORING CONFIGURATION
# =============================================================================

variable "log_retention_days" {
  description = "CloudWatch log retention period in days"
  type        = number
  default     = 30
  
  validation {
    condition = contains([
      1, 3, 5, 7, 14, 30, 60, 90, 120, 150, 180, 365, 400, 545, 731, 1827, 3653
    ], var.log_retention_days)
    error_message = "Log retention days must be a valid CloudWatch retention period."
  }
}

variable "enable_performance_insights" {
  description = "Enable Performance Insights for Aurora"
  type        = bool
  default     = true
}

variable "enable_enhanced_monitoring" {
  description = "Enable enhanced monitoring for RDS"
  type        = bool
  default     = true
}

# =============================================================================
# SECURITY CONFIGURATION
# =============================================================================

variable "enable_encryption_at_rest" {
  description = "Enable encryption at rest for all services"
  type        = bool
  default     = true
}

variable "enable_encryption_in_transit" {
  description = "Enable encryption in transit for all services"
  type        = bool
  default     = true
}

# =============================================================================
# COST OPTIMIZATION
# =============================================================================

variable "enable_spot_instances" {
  description = "Enable spot instances for non-critical workloads"
  type        = bool
  default     = false  # Disabled for production by default
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
  default     = {}
}
