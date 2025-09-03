package com.wallet.domain.event;

import java.math.BigDecimal;
import java.time.Instant;

public class WalletEvent {
    private String eventId;
    private WalletEventType eventType;
    private String walletId;
    private String userId;
    private BigDecimal amount;

    private String destinationWalletId;
    private String transactionId;
    private Instant timestamp;
    private long version;

    // Getters and Setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public WalletEventType getEventType() {
        return eventType;
    }

    public void setEventType(WalletEventType eventType) {
        this.eventType = eventType;
    }

    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}