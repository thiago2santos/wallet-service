# RDS Aurora MySQL Cluster
# High-performance, scalable database for the wallet service

terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# Random password for database
resource "random_password" "master_password" {
  length  = 16
  special = true
}

# KMS Key for RDS encryption
resource "aws_kms_key" "rds" {
  description             = "KMS key for RDS encryption - ${var.environment} wallet service"
  deletion_window_in_days = 7

  tags = {
    Name        = "${var.environment}-wallet-rds-kms"
    Environment = var.environment
    Project     = "wallet-service"
  }
}

resource "aws_kms_alias" "rds" {
  name          = "alias/${var.environment}-wallet-rds"
  target_key_id = aws_kms_key.rds.key_id
}

# Secrets Manager secret for database credentials
resource "aws_secretsmanager_secret" "db_credentials" {
  name                    = "${var.environment}-wallet-db-credentials"
  description             = "Database credentials for wallet service"
  recovery_window_in_days = 7
  kms_key_id              = aws_kms_key.rds.arn

  tags = {
    Name        = "${var.environment}-wallet-db-credentials"
    Environment = var.environment
    Project     = "wallet-service"
  }
}

resource "aws_secretsmanager_secret_version" "db_credentials" {
  secret_id = aws_secretsmanager_secret.db_credentials.id
  secret_string = jsonencode({
    username = var.master_username
    password = random_password.master_password.result
  })
}

# DB Parameter Group for Aurora MySQL
resource "aws_rds_cluster_parameter_group" "main" {
  family      = "aurora-mysql8.0"
  name        = "${var.environment}-wallet-cluster-params"
  description = "Aurora MySQL cluster parameter group for wallet service"

  # Performance optimizations for financial workloads
  parameter {
    name  = "innodb_buffer_pool_size"
    value = "{DBInstanceClassMemory*3/4}"
  }

  parameter {
    name  = "innodb_log_file_size"
    value = "268435456" # 256MB
  }

  parameter {
    name  = "innodb_log_buffer_size"
    value = "67108864" # 64MB
  }

  parameter {
    name  = "max_connections"
    value = var.max_connections
  }

  parameter {
    name  = "innodb_flush_log_at_trx_commit"
    value = "1" # ACID compliance for financial data
  }

  parameter {
    name  = "binlog_format"
    value = "ROW"
  }

  parameter {
    name  = "slow_query_log"
    value = "1"
  }

  parameter {
    name  = "long_query_time"
    value = "2"
  }

  tags = {
    Name        = "${var.environment}-wallet-cluster-params"
    Environment = var.environment
    Project     = "wallet-service"
  }
}

# DB Parameter Group for Aurora MySQL instances
resource "aws_db_parameter_group" "main" {
  family = "aurora-mysql8.0"
  name   = "${var.environment}-wallet-instance-params"

  # Instance-level optimizations
  parameter {
    name  = "innodb_buffer_pool_instances"
    value = "8"
  }

  parameter {
    name  = "innodb_io_capacity"
    value = "2000"
  }

  parameter {
    name  = "innodb_io_capacity_max"
    value = "4000"
  }

  tags = {
    Name        = "${var.environment}-wallet-instance-params"
    Environment = var.environment
    Project     = "wallet-service"
  }
}

# Aurora MySQL Cluster
resource "aws_rds_cluster" "main" {
  cluster_identifier      = "${var.environment}-wallet-cluster"
  engine                  = "aurora-mysql"
  engine_version          = var.engine_version
  database_name           = var.database_name
  master_username         = var.master_username
  master_password         = random_password.master_password.result
  backup_retention_period = var.backup_retention_period
  preferred_backup_window = var.backup_window
  preferred_maintenance_window = var.maintenance_window
  
  # Network configuration
  db_subnet_group_name   = var.db_subnet_group_name
  vpc_security_group_ids = [var.security_group_id]
  
  # Security and encryption
  storage_encrypted   = true
  kms_key_id         = aws_kms_key.rds.arn
  deletion_protection = var.deletion_protection
  
  # Performance and monitoring
  db_cluster_parameter_group_name = aws_rds_cluster_parameter_group.main.name
  enabled_cloudwatch_logs_exports = ["audit", "error", "general", "slowquery"]
  
  # Backup configuration
  copy_tags_to_snapshot = true
  skip_final_snapshot   = var.skip_final_snapshot
  final_snapshot_identifier = var.skip_final_snapshot ? null : "${var.environment}-wallet-cluster-final-snapshot-${formatdate("YYYY-MM-DD-hhmm", timestamp())}"
  
  # Performance Insights
  performance_insights_enabled          = true
  performance_insights_kms_key_id      = aws_kms_key.rds.arn
  performance_insights_retention_period = var.performance_insights_retention

  tags = {
    Name        = "${var.environment}-wallet-cluster"
    Environment = var.environment
    Project     = "wallet-service"
  }

  lifecycle {
    ignore_changes = [
      master_password,
      final_snapshot_identifier
    ]
  }
}

# Aurora MySQL Writer Instance
resource "aws_rds_cluster_instance" "writer" {
  identifier           = "${var.environment}-wallet-writer"
  cluster_identifier   = aws_rds_cluster.main.id
  instance_class       = var.writer_instance_class
  engine               = aws_rds_cluster.main.engine
  engine_version       = aws_rds_cluster.main.engine_version
  db_parameter_group_name = aws_db_parameter_group.main.name
  
  # Performance monitoring
  performance_insights_enabled    = true
  performance_insights_kms_key_id = aws_kms_key.rds.arn
  monitoring_interval             = 60
  monitoring_role_arn            = aws_iam_role.rds_enhanced_monitoring.arn
  
  # Auto minor version upgrade
  auto_minor_version_upgrade = var.auto_minor_version_upgrade

  tags = {
    Name        = "${var.environment}-wallet-writer"
    Environment = var.environment
    Project     = "wallet-service"
    Role        = "writer"
  }
}

# Aurora MySQL Reader Instances
resource "aws_rds_cluster_instance" "reader" {
  count = var.reader_count

  identifier           = "${var.environment}-wallet-reader-${count.index + 1}"
  cluster_identifier   = aws_rds_cluster.main.id
  instance_class       = var.reader_instance_class
  engine               = aws_rds_cluster.main.engine
  engine_version       = aws_rds_cluster.main.engine_version
  db_parameter_group_name = aws_db_parameter_group.main.name
  
  # Performance monitoring
  performance_insights_enabled    = true
  performance_insights_kms_key_id = aws_kms_key.rds.arn
  monitoring_interval             = 60
  monitoring_role_arn            = aws_iam_role.rds_enhanced_monitoring.arn
  
  # Auto minor version upgrade
  auto_minor_version_upgrade = var.auto_minor_version_upgrade

  tags = {
    Name        = "${var.environment}-wallet-reader-${count.index + 1}"
    Environment = var.environment
    Project     = "wallet-service"
    Role        = "reader"
  }
}

# IAM Role for Enhanced Monitoring
resource "aws_iam_role" "rds_enhanced_monitoring" {
  name = "${var.environment}-wallet-rds-enhanced-monitoring"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "monitoring.rds.amazonaws.com"
        }
      }
    ]
  })

  tags = {
    Name        = "${var.environment}-wallet-rds-enhanced-monitoring"
    Environment = var.environment
    Project     = "wallet-service"
  }
}

resource "aws_iam_role_policy_attachment" "rds_enhanced_monitoring" {
  role       = aws_iam_role.rds_enhanced_monitoring.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonRDSEnhancedMonitoringRole"
}

# CloudWatch Alarms for Database Monitoring
resource "aws_cloudwatch_metric_alarm" "database_cpu" {
  alarm_name          = "${var.environment}-wallet-db-cpu-utilization"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "80"
  alarm_description   = "This metric monitors RDS CPU utilization"
  alarm_actions       = var.alarm_actions

  dimensions = {
    DBClusterIdentifier = aws_rds_cluster.main.cluster_identifier
  }

  tags = {
    Name        = "${var.environment}-wallet-db-cpu-alarm"
    Environment = var.environment
    Project     = "wallet-service"
  }
}

resource "aws_cloudwatch_metric_alarm" "database_connections" {
  alarm_name          = "${var.environment}-wallet-db-connections"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "DatabaseConnections"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = var.max_connections * 0.8
  alarm_description   = "This metric monitors RDS connection count"
  alarm_actions       = var.alarm_actions

  dimensions = {
    DBClusterIdentifier = aws_rds_cluster.main.cluster_identifier
  }

  tags = {
    Name        = "${var.environment}-wallet-db-connections-alarm"
    Environment = var.environment
    Project     = "wallet-service"
  }
}

resource "aws_cloudwatch_metric_alarm" "database_read_latency" {
  alarm_name          = "${var.environment}-wallet-db-read-latency"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "ReadLatency"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "0.2"
  alarm_description   = "This metric monitors RDS read latency"
  alarm_actions       = var.alarm_actions

  dimensions = {
    DBClusterIdentifier = aws_rds_cluster.main.cluster_identifier
  }

  tags = {
    Name        = "${var.environment}-wallet-db-read-latency-alarm"
    Environment = var.environment
    Project     = "wallet-service"
  }
}

resource "aws_cloudwatch_metric_alarm" "database_write_latency" {
  alarm_name          = "${var.environment}-wallet-db-write-latency"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "WriteLatency"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "0.2"
  alarm_description   = "This metric monitors RDS write latency"
  alarm_actions       = var.alarm_actions

  dimensions = {
    DBClusterIdentifier = aws_rds_cluster.main.cluster_identifier
  }

  tags = {
    Name        = "${var.environment}-wallet-db-write-latency-alarm"
    Environment = var.environment
    Project     = "wallet-service"
  }
}
