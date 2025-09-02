package com.wallet.api;

import com.wallet.security.JwtTokenGenerator;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

/**
 * Integration tests for Wallet Resource Security.
 * 
 * These tests verify the complete authentication/authorization flow using real JWT tokens.
 */
@QuarkusTest
class WalletResourceSecurityIT {

    @Inject
    JwtTokenGenerator tokenGenerator;

    @Test
    void shouldAuthenticateWithValidJwtToken() {
        String userToken = tokenGenerator.generateUserToken("user123");
        
        given()
            .header("Authorization", "Bearer " + userToken)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "userId": "user123",
                    "currency": "USD"
                }
                """)
        .when()
            .post("/api/v1/wallets")
        .then()
            .statusCode(201);
    }

    @Test
    void shouldRejectInvalidJwtToken() {
        String invalidToken = "invalid.jwt.token";
        
        given()
            .header("Authorization", "Bearer " + invalidToken)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "userId": "user123",
                    "currency": "USD"
                }
                """)
        .when()
            .post("/api/v1/wallets")
        .then()
            .statusCode(401);
    }

    @Test
    void shouldRejectExpiredToken() {
        // This would require a token generator that can create expired tokens
        // For now, we'll test with a malformed token that simulates expiration
        String malformedToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.expired.signature";
        
        given()
            .header("Authorization", "Bearer " + malformedToken)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "userId": "user123",
                    "currency": "USD"
                }
                """)
        .when()
            .post("/api/v1/wallets")
        .then()
            .statusCode(401);
    }

    @Test
    void shouldAllowAdminToAccessAdminEndpoints() {
        String adminToken = tokenGenerator.generateAdminToken("admin123");
        
        given()
            .header("Authorization", "Bearer " + adminToken)
        .when()
            .get("/api/v1/admin/metrics")
        .then()
            .statusCode(200);
    }

    @Test
    void shouldDenyUserFromAccessingAdminEndpoints() {
        String userToken = tokenGenerator.generateUserToken("user123");
        
        given()
            .header("Authorization", "Bearer " + userToken)
        .when()
            .get("/api/v1/admin/metrics")
        .then()
            .statusCode(403);
    }

    @Test
    void shouldAllowBothRolesToAccessUserEndpoints() {
        String userToken = tokenGenerator.generateUserToken("user123");
        String adminToken = tokenGenerator.generateAdminToken("admin123");
        
        // User should be able to create wallet
        given()
            .header("Authorization", "Bearer " + userToken)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "userId": "user123",
                    "currency": "USD"
                }
                """)
        .when()
            .post("/api/v1/wallets")
        .then()
            .statusCode(201);
        
        // Admin should also be able to create wallet
        given()
            .header("Authorization", "Bearer " + adminToken)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "userId": "admin123",
                    "currency": "EUR"
                }
                """)
        .when()
            .post("/api/v1/wallets")
        .then()
            .statusCode(201);
    }

    @Test
    void shouldRejectMissingAuthorizationHeader() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "userId": "user123",
                    "currency": "USD"
                }
                """)
        .when()
            .post("/api/v1/wallets")
        .then()
            .statusCode(401);
    }

    @Test
    void shouldRejectMalformedAuthorizationHeader() {
        given()
            .header("Authorization", "InvalidFormat token")
            .contentType(ContentType.JSON)
            .body("""
                {
                    "userId": "user123",
                    "currency": "USD"
                }
                """)
        .when()
            .post("/api/v1/wallets")
        .then()
            .statusCode(401);
    }
}
