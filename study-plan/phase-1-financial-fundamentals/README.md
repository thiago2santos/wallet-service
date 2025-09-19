# Phase 1: Financial Fundamentals 🏦

> **Duration**: 2 weeks  
> **Goal**: Master financial domain patterns and money handling

## 📚 Required Readings

### Week 1: Money Patterns & Domain Modeling

#### Core Reading (8-10 hours)
1. **"Analysis Patterns" by Martin Fowler**
   - Chapter 3: "Money" (pages 73-95)
   - Chapter 4: "Temporal Patterns" (pages 96-120)
   - Focus: Money class design, currency handling, temporal data

2. **"Domain-Driven Design" by Eric Evans**
   - Chapter 5: "A Model Expressed in Software" (pages 81-125)
   - Chapter 6: "The Life Cycle of a Domain Object" (pages 125-156)
   - Focus: Aggregate design for financial entities

#### Supplementary Reading (4-6 hours)
3. **Martin Fowler's Blog Posts**:
   - ["Money Pattern"](https://martinfowler.com/eaaCatalog/money.html)
   - ["Unit of Work"](https://martinfowler.com/eaaCatalog/unitOfWork.html)
   - ["Identity Map"](https://martinfowler.com/eaaCatalog/identityMap.html)

4. **ISO 4217 Currency Codes Standard** (1 hour)
   - [Official ISO documentation](https://www.iso.org/iso-4217-currency-codes.html)
   - Understanding currency precision and rounding rules

### Week 2: Financial System Architecture

#### Core Reading (6-8 hours)
5. **"Building Event-Driven Microservices" by Adam Bellemare**
   - Chapter 2: "Event-Driven Microservice Fundamentals" (pages 15-35)
   - Chapter 8: "Handling Failures" (pages 145-170)
   - Focus: Financial event modeling and failure handling

6. **Federal Reserve: "Payment Systems in the U.S."** (3-4 hours)
   - [Download PDF](https://www.federalreserve.gov/paymentsystems/files/core_principles_2012.pdf)
   - Sections 1-3: Payment system fundamentals
   - Focus: Settlement, clearing, risk management

#### Supplementary Reading (2-3 hours)
7. **PCI DSS Quick Reference Guide** (2 hours)
   - [Official PCI DSS documentation](https://www.pcisecuritystandards.org/document_library/)
   - Focus: Data protection requirements for financial systems

## 🎥 Video Resources

### Essential Videos (6-8 hours total)

1. **"Money and Currency Design Patterns"** - Martin Fowler (45 min)
   - [InfoQ Presentation](https://www.infoq.com/presentations/Money-Currency-Design-Patterns/)

2. **"Building Financial Services on AWS"** - AWS re:Invent (1 hour)
   - [AWS YouTube Channel](https://www.youtube.com/results?search_query=aws+reinvent+financial+services)

3. **"Event Sourcing in Financial Systems"** - Greg Young (1.5 hours)
   - [DDD Europe Conference](https://www.youtube.com/results?search_query=greg+young+event+sourcing+financial)

4. **"Microservices at Capital One"** - Engineering at Scale (45 min)
   - [Capital One Tech Blog](https://www.capitalone.com/tech/)

### Supplementary Videos (4-6 hours)

5. **"Payment Processing 101"** - Stripe Engineering (30 min)
   - [Stripe YouTube Channel](https://www.youtube.com/c/StripeDev)

6. **"Database Transactions Explained"** - Hussein Nasser (45 min)
   - [YouTube: Database Engineering](https://www.youtube.com/c/HusseinNasser-software-engineering)

## 💻 Coding Exercises

### Exercise 1.1: Money Class Implementation
**File**: `coding-exercises/01-money-class/`
**Duration**: 4-6 hours
**Difficulty**: ⭐⭐☆☆☆

Implement a robust Money class following Martin Fowler's patterns.

**Requirements**:
- Support multiple currencies (USD, EUR, BRL)
- Proper precision handling (no floating-point arithmetic)
- Currency conversion with exchange rates
- Arithmetic operations (add, subtract, multiply, divide)
- Comparison operations
- Immutability

**Test Cases**: Provided in exercise folder
**Reference**: Fowler's "Analysis Patterns" Chapter 3

### Exercise 1.2: Account Aggregate Design
**File**: `coding-exercises/02-account-aggregate/`
**Duration**: 6-8 hours
**Difficulty**: ⭐⭐⭐☆☆

Design a bank account aggregate following DDD principles.

**Requirements**:
- Account entity with proper invariants
- Transaction value objects
- Balance calculation rules
- Overdraft protection
- Account status management (ACTIVE, FROZEN, CLOSED)

**Test Cases**: Business rule validation scenarios
**Reference**: Evans' "Domain-Driven Design" Chapter 6

### Exercise 1.3: Currency Exchange Service
**File**: `coding-exercises/03-currency-exchange/`
**Duration**: 4-6 hours
**Difficulty**: ⭐⭐☆☆☆

Build a currency exchange service with proper rate handling.

**Requirements**:
- Exchange rate management
- Rate expiration and refresh
- Cross-currency calculations
- Rounding rules per currency
- Rate history tracking

**Test Cases**: Exchange scenarios with different currencies
**Reference**: ISO 4217 standard + Fowler's Money patterns

## 📝 Weekly Assignments

### Week 1 Assignment: Financial Domain Model
Create a complete domain model for a simple banking system:
- Customer, Account, Transaction entities
- Money value object implementation
- Basic business rules and invariants
- Unit tests for all business logic

**Deliverable**: UML diagram + Java/C# implementation
**Review Criteria**: Proper encapsulation, immutability, business rule enforcement

### Week 2 Assignment: Payment Processing Flow
Design and document a payment processing flow:
- Authorization, capture, settlement phases
- Error handling and rollback scenarios
- Audit trail requirements
- Compliance considerations

**Deliverable**: Sequence diagrams + technical specification
**Review Criteria**: Completeness, error handling, regulatory awareness

## 🔍 Self-Assessment Questions

After completing Phase 1, you should be able to answer:

1. **Money Handling**:
   - Why should you never use `float` or `double` for money?
   - How do you handle currency conversion with proper rounding?
   - What are the key properties of a well-designed Money class?

2. **Domain Modeling**:
   - What makes a good aggregate boundary in financial systems?
   - How do you enforce business invariants across multiple entities?
   - When should you use value objects vs. entities?

3. **Financial Systems**:
   - What are the key phases of payment processing?
   - How do settlement and clearing differ?
   - What are the main regulatory requirements for financial data?

## 📋 Phase 1 Checklist

- [ ] Read Fowler's "Analysis Patterns" (Money chapter)
- [ ] Read Evans' "DDD" (Model chapters)
- [ ] Watch Martin Fowler's money patterns video
- [ ] Complete Money class exercise
- [ ] Complete Account aggregate exercise
- [ ] Complete Currency exchange exercise
- [ ] Submit Week 1 assignment
- [ ] Submit Week 2 assignment
- [ ] Pass self-assessment (80%+ correct)

## ➡️ Next Phase

Once you complete Phase 1, proceed to [Phase 2: Concurrency Control](../phase-2-concurrency-control/README.md)

---

**Need Help?** 
- Check the [reference materials](../reference-materials/) 
- Join our [study group](../reference-materials/communities.md)
- Review [common mistakes](common-mistakes.md)
