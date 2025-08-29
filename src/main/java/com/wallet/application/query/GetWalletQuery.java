package com.wallet.application.query;

import com.wallet.core.query.Query;
import com.wallet.domain.model.Wallet;
import java.util.UUID;

public class GetWalletQuery implements Query<Wallet> {
    private final String queryId;
    private final String walletId;

    public GetWalletQuery(String walletId) {
        this.queryId = UUID.randomUUID().toString();
        this.walletId = walletId;
    }

    @Override
    public String getQueryId() {
        return queryId;
    }

    public String getWalletId() {
        return walletId;
    }
}