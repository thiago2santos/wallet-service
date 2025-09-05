package com.wallet.infrastructure.resilience;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.logging.Log;

import com.wallet.infrastructure.metrics.WalletMetrics;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Graceful Degradation Service
 * 
 * Coordinates all degradation strategies to ensure the wallet service
 * continues operating even when dependencies fail. This service implements
 * the "graceful degradation" pattern where the system provides reduced
 * functionality rather than complete failure.
 * 
 * Degradation Strategies:
 * 1. Read-Only Mode: When primary DB fails, allow queries but block writes
 * 2. Cache Bypass: When Redis fails, use database directly (slower but functional)
 * 3. Async Event Processing: When Kafka fails, queue events for later processing
 * 4. Performance Warnings: Alert users when response times are degraded
 */
@ApplicationScoped
public class GracefulDegradationService {

    @Inject
    ReadOnlyModeManager readOnlyModeManager;

    @Inject
    WalletMetrics walletMetrics;

    // Cache bypass state
    private final AtomicBoolean cacheBypassMode = new AtomicBoolean(false);
    private final AtomicReference<Instant> cacheBypassStartTime = new AtomicReference<>();
    private final AtomicReference<String> cacheBypassReason = new AtomicReference<>();

    // Performance degradation state
    private final AtomicBoolean performanceDegraded = new AtomicBoolean(false);
    private final AtomicReference<Instant> performanceDegradationStartTime = new AtomicReference<>();
    private final AtomicReference<String> performanceDegradationReason = new AtomicReference<>();

    // Event processing degradation state
    private final AtomicBoolean eventProcessingDegraded = new AtomicBoolean(false);
    private final AtomicReference<Instant> eventDegradationStartTime = new AtomicReference<>();

    // ============================================================================
    // CACHE BYPASS DEGRADATION
    // ============================================================================

    /**
     * Enter cache bypass mode when Redis is unavailable
     */
    public void enterCacheBypassMode(String reason) {
        if (cacheBypassMode.compareAndSet(false, true)) {
            cacheBypassStartTime.set(Instant.now());
            cacheBypassReason.set(reason);
            
            Log.warnf("🔶 ENTERING CACHE BYPASS MODE: %s", reason);
            Log.infof("⚠️  Response times may be 2-3x slower (database direct access)");
            Log.infof("✅ All wallet operations continue to work");
            
            walletMetrics.recordDegradationEvent("CACHE_BYPASS", "ACTIVATED", reason);
            walletMetrics.incrementDegradationActivations("cache_bypass");
        }
    }

    /**
     * Exit cache bypass mode when Redis is restored
     */
    public void exitCacheBypassMode(String reason) {
        if (cacheBypassMode.compareAndSet(true, false)) {
            Instant startTime = cacheBypassStartTime.getAndSet(null);
            long durationMs = startTime != null ? 
                Instant.now().toEpochMilli() - startTime.toEpochMilli() : 0;
            
            Log.infof("🟢 EXITING CACHE BYPASS MODE: %s (Duration: %d ms)", reason, durationMs);
            Log.infof("✅ Cache restored - response times back to normal");
            
            walletMetrics.recordDegradationEvent("CACHE_BYPASS", "DEACTIVATED", reason);
            walletMetrics.recordDegradationDuration("cache_bypass", durationMs);
            
            cacheBypassReason.set(null);
        }
    }

    /**
     * Check if system is in cache bypass mode
     */
    public boolean isCacheBypassMode() {
        return cacheBypassMode.get();
    }

    // ============================================================================
    // PERFORMANCE DEGRADATION
    // ============================================================================

    /**
     * Enter performance degradation mode
     */
    public void enterPerformanceDegradation(String reason) {
        if (performanceDegraded.compareAndSet(false, true)) {
            performanceDegradationStartTime.set(Instant.now());
            performanceDegradationReason.set(reason);
            
            Log.warnf("🔶 PERFORMANCE DEGRADED: %s", reason);
            Log.infof("⚠️  Response times may be slower than normal");
            
            walletMetrics.recordDegradationEvent("PERFORMANCE_DEGRADED", "ACTIVATED", reason);
            walletMetrics.incrementDegradationActivations("performance_degradation");
        }
    }

    /**
     * Exit performance degradation mode
     */
    public void exitPerformanceDegradation(String reason) {
        if (performanceDegraded.compareAndSet(true, false)) {
            Instant startTime = performanceDegradationStartTime.getAndSet(null);
            long durationMs = startTime != null ? 
                Instant.now().toEpochMilli() - startTime.toEpochMilli() : 0;
            
            Log.infof("🟢 PERFORMANCE RESTORED: %s (Duration: %d ms)", reason, durationMs);
            
            walletMetrics.recordDegradationEvent("PERFORMANCE_DEGRADED", "DEACTIVATED", reason);
            walletMetrics.recordDegradationDuration("performance_degradation", durationMs);
            
            performanceDegradationReason.set(null);
        }
    }

    /**
     * Check if system performance is degraded
     */
    public boolean isPerformanceDegraded() {
        return performanceDegraded.get();
    }

    // ============================================================================
    // EVENT PROCESSING DEGRADATION
    // ============================================================================

    /**
     * Enter event processing degradation mode when Kafka is unavailable
     */
    public void enterEventProcessingDegradation(String reason) {
        if (eventProcessingDegraded.compareAndSet(false, true)) {
            eventDegradationStartTime.set(Instant.now());
            
            Log.warnf("🔶 EVENT PROCESSING DEGRADED: %s", reason);
            Log.infof("⚠️  Events stored in outbox table for later processing");
            Log.infof("✅ All wallet operations continue to work");
            
            walletMetrics.recordDegradationEvent("EVENT_PROCESSING_DEGRADED", "ACTIVATED", reason);
            walletMetrics.incrementDegradationActivations("event_processing_degradation");
        }
    }

    /**
     * Exit event processing degradation mode when Kafka is restored
     */
    public void exitEventProcessingDegradation(String reason) {
        if (eventProcessingDegraded.compareAndSet(true, false)) {
            Instant startTime = eventDegradationStartTime.getAndSet(null);
            long durationMs = startTime != null ? 
                Instant.now().toEpochMilli() - startTime.toEpochMilli() : 0;
            
            Log.infof("🟢 EVENT PROCESSING RESTORED: %s (Duration: %d ms)", reason, durationMs);
            Log.infof("✅ Outbox events will be processed and published to Kafka");
            
            walletMetrics.recordDegradationEvent("EVENT_PROCESSING_DEGRADED", "DEACTIVATED", reason);
            walletMetrics.recordDegradationDuration("event_processing_degradation", durationMs);
        }
    }

    /**
     * Check if event processing is degraded
     */
    public boolean isEventProcessingDegraded() {
        return eventProcessingDegraded.get();
    }

    // ============================================================================
    // OVERALL SYSTEM STATUS
    // ============================================================================

    /**
     * Get overall system degradation status
     */
    public SystemDegradationStatus getSystemStatus() {
        return new SystemDegradationStatus(
            readOnlyModeManager.isReadOnlyMode(),
            isCacheBypassMode(),
            isPerformanceDegraded(),
            isEventProcessingDegraded(),
            calculateOverallHealthScore(),
            getSystemStatusMessage()
        );
    }

    /**
     * Calculate overall system health score (0-100)
     */
    public int calculateOverallHealthScore() {
        int score = 100;
        
        if (readOnlyModeManager.isReadOnlyMode()) {
            score -= 40; // Major impact - write operations disabled
        }
        
        if (isCacheBypassMode()) {
            score -= 20; // Moderate impact - slower responses
        }
        
        if (isPerformanceDegraded()) {
            score -= 15; // Moderate impact - general slowness
        }
        
        if (isEventProcessingDegraded()) {
            score -= 10; // Minor impact - events delayed but not lost
        }
        
        return Math.max(score, 0);
    }

    /**
     * Get human-readable system status message
     */
    public String getSystemStatusMessage() {
        if (readOnlyModeManager.isReadOnlyMode()) {
            return "System in read-only mode - balance queries available, transactions disabled";
        }
        
        if (isCacheBypassMode() && isEventProcessingDegraded()) {
            return "System degraded - slower responses and delayed event processing";
        }
        
        if (isCacheBypassMode()) {
            return "System degraded - slower response times due to cache bypass";
        }
        
        if (isEventProcessingDegraded()) {
            return "System degraded - events queued for later processing";
        }
        
        if (isPerformanceDegraded()) {
            return "System degraded - slower response times";
        }
        
        return "System operating normally";
    }

    /**
     * Check if any degradation is active
     */
    public boolean isAnyDegradationActive() {
        return readOnlyModeManager.isReadOnlyMode() || 
               isCacheBypassMode() || 
               isPerformanceDegraded() || 
               isEventProcessingDegraded();
    }

    // ============================================================================
    // RESPONSE TIME MONITORING
    // ============================================================================

    /**
     * Record response time and trigger degradation if needed
     */
    public void recordResponseTime(String operation, long responseTimeMs) {
        // Record performance degradation metrics
        walletMetrics.recordDegradedPerformance(operation + "_response_time_" + responseTimeMs + "ms");
        
        // Trigger performance degradation if response times are too high
        if (responseTimeMs > 1000 && !isPerformanceDegraded()) {
            enterPerformanceDegradation("High response time detected: " + responseTimeMs + "ms for " + operation);
        }
        
        // Exit performance degradation if response times are back to normal
        if (responseTimeMs < 200 && isPerformanceDegraded()) {
            exitPerformanceDegradation("Response times back to normal: " + responseTimeMs + "ms for " + operation);
        }
    }

    // ============================================================================
    // INNER CLASSES
    // ============================================================================

    /**
     * Overall system degradation status
     */
    public record SystemDegradationStatus(
        boolean readOnlyMode,
        boolean cacheBypassMode,
        boolean performanceDegraded,
        boolean eventProcessingDegraded,
        int healthScore,
        String statusMessage
    ) {
        public boolean isHealthy() {
            return healthScore >= 90;
        }
        
        public boolean isDegraded() {
            return healthScore < 90 && healthScore >= 50;
        }
        
        public boolean isCritical() {
            return healthScore < 50;
        }
    }
}
