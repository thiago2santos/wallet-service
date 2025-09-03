package com.wallet.infrastructure.outbox;

import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import com.wallet.infrastructure.metrics.WalletMetrics;
import io.micrometer.core.instrument.Timer;

import java.util.List;

/**
 * OutboxPublisher processes events from the outbox table and publishes them to Kafka.
 * This implements the reliable event publishing part of the Transactional Outbox Pattern.
 * 
 * The publisher runs on a schedule to ensure eventual consistency and guaranteed delivery.
 */
@ApplicationScoped
public class OutboxPublisher {

    private static final int BATCH_SIZE = 100;

    @Inject
    OutboxEventRepository outboxRepository;

    @Inject
    @Channel("wallet-events")
    MutinyEmitter<String> eventEmitter;
    
    @Inject
    WalletMetrics metrics;

    /**
     * Scheduled method that processes unprocessed outbox events.
     * Runs every 5 seconds to ensure timely event publishing.
     */
    @Scheduled(every = "5s")
    public Uni<Void> publishPendingEvents() {
        Timer.Sample sample = metrics.startOutboxPublishingTimer();
        
        return outboxRepository.findUnprocessedEvents(BATCH_SIZE)
                .chain(this::publishEvents)
                .onItem().invoke(() -> metrics.recordOutboxPublishing(sample))
                .onFailure().invoke(throwable -> {
                    metrics.recordOutboxEventFailed();
                    metrics.recordOutboxPublishing(sample);
                });
    }

    /**
     * Publish a batch of events to Kafka
     */
    private Uni<Void> publishEvents(List<OutboxEvent> events) {
        if (events.isEmpty()) {
            return Uni.createFrom().voidItem();
        }

        // Record metrics instead of logging
        events.forEach(event -> metrics.recordOutboxEventPublished());

        return Uni.combine().all().unis(
            events.stream()
                .map(this::publishSingleEvent)
                .toList()
        ).discardItems();
    }

    /**
     * Publish a single event to Kafka and mark it as processed
     */
    private Uni<Void> publishSingleEvent(OutboxEvent event) {

        // Create the Kafka message with proper key for partitioning
        String kafkaKey = event.aggregateId;
        String kafkaMessage = createKafkaMessage(event);

        return eventEmitter.send(kafkaMessage)
                .chain(() -> markEventAsProcessed(event))
                .onFailure().invoke(throwable -> metrics.recordOutboxEventFailed());
    }

    /**
     * Create the Kafka message payload from the outbox event
     */
    private String createKafkaMessage(OutboxEvent event) {
        // For now, we'll send the raw event data
        // In a more sophisticated setup, you might want to wrap this in an envelope
        return event.eventData;
    }

    /**
     * Mark an event as processed in the database
     */
    private Uni<Void> markEventAsProcessed(OutboxEvent event) {
        event.markAsProcessed();
        return outboxRepository.persist(event).replaceWithVoid();
    }

    /**
     * Manual trigger for publishing events (useful for testing or manual operations)
     */
    public Uni<Integer> publishAllPendingEvents() {
        Timer.Sample sample = metrics.startOutboxPublishingTimer();
        
        return outboxRepository.findUnprocessedEvents()
                .chain(events -> {
                    int eventCount = events.size();
                    return publishEvents(events)
                            .map(v -> eventCount);
                })
                .onItem().invoke(() -> metrics.recordOutboxPublishing(sample))
                .onFailure().invoke(throwable -> {
                    metrics.recordOutboxEventFailed();
                    metrics.recordOutboxPublishing(sample);
                });
    }
}
