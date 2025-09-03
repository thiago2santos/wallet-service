# 🏗️ Design Decisions

> Architectural choices and rationale for the Wallet Service implementation

## 🎯 Core Design Philosophy

The Wallet Service was designed as a **mission-critical financial service** with emphasis on:
- **Performance** - Sub-100ms response times required
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

**Implementation**:
```java
@ApplicationScoped
public class CommandBus {
    public <T> Uni<Void> send(Command<T> command) { ... }
}

@ApplicationScoped  
public class QueryBus {
    public <T> Uni<T> send(Query<T> query) { ... }
}
```

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

**Implementation**:
```java
// Write operations use primary
@WriteDataSource
public class WalletRepository { ... }

// Read operations use replica  
@ReadDataSource
public class WalletQueryRepository { ... }
```

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

**Example**:
```java
public Uni<WalletResponse> createWallet(CreateWalletCommand command) {
    return commandBus.send(command)
        .chain(() -> queryBus.send(new GetWalletQuery(command.getWalletId())));
}
```

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

**Configuration**:
```properties
quarkus.datasource.reactive.max-size=20
quarkus.redis.max-pool-size=20
```

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

**Implementation**:
```java
public class DepositRequest {
    @NotNull @DecimalMin("0.01")
    private BigDecimal amount;
    
    @NotBlank @Size(max = 100)
    private String referenceId;
}
```

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

---

## 📝 Summary

These design decisions prioritize **performance**, **reliability**, and **maintainability** while keeping the implementation focused on the core assessment requirements. The architecture is designed to be production-ready while remaining simple enough to implement within the time constraints.

**Key Principles Applied**:
- ✅ **KISS** (Keep It Simple, Stupid) - Avoid over-engineering
- ✅ **YAGNI** (You Aren't Gonna Need It) - Implement what's needed now
- ✅ **DRY** (Don't Repeat Yourself) - Reusable components and patterns
- ✅ **SOLID** - Object-oriented design principles throughout
