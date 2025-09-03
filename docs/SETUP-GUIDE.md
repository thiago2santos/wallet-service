# 🛠️ Setup Guide

> Step-by-step instructions for installation, testing, and running the Wallet Service

## 📋 Prerequisites

### Required Software
- **Java 17+** (OpenJDK or Oracle JDK)
- **Docker** (version 20.10+)
- **Docker Compose** (version 2.0+)
- **Maven** (version 3.8+)
- **Git**

### Verify Prerequisites
```bash
# Check Java version
java -version
# Should show Java 17 or higher

# Check Docker
docker --version
docker-compose --version

# Check Maven
mvn --version
```

## 🚀 Quick Start (Recommended)

### 1. Clone Repository
```bash
git clone https://github.com/thiago2santos/wallet-service.git
cd wallet-service
```

### 2. Start All Services
```bash
# Start all services with Docker Compose
docker-compose up -d

# Wait for services to be ready (30-60 seconds)
# Check service health
curl http://localhost:8080/health
```

### 3. Verify Installation
```bash
# Check all services are running
docker-compose ps

# Test API endpoint
curl http://localhost:8080/api/v1/wallets \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"userId": "test-user"}'
```

**Expected Response**:
```json
{
  "walletId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "test-user",
  "balance": "0.00",
  "currency": "BRL",
  "status": "ACTIVE",
  "createdAt": "2024-01-01T10:00:00Z"
}
```

## 🛠️ Development Setup

### 1. Start Infrastructure Services
```bash
# Start only infrastructure (not the app)
docker-compose up -d mysql-primary mysql-replica redis kafka zookeeper
```

### 2. Run Application in Development Mode
```bash
# Start Quarkus in dev mode (with live reload)
./mvnw quarkus:dev
```

### 3. Access Development Tools
- **Application**: http://localhost:8080
- **Dev UI**: http://localhost:8080/q/dev/
- **API Documentation**: http://localhost:8080/q/swagger-ui/
- **Health Checks**: http://localhost:8080/q/health

## 🧪 Testing Instructions

### Unit Tests
```bash
# Run all unit tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=CreateWalletCommandHandlerTest

# Run tests with coverage
./mvnw test jacoco:report
```

### Integration Tests
```bash
# Run integration tests
./mvnw test -Dtest="*IntegrationTest"

# Run specific integration test
./mvnw test -Dtest=WalletOperationsIntegrationTest
```

### Mutation Testing
```bash
# Run mutation testing (validates test quality)
./mvnw org.pitest:pitest-maven:mutationCoverage

# View results
open target/pit-reports/index.html
```

### Performance Testing
```bash
# Run basic performance test
./scripts/setup-load-test.sh

# Monitor performance during test
curl http://localhost:8080/metrics | grep wallet_operations
```

## 🔧 API Testing

### Create a Wallet
```bash
curl -X POST http://localhost:8080/api/v1/wallets \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123"
  }'
```

### Deposit Funds
```bash
# Replace {walletId} with actual wallet ID from previous response
curl -X POST http://localhost:8080/api/v1/wallets/{walletId}/deposit \
  -H "Content-Type: application/json" \
  -d '{
    "amount": "100.00",
    "referenceId": "deposit-001",
    "description": "Initial deposit"
  }'
```

### Check Balance
```bash
curl http://localhost:8080/api/v1/wallets/{walletId}/balance
```

### Withdraw Funds
```bash
curl -X POST http://localhost:8080/api/v1/wallets/{walletId}/withdraw \
  -H "Content-Type: application/json" \
  -d '{
    "amount": "25.00",
    "referenceId": "withdraw-001",
    "description": "ATM withdrawal"
  }'
```

### Transfer Between Wallets
```bash
# Create second wallet first, then transfer
curl -X POST http://localhost:8080/api/v1/wallets/{sourceWalletId}/transfer \
  -H "Content-Type: application/json" \
  -d '{
    "destinationWalletId": "{destinationWalletId}",
    "amount": "50.00",
    "referenceId": "transfer-001",
    "description": "Payment to friend"
  }'
```

### Historical Balance
```bash
curl "http://localhost:8080/api/v1/wallets/{walletId}/balance/historical?timestamp=2024-01-01T10:30:00"
```

## 📊 Monitoring and Health Checks

### Application Health
```bash
# Overall health status
curl http://localhost:8080/q/health

# Detailed health information
curl http://localhost:8080/q/health | jq
```

### Prometheus Metrics
```bash
# All metrics
curl http://localhost:8080/metrics

# Wallet-specific metrics
curl http://localhost:8080/metrics | grep wallet_operations

# JVM metrics
curl http://localhost:8080/metrics | grep jvm
```

### Database Health
```bash
# Check database connectivity
curl http://localhost:8080/q/health/ready

# Check specific database connections
docker-compose exec mysql-primary mysql -u wallet -ppassword -e "SELECT 1"
```

### Kafka Health
```bash
# Check Kafka topics
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# Check recent events
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic wallet-events \
  --from-beginning \
  --max-messages 10
```

## 🐳 Docker Commands

### Service Management
```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down

# Restart specific service
docker-compose restart wallet-service

# View logs
docker-compose logs -f wallet-service
```

### Database Management
```bash
# Connect to primary database
docker-compose exec mysql-primary mysql -u wallet -ppassword wallet_db

# Connect to replica database
docker-compose exec mysql-replica mysql -u wallet -ppassword wallet_db

# Reset database (WARNING: deletes all data)
docker-compose down -v
docker-compose up -d
```

### Redis Management
```bash
# Connect to Redis
docker-compose exec redis redis-cli

# Check cached data
docker-compose exec redis redis-cli KEYS "wallet:*"

# Clear cache
docker-compose exec redis redis-cli FLUSHALL
```

## 🔍 Troubleshooting

### Common Issues

#### Port Already in Use
```bash
# Check what's using port 8080
lsof -i :8080

# Kill process using port
kill -9 $(lsof -t -i:8080)
```

#### Database Connection Issues
```bash
# Check database status
docker-compose ps mysql-primary mysql-replica

# Restart databases
docker-compose restart mysql-primary mysql-replica

# Check database logs
docker-compose logs mysql-primary
```

#### Application Won't Start
```bash
# Check Java version
java -version

# Clean and rebuild
./mvnw clean compile

# Check for port conflicts
netstat -tulpn | grep :8080
```

#### Performance Issues
```bash
# Check system resources
docker stats

# Check application metrics
curl http://localhost:8080/metrics | grep -E "(cpu|memory|gc)"

# Check database performance
docker-compose exec mysql-primary mysql -u wallet -ppassword -e "SHOW PROCESSLIST"
```

### Log Locations
- **Application Logs**: `docker-compose logs wallet-service`
- **Database Logs**: `docker-compose logs mysql-primary`
- **Kafka Logs**: `docker-compose logs kafka`
- **Application File Log**: `./app.log` (when running in dev mode)

### Reset Everything
```bash
# Complete reset (WARNING: deletes all data)
docker-compose down -v
docker system prune -f
./mvnw clean
docker-compose up -d
```

## 🚀 Production Deployment Notes

### Environment Variables
```bash
# Required for production
export QUARKUS_PROFILE=prod
export MYSQL_PRIMARY_URL=jdbc:mysql://prod-primary:3306/wallet_db
export MYSQL_REPLICA_URL=jdbc:mysql://prod-replica:3306/wallet_db
export REDIS_URL=redis://prod-redis:6379
export KAFKA_BOOTSTRAP_SERVERS=prod-kafka:9092
```

### Build for Production
```bash
# Build JVM version
./mvnw package

# Build native version (requires GraalVM)
./mvnw package -Dnative

# Build Docker image
docker build -f src/main/docker/Dockerfile.jvm -t wallet-service .
```

### Security Considerations
- Configure AWS API Gateway for authentication
- Set up AWS WAF for application protection
- Use AWS RDS for managed database
- Configure VPC and security groups
- Enable HTTPS/TLS termination

---

## ✅ Verification Checklist

After setup, verify these work:

- [ ] Application starts successfully
- [ ] Health check returns HTTP 200
- [ ] Can create a wallet
- [ ] Can deposit funds
- [ ] Can check balance
- [ ] Can withdraw funds
- [ ] Can transfer between wallets
- [ ] Can query historical balance
- [ ] Metrics are available
- [ ] Database connections work
- [ ] Kafka events are published

## 🆘 Getting Help

If you encounter issues:

1. **Check the logs**: `docker-compose logs -f`
2. **Verify prerequisites**: Ensure Java 17+, Docker, Maven are installed
3. **Reset environment**: `docker-compose down -v && docker-compose up -d`
4. **Check GitHub Issues**: [Issues](https://github.com/thiago2santos/wallet-service/issues)
5. **Create new issue**: Include logs and system information

---

**Setup complete! 🎉 Your Wallet Service is ready for testing.**
