# Wallet Service Kubernetes Management
.PHONY: help deploy status logs clean port-forward-all

# Default target
help: ## Show this help message
	@echo "Wallet Service Kubernetes Management"
	@echo "===================================="
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

deploy: ## Deploy all services to Kubernetes
	@echo "🚀 Deploying wallet service to Kubernetes..."
	./deploy.sh

status: ## Show status of all resources
	@echo "📊 Wallet Service Status:"
	@echo "========================"
	kubectl get all -n wallet-service
	@echo ""
	@echo "💾 Storage:"
	@echo "==========="
	kubectl get pv,pvc -n wallet-service

logs: ## Show logs for all services
	@echo "📋 Recent logs from all services:"
	@echo "================================="
	kubectl logs -n wallet-service -l app=mysql-primary --tail=10 --prefix=true || true
	kubectl logs -n wallet-service -l app=mysql-replica --tail=10 --prefix=true || true
	kubectl logs -n wallet-service -l app=redis --tail=10 --prefix=true || true
	kubectl logs -n wallet-service -l app=zookeeper --tail=10 --prefix=true || true
	kubectl logs -n wallet-service -l app=kafka --tail=10 --prefix=true || true
	kubectl logs -n wallet-service -l app=schema-registry --tail=10 --prefix=true || true

clean: ## Remove all wallet service resources
	@echo "🧹 Cleaning up wallet service resources..."
	./cleanup.sh

# Port forwarding targets
port-forward-kafka-ui: ## Port forward Kafka UI to localhost:8080
	@echo "🔌 Port forwarding Kafka UI to localhost:8080"
	kubectl port-forward -n wallet-service svc/kafka-ui 8080:8080

port-forward-prometheus: ## Port forward Prometheus to localhost:9090
	@echo "🔌 Port forwarding Prometheus to localhost:9090"
	kubectl port-forward -n wallet-service svc/prometheus 9090:9090

port-forward-grafana: ## Port forward Grafana to localhost:3000
	@echo "🔌 Port forwarding Grafana to localhost:3000"
	kubectl port-forward -n wallet-service svc/grafana 3000:3000

port-forward-mysql: ## Port forward MySQL primary to localhost:3306
	@echo "🔌 Port forwarding MySQL primary to localhost:3306"
	kubectl port-forward -n wallet-service svc/mysql-primary 3306:3306

port-forward-redis: ## Port forward Redis to localhost:6379
	@echo "🔌 Port forwarding Redis to localhost:6379"
	kubectl port-forward -n wallet-service svc/redis 6379:6379

port-forward-kafka: ## Port forward Kafka to localhost:9092
	@echo "🔌 Port forwarding Kafka to localhost:9092"
	kubectl port-forward -n wallet-service svc/kafka 9092:9092

# Monitoring targets
watch: ## Watch pod status in real-time
	watch kubectl get pods -n wallet-service

events: ## Show recent events
	kubectl get events -n wallet-service --sort-by='.lastTimestamp'

describe-failed: ## Describe failed pods
	@echo "🔍 Describing failed pods:"
	@kubectl get pods -n wallet-service --field-selector=status.phase!=Running,status.phase!=Succeeded -o name | xargs -I {} kubectl describe -n wallet-service {}

# Development targets
restart-kafka: ## Restart Kafka StatefulSet
	kubectl rollout restart statefulset/kafka -n wallet-service

restart-mysql: ## Restart MySQL StatefulSets
	kubectl rollout restart statefulset/mysql-primary -n wallet-service
	kubectl rollout restart statefulset/mysql-replica -n wallet-service

scale-kafka: ## Scale Kafka to 3 replicas
	kubectl scale statefulset kafka --replicas=3 -n wallet-service

# Backup targets
backup-mysql: ## Create MySQL backup
	@echo "💾 Creating MySQL backup..."
	kubectl exec -n wallet-service mysql-primary-0 -- mysqldump -u wallet -pwallet wallet > backup-$$(date +%Y%m%d-%H%M%S).sql
	@echo "✅ Backup created: backup-$$(date +%Y%m%d-%H%M%S).sql"

# Testing targets
test-connectivity: ## Test connectivity between services
	@echo "🔗 Testing service connectivity..."
	kubectl run test-pod --rm -i --tty --image=busybox -n wallet-service -- /bin/sh
