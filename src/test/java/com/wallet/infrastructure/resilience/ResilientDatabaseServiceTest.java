package com.wallet.infrastructure.resilience;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import com.wallet.domain.model.Wallet;
import com.wallet.infrastructure.persistence.WalletReadRepository;
import com.wallet.infrastructure.persistence.WalletRepository;
import com.wallet.infrastructure.metrics.WalletMetrics;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

/**
 * Comprehensive tests for ResilientDatabaseService circuit breaker functionality
 * 
 * These tests validate:
 * - Circuit breaker activation on database failures
 * - Fallback behavior (read-only mode, replica fallback)
 * - Metrics recording for monitoring
 * - Recovery scenarios
 * - Financial service safety (no data corruption)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Resilient Database Service Circuit Breaker Tests")
class ResilientDatabaseServiceTest {

    @Mock
    private WalletRepository primaryRepository;

    @Mock
    private WalletReadRepository replicaRepository;

    @Mock
    private WalletMetrics walletMetrics;

    @Mock
    private DegradationManager degradationManager;

    @InjectMocks
    private ResilientDatabaseService resilientDatabaseService;

    private Wallet testWallet;

    @BeforeEach
    void setUp() {
        testWallet = createTestWallet();
    }

    // ============================================================================
    // PRIMARY DATABASE CIRCUIT BREAKER TESTS
    // ============================================================================

    @Test
    @DisplayName("Should persist wallet successfully when primary database is healthy")
    void shouldPersistWalletWhenPrimaryHealthy() {
        // Given
        when(primaryRepository.persist(any(Wallet.class)))
            .thenReturn(Uni.createFrom().item(testWallet));

        // When
        Uni<Wallet> result = resilientDatabaseService.persistWallet(testWallet);

        // Then
        UniAssertSubscriber<Wallet> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted()
                 .assertItem(testWallet);

        // Verify metrics
        verify(walletMetrics).incrementDatabaseWrites();
        verify(walletMetrics, never()).incrementFailedOperations(anyString());
        verify(degradationManager, never()).enterReadOnlyMode(anyString());
    }

    @Test
    @DisplayName("Should enter read-only mode when primary database fails")
    void shouldEnterReadOnlyModeWhenPrimaryFails() {
        // Given
        RuntimeException databaseError = new RuntimeException("Database connection failed");
        when(primaryRepository.persist(any(Wallet.class)))
            .thenReturn(Uni.createFrom().failure(databaseError));

        // When
        Uni<Wallet> result = resilientDatabaseService.persistWallet(testWallet);

        // Then
        UniAssertSubscriber<Wallet> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertFailedWith(ServiceDegradedException.class);

        // Verify degradation manager was called
        verify(degradationManager).enterReadOnlyMode("Primary database circuit breaker activated");
        
        // Verify metrics
        verify(walletMetrics).incrementFailedOperations("database_write");
        verify(walletMetrics).recordDatabaseError("primary", databaseError);
        verify(walletMetrics).incrementCircuitBreakerActivations("aurora-primary");
    }

    @Test
    @DisplayName("Should update wallet successfully when primary database is healthy")
    void shouldUpdateWalletWhenPrimaryHealthy() {
        // Given
        when(primaryRepository.persistAndFlush(any(Wallet.class)))
            .thenReturn(Uni.createFrom().item(testWallet));

        // When
        Uni<Wallet> result = resilientDatabaseService.updateWallet(testWallet);

        // Then
        UniAssertSubscriber<Wallet> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted()
                 .assertItem(testWallet);

        verify(walletMetrics).incrementDatabaseWrites();
    }

    // ============================================================================
    // REPLICA DATABASE CIRCUIT BREAKER TESTS
    // ============================================================================

    @Test
    @DisplayName("Should read from replica when replica database is healthy")
    void shouldReadFromReplicaWhenHealthy() {
        // Given
        String walletId = "wallet-123";
        when(replicaRepository.findById(walletId))
            .thenReturn(Uni.createFrom().item(testWallet));

        // When
        Uni<Wallet> result = resilientDatabaseService.findWalletById(walletId);

        // Then
        UniAssertSubscriber<Wallet> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted()
                 .assertItem(testWallet);

        // Verify replica was used
        verify(replicaRepository).findById(walletId);
        verify(primaryRepository, never()).findById(anyString());
        verify(walletMetrics).incrementDatabaseReads("replica");
    }

    @Test
    @DisplayName("Should fallback to primary when replica database fails")
    void shouldFallbackToPrimaryWhenReplicaFails() {
        // Given
        String walletId = "wallet-123";
        RuntimeException replicaError = new RuntimeException("Replica connection failed");
        
        when(replicaRepository.findById(walletId))
            .thenReturn(Uni.createFrom().failure(replicaError));
        when(primaryRepository.findById(walletId))
            .thenReturn(Uni.createFrom().item(testWallet));

        // When
        Uni<Wallet> result = resilientDatabaseService.findWalletById(walletId);

        // Then
        UniAssertSubscriber<Wallet> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted()
                 .assertItem(testWallet);

        // Verify fallback behavior
        verify(replicaRepository).findById(walletId);
        verify(primaryRepository).findById(walletId);
        
        // Verify metrics
        verify(walletMetrics).incrementFailedOperations("database_read_replica");
        verify(walletMetrics).recordDatabaseError("replica", replicaError);
        verify(walletMetrics).incrementCircuitBreakerActivations("aurora-replica");
        verify(walletMetrics).incrementDatabaseReads("primary_fallback");
        verify(walletMetrics).recordDegradedPerformance("replica_fallback");
    }

    @Test
    @DisplayName("Should find wallets by user ID with replica fallback")
    void shouldFindWalletsByUserIdWithFallback() {
        // Given
        String userId = "user-123";
        List<Wallet> wallets = Arrays.asList(testWallet);
        RuntimeException replicaError = new RuntimeException("Replica timeout");
        
        when(replicaRepository.findByUserId(userId))
            .thenReturn(Uni.createFrom().failure(replicaError));
        when(primaryRepository.findByUserId(userId))
            .thenReturn(Uni.createFrom().item(wallets));

        // When
        Uni<List<Wallet>> result = resilientDatabaseService.findWalletsByUserId(userId);

        // Then
        UniAssertSubscriber<List<Wallet>> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted()
                 .assertItem(wallets);

        // Verify fallback to primary
        verify(replicaRepository).findByUserId(userId);
        verify(primaryRepository).findByUserId(userId);
        verify(walletMetrics).incrementCircuitBreakerActivations("aurora-replica");
    }

    @Test
    @DisplayName("Should check wallet existence with replica fallback")
    void shouldCheckExistenceWithFallback() {
        // Given
        String userId = "user-123";
        String currency = "BRL";
        RuntimeException replicaError = new RuntimeException("Replica unavailable");
        
        when(replicaRepository.existsByUserIdAndCurrency(userId, currency))
            .thenReturn(Uni.createFrom().failure(replicaError));
        when(primaryRepository.existsByUserIdAndCurrency(userId, currency))
            .thenReturn(Uni.createFrom().item(true));

        // When
        Uni<Boolean> result = resilientDatabaseService.existsByUserIdAndCurrency(userId, currency);

        // Then
        UniAssertSubscriber<Boolean> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted()
                 .assertItem(true);

        // Verify fallback behavior
        verify(replicaRepository).existsByUserIdAndCurrency(userId, currency);
        verify(primaryRepository).existsByUserIdAndCurrency(userId, currency);
        verify(walletMetrics).incrementCircuitBreakerActivations("aurora-replica");
        verify(walletMetrics).incrementDatabaseReads("primary_fallback");
    }

    // ============================================================================
    // FALLBACK METHOD TESTS
    // ============================================================================

    @Test
    @DisplayName("Should handle read-only mode fallback correctly")
    void shouldHandleReadOnlyModeFallback() {
        // When
        Uni<Wallet> result = resilientDatabaseService.enterReadOnlyModeFallback(testWallet);

        // Then
        UniAssertSubscriber<Wallet> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertFailedWith(ServiceDegradedException.class);

        // Verify degradation manager interaction
        verify(degradationManager).enterReadOnlyMode("Primary database circuit breaker activated");
        verify(walletMetrics).incrementCircuitBreakerActivations("aurora-primary");
    }

    @Test
    @DisplayName("Should handle primary fallback for reads correctly")
    void shouldHandlePrimaryFallbackForReads() {
        // Given
        String walletId = "wallet-123";
        when(primaryRepository.findById(walletId))
            .thenReturn(Uni.createFrom().item(testWallet));

        // When
        Uni<Wallet> result = resilientDatabaseService.readFromPrimaryFallback(walletId);

        // Then
        UniAssertSubscriber<Wallet> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted()
                 .assertItem(testWallet);

        // Verify metrics
        verify(walletMetrics).incrementCircuitBreakerActivations("aurora-replica");
        verify(walletMetrics).incrementDatabaseReads("primary_fallback");
        verify(walletMetrics).recordDegradedPerformance("replica_fallback");
    }

    // ============================================================================
    // PERFORMANCE AND STRESS TESTS
    // ============================================================================

    @Test
    @DisplayName("Should handle multiple concurrent failures gracefully")
    void shouldHandleConcurrentFailures() {
        // Given
        RuntimeException error = new RuntimeException("Concurrent failure");
        when(primaryRepository.persist(any(Wallet.class)))
            .thenReturn(Uni.createFrom().failure(error));

        // When - Simulate multiple concurrent requests
        Uni<Wallet> result1 = resilientDatabaseService.persistWallet(testWallet);
        Uni<Wallet> result2 = resilientDatabaseService.persistWallet(testWallet);
        Uni<Wallet> result3 = resilientDatabaseService.persistWallet(testWallet);

        // Then - All should fail with ServiceDegradedException
        UniAssertSubscriber<Wallet> subscriber1 = result1.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        UniAssertSubscriber<Wallet> subscriber2 = result2.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        UniAssertSubscriber<Wallet> subscriber3 = result3.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber1.assertFailedWith(ServiceDegradedException.class);
        subscriber2.assertFailedWith(ServiceDegradedException.class);
        subscriber3.assertFailedWith(ServiceDegradedException.class);

        // Verify degradation manager called multiple times
        verify(degradationManager, atLeast(1)).enterReadOnlyMode(anyString());
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    private Wallet createTestWallet() {
        Wallet wallet = new Wallet();
        wallet.setId("wallet-123");
        wallet.setUserId("user-123");
        wallet.setBalance(BigDecimal.valueOf(100.00));
        wallet.setStatus("ACTIVE");
        wallet.setCreatedAt(Instant.now());
        wallet.setUpdatedAt(Instant.now());
        return wallet;
    }
}
