package com.wallet.infrastructure.persistence;

import com.wallet.domain.model.Wallet;
import com.wallet.domain.model.WalletStatus;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
@ReactiveDataSource("write")
public class WalletRepository implements PanacheRepositoryBase<Wallet, String> {
    
    public Uni<Wallet> findByIdAndStatus(String id, WalletStatus status) {
        return find("id = ?1 and status = ?2", id, status).firstResult();
    }

    public Uni<List<Wallet>> findByUserId(String userId) {
        return find("userId", userId).list();
    }

    public Uni<List<Wallet>> findByUserIdAndStatus(String userId, WalletStatus status) {
        return find("userId = ?1 and status = ?2", userId, status).list();
    }

    public Uni<Boolean> existsByUserIdAndCurrency(String userId, String currency) {
        return count("userId = ?1 and currency = ?2", userId, currency)
            .map(count -> count > 0);
    }
}
