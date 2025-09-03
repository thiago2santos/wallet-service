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
    
    // Timers for operation performance
    private final Timer walletCreationTimer;
    private final Timer depositTimer;
    private final Timer withdrawalTimer;
    private final Timer transferTimer;
    private final Timer queryTimer;
    
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
    
    // Amount tracking
    public void recordDepositAmount(BigDecimal amount) {
        meterRegistry.counter("wallet.amounts.deposited", "currency", "BRL")
                .increment(amount.doubleValue());
    }
    
    public void recordWithdrawalAmount(BigDecimal amount) {
        meterRegistry.counter("wallet.amounts.withdrawn", "currency", "BRL")
                .increment(amount.doubleValue());
    }
    
    public void recordTransferAmount(BigDecimal amount) {
        meterRegistry.counter("wallet.amounts.transferred", "currency", "BRL")
                .increment(amount.doubleValue());
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
