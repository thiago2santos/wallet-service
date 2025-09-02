package com.wallet.security;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Security Configuration and RBAC permissions.
 */
@QuarkusTest
class SecurityConfigTest {

    @Inject
    SecurityConfig securityConfig;

    @Test
    void shouldGrantUserPermissionsToUserRole() {
        assertTrue(securityConfig.hasPermission(SecurityConfig.USER_ROLE, SecurityConfig.WALLET_READ));
        assertTrue(securityConfig.hasPermission(SecurityConfig.USER_ROLE, SecurityConfig.WALLET_DEPOSIT));
        assertTrue(securityConfig.hasPermission(SecurityConfig.USER_ROLE, SecurityConfig.WALLET_WITHDRAW));
        assertTrue(securityConfig.hasPermission(SecurityConfig.USER_ROLE, SecurityConfig.WALLET_TRANSFER));
    }

    @Test
    void shouldDenyAdminPermissionsToUserRole() {
        assertFalse(securityConfig.hasPermission(SecurityConfig.USER_ROLE, SecurityConfig.WALLET_WRITE));
        assertFalse(securityConfig.hasPermission(SecurityConfig.USER_ROLE, SecurityConfig.WALLET_FREEZE));
        assertFalse(securityConfig.hasPermission(SecurityConfig.USER_ROLE, SecurityConfig.WALLET_CLOSE));
        assertFalse(securityConfig.hasPermission(SecurityConfig.USER_ROLE, SecurityConfig.SYSTEM_METRICS));
    }

    @Test
    void shouldGrantAllPermissionsToAdminRole() {
        assertTrue(securityConfig.hasPermission(SecurityConfig.ADMIN_ROLE, SecurityConfig.WALLET_READ));
        assertTrue(securityConfig.hasPermission(SecurityConfig.ADMIN_ROLE, SecurityConfig.WALLET_WRITE));
        assertTrue(securityConfig.hasPermission(SecurityConfig.ADMIN_ROLE, SecurityConfig.WALLET_FREEZE));
        assertTrue(securityConfig.hasPermission(SecurityConfig.ADMIN_ROLE, SecurityConfig.WALLET_CLOSE));
        assertTrue(securityConfig.hasPermission(SecurityConfig.ADMIN_ROLE, SecurityConfig.SYSTEM_METRICS));
    }

    @Test
    void shouldDenyAllPermissionsToInvalidRole() {
        String invalidRole = "invalid-role";
        
        assertFalse(securityConfig.hasPermission(invalidRole, SecurityConfig.WALLET_READ));
        assertFalse(securityConfig.hasPermission(invalidRole, SecurityConfig.WALLET_DEPOSIT));
        assertFalse(securityConfig.hasPermission(invalidRole, SecurityConfig.WALLET_WITHDRAW));
        assertFalse(securityConfig.hasPermission(invalidRole, SecurityConfig.WALLET_TRANSFER));
        assertFalse(securityConfig.hasPermission(invalidRole, SecurityConfig.WALLET_WRITE));
        assertFalse(securityConfig.hasPermission(invalidRole, SecurityConfig.WALLET_FREEZE));
        assertFalse(securityConfig.hasPermission(invalidRole, SecurityConfig.WALLET_CLOSE));
        assertFalse(securityConfig.hasPermission(invalidRole, SecurityConfig.SYSTEM_METRICS));
    }

    @Test
    void shouldDenyPermissionsForNullRole() {
        assertFalse(securityConfig.hasPermission(null, SecurityConfig.WALLET_READ));
        assertFalse(securityConfig.hasPermission(null, SecurityConfig.SYSTEM_METRICS));
    }

    @Test
    void shouldDenyPermissionsForInvalidPermission() {
        assertFalse(securityConfig.hasPermission(SecurityConfig.USER_ROLE, "invalid:permission"));
        assertFalse(securityConfig.hasPermission(SecurityConfig.ADMIN_ROLE, "invalid:permission"));
    }

    @Test
    void shouldHaveCorrectRoleConstants() {
        assertEquals("user", SecurityConfig.USER_ROLE);
        assertEquals("admin", SecurityConfig.ADMIN_ROLE);
    }

    @Test
    void shouldHaveCorrectPermissionConstants() {
        assertEquals("wallet:read", SecurityConfig.WALLET_READ);
        assertEquals("wallet:deposit", SecurityConfig.WALLET_DEPOSIT);
        assertEquals("wallet:withdraw", SecurityConfig.WALLET_WITHDRAW);
        assertEquals("wallet:transfer", SecurityConfig.WALLET_TRANSFER);
        assertEquals("wallet:write", SecurityConfig.WALLET_WRITE);
        assertEquals("wallet:freeze", SecurityConfig.WALLET_FREEZE);
        assertEquals("wallet:close", SecurityConfig.WALLET_CLOSE);
        assertEquals("system:metrics", SecurityConfig.SYSTEM_METRICS);
    }
}
