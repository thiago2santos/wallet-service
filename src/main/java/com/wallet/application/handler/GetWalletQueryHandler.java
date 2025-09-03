package com.wallet.application.handler;

import com.wallet.application.query.GetWalletQuery;
import com.wallet.core.query.QueryHandler;
import com.wallet.domain.model.Wallet;
import com.wallet.infrastructure.cache.WalletStateCache;
import com.wallet.infrastructure.persistence.WalletReadRepository;
import com.wallet.infrastructure.metrics.WalletMetrics;

import jakarta.ws.rs.NotFoundException;

import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GetWalletQueryHandler implements QueryHandler<GetWalletQuery, Wallet> {

    @Inject
    @ReactiveDataSource("read")
    WalletReadRepository walletRepository;

    @Inject
    WalletStateCache walletCache;
    
    @Inject
    WalletMetrics walletMetrics;

    @Override
    public Uni<Wallet> handle(GetWalletQuery query) {
        var timer = walletMetrics.startQueryTimer();
        
        return walletCache.getWallet(query.getWalletId())
                .onItem().ifNull().switchTo(() -> 
                    walletRepository.findById(query.getWalletId())
                        .onItem().ifNotNull().call(wallet -> 
                            walletCache.cacheWallet(wallet)
                        )
                )
                .onItem().ifNull().failWith(() -> 
                    new NotFoundException("Wallet not found: " + query.getWalletId())
                )
                .onItem().invoke(wallet -> {
                    walletMetrics.incrementQueries();
                    walletMetrics.recordQuery(timer);
                })
                .onFailure().invoke(throwable -> {
                    walletMetrics.incrementFailedOperations("query");
                    walletMetrics.recordQuery(timer);
                });
    }
}