# Development Guide

> Everything you need to know to contribute to the Wallet Service

## 🚀 Quick Setup

### Prerequisites

- **Java 17+** (OpenJDK or Oracle JDK)
- **Maven 3.8+** 
- **Docker & Docker Compose**
- **Git**
- **IDE** (IntelliJ IDEA recommended)

### 🏃‍♂️ One-Command Setup

```bash
# Clone and start everything
git clone https://github.com/your-org/wallet-service
cd wallet-service
docker-compose up -d
./mvnw quarkus:dev
```

🎉 **That's it!** Your development environment is ready at http://localhost:8080

## 🛠️ Development Environment

### Infrastructure Services

```bash
# Start all infrastructure
docker-compose up -d

# Check service health
docker-compose ps

# View logs
docker-compose logs -f wallet-service
```

**Services Started:**
- **MySQL Primary** (port 3306) - Write operations
- **MySQL Replica** (port 3307) - Read operations  
- **Redis** (port 6379) - Caching and sessions
- **Kafka** (port 9092) - Event streaming
- **Zookeeper** (port 2181) - Kafka coordination
- **Schema Registry** (port 8081) - Avro schemas
- **Prometheus** (port 9090) - Metrics collection
- **Grafana** (port 3000) - Metrics visualization

### Application Development

```bash
# Development mode with live reload
./mvnw quarkus:dev

# Access development UI
open http://localhost:8080/q/dev/

# Run tests
./mvnw test

# Run with different profile
./mvnw quarkus:dev -Dquarkus.profile=test
```

## 🏗️ Project Structure

```
wallet-service/
├── src/
│   ├── main/
│   │   ├── java/com/wallet/
│   │   │   ├── api/                 # REST endpoints
│   │   │   ├── application/         # CQRS commands/queries
│   │   │   │   ├── command/         # Command definitions
│   │   │   │   ├── handler/         # Command/query handlers
│   │   │   │   └── query/           # Query definitions
│   │   │   ├── core/                # Core interfaces
│   │   │   ├── domain/              # Domain models
│   │   │   │   ├── model/           # Entities
│   │   │   │   └── event/           # Domain events
│   │   │   ├── dto/                 # Data transfer objects
│   │   │   ├── infrastructure/      # External integrations
│   │   │   │   ├── persistence/     # Repositories
│   │   │   │   ├── cache/           # Redis integration
│   │   │   │   └── messaging/       # Kafka integration
│   │   │   └── service/             # Application services
│   │   └── resources/
│   │       ├── application.properties
│   │       └── import.sql
│   └── test/
│       ├── java/                    # Test classes
│       └── resources/
│           └── application.properties
├── docs/                            # Documentation
├── k8s/                            # Kubernetes manifests
├── docker-compose.yml              # Local development
└── pom.xml                         # Maven configuration
```

## 🧪 Testing Strategy

### Unit Tests
```bash
# Run all unit tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=CreateWalletCommandHandlerTest

# Run with coverage
./mvnw test jacoco:report
open target/site/jacoco/index.html
```

### Integration Tests
```bash
# Run integration tests
./mvnw verify

# Run with Testcontainers
./mvnw test -Dtest="*IT"
```

### Mutation Testing
```bash
# Run PIT mutation testing
./mvnw org.pitest:pitest-maven:mutationCoverage

# View mutation report
open target/pit-reports/index.html
```

### Load Testing
```bash
# Start application
./mvnw quarkus:dev

# Run load tests (requires K6)
k6 run scripts/load-test.js
```

## 🎯 Code Quality

### Code Style
We use **Google Java Style** with slight modifications:

```bash
# Format code
./mvnw spotless:apply

# Check formatting
./mvnw spotless:check
```

### Static Analysis
```bash
# Run Checkstyle
./mvnw checkstyle:check

# Run PMD
./mvnw pmd:check

# Run SpotBugs
./mvnw spotbugs:check
```

### Quality Gates
All PRs must pass:
- ✅ **Unit tests** (95%+ coverage)
- ✅ **Integration tests** (all critical paths)
- ✅ **Mutation tests** (100% score)
- ✅ **Static analysis** (no violations)
- ✅ **Security scan** (no high/critical issues)

## 🔧 Configuration

### Profiles

| Profile | Purpose | Database | Cache | Messaging |
|---------|---------|----------|-------|-----------|
| `dev` | Development | H2 | Local Redis | Local Kafka |
| `test` | Testing | H2 | Mock | Mock |
| `prod` | Production | MySQL | Redis Cluster | Kafka Cluster |

### Environment Variables

```bash
# Database
QUARKUS_DATASOURCE_WRITE_REACTIVE_URL=vertx-reactive:mysql://localhost:3306/wallet
QUARKUS_DATASOURCE_READ_REACTIVE_URL=vertx-reactive:mysql://localhost:3307/wallet

# Redis
QUARKUS_REDIS_HOSTS=redis://localhost:6379

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Monitoring
QUARKUS_MICROMETER_EXPORT_PROMETHEUS_ENABLED=true
```

### Configuration Files

```properties
# application.properties (development)
quarkus.hibernate-orm.database.generation=update
quarkus.hibernate-orm.log.sql=true
quarkus.log.category."com.wallet".level=DEBUG

# application-prod.properties (production)
quarkus.hibernate-orm.database.generation=validate
quarkus.hibernate-orm.log.sql=false
quarkus.log.category."com.wallet".level=INFO
```

## 🐛 Debugging

### Local Debugging
```bash
# Debug mode
./mvnw quarkus:dev -Ddebug=5005

# Attach debugger to port 5005
```

### Database Debugging
```bash
# Connect to MySQL primary
mysql -h localhost -P 3306 -u wallet -p

# Connect to MySQL replica
mysql -h localhost -P 3307 -u wallet -p

# Check replication status
SHOW REPLICA STATUS\G
```

### Redis Debugging
```bash
# Connect to Redis
redis-cli -h localhost -p 6379

# Monitor commands
MONITOR

# Check cache keys
KEYS wallet:*
```

### Kafka Debugging
```bash
# List topics
kafka-topics --bootstrap-server localhost:9092 --list

# Consume messages
kafka-console-consumer --bootstrap-server localhost:9092 --topic wallet-events --from-beginning

# Check consumer groups
kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

## 🚀 Building & Packaging

### JVM Mode
```bash
# Package application
./mvnw package

# Run packaged application
java -jar target/quarkus-app/quarkus-run.jar

# Create uber-jar
./mvnw package -Dquarkus.package.type=uber-jar
```

### Native Mode
```bash
# Build native executable (requires GraalVM)
./mvnw package -Dnative

# Build native in container
./mvnw package -Dnative -Dquarkus.native.container-build=true

# Run native executable
./target/wallet-service-*-runner
```

### Docker
```bash
# Build Docker image
docker build -t wallet-service .

# Run container
docker run -p 8080:8080 wallet-service

# Build multi-stage (JVM + Native)
docker build --target jvm -t wallet-service:jvm .
docker build --target native -t wallet-service:native .
```

## 🔄 Development Workflow

### Feature Development
1. **Create feature branch**
   ```bash
   git checkout -b feature/new-feature
   ```

2. **Develop with live reload**
   ```bash
   ./mvnw quarkus:dev
   ```

3. **Write tests first** (TDD approach)
   ```bash
   # Create test
   # Implement feature
   # Verify tests pass
   ./mvnw test
   ```

4. **Run quality checks**
   ```bash
   ./mvnw verify
   ```

5. **Create pull request**

### Database Migrations
```bash
# Create migration script
src/main/resources/db/migration/V1__Initial_schema.sql

# Run migrations
./mvnw flyway:migrate

# Check migration status
./mvnw flyway:info
```

### Adding New Dependencies
```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-new-extension</artifactId>
</dependency>
```

```bash
# Update dependencies
./mvnw dependency:resolve

# Check for updates
./mvnw versions:display-dependency-updates
```

## 🎨 IDE Setup

### IntelliJ IDEA
1. **Install plugins:**
   - Quarkus Tools
   - SonarLint
   - CheckStyle-IDEA
   - Lombok

2. **Import project:**
   - File → Open → Select `pom.xml`
   - Enable auto-import

3. **Configure code style:**
   - Import `google-java-format.xml`
   - Enable "Reformat code" on save

### VS Code
1. **Install extensions:**
   - Extension Pack for Java
   - Quarkus
   - SonarLint

2. **Configure settings:**
   ```json
   {
     "java.format.settings.url": "google-java-format.xml",
     "editor.formatOnSave": true
   }
   ```

## 🤝 Contributing

### Pull Request Process
1. **Fork the repository**
2. **Create feature branch**
3. **Make changes with tests**
4. **Run quality checks**
5. **Submit pull request**
6. **Address review feedback**
7. **Merge after approval**

### Commit Messages
Follow [Conventional Commits](https://www.conventionalcommits.org/):

```bash
feat: add historical balance queries
fix: resolve cache invalidation issue
docs: update API documentation
test: add integration tests for transfers
refactor: simplify command handler logic
```

### Code Review Checklist
- [ ] **Functionality** - Does it work as expected?
- [ ] **Tests** - Are there adequate tests?
- [ ] **Performance** - Any performance implications?
- [ ] **Security** - Any security concerns?
- [ ] **Documentation** - Is documentation updated?
- [ ] **Style** - Follows coding standards?

## 🆘 Troubleshooting

### Common Issues

#### Port Already in Use
```bash
# Find process using port
lsof -i :8080

# Kill process
kill -9 <PID>
```

#### Database Connection Issues
```bash
# Check MySQL status
docker-compose ps mysql-primary

# Restart MySQL
docker-compose restart mysql-primary

# Check logs
docker-compose logs mysql-primary
```

#### Kafka Issues
```bash
# Reset Kafka
docker-compose down
docker volume rm wallet-service_kafka-data
docker-compose up -d
```

#### Memory Issues
```bash
# Increase JVM memory
export MAVEN_OPTS="-Xmx2g"
./mvnw quarkus:dev
```

### Getting Help
- **Documentation**: Check this guide first
- **Issues**: Search [GitHub Issues](https://github.com/your-org/wallet-service/issues)
- **Discussions**: Join [GitHub Discussions](https://github.com/your-org/wallet-service/discussions)
- **Team Chat**: #wallet-service Slack channel

## 📚 Learning Resources

### Quarkus
- [Quarkus Guides](https://quarkus.io/guides/)
- [Quarkus University](https://developers.redhat.com/learn/quarkus)

### CQRS & Event Sourcing
- [AWS CQRS Patterns](https://aws.amazon.com/blogs/compute/implementing-cqrs-and-event-sourcing-patterns-with-aws-lambda-and-amazon-eventbridge/)
- [Event Sourcing Pattern](https://martinfowler.com/eaaDev/EventSourcing.html)

### Reactive Programming
- [Mutiny Guide](https://smallrye.io/smallrye-mutiny/)
- [Reactive Streams](https://www.reactive-streams.org/)

---

**Happy coding! 🚀**
