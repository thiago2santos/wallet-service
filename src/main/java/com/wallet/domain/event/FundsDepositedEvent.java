package com.wallet.domain.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event published when funds are deposited to a wallet
 */
public class FundsDepositedEvent extends WalletEvent {
    
    private String walletId;
    private String transactionId;
    private BigDecimal amount;
    private String referenceId;
    private String description;
    private Instant timestamp;

    // Default constructor for serialization
    public FundsDepositedEvent() {
    }

    public FundsDepositedEvent(String walletId, String transactionId, BigDecimal amount, 
                              String referenceId, String description) {
        this.walletId = walletId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.referenceId = referenceId;
        this.description = description;
        this.timestamp = Instant.now();
    }

    public String getAggregateId() {
        return walletId;
    }

    public WalletEventType getEventType() {
        return WalletEventType.FUNDS_DEPOSITED;
    }

    // Getters and setters
    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "FundsDepositedEvent{" +
                "walletId='" + walletId + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", amount=" + amount +
                ", referenceId='" + referenceId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}