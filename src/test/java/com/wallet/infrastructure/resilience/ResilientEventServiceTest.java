package com.wallet.infrastructure.resilience;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import com.wallet.infrastructure.outbox.OutboxEventService;
import com.wallet.infrastructure.outbox.OutboxEvent;
import com.wallet.infrastructure.metrics.WalletMetrics;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.reactive.messaging.MutinyEmitter;

/**
 * Comprehensive tests for ResilientEventService circuit breaker functionality
 * 
 * These tests validate:
 * - Circuit breaker activation on Kafka failures
 * - Fallback to outbox pattern for guaranteed event delivery
 * - Event preservation during Kafka outages (critical for financial compliance)
 * - Recovery scenarios and health monitoring
 * - Metrics recording for audit purposes
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Resilient Event Service Circuit Breaker Tests")
class ResilientEventServiceTest {

    @Mock
    private MutinyEmitter<String> kafkaEmitter;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private WalletMetrics walletMetrics;

    @Mock
    private DegradationManager degradationManager;

    @InjectMocks
    private ResilientEventService resilientEventService;

    private OutboxEvent mockOutboxEvent;

    @BeforeEach
    void setUp() {
        mockOutboxEvent = createMockOutboxEvent();
        when(outboxEventService.storeWalletEvent(anyString(), anyString(), any()))
            .thenReturn(Uni.createFrom().item(mockOutboxEvent));
    }

    // ============================================================================
    // WALLET CREATED EVENT CIRCUIT BREAKER TESTS
    // ============================================================================

    @Test
    @DisplayName("Should publish wallet created event directly when Kafka is healthy")
    void shouldPublishWalletCreatedEventWhenKafkaHealthy() {
        // Given
        String walletId = "wallet-123";
        String userId = "user-123";
        when(kafkaEmitter.send(anyString()))
            .thenReturn(Uni.createFrom().voidItem());

        // When
        Uni<Void> result = resilientEventService.publishWalletCreatedEvent(walletId, userId);

        // Then
        UniAssertSubscriber<Void> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted();

        // Verify direct Kafka publishing
        verify(kafkaEmitter).send(anyString());
        verify(outboxEventService, never()).storeWalletEvent(anyString(), anyString(), any());
        verify(walletMetrics).recordEventPublished("WALLET_CREATED");
        verify(walletMetrics).recordEventPublished("KAFKA_DIRECT");
    }

    @Test
    @DisplayName("Should fallback to outbox when Kafka fails for wallet created event")
    void shouldFallbackToOutboxWhenKafkaFailsForWalletCreated() {
        // Given
        String walletId = "wallet-123";
        String userId = "user-123";
        RuntimeException kafkaError = new RuntimeException("Kafka broker unavailable");
        
        when(kafkaEmitter.send(anyString()))
            .thenReturn(Uni.createFrom().failure(kafkaError));

        // When
        Uni<Void> result = resilientEventService.publishWalletCreatedEvent(walletId, userId);

        // Then
        UniAssertSubscriber<Void> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted();

        // Verify fallback to outbox
        verify(kafkaEmitter).send(anyString());
        verify(outboxEventService).storeWalletEvent(eq(walletId), eq("WalletCreated"), any());
        
        // Verify degradation tracking
        verify(degradationManager).setEventProcessingDegraded("Kafka circuit breaker activated during wallet creation");
        verify(walletMetrics).incrementCircuitBreakerActivations("kafka-events");
        verify(walletMetrics).recordFallbackExecution("storeWalletCreatedInOutbox", "kafka_circuit_breaker");
        verify(walletMetrics).recordEventPublished("WALLET_CREATED_OUTBOX");
        verify(walletMetrics).recordEventPublished("OUTBOX_STORED");
    }

    // ============================================================================
    // FUNDS DEPOSITED EVENT CIRCUIT BREAKER TESTS
    // ============================================================================

    @Test
    @DisplayName("Should publish funds deposited event directly when Kafka is healthy")
    void shouldPublishFundsDepositedEventWhenKafkaHealthy() {
        // Given
        String walletId = "wallet-123";
        BigDecimal amount = BigDecimal.valueOf(100.00);
        String referenceId = "dep-123";
        String description = "Test deposit";
        
        when(kafkaEmitter.send(anyString()))
            .thenReturn(Uni.createFrom().voidItem());

        // When
        Uni<Void> result = resilientEventService.publishFundsDepositedEvent(
            walletId, amount, referenceId, description);

        // Then
        UniAssertSubscriber<Void> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted();

        // Verify direct publishing and metrics
        verify(kafkaEmitter).send(anyString());
        verify(walletMetrics).recordEventPublished("FUNDS_DEPOSITED");
        verify(walletMetrics).recordEventPublished("KAFKA_DIRECT");
        verify(walletMetrics).recordDepositAmount(amount);
    }

    @Test
    @DisplayName("Should fallback to outbox when Kafka fails for funds deposited event")
    void shouldFallbackToOutboxWhenKafkaFailsForFundsDeposited() {
        // Given
        String walletId = "wallet-123";
        BigDecimal amount = BigDecimal.valueOf(100.00);
        String referenceId = "dep-123";
        String description = "Test deposit";
        RuntimeException kafkaError = new RuntimeException("Kafka partition unavailable");
        
        when(kafkaEmitter.send(anyString()))
            .thenReturn(Uni.createFrom().failure(kafkaError));

        // When
        Uni<Void> result = resilientEventService.publishFundsDepositedEvent(
            walletId, amount, referenceId, description);

        // Then
        UniAssertSubscriber<Void> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted();

        // Verify fallback behavior
        verify(outboxEventService).storeWalletEvent(eq(walletId), eq("FundsDeposited"), any());
        verify(degradationManager).setEventProcessingDegraded("Kafka circuit breaker activated during deposit");
        verify(walletMetrics).recordEventPublished("FUNDS_DEPOSITED_OUTBOX");
        verify(walletMetrics).recordDepositAmount(amount);
    }

    // ============================================================================
    // FUNDS WITHDRAWN EVENT CIRCUIT BREAKER TESTS
    // ============================================================================

    @Test
    @DisplayName("Should publish funds withdrawn event directly when Kafka is healthy")
    void shouldPublishFundsWithdrawnEventWhenKafkaHealthy() {
        // Given
        String walletId = "wallet-123";
        BigDecimal amount = BigDecimal.valueOf(50.00);
        String referenceId = "wd-123";
        String description = "Test withdrawal";
        
        when(kafkaEmitter.send(anyString()))
            .thenReturn(Uni.createFrom().voidItem());

        // When
        Uni<Void> result = resilientEventService.publishFundsWithdrawnEvent(
            walletId, amount, referenceId, description);

        // Then
        UniAssertSubscriber<Void> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted();

        verify(walletMetrics).recordEventPublished("FUNDS_WITHDRAWN");
        verify(walletMetrics).recordWithdrawalAmount(amount);
    }

    @Test
    @DisplayName("Should fallback to outbox when Kafka fails for funds withdrawn event")
    void shouldFallbackToOutboxWhenKafkaFailsForFundsWithdrawn() {
        // Given
        String walletId = "wallet-123";
        BigDecimal amount = BigDecimal.valueOf(50.00);
        String referenceId = "wd-123";
        String description = "Test withdrawal";
        RuntimeException kafkaError = new RuntimeException("Kafka cluster down");
        
        when(kafkaEmitter.send(anyString()))
            .thenReturn(Uni.createFrom().failure(kafkaError));

        // When
        Uni<Void> result = resilientEventService.publishFundsWithdrawnEvent(
            walletId, amount, referenceId, description);

        // Then
        UniAssertSubscriber<Void> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted();

        // Verify outbox fallback
        verify(outboxEventService).storeWalletEvent(eq(walletId), eq("FundsWithdrawn"), any());
        verify(degradationManager).setEventProcessingDegraded("Kafka circuit breaker activated during withdrawal");
        verify(walletMetrics).recordEventPublished("FUNDS_WITHDRAWN_OUTBOX");
        verify(walletMetrics).recordWithdrawalAmount(amount);
    }

    // ============================================================================
    // FUNDS TRANSFERRED EVENT CIRCUIT BREAKER TESTS
    // ============================================================================

    @Test
    @DisplayName("Should publish funds transferred event directly when Kafka is healthy")
    void shouldPublishFundsTransferredEventWhenKafkaHealthy() {
        // Given
        String sourceWalletId = "wallet-123";
        String destinationWalletId = "wallet-456";
        BigDecimal amount = BigDecimal.valueOf(75.00);
        String referenceId = "xfer-123";
        String description = "Test transfer";
        
        when(kafkaEmitter.send(anyString()))
            .thenReturn(Uni.createFrom().voidItem());

        // When
        Uni<Void> result = resilientEventService.publishFundsTransferredEvent(
            sourceWalletId, destinationWalletId, amount, referenceId, description);

        // Then
        UniAssertSubscriber<Void> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted();

        verify(walletMetrics).recordEventPublished("FUNDS_TRANSFERRED");
        verify(walletMetrics).recordTransferAmount(amount);
    }

    @Test
    @DisplayName("Should fallback to outbox when Kafka fails for funds transferred event")
    void shouldFallbackToOutboxWhenKafkaFailsForFundsTransferred() {
        // Given
        String sourceWalletId = "wallet-123";
        String destinationWalletId = "wallet-456";
        BigDecimal amount = BigDecimal.valueOf(75.00);
        String referenceId = "xfer-123";
        String description = "Test transfer";
        RuntimeException kafkaError = new RuntimeException("Kafka producer timeout");
        
        when(kafkaEmitter.send(anyString()))
            .thenReturn(Uni.createFrom().failure(kafkaError));

        // When
        Uni<Void> result = resilientEventService.publishFundsTransferredEvent(
            sourceWalletId, destinationWalletId, amount, referenceId, description);

        // Then
        UniAssertSubscriber<Void> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted();

        // Verify outbox fallback
        verify(outboxEventService).storeWalletEvent(eq(sourceWalletId), eq("FundsTransferred"), any());
        verify(degradationManager).setEventProcessingDegraded("Kafka circuit breaker activated during transfer");
        verify(walletMetrics).recordEventPublished("FUNDS_TRANSFERRED_OUTBOX");
        verify(walletMetrics).recordTransferAmount(amount);
    }

    // ============================================================================
    // GENERIC EVENT PUBLISHING TESTS
    // ============================================================================

    @Test
    @DisplayName("Should publish generic wallet event directly when Kafka is healthy")
    void shouldPublishGenericWalletEventWhenKafkaHealthy() {
        // Given
        String walletId = "wallet-123";
        String eventType = "CUSTOM_EVENT";
        Object eventData = new TestEventData("test-data");
        
        when(kafkaEmitter.send(anyString()))
            .thenReturn(Uni.createFrom().voidItem());

        // When
        Uni<Void> result = resilientEventService.publishWalletEvent(walletId, eventType, eventData);

        // Then
        UniAssertSubscriber<Void> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted();

        verify(kafkaEmitter).send(anyString());
        verify(walletMetrics).recordEventPublished(eventType);
        verify(walletMetrics).recordEventPublished("KAFKA_DIRECT");
    }

    @Test
    @DisplayName("Should fallback to outbox for generic event when Kafka fails")
    void shouldFallbackToOutboxForGenericEventWhenKafkaFails() {
        // Given
        String walletId = "wallet-123";
        String eventType = "CUSTOM_EVENT";
        Object eventData = new TestEventData("test-data");
        RuntimeException kafkaError = new RuntimeException("Kafka serialization error");
        
        when(kafkaEmitter.send(anyString()))
            .thenReturn(Uni.createFrom().failure(kafkaError));

        // When
        Uni<Void> result = resilientEventService.publishWalletEvent(walletId, eventType, eventData);

        // Then
        UniAssertSubscriber<Void> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted();

        // Verify fallback
        verify(outboxEventService).storeWalletEvent(eq(walletId), eq(eventType), eq(eventData));
        verify(degradationManager).setEventProcessingDegraded("Kafka circuit breaker activated");
        verify(walletMetrics).recordEventPublished("OUTBOX_STORED");
    }

    // ============================================================================
    // KAFKA HEALTH MONITORING TESTS
    // ============================================================================

    @Test
    @DisplayName("Should detect Kafka recovery and clear degradation")
    void shouldDetectKafkaRecoveryAndClearDegradation() {
        // Given
        when(kafkaEmitter.send("HEALTH_CHECK"))
            .thenReturn(Uni.createFrom().voidItem());
        when(degradationManager.isEventProcessingDegraded())
            .thenReturn(true);

        // When
        Uni<Boolean> result = resilientEventService.checkKafkaHealth();

        // Then
        UniAssertSubscriber<Boolean> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted()
                 .assertItem(true);

        // Verify recovery handling
        verify(degradationManager).clearEventProcessingDegradation("Kafka health check successful");
        verify(walletMetrics).recordDegradationEvent("EVENT_PROCESSING", "RECOVERED", "Health check passed");
        verify(walletMetrics).recordEventPublished("KAFKA_HEALTH_CHECK_SUCCESS");
    }

    @Test
    @DisplayName("Should handle Kafka health check failure")
    void shouldHandleKafkaHealthCheckFailure() {
        // Given
        when(kafkaEmitter.send("HEALTH_CHECK"))
            .thenReturn(Uni.createFrom().failure(new RuntimeException("Health check failed")));

        // When
        Uni<Boolean> result = resilientEventService.checkKafkaHealth();

        // Then
        UniAssertSubscriber<Boolean> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertCompleted()
                 .assertItem(false);

        verify(walletMetrics).recordEventPublished("KAFKA_HEALTH_CHECK_FAILURE");
        verify(degradationManager, never()).clearEventProcessingDegradation(anyString());
    }

    // ============================================================================
    // FINANCIAL COMPLIANCE TESTS
    // ============================================================================

    @Test
    @DisplayName("Should ensure zero event loss during Kafka outage")
    void shouldEnsureZeroEventLossDuringKafkaOutage() {
        // Given - Kafka is completely down
        RuntimeException kafkaError = new RuntimeException("Kafka cluster unreachable");
        when(kafkaEmitter.send(anyString()))
            .thenReturn(Uni.createFrom().failure(kafkaError));

        // When - Multiple financial events occur
        String walletId = "wallet-123";
        Uni<Void> createEvent = resilientEventService.publishWalletCreatedEvent(walletId, "user-123");
        Uni<Void> depositEvent = resilientEventService.publishFundsDepositedEvent(
            walletId, BigDecimal.valueOf(100), "dep-123", "Deposit");
        Uni<Void> withdrawEvent = resilientEventService.publishFundsWithdrawnEvent(
            walletId, BigDecimal.valueOf(50), "wd-123", "Withdrawal");

        // Then - All events should be preserved in outbox
        UniAssertSubscriber<Void> createSubscriber = createEvent.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        UniAssertSubscriber<Void> depositSubscriber = depositEvent.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        UniAssertSubscriber<Void> withdrawSubscriber = withdrawEvent.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        createSubscriber.assertCompleted();
        depositSubscriber.assertCompleted();
        withdrawSubscriber.assertCompleted();

        // Verify all events stored in outbox (zero loss)
        verify(outboxEventService).storeWalletEvent(eq(walletId), eq("WalletCreated"), any());
        verify(outboxEventService).storeWalletEvent(eq(walletId), eq("FundsDeposited"), any());
        verify(outboxEventService).storeWalletEvent(eq(walletId), eq("FundsWithdrawn"), any());
        
        // Verify degradation mode activated
        verify(degradationManager, atLeast(1)).setEventProcessingDegraded(anyString());
    }

    @Test
    @DisplayName("Should handle critical outbox failure gracefully")
    void shouldHandleCriticalOutboxFailureGracefully() {
        // Given - Both Kafka and outbox fail (critical scenario)
        RuntimeException kafkaError = new RuntimeException("Kafka down");
        RuntimeException outboxError = new RuntimeException("Database connection lost");
        
        when(kafkaEmitter.send(anyString()))
            .thenReturn(Uni.createFrom().failure(kafkaError));
        when(outboxEventService.storeWalletEvent(anyString(), anyString(), any()))
            .thenReturn(Uni.createFrom().failure(outboxError));

        // When
        Uni<Void> result = resilientEventService.publishWalletCreatedEvent("wallet-123", "user-123");

        // Then - Should fail (this is a critical system failure)
        UniAssertSubscriber<Void> subscriber = result.subscribe()
            .withSubscriber(UniAssertSubscriber.create());
        
        subscriber.assertFailedWith(RuntimeException.class);

        // Verify critical failure metrics
        verify(walletMetrics).incrementFailedOperations("outbox_store_critical");
        verify(walletMetrics).recordDatabaseError("outbox_fallback", outboxError);
    }

    // ============================================================================
    // UTILITY METHODS AND CLASSES
    // ============================================================================

    private OutboxEvent createMockOutboxEvent() {
        OutboxEvent event = new OutboxEvent();
        event.id = "event-123";
        event.aggregateId = "wallet-123";
        event.eventType = "TEST_EVENT";
        event.eventData = "{}";
        return event;
    }

    private record TestEventData(String data) {}
}
