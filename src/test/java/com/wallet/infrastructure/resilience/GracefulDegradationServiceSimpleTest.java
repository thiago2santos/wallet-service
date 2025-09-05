package com.wallet.infrastructure.resilience;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple tests for Graceful Degradation Service
 * 
 * Tests core functionality without complex assertions
 */
@QuarkusTest
class GracefulDegradationServiceSimpleTest {

    @Inject
    GracefulDegradationService gracefulDegradationService;

    @Inject
    ReadOnlyModeManager readOnlyModeManager;

    @BeforeEach
    void setUp() {
        // Clean state
        if (readOnlyModeManager.isReadOnlyMode()) {
            readOnlyModeManager.exitReadOnlyMode("Test cleanup");
        }
        if (gracefulDegradationService.isCacheBypassMode()) {
            gracefulDegradationService.exitCacheBypassMode("Test cleanup");
        }
        if (gracefulDegradationService.isPerformanceDegraded()) {
            gracefulDegradationService.exitPerformanceDegradation("Test cleanup");
        }
        if (gracefulDegradationService.isEventProcessingDegraded()) {
            gracefulDegradationService.exitEventProcessingDegradation("Test cleanup");
        }
    }

    @Test
    @DisplayName("Should manage cache bypass mode")
    void shouldManageCacheBypassMode() {
        // Initially not in cache bypass mode
        assertFalse(gracefulDegradationService.isCacheBypassMode());

        // Enter cache bypass mode
        gracefulDegradationService.enterCacheBypassMode("Redis failure");
        assertTrue(gracefulDegradationService.isCacheBypassMode());

        // Exit cache bypass mode
        gracefulDegradationService.exitCacheBypassMode("Redis recovered");
        assertFalse(gracefulDegradationService.isCacheBypassMode());
    }

    @Test
    @DisplayName("Should manage performance degradation")
    void shouldManagePerformanceDegradation() {
        // Initially not degraded
        assertFalse(gracefulDegradationService.isPerformanceDegraded());

        // Enter performance degradation
        gracefulDegradationService.enterPerformanceDegradation("High latency");
        assertTrue(gracefulDegradationService.isPerformanceDegraded());

        // Exit performance degradation
        gracefulDegradationService.exitPerformanceDegradation("Performance restored");
        assertFalse(gracefulDegradationService.isPerformanceDegraded());
    }

    @Test
    @DisplayName("Should manage event processing degradation")
    void shouldManageEventProcessingDegradation() {
        // Initially not degraded
        assertFalse(gracefulDegradationService.isEventProcessingDegraded());

        // Enter event processing degradation
        gracefulDegradationService.enterEventProcessingDegradation("Kafka failure");
        assertTrue(gracefulDegradationService.isEventProcessingDegraded());

        // Exit event processing degradation
        gracefulDegradationService.exitEventProcessingDegradation("Kafka recovered");
        assertFalse(gracefulDegradationService.isEventProcessingDegraded());
    }

    @Test
    @DisplayName("Should calculate health score correctly")
    void shouldCalculateHealthScoreCorrectly() {
        // Healthy system
        assertEquals(100, gracefulDegradationService.calculateOverallHealthScore());

        // With cache bypass
        gracefulDegradationService.enterCacheBypassMode("Redis failure");
        assertEquals(80, gracefulDegradationService.calculateOverallHealthScore());

        // With read-only mode
        readOnlyModeManager.enterReadOnlyMode("Database failure");
        assertEquals(40, gracefulDegradationService.calculateOverallHealthScore()); // 80 - 40

        // Recovery
        readOnlyModeManager.exitReadOnlyMode("Database recovered");
        gracefulDegradationService.exitCacheBypassMode("Redis recovered");
        assertEquals(100, gracefulDegradationService.calculateOverallHealthScore());
    }

    @Test
    @DisplayName("Should detect any degradation")
    void shouldDetectAnyDegradation() {
        // Initially healthy
        assertFalse(gracefulDegradationService.isAnyDegradationActive());

        // With degradation
        gracefulDegradationService.enterCacheBypassMode("Redis failure");
        assertTrue(gracefulDegradationService.isAnyDegradationActive());

        // Recovery
        gracefulDegradationService.exitCacheBypassMode("Redis recovered");
        assertFalse(gracefulDegradationService.isAnyDegradationActive());
    }

    @Test
    @DisplayName("Should provide system status")
    void shouldProvideSystemStatus() {
        // Healthy status
        var healthyStatus = gracefulDegradationService.getSystemStatus();
        assertEquals(100, healthyStatus.healthScore());
        assertFalse(healthyStatus.readOnlyMode());
        assertFalse(healthyStatus.cacheBypassMode());
        assertTrue(healthyStatus.isHealthy());

        // Degraded status
        gracefulDegradationService.enterCacheBypassMode("Redis failure");
        var degradedStatus = gracefulDegradationService.getSystemStatus();
        assertEquals(80, degradedStatus.healthScore());
        assertTrue(degradedStatus.cacheBypassMode());
        assertTrue(degradedStatus.isDegraded());
        assertFalse(degradedStatus.isHealthy());
    }

    @Test
    @DisplayName("Should handle response time monitoring")
    void shouldHandleResponseTimeMonitoring() {
        // Initially not degraded
        assertFalse(gracefulDegradationService.isPerformanceDegraded());

        // High response time should trigger degradation
        gracefulDegradationService.recordResponseTime("test_operation", 1500);
        assertTrue(gracefulDegradationService.isPerformanceDegraded());

        // Low response time should clear degradation
        gracefulDegradationService.recordResponseTime("test_operation", 100);
        assertFalse(gracefulDegradationService.isPerformanceDegraded());
    }
}
