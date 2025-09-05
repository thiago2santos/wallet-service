package com.wallet.infrastructure.resilience;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import com.wallet.infrastructure.metrics.WalletMetrics;
import com.wallet.infrastructure.resilience.DegradationManager.ServiceHealthStatus;

/**
 * Comprehensive tests for DegradationManager
 * 
 * These tests validate:
 * - Service degradation state management
 * - Read-only mode activation and deactivation
 * - Cache bypass mode handling
 * - Event processing degradation tracking
 * - Rate limiting state management
 * - Overall service health assessment
 * - Metrics recording for monitoring
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Degradation Manager Tests")
class DegradationManagerTest {

    @Mock
    private WalletMetrics walletMetrics;

    @InjectMocks
    private DegradationManager degradationManager;

    @BeforeEach
    void setUp() {
        // Reset degradation manager state before each test
        degradationManager = new DegradationManager();
        degradationManager.walletMetrics = walletMetrics;
    }

    // ============================================================================
    // READ-ONLY MODE TESTS
    // ============================================================================

    @Test
    @DisplayName("Should enter read-only mode successfully")
    void shouldEnterReadOnlyModeSuccessfully() {
        // Given
        String reason = "Primary database unavailable";
        assertFalse(degradationManager.isReadOnlyMode());

        // When
        degradationManager.enterReadOnlyMode(reason);

        // Then
        assertTrue(degradationManager.isReadOnlyMode());
        
        // Verify metrics
        verify(walletMetrics).recordDegradationEvent("READ_ONLY_MODE", "ENTERED", reason);
        verify(walletMetrics).incrementDegradationActivations("read_only_mode");
    }

    @Test
    @DisplayName("Should exit read-only mode successfully")
    void shouldExitReadOnlyModeSuccessfully() {
        // Given
        String enterReason = "Database failure";
        String exitReason = "Database recovered";
        degradationManager.enterReadOnlyMode(enterReason);
        assertTrue(degradationManager.isReadOnlyMode());

        // When
        degradationManager.exitReadOnlyMode(exitReason);

        // Then
        assertFalse(degradationManager.isReadOnlyMode());
        
        // Verify metrics
        verify(walletMetrics).recordDegradationEvent("READ_ONLY_MODE", "EXITED", exitReason);
        verify(walletMetrics).recordDegradationDuration(eq("read_only_mode"), anyLong());
    }

    @Test
    @DisplayName("Should validate write operations when not in read-only mode")
    void shouldValidateWriteOperationsWhenNotInReadOnlyMode() {
        // Given
        assertFalse(degradationManager.isReadOnlyMode());

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> degradationManager.validateWriteOperation());
    }

    @Test
    @DisplayName("Should reject write operations when in read-only mode")
    void shouldRejectWriteOperationsWhenInReadOnlyMode() {
        // Given
        degradationManager.enterReadOnlyMode("Database failure");
        assertTrue(degradationManager.isReadOnlyMode());

        // When & Then
        ServiceDegradedException exception = assertThrows(
            ServiceDegradedException.class,
            () -> degradationManager.validateWriteOperation()
        );
        
        assertEquals("READ_ONLY_MODE", exception.getDegradationCode());
        assertTrue(exception.getMessage().contains("read-only mode"));
    }

    @Test
    @DisplayName("Should handle multiple read-only mode entries gracefully")
    void shouldHandleMultipleReadOnlyModeEntriesGracefully() {
        // Given
        String reason1 = "First failure";
        String reason2 = "Second failure";

        // When
        degradationManager.enterReadOnlyMode(reason1);
        degradationManager.enterReadOnlyMode(reason2); // Should be idempotent

        // Then
        assertTrue(degradationManager.isReadOnlyMode());
        
        // Verify only first entry recorded
        verify(walletMetrics, times(1)).recordDegradationEvent("READ_ONLY_MODE", "ENTERED", reason1);
        verify(walletMetrics, never()).recordDegradationEvent("READ_ONLY_MODE", "ENTERED", reason2);
    }

    // ============================================================================
    // CACHE BYPASS MODE TESTS
    // ============================================================================

    @Test
    @DisplayName("Should enter cache bypass mode successfully")
    void shouldEnterCacheBypassModeSuccessfully() {
        // Given
        String reason = "Redis connection timeout";
        assertFalse(degradationManager.isCacheBypassMode());

        // When
        degradationManager.enterCacheBypassMode(reason);

        // Then
        assertTrue(degradationManager.isCacheBypassMode());
        
        // Verify metrics
        verify(walletMetrics).recordDegradationEvent("CACHE_BYPASS", "ENTERED", reason);
        verify(walletMetrics).incrementDegradationActivations("cache_bypass");
    }

    @Test
    @DisplayName("Should exit cache bypass mode successfully")
    void shouldExitCacheBypassModeSuccessfully() {
        // Given
        String enterReason = "Redis failure";
        String exitReason = "Redis recovered";
        degradationManager.enterCacheBypassMode(enterReason);
        assertTrue(degradationManager.isCacheBypassMode());

        // When
        degradationManager.exitCacheBypassMode(exitReason);

        // Then
        assertFalse(degradationManager.isCacheBypassMode());
        
        // Verify metrics
        verify(walletMetrics).recordDegradationEvent("CACHE_BYPASS", "EXITED", exitReason);
        verify(walletMetrics).recordDegradationDuration(eq("cache_bypass"), anyLong());
    }

    // ============================================================================
    // EVENT PROCESSING DEGRADATION TESTS
    // ============================================================================

    @Test
    @DisplayName("Should set event processing as degraded successfully")
    void shouldSetEventProcessingAsDegradedSuccessfully() {
        // Given
        String reason = "Kafka cluster unavailable";
        assertFalse(degradationManager.isEventProcessingDegraded());

        // When
        degradationManager.setEventProcessingDegraded(reason);

        // Then
        assertTrue(degradationManager.isEventProcessingDegraded());
        
        // Verify metrics
        verify(walletMetrics).recordDegradationEvent("EVENT_PROCESSING", "DEGRADED", reason);
        verify(walletMetrics).incrementDegradationActivations("event_processing");
    }

    @Test
    @DisplayName("Should clear event processing degradation successfully")
    void shouldClearEventProcessingDegradationSuccessfully() {
        // Given
        String degradedReason = "Kafka failure";
        String recoveredReason = "Kafka recovered";
        degradationManager.setEventProcessingDegraded(degradedReason);
        assertTrue(degradationManager.isEventProcessingDegraded());

        // When
        degradationManager.clearEventProcessingDegradation(recoveredReason);

        // Then
        assertFalse(degradationManager.isEventProcessingDegraded());
        
        // Verify metrics
        verify(walletMetrics).recordDegradationEvent("EVENT_PROCESSING", "RECOVERED", recoveredReason);
        verify(walletMetrics).recordDegradationDuration(eq("event_processing"), anyLong());
    }

    // ============================================================================
    // RATE LIMITING TESTS
    // ============================================================================

    @Test
    @DisplayName("Should activate rate limiting successfully")
    void shouldActivateRateLimitingSuccessfully() {
        // Given
        String reason = "High load detected";
        assertFalse(degradationManager.isRateLimited());

        // When
        degradationManager.activateRateLimiting(reason);

        // Then
        assertTrue(degradationManager.isRateLimited());
        
        // Verify metrics
        verify(walletMetrics).recordDegradationEvent("RATE_LIMITING", "ACTIVATED", reason);
        verify(walletMetrics).incrementDegradationActivations("rate_limiting");
    }

    @Test
    @DisplayName("Should deactivate rate limiting successfully")
    void shouldDeactivateRateLimitingSuccessfully() {
        // Given
        String activatedReason = "Load spike";
        String deactivatedReason = "Load normalized";
        degradationManager.activateRateLimiting(activatedReason);
        assertTrue(degradationManager.isRateLimited());

        // When
        degradationManager.deactivateRateLimiting(deactivatedReason);

        // Then
        assertFalse(degradationManager.isRateLimited());
        
        // Verify metrics
        verify(walletMetrics).recordDegradationEvent("RATE_LIMITING", "DEACTIVATED", deactivatedReason);
    }

    // ============================================================================
    // SERVICE HEALTH STATUS TESTS
    // ============================================================================

    @Test
    @DisplayName("Should return healthy status when no degradations active")
    void shouldReturnHealthyStatusWhenNoDegradationsActive() {
        // Given - No degradations active
        assertFalse(degradationManager.isReadOnlyMode());
        assertFalse(degradationManager.isCacheBypassMode());
        assertFalse(degradationManager.isEventProcessingDegraded());
        assertFalse(degradationManager.isRateLimited());

        // When
        ServiceHealthStatus status = degradationManager.getServiceHealth();

        // Then
        assertEquals(ServiceHealthStatus.HEALTHY, status);
    }

    @Test
    @DisplayName("Should return degraded read-only status when in read-only mode")
    void shouldReturnDegradedReadOnlyStatusWhenInReadOnlyMode() {
        // Given
        degradationManager.enterReadOnlyMode("Database failure");

        // When
        ServiceHealthStatus status = degradationManager.getServiceHealth();

        // Then
        assertEquals(ServiceHealthStatus.DEGRADED_READ_ONLY, status);
    }

    @Test
    @DisplayName("Should return degraded status when cache bypass is active")
    void shouldReturnDegradedStatusWhenCacheBypassActive() {
        // Given
        degradationManager.enterCacheBypassMode("Redis failure");

        // When
        ServiceHealthStatus status = degradationManager.getServiceHealth();

        // Then
        assertEquals(ServiceHealthStatus.DEGRADED, status);
    }

    @Test
    @DisplayName("Should return degraded status when event processing is degraded")
    void shouldReturnDegradedStatusWhenEventProcessingDegraded() {
        // Given
        degradationManager.setEventProcessingDegraded("Kafka failure");

        // When
        ServiceHealthStatus status = degradationManager.getServiceHealth();

        // Then
        assertEquals(ServiceHealthStatus.DEGRADED, status);
    }

    @Test
    @DisplayName("Should return degraded status when rate limiting is active")
    void shouldReturnDegradedStatusWhenRateLimitingActive() {
        // Given
        degradationManager.activateRateLimiting("High load");

        // When
        ServiceHealthStatus status = degradationManager.getServiceHealth();

        // Then
        assertEquals(ServiceHealthStatus.DEGRADED, status);
    }

    @Test
    @DisplayName("Should prioritize read-only mode over other degradations")
    void shouldPrioritizeReadOnlyModeOverOtherDegradations() {
        // Given - Multiple degradations active
        degradationManager.enterReadOnlyMode("Database failure");
        degradationManager.enterCacheBypassMode("Redis failure");
        degradationManager.setEventProcessingDegraded("Kafka failure");
        degradationManager.activateRateLimiting("High load");

        // When
        ServiceHealthStatus status = degradationManager.getServiceHealth();

        // Then
        assertEquals(ServiceHealthStatus.DEGRADED_READ_ONLY, status);
    }

    // ============================================================================
    // DEGRADATION STATUS TESTS
    // ============================================================================

    @Test
    @DisplayName("Should return accurate degradation status")
    void shouldReturnAccurateDegradationStatus() {
        // Given
        degradationManager.enterCacheBypassMode("Redis failure");
        degradationManager.setEventProcessingDegraded("Kafka failure");

        // When
        var status = degradationManager.getDegradationStatus();

        // Then
        assertFalse(status.readOnlyMode());
        assertTrue(status.cacheBypassMode());
        assertTrue(status.eventProcessingDegraded());
        assertFalse(status.rateLimited());
        assertNotNull(status.cacheBypassStartTime());
        assertNotNull(status.eventDegradationStartTime());
        assertNull(status.readOnlyModeStartTime());
    }

    // ============================================================================
    // CONCURRENT ACCESS TESTS
    // ============================================================================

    @Test
    @DisplayName("Should handle concurrent degradation state changes safely")
    void shouldHandleConcurrentDegradationStateChangesSafely() {
        // Given - Simulate concurrent access
        String reason1 = "Concurrent failure 1";
        String reason2 = "Concurrent failure 2";

        // When - Multiple threads try to enter read-only mode
        degradationManager.enterReadOnlyMode(reason1);
        degradationManager.enterReadOnlyMode(reason2);
        degradationManager.enterReadOnlyMode(reason1); // Duplicate

        // Then - Should be in read-only mode
        assertTrue(degradationManager.isReadOnlyMode());
        
        // Verify only first entry was recorded
        verify(walletMetrics, times(1)).recordDegradationEvent("READ_ONLY_MODE", "ENTERED", reason1);
        verify(walletMetrics, never()).recordDegradationEvent("READ_ONLY_MODE", "ENTERED", reason2);
    }

    @Test
    @DisplayName("Should handle rapid state transitions correctly")
    void shouldHandleRapidStateTransitionsCorrectly() {
        // Given
        String enterReason = "Failure";
        String exitReason = "Recovery";

        // When - Rapid enter/exit cycles
        degradationManager.enterReadOnlyMode(enterReason);
        assertTrue(degradationManager.isReadOnlyMode());
        
        degradationManager.exitReadOnlyMode(exitReason);
        assertFalse(degradationManager.isReadOnlyMode());
        
        degradationManager.enterReadOnlyMode(enterReason);
        assertTrue(degradationManager.isReadOnlyMode());

        // Then - All transitions should be recorded
        verify(walletMetrics, times(2)).recordDegradationEvent("READ_ONLY_MODE", "ENTERED", enterReason);
        verify(walletMetrics, times(1)).recordDegradationEvent("READ_ONLY_MODE", "EXITED", exitReason);
    }

    // ============================================================================
    // EDGE CASES AND ERROR HANDLING
    // ============================================================================

    @Test
    @DisplayName("Should handle null reasons gracefully")
    void shouldHandleNullReasonsGracefully() {
        // When & Then - Should not throw exceptions
        assertDoesNotThrow(() -> degradationManager.enterReadOnlyMode(null));
        assertDoesNotThrow(() -> degradationManager.enterCacheBypassMode(null));
        assertDoesNotThrow(() -> degradationManager.setEventProcessingDegraded(null));
        assertDoesNotThrow(() -> degradationManager.activateRateLimiting(null));
        
        // Verify states were set despite null reasons
        assertTrue(degradationManager.isReadOnlyMode());
        assertTrue(degradationManager.isCacheBypassMode());
        assertTrue(degradationManager.isEventProcessingDegraded());
        assertTrue(degradationManager.isRateLimited());
    }

    @Test
    @DisplayName("Should handle exit operations when not in degraded state")
    void shouldHandleExitOperationsWhenNotInDegradedState() {
        // Given - Not in any degraded state
        assertFalse(degradationManager.isReadOnlyMode());
        assertFalse(degradationManager.isCacheBypassMode());

        // When & Then - Should not throw exceptions
        assertDoesNotThrow(() -> degradationManager.exitReadOnlyMode("Not in read-only mode"));
        assertDoesNotThrow(() -> degradationManager.exitCacheBypassMode("Not in cache bypass mode"));
        
        // Verify no metrics recorded for non-existent state changes
        verify(walletMetrics, never()).recordDegradationEvent(eq("READ_ONLY_MODE"), eq("EXITED"), anyString());
        verify(walletMetrics, never()).recordDegradationEvent(eq("CACHE_BYPASS"), eq("EXITED"), anyString());
    }
}
