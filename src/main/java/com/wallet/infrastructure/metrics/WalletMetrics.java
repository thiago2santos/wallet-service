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
}
