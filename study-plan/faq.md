# ❓ Frequently Asked Questions

> Common questions and answers about the Financial Systems Engineering Study Plan

## 🎯 General Questions

### Q: Who is this study plan for?
**A:** This study plan is designed for:
- **Software engineers** wanting to specialize in financial systems
- **Backend developers** looking to understand financial domain patterns
- **Architects** designing payment and banking systems
- **Anyone** who received feedback about missing financial system fundamentals

### Q: What programming languages are supported?
**A:** The study plan primarily uses **Java** with Spring Boot/Quarkus examples, but the patterns apply to:
- **C#** with .NET Core
- **Python** with Django/FastAPI
- **Node.js** with Express
- **Go** with Gin/Echo

The concepts are language-agnostic; implementation details may vary.

### Q: How much time should I dedicate daily?
**A:** Recommended time commitment:
- **Minimum**: 1 hour/day (12-16 week timeline)
- **Recommended**: 2-3 hours/day (9 week timeline)
- **Intensive**: 4-6 hours/day (4-5 week timeline)

Quality over quantity - consistent daily practice beats marathon sessions.

### Q: Do I need prior financial domain knowledge?
**A:** No prior financial knowledge required! The study plan starts with fundamentals:
- Money handling basics
- Currency concepts
- Financial transaction patterns
- Regulatory considerations

## 📚 Learning Path Questions

### Q: Can I skip phases or do them out of order?
**A:** **Not recommended.** Each phase builds on previous knowledge:
- **Phase 1** provides financial domain foundation
- **Phase 2** adds concurrency safety
- **Phase 3** builds on Phase 2 for API reliability
- **Phase 4** integrates all previous concepts

However, if you're already expert in certain areas, you can accelerate through familiar content.

### Q: What if I get stuck on an exercise?
**A:** Multiple support options available:
1. **Check hints** in the exercise `docs/hints.md` file
2. **Review reference materials** for the relevant phase
3. **Ask in Discord/Slack** community channels
4. **Join office hours** for real-time help
5. **Find a study buddy** for peer support

### Q: How do I know if I'm ready to move to the next phase?
**A:** Phase completion criteria:
- [ ] Complete all reading assignments
- [ ] Finish all coding exercises with passing tests
- [ ] Submit weekly assignments
- [ ] Pass phase assessment (80%+ score)
- [ ] Feel confident explaining key concepts

### Q: Can I use different technologies than suggested?
**A:** Absolutely! The patterns are universal. Common substitutions:
- **Database**: PostgreSQL → MySQL, SQL Server
- **Message Broker**: Kafka → RabbitMQ, AWS SQS
- **Cache**: Redis → Memcached, Hazelcast
- **Framework**: Spring Boot → Quarkus, Micronaut

Just ensure you understand the core patterns.

## 💻 Technical Questions

### Q: My tests are failing. What should I check?
**A:** Common issues and solutions:

#### Money Class Issues
```java
// ❌ Wrong: Using double
Money money = new Money(19.99, Currency.USD);

// ✅ Correct: Using BigDecimal
Money money = Money.of(new BigDecimal("19.99"), Currency.USD);
```

#### Optimistic Locking Issues
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

#### Idempotency Issues
```java
// ❌ Wrong: Not checking for existing operations
public PaymentResponse processPayment(PaymentRequest request) {
    return paymentService.createPayment(request);
}

// ✅ Correct: Checking idempotency key
public PaymentResponse processPayment(String idempotencyKey, PaymentRequest request) {
    Optional<Payment> existing = paymentService.findByIdempotencyKey(idempotencyKey);
    if (existing.isPresent()) {
        return toResponse(existing.get());
    }
    return paymentService.createPayment(request, idempotencyKey);
}
```

### Q: How do I set up the development environment?
**A:** Step-by-step setup:

#### Java Environment
```bash
# Install Java 17+
brew install openjdk@17  # macOS
sudo apt install openjdk-17-jdk  # Ubuntu

# Install Maven
brew install maven  # macOS
sudo apt install maven  # Ubuntu

# Install Docker
# Download from https://docker.com/get-started
```

#### Database Setup
```bash
# Start PostgreSQL with Docker
docker run --name postgres-dev \
  -e POSTGRES_PASSWORD=password \
  -e POSTGRES_DB=wallet_db \
  -p 5432:5432 -d postgres:13

# Start Redis
docker run --name redis-dev \
  -p 6379:6379 -d redis:7-alpine
```

### Q: What if I prefer a different database?
**A:** Database-specific considerations:

#### PostgreSQL (Recommended)
- Excellent ACID compliance
- Advanced features (JSONB, arrays)
- Great performance for financial workloads

#### MySQL
- Widely used in financial systems
- Good performance and reliability
- Simpler than PostgreSQL

#### SQL Server
- Enterprise features
- Excellent tooling
- Good for .NET environments

All exercises work with any ACID-compliant database.

## 🎓 Assessment Questions

### Q: What happens if I fail a phase assessment?
**A:** No problem! Learning is iterative:
1. **Review feedback** to identify knowledge gaps
2. **Revisit materials** for weak areas
3. **Practice more exercises** in problem areas
4. **Retake assessment** when ready
5. **Get help** from community if needed

There's no penalty for retaking assessments.

### Q: How are exercises graded?
**A:** Exercise evaluation criteria:

#### Automated Checks (60%)
- [ ] All tests pass
- [ ] Code coverage > 80%
- [ ] No critical bugs detected
- [ ] Performance benchmarks met

#### Code Review (40%)
- [ ] Clean, readable code
- [ ] Proper error handling
- [ ] Good documentation
- [ ] Follows best practices

### Q: Can I get a certificate upon completion?
**A:** Currently working on certification options:
- **Digital badge** for LinkedIn profile
- **Certificate of completion** with skill verification
- **Portfolio showcase** for job interviews
- **Community recognition** as study plan graduate

## 🤝 Community Questions

### Q: How active is the community?
**A:** Growing community with:
- **Daily activity** in Discord channels
- **Weekly office hours** with mentors
- **Monthly workshops** on advanced topics
- **Code review sessions** for exercises
- **Job referral network** for graduates

### Q: Can I contribute to the study plan?
**A:** Absolutely! Contributions welcome:
- **Fix typos** or improve explanations
- **Add exercises** for additional practice
- **Share resources** (books, videos, articles)
- **Mentor newcomers** in the community
- **Translate content** to other languages

### Q: Is there a job placement program?
**A:** While not formal placement, we offer:
- **Resume review** for financial systems roles
- **Interview preparation** with mock technical interviews
- **Referral network** of alumni in financial companies
- **Portfolio guidance** for showcasing skills
- **Industry connections** through community events

## 🔧 Troubleshooting

### Q: Docker containers won't start
**A:** Common solutions:
```bash
# Check Docker is running
docker --version

# Check port conflicts
lsof -i :5432  # PostgreSQL
lsof -i :6379  # Redis

# Clean up old containers
docker system prune -a

# Restart Docker Desktop
```

### Q: Tests are slow or timing out
**A:** Performance optimization:
```bash
# Use TestContainers with reuse
@Testcontainers
class IntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withReuse(true);  // Reuse container
}

# Increase test timeouts
@Test
@Timeout(value = 30, unit = TimeUnit.SECONDS)
void longRunningTest() { ... }
```

### Q: IDE not recognizing generated code
**A:** Common fixes:
```bash
# Refresh Maven/Gradle
./mvnw clean compile  # Maven
./gradlew clean build  # Gradle

# Reimport project in IDE
# IntelliJ: File → Reload Gradle/Maven Project
# VS Code: Reload window
```

## 📈 Career Questions

### Q: What jobs can I get after completing this?
**A:** Career opportunities include:
- **Backend Engineer** at fintech companies
- **Payment Systems Developer** at payment processors
- **Banking Software Engineer** at traditional banks
- **Financial Platform Architect** at financial services
- **Consultant** for financial system implementations

### Q: What salary range can I expect?
**A:** Varies by location and experience:
- **Junior Financial Systems Engineer**: $80k-120k
- **Mid-level**: $120k-180k
- **Senior**: $180k-250k+
- **Principal/Staff**: $250k-400k+

Financial systems engineers typically earn 10-20% more than general backend engineers.

### Q: Which companies hire financial systems engineers?
**A:** Target companies:
- **Fintech**: Stripe, Square, PayPal, Plaid, Robinhood
- **Traditional Banks**: JPMorgan Chase, Bank of America, Wells Fargo
- **Payment Processors**: Visa, Mastercard, American Express
- **Crypto**: Coinbase, Kraken, Binance
- **E-commerce**: Amazon, Shopify, eBay
- **Consulting**: Accenture, Deloitte, McKinsey

## 🚀 Getting Started

### Q: I'm ready to start. What's the first step?
**A:** Welcome! Here's your launch sequence:

1. **[Complete setup](getting-started)** - Environment and tools
2. **[Join community](reference-materials/communities)** - Discord/Slack
3. **[Start Phase 1](phase-1-financial-fundamentals/)** - Financial fundamentals
4. **[Track progress](progress-tracking/)** - Monitor your journey

### Q: Still have questions?
**A:** Multiple ways to get help:
- **[Join Discord](reference-materials/communities)** for real-time chat
- **[GitHub Issues](https://github.com/your-username/financial-systems-study-plan/issues)** for study plan feedback
- **[Office Hours](reference-materials/communities)** for live Q&A sessions
- **Email**: financial-systems-study@example.com

---

**Don't see your question?** Ask in the community - chances are others have the same question! 🤝
