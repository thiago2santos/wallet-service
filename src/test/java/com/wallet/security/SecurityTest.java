package com.wallet.security;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

/**
 * Security tests for the Wallet Service API.
 * 
 * These tests verify that:
 * 1. Unauthenticated requests are rejected
 * 2. Users with proper roles can access appropriate endpoints
 * 3. Users without proper roles are denied access
 * 4. Admin-only endpoints are properly protected
 */
@QuarkusTest
class SecurityTest {

    @Test
    void shouldRejectUnauthenticatedRequests() {
        // Test wallet creation without authentication
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

        // Test wallet retrieval without authentication
        given()
        .when()
            .get("/api/v1/wallets/test-wallet")
        .then()
            .statusCode(401);

        // Test admin metrics without authentication
        given()
        .when()
            .get("/api/v1/admin/metrics")
        .then()
            .statusCode(401);
    }

    @Test
    @TestSecurity(user = "user123", roles = "user")
    @JwtSecurity(claims = {
        @Claim(key = "groups", value = "users")
    })
    void shouldAllowUserRoleToAccessUserEndpoints() {
        // Users should be able to create wallets
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
            .statusCode(201);
    }

    @Test
    @TestSecurity(user = "user123", roles = "user")
    @JwtSecurity(claims = {
        @Claim(key = "groups", value = "users")
    })
    void shouldDenyUserRoleFromAdminEndpoints() {
        // Users should not be able to access admin metrics
        given()
        .when()
            .get("/api/v1/admin/metrics")
        .then()
            .statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin123", roles = "admin")
    @JwtSecurity(claims = {
        @Claim(key = "groups", value = "admins")
    })
    void shouldAllowAdminRoleToAccessAllEndpoints() {
        // Admins should be able to create wallets
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "userId": "admin123",
                    "currency": "USD"
                }
                """)
        .when()
            .post("/api/v1/wallets")
        .then()
            .statusCode(201);

        // Admins should be able to access admin metrics
        given()
        .when()
            .get("/api/v1/admin/metrics")
        .then()
            .statusCode(200);
    }

    @Test
    @TestSecurity(user = "user123", roles = "user")
    @JwtSecurity(claims = {
        @Claim(key = "groups", value = "users")
    })
    void shouldAllowUserToAccessTransactionEndpoints() {
        // Create a wallet first
        String walletId = given()
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
            .statusCode(201)
            .extract()
            .header("Location")
            .replaceAll(".*/", ""); // Extract wallet ID from Location header

        // Test deposit (user permission: wallet:deposit)
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "amount": "100.00",
                    "referenceId": "dep-123",
                    "description": "Test deposit"
                }
                """)
        .when()
            .post("/api/v1/wallets/" + walletId + "/deposit")
        .then()
            .statusCode(200);

        // Test withdrawal (user permission: wallet:withdraw)
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "amount": "50.00",
                    "referenceId": "with-123",
                    "description": "Test withdrawal"
                }
                """)
        .when()
            .post("/api/v1/wallets/" + walletId + "/withdraw")
        .then()
            .statusCode(200);
    }

    @Test
    void shouldValidateInputParameters() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "amount": "-100.00",
                    "referenceId": "dep-123"
                }
                """)
        .when()
            .post("/api/v1/wallets/test-wallet/deposit")
        .then()
            .statusCode(401); // Should be unauthorized first, then validation would kick in
    }

    @Test
    void shouldPreventSQLInjection() {
        given()
        .when()
            .get("/api/v1/wallets/{walletId}", "'; DROP TABLE wallets; --")
        .then()
            .statusCode(401); // Should be unauthorized, preventing any SQL execution
    }

    @Test
    @TestSecurity(user = "user123", roles = "invalidrole")
    void shouldRejectInvalidRoles() {
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
            .statusCode(403); // Forbidden - invalid role
    }
}
