package com.wallet.infrastructure.resilience;

import java.util.List;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;

import com.wallet.domain.model.Wallet;
import com.wallet.infrastructure.persistence.WalletReadRepository;
import com.wallet.infrastructure.persistence.WalletRepository;
import com.wallet.infrastructure.metrics.WalletMetrics;

import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Resilient Database Service with Circuit Breaker Protection
 * 
 * This service wraps database operations with circuit breakers to prevent
 * cascade failures and provide graceful degradation for the financial wallet service.
 * 
 * Financial Service Considerations:
 * - Write operations are more critical than reads
 * - Primary database failures trigger read-only mode
 * - Replica failures fall back to primary database
 * - All failures are logged for audit purposes
 */
@ApplicationScoped
public class ResilientDatabaseService {

    @Inject
    @ReactiveDataSource("write")
    WalletRepository primaryRepository;

    @Inject
    @ReactiveDataSource("read")
    WalletReadRepository replicaRepository;

    @Inject
    WalletMetrics walletMetrics;

    @Inject
    DegradationManager degradationManager;

    /**
     * Write operations with circuit breaker protection
     * Fallback: Enter read-only mode to prevent data corruption
     */
    @CircuitBreaker
    @Fallback(fallbackMethod = "enterReadOnlyModeFallback")
    public Uni<Wallet> persistWallet(Wallet wallet) {
        return primaryRepository.persist(wallet)
            .onItem().invoke(() -> walletMetrics.incrementDatabaseWrites())
            .onFailure().invoke(throwable -> {
                walletMetrics.incrementFailedOperations("database_write");
                walletMetrics.recordDatabaseError("primary", throwable);
            });
    }

    /**
     * Update wallet with circuit breaker protection
     */
    @CircuitBreaker
    @Fallback(fallbackMethod = "enterReadOnlyModeFallback")
    public Uni<Wallet> updateWallet(Wallet wallet) {
        return primaryRepository.persistAndFlush(wallet)
            .onItem().invoke(() -> walletMetrics.incrementDatabaseWrites())
            .onFailure().invoke(throwable -> {
                walletMetrics.incrementFailedOperations("database_write");
                walletMetrics.recordDatabaseError("primary", throwable);
            });
    }

    /**
     * Read operations with replica circuit breaker and primary fallback
     */
    @CircuitBreaker
    @Fallback(fallbackMethod = "readFromPrimaryFallback")
    public Uni<Wallet> findWalletById(String walletId) {
        return replicaRepository.findById(walletId)
            .onItem().invoke(() -> walletMetrics.incrementDatabaseReads("replica"))
            .onFailure().invoke(throwable -> {
                walletMetrics.incrementFailedOperations("database_read_replica");
                walletMetrics.recordDatabaseError("replica", throwable);
            });
    }

    /**
     * Find wallets by user ID with circuit breaker protection
     */
    @CircuitBreaker
    @Fallback(fallbackMethod = "findByUserIdFromPrimaryFallback")
    public Uni<List<Wallet>> findWalletsByUserId(String userId) {
        return replicaRepository.findByUserId(userId)
            .onItem().invoke(() -> walletMetrics.incrementDatabaseReads("replica"))
            .onFailure().invoke(throwable -> {
                walletMetrics.incrementFailedOperations("database_read_replica");
                walletMetrics.recordDatabaseError("replica", throwable);
            });
    }

    /**
     * Check wallet existence with circuit breaker protection
     */
    @CircuitBreaker
    @Fallback(fallbackMethod = "existsByUserIdFromPrimaryFallback")
    public Uni<Boolean> existsByUserIdAndCurrency(String userId, String currency) {
        return replicaRepository.existsByUserIdAndCurrency(userId, currency)
            .onItem().invoke(() -> walletMetrics.incrementDatabaseReads("replica"))
            .onFailure().invoke(throwable -> {
                walletMetrics.incrementFailedOperations("database_read_replica");
                walletMetrics.recordDatabaseError("replica", throwable);
            });
    }

    // ============================================================================
    // FALLBACK METHODS
    // ============================================================================

    /**
     * Fallback for write operations: Enter read-only mode
     * This prevents data corruption when primary database is unavailable
     */
    public Uni<Wallet> enterReadOnlyModeFallback(Wallet wallet) {
        walletMetrics.incrementCircuitBreakerActivations("aurora-primary");
        degradationManager.enterReadOnlyMode("Primary database circuit breaker activated");
        
        return Uni.createFrom().failure(
            ServiceDegradedException.readOnlyMode()
        );
    }

    /**
     * Fallback for replica reads: Use primary database
     * This maintains read functionality when replica is unavailable
     */
    public Uni<Wallet> readFromPrimaryFallback(String walletId) {
        walletMetrics.incrementCircuitBreakerActivations("aurora-replica");
        walletMetrics.incrementDatabaseReads("primary_fallback");
        
        return primaryRepository.findById(walletId)
            .onItem().invoke(() -> walletMetrics.recordDegradedPerformance("replica_fallback"))
            .onFailure().invoke(throwable -> {
                walletMetrics.incrementFailedOperations("database_read_primary_fallback");
                walletMetrics.recordDatabaseError("primary_fallback", throwable);
            });
    }

    /**
     * Fallback for finding wallets by user ID
     */
    public Uni<List<Wallet>> findByUserIdFromPrimaryFallback(String userId) {
        walletMetrics.incrementCircuitBreakerActivations("aurora-replica");
        walletMetrics.incrementDatabaseReads("primary_fallback");
        
        return primaryRepository.findByUserId(userId)
            .onItem().invoke(() -> walletMetrics.recordDegradedPerformance("replica_fallback"))
            .onFailure().invoke(throwable -> {
                walletMetrics.incrementFailedOperations("database_read_primary_fallback");
                walletMetrics.recordDatabaseError("primary_fallback", throwable);
            });
    }

    /**
     * Fallback for existence check
     */
    public Uni<Boolean> existsByUserIdFromPrimaryFallback(String userId, String currency) {
        walletMetrics.incrementCircuitBreakerActivations("aurora-replica");
        walletMetrics.incrementDatabaseReads("primary_fallback");
        
        return primaryRepository.existsByUserIdAndCurrency(userId, currency)
            .onItem().invoke(() -> walletMetrics.recordDegradedPerformance("replica_fallback"))
            .onFailure().invoke(throwable -> {
                walletMetrics.incrementFailedOperations("database_read_primary_fallback");
                walletMetrics.recordDatabaseError("primary_fallback", throwable);
            });
    }
}
