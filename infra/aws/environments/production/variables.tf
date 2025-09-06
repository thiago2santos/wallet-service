# Variables for production environment

# General Configuration
variable "aws_region" {
  description = "AWS region for deployment"
  type        = string
  default     = "sa-east-1"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "production"
}

variable "owner" {
  description = "Owner of the resources"
  type        = string
  default     = "wallet-team"
}

variable "alert_email_addresses" {
  description = "Email addresses for alerts"
  type        = list(string)
  default     = []
}

# Networking Configuration
variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets"
  type        = list(string)
  default     = ["10.0.11.0/24", "10.0.12.0/24", "10.0.13.0/24"]
}

variable "database_subnet_cidrs" {
  description = "CIDR blocks for database subnets"
  type        = list(string)
  default     = ["10.0.21.0/24", "10.0.22.0/24", "10.0.23.0/24"]
}

# Database Configuration
variable "database_name" {
  description = "Name of the database"
  type        = string
  default     = "wallet"
}

variable "database_master_username" {
  description = "Master username for the database"
  type        = string
  default     = "walletadmin"
}

variable "database_engine_version" {
  description = "Aurora MySQL engine version"
  type        = string
  default     = "8.0.mysql_aurora.3.02.0"
}

variable "database_writer_instance_class" {
  description = "Instance class for database writer"
  type        = string
  default     = "db.r6g.2xlarge"
}

variable "database_reader_instance_class" {
  description = "Instance class for database readers"
  type        = string
  default     = "db.r6g.2xlarge"
}

variable "database_reader_count" {
  description = "Number of database reader instances"
  type        = number
  default     = 2
}

variable "database_backup_retention_period" {
  description = "Database backup retention period in days"
  type        = number
  default     = 35
}

variable "database_backup_window" {
  description = "Database backup window"
  type        = string
  default     = "03:00-04:00"
}

variable "database_maintenance_window" {
  description = "Database maintenance window"
  type        = string
  default     = "sun:04:00-sun:05:00"
}

variable "database_max_connections" {
  description = "Maximum database connections"
  type        = string
  default     = "2000"
}

# Cache Configuration
variable "cache_node_type" {
  description = "ElastiCache node type"
  type        = string
  default     = "cache.r6g.large"
}

variable "cache_num_nodes" {
  description = "Number of cache nodes"
  type        = number
  default     = 3
}

variable "cache_parameter_group_name" {
  description = "Cache parameter group name"
  type        = string
  default     = "default.redis7"
}

variable "cache_engine_version" {
  description = "Redis engine version"
  type        = string
  default     = "7.0"
}

# Messaging Configuration (Kafka)
variable "kafka_version" {
  description = "Kafka version"
  type        = string
  default     = "2.8.1"
}

variable "kafka_instance_type" {
  description = "Kafka instance type"
  type        = string
  default     = "kafka.m5.large"
}

variable "kafka_number_of_brokers" {
  description = "Number of Kafka brokers"
  type        = number
  default     = 3
}

variable "kafka_ebs_volume_size" {
  description = "EBS volume size for Kafka brokers"
  type        = number
  default     = 100
}

# Application Configuration
variable "container_image" {
  description = "Container image for the application"
  type        = string
  default     = "wallet-service:latest"
}

variable "app_cpu" {
  description = "CPU units for the application (1024 = 1 vCPU)"
  type        = number
  default     = 4096
}

variable "app_memory" {
  description = "Memory for the application in MB"
  type        = number
  default     = 8192
}

variable "app_desired_count" {
  description = "Desired number of application instances"
  type        = number
  default     = 6
}

variable "app_min_capacity" {
  description = "Minimum number of application instances"
  type        = number
  default     = 3
}

variable "app_max_capacity" {
  description = "Maximum number of application instances"
  type        = number
  default     = 20
}

# Monitoring Configuration
variable "grafana_version" {
  description = "Grafana version"
  type        = string
  default     = "9.4"
}
