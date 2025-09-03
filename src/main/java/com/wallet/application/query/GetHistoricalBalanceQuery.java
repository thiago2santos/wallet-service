package com.wallet.application.query;

import java.time.LocalDateTime;
import java.util.UUID;

import com.wallet.core.query.Query;
import com.wallet.dto.HistoricalBalanceResponse;

public class GetHistoricalBalanceQuery implements Query<HistoricalBalanceResponse> {
    private final String queryId;
    private final String walletId;
    private final LocalDateTime timestamp;

    public GetHistoricalBalanceQuery(String walletId, LocalDateTime timestamp) {
        this.queryId = UUID.randomUUID().toString();
        this.walletId = walletId;
        this.timestamp = timestamp;
    }

    @Override
    public String getQueryId() {
        return queryId;
    }

    public String getWalletId() {
        return walletId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "GetHistoricalBalanceQuery{" +
                "queryId='" + queryId + '\'' +
                ", walletId='" + walletId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
