package com.wallet.infrastructure.resilience;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;

import com.wallet.infrastructure.outbox.OutboxEventService;
import com.wallet.infrastructure.metrics.WalletMetrics;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;

/**
 * Resilient Event Service with Circuit Breaker Protection
 * 
 * This service wraps Kafka event publishing with circuit breakers to ensure
 * audit trail preservation during Kafka outages. For financial services,
 * losing audit events is unacceptable - all events must be preserved.
 * 
 * Fallback Strategy:
 * - Kafka available → Direct publishing for real-time audit trail
 * - Kafka unavailable → Store in outbox table for guaranteed delivery
 * - Outbox processor → Replays events when Kafka recovers
 * 
 * Financial Compliance:
 * - Zero event loss (regulatory requirement)
 * - Eventual consistency acceptable for audit trail
 * - All financial operations must be traceable
 */
@ApplicationScoped
public class ResilientEventService {

    @Inject
    @Channel("wallet-events")
    MutinyEmitter<String> kafkaEmitter;

    @Inject
    OutboxEventService outboxEventService;

    @Inject
    WalletMetrics walletMetrics;

    @Inject
    DegradationManager degradationManager;

    /**
     * Publish wallet event with circuit breaker protection
     * Fallback: Store in outbox table for guaranteed delivery
     */
    @CircuitBreaker
    @Fallback(fallbackMethod = "storeInOutboxFallback")
    public Uni<Void> publishWalletEvent(String walletId, String eventType, Object eventData) {
        String eventJson = serializeEvent(eventData);
        
        return kafkaEmitter.send(eventJson)
            .onItem().invoke(() -> {
                walletMetrics.recordEventPublished(eventType);
                walletMetrics.recordEventPublished("KAFKA_DIRECT");
            })
            .onFailure().invoke(throwable -> {
                walletMetrics.incrementFailedOperations("kafka_publish");
                walletMetrics.recordEventPublishFailure(eventType);
                walletMetrics.recordDatabaseError("kafka", throwable);
            });
    }

    /**
     * Publish wallet created event
     */
    @CircuitBreaker
    @Fallback(fallbackMethod = "storeWalletCreatedInOutboxFallback")
    public Uni<Void> publishWalletCreatedEvent(String walletId, String userId) {
        WalletCreatedEvent event = new WalletCreatedEvent(walletId, userId);
        String eventJson = serializeEvent(event);
        
        return kafkaEmitter.send(eventJson)
            .onItem().invoke(() -> {
                walletMetrics.recordEventPublished("WALLET_CREATED");
                walletMetrics.recordEventPublished("KAFKA_DIRECT");
            })
            .onFailure().invoke(throwable -> {
                walletMetrics.incrementFailedOperations("kafka_publish_wallet_created");
                walletMetrics.recordEventPublishFailure("WALLET_CREATED");
                walletMetrics.recordDatabaseError("kafka", throwable);
            });
    }

    /**
     * Publish funds deposited event
     */
    @CircuitBreaker
    @Fallback(fallbackMethod = "storeFundsDepositedInOutboxFallback")
    public Uni<Void> publishFundsDepositedEvent(String walletId, java.math.BigDecimal amount, 
                                               String referenceId, String description) {
        FundsDepositedEvent event = new FundsDepositedEvent(walletId, amount, referenceId, description);
        String eventJson = serializeEvent(event);
        
        return kafkaEmitter.send(eventJson)
            .onItem().invoke(() -> {
                walletMetrics.recordEventPublished("FUNDS_DEPOSITED");
                walletMetrics.recordEventPublished("KAFKA_DIRECT");
                walletMetrics.recordDepositAmount(amount);
            })
            .onFailure().invoke(throwable -> {
                walletMetrics.incrementFailedOperations("kafka_publish_funds_deposited");
                walletMetrics.recordEventPublishFailure("FUNDS_DEPOSITED");
                walletMetrics.recordDatabaseError("kafka", throwable);
            });
    }

    /**
     * Publish funds withdrawn event
     */
    @CircuitBreaker
    @Fallback(fallbackMethod = "storeFundsWithdrawnInOutboxFallback")
    public Uni<Void> publishFundsWithdrawnEvent(String walletId, java.math.BigDecimal amount, 
                                               String referenceId, String description) {
        FundsWithdrawnEvent event = new FundsWithdrawnEvent(walletId, amount, referenceId, description);
        String eventJson = serializeEvent(event);
        
        return kafkaEmitter.send(eventJson)
            .onItem().invoke(() -> {
                walletMetrics.recordEventPublished("FUNDS_WITHDRAWN");
                walletMetrics.recordEventPublished("KAFKA_DIRECT");
                walletMetrics.recordWithdrawalAmount(amount);
            })
            .onFailure().invoke(throwable -> {
                walletMetrics.incrementFailedOperations("kafka_publish_funds_withdrawn");
                walletMetrics.recordEventPublishFailure("FUNDS_WITHDRAWN");
                walletMetrics.recordDatabaseError("kafka", throwable);
            });
    }

    /**
     * Publish funds transferred event
     */
    @CircuitBreaker
    @Fallback(fallbackMethod = "storeFundsTransferredInOutboxFallback")
    public Uni<Void> publishFundsTransferredEvent(String sourceWalletId, String destinationWalletId,
                                                 java.math.BigDecimal amount, String referenceId, String description) {
        FundsTransferredEvent event = new FundsTransferredEvent(sourceWalletId, destinationWalletId, 
                                                               amount, referenceId, description);
        String eventJson = serializeEvent(event);
        
        return kafkaEmitter.send(eventJson)
            .onItem().invoke(() -> {
                walletMetrics.recordEventPublished("FUNDS_TRANSFERRED");
                walletMetrics.recordEventPublished("KAFKA_DIRECT");
                walletMetrics.recordTransferAmount(amount);
            })
            .onFailure().invoke(throwable -> {
                walletMetrics.incrementFailedOperations("kafka_publish_funds_transferred");
                walletMetrics.recordEventPublishFailure("FUNDS_TRANSFERRED");
                walletMetrics.recordDatabaseError("kafka", throwable);
            });
    }

    // ============================================================================
    // FALLBACK METHODS - OUTBOX PATTERN
    // ============================================================================

    /**
     * Generic fallback: Store event in outbox table
     * This ensures zero event loss during Kafka outages
     */
    public Uni<Void> storeInOutboxFallback(String walletId, String eventType, Object eventData) {
        walletMetrics.incrementCircuitBreakerActivations("kafka-events");
        walletMetrics.recordFallbackExecution("storeInOutbox", "kafka_circuit_breaker");
        
        // Enter event processing degradation mode
        degradationManager.setEventProcessingDegraded("Kafka circuit breaker activated");
        
        return outboxEventService.storeWalletEvent(walletId, eventType, eventData)
            .replaceWithVoid()
            .onItem().invoke(() -> {
                walletMetrics.recordEventPublished("OUTBOX_STORED");
                walletMetrics.recordDegradationEvent("EVENT_PROCESSING", "OUTBOX_FALLBACK", 
                    "Event stored in outbox due to Kafka unavailability");
            })
            .onFailure().invoke(throwable -> {
                // This is critical - if outbox fails, we have a serious problem
                walletMetrics.incrementFailedOperations("outbox_store_critical");
                walletMetrics.recordDatabaseError("outbox_fallback", throwable);
            });
    }

    /**
     * Fallback for wallet created event
     */
    public Uni<Void> storeWalletCreatedInOutboxFallback(String walletId, String userId) {
        walletMetrics.incrementCircuitBreakerActivations("kafka-events");
        walletMetrics.recordFallbackExecution("storeWalletCreatedInOutbox", "kafka_circuit_breaker");
        
        degradationManager.setEventProcessingDegraded("Kafka circuit breaker activated during wallet creation");
        
        WalletCreatedEvent event = new WalletCreatedEvent(walletId, userId);
        return outboxEventService.storeWalletEvent(walletId, "WalletCreated", event)
            .replaceWithVoid()
            .onItem().invoke(() -> {
                walletMetrics.recordEventPublished("WALLET_CREATED_OUTBOX");
                walletMetrics.recordEventPublished("OUTBOX_STORED");
            });
    }

    /**
     * Fallback for funds deposited event
     */
    public Uni<Void> storeFundsDepositedInOutboxFallback(String walletId, java.math.BigDecimal amount, 
                                                        String referenceId, String description) {
        walletMetrics.incrementCircuitBreakerActivations("kafka-events");
        walletMetrics.recordFallbackExecution("storeFundsDepositedInOutbox", "kafka_circuit_breaker");
        
        degradationManager.setEventProcessingDegraded("Kafka circuit breaker activated during deposit");
        
        FundsDepositedEvent event = new FundsDepositedEvent(walletId, amount, referenceId, description);
        return outboxEventService.storeWalletEvent(walletId, "FundsDeposited", event)
            .replaceWithVoid()
            .onItem().invoke(() -> {
                walletMetrics.recordEventPublished("FUNDS_DEPOSITED_OUTBOX");
                walletMetrics.recordEventPublished("OUTBOX_STORED");
                walletMetrics.recordDepositAmount(amount);
            });
    }

    /**
     * Fallback for funds withdrawn event
     */
    public Uni<Void> storeFundsWithdrawnInOutboxFallback(String walletId, java.math.BigDecimal amount, 
                                                        String referenceId, String description) {
        walletMetrics.incrementCircuitBreakerActivations("kafka-events");
        walletMetrics.recordFallbackExecution("storeFundsWithdrawnInOutbox", "kafka_circuit_breaker");
        
        degradationManager.setEventProcessingDegraded("Kafka circuit breaker activated during withdrawal");
        
        FundsWithdrawnEvent event = new FundsWithdrawnEvent(walletId, amount, referenceId, description);
        return outboxEventService.storeWalletEvent(walletId, "FundsWithdrawn", event)
            .replaceWithVoid()
            .onItem().invoke(() -> {
                walletMetrics.recordEventPublished("FUNDS_WITHDRAWN_OUTBOX");
                walletMetrics.recordEventPublished("OUTBOX_STORED");
                walletMetrics.recordWithdrawalAmount(amount);
            });
    }

    /**
     * Fallback for funds transferred event
     */
    public Uni<Void> storeFundsTransferredInOutboxFallback(String sourceWalletId, String destinationWalletId,
                                                          java.math.BigDecimal amount, String referenceId, String description) {
        walletMetrics.incrementCircuitBreakerActivations("kafka-events");
        walletMetrics.recordFallbackExecution("storeFundsTransferredInOutbox", "kafka_circuit_breaker");
        
        degradationManager.setEventProcessingDegraded("Kafka circuit breaker activated during transfer");
        
        FundsTransferredEvent event = new FundsTransferredEvent(sourceWalletId, destinationWalletId, 
                                                               amount, referenceId, description);
        return outboxEventService.storeWalletEvent(sourceWalletId, "FundsTransferred", event)
            .replaceWithVoid()
            .onItem().invoke(() -> {
                walletMetrics.recordEventPublished("FUNDS_TRANSFERRED_OUTBOX");
                walletMetrics.recordEventPublished("OUTBOX_STORED");
                walletMetrics.recordTransferAmount(amount);
            });
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    /**
     * Serialize event to JSON for Kafka publishing
     */
    private String serializeEvent(Object event) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event for Kafka", e);
        }
    }

    /**
     * Check Kafka health and update degradation state
     */
    public Uni<Boolean> checkKafkaHealth() {
        // Simple health check - try to send a ping message
        return kafkaEmitter.send("HEALTH_CHECK")
            .map(success -> {
                if (degradationManager.isEventProcessingDegraded()) {
                    // Kafka recovered, clear degradation
                    degradationManager.clearEventProcessingDegradation("Kafka health check successful");
                    walletMetrics.recordDegradationEvent("EVENT_PROCESSING", "RECOVERED", "Health check passed");
                }
                return true;
            })
            .onFailure().recoverWithItem(false)
            .onItem().invoke(healthy -> {
                if (healthy) {
                    walletMetrics.recordEventPublished("KAFKA_HEALTH_CHECK_SUCCESS");
                } else {
                    walletMetrics.recordEventPublished("KAFKA_HEALTH_CHECK_FAILURE");
                }
            });
    }

    // ============================================================================
    // EVENT CLASSES
    // ============================================================================

    public record WalletCreatedEvent(String walletId, String userId) {}
    public record FundsDepositedEvent(String walletId, java.math.BigDecimal amount, String referenceId, String description) {}
    public record FundsWithdrawnEvent(String walletId, java.math.BigDecimal amount, String referenceId, String description) {}
    public record FundsTransferredEvent(String sourceWalletId, String destinationWalletId, 
                                       java.math.BigDecimal amount, String referenceId, String description) {}
}
