package com.wallet.application.handler;

import com.wallet.application.query.GetWalletQuery;
import com.wallet.core.query.QueryHandler;
import com.wallet.domain.model.Wallet;
import com.wallet.infrastructure.cache.WalletStateCache;
import com.wallet.infrastructure.persistence.WalletReadRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GetWalletQueryHandler implements QueryHandler<GetWalletQuery, Wallet> {

    @Inject
    WalletReadRepository walletRepository;

    @Inject
    WalletStateCache walletCache;

    @Override
    public Uni<Wallet> handle(GetWalletQuery query) {
        return walletCache.getWallet(query.getWalletId())
                .onItem().ifNull().switchTo(() -> 
                    walletRepository.findById(query.getWalletId())
                        .onItem().ifNotNull().call(wallet -> 
                            walletCache.cacheWallet(wallet)
                        )
                );
    }
}