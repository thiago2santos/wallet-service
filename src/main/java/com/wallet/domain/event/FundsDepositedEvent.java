package com.wallet.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class FundsDepositedEvent extends WalletEvent {

    public FundsDepositedEvent(String walletId, BigDecimal amount, String transactionId, String referenceId) {
        setEventId(UUID.randomUUID().toString());
        setWalletId(walletId);
        setAmount(amount);
        setTransactionId(transactionId);
        setEventType(WalletEventType.FUNDS_DEPOSITED);
        setTimestamp(Instant.now());
        setVersion(1L);
    }
}
