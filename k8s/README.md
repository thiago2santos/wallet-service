# Kubernetes Migration Guide

This directory contains Kubernetes manifests to migrate your wallet service infrastructure from Docker Compose to Minikube.

## 📋 Overview

The migration includes all services from your original `docker-compose.yml`:

- **MySQL Primary/Replica** - Master-slave replication setup
- **Kafka Ecosystem** - Zookeeper, Kafka, Schema Registry, Kafka UI
- **Redis** - Caching layer
- **Monitoring** - Prometheus & Grafana
- **Topic Management** - Automated Kafka topic creation

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Minikube Cluster                         │
├─────────────────────────────────────────────────────────────┤
│  Namespace: wallet-service                                  │
│                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │   MySQL     │  │   MySQL     │  │    Redis    │        │
│  │  Primary    │  │   Replica   │  │             │        │
│  │ (StatefulSet│  │(StatefulSet)│  │(Deployment) │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
│                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │ Zookeeper   │  │    Kafka    │  │   Schema    │        │
│  │(StatefulSet)│  │(StatefulSet)│  │  Registry   │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
│                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │ Prometheus  │  │   Grafana   │  │  Kafka UI   │        │
│  │(Deployment) │  │(Deployment) │  │(Deployment) │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
└─────────────────────────────────────────────────────────────┘
```

## 📁 File Structure

```
k8s/
├── namespace.yaml          # Namespace definition
├── secrets.yaml           # Sensitive data (passwords)
├── configmap.yaml         # Configuration data
├── mysql.yaml             # MySQL primary/replica StatefulSets
├── redis.yaml             # Redis deployment
├── kafka.yaml             # Kafka ecosystem (Zookeeper, Kafka, Schema Registry, UI)
├── kafka-setup.yaml       # Job to create Kafka topics
├── monitoring.yaml        # Prometheus & Grafana
├── deploy.sh              # Automated deployment script
└── README.md              # This file
```

## 🚀 Quick Start

### Prerequisites

1. **Minikube** installed and running:
   ```bash
   minikube start --memory=8192 --cpus=4
   ```

2. **kubectl** configured to use minikube context:
   ```bash
   kubectl config use-context minikube
   ```

### Deployment

1. **Navigate to k8s directory**:
   ```bash
   cd k8s
   ```

2. **Run the deployment script**:
   ```bash
   ./deploy.sh
   ```

3. **Wait for all services to be ready** (the script handles this automatically)

## 🔌 Accessing Services

### NodePort Services (External Access)

Get your Minikube IP:
```bash
minikube ip
```

Then access services at:
- **Kafka UI**: `http://<minikube-ip>:30080`
- **Prometheus**: `http://<minikube-ip>:30090`
- **Grafana**: `http://<minikube-ip>:30300` (admin/admin)

### Port Forwarding (Local Access)

For local development, use port forwarding:

```bash
# Kafka UI
kubectl port-forward -n wallet-service svc/kafka-ui 8080:8080

# Prometheus
kubectl port-forward -n wallet-service svc/prometheus 9090:9090

# Grafana
kubectl port-forward -n wallet-service svc/grafana 3000:3000

# MySQL Primary
kubectl port-forward -n wallet-service svc/mysql-primary 3306:3306

# MySQL Replica
kubectl port-forward -n wallet-service svc/mysql-replica 3307:3306

# Redis
kubectl port-forward -n wallet-service svc/redis 6379:6379

# Kafka
kubectl port-forward -n wallet-service svc/kafka 9092:9092
```

## 🔍 Monitoring & Troubleshooting

### Check Deployment Status
```bash
kubectl get all -n wallet-service
```

### View Pod Logs
```bash
# View logs for a specific service
kubectl logs -n wallet-service -l app=<service-name> -f

# Examples:
kubectl logs -n wallet-service -l app=mysql-primary -f
kubectl logs -n wallet-service -l app=kafka -f
kubectl logs -n wallet-service -l app=redis -f
```

### Check Persistent Volumes
```bash
kubectl get pv,pvc -n wallet-service
```

### Describe Resources
```bash
kubectl describe pod -n wallet-service <pod-name>
kubectl describe statefulset -n wallet-service <statefulset-name>
```

## 🗄️ Data Persistence

All stateful services use Persistent Volume Claims (PVCs):

- **MySQL Primary**: 10Gi storage
- **MySQL Replica**: 10Gi storage
- **Kafka**: 10Gi storage
- **Zookeeper**: 5Gi storage
- **Redis**: 5Gi storage
- **Prometheus**: 10Gi storage
- **Grafana**: 5Gi storage

## 🔧 Configuration

### Secrets (Base64 Encoded)
- `MYSQL_ROOT_PASSWORD`: root
- `MYSQL_PASSWORD`: wallet
- `MYSQL_REPLICATION_PASSWORD`: repl

### ConfigMaps
All non-sensitive configuration is stored in ConfigMaps and can be modified as needed.

## 📊 Kafka Topics

The following topics are automatically created:
- `wallet-events` (6 partitions, compact cleanup)
- `wallet-commands` (6 partitions, delete cleanup)
- `wallet-snapshots` (6 partitions, compact cleanup)

## 🔄 Migration from Docker Compose

### Key Differences

1. **Networking**: Services communicate using Kubernetes DNS names
2. **Storage**: Persistent volumes replace Docker volumes
3. **Health Checks**: Kubernetes probes replace Docker health checks
4. **Scaling**: Easy horizontal scaling with `kubectl scale`
5. **Service Discovery**: Automatic service discovery within the cluster

### Connection Strings Update

Update your application configuration:

```properties
# Before (Docker Compose)
spring.datasource.url=jdbc:mysql://mysql-primary:3306/wallet

# After (Kubernetes)
spring.datasource.url=jdbc:mysql://mysql-primary.wallet-service.svc.cluster.local:3306/wallet

# Or use the short form (within same namespace)
spring.datasource.url=jdbc:mysql://mysql-primary:3306/wallet
```

## 🧹 Cleanup

To remove all resources:

```bash
kubectl delete namespace wallet-service
```

## 🔧 Customization

### Resource Limits
Adjust resource requests/limits in the YAML files based on your needs:

```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "200m"
  limits:
    memory: "1Gi"
    cpu: "500m"
```

### Storage
Modify storage sizes in PVC templates:

```yaml
resources:
  requests:
    storage: 20Gi  # Increase as needed
```

### Replicas
Scale StatefulSets and Deployments:

```bash
kubectl scale statefulset kafka --replicas=3 -n wallet-service
```

## 🚨 Important Notes

1. **MySQL Replication**: The replica setup requires the primary to be ready first
2. **Kafka Dependencies**: Kafka requires Zookeeper, Schema Registry requires Kafka
3. **Topic Creation**: The kafka-setup job runs after Kafka is ready
4. **Persistent Data**: Data persists across pod restarts but not cluster recreation
5. **Resource Requirements**: Ensure your Minikube has sufficient resources (8GB+ RAM recommended)

## 📞 Support

For issues or questions:
1. Check pod logs: `kubectl logs -n wallet-service <pod-name>`
2. Check events: `kubectl get events -n wallet-service --sort-by='.lastTimestamp'`
3. Verify resource status: `kubectl get all -n wallet-service`
