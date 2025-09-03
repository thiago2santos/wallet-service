package com.wallet.application.dto;

import com.wallet.domain.model.WalletStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletDTO {
    private final String id;
    private final String userId;

    private final BigDecimal balance;
    private final WalletStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public WalletDTO(String id, String userId, BigDecimal balance,
                     WalletStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.balance = balance;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }



    public BigDecimal getBalance() {
        return balance;
    }

    public WalletStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
