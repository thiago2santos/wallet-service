#!/bin/bash

# AWS Infrastructure Deployment Script for Wallet Service
# Supports both staging and production environments

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
ENVIRONMENT=""
AWS_REGION="sa-east-1"
AUTO_APPROVE=false
PLAN_ONLY=false
DESTROY=false

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to show usage
show_usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Deploy AWS infrastructure for the Wallet Service

OPTIONS:
    -e, --environment ENVIRONMENT    Environment to deploy (staging|production) [REQUIRED]
    -r, --region REGION             AWS region (default: sa-east-1)
    -a, --auto-approve              Auto approve terraform apply (skip confirmation)
    -p, --plan-only                 Only run terraform plan (don't apply)
    -d, --destroy                   Destroy infrastructure instead of creating
    -h, --help                      Show this help message

EXAMPLES:
    # Deploy staging environment
    $0 --environment staging

    # Deploy production with auto-approve
    $0 --environment production --auto-approve

    # Plan only for production
    $0 --environment production --plan-only

    # Destroy staging environment
    $0 --environment staging --destroy

PREREQUISITES:
    - AWS CLI configured with appropriate credentials
    - Terraform >= 1.0 installed
    - terraform.tfvars file configured for the target environment

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--environment)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -r|--region)
            AWS_REGION="$2"
            shift 2
            ;;
        -a|--auto-approve)
            AUTO_APPROVE=true
            shift
            ;;
        -p|--plan-only)
            PLAN_ONLY=true
            shift
            ;;
        -d|--destroy)
            DESTROY=true
            shift
            ;;
        -h|--help)
            show_usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Validate required parameters
if [[ -z "$ENVIRONMENT" ]]; then
    print_error "Environment is required. Use -e or --environment"
    show_usage
    exit 1
fi

if [[ "$ENVIRONMENT" != "staging" && "$ENVIRONMENT" != "production" ]]; then
    print_error "Environment must be either 'staging' or 'production'"
    exit 1
fi

# Set working directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$(dirname "$SCRIPT_DIR")"
ENV_DIR="$INFRA_DIR/environments/$ENVIRONMENT"

if [[ ! -d "$ENV_DIR" ]]; then
    print_error "Environment directory not found: $ENV_DIR"
    exit 1
fi

cd "$ENV_DIR"

print_status "Deploying Wallet Service infrastructure"
print_status "Environment: $ENVIRONMENT"
print_status "Region: $AWS_REGION"
print_status "Working directory: $ENV_DIR"

# Check prerequisites
print_status "Checking prerequisites..."

# Check AWS CLI
if ! command -v aws &> /dev/null; then
    print_error "AWS CLI is not installed"
    exit 1
fi

# Check Terraform
if ! command -v terraform &> /dev/null; then
    print_error "Terraform is not installed"
    exit 1
fi

# Check AWS credentials
if ! aws sts get-caller-identity &> /dev/null; then
    print_error "AWS credentials not configured or invalid"
    exit 1
fi

# Check terraform.tfvars
if [[ ! -f "terraform.tfvars" ]]; then
    print_warning "terraform.tfvars not found. Please copy from terraform.tfvars.example and customize"
    if [[ -f "terraform.tfvars.example" ]]; then
        print_status "Example file available: terraform.tfvars.example"
    fi
    exit 1
fi

print_success "Prerequisites check passed"

# Initialize Terraform
print_status "Initializing Terraform..."
terraform init

if [[ $? -ne 0 ]]; then
    print_error "Terraform initialization failed"
    exit 1
fi

print_success "Terraform initialized"

# Validate Terraform configuration
print_status "Validating Terraform configuration..."
terraform validate

if [[ $? -ne 0 ]]; then
    print_error "Terraform validation failed"
    exit 1
fi

print_success "Terraform configuration is valid"

# Plan infrastructure changes
print_status "Planning infrastructure changes..."

if [[ "$DESTROY" == "true" ]]; then
    terraform plan -destroy -out=tfplan
    PLAN_EXIT_CODE=$?
else
    terraform plan -out=tfplan
    PLAN_EXIT_CODE=$?
fi

if [[ $PLAN_EXIT_CODE -ne 0 ]]; then
    print_error "Terraform planning failed"
    exit 1
fi

print_success "Terraform planning completed"

# If plan-only, exit here
if [[ "$PLAN_ONLY" == "true" ]]; then
    print_success "Plan-only mode: Infrastructure plan completed successfully"
    exit 0
fi

# Confirm before applying (unless auto-approve is set)
if [[ "$AUTO_APPROVE" != "true" ]]; then
    echo
    if [[ "$DESTROY" == "true" ]]; then
        print_warning "This will DESTROY the $ENVIRONMENT environment infrastructure!"
        read -p "Are you sure you want to continue? (yes/no): " -r
    else
        print_status "This will apply the planned changes to the $ENVIRONMENT environment"
        read -p "Do you want to continue? (yes/no): " -r
    fi
    
    if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
        print_status "Deployment cancelled by user"
        exit 0
    fi
fi

# Apply infrastructure changes
if [[ "$DESTROY" == "true" ]]; then
    print_status "Destroying infrastructure..."
    terraform apply tfplan
else
    print_status "Applying infrastructure changes..."
    terraform apply tfplan
fi

APPLY_EXIT_CODE=$?

# Clean up plan file
rm -f tfplan

if [[ $APPLY_EXIT_CODE -ne 0 ]]; then
    print_error "Terraform apply failed"
    exit 1
fi

if [[ "$DESTROY" == "true" ]]; then
    print_success "Infrastructure destroyed successfully!"
else
    print_success "Infrastructure deployed successfully!"
    
    # Show important outputs
    print_status "Retrieving deployment information..."
    
    echo
    print_status "=== Deployment Summary ==="
    terraform output environment_summary
    
    echo
    print_status "=== Connection Information ==="
    terraform output connection_info
    
    echo
    print_status "=== Next Steps ==="
    echo "1. Verify the application is running: curl \$(terraform output -raw application_url)/q/health"
    echo "2. Access Swagger UI: \$(terraform output -raw application_url)/q/swagger-ui"
    echo "3. Monitor via CloudWatch: \$(terraform output -raw cloudwatch_dashboard_url)"
    echo "4. Access Grafana: \$(terraform output -raw grafana_workspace_endpoint)"
    
    if [[ "$ENVIRONMENT" == "staging" ]]; then
        echo "5. Run integration tests against the staging environment"
        echo "6. If tests pass, deploy to production"
    else
        echo "5. Monitor production metrics and alerts"
        echo "6. Set up automated deployments via CI/CD"
    fi
fi

print_success "Deployment script completed successfully!"
