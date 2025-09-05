package com.wallet.infrastructure.resilience;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Timeout;

import com.wallet.domain.model.Wallet;
import com.wallet.infrastructure.cache.WalletStateCache;
import com.wallet.infrastructure.metrics.WalletMetrics;
import com.wallet.infrastructure.persistence.WalletReadRepository;

import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Resilient Cache Service with Circuit Breaker Protection
 * 
 * This service wraps Redis cache operations with circuit breakers to provide
 * graceful degradation when cache is unavailable. For financial services,
 * cache failures should not impact correctness, only performance.
 * 
 * Fallback Strategy:
 * - Cache miss → Direct database query
 * - Cache write failure → Continue operation (cache is performance optimization)
 * - Cache read failure → Direct database query with performance degradation
 * 
 * Performance Impact:
 * - Normal: ~8ms (cached response)
 * - Degraded: ~50-200ms (direct database query)
 */
@ApplicationScoped
public class ResilientCacheService {

    @Inject
    WalletStateCache walletCache;

    @Inject
    @ReactiveDataSource("read")
    WalletReadRepository walletRepository;

    @Inject
    WalletMetrics walletMetrics;

    @Inject
    DegradationManager degradationManager;

    @Inject
    GracefulDegradationService gracefulDegradationService;

    /**
     * Get wallet from cache with circuit breaker protection
     * Fallback: Direct database query
     */
    @CircuitBreaker
    @Timeout("redis-operations")
    @Fallback(fallbackMethod = "getWalletFromDatabaseFallback")
    public Uni<Wallet> getWallet(String walletId) {
        return walletCache.getWallet(walletId)
            .onItem().invoke(wallet -> {
                if (wallet != null) {
                    walletMetrics.recordEventPublished("CACHE_HIT");
                } else {
                    walletMetrics.recordEventPublished("CACHE_MISS");
                }
            })
            .onFailure().invoke(throwable -> {
                walletMetrics.incrementFailedOperations("cache_read");
                walletMetrics.recordDatabaseError("redis", throwable);
            });
    }

    /**
     * Cache wallet with circuit breaker protection
     * Fallback: Continue without caching (operation succeeds)
     */
    @CircuitBreaker
    @Timeout("redis-operations")
    @Fallback(fallbackMethod = "skipCachingFallback")
    public Uni<Void> cacheWallet(Wallet wallet) {
        return walletCache.cacheWallet(wallet)
            .onItem().invoke(() -> walletMetrics.recordEventPublished("CACHE_WRITE"))
            .onFailure().invoke(throwable -> {
                walletMetrics.incrementFailedOperations("cache_write");
                walletMetrics.recordDatabaseError("redis", throwable);
            });
    }

    /**
     * Invalidate wallet cache with circuit breaker protection
     * Fallback: Continue without invalidation (eventual consistency)
     */
    @CircuitBreaker
    @Timeout("redis-operations")
    @Fallback(fallbackMethod = "skipCacheInvalidationFallback")
    public Uni<Void> invalidateWallet(String walletId) {
        return walletCache.invalidateWallet(walletId)
            .onItem().invoke(() -> walletMetrics.recordEventPublished("CACHE_INVALIDATION"))
            .onFailure().invoke(throwable -> {
                walletMetrics.incrementFailedOperations("cache_invalidation");
                walletMetrics.recordDatabaseError("redis", throwable);
            });
    }

    /**
     * Get wallet balance from cache with circuit breaker protection
     * This is a high-frequency operation that benefits most from caching
     */
    @CircuitBreaker
    @Timeout("redis-operations")
    @Fallback(fallbackMethod = "getBalanceFromDatabaseFallback")
    public Uni<java.math.BigDecimal> getWalletBalance(String walletId) {
        return walletCache.getWallet(walletId)
            .map(wallet -> wallet != null ? wallet.getBalance() : null)
            .onItem().invoke(balance -> {
                if (balance != null) {
                    walletMetrics.recordEventPublished("BALANCE_CACHE_HIT");
                } else {
                    walletMetrics.recordEventPublished("BALANCE_CACHE_MISS");
                }
            })
            .onFailure().invoke(throwable -> {
                walletMetrics.incrementFailedOperations("balance_cache_read");
                walletMetrics.recordDatabaseError("redis", throwable);
            });
    }

    // ============================================================================
    // FALLBACK METHODS
    // ============================================================================

    /**
     * Fallback for wallet retrieval: Query database directly
     * This maintains correctness with performance degradation
     */
    public Uni<Wallet> getWalletFromDatabaseFallback(String walletId) {
        walletMetrics.incrementCircuitBreakerActivations("redis-cache");
        walletMetrics.incrementCacheBypass();
        walletMetrics.recordDegradedPerformance("cache_bypass");
        
        // Enter cache bypass mode if not already active
        gracefulDegradationService.enterCacheBypassMode("Redis circuit breaker activated during wallet retrieval");
        
        return walletRepository.findById(walletId)
            .onItem().invoke(() -> {
                walletMetrics.incrementDatabaseReads("direct_fallback");
                walletMetrics.recordFallbackExecution("getWalletFromDatabase", "redis_circuit_breaker");
            })
            .onFailure().invoke(throwable -> {
                walletMetrics.incrementFailedOperations("database_read_fallback");
                walletMetrics.recordDatabaseError("direct_fallback", throwable);
            });
    }

    /**
     * Fallback for wallet caching: Skip caching operation
     * The main operation continues successfully without caching
     */
    public Uni<Void> skipCachingFallback(Wallet wallet) {
        walletMetrics.incrementCircuitBreakerActivations("redis-cache");
        walletMetrics.recordFallbackExecution("skipCaching", "redis_circuit_breaker");
        
        // Enter cache bypass mode if not already active
        gracefulDegradationService.enterCacheBypassMode("Redis circuit breaker activated during cache write");
        
        // Operation continues successfully without caching
        return Uni.createFrom().voidItem();
    }

    /**
     * Fallback for cache invalidation: Skip invalidation
     * Accepts eventual consistency risk for availability
     */
    public Uni<Void> skipCacheInvalidationFallback(String walletId) {
        walletMetrics.incrementCircuitBreakerActivations("redis-cache");
        walletMetrics.recordFallbackExecution("skipCacheInvalidation", "redis_circuit_breaker");
        
        // Log for monitoring - eventual consistency risk
        walletMetrics.recordDegradationEvent("CACHE_INVALIDATION", "SKIPPED", 
            "Redis unavailable - cache may be stale");
        
        // Operation continues successfully without invalidation
        return Uni.createFrom().voidItem();
    }

    /**
     * Fallback for balance retrieval: Query database directly
     */
    public Uni<java.math.BigDecimal> getBalanceFromDatabaseFallback(String walletId) {
        walletMetrics.incrementCircuitBreakerActivations("redis-cache");
        walletMetrics.incrementCacheBypass();
        walletMetrics.recordDegradedPerformance("balance_cache_bypass");
        
        // Enter cache bypass mode if not already active
        degradationManager.enterCacheBypassMode("Redis circuit breaker activated during balance query");
        
        return walletRepository.findById(walletId)
            .map(wallet -> wallet != null ? wallet.getBalance() : null)
            .onItem().invoke(() -> {
                walletMetrics.incrementDatabaseReads("balance_direct_fallback");
                walletMetrics.recordFallbackExecution("getBalanceFromDatabase", "redis_circuit_breaker");
            })
            .onFailure().invoke(throwable -> {
                walletMetrics.incrementFailedOperations("balance_database_read_fallback");
                walletMetrics.recordDatabaseError("balance_direct_fallback", throwable);
            });
    }

    // ============================================================================
    // CACHE HEALTH MONITORING
    // ============================================================================

    /**
     * Check cache health and update degradation state
     * This method can be called periodically to monitor cache recovery
     */
    public Uni<Boolean> checkCacheHealth() {
        return walletCache.ping()
            .map(success -> {
                if (success && degradationManager.isCacheBypassMode()) {
                    // Cache recovered, exit bypass mode
                    degradationManager.exitCacheBypassMode("Cache health check successful");
                    walletMetrics.recordDegradationEvent("CACHE_BYPASS", "RECOVERED", "Health check passed");
                }
                return success;
            })
            .onFailure().recoverWithItem(false)
            .onItem().invoke(healthy -> {
                if (healthy) {
                    walletMetrics.recordEventPublished("CACHE_HEALTH_CHECK_SUCCESS");
                } else {
                    walletMetrics.recordEventPublished("CACHE_HEALTH_CHECK_FAILURE");
                }
            });
    }
}
