# =============================================================================
# WALLET SERVICE - PRODUCTION ENVIRONMENT
# =============================================================================
# Scale: 10M users, 5000+ TPS
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
  #   key    = "production/terraform.tfstate"
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
      Environment   = "production"
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
  name_prefix = "wallet-prod"
  
  # Network configuration
  vpc_cidr = "10.0.0.0/16"
  azs      = slice(data.aws_availability_zones.available.names, 0, 3)
  
  # Application configuration
  app_name    = "wallet-service"
  environment = "production"
  
  # Scaling configuration
  target_tps           = 5000
  expected_users       = 10000000
  pods_per_node       = 10
  tps_per_pod         = 250
  min_pods            = local.target_tps / local.tps_per_pod  # 20 pods
  
  # Common tags
  common_tags = {
    Environment = local.environment
    Application = local.app_name
    Terraform   = "true"
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
  
  # Public subnets for ALB
  public_subnet_cidrs = [
    "10.0.1.0/24",
    "10.0.2.0/24", 
    "10.0.3.0/24"
  ]
  
  # Private subnets for EKS nodes
  private_subnet_cidrs = [
    "10.0.11.0/24",
    "10.0.12.0/24",
    "10.0.13.0/24"
  ]
  
  # Database subnets
  database_subnet_cidrs = [
    "10.0.21.0/24",
    "10.0.22.0/24",
    "10.0.23.0/24"
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
# COMPUTE MODULE (EKS)
# =============================================================================

module "compute" {
  source = "../../modules/compute"
  
  name_prefix = local.name_prefix
  
  # Network configuration
  vpc_id              = module.networking.vpc_id
  private_subnet_ids  = module.networking.private_subnet_ids
  
  # EKS configuration
  kubernetes_version = "1.28"
  
  # Node groups for production scale
  node_groups = {
    # Main application nodes
    wallet_app = {
      instance_types = ["c5.2xlarge"]  # 8 vCPU, 16 GB RAM
      min_size      = 10               # Minimum for 5000 TPS
      max_size      = 50               # Scale up to 12,500 TPS
      desired_size  = 20               # Target for 5000 TPS
      
      labels = {
        role = "application"
        tier = "compute"
      }
      
      taints = []
    }
    
    # Monitoring and system nodes
    system = {
      instance_types = ["m5.large"]    # 2 vCPU, 8 GB RAM
      min_size      = 2
      max_size      = 4
      desired_size  = 2
      
      labels = {
        role = "system"
        tier = "monitoring"
      }
      
      taints = [{
        key    = "system"
        value  = "true"
        effect = "NO_SCHEDULE"
      }]
    }
  }
  
  tags = local.common_tags
}

# =============================================================================
# DATABASE MODULE (Aurora MySQL)
# =============================================================================

module "database" {
  source = "../../modules/database"
  
  name_prefix = local.name_prefix
  
  # Network configuration
  vpc_id             = module.networking.vpc_id
  database_subnet_ids = module.networking.database_subnet_ids
  
  # Aurora Serverless v2 configuration for production scale
  engine_version = "8.0.mysql_aurora.3.04.0"
  
  # Serverless v2 scaling for 5000+ TPS
  serverlessv2_scaling_configuration = {
    max_capacity = 64    # 128 GB RAM, 32 vCPU equivalent
    min_capacity = 8     # 16 GB RAM, 4 vCPU equivalent
  }
  
  # Read replicas for read scaling
  replica_count = 3
  
  # Backup configuration
  backup_retention_period = 35
  backup_window          = "03:00-04:00"  # 11 PM - 12 AM BRT
  maintenance_window     = "sun:04:00-sun:05:00"  # 12 AM - 1 AM BRT Sunday
  
  # Security
  database_name = "wallet_db"
  master_username = "wallet_admin"
  
  # Performance Insights
  performance_insights_enabled = true
  
  tags = local.common_tags
}

# =============================================================================
# CACHE MODULE (ElastiCache Redis)
# =============================================================================

module "cache" {
  source = "../../modules/cache"
  
  name_prefix = local.name_prefix
  
  # Network configuration
  vpc_id              = module.networking.vpc_id
  private_subnet_ids  = module.networking.private_subnet_ids
  
  # Redis configuration for production scale
  node_type = "r6g.2xlarge"  # 8 vCPU, 52 GB RAM
  num_cache_nodes = 3        # Multi-AZ with failover
  
  # Cluster configuration
  parameter_group_name = "default.redis7"
  engine_version      = "7.0"
  
  # Security and backup
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  
  tags = local.common_tags
}

# =============================================================================
# MESSAGING MODULE (MSK Kafka)
# =============================================================================

module "messaging" {
  source = "../../modules/messaging"
  
  name_prefix = local.name_prefix
  
  # Network configuration
  vpc_id              = module.networking.vpc_id
  private_subnet_ids  = module.networking.private_subnet_ids
  
  # MSK configuration for production scale
  kafka_version = "2.8.1"
  instance_type = "m5.2xlarge"  # 8 vCPU, 32 GB RAM
  
  # Broker configuration - 6 brokers (2 per AZ)
  number_of_broker_nodes = 6
  
  # Storage configuration
  ebs_volume_size = 1000  # 1 TB per broker
  
  # Security
  encryption_in_transit_client_broker = "TLS"
  encryption_in_transit_in_cluster   = true
  
  tags = local.common_tags
}

# =============================================================================
# MONITORING MODULE
# =============================================================================

module "monitoring" {
  source = "../../modules/monitoring"
  
  name_prefix = local.name_prefix
  
  # EKS configuration
  cluster_name = module.compute.cluster_name
  
  # Monitoring configuration
  prometheus_enabled = true
  grafana_enabled   = true
  
  # CloudWatch configuration
  log_retention_days = 30
  
  tags = local.common_tags
}
