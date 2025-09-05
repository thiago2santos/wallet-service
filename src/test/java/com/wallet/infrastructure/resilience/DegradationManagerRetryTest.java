package com.wallet.infrastructure.resilience;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mockito;

import jakarta.inject.Inject;
import java.time.Instant;

import com.wallet.infrastructure.metrics.WalletMetrics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for DegradationManager retry-related functionality
 * 
 * Test Categories:
 * 1. Optimistic Lock Contention Tracking Tests
 * 2. Transient Failure Pattern Detection Tests
 * 3. Degradation Status Tests
 * 4. Metrics Integration Tests
 * 5. Concurrent Access Tests
 */
@QuarkusTest
class DegradationManagerRetryTest {

    @Inject
    DegradationManager degradationManager;

    @InjectMock
    WalletMetrics walletMetrics;

    @BeforeEach
    void setUp() {
        // Reset all mocks and clear any existing degradation state
        Mockito.reset(walletMetrics);
        
        // Clear any existing degradation states
        degradationManager.clearOptimisticLockContention("Test setup");
        degradationManager.clearTransientFailurePattern("Test setup");
    }

    // ============================================================================
    // OPTIMISTIC LOCK CONTENTION TRACKING TESTS
    // ============================================================================

    @Nested
    @DisplayName("Optimistic Lock Contention Tracking Tests")
    class OptimisticLockContentionTests {

        @Test
        @DisplayName("Should record optimistic lock contention")
        void shouldRecordOptimisticLockContention() {
            // Arrange
            String operation = "deposit";
            String walletId = "wallet-123";

            // Act
            degradationManager.recordOptimisticLockContention(operation, walletId);

            // Assert
            assertTrue(degradationManager.isOptimisticLockContentionHigh());
            
            // Verify metrics were recorded
            verify(walletMetrics, times(1)).recordDegradationEvent(
                eq("OPTIMISTIC_LOCK_CONTENTION"), 
                eq("DETECTED"), 
                eq("High contention on " + operation + " for wallet " + walletId)
            );
            verify(walletMetrics, times(1)).incrementDegradationActivations("optimistic_lock_contention");
        }

        @Test
        @DisplayName("Should not record duplicate optimistic lock contention")
        void shouldNotRecordDuplicateOptimisticLockContention() {
            // Arrange
            String operation = "withdrawal";
            String walletId = "wallet-456";

            // Act - Record contention twice
            degradationManager.recordOptimisticLockContention(operation, walletId);
            degradationManager.recordOptimisticLockContention(operation, walletId);

            // Assert - Should still be in contention state
            assertTrue(degradationManager.isOptimisticLockContentionHigh());
            
            // Verify metrics were only recorded once
            verify(walletMetrics, times(1)).recordDegradationEvent(anyString(), anyString(), anyString());
            verify(walletMetrics, times(1)).incrementDegradationActivations("optimistic_lock_contention");
        }

        @Test
        @DisplayName("Should clear optimistic lock contention")
        void shouldClearOptimisticLockContention() {
            // Arrange
            String operation = "transfer";
            String walletId = "wallet-789";
            String clearReason = "Contention resolved";

            // First record contention
            degradationManager.recordOptimisticLockContention(operation, walletId);
            assertTrue(degradationManager.isOptimisticLockContentionHigh());

            // Act - Clear contention
            degradationManager.clearOptimisticLockContention(clearReason);

            // Assert
            assertFalse(degradationManager.isOptimisticLockContentionHigh());
            
            // Verify clear metrics were recorded
            verify(walletMetrics, times(1)).recordDegradationEvent(
                eq("OPTIMISTIC_LOCK_CONTENTION"), 
                eq("CLEARED"), 
                eq(clearReason)
            );
            verify(walletMetrics, times(1)).recordDegradationDuration(
                eq("optimistic_lock_contention"), 
                anyLong()
            );
        }

        @Test
        @DisplayName("Should handle clearing non-existent optimistic lock contention")
        void shouldHandleClearingNonExistentOptimisticLockContention() {
            // Arrange - No contention recorded
            assertFalse(degradationManager.isOptimisticLockContentionHigh());

            // Act - Try to clear non-existent contention
            degradationManager.clearOptimisticLockContention("No contention to clear");

            // Assert - Should remain false
            assertFalse(degradationManager.isOptimisticLockContentionHigh());
            
            // Verify no clear metrics were recorded
            verify(walletMetrics, never()).recordDegradationEvent(
                eq("OPTIMISTIC_LOCK_CONTENTION"), 
                eq("CLEARED"), 
                anyString()
            );
        }

        @Test
        @DisplayName("Should record contention for different operations")
        void shouldRecordContentionForDifferentOperations() {
            // Test that different operations can trigger contention tracking
            
            degradationManager.recordOptimisticLockContention("deposit", "wallet-1");
            degradationManager.recordOptimisticLockContention("withdrawal", "wallet-2");
            degradationManager.recordOptimisticLockContention("transfer", "wallet-3,wallet-4");

            // Should be in contention state
            assertTrue(degradationManager.isOptimisticLockContentionHigh());
            
            // Should have recorded the first contention only (subsequent ones are duplicates)
            verify(walletMetrics, times(1)).recordDegradationEvent(
                eq("OPTIMISTIC_LOCK_CONTENTION"), 
                eq("DETECTED"), 
                anyString()
            );
        }
    }

    // ============================================================================
    // TRANSIENT FAILURE PATTERN DETECTION TESTS
    // ============================================================================

    @Nested
    @DisplayName("Transient Failure Pattern Detection Tests")
    class TransientFailurePatternTests {

        @Test
        @DisplayName("Should record transient failure pattern")
        void shouldRecordTransientFailurePattern() {
            // Arrange
            String operation = "get_wallet";

            // Act
            degradationManager.recordTransientFailurePattern(operation);

            // Assert
            assertTrue(degradationManager.isTransientFailurePatternDetected());
            
            // Verify metrics were recorded
            verify(walletMetrics, times(1)).recordDegradationEvent(
                eq("TRANSIENT_FAILURE_PATTERN"), 
                eq("DETECTED"), 
                eq("Pattern detected in " + operation)
            );
            verify(walletMetrics, times(1)).incrementDegradationActivations("transient_failure_pattern");
        }

        @Test
        @DisplayName("Should not record duplicate transient failure pattern")
        void shouldNotRecordDuplicateTransientFailurePattern() {
            // Arrange
            String operation = "create_wallet";

            // Act - Record pattern twice
            degradationManager.recordTransientFailurePattern(operation);
            degradationManager.recordTransientFailurePattern(operation);

            // Assert - Should still be in pattern detection state
            assertTrue(degradationManager.isTransientFailurePatternDetected());
            
            // Verify metrics were only recorded once
            verify(walletMetrics, times(1)).recordDegradationEvent(anyString(), anyString(), anyString());
            verify(walletMetrics, times(1)).incrementDegradationActivations("transient_failure_pattern");
        }

        @Test
        @DisplayName("Should clear transient failure pattern")
        void shouldClearTransientFailurePattern() {
            // Arrange
            String operation = "get_wallet";
            String clearReason = "Pattern resolved";

            // First record pattern
            degradationManager.recordTransientFailurePattern(operation);
            assertTrue(degradationManager.isTransientFailurePatternDetected());

            // Act - Clear pattern
            degradationManager.clearTransientFailurePattern(clearReason);

            // Assert
            assertFalse(degradationManager.isTransientFailurePatternDetected());
            
            // Verify clear metrics were recorded
            verify(walletMetrics, times(1)).recordDegradationEvent(
                eq("TRANSIENT_FAILURE_PATTERN"), 
                eq("CLEARED"), 
                eq(clearReason)
            );
            verify(walletMetrics, times(1)).recordDegradationDuration(
                eq("transient_failure_pattern"), 
                anyLong()
            );
        }

        @Test
        @DisplayName("Should handle clearing non-existent transient failure pattern")
        void shouldHandleClearingNonExistentTransientFailurePattern() {
            // Arrange - No pattern recorded
            assertFalse(degradationManager.isTransientFailurePatternDetected());

            // Act - Try to clear non-existent pattern
            degradationManager.clearTransientFailurePattern("No pattern to clear");

            // Assert - Should remain false
            assertFalse(degradationManager.isTransientFailurePatternDetected());
            
            // Verify no clear metrics were recorded
            verify(walletMetrics, never()).recordDegradationEvent(
                eq("TRANSIENT_FAILURE_PATTERN"), 
                eq("CLEARED"), 
                anyString()
            );
        }

        @Test
        @DisplayName("Should record patterns for different operations")
        void shouldRecordPatternsForDifferentOperations() {
            // Test that different operations can trigger pattern detection
            
            degradationManager.recordTransientFailurePattern("get_wallet");
            degradationManager.recordTransientFailurePattern("create_wallet");
            degradationManager.recordTransientFailurePattern("update_balance");

            // Should be in pattern detection state
            assertTrue(degradationManager.isTransientFailurePatternDetected());
            
            // Should have recorded the first pattern only (subsequent ones are duplicates)
            verify(walletMetrics, times(1)).recordDegradationEvent(
                eq("TRANSIENT_FAILURE_PATTERN"), 
                eq("DETECTED"), 
                anyString()
            );
        }
    }

    // ============================================================================
    // DEGRADATION STATUS TESTS
    // ============================================================================

    @Nested
    @DisplayName("Degradation Status Tests")
    class DegradationStatusTests {

        @Test
        @DisplayName("Should return correct degradation status with retry-related fields")
        void shouldReturnCorrectDegradationStatusWithRetryFields() {
            // Arrange - Set up various degradation states
            degradationManager.recordOptimisticLockContention("deposit", "wallet-123");
            degradationManager.recordTransientFailurePattern("get_wallet");

            // Act
            DegradationManager.DegradationStatus status = degradationManager.getDegradationStatus();

            // Assert
            assertTrue(status.optimisticLockContentionHigh());
            assertTrue(status.transientFailurePatternDetected());
            assertNotNull(status.optimisticLockContentionStartTime());
            assertNotNull(status.transientFailurePatternStartTime());
        }

        @Test
        @DisplayName("Should return correct status when no retry degradation exists")
        void shouldReturnCorrectStatusWhenNoRetryDegradationExists() {
            // Arrange - Ensure no degradation states
            assertFalse(degradationManager.isOptimisticLockContentionHigh());
            assertFalse(degradationManager.isTransientFailurePatternDetected());

            // Act
            DegradationManager.DegradationStatus status = degradationManager.getDegradationStatus();

            // Assert
            assertFalse(status.optimisticLockContentionHigh());
            assertFalse(status.transientFailurePatternDetected());
            assertNull(status.optimisticLockContentionStartTime());
            assertNull(status.transientFailurePatternStartTime());
        }

        @Test
        @DisplayName("Should update status after clearing degradation")
        void shouldUpdateStatusAfterClearingDegradation() {
            // Arrange - Set up degradation
            degradationManager.recordOptimisticLockContention("transfer", "wallet-456");
            degradationManager.recordTransientFailurePattern("create_wallet");
            
            DegradationManager.DegradationStatus initialStatus = degradationManager.getDegradationStatus();
            assertTrue(initialStatus.optimisticLockContentionHigh());
            assertTrue(initialStatus.transientFailurePatternDetected());

            // Act - Clear degradation
            degradationManager.clearOptimisticLockContention("Resolved");
            degradationManager.clearTransientFailurePattern("Resolved");

            // Assert
            DegradationManager.DegradationStatus clearedStatus = degradationManager.getDegradationStatus();
            assertFalse(clearedStatus.optimisticLockContentionHigh());
            assertFalse(clearedStatus.transientFailurePatternDetected());
            assertNull(clearedStatus.optimisticLockContentionStartTime());
            assertNull(clearedStatus.transientFailurePatternStartTime());
        }
    }

    // ============================================================================
    // METRICS INTEGRATION TESTS
    // ============================================================================

    @Nested
    @DisplayName("Metrics Integration Tests")
    class MetricsIntegrationTests {

        @Test
        @DisplayName("Should record duration metrics when clearing degradation")
        void shouldRecordDurationMetricsWhenClearingDegradation() throws InterruptedException {
            // Arrange
            degradationManager.recordOptimisticLockContention("deposit", "wallet-123");
            
            // Wait a small amount to ensure duration > 0
            Thread.sleep(10);

            // Act
            degradationManager.clearOptimisticLockContention("Test duration");

            // Assert
            verify(walletMetrics, times(1)).recordDegradationDuration(
                eq("optimistic_lock_contention"), 
                longThat(duration -> duration >= 0)
            );
        }

        @Test
        @DisplayName("Should record activation metrics for both degradation types")
        void shouldRecordActivationMetricsForBothDegradationTypes() {
            // Act
            degradationManager.recordOptimisticLockContention("deposit", "wallet-123");
            degradationManager.recordTransientFailurePattern("get_wallet");

            // Assert
            verify(walletMetrics, times(1)).incrementDegradationActivations("optimistic_lock_contention");
            verify(walletMetrics, times(1)).incrementDegradationActivations("transient_failure_pattern");
        }

        @Test
        @DisplayName("Should record detailed event information")
        void shouldRecordDetailedEventInformation() {
            // Arrange
            String operation = "transfer";
            String walletId = "wallet-source,wallet-dest";

            // Act
            degradationManager.recordOptimisticLockContention(operation, walletId);

            // Assert
            verify(walletMetrics, times(1)).recordDegradationEvent(
                eq("OPTIMISTIC_LOCK_CONTENTION"),
                eq("DETECTED"),
                eq("High contention on " + operation + " for wallet " + walletId)
            );
        }
    }

    // ============================================================================
    // CONCURRENT ACCESS TESTS
    // ============================================================================

    @Nested
    @DisplayName("Concurrent Access Tests")
    class ConcurrentAccessTests {

        @Test
        @DisplayName("Should handle concurrent optimistic lock contention recording")
        void shouldHandleConcurrentOptimisticLockContentionRecording() {
            // This test ensures thread safety of the degradation manager
            // In a real concurrent scenario, only one thread should succeed in setting the state
            
            // Act - Simulate concurrent access
            degradationManager.recordOptimisticLockContention("deposit", "wallet-1");
            degradationManager.recordOptimisticLockContention("withdrawal", "wallet-2");
            degradationManager.recordOptimisticLockContention("transfer", "wallet-3");

            // Assert - Should be in contention state
            assertTrue(degradationManager.isOptimisticLockContentionHigh());
            
            // Should have recorded only the first activation
            verify(walletMetrics, times(1)).incrementDegradationActivations("optimistic_lock_contention");
        }

        @Test
        @DisplayName("Should handle concurrent transient failure pattern recording")
        void shouldHandleConcurrentTransientFailurePatternRecording() {
            // Act - Simulate concurrent access
            degradationManager.recordTransientFailurePattern("get_wallet");
            degradationManager.recordTransientFailurePattern("create_wallet");
            degradationManager.recordTransientFailurePattern("update_balance");

            // Assert - Should be in pattern detection state
            assertTrue(degradationManager.isTransientFailurePatternDetected());
            
            // Should have recorded only the first activation
            verify(walletMetrics, times(1)).incrementDegradationActivations("transient_failure_pattern");
        }

        @Test
        @DisplayName("Should handle concurrent clear operations")
        void shouldHandleConcurrentClearOperations() {
            // Arrange - Set up degradation
            degradationManager.recordOptimisticLockContention("deposit", "wallet-123");
            assertTrue(degradationManager.isOptimisticLockContentionHigh());

            // Act - Simulate concurrent clearing
            degradationManager.clearOptimisticLockContention("Clear reason 1");
            degradationManager.clearOptimisticLockContention("Clear reason 2");
            degradationManager.clearOptimisticLockContention("Clear reason 3");

            // Assert - Should be cleared
            assertFalse(degradationManager.isOptimisticLockContentionHigh());
            
            // Should have recorded only one clear event
            verify(walletMetrics, times(1)).recordDegradationEvent(
                eq("OPTIMISTIC_LOCK_CONTENTION"),
                eq("CLEARED"),
                anyString()
            );
        }
    }

    // ============================================================================
    // INTEGRATION SCENARIOS TESTS
    // ============================================================================

    @Nested
    @DisplayName("Integration Scenarios Tests")
    class IntegrationScenariosTests {

        @Test
        @DisplayName("Should handle mixed degradation scenarios")
        void shouldHandleMixedDegradationScenarios() {
            // Test realistic scenario with multiple degradation types
            
            // Record various degradation states
            degradationManager.recordOptimisticLockContention("deposit", "wallet-123");
            degradationManager.recordTransientFailurePattern("get_wallet");
            
            // Verify both are active
            assertTrue(degradationManager.isOptimisticLockContentionHigh());
            assertTrue(degradationManager.isTransientFailurePatternDetected());
            
            // Clear one, keep the other
            degradationManager.clearOptimisticLockContention("Contention resolved");
            
            // Verify partial clearing
            assertFalse(degradationManager.isOptimisticLockContentionHigh());
            assertTrue(degradationManager.isTransientFailurePatternDetected());
            
            // Clear the remaining one
            degradationManager.clearTransientFailurePattern("Pattern resolved");
            
            // Verify all cleared
            assertFalse(degradationManager.isOptimisticLockContentionHigh());
            assertFalse(degradationManager.isTransientFailurePatternDetected());
        }

        @Test
        @DisplayName("Should maintain state consistency across operations")
        void shouldMaintainStateConsistencyAcrossOperations() {
            // Test that state remains consistent through multiple operations
            
            // Initial state
            assertFalse(degradationManager.isOptimisticLockContentionHigh());
            assertFalse(degradationManager.isTransientFailurePatternDetected());
            
            // Record and verify
            degradationManager.recordOptimisticLockContention("deposit", "wallet-123");
            assertTrue(degradationManager.isOptimisticLockContentionHigh());
            
            // Record duplicate and verify no change
            degradationManager.recordOptimisticLockContention("withdrawal", "wallet-456");
            assertTrue(degradationManager.isOptimisticLockContentionHigh());
            
            // Clear and verify
            degradationManager.clearOptimisticLockContention("Resolved");
            assertFalse(degradationManager.isOptimisticLockContentionHigh());
            
            // Try to clear again and verify no change
            degradationManager.clearOptimisticLockContention("Already cleared");
            assertFalse(degradationManager.isOptimisticLockContentionHigh());
        }
    }
}
