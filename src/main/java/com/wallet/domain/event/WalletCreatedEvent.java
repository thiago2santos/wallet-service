package com.wallet.domain.event;

import java.time.Instant;
import java.util.UUID;

public class WalletCreatedEvent extends WalletEvent {

    public WalletCreatedEvent(String walletId, String userId) {
        setEventId(UUID.randomUUID().toString());
        setWalletId(walletId);
        setUserId(userId);
        setEventType(WalletEventType.WALLET_CREATED);
        setTimestamp(Instant.now());
        setVersion(1L);
    }
}
