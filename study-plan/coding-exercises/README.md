# 💻 Coding Exercises

> Hands-on exercises to master financial systems engineering patterns

## 📋 Exercise Overview

| Exercise | Phase | Difficulty | Duration | Key Learning |
|----------|-------|------------|----------|--------------|
| [01 - Money Class](#01-money-class) | 1 | ⭐⭐☆☆☆ | 4-6h | Financial precision, currency handling |
| [02 - Account Aggregate](#02-account-aggregate) | 1 | ⭐⭐⭐☆☆ | 6-8h | Domain modeling, business invariants |
| [03 - Currency Exchange](#03-currency-exchange) | 1 | ⭐⭐☆☆☆ | 4-6h | Exchange rates, rounding rules |
| [04 - Optimistic Locking](#04-optimistic-locking) | 2 | ⭐⭐⭐☆☆ | 6-8h | Concurrency control, version management |
| [05 - Race Conditions](#05-race-conditions) | 2 | ⭐⭐⭐⭐☆ | 4-6h | Thread safety, atomic operations |
| [06 - Isolation Levels](#06-isolation-levels) | 2 | ⭐⭐⭐☆☆ | 4-6h | Database transactions, consistency |
| [07 - Idempotent API](#07-idempotent-api) | 3 | ⭐⭐⭐⭐☆ | 8-10h | API reliability, duplicate handling |
| [08 - Retry Framework](#08-retry-framework) | 3 | ⭐⭐⭐☆☆ | 6-8h | Exponential backoff, resilience |
| [09 - Circuit Breaker](#09-circuit-breaker) | 3 | ⭐⭐⭐⭐☆ | 6-8h | Failure detection, fallback mechanisms |
| [10 - Event Store](#10-event-store) | 4 | ⭐⭐⭐⭐☆ | 8-10h | Event sourcing, audit trails |
| [11 - Outbox Pattern](#11-outbox-pattern) | 4 | ⭐⭐⭐⭐⭐ | 10-12h | Reliable messaging, transactional events |
| [12 - Saga Pattern](#12-saga-pattern) | 4 | ⭐⭐⭐⭐⭐ | 8-10h | Distributed transactions, compensation |

## 🎯 Exercise Guidelines

### Setup Requirements
- **Java 17+** or **C# .NET 6+**
- **Maven** or **Gradle** for build management
- **Docker** for database containers
- **IDE** with debugging support (IntelliJ IDEA, VS Code)

### Testing Standards
- **Minimum 80% code coverage**
- **Unit tests** for all business logic
- **Integration tests** for database operations
- **Performance tests** for concurrency scenarios

### Code Quality
- **Clean Code** principles (meaningful names, small functions)
- **SOLID** design principles
- **Proper exception handling**
- **Comprehensive documentation**

## 📁 Exercise Structure

Each exercise follows this structure:
```
exercises/XX-exercise-name/
├── README.md              # Exercise description and requirements
├── src/
│   ├── main/java/         # Implementation code
│   └── test/java/         # Test cases
├── docs/
│   ├── requirements.md    # Detailed requirements
│   ├── hints.md          # Implementation hints
│   └── solution.md       # Reference solution (after completion)
├── docker-compose.yml     # Infrastructure setup
└── build.gradle          # Build configuration
```

## 🚀 Getting Started

### 1. Clone the Repository
```bash
git clone <repository-url>
cd financial-systems-study-plan/coding-exercises
```

### 2. Setup Development Environment
```bash
# Install Java 17+ and Maven/Gradle
# Install Docker and Docker Compose
# Setup your preferred IDE
```

### 3. Start with Exercise 01
```bash
cd 01-money-class
./gradlew test  # Should fail initially
# Implement the solution
./gradlew test  # Should pass when complete
```

## 📚 Exercise Details

### 01 - Money Class
**Phase**: Financial Fundamentals  
**Focus**: Precision arithmetic, currency handling

Implement a robust Money class following Martin Fowler's patterns:
- BigDecimal for precision (no floating-point)
- Currency support (USD, EUR, BRL)
- Arithmetic operations (add, subtract, multiply, divide)
- Comparison operations
- Immutability and thread safety

**Key Test Cases**:
```java
Money usd100 = Money.of(100, USD);
Money usd50 = Money.of(50, USD);
assertThat(usd100.add(usd50)).isEqualTo(Money.of(150, USD));

// Should throw exception for different currencies
assertThrows(IllegalArgumentException.class, () -> 
    Money.of(100, USD).add(Money.of(50, EUR))
);
```

### 04 - Optimistic Locking
**Phase**: Concurrency Control  
**Focus**: Version-based concurrency control

Implement proper optimistic locking for financial entities:
- @Version annotation usage
- OptimisticLockException handling
- Retry mechanisms for conflicts
- Performance comparison with pessimistic locking

**Key Test Cases**:
```java
@Test
void shouldHandleConcurrentUpdates() {
    Account account = createAccount(Money.of(1000, USD));
    
    // Simulate concurrent updates
    CompletableFuture<Void> update1 = updateBalanceAsync(account.getId(), 100);
    CompletableFuture<Void> update2 = updateBalanceAsync(account.getId(), 200);
    
    // Both should succeed through retry mechanism
    CompletableFuture.allOf(update1, update2).join();
    
    // Final balance should be correct
    assertThat(getBalance(account.getId())).isEqualTo(Money.of(1300, USD));
}
```

### 07 - Idempotent API
**Phase**: API Reliability  
**Focus**: Duplicate request handling

Build a payment API with proper idempotency:
- Idempotency key validation
- Duplicate request detection
- Response caching
- Concurrent idempotency key handling

**Key Test Cases**:
```java
@Test
void shouldReturnSameResponseForDuplicateRequests() {
    String idempotencyKey = UUID.randomUUID().toString();
    PaymentRequest request = new PaymentRequest(100, USD, "card_123");
    
    // First request
    PaymentResponse response1 = paymentApi.processPayment(idempotencyKey, request);
    assertThat(response1.getStatus()).isEqualTo(CREATED);
    
    // Duplicate request
    PaymentResponse response2 = paymentApi.processPayment(idempotencyKey, request);
    assertThat(response2.getStatus()).isEqualTo(OK);
    assertThat(response2.getPaymentId()).isEqualTo(response1.getPaymentId());
}
```

### 11 - Outbox Pattern
**Phase**: Event-Driven Patterns  
**Focus**: Reliable event publishing

Implement the complete transactional outbox pattern:
- Outbox table design
- Atomic event storage with business data
- Reliable event publisher
- Failure recovery mechanisms

**Key Test Cases**:
```java
@Test
void shouldPublishEventsReliablyDespiteFailures() {
    // Process business operation
    String transactionId = transferService.transfer(sourceId, destId, 100);
    
    // Verify event stored in outbox
    List<OutboxEvent> events = outboxRepository.findUnprocessedEvents();
    assertThat(events).hasSize(1);
    
    // Simulate publisher failure and recovery
    simulatePublisherFailure();
    outboxPublisher.publishPendingEvents();
    
    // Event should eventually be published
    await().until(() -> kafkaConsumer.getReceivedEvents().size() == 1);
}
```

## 🧪 Testing Guidelines

### Unit Testing
- Test business logic in isolation
- Use mocks for external dependencies
- Focus on edge cases and error conditions
- Aim for 100% coverage of critical paths

### Integration Testing
- Use TestContainers for real databases
- Test complete workflows end-to-end
- Verify data consistency across transactions
- Test failure scenarios and recovery

### Performance Testing
- Measure throughput under load
- Test concurrent access patterns
- Validate memory usage and GC behavior
- Benchmark different implementation approaches

### Example Test Structure
```java
@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
class PaymentServiceIntegrationTest {
    
    @BeforeEach
    void setup() {
        // Clean database state
        // Initialize test data
    }
    
    @Test
    @Order(1)
    void shouldProcessPaymentSuccessfully() {
        // Happy path test
    }
    
    @Test
    @Order(2)
    void shouldHandleInsufficientFunds() {
        // Error condition test
    }
    
    @Test
    @Order(3)
    void shouldHandleConcurrentPayments() {
        // Concurrency test
    }
}
```

## 🔍 Code Review Checklist

Before submitting each exercise, ensure:

### Financial Domain
- [ ] Money calculations use BigDecimal
- [ ] Currency handling follows ISO 4217
- [ ] Business invariants are enforced
- [ ] Precision and rounding rules are correct

### Concurrency
- [ ] Optimistic locking implemented correctly
- [ ] Race conditions eliminated
- [ ] Thread safety verified through testing
- [ ] Performance acceptable under load

### Reliability
- [ ] Idempotency implemented properly
- [ ] Retry logic uses exponential backoff
- [ ] Circuit breakers configured appropriately
- [ ] Error handling is comprehensive

### Event-Driven
- [ ] Events stored atomically with business data
- [ ] Event publishing is reliable
- [ ] Event ordering preserved where needed
- [ ] Failure recovery mechanisms work

### Code Quality
- [ ] Clean, readable code
- [ ] Proper exception handling
- [ ] Comprehensive logging
- [ ] Documentation complete

## 🆘 Getting Help

### When You're Stuck
1. **Read the hints** in `docs/hints.md`
2. **Review the reference materials** in each phase
3. **Check the test cases** for expected behavior
4. **Join the study group** for peer support

### Common Issues
- **Precision Errors**: Always use BigDecimal for money
- **Race Conditions**: Add proper synchronization or optimistic locking
- **Idempotency**: Store and check idempotency keys properly
- **Event Ordering**: Use proper partitioning strategies

### Code Review Process
1. **Self-review** using the checklist above
2. **Run all tests** and ensure they pass
3. **Submit for peer review** in the study group
4. **Iterate** based on feedback

## 🏆 Completion Criteria

Each exercise is complete when:
- [ ] All tests pass (including provided and custom tests)
- [ ] Code coverage meets minimum requirements (80%+)
- [ ] Performance benchmarks are met
- [ ] Code review checklist is satisfied
- [ ] Documentation is complete

## ➡️ Next Steps

After completing all exercises:
1. **Integrate learnings** in the final project (Phase 5)
2. **Build a portfolio** showcasing your implementations
3. **Contribute** to open-source financial projects
4. **Apply knowledge** in real-world financial systems

---

**Remember**: These exercises are designed to be challenging. Take your time, focus on understanding the patterns, and don't hesitate to ask for help when needed. The goal is deep learning, not speed.

**Good luck!** 🚀
