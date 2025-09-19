# Exercise 01: Money Class Implementation 💰

> **Phase**: Financial Fundamentals  
> **Duration**: 4-6 hours  
> **Difficulty**: ⭐⭐☆☆☆

## 🎯 Learning Objectives

After completing this exercise, you will be able to:
- Implement proper financial precision using BigDecimal
- Handle multiple currencies correctly
- Design immutable value objects
- Implement arithmetic operations safely
- Follow Martin Fowler's Money pattern

## 📋 Requirements

### Functional Requirements

#### 1. Money Value Object
Create a `Money` class that represents a monetary amount with currency:

```java
public class Money {
    // Must use BigDecimal for amount (never double/float)
    // Must include Currency information
    // Must be immutable
    // Must override equals() and hashCode()
}
```

#### 2. Supported Currencies
Support these currencies with proper precision:
- **USD** (United States Dollar) - 2 decimal places
- **EUR** (Euro) - 2 decimal places  
- **BRL** (Brazilian Real) - 2 decimal places
- **JPY** (Japanese Yen) - 0 decimal places
- **BTC** (Bitcoin) - 8 decimal places

#### 3. Factory Methods
Provide convenient creation methods:
```java
Money.of(100, Currency.USD)           // $100.00
Money.of("99.99", Currency.USD)       // $99.99
Money.dollars(100)                    // $100.00
Money.euros(50)                       // €50.00
Money.zero(Currency.USD)              // $0.00
```

#### 4. Arithmetic Operations
Implement safe arithmetic operations:
```java
Money usd100 = Money.dollars(100);
Money usd50 = Money.dollars(50);

Money sum = usd100.add(usd50);        // $150.00
Money diff = usd100.subtract(usd50);  // $50.00
Money product = usd100.multiply(2);   // $200.00
Money quotient = usd100.divide(2);    // $50.00
```

#### 5. Comparison Operations
Support proper comparison:
```java
Money a = Money.dollars(100);
Money b = Money.dollars(50);

boolean greater = a.isGreaterThan(b);     // true
boolean equal = a.equals(Money.dollars(100)); // true
int comparison = a.compareTo(b);          // positive number
```

#### 6. Currency Validation
- Same-currency operations should work normally
- Cross-currency operations should throw `IllegalArgumentException`
- Provide currency conversion method (bonus)

### Technical Requirements

#### 1. Immutability
- All Money instances must be immutable
- Operations return new Money instances
- No setters allowed

#### 2. Precision Handling
- Use `BigDecimal` for all calculations
- Respect currency-specific decimal places
- Handle rounding properly (HALF_UP strategy)

#### 3. Null Safety
- Constructor should reject null values
- Methods should handle null inputs gracefully

#### 4. Performance
- Implement efficient equals() and hashCode()
- Consider caching for common values (optional)

## 🧪 Test Cases

Your implementation must pass these test cases:

### Basic Creation Tests
```java
@Test
void shouldCreateMoneyWithBigDecimal() {
    Money money = Money.of(new BigDecimal("100.50"), Currency.USD);
    assertThat(money.getAmount()).isEqualTo(new BigDecimal("100.50"));
    assertThat(money.getCurrency()).isEqualTo(Currency.USD);
}

@Test
void shouldCreateMoneyWithString() {
    Money money = Money.of("99.99", Currency.USD);
    assertThat(money.getAmount()).isEqualTo(new BigDecimal("99.99"));
}

@Test
void shouldCreateMoneyWithFactoryMethods() {
    assertThat(Money.dollars(100)).isEqualTo(Money.of(100, Currency.USD));
    assertThat(Money.euros(50)).isEqualTo(Money.of(50, Currency.EUR));
    assertThat(Money.zero(Currency.USD)).isEqualTo(Money.of(0, Currency.USD));
}
```

### Arithmetic Tests
```java
@Test
void shouldAddSameCurrency() {
    Money a = Money.dollars(100);
    Money b = Money.dollars(50);
    Money result = a.add(b);
    
    assertThat(result).isEqualTo(Money.dollars(150));
    assertThat(result.getCurrency()).isEqualTo(Currency.USD);
}

@Test
void shouldSubtractSameCurrency() {
    Money a = Money.dollars(100);
    Money b = Money.dollars(30);
    Money result = a.subtract(b);
    
    assertThat(result).isEqualTo(Money.dollars(70));
}

@Test
void shouldMultiplyByNumber() {
    Money money = Money.dollars(50);
    Money result = money.multiply(3);
    
    assertThat(result).isEqualTo(Money.dollars(150));
}

@Test
void shouldDivideByNumber() {
    Money money = Money.dollars(100);
    Money result = money.divide(4);
    
    assertThat(result).isEqualTo(Money.dollars(25));
}
```

### Currency Validation Tests
```java
@Test
void shouldThrowExceptionForCrossCurrencyOperations() {
    Money usd = Money.dollars(100);
    Money eur = Money.euros(50);
    
    assertThrows(IllegalArgumentException.class, () -> usd.add(eur));
    assertThrows(IllegalArgumentException.class, () -> usd.subtract(eur));
}

@Test
void shouldHandleDifferentCurrencyPrecision() {
    Money jpy = Money.of(1000, Currency.JPY); // No decimals
    Money btc = Money.of("0.12345678", Currency.BTC); // 8 decimals
    
    assertThat(jpy.toString()).isEqualTo("¥1000");
    assertThat(btc.toString()).isEqualTo("₿0.12345678");
}
```

### Comparison Tests
```java
@Test
void shouldCompareSameCurrency() {
    Money a = Money.dollars(100);
    Money b = Money.dollars(50);
    Money c = Money.dollars(100);
    
    assertThat(a.isGreaterThan(b)).isTrue();
    assertThat(b.isLessThan(a)).isTrue();
    assertThat(a.equals(c)).isTrue();
    assertThat(a.compareTo(c)).isEqualTo(0);
}

@Test
void shouldThrowExceptionWhenComparingDifferentCurrencies() {
    Money usd = Money.dollars(100);
    Money eur = Money.euros(100);
    
    assertThrows(IllegalArgumentException.class, () -> usd.compareTo(eur));
}
```

### Immutability Tests
```java
@Test
void shouldBeImmutable() {
    Money original = Money.dollars(100);
    Money result = original.add(Money.dollars(50));
    
    // Original should be unchanged
    assertThat(original).isEqualTo(Money.dollars(100));
    assertThat(result).isEqualTo(Money.dollars(150));
    assertThat(original).isNotSameAs(result);
}
```

### Edge Cases
```java
@Test
void shouldHandleZeroAmounts() {
    Money zero = Money.zero(Currency.USD);
    Money hundred = Money.dollars(100);
    
    assertThat(zero.add(hundred)).isEqualTo(hundred);
    assertThat(hundred.subtract(hundred)).isEqualTo(zero);
}

@Test
void shouldHandleNegativeAmounts() {
    Money positive = Money.dollars(100);
    Money negative = Money.dollars(-50);
    
    assertThat(positive.add(negative)).isEqualTo(Money.dollars(50));
    assertThat(negative.isNegative()).isTrue();
}

@Test
void shouldRejectNullInputs() {
    assertThrows(IllegalArgumentException.class, () -> 
        Money.of(null, Currency.USD));
    assertThrows(IllegalArgumentException.class, () -> 
        Money.of(BigDecimal.TEN, null));
}
```

## 🏗️ Implementation Hints

### 1. Currency Enum
Create a Currency enum with precision information:
```java
public enum Currency {
    USD("$", 2),
    EUR("€", 2),
    BRL("R$", 2),
    JPY("¥", 0),
    BTC("₿", 8);
    
    private final String symbol;
    private final int decimalPlaces;
    
    // Constructor and getters
}
```

### 2. Precision Handling
Use BigDecimal with proper scale:
```java
private BigDecimal normalizeAmount(BigDecimal amount, Currency currency) {
    return amount.setScale(currency.getDecimalPlaces(), RoundingMode.HALF_UP);
}
```

### 3. Validation Helper
Create validation methods:
```java
private void validateSameCurrency(Money other) {
    if (!this.currency.equals(other.currency)) {
        throw new IllegalArgumentException(
            "Cannot operate on different currencies: " + 
            this.currency + " and " + other.currency);
    }
}
```

### 4. Factory Methods
Implement convenient factory methods:
```java
public static Money dollars(double amount) {
    return of(BigDecimal.valueOf(amount), Currency.USD);
}

public static Money of(String amount, Currency currency) {
    return of(new BigDecimal(amount), currency);
}
```

## 📊 Success Criteria

Your implementation is complete when:

- [ ] All test cases pass
- [ ] Code coverage > 90%
- [ ] No floating-point arithmetic used
- [ ] Proper currency validation
- [ ] Immutability maintained
- [ ] Clean, readable code
- [ ] Comprehensive JavaDoc

## 🔍 Code Review Checklist

Before submitting, verify:

### Financial Correctness
- [ ] BigDecimal used for all amounts
- [ ] Currency precision respected
- [ ] Rounding handled properly
- [ ] No precision loss in operations

### Object Design
- [ ] Class is immutable
- [ ] equals() and hashCode() implemented correctly
- [ ] Comparable interface implemented
- [ ] toString() provides readable format

### Error Handling
- [ ] Null inputs rejected
- [ ] Cross-currency operations prevented
- [ ] Division by zero handled
- [ ] Meaningful error messages

### Performance
- [ ] Efficient equals() implementation
- [ ] No unnecessary object creation
- [ ] Proper use of BigDecimal operations

## 🚀 Bonus Challenges

If you finish early, try these extensions:

### 1. Currency Conversion
Add support for currency conversion:
```java
Money usd = Money.dollars(100);
Money eur = usd.convertTo(Currency.EUR, exchangeRate);
```

### 2. Money Allocation
Implement allocation for splitting money:
```java
Money total = Money.dollars(100);
List<Money> parts = total.allocate(3, 2, 5); // Split by ratios
```

### 3. Formatting
Add locale-specific formatting:
```java
Money money = Money.dollars(1234.56);
String formatted = money.format(Locale.US); // "$1,234.56"
```

### 4. JSON Serialization
Add Jackson annotations for JSON support:
```java
@JsonCreator
public static Money fromJson(@JsonProperty("amount") String amount, 
                           @JsonProperty("currency") Currency currency) {
    return Money.of(amount, currency);
}
```

## 📚 References

- **Martin Fowler**: ["Money Pattern"](https://martinfowler.com/eaaCatalog/money.html)
- **ISO 4217**: [Currency Codes Standard](https://www.iso.org/iso-4217-currency-codes.html)
- **BigDecimal**: [Java Documentation](https://docs.oracle.com/javase/8/docs/api/java/math/BigDecimal.html)
- **Effective Java**: Item 17 - "Minimize mutability"

---

**Good luck!** This exercise forms the foundation for all financial operations. Take your time to get it right - proper money handling is critical for financial systems. 💪
