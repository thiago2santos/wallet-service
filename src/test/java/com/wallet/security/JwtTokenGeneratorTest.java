package com.wallet.security;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JWT Token Generator functionality.
 */
@QuarkusTest
class JwtTokenGeneratorTest {

    @Inject
    JwtTokenGenerator tokenGenerator;

    @Test
    void shouldGenerateValidUserToken() {
        String token = tokenGenerator.generateUserToken("user123");
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.startsWith("eyJ")); // JWT tokens start with eyJ
        
        // Token should have 3 parts separated by dots
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    @Test
    void shouldGenerateValidAdminToken() {
        String token = tokenGenerator.generateAdminToken("admin123");
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.startsWith("eyJ"));
        
        // Token should have 3 parts separated by dots
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    @Test
    void shouldGenerateTokenWithCustomRoles() {
        Set<String> roles = Set.of("custom-role");
        Set<String> groups = Set.of("custom-group");
        
        String token = tokenGenerator.generateToken("custom-user", roles, groups);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.startsWith("eyJ"));
    }

    @Test
    void shouldGenerateDifferentTokensForDifferentUsers() {
        String userToken1 = tokenGenerator.generateUserToken("user1");
        String userToken2 = tokenGenerator.generateUserToken("user2");
        
        assertNotEquals(userToken1, userToken2);
    }

    @Test
    void shouldGenerateDifferentTokensForSameUserAtDifferentTimes() throws InterruptedException {
        String token1 = tokenGenerator.generateUserToken("user123");
        Thread.sleep(1000); // Wait 1 second to ensure different issued time
        String token2 = tokenGenerator.generateUserToken("user123");
        
        assertNotEquals(token1, token2);
    }
}
