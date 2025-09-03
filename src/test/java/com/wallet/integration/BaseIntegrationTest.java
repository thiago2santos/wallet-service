package com.wallet.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for integration tests providing common utilities and setup.
 * Tests the full stack including database, Kafka, Redis, and HTTP endpoints.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    protected static final String API_BASE_PATH = "/api/v1/wallets";
    
    @BeforeEach
    void setUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    /**
     * Creates a wallet and returns the wallet ID from the Location header
     */
    protected String createWallet(String userId) {
        Map<String, Object> request = Map.of("userId", userId);
        
        Response response = RestAssured
            .given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post(API_BASE_PATH)
            .then()
                .statusCode(201)
                .extract().response();
                
        String location = response.getHeader("Location");
        return location.substring(location.lastIndexOf("/") + 1);
    }

    /**
     * Gets wallet details by ID
     */
    protected Response getWallet(String walletId) {
        return RestAssured
            .given()
            .when()
                .get(API_BASE_PATH + "/" + walletId)
            .then()
                .extract().response();
    }

    /**
     * Deposits funds to a wallet
     */
    protected Response depositFunds(String walletId, BigDecimal amount, String referenceId, String description) {
        Map<String, Object> request = new HashMap<>();
        request.put("amount", amount);
        request.put("referenceId", referenceId);
        if (description != null) {
            request.put("description", description);
        }
        
        return RestAssured
            .given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post(API_BASE_PATH + "/" + walletId + "/deposit")
            .then()
                .extract().response();
    }

    /**
     * Withdraws funds from a wallet
     */
    protected Response withdrawFunds(String walletId, BigDecimal amount, String referenceId, String description) {
        Map<String, Object> request = new HashMap<>();
        request.put("amount", amount);
        request.put("referenceId", referenceId);
        if (description != null) {
            request.put("description", description);
        }
        
        return RestAssured
            .given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post(API_BASE_PATH + "/" + walletId + "/withdraw")
            .then()
                .extract().response();
    }

    /**
     * Transfers funds between wallets
     */
    protected Response transferFunds(String sourceWalletId, String destinationWalletId, 
                                   BigDecimal amount, String referenceId, String description) {
        Map<String, Object> request = new HashMap<>();
        request.put("destinationWalletId", destinationWalletId);
        request.put("amount", amount);
        request.put("referenceId", referenceId);
        if (description != null) {
            request.put("description", description);
        }
        
        return RestAssured
            .given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post(API_BASE_PATH + "/" + sourceWalletId + "/transfer")
            .then()
                .extract().response();
    }

    /**
     * Gets historical balance for a wallet at a specific timestamp
     */
    protected Response getHistoricalBalance(String walletId, String timestamp) {
        return RestAssured
            .given()
                .queryParam("timestamp", timestamp)
            .when()
                .get(API_BASE_PATH + "/" + walletId + "/balance/historical")
            .then()
                .extract().response();
    }

    /**
     * Generates a unique reference ID for testing
     */
    protected String generateReferenceId(String prefix) {
        return prefix + "-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
    }

    /**
     * Generates a unique user ID for testing
     */
    protected String generateUserId(String prefix) {
        return prefix + "-user-" + System.currentTimeMillis();
    }
}
