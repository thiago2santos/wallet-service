package com.wallet.infrastructure.resilience;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple tests for Read-Only Mode Manager
 * 
 * Tests core functionality without complex assertions
 */
@QuarkusTest
class ReadOnlyModeManagerSimpleTest {

    @Inject
    ReadOnlyModeManager readOnlyModeManager;

    @BeforeEach
    void setUp() {
        // Ensure clean state before each test
        if (readOnlyModeManager.isReadOnlyMode()) {
            readOnlyModeManager.exitReadOnlyMode("Test cleanup");
        }
    }

    @Test
    @DisplayName("Should enter and exit read-only mode")
    void shouldEnterAndExitReadOnlyMode() {
        // Initially not in read-only mode
        assertFalse(readOnlyModeManager.isReadOnlyMode());

        // Enter read-only mode
        readOnlyModeManager.enterReadOnlyMode("Test failure");
        assertTrue(readOnlyModeManager.isReadOnlyMode());

        // Exit read-only mode
        readOnlyModeManager.exitReadOnlyMode("Test recovery");
        assertFalse(readOnlyModeManager.isReadOnlyMode());
    }

    @Test
    @DisplayName("Should block write operations in read-only mode")
    void shouldBlockWriteOperationsInReadOnlyMode() {
        // Enter read-only mode
        readOnlyModeManager.enterReadOnlyMode("Database failure");

        // Write operations should throw exception
        ServiceDegradedException exception = assertThrows(
            ServiceDegradedException.class,
            () -> readOnlyModeManager.validateWriteOperation("deposit")
        );

        assertTrue(exception.getMessage().contains("read-only mode"));
        assertEquals("READ_ONLY_MODE_ACTIVE", exception.getDegradationCode());
    }

    @Test
    @DisplayName("Should allow write operations when not in read-only mode")
    void shouldAllowWriteOperationsWhenNotInReadOnlyMode() {
        // Should not throw exception
        assertDoesNotThrow(() -> readOnlyModeManager.validateWriteOperation("deposit"));
    }

    @Test
    @DisplayName("Should track read-only mode duration")
    void shouldTrackReadOnlyModeDuration() throws InterruptedException {
        // Enter read-only mode
        readOnlyModeManager.enterReadOnlyMode("Duration test");
        
        // Wait a bit
        Thread.sleep(50);
        
        // Duration should be positive
        long duration = readOnlyModeManager.calculateReadOnlyModeDuration();
        assertTrue(duration > 0);
        
        // Exit read-only mode
        readOnlyModeManager.exitReadOnlyMode("Duration test complete");
        
        // Duration should be zero when not in read-only mode
        assertEquals(0, readOnlyModeManager.calculateReadOnlyModeDuration());
    }

    @Test
    @DisplayName("Should provide status information")
    void shouldProvideStatusInformation() {
        // Test healthy status
        var healthyStatus = readOnlyModeManager.getStatus();
        assertFalse(healthyStatus.active());
        assertNull(healthyStatus.reason());

        // Test degraded status
        readOnlyModeManager.enterReadOnlyMode("Status test");
        var degradedStatus = readOnlyModeManager.getStatus();
        assertTrue(degradedStatus.active());
        assertEquals("Status test", degradedStatus.reason());
        assertNotNull(degradedStatus.startTime());
        assertTrue(degradedStatus.durationMs() >= 0);
    }
}
