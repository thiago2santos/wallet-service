package com.wallet.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class HistoricalBalanceResponse {
    private String walletId;
    private BigDecimal balance;

    private LocalDateTime timestamp;

    public HistoricalBalanceResponse() {}

    public HistoricalBalanceResponse(String walletId, BigDecimal balance, LocalDateTime timestamp) {
        this.walletId = walletId;
        this.balance = balance;
        this.timestamp = timestamp;
    }

    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }



    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "HistoricalBalanceResponse{" +
                "walletId='" + walletId + '\'' +
                ", balance=" + balance +

                ", timestamp=" + timestamp +
                '}';
    }
}
