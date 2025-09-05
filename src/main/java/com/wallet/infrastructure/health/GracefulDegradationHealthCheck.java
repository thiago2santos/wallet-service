package com.wallet.infrastructure.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import com.wallet.infrastructure.resilience.GracefulDegradationService;
import com.wallet.infrastructure.resilience.ReadOnlyModeManager;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Health Check for Graceful Degradation Status
 * 
 * This health check provides visibility into the system's degradation state
 * for monitoring and alerting purposes. It reports the overall health score
 * and specific degradation modes that are currently active.
 * 
 * Health Levels:
 * - UP (90-100): System operating normally
 * - UP (50-89): System degraded but functional  
 * - DOWN (<50): System critically degraded
 */
@Readiness
@ApplicationScoped
public class GracefulDegradationHealthCheck implements HealthCheck {

    @Inject
    GracefulDegradationService gracefulDegradationService;

    @Inject
    ReadOnlyModeManager readOnlyModeManager;

    @Override
    public HealthCheckResponse call() {
        var systemStatus = gracefulDegradationService.getSystemStatus();
        var readOnlyStatus = readOnlyModeManager.getStatus();
        
        // Determine overall health based on degradation level
        boolean isUp = systemStatus.healthScore() >= 50; // Critical threshold
        
        var responseBuilder = HealthCheckResponse.named("Graceful Degradation Status")
                .status(isUp);
        
        // Add overall system metrics
        responseBuilder.withData("healthScore", systemStatus.healthScore());
        responseBuilder.withData("statusMessage", systemStatus.statusMessage());
        responseBuilder.withData("overallStatus", getOverallStatusText(systemStatus.healthScore()));
        
        // Add specific degradation states
        responseBuilder.withData("readOnlyMode", systemStatus.readOnlyMode());
        responseBuilder.withData("cacheBypassMode", systemStatus.cacheBypassMode());
        responseBuilder.withData("performanceDegraded", systemStatus.performanceDegraded());
        responseBuilder.withData("eventProcessingDegraded", systemStatus.eventProcessingDegraded());
        
        // Add read-only mode details if active
        if (readOnlyStatus.active()) {
            responseBuilder.withData("readOnlyReason", readOnlyStatus.reason());
            responseBuilder.withData("readOnlyDuration", readOnlyStatus.getFormattedDuration());
            responseBuilder.withData("readOnlyStartTime", readOnlyStatus.startTime().toString());
        }
        
        // Add degradation impact assessment
        responseBuilder.withData("impactAssessment", getImpactAssessment(systemStatus));
        
        return responseBuilder.build();
    }

    private String getOverallStatusText(int healthScore) {
        if (healthScore >= 90) {
            return "HEALTHY";
        } else if (healthScore >= 70) {
            return "DEGRADED_MINOR";
        } else if (healthScore >= 50) {
            return "DEGRADED_MAJOR";
        } else {
            return "CRITICAL";
        }
    }

    private String getImpactAssessment(GracefulDegradationService.SystemDegradationStatus status) {
        if (status.readOnlyMode()) {
            return "HIGH_IMPACT - Write operations disabled, balance queries available";
        } else if (status.cacheBypassMode() && status.eventProcessingDegraded()) {
            return "MEDIUM_IMPACT - Slower responses and delayed event processing";
        } else if (status.cacheBypassMode()) {
            return "LOW_IMPACT - Slower response times due to cache bypass";
        } else if (status.eventProcessingDegraded()) {
            return "LOW_IMPACT - Events queued for later processing";
        } else if (status.performanceDegraded()) {
            return "LOW_IMPACT - General performance degradation";
        } else {
            return "NO_IMPACT - System operating normally";
        }
    }
}
