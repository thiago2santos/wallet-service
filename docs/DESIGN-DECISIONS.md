# 🏗️ Design Decisions

> Architectural choices and rationale for the Wallet Service implementation

## 🎯 Core Design Philosophy

The Wallet Service was designed as a **mission-critical financial service** with emphasis on:
- **Performance** - High-performance operations with low latency
- **Traceability** - Full audit trail for financial operations
- **Scalability** - Handle growing user base and transaction volume
- **Reliability** - Minimize downtime and ensure data consistency

## 🏛️ Architecture Decisions

### 1. CQRS (Command Query Responsibility Segregation)

**Decision**: Implement separate command and query models with dedicated buses.

**Rationale**:
- **Performance**: Optimize read and write operations independently
- **Scalability**: Scale read and write workloads separately
- **Flexibility**: Different data models for different use cases
- **Maintainability**: Clear separation of concerns

**Key Benefits**:
- Clear separation of concerns between commands and queries
- Independent scaling of read and write operations
- Optimized data models for different access patterns

### 2. Event Sourcing with Kafka

**Decision**: Use Apache Kafka for event streaming and audit trail.

**Rationale**:
- **Audit Requirements**: Financial services need complete transaction history
- **Historical Balance**: Replay events to calculate balance at any point in time
- **Integration**: Enable future microservices to consume wallet events
- **Durability**: Kafka provides persistent, replicated event storage

**Implementation**:
- Events published after successful database transactions
- Avro schemas for type safety and evolution
- Separate topics for different event types

### 3. Database Replication (Primary-Replica)

**Decision**: MySQL with primary-replica setup for read/write separation.

**Rationale**:
- **Performance**: Distribute read load across multiple replicas
- **Availability**: Replica can serve reads if primary is temporarily unavailable
- **Consistency**: Strong consistency for writes, eventual consistency for reads
- **Cost-Effective**: More economical than distributed databases for this scale

**Key Benefits**:
- Write operations isolated to primary database
- Read operations distributed across replicas
- Improved performance and availability

### 4. Redis Caching Strategy

**Decision**: Redis for caching wallet state and balance information.

**Rationale**:
- **Performance**: Sub-5ms response times for cached reads
- **Reduced Database Load**: Cache frequent balance queries
- **Session Management**: Future support for user sessions
- **Flexibility**: Support for complex data structures

**Cache Strategy**:
- Cache wallet state after successful operations
- TTL-based expiration (5 minutes)
- Cache invalidation after balance changes
- Write-through pattern for consistency

### 5. Quarkus Framework

**Decision**: Quarkus over Spring Boot for the microservice framework.

**Rationale**:
- **Performance**: Faster startup times (< 1 second vs 10+ seconds)
- **Memory Efficiency**: Lower memory footprint (< 100MB)
- **Native Compilation**: GraalVM support for containerized environments
- **Reactive**: Built-in reactive programming support
- **Developer Experience**: Live reload and dev UI

### 6. Reactive Programming Model

**Decision**: Use Mutiny (reactive streams) throughout the application.

**Rationale**:
- **Concurrency**: Handle high concurrent loads with fewer threads
- **Non-blocking**: Avoid thread blocking on I/O operations
- **Backpressure**: Handle load spikes gracefully
- **Integration**: Natural fit with Quarkus and reactive databases

**Key Benefits**:
- Non-blocking operations improve throughput
- Better resource utilization under load
- Natural composition of asynchronous operations

## 💾 Data Design Decisions

### 1. Single Currency (BRL)

**Decision**: Support only Brazilian Real (BRL) initially.

**Rationale**:
- **Simplicity**: Avoid currency conversion complexity
- **Time Constraints**: Focus on core functionality first
- **Future Extension**: Architecture supports multiple currencies later

### 2. BigDecimal for Money

**Decision**: Use `BigDecimal` for all monetary amounts.

**Rationale**:
- **Precision**: Avoid floating-point rounding errors
- **Financial Standard**: Industry standard for financial calculations
- **Compliance**: Required for accurate financial reporting

### 3. Optimistic Locking

**Decision**: Use database-level optimistic locking for wallet updates.

**Rationale**:
- **Performance**: Better than pessimistic locking for high concurrency
- **Deadlock Prevention**: Reduces database deadlock scenarios
- **Scalability**: Allows multiple concurrent reads

## 🔧 Technology Stack Rationale

### Java 21
- **Latest LTS Version**: Long-term support and stability with latest features
- **Performance**: Enhanced garbage collection and JIT optimizations
- **Modern Features**: Virtual threads, pattern matching, records, text blocks, and more

### MySQL 8.0
- **ACID Compliance**: Required for financial transactions
- **Replication**: Built-in primary-replica support
- **Performance**: Excellent performance for transactional workloads
- **Ecosystem**: Mature tooling and monitoring

### Maven
- **Simplicity**: Straightforward dependency management
- **Integration**: Excellent Quarkus integration
- **Tooling**: Rich plugin ecosystem for testing and analysis

## 🚀 Performance Design Decisions

### 1. Connection Pooling

**Decision**: Aggressive connection pooling configuration.

**Key Benefits**:
- Optimized connection usage for high concurrency
- Reduced resource overhead
- Better performance under load

### 2. Async Processing

**Decision**: Asynchronous processing for all I/O operations.

**Benefits**:
- Non-blocking database operations
- Concurrent request handling
- Better resource utilization

### 3. Caching Strategy

**Decision**: Multi-level caching approach.

**Levels**:
1. **Application Cache**: In-memory for frequently accessed data
2. **Redis Cache**: Distributed cache for wallet state
3. **Database**: Optimized queries with proper indexing

## 🔒 Security Design Decisions

### 1. Input Validation

**Decision**: Comprehensive validation using Jakarta Bean Validation.

**Key Benefits**:
- Comprehensive validation at API boundary
- Prevents invalid data from entering the system
- Clear error messages for client applications

### 2. Production Security Strategy

**Decision**: Delegate security to AWS API Gateway + WAF.

**Rationale**:
- **Expertise**: Leverage AWS security expertise
- **Features**: Rate limiting, DDoS protection, authentication
- **Maintenance**: Reduced security maintenance burden
- **Compliance**: AWS compliance certifications

## 📊 Monitoring Design Decisions

### 1. Custom Prometheus Metrics

**Decision**: Implement business-specific metrics beyond standard JVM metrics.

**Metrics**:
- `wallet_operations_total` - Operation counters
- `wallet_operations_duration` - Response time histograms
- `wallet_balance_changes` - Balance change tracking

### 2. Health Check Strategy

**Decision**: Comprehensive health checks for all dependencies.

**Components Monitored**:
- Database connectivity (primary and replica)
- Redis connectivity
- Kafka connectivity
- Application readiness

## 🧪 Testing Strategy Decisions

### 1. Testing Pyramid

**Decision**: Comprehensive testing at multiple levels.

**Levels**:
- **Unit Tests**: Core business logic (command handlers, validators)
- **Integration Tests**: Full stack testing (HTTP → Database → Kafka)
- **Mutation Testing**: Code quality validation with PIT

### 2. Test Data Strategy

**Decision**: H2 in-memory database for unit tests, Docker for integration tests.

**Benefits**:
- Fast unit test execution
- Realistic integration test environment
- Isolated test data

## 🔄 Future-Proofing Decisions

### 1. Microservice Architecture

**Decision**: Design as a standalone microservice from the start.

**Benefits**:
- **Scalability**: Independent scaling
- **Technology Evolution**: Can evolve independently
- **Team Autonomy**: Separate development and deployment

### 2. API Versioning Ready

**Decision**: URL-based versioning structure (`/api/v1/`).

**Future Support**:
- Backward compatibility
- Gradual migration strategies
- Multiple API versions

### 3. Event-Driven Architecture

**Decision**: Event-first design for all state changes.

**Benefits**:
- **Integration**: Easy to add new consumers
- **Analytics**: Events can feed analytics systems
- **Audit**: Complete audit trail from day one

## ☁️ AWS Service Mapping

> How our design decisions translate into concrete AWS service choices

### 🏗️ **Architectural Patterns → AWS Services**

| **Design Decision** | **AWS Service** | **Rationale** |
|-------------------|-----------------|---------------|
| **CQRS Architecture** | ECS Fargate + ALB | Separate scaling for read/write workloads |
| **Database Read/Write Separation** | Aurora MySQL (1 Writer + 2 Readers) | Native support for read replicas with automatic failover |
| **Redis Caching** | ElastiCache Redis Cluster | Managed Redis with Multi-AZ and automatic failover |
| **Event Sourcing** | MSK (Managed Kafka) | Fully managed Kafka with enterprise features |
| **Reactive Programming** | ECS Fargate | Auto-scaling containers handle non-blocking workload efficiently |

### 🛡️ **Resilience Patterns → AWS Services**

| **Resilience Pattern** | **AWS Implementation** | **Benefits** |
|----------------------|----------------------|-------------|
| **Circuit Breakers** | Application-level + ALB Health Checks | Prevent cascade failures across services |
| **Auto-scaling** | ECS Auto Scaling + Target Tracking | Scale based on CPU/memory/custom metrics |
| **Multi-AZ Deployment** | Aurora Multi-AZ + ElastiCache Multi-AZ | 99.99% availability with automatic failover |
| **Load Balancing** | Application Load Balancer | Distribute traffic across healthy instances |
| **Health Monitoring** | CloudWatch + ALB Health Checks | Comprehensive monitoring and alerting |

### 📊 **Performance Decisions → AWS Services**

| **Performance Requirement** | **AWS Solution** | **Expected Outcome** |
|---------------------------|------------------|-------------------|
| **Sub-20ms Response Times** | Aurora Serverless v2 + ElastiCache | Database auto-scaling + sub-ms cache |
| **High Concurrency** | ECS Fargate (4 vCPU, 8GB RAM) | Handle 5000+ TPS with reactive programming |
| **Connection Pooling** | Aurora Proxy + Application Pools | Optimize database connections |
| **Caching Strategy** | ElastiCache Redis (r6g.large) | 99.9% cache hit ratio for balance queries |

### 🔐 **Security Decisions → AWS Services**

| **Security Requirement** | **AWS Implementation** | **Protection Level** |
|------------------------|----------------------|-------------------|
| **API Security** | API Gateway + WAF | Rate limiting, DDoS protection, authentication |
| **Network Security** | VPC + Security Groups + NACLs | Network-level isolation and access control |
| **Data Encryption** | Aurora Encryption + EBS Encryption | Encryption at rest and in transit |
| **Secrets Management** | AWS Secrets Manager | Secure credential rotation and access |
| **Compliance** | CloudTrail + Config | Audit logging and compliance monitoring |

### 💰 **Cost Optimization Decisions → AWS Services**

| **Cost Strategy** | **AWS Implementation** | **Cost Benefit** |
|------------------|----------------------|-----------------|
| **Right-sizing** | ECS Auto Scaling (6-20 tasks) | Pay only for needed capacity |
| **Serverless Database** | Aurora Serverless v2 | Scale to zero during low usage |
| **Spot Instances** | ECS Spot Instances (non-prod) | 70% cost reduction for development |
| **Reserved Capacity** | RDS Reserved Instances | 40% savings for predictable workloads |
| **Storage Tiering** | S3 Intelligent Tiering | Automatic cost optimization for backups |

### 🌍 **Regional Strategy → AWS Services**

| **Regional Requirement** | **AWS Implementation** | **Business Value** |
|------------------------|----------------------|------------------|
| **Brazil Focus** | Primary: sa-east-1 (São Paulo) | Low latency for Brazilian users |
| **Disaster Recovery** | Backup: us-east-1 (N. Virginia) | Cost-effective DR and backup storage |
| **Data Residency** | All data in sa-east-1 | Compliance with Brazilian regulations |
| **Global CDN** | CloudFront (optional) | Global edge locations for static content |

### 📈 **Monitoring Decisions → AWS Services**

| **Monitoring Need** | **AWS Solution** | **Capability** |
|-------------------|------------------|---------------|
| **Application Metrics** | CloudWatch Custom Metrics | Business and technical metrics |
| **Infrastructure Monitoring** | CloudWatch + AWS X-Ray | Full observability stack |
| **Log Management** | CloudWatch Logs | Centralized logging with retention |
| **Alerting** | CloudWatch Alarms + SNS | Proactive incident response |
| **Dashboards** | Managed Grafana + CloudWatch | Visual monitoring and reporting |

---

## 📝 Summary

These design decisions prioritize **performance**, **reliability**, and **maintainability** while keeping the implementation focused on the core assessment requirements. The architecture is designed to be production-ready while remaining simple enough to implement within the time constraints.

**Key Principles Applied**:
- ✅ **KISS** (Keep It Simple, Stupid) - Avoid over-engineering
- ✅ **YAGNI** (You Aren't Gonna Need It) - Implement what's needed now
- ✅ **DRY** (Don't Repeat Yourself) - Reusable components and patterns
- ✅ **SOLID** - Object-oriented design principles throughout
