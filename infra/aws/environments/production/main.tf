# Production Environment for Wallet Service
# Designed for 10M users, 5000+ TPS, high availability

terraform {
  required_version = ">= 1.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.1"
    }
  }

  # Backend configuration - uncomment and configure for remote state
  # backend "s3" {
  #   bucket         = "wallet-service-terraform-state-prod"
  #   key            = "production/terraform.tfstate"
  #   region         = "sa-east-1"
  #   encrypt        = true
  #   dynamodb_table = "wallet-service-terraform-locks"
  # }
}

# Configure AWS Provider
provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Environment = "production"
      Project     = "wallet-service"
      ManagedBy   = "terraform"
      Owner       = var.owner
    }
  }
}

# Data sources
data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

# Networking Module
module "networking" {
  source = "../../modules/networking"

  environment              = "production"
  vpc_cidr                = var.vpc_cidr
  public_subnet_cidrs     = var.public_subnet_cidrs
  private_subnet_cidrs    = var.private_subnet_cidrs
  database_subnet_cidrs   = var.database_subnet_cidrs
  enable_nat_gateway      = true
  enable_flow_logs        = true
}

# Database Module
module "database" {
  source = "../../modules/database"

  environment                      = "production"
  db_subnet_group_name            = module.networking.db_subnet_group_name
  security_group_id               = module.networking.rds_security_group_id
  database_name                   = var.database_name
  master_username                 = var.database_master_username
  engine_version                  = var.database_engine_version
  writer_instance_class           = var.database_writer_instance_class
  reader_instance_class           = var.database_reader_instance_class
  reader_count                    = var.database_reader_count
  backup_retention_period         = var.database_backup_retention_period
  backup_window                   = var.database_backup_window
  maintenance_window              = var.database_maintenance_window
  deletion_protection             = true
  skip_final_snapshot             = false
  performance_insights_retention  = 31
  auto_minor_version_upgrade      = true
  max_connections                 = var.database_max_connections
  alarm_actions                   = [aws_sns_topic.alerts.arn]
}

# Cache Module (ElastiCache Redis)
module "cache" {
  source = "../../modules/cache"

  environment               = "production"
  subnet_group_name        = module.networking.elasticache_subnet_group_name
  security_group_id        = module.networking.elasticache_security_group_id
  node_type                = var.cache_node_type
  num_cache_nodes          = var.cache_num_nodes
  parameter_group_name     = var.cache_parameter_group_name
  engine_version           = var.cache_engine_version
  port                     = 6379
  maintenance_window       = "sun:05:00-sun:06:00"
  snapshot_retention_limit = 5
  snapshot_window          = "03:00-05:00"
  alarm_actions           = [aws_sns_topic.alerts.arn]
}

# Messaging Module (MSK Kafka)
module "messaging" {
  source = "../../modules/messaging"

  environment           = "production"
  vpc_id               = module.networking.vpc_id
  subnet_ids           = module.networking.private_subnet_ids
  security_group_id    = module.networking.msk_security_group_id
  kafka_version        = var.kafka_version
  instance_type        = var.kafka_instance_type
  number_of_brokers    = var.kafka_number_of_brokers
  ebs_volume_size      = var.kafka_ebs_volume_size
  client_authentication = {
    tls = true
  }
  encryption_in_transit = {
    client_broker = "TLS"
    in_cluster    = true
  }
  logging_info = {
    broker_logs = {
      cloudwatch_logs = {
        enabled   = true
        log_group = "/aws/msk/wallet-service-production"
      }
      s3 = {
        enabled = true
        bucket  = aws_s3_bucket.kafka_logs.id
        prefix  = "kafka-logs/"
      }
    }
  }
  alarm_actions = [aws_sns_topic.alerts.arn]
}

# Compute Module (ECS Fargate)
module "compute" {
  source = "../../modules/compute"

  environment                = "production"
  vpc_id                    = module.networking.vpc_id
  private_subnet_ids        = module.networking.private_subnet_ids
  public_subnet_ids         = module.networking.public_subnet_ids
  alb_security_group_id     = module.networking.alb_security_group_id
  ecs_security_group_id     = module.networking.ecs_security_group_id
  
  # Application configuration
  app_name                  = "wallet-service"
  app_port                  = 8080
  app_cpu                   = var.app_cpu
  app_memory                = var.app_memory
  app_desired_count         = var.app_desired_count
  app_min_capacity          = var.app_min_capacity
  app_max_capacity          = var.app_max_capacity
  
  # Container image
  container_image           = var.container_image
  
  # Environment variables
  environment_variables = {
    SPRING_PROFILES_ACTIVE = "production"
    AWS_REGION            = var.aws_region
    DB_SECRET_ARN         = module.database.secret_arn
    REDIS_ENDPOINT        = module.cache.primary_endpoint
    KAFKA_BOOTSTRAP_SERVERS = module.messaging.bootstrap_brokers_tls
  }
  
  # Secrets from AWS Secrets Manager
  secrets = {
    DB_USERNAME = "${module.database.secret_arn}:username::"
    DB_PASSWORD = "${module.database.secret_arn}:password::"
  }
  
  # Health check configuration
  health_check_path         = "/q/health"
  health_check_matcher      = "200"
  
  # Auto-scaling configuration
  scale_up_cooldown         = 300
  scale_down_cooldown       = 300
  target_cpu_utilization    = 70
  target_memory_utilization = 80
  
  # Logging
  log_group_name           = "/aws/ecs/wallet-service-production"
  log_retention_days       = 30
  
  alarm_actions            = [aws_sns_topic.alerts.arn]
}

# Monitoring Module
module "monitoring" {
  source = "../../modules/monitoring"

  environment     = "production"
  vpc_id         = module.networking.vpc_id
  subnet_ids     = module.networking.private_subnet_ids
  
  # Grafana configuration
  grafana_version = var.grafana_version
  
  # CloudWatch configuration
  log_retention_days = 30
  
  # Alerting
  sns_topic_arn = aws_sns_topic.alerts.arn
  
  # Dashboard configuration
  create_business_dashboard     = true
  create_technical_dashboard    = true
  create_infrastructure_dashboard = true
  
  depends_on = [
    module.compute,
    module.database,
    module.cache,
    module.messaging
  ]
}

# S3 Bucket for Kafka logs
resource "aws_s3_bucket" "kafka_logs" {
  bucket = "${var.environment}-wallet-kafka-logs-${random_id.bucket_suffix.hex}"

  tags = {
    Name        = "${var.environment}-wallet-kafka-logs"
    Environment = var.environment
    Project     = "wallet-service"
  }
}

resource "random_id" "bucket_suffix" {
  byte_length = 4
}

resource "aws_s3_bucket_versioning" "kafka_logs" {
  bucket = aws_s3_bucket.kafka_logs.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "kafka_logs" {
  bucket = aws_s3_bucket.kafka_logs.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "kafka_logs" {
  bucket = aws_s3_bucket.kafka_logs.id

  rule {
    id     = "kafka_logs_lifecycle"
    status = "Enabled"

    expiration {
      days = 90
    }

    noncurrent_version_expiration {
      noncurrent_days = 30
    }
  }
}

# SNS Topic for Alerts
resource "aws_sns_topic" "alerts" {
  name = "${var.environment}-wallet-alerts"

  tags = {
    Name        = "${var.environment}-wallet-alerts"
    Environment = var.environment
    Project     = "wallet-service"
  }
}

resource "aws_sns_topic_subscription" "email_alerts" {
  count = length(var.alert_email_addresses)

  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "email"
  endpoint  = var.alert_email_addresses[count.index]
}

# CloudWatch Dashboard for high-level metrics
resource "aws_cloudwatch_dashboard" "main" {
  dashboard_name = "${var.environment}-wallet-service-overview"

  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "metric"
        x      = 0
        y      = 0
        width  = 12
        height = 6

        properties = {
          metrics = [
            ["AWS/ApplicationELB", "RequestCount", "LoadBalancer", module.compute.alb_arn_suffix],
            [".", "TargetResponseTime", ".", "."],
            [".", "HTTPCode_Target_2XX_Count", ".", "."],
            [".", "HTTPCode_Target_4XX_Count", ".", "."],
            [".", "HTTPCode_Target_5XX_Count", ".", "."]
          ]
          view    = "timeSeries"
          stacked = false
          region  = var.aws_region
          title   = "Application Load Balancer Metrics"
          period  = 300
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 6
        width  = 12
        height = 6

        properties = {
          metrics = [
            ["AWS/ECS", "CPUUtilization", "ServiceName", module.compute.service_name, "ClusterName", module.compute.cluster_name],
            [".", "MemoryUtilization", ".", ".", ".", "."]
          ]
          view    = "timeSeries"
          stacked = false
          region  = var.aws_region
          title   = "ECS Service Metrics"
          period  = 300
        }
      }
    ]
  })
}
