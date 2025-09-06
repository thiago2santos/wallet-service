#!/bin/bash

# Deploy Wallet Service for Load Testing
# This script builds and deploys the containerized wallet service optimized for performance testing

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="docker-compose.loadtest.yml"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 WALLET SERVICE LOAD TEST DEPLOYMENT${NC}"
echo -e "${BLUE}======================================${NC}"
echo ""

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check prerequisites
echo -e "${YELLOW}📋 Checking prerequisites...${NC}"

if ! command_exists docker; then
    echo -e "${RED}❌ Docker is not installed. Please install Docker first.${NC}"
    exit 1
fi

if ! command_exists docker-compose; then
    echo -e "${RED}❌ Docker Compose is not installed. Please install Docker Compose first.${NC}"
    exit 1
fi

if ! command_exists mvn && ! command_exists ./mvnw; then
    echo -e "${RED}❌ Maven is not available. Please install Maven or use ./mvnw.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Prerequisites check passed${NC}"
echo ""

# Stop any existing development services
echo -e "${YELLOW}🛑 Stopping any existing services...${NC}"
if docker-compose ps | grep -q "Up"; then
    docker-compose down
fi

if docker-compose -f "$COMPOSE_FILE" ps | grep -q "Up"; then
    docker-compose -f "$COMPOSE_FILE" down
fi

# Clean up any existing containers
echo -e "${YELLOW}🧹 Cleaning up existing containers...${NC}"
docker container prune -f
docker network prune -f

# Build the application
echo -e "${YELLOW}🔨 Building the application...${NC}"
cd "$PROJECT_ROOT"

# Use Maven wrapper if available, otherwise use system Maven
if [ -f "./mvnw" ]; then
    MAVEN_CMD="./mvnw"
else
    MAVEN_CMD="mvn"
fi

echo -e "${BLUE}Building with profile: loadtest${NC}"
$MAVEN_CMD clean package -DskipTests -Dquarkus.profile=loadtest

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Build failed. Please check the build output.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Build completed successfully${NC}"
echo ""

# Build Docker image
echo -e "${YELLOW}🐳 Building Docker image...${NC}"
docker build -f src/main/docker/Dockerfile.jvm -t wallet-service:loadtest .

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Docker build failed.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Docker image built successfully${NC}"
echo ""

# Start the load test environment
echo -e "${YELLOW}🚀 Starting load test environment...${NC}"
docker-compose -f "$COMPOSE_FILE" up -d

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Failed to start load test environment.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Load test environment started${NC}"
echo ""

# Wait for services to be healthy
echo -e "${YELLOW}⏳ Waiting for services to be healthy...${NC}"

# Function to wait for service health
wait_for_service() {
    local service_name=$1
    local max_attempts=30
    local attempt=1
    
    echo -e "${BLUE}Waiting for $service_name...${NC}"
    
    while [ $attempt -le $max_attempts ]; do
        if docker-compose -f "$COMPOSE_FILE" ps "$service_name" | grep -q "healthy\\|Up"; then
            echo -e "${GREEN}✅ $service_name is ready${NC}"
            return 0
        fi
        
        echo -e "${YELLOW}⏳ Attempt $attempt/$max_attempts - $service_name not ready yet...${NC}"
        sleep 10
        attempt=$((attempt + 1))
    done
    
    echo -e "${RED}❌ $service_name failed to become healthy${NC}"
    return 1
}

# Wait for each service
wait_for_service "mysql-loadtest"
wait_for_service "redis-loadtest"
wait_for_service "wallet-service"

echo ""
echo -e "${GREEN}🎉 LOAD TEST ENVIRONMENT READY!${NC}"
echo ""
echo -e "${PURPLE}📊 Service URLs:${NC}"
echo -e "${BLUE}  • Wallet Service:${NC} http://localhost:8080"
echo -e "${BLUE}  • Swagger UI:${NC}     http://localhost:8080/q/swagger-ui"
echo -e "${BLUE}  • Health Check:${NC}   http://localhost:8080/q/health"
echo -e "${BLUE}  • Metrics:${NC}        http://localhost:8080/metrics"
echo -e "${BLUE}  • Grafana:${NC}        http://localhost:3000 (admin/admin)"
echo -e "${BLUE}  • Prometheus:${NC}     http://localhost:9090"
echo ""
echo -e "${PURPLE}🧪 Ready for Load Testing:${NC}"
echo -e "${BLUE}  • Run: k6 run performance/scripts/k6/extreme-load-test.js${NC}"
echo -e "${BLUE}  • Or:  ./performance/scripts/shell/run-extreme-load-test.sh${NC}"
echo ""
echo -e "${YELLOW}📝 To view logs:${NC}"
echo -e "${BLUE}  • All services: docker-compose -f $COMPOSE_FILE logs -f${NC}"
echo -e "${BLUE}  • Wallet only:  docker-compose -f $COMPOSE_FILE logs -f wallet-service${NC}"
echo ""
echo -e "${YELLOW}🛑 To stop:${NC}"
echo -e "${BLUE}  • docker-compose -f $COMPOSE_FILE down${NC}"
echo ""

# Quick health check
echo -e "${YELLOW}🔍 Quick health check...${NC}"
sleep 5

if curl -s http://localhost:8080/q/health | grep -q "UP"; then
    echo -e "${GREEN}✅ Wallet service is responding to health checks${NC}"
else
    echo -e "${RED}⚠️  Wallet service health check failed - check logs${NC}"
    echo -e "${BLUE}Run: docker-compose -f $COMPOSE_FILE logs wallet-service${NC}"
fi

echo ""
echo -e "${GREEN}🚀 Deployment complete! Ready for load testing.${NC}"
