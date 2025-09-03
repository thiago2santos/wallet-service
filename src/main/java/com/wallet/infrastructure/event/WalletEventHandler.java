package com.wallet.infrastructure.event;

import java.math.BigDecimal;

import com.wallet.domain.event.WalletEvent;
import com.wallet.domain.model.Wallet;
import com.wallet.infrastructure.cache.WalletStateCache;
import com.wallet.infrastructure.persistence.WalletReadRepository;

import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class WalletEventHandler {

    @Inject
    @ReactiveDataSource("write")
    WalletReadRepository walletRepository;

    @Inject
    WalletStateCache walletCache;

    // @Incoming("wallet-events") // Disabled for now - focusing on command side
    public Uni<Void> handleEvent(WalletEvent event) {
        switch (event.getEventType()) {
            case WALLET_CREATED:
                return handleWalletCreated(event);
            case FUNDS_DEPOSITED:
                return handleFundsDeposited(event);
            case FUNDS_WITHDRAWN:
                return handleFundsWithdrawn(event);
            case FUNDS_TRANSFERRED:
                return handleFundsTransferred(event);
            default:
                return Uni.createFrom().voidItem();
        }
    }

    private Uni<Void> handleWalletCreated(WalletEvent event) {
        Wallet wallet = new Wallet();
        wallet.setId(event.getWalletId());
        wallet.setUserId(event.getUserId());
        wallet.setBalance(BigDecimal.ZERO);

        
        return walletRepository.persist(wallet)
                .call(() -> walletCache.cacheWallet(wallet))
                .replaceWithVoid();
    }

    private Uni<Void> handleFundsDeposited(WalletEvent event) {
        return walletRepository.findById(event.getWalletId())
                .onItem().ifNotNull().transformToUni(wallet -> {
                    wallet.setBalance(wallet.getBalance().add(event.getAmount()));
                    return walletRepository.persist(wallet)
                            .call(() -> walletCache.cacheWallet(wallet));
                })
                .replaceWithVoid();
    }

    private Uni<Void> handleFundsWithdrawn(WalletEvent event) {
        return walletRepository.findById(event.getWalletId())
                .onItem().ifNotNull().transformToUni(wallet -> {
                    wallet.setBalance(wallet.getBalance().subtract(event.getAmount()));
                    return walletRepository.persist(wallet)
                            .call(() -> walletCache.cacheWallet(wallet));
                })
                .replaceWithVoid();
    }

    private Uni<Void> handleFundsTransferred(WalletEvent event) {
        return walletRepository.findById(event.getWalletId())
                .onItem().ifNotNull().transformToUni(sourceWallet -> {
                    sourceWallet.setBalance(sourceWallet.getBalance().subtract(event.getAmount()));
                    return walletRepository.persist(sourceWallet)
                            .call(() -> walletCache.cacheWallet(sourceWallet))
                            .chain(() -> walletRepository.findById(event.getDestinationWalletId()))
                            .onItem().ifNotNull().transformToUni(destWallet -> {
                                destWallet.setBalance(destWallet.getBalance().add(event.getAmount()));
                                return walletRepository.persist(destWallet)
                                        .call(() -> walletCache.cacheWallet(destWallet));
                            });
                })
                .replaceWithVoid();
    }
}