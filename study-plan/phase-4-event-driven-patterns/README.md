# Phase 4: Event-Driven Patterns 📡

> **Duration**: 2 weeks  
> **Goal**: Master the Transactional Outbox pattern, event sourcing, and reliable event publishing

## 📚 Required Readings

### Week 1: Event Sourcing & CQRS Fundamentals

#### Core Reading (8-10 hours)
1. **"Microservices Patterns" by Chris Richardson**
   - Chapter 6: "Developing Business Logic with Event Sourcing" (pages 177-220)
   - Chapter 7: "Implementing Queries in a Microservice Architecture" (pages 221-264)
   - Focus: Event sourcing implementation, CQRS patterns

2. **"Building Event-Driven Microservices" by Adam Bellemare**
   - Chapter 3: "Communication Structures" (pages 37-58)
   - Chapter 4: "Integrating Event-Driven Architectures with Existing Systems" (pages 59-82)
   - Focus: Event design, integration patterns

#### Supplementary Reading (4-6 hours)
3. **Greg Young's Blog Posts**:
   - ["CQRS Documents"](https://cqrs.files.wordpress.com/2010/11/cqrs_documents.pdf)
   - ["Event Sourcing"](https://martinfowler.com/eaaDev/EventSourcing.html) by Martin Fowler

4. **Apache Kafka Documentation**:
   - ["Kafka Streams"](https://kafka.apache.org/documentation/streams/)
   - ["Exactly Once Semantics"](https://kafka.apache.org/documentation/#semantics)

### Week 2: Transactional Outbox Pattern

#### Core Reading (6-8 hours)
5. **"Microservices Patterns" by Chris Richardson**
   - Chapter 4: "Managing Transactions with Sagas" (pages 129-176)
   - Focus: Transactional outbox, saga patterns

6. **"Designing Data-Intensive Applications" by Martin Kleppmann**
   - Chapter 11: "Stream Processing" (pages 441-492)
   - Focus: Event logs, stream processing patterns

#### Supplementary Reading (2-3 hours)
7. **Debezium Documentation**:
   - ["Outbox Event Router"](https://debezium.io/documentation/reference/transformations/outbox-event-router.html)
   - ["Change Data Capture"](https://debezium.io/documentation/reference/architecture.html)

## 🎥 Video Resources

### Essential Videos (6-8 hours total)

1. **"Event Sourcing and CQRS"** - Greg Young (2 hours)
   - [DDD Europe Conference](https://www.youtube.com/results?search_query=greg+young+event+sourcing+ddd+europe)

2. **"The Many Meanings of Event-Driven Architecture"** - Martin Fowler (1 hour)
   - [GOTO Conference](https://www.youtube.com/watch?v=STKCRSUsyP0)

3. **"Implementing the Outbox Pattern"** - Gunnar Morling (45 min)
   - [Devoxx Conference](https://www.youtube.com/results?search_query=gunnar+morling+outbox+pattern)

4. **"Event Sourcing in Practice"** - Udi Dahan (1.5 hours)
   - [NDC Conference](https://www.youtube.com/results?search_query=udi+dahan+event+sourcing)

### Supplementary Videos (4-6 hours)

5. **"Kafka Connect and the Outbox Pattern"** - Confluent (45 min)
   - [Confluent YouTube](https://www.youtube.com/c/Confluent)

6. **"Building Event-Driven Systems"** - Ben Stopford (1 hour)
   - [Kafka Summit](https://www.youtube.com/results?search_query=ben+stopford+kafka+summit)

## 💻 Coding Exercises

### Exercise 4.1: Event Store Implementation
**File**: `coding-exercises/10-event-store/`
**Duration**: 8-10 hours
**Difficulty**: ⭐⭐⭐⭐☆

Build a complete event store for financial domain events.

**Requirements**:
- Event storage with proper versioning
- Event replay capabilities
- Snapshot mechanism for performance
- Concurrent append handling
- Event serialization (JSON/Avro)
- Stream reading with pagination

**Event Types**:
```java
- AccountCreated
- FundsDeposited  
- FundsWithdrawn
- FundsTransferred
- AccountFrozen
- AccountClosed
```

**Test Cases**:
- Store and retrieve events by aggregate ID
- Replay events to rebuild aggregate state
- Handle concurrent event appends
- Snapshot creation and restoration
- Event versioning and migration

**Reference**: Richardson's "Microservices Patterns" Chapter 6

### Exercise 4.2: Transactional Outbox Pattern
**File**: `coding-exercises/11-outbox-pattern/`
**Duration**: 10-12 hours
**Difficulty**: ⭐⭐⭐⭐⭐

Implement the complete transactional outbox pattern.

**Requirements**:
- Outbox table design and entity
- Transactional event storage
- Outbox publisher (polling/CDC)
- Event deduplication handling
- Failure recovery mechanisms
- Message ordering guarantees

**Components**:
1. **Outbox Entity**: Store events in same transaction as business data
2. **Outbox Publisher**: Reliable event publishing to message broker
3. **Event Handler**: Process published events
4. **Failure Recovery**: Handle partial failures and retries

**Test Cases**:
- Events stored atomically with business data
- Events published exactly once
- Publisher handles broker failures gracefully
- Event ordering preserved per aggregate
- Recovery from partial failures

**Reference**: Richardson's "Microservices Patterns" Chapter 4

### Exercise 4.3: Saga Pattern Implementation
**File**: `coding-exercises/12-saga-pattern/`
**Duration**: 8-10 hours
**Difficulty**: ⭐⭐⭐⭐⭐

Implement a saga for distributed money transfer.

**Scenario**: Transfer money between accounts in different services
**Services**: Account Service, Payment Service, Notification Service

**Requirements**:
- Choreography-based saga implementation
- Compensating actions for rollback
- Saga state management
- Timeout handling
- Failure recovery

**Saga Steps**:
1. Reserve funds in source account
2. Process payment authorization
3. Complete transfer
4. Send notification
5. Release reservation

**Compensating Actions**:
- Cancel reservation
- Reverse payment
- Cancel transfer
- Send failure notification

**Test Cases**:
- Successful saga completion
- Compensation on service failure
- Timeout handling
- Partial failure recovery

**Reference**: Richardson's "Microservices Patterns" Chapter 4

## 🛠️ Practical Labs

### Lab 4.1: Event-Driven Banking System
**Duration**: 12-15 hours
**Difficulty**: ⭐⭐⭐⭐⭐

Build a complete event-driven banking system.

**Architecture**:
- **Account Service**: Manages account state via event sourcing
- **Transaction Service**: Processes payments using outbox pattern
- **Notification Service**: Sends alerts based on events
- **Audit Service**: Maintains complete audit trail

**Event Flow**:
```
Transfer Request → Account Service → Outbox → Kafka → 
Transaction Service → Outbox → Kafka → Notification Service
```

**Success Criteria**:
- All events stored reliably
- No lost or duplicate events
- Complete audit trail
- Handles service failures gracefully
- Maintains data consistency

### Lab 4.2: Event Sourcing Performance Testing
**Duration**: 6-8 hours
**Difficulty**: ⭐⭐⭐☆☆

Test event sourcing performance under load.

**Scenarios**:
- High-frequency trading simulation (1000+ events/sec)
- Large aggregate reconstruction (10,000+ events)
- Concurrent aggregate updates
- Snapshot effectiveness

**Measurements**:
- Event append throughput
- Aggregate reconstruction time
- Snapshot creation overhead
- Memory usage patterns

**Optimizations**:
- Event batching strategies
- Snapshot frequency tuning
- Caching mechanisms
- Database indexing

## 📝 Weekly Assignments

### Week 1 Assignment: Event Design Workshop
Design events for a complete financial domain:
- Identify all business events in a banking system
- Design event schemas with versioning strategy
- Plan event store partitioning and scaling
- Document event sourcing migration strategy

**Deliverable**: Event catalog with schemas and documentation
**Review Criteria**: Completeness, versioning strategy, practical considerations

### Week 2 Assignment: Outbox Pattern Architecture
Design outbox pattern implementation for a real system:
- Choose a financial use case (payments, trading, etc.)
- Design outbox table schema and publisher
- Plan failure scenarios and recovery mechanisms
- Document monitoring and alerting strategy

**Deliverable**: Technical architecture document
**Review Criteria**: Reliability guarantees, failure handling, operational concerns

## 🧪 Testing Scenarios

### Scenario 1: The Dual Write Problem
**Setup**: Service tries to update database and publish event separately
**Expected**: Either both succeed or both fail (atomicity)
**Test**: Verify outbox pattern prevents inconsistency

### Scenario 2: The Lost Event Problem
**Setup**: Database transaction commits but event publishing fails
**Expected**: Event eventually published via outbox publisher
**Test**: Verify eventual consistency and no lost events

### Scenario 3: The Duplicate Event Problem
**Setup**: Event publisher retries due to network failure
**Expected**: Event processed exactly once despite retries
**Test**: Verify idempotent event processing

### Scenario 4: The Ordering Problem
**Setup**: Multiple events for same aggregate published concurrently
**Expected**: Events processed in correct order
**Test**: Verify ordering guarantees per aggregate

## 🔍 Self-Assessment Questions

After completing Phase 4, you should be able to answer:

1. **Event Sourcing**:
   - When should you use event sourcing vs traditional CRUD?
   - How do you handle event schema evolution?
   - What are the performance implications of event replay?

2. **Outbox Pattern**:
   - Why is the outbox pattern necessary for reliable messaging?
   - How do you handle outbox publisher failures?
   - What are the trade-offs between polling and CDC approaches?

3. **Saga Patterns**:
   - When should you use choreography vs orchestration?
   - How do you design effective compensating actions?
   - How do you handle saga timeouts and failures?

## 📋 Phase 4 Checklist

- [ ] Read Richardson's "Microservices Patterns" (Event Sourcing & Sagas)
- [ ] Read Bellemare's "Building Event-Driven Microservices"
- [ ] Read Kleppmann's "Designing Data-Intensive Applications" (Streams)
- [ ] Watch Greg Young's event sourcing presentations
- [ ] Watch Martin Fowler's event-driven architecture talk
- [ ] Complete event store exercise
- [ ] Complete transactional outbox exercise
- [ ] Complete saga pattern exercise
- [ ] Complete event-driven banking system lab
- [ ] Complete performance testing lab
- [ ] Submit Week 1 assignment
- [ ] Submit Week 2 assignment
- [ ] Pass self-assessment (85%+ correct)

## ➡️ Next Phase

Once you complete Phase 4, proceed to [Phase 5: Integration & Testing](../phase-5-integration/README.md)

---

**Need Help?** 
- Check the [event sourcing patterns guide](event-sourcing-patterns.md)
- Review [common outbox antipatterns](outbox-antipatterns.md)
- Join our [study group](../reference-materials/communities.md)
