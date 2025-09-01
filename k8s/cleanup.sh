#!/bin/bash

# Wallet Service Kubernetes Cleanup Script
# This script removes all wallet service resources from Kubernetes

set -e

echo "🧹 Starting Wallet Service Kubernetes Cleanup..."

# Check if kubectl is available
if ! command -v kubectl &> /dev/null; then
    echo "❌ kubectl is not installed. Please install kubectl first."
    exit 1
fi

# Check if minikube is running
if ! kubectl cluster-info &> /dev/null; then
    echo "❌ Kubernetes cluster is not accessible. Please start minikube first:"
    echo "   minikube start"
    exit 1
fi

# Confirm deletion
echo "⚠️  This will delete ALL wallet service resources including:"
echo "   - All deployments and statefulsets"
echo "   - All persistent volumes and data"
echo "   - All services and configmaps"
echo "   - The entire wallet-service namespace"
echo ""
read -p "Are you sure you want to continue? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ Cleanup cancelled."
    exit 1
fi

echo "🗑️ Deleting wallet-service namespace and all resources..."
kubectl delete namespace wallet-service --ignore-not-found=true

echo "⏳ Waiting for namespace deletion to complete..."
while kubectl get namespace wallet-service &> /dev/null; do
    echo "   Still deleting..."
    sleep 5
done

echo "✅ Cleanup completed successfully!"
echo ""
echo "🔍 Verify cleanup:"
echo "   kubectl get all -n wallet-service"
echo "   (Should return 'No resources found')"
