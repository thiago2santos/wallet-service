# =============================================================================
# WALLET SERVICE - STAGING ENVIRONMENT
# =============================================================================
# Scale: Minimal cost, full functionality
# Region: sa-east-1 (São Paulo, Brazil)
# =============================================================================

terraform {
  required_version = ">= 1.5"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.23"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.11"
    }
  }

  # TODO: Configure remote state
  # backend "s3" {
  #   bucket = "wallet-service-terraform-state"
  #   key    = "staging/terraform.tfstate"
  #   region = "sa-east-1"
  # }
}

# =============================================================================
# PROVIDERS
# =============================================================================

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Environment   = "staging"
      Project      = "wallet-service"
      ManagedBy    = "terraform"
      Owner        = "platform-team"
      CostCenter   = "engineering"
    }
  }
}

# =============================================================================
# DATA SOURCES
# =============================================================================

data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_caller_identity" "current" {}

# =============================================================================
# LOCAL VALUES
# =============================================================================

locals {
  name_prefix = "wallet-staging"
  
  # Network configuration
  vpc_cidr = "10.1.0.0/16"  # Different from production
  azs      = slice(data.aws_availability_zones.available.names, 0, 2)  # Only 2 AZs for cost
  
  # Application configuration
  app_name    = "wallet-service"
  environment = "staging"
  
  # Minimal scaling configuration
  target_tps    = 100   # Much lower for staging
  min_pods      = 2     # Minimal pods
  
  # Common tags
  common_tags = {
    Environment = local.environment
    Application = local.app_name
    Terraform   = "true"
    CostOptimized = "true"
  }
}

# =============================================================================
# NETWORKING MODULE
# =============================================================================

module "networking" {
  source = "../../modules/networking"
  
  name_prefix = local.name_prefix
  vpc_cidr    = local.vpc_cidr
  azs         = local.azs
  
  # Public subnets for ALB (2 AZs only)
  public_subnet_cidrs = [
    "10.1.1.0/24",
    "10.1.2.0/24"
  ]
  
  # Private subnets for EKS nodes (2 AZs only)
  private_subnet_cidrs = [
    "10.1.11.0/24",
    "10.1.12.0/24"
  ]
  
  # Database subnets (2 AZs only)
  database_subnet_cidrs = [
    "10.1.21.0/24",
    "10.1.22.0/24"
  ]
  
  tags = local.common_tags
}

# =============================================================================
# SECURITY MODULE
# =============================================================================

module "security" {
  source = "../../modules/security"
  
  name_prefix = local.name_prefix
  vpc_id      = module.networking.vpc_id
  
  # Certificate configuration
  domain_name = var.domain_name
  
  tags = local.common_tags
}

# =============================================================================
# COMPUTE MODULE (EKS) - MINIMAL CONFIGURATION
# =============================================================================

module "compute" {
  source = "../../modules/compute"
  
  name_prefix = local.name_prefix
  
  # Network configuration
  vpc_id              = module.networking.vpc_id
  private_subnet_ids  = module.networking.private_subnet_ids
  
  # EKS configuration
  kubernetes_version = "1.28"
  
  # Minimal node groups for staging
  node_groups = {
    # Single node group for everything
    staging_nodes = {
      instance_types = ["t3.large"]  # 2 vCPU, 8 GB RAM - cost optimized
      min_size      = 2             # Minimal for HA
      max_size      = 4             # Limited scaling
      desired_size  = 2             # Start small
      
      labels = {
        role = "multi-purpose"
        tier = "staging"
      }
      
      taints = []
    }
  }
  
  tags = local.common_tags
}

# =============================================================================
# DATABASE MODULE (Aurora MySQL) - MINIMAL CONFIGURATION
# =============================================================================

module "database" {
  source = "../../modules/database"
  
  name_prefix = local.name_prefix
  
  # Network configuration
  vpc_id             = module.networking.vpc_id
  database_subnet_ids = module.networking.database_subnet_ids
  
  # Aurora Serverless v2 configuration for staging
  engine_version = "8.0.mysql_aurora.3.04.0"
  
  # Minimal Serverless v2 scaling
  serverlessv2_scaling_configuration = {
    max_capacity = 2     # 4 GB RAM, 1 vCPU equivalent
    min_capacity = 0.5   # 1 GB RAM, 0.25 vCPU equivalent
  }
  
  # No read replicas for staging
  replica_count = 0
  
  # Minimal backup configuration
  backup_retention_period = 7   # Minimum required
  backup_window          = "03:00-04:00"
  maintenance_window     = "sun:04:00-sun:05:00"
  
  # Security
  database_name = "wallet_db_staging"
  master_username = "wallet_admin"
  
  # Performance Insights disabled for cost
  performance_insights_enabled = false
  
  tags = local.common_tags
}

# =============================================================================
# CACHE MODULE (ElastiCache Redis) - MINIMAL CONFIGURATION
# =============================================================================

module "cache" {
  source = "../../modules/cache"
  
  name_prefix = local.name_prefix
  
  # Network configuration
  vpc_id              = module.networking.vpc_id
  private_subnet_ids  = module.networking.private_subnet_ids
  
  # Minimal Redis configuration
  node_type = "t3.micro"  # 2 vCPU, 1 GB RAM - smallest available
  num_cache_nodes = 1     # Single node for cost
  
  # Basic configuration
  parameter_group_name = "default.redis7"
  engine_version      = "7.0"
  
  # Security (still enabled for consistency)
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  
  tags = local.common_tags
}

# =============================================================================
# MESSAGING MODULE (MSK Kafka) - MINIMAL CONFIGURATION
# =============================================================================

module "messaging" {
  source = "../../modules/messaging"
  
  name_prefix = local.name_prefix
  
  # Network configuration
  vpc_id              = module.networking.vpc_id
  private_subnet_ids  = module.networking.private_subnet_ids
  
  # Minimal MSK configuration
  kafka_version = "2.8.1"
  instance_type = "t3.small"  # 2 vCPU, 2 GB RAM - cost optimized
  
  # Minimal broker configuration - 2 brokers (1 per AZ)
  number_of_broker_nodes = 2
  
  # Minimal storage configuration
  ebs_volume_size = 100  # 100 GB per broker
  
  # Security (still enabled for consistency)
  encryption_in_transit_client_broker = "TLS"
  encryption_in_transit_in_cluster   = true
  
  tags = local.common_tags
}

# =============================================================================
# MONITORING MODULE - BASIC CONFIGURATION
# =============================================================================

module "monitoring" {
  source = "../../modules/monitoring"
  
  name_prefix = local.name_prefix
  
  # EKS configuration
  cluster_name = module.compute.cluster_name
  
  # Basic monitoring configuration
  prometheus_enabled = true
  grafana_enabled   = true
  
  # Shorter log retention for cost
  log_retention_days = 7
  
  tags = local.common_tags
}
