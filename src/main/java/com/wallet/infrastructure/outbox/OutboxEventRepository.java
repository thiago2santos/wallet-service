package com.wallet.infrastructure.outbox;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Repository for OutboxEvent entities.
 * Provides methods for querying unprocessed events and managing outbox state.
 */
@ApplicationScoped
public class OutboxEventRepository implements PanacheRepository<OutboxEvent> {

    /**
     * Find all unprocessed events ordered by creation time
     * This is used by the outbox publisher to process events in order
     */
    public Uni<List<OutboxEvent>> findUnprocessedEvents() {
        return find("processedAt IS NULL ORDER BY createdAt ASC").list();
    }

    /**
     * Find unprocessed events with a limit for batch processing
     */
    public Uni<List<OutboxEvent>> findUnprocessedEvents(int limit) {
        return find("processedAt IS NULL ORDER BY createdAt ASC")
                .page(0, limit)
                .list();
    }

    /**
     * Find events for a specific aggregate
     */
    public Uni<List<OutboxEvent>> findByAggregateId(String aggregateId) {
        return find("aggregateId = ?1 ORDER BY createdAt ASC", aggregateId).list();
    }

    /**
     * Find events by type
     */
    public Uni<List<OutboxEvent>> findByEventType(String eventType) {
        return find("eventType = ?1 ORDER BY createdAt ASC", eventType).list();
    }

    /**
     * Count unprocessed events
     */
    public Uni<Long> countUnprocessedEvents() {
        return count("processedAt IS NULL");
    }
}
