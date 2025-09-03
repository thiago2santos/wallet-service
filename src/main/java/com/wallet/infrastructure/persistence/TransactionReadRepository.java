package com.wallet.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;

import com.wallet.domain.model.Transaction;
import com.wallet.domain.model.TransactionStatus;
import com.wallet.domain.model.TransactionType;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ReactiveDataSource("read")  // Use replica database for read operations
public class TransactionReadRepository implements PanacheRepository<Transaction> {
    
    public Uni<Transaction> findByReferenceId(String referenceId) {
        return find("referenceId", referenceId).firstResult();
    }

    public Uni<List<Transaction>> findByWalletId(String walletId) {
        return find("walletId", walletId)
            .page(0, 100)
            .list();
    }

    public Uni<List<Transaction>> findByWalletIdAndType(String walletId, TransactionType type) {
        return find("walletId = ?1 and type = ?2", walletId, type).list();
    }

    public Uni<List<Transaction>> findByWalletIdAndStatus(String walletId, TransactionStatus status) {
        return find("walletId = ?1 and status = ?2", walletId, status).list();
    }

    public Uni<List<Transaction>> findByWalletIdAndDateRange(String walletId, 
            LocalDateTime fromDate, LocalDateTime toDate) {
        return find("walletId = ?1 and createdAt between ?2 and ?3", 
            walletId, fromDate, toDate).list();
    }

    public Uni<Boolean> existsByReferenceId(String referenceId) {
        return count("referenceId", referenceId)
            .map(count -> count > 0);
    }
}
