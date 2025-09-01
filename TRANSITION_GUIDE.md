# 🚀 Docker to Kubernetes Transition Guide

## 🎯 Current Status

✅ **Migration Complete**: Your infrastructure has been successfully migrated from Docker Compose to Kubernetes!

## 📊 What Changed

### Before (Docker Compose)
```bash
# Start infrastructure
docker-compose up -d

# Access services
curl http://localhost:8080  # Kafka UI
curl http://localhost:9090  # Prometheus
curl http://localhost:3000  # Grafana
```

### After (Kubernetes)
```bash
# Start infrastructure
cd k8s && ./deploy.sh

# Access services (via NodePort)
minikube ip  # Get cluster IP
# Kafka UI: http://<minikube-ip>:30080
# Prometheus: http://<minikube-ip>:30090
# Grafana: http://<minikube-ip>:30300

# Or use port forwarding for localhost access
kubectl port-forward -n wallet-service svc/kafka-ui 8080:8080
```

## 🧹 Cleaning Up Docker Resources

Since Docker/Colima is not currently running, there are no active containers to clean up. However, when you're ready to completely transition:

### Option 1: Complete Docker Cleanup (Recommended)
```bash
# If you want to clean up any Docker resources later
./cleanup-docker.sh
```

### Option 2: Manual Cleanup (if needed)
```bash
# Start Docker first (if needed)
colima start

# Stop any running containers
docker-compose down

# Remove volumes (⚠️ DATA WILL BE LOST)
docker-compose down -v

# Remove unused images
docker image prune -a

# Stop Docker to save resources
colima stop
```

## 🎯 Recommended Next Steps

### 1. **Deploy to Kubernetes** (Primary Infrastructure)
```bash
# Start Minikube with sufficient resources
minikube start --memory=8192 --cpus=4

# Deploy everything
cd k8s
./deploy.sh

# Verify deployment
kubectl get all -n wallet-service
```

### 2. **Update Your Application Configuration**
Update your wallet service application to use Kubernetes service names:

```properties
# Before (Docker Compose)
spring.datasource.url=jdbc:mysql://mysql-primary:3306/wallet
spring.redis.host=redis
spring.kafka.bootstrap-servers=kafka:9092

# After (Kubernetes) - Same names work!
spring.datasource.url=jdbc:mysql://mysql-primary:3306/wallet
spring.redis.host=redis
spring.kafka.bootstrap-servers=kafka:9092
```

### 3. **Create Your Application Deployment**
Create Kubernetes manifests for your wallet service application:

```bash
# I can help you create these files
touch k8s/wallet-app.yaml
```

### 4. **Optional: Remove Docker Compose Files**
Once you're confident with Kubernetes:

```bash
# Keep for reference or remove completely
rm docker-compose.yml.deprecated
rm cleanup-docker.sh

# Or just keep the deprecation notice
# (current docker-compose.yml is already marked as deprecated)
```

## 🔄 Development Workflow

### Daily Operations
```bash
# Check status
make status

# View logs
make logs

# Port forward for development
make port-forward-kafka-ui
make port-forward-grafana

# Restart services
make restart-kafka
make restart-mysql
```

### Troubleshooting
```bash
# Check failed pods
make describe-failed

# Watch pods in real-time
make watch

# View recent events
make events
```

## 📈 Benefits of the Migration

### ✅ What You Gained
1. **Production-Ready**: Kubernetes is production-grade orchestration
2. **Auto-Recovery**: Failed pods restart automatically
3. **Better Monitoring**: Built-in health checks and metrics
4. **Scalability**: Easy horizontal scaling
5. **Resource Management**: CPU/memory limits and requests
6. **Service Discovery**: Built-in DNS-based discovery
7. **Rolling Updates**: Zero-downtime deployments
8. **Cloud Ready**: Easy migration to cloud providers

### 🔧 Operational Improvements
- **Health Checks**: Liveness and readiness probes
- **Resource Limits**: Prevent resource exhaustion
- **Persistent Storage**: Better volume management
- **Networking**: Improved service-to-service communication
- **Security**: RBAC and security contexts ready

## 🎮 Quick Commands Reference

### Kubernetes Management
```bash
# Deploy everything
cd k8s && ./deploy.sh

# Check status
kubectl get all -n wallet-service

# Access Kafka UI locally
kubectl port-forward -n wallet-service svc/kafka-ui 8080:8080

# View logs
kubectl logs -n wallet-service -l app=kafka -f

# Clean up everything
cd k8s && ./cleanup.sh
```

### Using Makefile (Recommended)
```bash
cd k8s

make deploy          # Deploy everything
make status          # Check all resources
make logs            # View recent logs
make port-forward-kafka-ui  # Access Kafka UI
make clean           # Remove everything
```

## 🚨 Important Notes

1. **Data Migration**: Your Kubernetes setup starts fresh (no data migration from Docker volumes)
2. **Port Changes**: Some services now use NodePort (30080, 30090, 30300) for external access
3. **Service Names**: Internal service names remain the same for easy application migration
4. **Resource Requirements**: Kubernetes needs more resources than Docker Compose
5. **Persistent Storage**: Data persists across pod restarts but not cluster recreation

## 🎉 You're All Set!

Your infrastructure is now running on Kubernetes! The Docker Compose setup is deprecated and can be safely ignored. All future development should use the Kubernetes manifests in the `k8s/` directory.

### Next Steps:
1. ✅ Docker Compose deprecated
2. ✅ Kubernetes manifests ready
3. 🎯 Deploy to Kubernetes: `cd k8s && ./deploy.sh`
4. 🔧 Update your application configuration
5. 🚀 Enjoy the benefits of Kubernetes orchestration!

---

**Need Help?** Check the comprehensive documentation in `k8s/README.md` or the migration summary in `KUBERNETES_MIGRATION.md`.
