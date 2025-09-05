package com.wallet.infrastructure.resilience;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.logging.Log;

import com.wallet.infrastructure.metrics.WalletMetrics;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Read-Only Mode Manager for Graceful Degradation
 * 
 * When the primary database fails, the system enters read-only mode:
 * - Balance queries continue using replica database
 * - Write operations (deposit/withdraw/transfer) are blocked
 * - Users receive clear messaging about temporary limitations
 * - System automatically recovers when primary database is restored
 * 
 * This ensures financial data integrity while maintaining service availability.
 */
@ApplicationScoped
public class ReadOnlyModeManager {

    @Inject
    WalletMetrics walletMetrics;

    private final AtomicBoolean readOnlyMode = new AtomicBoolean(false);
    private final AtomicReference<Instant> readOnlyModeStartTime = new AtomicReference<>();
    private final AtomicReference<String> readOnlyModeReason = new AtomicReference<>();

    // ============================================================================
    // READ-ONLY MODE MANAGEMENT
    // ============================================================================

    /**
     * Enter read-only mode due to primary database failure
     */
    public void enterReadOnlyMode(String reason) {
        if (readOnlyMode.compareAndSet(false, true)) {
            readOnlyModeStartTime.set(Instant.now());
            readOnlyModeReason.set(reason);
            
            Log.errorf("🔴 ENTERING READ-ONLY MODE: %s", reason);
            Log.warnf("⚠️  Write operations (deposit/withdraw/transfer) are temporarily disabled");
            Log.infof("✅ Balance queries continue using replica database");
            
            // Record metrics
            walletMetrics.recordDegradationEvent("READ_ONLY_MODE", "ACTIVATED", reason);
            walletMetrics.incrementDegradationActivations("read_only_mode");
            
            // TODO: Send alerts to operations team
            sendReadOnlyModeAlert(reason, true);
        }
    }

    /**
     * Exit read-only mode when primary database is restored
     */
    public void exitReadOnlyMode(String reason) {
        if (readOnlyMode.compareAndSet(true, false)) {
            Instant startTime = readOnlyModeStartTime.getAndSet(null);
            long durationMs = startTime != null ? 
                Instant.now().toEpochMilli() - startTime.toEpochMilli() : 0;
            
            Log.infof("🟢 EXITING READ-ONLY MODE: %s (Duration: %d ms)", reason, durationMs);
            Log.infof("✅ All wallet operations restored to normal");
            
            // Record metrics
            walletMetrics.recordDegradationEvent("READ_ONLY_MODE", "DEACTIVATED", reason);
            walletMetrics.recordDegradationDuration("read_only_mode", durationMs);
            
            readOnlyModeReason.set(null);
            
            // TODO: Send recovery alerts to operations team
            sendReadOnlyModeAlert(reason, false);
        }
    }

    /**
     * Check if system is in read-only mode
     */
    public boolean isReadOnlyMode() {
        return readOnlyMode.get();
    }

    /**
     * Get read-only mode reason
     */
    public String getReadOnlyModeReason() {
        return readOnlyModeReason.get();
    }

    /**
     * Get read-only mode start time
     */
    public Instant getReadOnlyModeStartTime() {
        return readOnlyModeStartTime.get();
    }

    /**
     * Validate that write operations are allowed
     * Throws exception if in read-only mode
     */
    public void validateWriteOperation() {
        if (isReadOnlyMode()) {
            String reason = getReadOnlyModeReason();
            throw new ServiceDegradedException(
                "Service is temporarily in read-only mode. " +
                "Balance queries are available, but transactions are disabled. " +
                "Reason: " + (reason != null ? reason : "Primary database unavailable"),
                "READ_ONLY_MODE_ACTIVE"
            );
        }
    }

    /**
     * Validate that write operations are allowed for a specific operation
     */
    public void validateWriteOperation(String operation) {
        if (isReadOnlyMode()) {
            String reason = getReadOnlyModeReason();
            walletMetrics.incrementFailedOperations("read_only_mode_" + operation);
            
            throw new ServiceDegradedException(
                String.format("Cannot perform %s: Service is in read-only mode. " +
                             "Balance queries are available. Reason: %s", 
                             operation, reason != null ? reason : "Primary database unavailable"),
                "READ_ONLY_MODE_ACTIVE"
            );
        }
    }

    // ============================================================================
    // HEALTH AND STATUS
    // ============================================================================

    /**
     * Get detailed read-only mode status
     */
    public ReadOnlyModeStatus getStatus() {
        return new ReadOnlyModeStatus(
            isReadOnlyMode(),
            getReadOnlyModeReason(),
            getReadOnlyModeStartTime(),
            calculateReadOnlyModeDuration()
        );
    }

    /**
     * Calculate how long the system has been in read-only mode
     */
    public long calculateReadOnlyModeDuration() {
        Instant startTime = getReadOnlyModeStartTime();
        if (startTime != null && isReadOnlyMode()) {
            return Instant.now().toEpochMilli() - startTime.toEpochMilli();
        }
        return 0;
    }

    /**
     * Check if read-only mode has been active for too long
     */
    public boolean isReadOnlyModeTooLong(long thresholdMs) {
        return isReadOnlyMode() && calculateReadOnlyModeDuration() > thresholdMs;
    }

    // ============================================================================
    // ALERTING (Placeholder for future implementation)
    // ============================================================================

    private void sendReadOnlyModeAlert(String reason, boolean entering) {
        // TODO: Implement alerting system integration
        // - Slack notifications
        // - PagerDuty alerts
        // - Email notifications
        // - SMS alerts for critical issues
        
        String action = entering ? "ENTERED" : "EXITED";
        Log.infof("📢 ALERT: System %s read-only mode - %s", action, reason);
    }

    // ============================================================================
    // INNER CLASSES
    // ============================================================================

    /**
     * Read-only mode status record
     */
    public record ReadOnlyModeStatus(
        boolean active,
        String reason,
        Instant startTime,
        long durationMs
    ) {
        public String getFormattedDuration() {
            if (durationMs < 1000) {
                return durationMs + "ms";
            } else if (durationMs < 60000) {
                return (durationMs / 1000) + "s";
            } else {
                return (durationMs / 60000) + "m " + ((durationMs % 60000) / 1000) + "s";
            }
        }
    }
}
