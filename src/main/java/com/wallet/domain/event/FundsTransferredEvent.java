package com.wallet.domain.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event published when funds are transferred between wallets
 */
public class FundsTransferredEvent extends WalletEvent {
    
    private String sourceWalletId;
    private String destinationWalletId;
    private String transactionId;
    private BigDecimal amount;
    private String referenceId;
    private String description;
    private Instant timestamp;

    // Default constructor for serialization
    public FundsTransferredEvent() {
    }

    public FundsTransferredEvent(String sourceWalletId, String destinationWalletId, 
                                String transactionId, BigDecimal amount, 
                                String referenceId, String description) {
        this.sourceWalletId = sourceWalletId;
        this.destinationWalletId = destinationWalletId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.referenceId = referenceId;
        this.description = description;
        this.timestamp = Instant.now();
    }

    public String getAggregateId() {
        return sourceWalletId; // Use source wallet as primary aggregate
    }

    public WalletEventType getEventType() {
        return WalletEventType.FUNDS_TRANSFERRED;
    }

    // Getters and setters
    public String getSourceWalletId() {
        return sourceWalletId;
    }

    public void setSourceWalletId(String sourceWalletId) {
        this.sourceWalletId = sourceWalletId;
    }

    public String getDestinationWalletId() {
        return destinationWalletId;
    }

    public void setDestinationWalletId(String destinationWalletId) {
        this.destinationWalletId = destinationWalletId;
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
        return "FundsTransferredEvent{" +
                "sourceWalletId='" + sourceWalletId + '\'' +
                ", destinationWalletId='" + destinationWalletId + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", amount=" + amount +
                ", referenceId='" + referenceId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
