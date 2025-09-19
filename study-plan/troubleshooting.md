# 🔧 Troubleshooting Guide

> Common issues and solutions for the Financial Systems Engineering Study Plan

## 🚨 Setup Issues

### Docker Problems

#### Docker containers won't start
```bash
# Check if Docker is running
docker --version
docker info

# Check for port conflicts
lsof -i :5432  # PostgreSQL
lsof -i :6379  # Redis
lsof -i :9092  # Kafka

# Solution: Kill conflicting processes or use different ports
sudo kill -9 <PID>
```

#### Database connection refused
```bash
# Check container status
docker ps -a

# Check logs
docker logs postgres-dev
docker logs redis-dev

# Restart containers
docker restart postgres-dev redis-dev
```

#### Out of disk space
```bash
# Clean up Docker
docker system prune -a
docker volume prune

# Remove unused images
docker image prune -a
```

### Java/Maven Issues

#### Wrong Java version
```bash
# Check current version
java --version

# Install Java 17+ (macOS)
brew install openjdk@17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# Install Java 17+ (Ubuntu)
sudo apt install openjdk-17-jdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```

#### Maven build failures
```bash
# Clean and rebuild
./mvnw clean compile

# Skip tests if needed
./mvnw clean compile -DskipTests

# Update dependencies
./mvnw dependency:resolve
```

#### IDE not recognizing code
```bash
# Refresh project
./mvnw clean compile

# IntelliJ IDEA
# File → Reload Maven Projects
# File → Invalidate Caches and Restart

# VS Code
# Ctrl+Shift+P → Java: Reload Projects
```

## 🧪 Testing Issues

### Tests Failing

#### Money class precision errors
```java
// ❌ Wrong: Using double
Money money = new Money(19.99, Currency.USD);

// ✅ Correct: Using BigDecimal
Money money = Money.of(new BigDecimal("19.99"), Currency.USD);

// ❌ Wrong: Comparing with ==
if (money1.getAmount() == money2.getAmount()) { ... }

// ✅ Correct: Using compareTo
if (money1.getAmount().compareTo(money2.getAmount()) == 0) { ... }
```

#### Optimistic locking not working
```java
// ❌ Missing: No @Version field
@Entity
public class Account {
    @Id private String id;
    private BigDecimal balance;
}

// ✅ Correct: With @Version field
@Entity
public class Account {
    @Id private String id;
    @Version private Long version;  // Essential!
    private BigDecimal balance;
}
```

#### Idempotency tests failing
```java
// ❌ Wrong: Not storing idempotency key
@PostMapping("/payments")
public PaymentResponse createPayment(@RequestBody PaymentRequest request) {
    return paymentService.createPayment(request);
}

// ✅ Correct: Handling idempotency key
@PostMapping("/payments")
public PaymentResponse createPayment(
    @RequestHeader("Idempotency-Key") String key,
    @RequestBody PaymentRequest request) {
    
    // Check for existing payment
    Optional<Payment> existing = paymentService.findByIdempotencyKey(key);
    if (existing.isPresent()) {
        return toResponse(existing.get());
    }
    
    return paymentService.createPayment(request, key);
}
```

### Performance Issues

#### Tests running slowly
```java
// Use TestContainers with reuse
@Testcontainers
class IntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withReuse(true);  // Reuse container across tests
}

// Increase timeouts
@Test
@Timeout(value = 30, unit = TimeUnit.SECONDS)
void longRunningTest() { ... }

// Use @DirtiesContext sparingly
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
```

#### Out of memory during tests
```bash
# Increase Maven memory
export MAVEN_OPTS="-Xmx2g -XX:MaxPermSize=256m"

# Or in pom.xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>-Xmx2g</argLine>
    </configuration>
</plugin>
```

## 💻 Code Issues

### Concurrency Problems

#### Race conditions in tests
```java
// ❌ Wrong: No synchronization
@Test
void testConcurrentDeposits() {
    CompletableFuture<Void> future1 = depositAsync(accountId, 100);
    CompletableFuture<Void> future2 = depositAsync(accountId, 200);
    
    // This might fail due to race conditions
    CompletableFuture.allOf(future1, future2).join();
}

// ✅ Correct: Proper synchronization
@Test
void testConcurrentDeposits() {
    CountDownLatch latch = new CountDownLatch(2);
    AtomicReference<Exception> exception = new AtomicReference<>();
    
    CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
        try {
            depositService.deposit(accountId, Money.dollars(100));
        } catch (Exception e) {
            exception.set(e);
        } finally {
            latch.countDown();
        }
    });
    
    // Similar for future2...
    latch.await(10, TimeUnit.SECONDS);
    assertThat(exception.get()).isNull();
}
```

#### Deadlocks in transfer operations
```java
// ❌ Wrong: Potential deadlock
public void transfer(String fromId, String toId, Money amount) {
    Account from = repository.findById(fromId);
    Account to = repository.findById(toId);
    
    synchronized(from) {
        synchronized(to) {  // Potential deadlock!
            from.withdraw(amount);
            to.deposit(amount);
        }
    }
}

// ✅ Correct: Ordered locking
public void transfer(String fromId, String toId, Money amount) {
    String firstId = fromId.compareTo(toId) < 0 ? fromId : toId;
    String secondId = fromId.compareTo(toId) < 0 ? toId : fromId;
    
    Account first = repository.findById(firstId);
    Account second = repository.findById(secondId);
    
    synchronized(first) {
        synchronized(second) {
            if (fromId.equals(firstId)) {
                first.withdraw(amount);
                second.deposit(amount);
            } else {
                second.withdraw(amount);
                first.deposit(amount);
            }
        }
    }
}
```

### Event Sourcing Issues

#### Events not being published
```java
// ❌ Wrong: Events published outside transaction
@Transactional
public void deposit(String accountId, Money amount) {
    Account account = repository.findById(accountId);
    account.deposit(amount);
    repository.save(account);
}
// Event published here - outside transaction!
eventPublisher.publish(new FundsDepositedEvent(...));

// ✅ Correct: Using outbox pattern
@Transactional
public void deposit(String accountId, Money amount) {
    Account account = repository.findById(accountId);
    account.deposit(amount);
    repository.save(account);
    
    // Store event in same transaction
    OutboxEvent event = new OutboxEvent(
        "FundsDeposited",
        accountId,
        new FundsDepositedEvent(accountId, amount)
    );
    outboxRepository.save(event);
}
```

#### Event ordering issues
```java
// ❌ Wrong: No ordering guarantee
@EventHandler
public void handle(FundsDepositedEvent event) {
    // Events might arrive out of order
    updateBalance(event.getAccountId(), event.getAmount());
}

// ✅ Correct: Sequence number or timestamp
@EventHandler
public void handle(FundsDepositedEvent event) {
    // Check sequence number
    if (event.getSequenceNumber() <= getLastProcessedSequence(event.getAccountId())) {
        return; // Already processed
    }
    
    updateBalance(event.getAccountId(), event.getAmount());
    updateLastProcessedSequence(event.getAccountId(), event.getSequenceNumber());
}
```

## 🌐 Docsify Issues

### Site not loading

#### Serve command not working
```bash
# Try different servers
npx serve .
# or
python -m http.server 3000
# or
php -S localhost:3000
```

#### Plugins not loading
```html
<!-- Check plugin order in index.html -->
<script src="//cdn.jsdelivr.net/npm/docsify@4"></script>
<!-- Load plugins AFTER docsify core -->
<script src="//cdn.jsdelivr.net/npm/docsify/lib/plugins/search.min.js"></script>
```

#### Sidebar not showing
```markdown
<!-- Check _sidebar.md exists and has correct format -->
* [Home](/)
* [Phase 1](phase-1-financial-fundamentals/)
```

### Styling issues

#### Custom CSS not applying
```html
<!-- Make sure CSS is in <style> tag in index.html -->
<style>
  .phase-card {
    border: 1px solid #dee2e6;
    /* ... */
  }
</style>
```

#### Interactive features not working
```javascript
// Check JavaScript console for errors
// Make sure event listeners are properly attached
document.addEventListener('DOMContentLoaded', function() {
    // Initialize interactive features
});
```

## 📚 Learning Issues

### Feeling Overwhelmed

#### Too much information
**Solution**: Focus on one phase at a time
- Complete Phase 1 before moving to Phase 2
- Don't try to read everything at once
- Take breaks between study sessions

#### Concepts not clicking
**Solution**: Multiple learning approaches
- Read the material first
- Watch videos for different perspective
- Code the exercises hands-on
- Discuss with community members

#### Falling behind schedule
**Solution**: Adjust expectations
- Quality over speed
- It's okay to take longer
- Focus on understanding, not completion
- Ask for help when stuck

### Code Not Working

#### Following tutorials exactly but getting errors
**Solution**: Version differences
- Check Java/framework versions
- Update dependencies to latest stable
- Look for updated examples in community

#### Can't debug complex issues
**Solution**: Systematic debugging
1. Read error messages carefully
2. Check logs for more details
3. Use debugger to step through code
4. Isolate the problem with minimal examples
5. Ask specific questions in community

## 🆘 Getting Help

### Before Asking for Help

1. **Search existing discussions** in Discord/forums
2. **Check the FAQ** for common issues
3. **Try the troubleshooting steps** above
4. **Create a minimal reproducible example**

### How to Ask Good Questions

```markdown
**Problem**: Brief description of what you're trying to achieve
**Environment**: 
- OS: macOS/Windows/Linux
- Java version: 17
- Framework: Spring Boot 2.7.0

**Code**: 
```java
// Minimal code that reproduces the issue
```

**Error**: 
```
Full error message and stack trace
```

**What I tried**:
- Tried solution A - didn't work because...
- Tried solution B - got different error...

**Expected**: What should happen
**Actual**: What actually happens
```

### Community Support

- **[Discord](reference-materials/communities)** - Real-time help
- **[GitHub Issues](https://github.com/your-repo/issues)** - Bug reports
- **[Study Groups](reference-materials/communities)** - Peer support
- **[Office Hours](reference-materials/communities)** - Expert help

## 🔄 Still Stuck?

If none of these solutions work:

1. **Join our Discord** and ask in the appropriate channel
2. **Create a GitHub issue** with full details
3. **Schedule office hours** for one-on-one help
4. **Find a study buddy** for peer support

Remember: Everyone gets stuck sometimes. The key is to ask for help when you need it and keep learning! 🚀

---

**Updated**: December 2024 | **Need more help?** Join our [community](reference-materials/communities)
