package com.wallet.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.wallet.infrastructure.metrics.WalletMetrics;

/**
 * Service for managing outbox events.
 * This service provides a clean API for storing events in the outbox
 * as part of business transactions.
 */
@ApplicationScoped
public class OutboxEventService {

    @Inject
    OutboxEventRepository outboxRepository;

    @Inject
    ObjectMapper objectMapper;
    
    @Inject
    WalletMetrics metrics;

    /**
     * Store an event in the outbox as part of the current transaction.
     * This ensures the event will be published reliably.
     */
    public Uni<OutboxEvent> storeEvent(String aggregateType, String aggregateId, 
                                       String eventType, Object eventPayload) {
        try {
            String eventData = objectMapper.writeValueAsString(eventPayload);
            OutboxEvent outboxEvent = new OutboxEvent(aggregateType, aggregateId, eventType, eventData);
            
            return outboxRepository.persist(outboxEvent)
                    .onItem().invoke(() -> metrics.recordOutboxEventCreated())
                    .map(v -> outboxEvent);
        } catch (JsonProcessingException e) {
            return Uni.createFrom().failure(
                new RuntimeException("Failed to serialize event payload", e)
            );
        }
    }

    /**
     * Store a wallet event specifically
     */
    public Uni<OutboxEvent> storeWalletEvent(String walletId, String eventType, Object eventPayload) {
        return storeEvent("Wallet", walletId, eventType, eventPayload);
    }

    /**
     * Store a transaction event specifically
     */
    public Uni<OutboxEvent> storeTransactionEvent(String transactionId, String eventType, Object eventPayload) {
        return storeEvent("Transaction", transactionId, eventType, eventPayload);
    }
}
