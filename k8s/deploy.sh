#!/bin/bash

# Wallet Service Kubernetes Deployment Script
# This script deploys the entire wallet service infrastructure to Minikube

set -e

echo "🚀 Starting Wallet Service Kubernetes Deployment..."

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

# Function to wait for deployment to be ready
wait_for_deployment() {
    local namespace=$1
    local deployment=$2
    echo "⏳ Waiting for $deployment to be ready..."
    kubectl wait --for=condition=available --timeout=300s deployment/$deployment -n $namespace
}

# Function to wait for statefulset to be ready
wait_for_statefulset() {
    local namespace=$1
    local statefulset=$2
    echo "⏳ Waiting for $statefulset to be ready..."
    kubectl wait --for=jsonpath='{.status.readyReplicas}'=1 --timeout=300s statefulset/$statefulset -n $namespace
}

# Function to wait for job to complete
wait_for_job() {
    local namespace=$1
    local job=$2
    echo "⏳ Waiting for job $job to complete..."
    kubectl wait --for=condition=complete --timeout=300s job/$job -n $namespace
}

echo "📁 Creating namespace..."
kubectl apply -f namespace.yaml

echo "🔐 Creating secrets and configmaps..."
kubectl apply -f secrets.yaml
kubectl apply -f configmap.yaml

echo "🗄️ Deploying MySQL (Primary and Replica)..."
kubectl apply -f mysql.yaml
wait_for_statefulset wallet-service mysql-primary
wait_for_statefulset wallet-service mysql-replica

echo "🔴 Deploying Redis..."
kubectl apply -f redis.yaml
wait_for_deployment wallet-service redis

echo "📊 Deploying Zookeeper..."
kubectl apply -f kafka.yaml
wait_for_statefulset wallet-service zookeeper

echo "📨 Deploying Kafka..."
wait_for_statefulset wallet-service kafka

echo "📋 Deploying Schema Registry..."
wait_for_deployment wallet-service schema-registry

echo "🖥️ Deploying Kafka UI..."
wait_for_deployment wallet-service kafka-ui

echo "🔧 Setting up Kafka topics..."
kubectl apply -f kafka-setup.yaml
wait_for_job wallet-service kafka-setup

echo "📈 Deploying monitoring stack (Prometheus & Grafana)..."
kubectl apply -f monitoring.yaml
wait_for_deployment wallet-service prometheus
wait_for_deployment wallet-service grafana

echo "✅ Deployment completed successfully!"
echo ""
echo "🌐 Access URLs (use 'minikube ip' to get the cluster IP):"
echo "   Kafka UI:    http://$(minikube ip):30080"
echo "   Prometheus:  http://$(minikube ip):30090"
echo "   Grafana:     http://$(minikube ip):30300 (admin/admin)"
echo ""
echo "🔌 Port forwarding commands for local access:"
echo "   kubectl port-forward -n wallet-service svc/kafka-ui 8080:8080"
echo "   kubectl port-forward -n wallet-service svc/prometheus 9090:9090"
echo "   kubectl port-forward -n wallet-service svc/grafana 3000:3000"
echo "   kubectl port-forward -n wallet-service svc/mysql-primary 3306:3306"
echo "   kubectl port-forward -n wallet-service svc/mysql-replica 3307:3306"
echo "   kubectl port-forward -n wallet-service svc/redis 6379:6379"
echo "   kubectl port-forward -n wallet-service svc/kafka 9092:9092"
echo ""
echo "📊 Check deployment status:"
echo "   kubectl get all -n wallet-service"
echo ""
echo "🔍 View logs:"
echo "   kubectl logs -n wallet-service -l app=<service-name> -f"
