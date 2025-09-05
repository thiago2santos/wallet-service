package com.wallet.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class WalletMetrics {

    private final MeterRegistry meterRegistry;
    
    // Counters for business operations
    private final Counter walletsCreatedCounter;
    private final Counter depositsCounter;
    private final Counter withdrawalsCounter;
    private final Counter transfersCounter;
    private final Counter queriesCounter;
    private final Counter failedOperationsCounter;
    
    // Outbox and Event Publishing metrics
    private final Counter outboxEventsCreatedCounter;
    private final Counter outboxEventsPublishedCounter;
    private final Counter outboxEventsFailedCounter;
    
    // CQRS Bus metrics
    private final Counter commandsDispatchedCounter;
    private final Counter queriesDispatchedCounter;
    private final Counter busErrorsCounter;
    
    // Money amount counters
    private final Counter moneyDepositedCounter;
    private final Counter moneyWithdrawnCounter;
    private final Counter moneyTransferredCounter;
    
    // Timers for operation performance
    private final Timer walletCreationTimer;
    private final Timer depositTimer;
    private final Timer withdrawalTimer;
    private final Timer transferTimer;
    private final Timer queryTimer;
    private final Timer outboxPublishingTimer;
    private final Timer commandDispatchTimer;
    private final Timer queryDispatchTimer;
    
    // Gauges for current state
    private final AtomicLong totalWallets = new AtomicLong(0);
    private final AtomicLong totalTransactions = new AtomicLong(0);
    
    @Inject
    public WalletMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize counters
        this.walletsCreatedCounter = Counter.builder("wallet_operations_created_total")
                .description("Total number of wallets created")
                .register(meterRegistry);
                
        this.depositsCounter = Counter.builder("wallet_operations_deposits_total")
                .description("Total number of deposit operations")
                .register(meterRegistry);
                
        this.withdrawalsCounter = Counter.builder("wallet_operations_withdrawals_total")
                .description("Total number of withdrawal operations")
                .register(meterRegistry);
                
        this.transfersCounter = Counter.builder("wallet_operations_transfers_total")
                .description("Total number of transfer operations")
                .register(meterRegistry);
                
        this.queriesCounter = Counter.builder("wallet_operations_queries_total")
                .description("Total number of query operations")
                .register(meterRegistry);
                
        this.failedOperationsCounter = Counter.builder("wallet_operations_failed_total")
                .description("Total number of failed operations")
                .tag("type", "all")
                .register(meterRegistry);
        
        // Initialize outbox metrics
        this.outboxEventsCreatedCounter = Counter.builder("wallet_outbox_events_created_total")
                .description("Total number of outbox events created")
                .register(meterRegistry);
                
        this.outboxEventsPublishedCounter = Counter.builder("wallet_outbox_events_published_total")
                .description("Total number of outbox events published to Kafka")
                .register(meterRegistry);
                
        this.outboxEventsFailedCounter = Counter.builder("wallet_outbox_events_failed_total")
                .description("Total number of outbox events that failed to publish")
                .register(meterRegistry);
        
        // Initialize CQRS bus metrics
        this.commandsDispatchedCounter = Counter.builder("wallet_cqrs_commands_dispatched_total")
                .description("Total number of commands dispatched")
                .register(meterRegistry);
                
        this.queriesDispatchedCounter = Counter.builder("wallet_cqrs_queries_dispatched_total")
                .description("Total number of queries dispatched")
                .register(meterRegistry);
                
        this.busErrorsCounter = Counter.builder("wallet_cqrs_bus_errors_total")
                .description("Total number of CQRS bus errors")
                .register(meterRegistry);
        
        // Initialize money amount counters
        this.moneyDepositedCounter = Counter.builder("wallet_money_deposited_total")
                .description("Total amount of money deposited")
                .register(meterRegistry);
                
        this.moneyWithdrawnCounter = Counter.builder("wallet_money_withdrawn_total")
                .description("Total amount of money withdrawn")
                .register(meterRegistry);
                
        this.moneyTransferredCounter = Counter.builder("wallet_money_transferred_total")
                .description("Total amount of money transferred")
                .register(meterRegistry);
        
        // Initialize timers
        this.walletCreationTimer = Timer.builder("wallet_operations_creation_duration_seconds")
                .description("Time taken to create a wallet")
                .register(meterRegistry);
                
        this.depositTimer = Timer.builder("wallet_operations_deposit_duration_seconds")
                .description("Time taken to process a deposit")
                .register(meterRegistry);
                
        this.withdrawalTimer = Timer.builder("wallet_operations_withdrawal_duration_seconds")
                .description("Time taken to process a withdrawal")
                .register(meterRegistry);
                
        this.transferTimer = Timer.builder("wallet_operations_transfer_duration_seconds")
                .description("Time taken to process a transfer")
                .register(meterRegistry);
                
        this.queryTimer = Timer.builder("wallet_operations_query_duration_seconds")
                .description("Time taken to process queries")
                .register(meterRegistry);
                
        this.outboxPublishingTimer = Timer.builder("wallet_outbox_publishing_duration_seconds")
                .description("Time taken to publish outbox events")
                .register(meterRegistry);
                
        this.commandDispatchTimer = Timer.builder("wallet_cqrs_command_dispatch_duration_seconds")
                .description("Time taken to dispatch commands")
                .register(meterRegistry);
                
        this.queryDispatchTimer = Timer.builder("wallet_cqrs_query_dispatch_duration_seconds")
                .description("Time taken to dispatch queries")
                .register(meterRegistry);
        
        // Initialize gauges
        Gauge.builder("wallet.total.count", this, WalletMetrics::getTotalWallets)
                .description("Current total number of wallets")
                .register(meterRegistry);
                
        Gauge.builder("wallet.transactions.total", this, WalletMetrics::getTotalTransactions)
                .description("Current total number of transactions")
                .register(meterRegistry);
    }
    
    // Counter methods
    public void incrementWalletsCreated() {
        walletsCreatedCounter.increment();
        totalWallets.incrementAndGet();
    }
    
    public void incrementDeposits() {
        depositsCounter.increment();
        totalTransactions.incrementAndGet();
    }
    
    public void incrementWithdrawals() {
        withdrawalsCounter.increment();
        totalTransactions.incrementAndGet();
    }
    
    public void incrementTransfers() {
        transfersCounter.increment();
        totalTransactions.incrementAndGet();
    }
    
    public void incrementQueries() {
        queriesCounter.increment();
    }
    
    public void incrementFailedOperations(String operationType) {
        Counter.builder("wallet.operations.failed")
                .description("Failed operations by type")
                .tag("type", operationType)
                .register(meterRegistry)
                .increment();
    }
    
    // Timer methods
    public Timer.Sample startWalletCreationTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordWalletCreation(Timer.Sample sample) {
        sample.stop(walletCreationTimer);
    }
    
    public Timer.Sample startDepositTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordDeposit(Timer.Sample sample) {
        sample.stop(depositTimer);
    }
    
    public Timer.Sample startWithdrawalTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordWithdrawal(Timer.Sample sample) {
        sample.stop(withdrawalTimer);
    }
    
    public Timer.Sample startTransferTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordTransfer(Timer.Sample sample) {
        sample.stop(transferTimer);
    }
    
    public Timer.Sample startQueryTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordQuery(Timer.Sample sample) {
        sample.stop(queryTimer);
    }
    
    // Outbox metrics
    public void recordOutboxEventCreated() {
        outboxEventsCreatedCounter.increment();
    }
    
    public void recordOutboxEventPublished() {
        outboxEventsPublishedCounter.increment();
    }
    
    public void recordOutboxEventFailed() {
        outboxEventsFailedCounter.increment();
    }
    
    public Timer.Sample startOutboxPublishingTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordOutboxPublishing(Timer.Sample sample) {
        sample.stop(outboxPublishingTimer);
    }
    
    // CQRS Bus metrics
    public void recordCommandDispatched() {
        commandsDispatchedCounter.increment();
    }
    
    public void recordQueryDispatched() {
        queriesDispatchedCounter.increment();
    }
    
    public void recordBusError() {
        busErrorsCounter.increment();
    }
    
    public Timer.Sample startCommandDispatchTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordCommandDispatch(Timer.Sample sample) {
        sample.stop(commandDispatchTimer);
    }
    
    public Timer.Sample startQueryDispatchTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordQueryDispatch(Timer.Sample sample) {
        sample.stop(queryDispatchTimer);
    }
    
    // Amount tracking
    public void recordDepositAmount(BigDecimal amount) {
        moneyDepositedCounter.increment(amount.doubleValue());
    }
    
    public void recordWithdrawalAmount(BigDecimal amount) {
        moneyWithdrawnCounter.increment(amount.doubleValue());
    }
    
    public void recordTransferAmount(BigDecimal amount) {
        moneyTransferredCounter.increment(amount.doubleValue());
    }
    
    // Gauge getters
    public double getTotalWallets() {
        return totalWallets.get();
    }
    
    public double getTotalTransactions() {
        return totalTransactions.get();
    }
    
    // Event publishing metrics
    public void recordEventPublished(String eventType) {
        Counter.builder("wallet.events.published")
                .description("Events published to Kafka")
                .tag("event_type", eventType)
                .register(meterRegistry)
                .increment();
    }
    
    public void recordEventPublishFailure(String eventType) {
        Counter.builder("wallet.events.failed")
                .description("Failed event publications")
                .tag("event_type", eventType)
                .register(meterRegistry)
                .increment();
    }
    
    // ============================================================================
    // RESILIENCE AND CIRCUIT BREAKER METRICS
    // ============================================================================
    
    /**
     * Record database operations by type (read/write) and source (primary/replica)
     */
    public void incrementDatabaseReads(String source) {
        Counter.builder("wallet.database.reads")
                .description("Database read operations")
                .tag("source", source)
                .register(meterRegistry)
                .increment();
    }
    
    public void incrementDatabaseWrites() {
        Counter.builder("wallet.database.writes")
                .description("Database write operations")
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * Record database errors by source and type
     */
    public void recordDatabaseError(String source, Throwable throwable) {
        Counter.builder("wallet.database.errors")
                .description("Database operation errors")
                .tag("source", source)
                .tag("error_type", throwable.getClass().getSimpleName())
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * Record circuit breaker activations
     */
    public void incrementCircuitBreakerActivations(String circuitBreakerName) {
        Counter.builder("wallet.circuit_breaker.activations")
                .description("Circuit breaker activations")
                .tag("circuit_breaker", circuitBreakerName)
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * Record degraded performance events
     */
    public void recordDegradedPerformance(String degradationType) {
        Counter.builder("wallet.performance.degraded")
                .description("Degraded performance events")
                .tag("degradation_type", degradationType)
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * Record service degradation events
     */
    public void recordDegradationEvent(String degradationType, String action, String reason) {
        Counter.builder("wallet.degradation.events")
                .description("Service degradation events")
                .tag("degradation_type", degradationType)
                .tag("action", action)
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * Record degradation activations
     */
    public void incrementDegradationActivations(String degradationType) {
        Counter.builder("wallet.degradation.activations")
                .description("Service degradation activations")
                .tag("degradation_type", degradationType)
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * Record degradation duration
     */
    public void recordDegradationDuration(String degradationType, long durationMs) {
        Timer.builder("wallet.degradation.duration")
                .description("Service degradation duration")
                .tag("degradation_type", degradationType)
                .register(meterRegistry)
                .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    /**
     * Record cache bypass events
     */
    public void incrementCacheBypass() {
        Counter.builder("wallet.cache.bypass")
                .description("Cache bypass events")
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * Record fallback method executions
     */
    public void recordFallbackExecution(String fallbackMethod, String reason) {
        Counter.builder("wallet.fallback.executions")
                .description("Fallback method executions")
                .tag("fallback_method", fallbackMethod)
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    // ============================================================================
    // RETRY METRICS
    // ============================================================================

    /**
     * Record retry attempt
     */
    public void recordRetryAttempt(String operation, String retryType, String exceptionType) {
        Counter.builder("wallet.retry.attempts")
                .description("Retry attempts by operation and type")
                .tag("operation", operation)
                .tag("retry_type", retryType)
                .tag("exception_type", exceptionType)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record successful retry operation
     */
    public void recordSuccessfulRetryOperation(String operation, String retryType) {
        Counter.builder("wallet.retry.successes")
                .description("Successful operations after retry")
                .tag("operation", operation)
                .tag("retry_type", retryType)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record retry exhaustion (all retries failed)
     */
    public void recordRetryExhaustion(String operation, String retryType) {
        Counter.builder("wallet.retry.exhaustions")
                .description("Operations that exhausted all retries")
                .tag("operation", operation)
                .tag("retry_type", retryType)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record retry duration
     */
    public void recordRetryDuration(String operation, String retryType, long durationMs) {
        Timer.builder("wallet.retry.duration")
                .description("Time spent in retry operations")
                .tag("operation", operation)
                .tag("retry_type", retryType)
                .register(meterRegistry)
                .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Get retry attempts count for monitoring
     */
    public long getRetryAttempts(String retryType) {
        Counter counter = meterRegistry.find("wallet.retry.attempts")
                .tag("retry_type", retryType)
                .counter();
        return counter != null ? (long) counter.count() : 0L;
    }

    /**
     * Get retry exhaustions count for monitoring
     */
    public long getRetryExhaustions(String retryType) {
        Counter counter = meterRegistry.find("wallet.retry.exhaustions")
                .tag("retry_type", retryType)
                .counter();
        return counter != null ? (long) counter.count() : 0L;
    }

    /**
     * Record optimistic lock contention metrics
     */
    public void recordOptimisticLockContention(String operation, String walletId) {
        Counter.builder("wallet.optimistic_lock.contentions")
                .description("Optimistic lock contentions by operation")
                .tag("operation", operation)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record transient failure pattern metrics
     */
    public void recordTransientFailurePattern(String operation) {
        Counter.builder("wallet.transient_failure.patterns")
                .description("Transient failure patterns by operation")
                .tag("operation", operation)
                .register(meterRegistry)
                .increment();
    }
}
