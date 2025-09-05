package com.wallet.infrastructure.resilience;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import com.wallet.domain.model.Wallet;
import com.wallet.infrastructure.cache.WalletStateCache;
import com.wallet.infrastructure.persistence.WalletReadRepository;
import com.wallet.infrastructure.metrics.WalletMetrics;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

/**
 * Comprehensive tests for ResilientCacheService circuit breaker functionality
 * 
 * These tests validate:
 * - Circuit breaker activation on Redis failures
 * - Fallback to database queries when cache unavailable
 * - Performance degradation handling
 * - Cache recovery scenarios
 * - Metrics recording for cache operations
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Resilient Cache Service Circuit Breaker Tests")
class ResilientCacheServiceTest {

    @Mock
    private WalletStateCache walletCache;

    @Mock
    private WalletReadRepository walletRepository;

    @Mock
    private WalletMetrics walletMetrics;

    @Mock
    private DegradationManager degradationManager;

    @InjectMocks
    private ResilientCacheService resilientCacheService;

    private Wallet testWallet;

    @BeforeEach
    void setUp() {
        testWallet = createTestWallet();
    }

    // ============================================================================
    // CACHE READ CIRCUIT BREAKER TESTS
    // ============================================================================

    @Test
    @DisplayName("Should return cached wallet when Redis is healthy")
    void shouldReturnCachedWalletWhenRedisHealthy() {
        // Given
        String walletId = "wallet-123";
        when(walletCache.getWallet(walletId))
            .thenReturn(Uni.createFrom().item(testWallet));

        // When
        Uni<Wallet> result = resilientCacheService.getWallet(walletId);

        // Then
        UniAssertSubscriber<Wallet> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted()
                 .assertItem(testWallet);

        // Verify cache was used
        verify(walletCache).getWallet(walletId);
        verify(walletRepository, never()).findById(anyString());
        verify(walletMetrics).recordEventPublished("CACHE_HIT");
    }

    @Test
    @DisplayName("Should fallback to database when Redis fails")
    void shouldFallbackToDatabaseWhenRedisFails() {
        // Given
        String walletId = "wallet-123";
        RuntimeException redisError = new RuntimeException("Redis connection timeout");
        
        when(walletCache.getWallet(walletId))
            .thenReturn(Uni.createFrom().failure(redisError));
        when(walletRepository.findById(walletId))
            .thenReturn(Uni.createFrom().item(testWallet));

        // When
        Uni<Wallet> result = resilientCacheService.getWallet(walletId);

        // Then
        UniAssertSubscriber<Wallet> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted()
                 .assertItem(testWallet);

        // Verify fallback behavior
        verify(walletCache).getWallet(walletId);
        verify(walletRepository).findById(walletId);
        
        // Verify degradation tracking
        verify(degradationManager).enterCacheBypassMode("Redis circuit breaker activated");
        verify(walletMetrics).incrementCircuitBreakerActivations("redis-cache");
        verify(walletMetrics).incrementCacheBypass();
        verify(walletMetrics).recordDegradedPerformance("cache_bypass");
        verify(walletMetrics).incrementDatabaseReads("direct_fallback");
    }

    @Test
    @DisplayName("Should handle cache miss gracefully")
    void shouldHandleCacheMissGracefully() {
        // Given
        String walletId = "wallet-123";
        when(walletCache.getWallet(walletId))
            .thenReturn(Uni.createFrom().nullItem());

        // When
        Uni<Wallet> result = resilientCacheService.getWallet(walletId);

        // Then
        UniAssertSubscriber<Wallet> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted()
                 .assertItem(null);

        verify(walletMetrics).recordEventPublished("CACHE_MISS");
    }

    // ============================================================================
    // CACHE WRITE CIRCUIT BREAKER TESTS
    // ============================================================================

    @Test
    @DisplayName("Should cache wallet successfully when Redis is healthy")
    void shouldCacheWalletWhenRedisHealthy() {
        // Given
        when(walletCache.cacheWallet(testWallet))
            .thenReturn(Uni.createFrom().voidItem());

        // When
        Uni<Void> result = resilientCacheService.cacheWallet(testWallet);

        // Then
        UniAssertSubscriber<Void> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted();

        verify(walletCache).cacheWallet(testWallet);
        verify(walletMetrics).recordEventPublished("CACHE_WRITE");
    }

    @Test
    @DisplayName("Should continue operation when cache write fails")
    void shouldContinueWhenCacheWriteFails() {
        // Given
        RuntimeException redisError = new RuntimeException("Redis write timeout");
        when(walletCache.cacheWallet(testWallet))
            .thenReturn(Uni.createFrom().failure(redisError));

        // When
        Uni<Void> result = resilientCacheService.cacheWallet(testWallet);

        // Then
        UniAssertSubscriber<Void> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted();

        // Verify fallback behavior
        verify(walletCache).cacheWallet(testWallet);
        verify(degradationManager).enterCacheBypassMode("Redis circuit breaker activated during cache write");
        verify(walletMetrics).incrementCircuitBreakerActivations("redis-cache");
        verify(walletMetrics).recordFallbackExecution("skipCaching", "redis_circuit_breaker");
    }

    // ============================================================================
    // CACHE INVALIDATION CIRCUIT BREAKER TESTS
    // ============================================================================

    @Test
    @DisplayName("Should invalidate cache successfully when Redis is healthy")
    void shouldInvalidateCacheWhenRedisHealthy() {
        // Given
        String walletId = "wallet-123";
        when(walletCache.invalidateWallet(walletId))
            .thenReturn(Uni.createFrom().voidItem());

        // When
        Uni<Void> result = resilientCacheService.invalidateWallet(walletId);

        // Then
        UniAssertSubscriber<Void> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted();

        verify(walletCache).invalidateWallet(walletId);
        verify(walletMetrics).recordEventPublished("CACHE_INVALIDATION");
    }

    @Test
    @DisplayName("Should continue operation when cache invalidation fails")
    void shouldContinueWhenCacheInvalidationFails() {
        // Given
        String walletId = "wallet-123";
        RuntimeException redisError = new RuntimeException("Redis delete failed");
        when(walletCache.invalidateWallet(walletId))
            .thenReturn(Uni.createFrom().failure(redisError));

        // When
        Uni<Void> result = resilientCacheService.invalidateWallet(walletId);

        // Then
        UniAssertSubscriber<Void> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted();

        // Verify eventual consistency handling
        verify(walletCache).invalidateWallet(walletId);
        verify(walletMetrics).incrementCircuitBreakerActivations("redis-cache");
        verify(walletMetrics).recordFallbackExecution("skipCacheInvalidation", "redis_circuit_breaker");
        verify(walletMetrics).recordDegradationEvent("CACHE_INVALIDATION", "SKIPPED", 
            "Redis unavailable - cache may be stale");
    }

    // ============================================================================
    // BALANCE QUERY CIRCUIT BREAKER TESTS
    // ============================================================================

    @Test
    @DisplayName("Should return cached balance when Redis is healthy")
    void shouldReturnCachedBalanceWhenRedisHealthy() {
        // Given
        String walletId = "wallet-123";
        when(walletCache.getWallet(walletId))
            .thenReturn(Uni.createFrom().item(testWallet));

        // When
        Uni<BigDecimal> result = resilientCacheService.getWalletBalance(walletId);

        // Then
        UniAssertSubscriber<BigDecimal> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted()
                 .assertItem(testWallet.getBalance());

        verify(walletMetrics).recordEventPublished("BALANCE_CACHE_HIT");
    }

    @Test
    @DisplayName("Should fallback to database for balance when Redis fails")
    void shouldFallbackToDatabaseForBalanceWhenRedisFails() {
        // Given
        String walletId = "wallet-123";
        RuntimeException redisError = new RuntimeException("Redis cluster down");
        
        when(walletCache.getWallet(walletId))
            .thenReturn(Uni.createFrom().failure(redisError));
        when(walletRepository.findById(walletId))
            .thenReturn(Uni.createFrom().item(testWallet));

        // When
        Uni<BigDecimal> result = resilientCacheService.getWalletBalance(walletId);

        // Then
        UniAssertSubscriber<BigDecimal> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted()
                 .assertItem(testWallet.getBalance());

        // Verify fallback behavior
        verify(walletCache).getWallet(walletId);
        verify(walletRepository).findById(walletId);
        verify(degradationManager).enterCacheBypassMode("Redis circuit breaker activated during balance query");
        verify(walletMetrics).incrementCircuitBreakerActivations("redis-cache");
        verify(walletMetrics).recordDegradedPerformance("balance_cache_bypass");
    }

    // ============================================================================
    // CACHE HEALTH MONITORING TESTS
    // ============================================================================

    @Test
    @DisplayName("Should detect cache recovery and exit bypass mode")
    void shouldDetectCacheRecoveryAndExitBypassMode() {
        // Given
        when(walletCache.ping())
            .thenReturn(Uni.createFrom().item(true));
        when(degradationManager.isCacheBypassMode())
            .thenReturn(true);

        // When
        Uni<Boolean> result = resilientCacheService.checkCacheHealth();

        // Then
        UniAssertSubscriber<Boolean> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted()
                 .assertItem(true);

        // Verify recovery handling
        verify(degradationManager).exitCacheBypassMode("Cache health check successful");
        verify(walletMetrics).recordDegradationEvent("CACHE_BYPASS", "RECOVERED", "Health check passed");
        verify(walletMetrics).recordEventPublished("CACHE_HEALTH_CHECK_SUCCESS");
    }

    @Test
    @DisplayName("Should handle cache health check failure")
    void shouldHandleCacheHealthCheckFailure() {
        // Given
        when(walletCache.ping())
            .thenReturn(Uni.createFrom().failure(new RuntimeException("Ping failed")));

        // When
        Uni<Boolean> result = resilientCacheService.checkCacheHealth();

        // Then
        UniAssertSubscriber<Boolean> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted()
                 .assertItem(false);

        verify(walletMetrics).recordEventPublished("CACHE_HEALTH_CHECK_FAILURE");
        verify(degradationManager, never()).exitCacheBypassMode(anyString());
    }

    // ============================================================================
    // PERFORMANCE AND STRESS TESTS
    // ============================================================================

    @Test
    @DisplayName("Should handle high frequency cache operations during Redis outage")
    void shouldHandleHighFrequencyCacheOperationsDuringOutage() {
        // Given
        String walletId = "wallet-123";
        RuntimeException redisError = new RuntimeException("Redis overloaded");
        
        when(walletCache.getWallet(walletId))
            .thenReturn(Uni.createFrom().failure(redisError));
        when(walletRepository.findById(walletId))
            .thenReturn(Uni.createFrom().item(testWallet));

        // When - Simulate high frequency requests
        Uni<Wallet> result1 = resilientCacheService.getWallet(walletId);
        Uni<Wallet> result2 = resilientCacheService.getWallet(walletId);
        Uni<Wallet> result3 = resilientCacheService.getWallet(walletId);

        // Then - All should fallback to database successfully
        UniAssertSubscriber<Wallet> subscriber1 = result1.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        UniAssertSubscriber<Wallet> subscriber2 = result2.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        UniAssertSubscriber<Wallet> subscriber3 = result3.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber1.assertCompleted().assertItem(testWallet);
        subscriber2.assertCompleted().assertItem(testWallet);
        subscriber3.assertCompleted().assertItem(testWallet);

        // Verify all requests fell back to database
        verify(walletRepository, times(3)).findById(walletId);
        verify(walletMetrics, times(3)).incrementCacheBypass();
        verify(walletMetrics, times(3)).recordDegradedPerformance("cache_bypass");
    }

    @Test
    @DisplayName("Should maintain correctness during cache write failures")
    void shouldMaintainCorrectnessduringCacheWriteFailures() {
        // Given
        RuntimeException redisError = new RuntimeException("Redis memory full");
        when(walletCache.cacheWallet(any(Wallet.class)))
            .thenReturn(Uni.createFrom().failure(redisError));

        // When - Multiple cache write attempts
        Uni<Void> result1 = resilientCacheService.cacheWallet(testWallet);
        Uni<Void> result2 = resilientCacheService.cacheWallet(testWallet);

        // Then - All operations should complete successfully (cache is optional)
        UniAssertSubscriber<Void> subscriber1 = result1.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        UniAssertSubscriber<Void> subscriber2 = result2.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber1.assertCompleted();
        subscriber2.assertCompleted();

        // Verify degradation mode entered
        verify(degradationManager, atLeast(1)).enterCacheBypassMode(anyString());
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    private Wallet createTestWallet() {
        Wallet wallet = new Wallet();
        wallet.setId("wallet-123");
        wallet.setUserId("user-123");
        wallet.setBalance(BigDecimal.valueOf(250.75));
        wallet.setStatus("ACTIVE");
        wallet.setCreatedAt(Instant.now());
        wallet.setUpdatedAt(Instant.now());
        return wallet;
    }
}
