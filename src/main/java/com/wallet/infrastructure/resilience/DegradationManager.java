package com.wallet.infrastructure.resilience;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.wallet.infrastructure.metrics.WalletMetrics;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Degradation Manager for Financial Wallet Service
 * 
 * Manages service degradation states to ensure graceful handling of failures
 * while maintaining financial service integrity and compliance requirements.
 * 
 * Degradation States:
 * - READ_ONLY_MODE: Primary database unavailable, only queries allowed
 * - CACHE_BYPASS: Redis unavailable, direct database queries
 * - EVENT_PROCESSING_DEGRADED: Kafka unavailable, events stored in outbox
 * - RATE_LIMITED: High load protection active
 */
@ApplicationScoped
public class DegradationManager {

    @Inject
    WalletMetrics walletMetrics;

    // Degradation state flags
    private final AtomicBoolean readOnlyMode = new AtomicBoolean(false);
    private final AtomicBoolean cacheBypassMode = new AtomicBoolean(false);
    private final AtomicBoolean eventProcessingDegraded = new AtomicBoolean(false);
    private final AtomicBoolean rateLimited = new AtomicBoolean(false);

    // Degradation timestamps for monitoring
    private final AtomicReference<Instant> readOnlyModeStartTime = new AtomicReference<>();
    private final AtomicReference<Instant> cacheBypassStartTime = new AtomicReference<>();
    private final AtomicReference<Instant> eventDegradationStartTime = new AtomicReference<>();

    // ============================================================================
    // READ-ONLY MODE MANAGEMENT
    // ============================================================================

    /**
     * Enter read-only mode when primary database is unavailable
     * This prevents data corruption and maintains service availability for queries
     */
    public void enterReadOnlyMode(String reason) {
        if (readOnlyMode.compareAndSet(false, true)) {
            readOnlyModeStartTime.set(Instant.now());
            
            Log.warnf("ENTERING READ-ONLY MODE: %s", reason);
            
            // Record metrics for monitoring and alerting
            walletMetrics.recordDegradationEvent("READ_ONLY_MODE", "ENTERED", reason);
            walletMetrics.incrementDegradationActivations("read_only_mode");
            
            // In production, this would trigger:
            // - Critical alerts to operations team
            // - Notification to business stakeholders
            // - Automatic incident creation
            // - Customer communication (maintenance page)
        }
    }

    /**
     * Exit read-only mode when primary database is recovered
     */
    public void exitReadOnlyMode(String reason) {
        if (readOnlyMode.compareAndSet(true, false)) {
            Instant startTime = readOnlyModeStartTime.getAndSet(null);
            long durationMs = startTime != null ? 
                Instant.now().toEpochMilli() - startTime.toEpochMilli() : 0;
            
            Log.infof("EXITING READ-ONLY MODE: %s (Duration: %d ms)", reason, durationMs);
            
            walletMetrics.recordDegradationEvent("READ_ONLY_MODE", "EXITED", reason);
            walletMetrics.recordDegradationDuration("read_only_mode", durationMs);
        }
    }

    /**
     * Check if service is in read-only mode
     */
    public boolean isReadOnlyMode() {
        return readOnlyMode.get();
    }

    /**
     * Validate that write operations are allowed
     * Throws exception if in read-only mode
     */
    public void validateWriteOperation() {
        if (isReadOnlyMode()) {
            throw new ServiceDegradedException(
                "Service is temporarily in read-only mode. Write operations are disabled.",
                "READ_ONLY_MODE"
            );
        }
    }

    // ============================================================================
    // CACHE BYPASS MODE MANAGEMENT
    // ============================================================================

    /**
     * Enter cache bypass mode when Redis is unavailable
     */
    public void enterCacheBypassMode(String reason) {
        if (cacheBypassMode.compareAndSet(false, true)) {
            cacheBypassStartTime.set(Instant.now());
            
            Log.warnf("ENTERING CACHE BYPASS MODE: %s", reason);
            
            walletMetrics.recordDegradationEvent("CACHE_BYPASS", "ENTERED", reason);
            walletMetrics.incrementDegradationActivations("cache_bypass");
        }
    }

    /**
     * Exit cache bypass mode when Redis is recovered
     */
    public void exitCacheBypassMode(String reason) {
        if (cacheBypassMode.compareAndSet(true, false)) {
            Instant startTime = cacheBypassStartTime.getAndSet(null);
            long durationMs = startTime != null ? 
                Instant.now().toEpochMilli() - startTime.toEpochMilli() : 0;
            
            Log.infof("EXITING CACHE BYPASS MODE: %s (Duration: %d ms)", reason, durationMs);
            
            walletMetrics.recordDegradationEvent("CACHE_BYPASS", "EXITED", reason);
            walletMetrics.recordDegradationDuration("cache_bypass", durationMs);
        }
    }

    /**
     * Check if cache bypass mode is active
     */
    public boolean isCacheBypassMode() {
        return cacheBypassMode.get();
    }

    // ============================================================================
    // EVENT PROCESSING DEGRADATION MANAGEMENT
    // ============================================================================

    /**
     * Set event processing as degraded when Kafka is unavailable
     */
    public void setEventProcessingDegraded(String reason) {
        if (eventProcessingDegraded.compareAndSet(false, true)) {
            eventDegradationStartTime.set(Instant.now());
            
            Log.warnf("EVENT PROCESSING DEGRADED: %s", reason);
            
            walletMetrics.recordDegradationEvent("EVENT_PROCESSING", "DEGRADED", reason);
            walletMetrics.incrementDegradationActivations("event_processing");
        }
    }

    /**
     * Clear event processing degradation when Kafka is recovered
     */
    public void clearEventProcessingDegradation(String reason) {
        if (eventProcessingDegraded.compareAndSet(true, false)) {
            Instant startTime = eventDegradationStartTime.getAndSet(null);
            long durationMs = startTime != null ? 
                Instant.now().toEpochMilli() - startTime.toEpochMilli() : 0;
            
            Log.infof("EVENT PROCESSING RECOVERED: %s (Duration: %d ms)", reason, durationMs);
            
            walletMetrics.recordDegradationEvent("EVENT_PROCESSING", "RECOVERED", reason);
            walletMetrics.recordDegradationDuration("event_processing", durationMs);
        }
    }

    /**
     * Check if event processing is degraded
     */
    public boolean isEventProcessingDegraded() {
        return eventProcessingDegraded.get();
    }

    // ============================================================================
    // RATE LIMITING MANAGEMENT
    // ============================================================================

    /**
     * Activate rate limiting during high load
     */
    public void activateRateLimiting(String reason) {
        if (rateLimited.compareAndSet(false, true)) {
            Log.warnf("RATE LIMITING ACTIVATED: %s", reason);
            
            walletMetrics.recordDegradationEvent("RATE_LIMITING", "ACTIVATED", reason);
            walletMetrics.incrementDegradationActivations("rate_limiting");
        }
    }

    /**
     * Deactivate rate limiting when load normalizes
     */
    public void deactivateRateLimiting(String reason) {
        if (rateLimited.compareAndSet(true, false)) {
            Log.infof("RATE LIMITING DEACTIVATED: %s", reason);
            
            walletMetrics.recordDegradationEvent("RATE_LIMITING", "DEACTIVATED", reason);
        }
    }

    /**
     * Check if rate limiting is active
     */
    public boolean isRateLimited() {
        return rateLimited.get();
    }

    // ============================================================================
    // OVERALL SERVICE HEALTH
    // ============================================================================

    /**
     * Get overall service health status
     */
    public ServiceHealthStatus getServiceHealth() {
        if (isReadOnlyMode()) {
            return ServiceHealthStatus.DEGRADED_READ_ONLY;
        }
        
        boolean hasAnyDegradation = isCacheBypassMode() || 
                                   isEventProcessingDegraded() || 
                                   isRateLimited();
        
        return hasAnyDegradation ? ServiceHealthStatus.DEGRADED : ServiceHealthStatus.HEALTHY;
    }

    /**
     * Get detailed degradation status for monitoring
     */
    public DegradationStatus getDegradationStatus() {
        return new DegradationStatus(
            isReadOnlyMode(),
            isCacheBypassMode(),
            isEventProcessingDegraded(),
            isRateLimited(),
            readOnlyModeStartTime.get(),
            cacheBypassStartTime.get(),
            eventDegradationStartTime.get()
        );
    }

    // ============================================================================
    // INNER CLASSES
    // ============================================================================

    public enum ServiceHealthStatus {
        HEALTHY,
        DEGRADED,
        DEGRADED_READ_ONLY
    }

    public record DegradationStatus(
        boolean readOnlyMode,
        boolean cacheBypassMode,
        boolean eventProcessingDegraded,
        boolean rateLimited,
        Instant readOnlyModeStartTime,
        Instant cacheBypassStartTime,
        Instant eventDegradationStartTime
    ) {}
}
