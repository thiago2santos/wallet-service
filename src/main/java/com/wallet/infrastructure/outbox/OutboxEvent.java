package com.wallet.infrastructure.outbox;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Outbox Event entity for implementing the Transactional Outbox Pattern.
 * This ensures reliable event publishing by storing events in the same 
 * database transaction as business data.
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent extends PanacheEntityBase {

    @Id
    @Column(name = "id", length = 36)
    public String id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    public String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 36)
    public String aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    public String eventType;

    @Column(name = "event_data", nullable = false, columnDefinition = "JSON")
    public String eventData;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "processed_at")
    public Instant processedAt;

    @Column(name = "version")
    public Integer version;

    // Default constructor for JPA
    public OutboxEvent() {
    }

    /**
     * Constructor for creating new outbox events
     */
    public OutboxEvent(String aggregateType, String aggregateId, String eventType, String eventData) {
        this.id = UUID.randomUUID().toString();
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.eventData = eventData;
        this.createdAt = Instant.now();
        this.version = 1;
        // processedAt is null until event is published
    }

    /**
     * Mark this event as processed
     */
    public void markAsProcessed() {
        this.processedAt = Instant.now();
    }

    /**
     * Check if this event has been processed
     */
    public boolean isProcessed() {
        return this.processedAt != null;
    }

    @Override
    public String toString() {
        return "OutboxEvent{" +
                "id='" + id + '\'' +
                ", aggregateType='" + aggregateType + '\'' +
                ", aggregateId='" + aggregateId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", createdAt=" + createdAt +
                ", processedAt=" + processedAt +
                '}';
    }
}
