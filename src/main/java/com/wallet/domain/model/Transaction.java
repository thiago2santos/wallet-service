package com.wallet.domain.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction", indexes = {
    @Index(name = "idx_wallet_id", columnList = "walletId"),
    @Index(name = "idx_reference_id", columnList = "referenceId", unique = true)
})
public class Transaction extends PanacheEntityBase {
    @Id
    @Column(nullable = false)
    private String id;

    @Column(nullable = false)
    private String walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private String referenceId;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column
    private String destinationWalletId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Transaction() {
    }

    public Transaction(String id, String walletId, TransactionType type, BigDecimal amount,
                      String referenceId, TransactionStatus status) {
        this.id = id;
        this.walletId = walletId;
        this.type = type;
        this.amount = amount;
        this.referenceId = referenceId;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getWalletId() {
        return walletId;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getDestinationWalletId() {
        return destinationWalletId;
    }

    public void setDestinationWalletId(String destinationWalletId) {
        this.destinationWalletId = destinationWalletId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}