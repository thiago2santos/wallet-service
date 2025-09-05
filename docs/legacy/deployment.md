# Deployment Guide

> Production deployment strategies for the Wallet Service

## 🎯 Deployment Options

### 🐳 Docker Deployment

#### Single Container
```bash
# Build application
./mvnw package

# Build Docker image
docker build -t wallet-service:latest .

# Run container
docker run -d \
  --name wallet-service \
  -p 8080:8080 \
  -e QUARKUS_PROFILE=prod \
  wallet-service:latest
```

#### Docker Compose (Production)
```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  wallet-service:
    image: wallet-service:latest
    ports:
      - "8080:8080"
    environment:
      - QUARKUS_PROFILE=prod
      - QUARKUS_DATASOURCE_WRITE_REACTIVE_URL=vertx-reactive:mysql://mysql-primary:3306/wallet
      - QUARKUS_DATASOURCE_READ_REACTIVE_URL=vertx-reactive:mysql://mysql-replica:3306/wallet
      - QUARKUS_REDIS_HOSTS=redis://redis-cluster:6379
    depends_on:
      - mysql-primary
      - redis-cluster
    deploy:
      replicas: 3
      resources:
        limits:
          memory: 512M
          cpus: '0.5'
        reservations:
          memory: 256M
          cpus: '0.25'

  mysql-primary:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: wallet
    volumes:
      - mysql_data:/var/lib/mysql
    deploy:
      replicas: 1
      placement:
        constraints:
          - node.role == manager

  redis-cluster:
    image: redis:7-alpine
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    deploy:
      replicas: 3

volumes:
  mysql_data:
  redis_data:
```

### ☸️ Kubernetes Deployment

#### Namespace
```yaml
# k8s/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: wallet-service
  labels:
    name: wallet-service
```

#### ConfigMap
```yaml
# k8s/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: wallet-config
  namespace: wallet-service
data:
  application.properties: |
    quarkus.profile=prod
    quarkus.hibernate-orm.database.generation=validate
    quarkus.log.level=INFO
    quarkus.micrometer.export.prometheus.enabled=true
```

#### Secrets
```yaml
# k8s/secrets.yaml
apiVersion: v1
kind: Secret
metadata:
  name: wallet-secrets
  namespace: wallet-service
type: Opaque
data:
  mysql-password: <base64-encoded-password>
  redis-password: <base64-encoded-password>

```

#### Deployment
```yaml
# k8s/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: wallet-service
  namespace: wallet-service
  labels:
    app: wallet-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: wallet-service
  template:
    metadata:
      labels:
        app: wallet-service
    spec:
      containers:
      - name: wallet-service
        image: wallet-service:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: QUARKUS_PROFILE
          value: "prod"
        - name: MYSQL_PASSWORD
          valueFrom:
            secretKeyRef:
              name: wallet-secrets
              key: mysql-password
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /q/health/live
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /q/health/ready
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
        volumeMounts:
        - name: config
          mountPath: /deployments/config
      volumes:
      - name: config
        configMap:
          name: wallet-config
```

#### Service
```yaml
# k8s/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: wallet-service
  namespace: wallet-service
spec:
  selector:
    app: wallet-service
  ports:
  - port: 80
    targetPort: 8080
  type: ClusterIP
```

#### Ingress
```yaml
# k8s/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: wallet-ingress
  namespace: wallet-service
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  tls:
  - hosts:
    - api.wallet-service.com
    secretName: wallet-tls
  rules:
  - host: api.wallet-service.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: wallet-service
            port:
              number: 80
```

### 🚀 Native Deployment

#### Build Native Image
```bash
# Build native executable
./mvnw package -Dnative

# Build native Docker image
docker build -f src/main/docker/Dockerfile.native -t wallet-service:native .

# Run native container
docker run -i --rm -p 8080:8080 wallet-service:native
```

#### Native Dockerfile
```dockerfile
# src/main/docker/Dockerfile.native
FROM quay.io/quarkus/quarkus-micro-image:2.0
WORKDIR /work/
COPY target/*-runner /work/application

EXPOSE 8080
USER 1001

ENTRYPOINT ["./application", "-Dquarkus.http.host=0.0.0.0"]
```

## 🌐 Cloud Deployments

### AWS EKS

#### EKS Cluster Setup
```bash
# Create EKS cluster
eksctl create cluster \
  --name wallet-service-cluster \
  --region us-west-2 \
  --nodegroup-name standard-workers \
  --node-type t3.medium \
  --nodes 3 \
  --nodes-min 1 \
  --nodes-max 4 \
  --managed

# Configure kubectl
aws eks update-kubeconfig --region us-west-2 --name wallet-service-cluster
```

#### AWS Load Balancer Controller
```bash
# Install AWS Load Balancer Controller
helm repo add eks https://aws.github.io/eks-charts
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=wallet-service-cluster
```

#### RDS Integration
```yaml
# k8s/rds-secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: rds-credentials
type: Opaque
data:
  endpoint: <base64-encoded-rds-endpoint>
  username: <base64-encoded-username>
  password: <base64-encoded-password>
```

### AWS EKS (Recommended)

#### GKE Cluster
```bash
# Create GKE cluster
gcloud container clusters create wallet-service-cluster \
  --zone us-central1-a \
  --num-nodes 3 \
  --enable-autoscaling \
  --min-nodes 1 \
  --max-nodes 10

# Get credentials
gcloud container clusters get-credentials wallet-service-cluster --zone us-central1-a
```

### AWS ECS (Alternative)

#### AKS Cluster
```bash
# Create resource group
az group create --name wallet-service-rg --location eastus

# Create AKS cluster
az aks create \
  --resource-group wallet-service-rg \
  --name wallet-service-cluster \
  --node-count 3 \
  --enable-addons monitoring \
  --generate-ssh-keys

# Get credentials
az aks get-credentials --resource-group wallet-service-rg --name wallet-service-cluster
```

## 📊 Monitoring Setup

### Prometheus & Grafana

#### Prometheus Configuration
```yaml
# k8s/prometheus.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
    scrape_configs:
    - job_name: 'wallet-service'
      kubernetes_sd_configs:
      - role: endpoints
      relabel_configs:
      - source_labels: [__meta_kubernetes_service_name]
        action: keep
        regex: wallet-service
```

#### Grafana Dashboard
```json
{
  "dashboard": {
    "title": "Wallet Service Metrics",
    "panels": [
      {
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_requests_total[5m])",
            "legendFormat": "{{method}} {{uri}}"
          }
        ]
      },
      {
        "title": "Response Time",
        "type": "graph", 
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))",
            "legendFormat": "95th percentile"
          }
        ]
      }
    ]
  }
}
```

## 🔒 Security Configuration

### TLS/SSL Setup
```yaml
# k8s/tls-secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: wallet-tls
  namespace: wallet-service
type: kubernetes.io/tls
data:
  tls.crt: <base64-encoded-certificate>
  tls.key: <base64-encoded-private-key>
```

### Network Policies
```yaml
# k8s/network-policy.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: wallet-service-netpol
  namespace: wallet-service
spec:
  podSelector:
    matchLabels:
      app: wallet-service
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - namespaceSelector:
        matchLabels:
          name: database
    ports:
    - protocol: TCP
      port: 3306
```

## 🚀 CI/CD Pipeline

### CI/CD Pipeline
```yaml
# Example deployment configuration
name: Deploy to Production

# Manual deployment trigger:
    branches: [main]
    tags: ['v*']

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Build application
      run: ./mvnw package -Dnative -Dquarkus.native.container-build=true
    
    - name: Build Docker image
      run: |
        docker build -f src/main/docker/Dockerfile.native \
          -t ${{ secrets.REGISTRY }}/wallet-service:${{ github.sha }} .
    
    - name: Push to registry
      run: |
        echo ${{ secrets.REGISTRY_PASSWORD }} | docker login ${{ secrets.REGISTRY }} -u ${{ secrets.REGISTRY_USERNAME }} --password-stdin
        docker push ${{ secrets.REGISTRY }}/wallet-service:${{ github.sha }}
    
    - name: Deploy to Kubernetes
      run: |
        kubectl set image deployment/wallet-service \
          wallet-service=${{ secrets.REGISTRY }}/wallet-service:${{ github.sha }} \
          -n wallet-service
```

### GitLab CI
```yaml
# .gitlab-ci.yml
stages:
  - build
  - test
  - deploy

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"

cache:
  paths:
    - .m2/repository/

build:
  stage: build
  image: maven:3.8-openjdk-17
  script:
    - ./mvnw package -Dnative -Dquarkus.native.container-build=true
  artifacts:
    paths:
      - target/

deploy:
  stage: deploy
  image: bitnami/kubectl:latest
  script:
    - kubectl apply -f k8s/
    - kubectl set image deployment/wallet-service wallet-service=$CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
  only:
    - main
```

## 🔧 Environment Configuration

### Production Properties
```properties
# application-prod.properties
quarkus.profile=prod

# Database
quarkus.datasource.write.reactive.url=vertx-reactive:mysql://mysql-primary:3306/wallet
quarkus.datasource.read.reactive.url=vertx-reactive:mysql://mysql-replica:3306/wallet
quarkus.hibernate-orm.database.generation=validate

# Security
quarkus.http.cors=false
quarkus.ssl.native=true

# Monitoring
quarkus.micrometer.export.prometheus.enabled=true
quarkus.log.level=INFO
quarkus.log.category."com.wallet".level=INFO

# Performance
quarkus.thread-pool.max-threads=200
quarkus.vertx.event-loops-pool-size=8
```

### Health Checks
```java
@ApplicationScoped
public class DatabaseHealthCheck implements HealthCheck {
    
    @Inject
    @ReactiveDataSource("write")
    MySQLPool writePool;
    
    @Override
    public HealthCheckResponse call() {
        try {
            writePool.query("SELECT 1").execute()
                .await().atMost(Duration.ofSeconds(5));
            return HealthCheckResponse.up("database");
        } catch (Exception e) {
            return HealthCheckResponse.down("database");
        }
    }
}
```

## 📈 Scaling Strategies

### Horizontal Pod Autoscaler
```yaml
# k8s/hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: wallet-service-hpa
  namespace: wallet-service
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: wallet-service
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### Vertical Pod Autoscaler
```yaml
# k8s/vpa.yaml
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: wallet-service-vpa
  namespace: wallet-service
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: wallet-service
  updatePolicy:
    updateMode: "Auto"
  resourcePolicy:
    containerPolicies:
    - containerName: wallet-service
      maxAllowed:
        cpu: 1
        memory: 1Gi
      minAllowed:
        cpu: 100m
        memory: 128Mi
```

## 🔄 Blue-Green Deployment

### Blue-Green Strategy
```bash
# Deploy to green environment
kubectl apply -f k8s/green/ -n wallet-service

# Test green environment
kubectl port-forward svc/wallet-service-green 8080:80 -n wallet-service

# Switch traffic to green
kubectl patch service wallet-service -p '{"spec":{"selector":{"version":"green"}}}' -n wallet-service

# Remove blue environment
kubectl delete -f k8s/blue/ -n wallet-service
```

## 🚨 Disaster Recovery

### Backup Strategy
```bash
# Database backup
kubectl create job --from=cronjob/mysql-backup mysql-backup-$(date +%Y%m%d-%H%M%S)

# Application state backup
kubectl create backup wallet-service-backup --include-namespaces=wallet-service
```

### Recovery Procedures
```bash
# Restore from backup
kubectl restore wallet-service-restore --from-backup=wallet-service-backup

# Verify restoration
kubectl get pods -n wallet-service
kubectl logs -f deployment/wallet-service -n wallet-service
```

---

**Ready for production! 🚀**
