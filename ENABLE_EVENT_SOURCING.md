# 🚀 Enable Full Event Sourcing for Wallet Service

## Current State
Your wallet service has **all the event sourcing infrastructure ready** but it's currently disabled. Here's how to enable it:

## 🔧 Step 1: Enable Event Emission in CreateWalletCommandHandler

**File**: `src/main/java/com/wallet/application/handler/CreateWalletCommandHandler.java`

**Current (Direct DB Write)**:
```java
@ApplicationScoped
public class CreateWalletCommandHandler implements CommandHandler<CreateWalletCommand, String> {
    @Inject
    WalletRepository writeRepository;

    // Temporarily disabled for create wallet focus
    // @Inject
    // @Channel("wallet-events")
    // MutinyEmitter<WalletCreatedEvent> eventEmitter;

    @Override
    @Transactional
    public Uni<String> handle(CreateWalletCommand command) {
        // ... create wallet object ...
        return writeRepository.persist(wallet)
            // .chain(() -> eventEmitter.send(event)) // Disabled for now
            .map(v -> wallet.getId());
    }
}
```

**Change to (Event-Driven)**:
```java
@ApplicationScoped
public class CreateWalletCommandHandler implements CommandHandler<CreateWalletCommand, String> {
    @Inject
    WalletEventStore eventStore; // Use event store instead of direct DB

    @Override
    public Uni<String> handle(CreateWalletCommand command) {
        String walletId = UUID.randomUUID().toString();
        
        WalletCreatedEvent event = new WalletCreatedEvent(
            walletId,
            command.getUserId(),
            command.getCurrency()
        );

        // Emit event instead of direct DB write
        return eventStore.store(event)
            .map(v -> walletId);
    }
}
```

## 🔧 Step 2: Enable Event Processing

**File**: `src/main/java/com/wallet/infrastructure/event/WalletEventHandler.java`

**Current (Disabled)**:
```java
// @Incoming("wallet-events") // Disabled for now - focusing on command side
public Uni<Void> handleEvent(WalletEvent event) {
```

**Change to (Enabled)**:
```java
@Incoming("wallet-events") // Enable event processing
public Uni<Void> handleEvent(WalletEvent event) {
```

## 🔧 Step 3: Enable Kafka Consumers in Configuration

**File**: `src/main/resources/application-k8s.properties`

**Add these lines**:
```properties
# Enable event consumer for wallet events
mp.messaging.incoming.wallet-events-consumer.connector=smallrye-kafka
mp.messaging.incoming.wallet-events-consumer.topic=wallet-events
mp.messaging.incoming.wallet-events-consumer.value.deserializer=io.quarkus.kafka.client.serialization.ObjectMapperDeserializer
mp.messaging.incoming.wallet-events-consumer.key.deserializer=org.apache.kafka.common.serialization.StringDeserializer
mp.messaging.incoming.wallet-events-consumer.auto.offset.reset=earliest
mp.messaging.incoming.wallet-events-consumer.enable.auto.commit=false
mp.messaging.incoming.wallet-events-consumer.isolation.level=read_committed
mp.messaging.incoming.wallet-events-consumer.group.id=wallet-service-group
```

## 🔧 Step 4: Update Event Handler Channel Name

**File**: `src/main/java/com/wallet/infrastructure/event/WalletEventHandler.java`

**Change**:
```java
@Incoming("wallet-events-consumer") // Use the consumer channel name
public Uni<Void> handleEvent(WalletEvent event) {
```

## 🚀 Complete Event-Driven Flow

After these changes, the flow will be:

```
1. API Request → CreateWalletCommandHandler
2. Create WalletCreatedEvent
3. Emit Event → Kafka (wallet-events topic)
4. WalletEventHandler consumes event
5. Create Wallet in Database (via event)
6. Update Cache
```

## 🎯 Benefits of Event Sourcing

1. **Audit Trail**: Complete history of all wallet operations
2. **Replay Capability**: Rebuild state from events
3. **Scalability**: Separate read/write models
4. **Integration**: Other services can consume wallet events
5. **Consistency**: Event-driven eventual consistency

## 🔍 Monitoring Events

Once enabled, you can monitor events in:
- **Kafka UI**: `http://192.168.49.2:30080` - See events in `wallet-events` topic
- **Application Logs**: Event processing logs
- **Metrics**: Kafka producer/consumer metrics in Prometheus

## 🧪 Testing Event Sourcing

```bash
# 1. Create a wallet (will emit WalletCreatedEvent)
curl -X POST http://192.168.49.2:30808/api/v1/wallets \
  -H "Content-Type: application/json" \
  -d '{"userId": "event-user", "currency": "USD"}'

# 2. Check Kafka UI for the event
# Visit: http://192.168.49.2:30080

# 3. Verify wallet was created via event processing
curl http://192.168.49.2:30808/api/v1/wallets/{walletId}
```

## ⚠️ Important Notes

1. **Dual Write Problem**: Current implementation does both event + direct DB write for some operations
2. **Consistency**: Consider implementing saga pattern for complex transactions
3. **Event Versioning**: Plan for event schema evolution
4. **Error Handling**: Implement proper error handling and retry mechanisms
5. **Idempotency**: Ensure event handlers are idempotent

## 🎉 Ready to Enable?

The infrastructure is **100% ready**. You just need to uncomment/enable the event processing components!
