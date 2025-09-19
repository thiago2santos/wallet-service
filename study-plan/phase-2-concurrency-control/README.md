# Phase 2: Concurrency Control 🔒

> **Duration**: 2 weeks  
> **Goal**: Master optimistic locking, race condition prevention, and concurrent transaction handling

## 📚 Required Readings

### Week 1: Database Concurrency Fundamentals

#### Core Reading (8-10 hours)
1. **"Designing Data-Intensive Applications" by Martin Kleppmann**
   - Chapter 7: "Transactions" (pages 221-260)
   - Chapter 9: "Consistency and Consensus" (pages 321-374)
   - Focus: ACID properties, isolation levels, concurrency control

2. **"Java Concurrency in Practice" by Brian Goetz**
   - Chapter 1: "Introduction" (pages 1-18)
   - Chapter 2: "Thread Safety" (pages 19-42)
   - Chapter 15: "Atomic Variables and Nonblocking Synchronization" (pages 319-356)
   - Focus: Thread safety patterns, atomic operations

#### Supplementary Reading (4-6 hours)
3. **Hibernate Documentation**:
   - ["Optimistic Locking"](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#locking-optimistic)
   - ["Pessimistic Locking"](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#locking-pessimistic)

4. **JPA Specification**:
   - [JSR 338 - Section 3.4.4: "Optimistic Locking"](https://jcp.org/en/jsr/detail?id=338)

### Week 2: Advanced Concurrency Patterns

#### Core Reading (6-8 hours)
5. **"Database Internals" by Alex Petrov**
   - Chapter 5: "Transaction Processing and Recovery" (pages 89-118)
   - Chapter 6: "B-Tree Locking" (pages 119-142)
   - Focus: Lock-free algorithms, MVCC

6. **"Patterns of Enterprise Application Architecture" by Martin Fowler**
   - Chapter 16: "Offline Concurrency Patterns" (pages 438-457)
   - Focus: Optimistic Offline Lock, Pessimistic Offline Lock, Coarse-Grained Lock

#### Supplementary Reading (2-3 hours)
7. **PostgreSQL Documentation**:
   - ["Concurrency Control"](https://www.postgresql.org/docs/current/mvcc.html)
   - Understanding MVCC and isolation levels

## 🎥 Video Resources

### Essential Videos (6-8 hours total)

1. **"Database Transactions and ACID Properties"** - Hussein Nasser (1 hour)
   - [YouTube: Database Engineering](https://www.youtube.com/watch?v=pomxJOFVcQs)

2. **"Optimistic vs Pessimistic Locking"** - Vlad Mihalcea (45 min)
   - [YouTube: Hibernate Tips](https://www.youtube.com/c/VladMihalcea)

3. **"Concurrency Control in Distributed Systems"** - Martin Kleppmann (1.5 hours)
   - [Strange Loop Conference](https://www.youtube.com/results?search_query=martin+kleppmann+concurrency)

4. **"Building Concurrent Applications"** - Brian Goetz (1 hour)
   - [Oracle Tech Network](https://www.youtube.com/results?search_query=brian+goetz+concurrency)

### Supplementary Videos (4-6 hours)

5. **"Race Conditions and Deadlocks"** - MIT OpenCourseWare (45 min)
   - [MIT 6.034 Artificial Intelligence](https://ocw.mit.edu/courses/electrical-engineering-and-computer-science/)

6. **"MVCC Explained"** - PostgreSQL Conference (30 min)
   - [PGCon YouTube Channel](https://www.youtube.com/results?search_query=postgresql+mvcc)

## 💻 Coding Exercises

### Exercise 2.1: Optimistic Locking Implementation
**File**: `coding-exercises/04-optimistic-locking/`
**Duration**: 6-8 hours
**Difficulty**: ⭐⭐⭐☆☆

Implement proper optimistic locking for financial entities.

**Requirements**:
- Add @Version field to Account entity
- Handle OptimisticLockException properly
- Implement retry logic with exponential backoff
- Test concurrent balance updates
- Measure performance vs pessimistic locking

**Test Cases**: 
- Concurrent deposits to same account
- Transfer between accounts under load
- Version conflict resolution

**Reference**: JPA 2.2 Specification + Hibernate docs

### Exercise 2.2: Race Condition Prevention
**File**: `coding-exercises/05-race-conditions/`
**Duration**: 4-6 hours
**Difficulty**: ⭐⭐⭐⭐☆

Build a thread-safe account balance manager.

**Requirements**:
- Prevent lost updates in concurrent scenarios
- Implement atomic balance operations
- Handle insufficient funds correctly under concurrency
- Use proper synchronization mechanisms
- Benchmark different approaches

**Test Cases**:
- 1000 concurrent deposits of $1 each = $1000 final balance
- Concurrent withdrawals with insufficient funds
- Transfer deadlock prevention

**Reference**: "Java Concurrency in Practice" Chapter 15

### Exercise 2.3: Database Isolation Level Testing
**File**: `coding-exercises/06-isolation-levels/`
**Duration**: 4-6 hours
**Difficulty**: ⭐⭐⭐☆☆

Demonstrate different isolation levels and their effects.

**Requirements**:
- Test READ_UNCOMMITTED, READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE
- Show dirty reads, phantom reads, non-repeatable reads
- Measure performance impact of each level
- Implement proper isolation for financial operations
- Document when to use each level

**Test Cases**:
- Dirty read scenarios with rollbacks
- Phantom reads during balance calculations
- Serializable conflicts during transfers

**Reference**: "Designing Data-Intensive Applications" Chapter 7

## 🛠️ Practical Labs

### Lab 2.1: Concurrent Bank Transfer System
**Duration**: 8-10 hours
**Difficulty**: ⭐⭐⭐⭐☆

Build a complete concurrent transfer system.

**Scenario**: Handle 1000 concurrent transfers between 100 accounts
**Requirements**:
- No lost updates
- No negative balances
- Deadlock prevention
- Performance monitoring
- Proper error handling

**Success Criteria**:
- All transfers complete successfully or fail gracefully
- Final balances are mathematically correct
- No deadlocks occur
- System handles 100+ TPS

### Lab 2.2: Optimistic Lock Retry Framework
**Duration**: 6-8 hours
**Difficulty**: ⭐⭐⭐☆☆

Create a reusable retry framework for optimistic lock conflicts.

**Requirements**:
- Configurable retry attempts and delays
- Exponential backoff with jitter
- Different strategies for different operations
- Metrics collection
- Integration with Spring/Quarkus

**Success Criteria**:
- Framework handles 95%+ of optimistic lock conflicts
- Configurable per operation type
- Comprehensive metrics and logging
- Easy integration with existing code

## 📝 Weekly Assignments

### Week 1 Assignment: Concurrency Analysis
Analyze a real financial system for concurrency issues:
- Choose a payment processor (Stripe, PayPal, etc.)
- Research their concurrency handling approaches
- Identify potential race conditions
- Propose solutions using learned patterns

**Deliverable**: Technical analysis report (3-5 pages)
**Review Criteria**: Depth of analysis, practical solutions, understanding of trade-offs

### Week 2 Assignment: Performance Benchmarking
Compare different concurrency approaches:
- Optimistic vs pessimistic locking
- Different isolation levels
- Various retry strategies
- Lock-free vs lock-based approaches

**Deliverable**: Benchmark results with analysis
**Review Criteria**: Methodology, statistical significance, practical insights

## 🧪 Testing Scenarios

### Scenario 1: The Double Spending Problem
**Setup**: Two concurrent requests to withdraw the last $100 from an account
**Expected**: Only one succeeds, the other fails with insufficient funds
**Test**: Verify no negative balance occurs

### Scenario 2: The Lost Update Problem
**Setup**: Two concurrent deposits of $50 each to an account with $100
**Expected**: Final balance is $200
**Test**: Verify both updates are applied

### Scenario 3: The Transfer Deadlock
**Setup**: Account A transfers to B while B transfers to A simultaneously
**Expected**: One completes, the other retries or fails gracefully
**Test**: Verify no deadlock occurs

### Scenario 4: High Concurrency Load
**Setup**: 1000 concurrent operations on 100 accounts
**Expected**: All operations complete within reasonable time
**Test**: Measure throughput and error rates

## 🔍 Self-Assessment Questions

After completing Phase 2, you should be able to answer:

1. **Optimistic Locking**:
   - When should you use optimistic vs pessimistic locking?
   - How do you handle OptimisticLockException in financial operations?
   - What are the performance implications of different locking strategies?

2. **Race Conditions**:
   - How do you prevent lost updates in concurrent scenarios?
   - What causes the ABA problem and how do you prevent it?
   - How do you test for race conditions effectively?

3. **Database Isolation**:
   - Which isolation level is appropriate for financial transactions?
   - What are the trade-offs between consistency and performance?
   - How do you handle deadlocks in transfer operations?

## 📋 Phase 2 Checklist

- [ ] Read Kleppmann's "Designing Data-Intensive Applications" (Transactions)
- [ ] Read Goetz's "Java Concurrency in Practice" (Thread Safety)
- [ ] Watch concurrency control videos
- [ ] Complete optimistic locking exercise
- [ ] Complete race condition prevention exercise
- [ ] Complete isolation level testing exercise
- [ ] Complete concurrent transfer system lab
- [ ] Complete retry framework lab
- [ ] Submit Week 1 assignment
- [ ] Submit Week 2 assignment
- [ ] Pass self-assessment (85%+ correct)

## ➡️ Next Phase

Once you complete Phase 2, proceed to [Phase 3: API Reliability](../phase-3-api-reliability/README.md)

---

**Need Help?** 
- Check the [concurrency debugging guide](debugging-concurrency.md)
- Review [common concurrency antipatterns](concurrency-antipatterns.md)
- Join our [study group](../reference-materials/communities.md)
