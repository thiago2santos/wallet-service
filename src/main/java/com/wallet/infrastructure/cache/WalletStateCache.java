package com.wallet.infrastructure.cache;

import io.quarkus.redis.client.reactive.ReactiveRedisClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Collections;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.domain.model.Wallet;

@ApplicationScoped
public class WalletStateCache {

    private static final String WALLET_KEY_PREFIX = "wallet:";
    private static final Duration CACHE_DURATION = Duration.ofMinutes(30);

    @Inject
    ReactiveRedisClient redis;

    @Inject
    ObjectMapper objectMapper;

    public Uni<Wallet> getWallet(String walletId) {
        return redis.get(WALLET_KEY_PREFIX + walletId)
                .map(response -> {
                    if (response == null) {
                        return null;
                    }
                    try {
                        return objectMapper.readValue(response.toString(), Wallet.class);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to deserialize wallet from cache", e);
                    }
                });
    }

    public Uni<Void> cacheWallet(Wallet wallet) {
        try {
            String walletJson = objectMapper.writeValueAsString(wallet);
            return redis.setex(
                    WALLET_KEY_PREFIX + wallet.getId(),
                    String.valueOf(CACHE_DURATION.toSeconds()),
                    walletJson
            ).replaceWithVoid();
        } catch (Exception e) {
            return Uni.createFrom().failure(new RuntimeException("Failed to serialize wallet for cache", e));
        }
    }

    public Uni<Void> invalidateWallet(String walletId) {
        return redis.del(Collections.singletonList(WALLET_KEY_PREFIX + walletId))
                .replaceWithVoid();
    }
}