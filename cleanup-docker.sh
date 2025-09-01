#!/bin/bash

# Docker Compose Cleanup Script
# This script removes all Docker Compose resources for the wallet service

set -e

echo "🧹 Docker Compose Cleanup for Wallet Service"
echo "============================================"

# Function to check if Docker is running
check_docker() {
    if ! docker info >/dev/null 2>&1; then
        echo "ℹ️  Docker is not running. No cleanup needed."
        echo "   If you want to clean up Docker resources later, start Docker first:"
        echo "   colima start"
        return 1
    fi
    return 0
}

# Function to stop and remove containers
cleanup_containers() {
    echo "🛑 Stopping and removing Docker Compose services..."
    
    # Stop all services
    if docker-compose ps -q | grep -q .; then
        docker-compose down
    else
        echo "   No running services found."
    fi
    
    # Remove any remaining containers with wallet prefix
    echo "🗑️  Removing any remaining wallet containers..."
    docker ps -a --filter "name=wallet-" --format "table {{.Names}}" | tail -n +2 | while read container; do
        if [ ! -z "$container" ]; then
            echo "   Removing container: $container"
            docker rm -f "$container" 2>/dev/null || true
        fi
    done
}

# Function to remove volumes
cleanup_volumes() {
    echo "💾 Removing Docker volumes..."
    
    # Remove volumes defined in docker-compose.yml
    local volumes=(
        "wallet-service_mysql-primary-data"
        "wallet-service_mysql-replica-data"
        "wallet-service_kafka-data"
        "wallet-service_zookeeper-data"
        "wallet-service_redis-data"
        "wallet-service_prometheus-data"
        "wallet-service_grafana-data"
    )
    
    for volume in "${volumes[@]}"; do
        if docker volume ls -q | grep -q "^${volume}$"; then
            echo "   Removing volume: $volume"
            docker volume rm "$volume" 2>/dev/null || true
        fi
    done
    
    # Remove any other wallet-related volumes
    docker volume ls --filter "name=wallet" --format "table {{.Name}}" | tail -n +2 | while read volume; do
        if [ ! -z "$volume" ]; then
            echo "   Removing volume: $volume"
            docker volume rm "$volume" 2>/dev/null || true
        fi
    done
}

# Function to remove networks
cleanup_networks() {
    echo "🌐 Removing Docker networks..."
    
    # Remove the wallet network
    if docker network ls --filter "name=wallet-service_wallet-network" --format "table {{.Name}}" | grep -q "wallet-service_wallet-network"; then
        echo "   Removing network: wallet-service_wallet-network"
        docker network rm wallet-service_wallet-network 2>/dev/null || true
    fi
    
    # Remove any other wallet-related networks
    docker network ls --filter "name=wallet" --format "table {{.Name}}" | tail -n +2 | while read network; do
        if [ ! -z "$network" ] && [ "$network" != "bridge" ] && [ "$network" != "host" ] && [ "$network" != "none" ]; then
            echo "   Removing network: $network"
            docker network rm "$network" 2>/dev/null || true
        fi
    done
}

# Function to remove images (optional)
cleanup_images() {
    echo "🖼️  Removing unused Docker images..."
    
    # Remove dangling images
    if docker images -f "dangling=true" -q | grep -q .; then
        docker image prune -f
    fi
    
    echo "   Note: Service images (mysql, kafka, etc.) are kept for potential reuse."
    echo "   To remove all unused images, run: docker image prune -a"
}

# Main cleanup function
main() {
    echo "This script will clean up all Docker Compose resources for the wallet service:"
    echo "  • Stop and remove all containers"
    echo "  • Remove all volumes (⚠️  DATA WILL BE LOST)"
    echo "  • Remove networks"
    echo "  • Clean up unused images"
    echo ""
    
    # Check if Docker is running
    if ! check_docker; then
        exit 0
    fi
    
    # Confirm cleanup
    read -p "Are you sure you want to continue? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "❌ Cleanup cancelled."
        exit 1
    fi
    
    # Perform cleanup
    cleanup_containers
    cleanup_volumes
    cleanup_networks
    cleanup_images
    
    echo ""
    echo "✅ Docker Compose cleanup completed!"
    echo ""
    echo "🎯 Next steps:"
    echo "   1. Your Kubernetes cluster is ready to use"
    echo "   2. Deploy to Kubernetes: cd k8s && ./deploy.sh"
    echo "   3. Optional: Stop Docker to save resources: colima stop"
    echo ""
    echo "📊 Verify cleanup:"
    echo "   docker ps -a        # Should show no wallet containers"
    echo "   docker volume ls    # Should show no wallet volumes"
    echo "   docker network ls   # Should show no wallet networks"
}

# Run main function
main "$@"

