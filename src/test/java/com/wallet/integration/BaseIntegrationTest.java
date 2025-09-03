package com.wallet.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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
    
    /**
     * Waits for eventual consistency between primary and replica databases.
     * This is crucial for integration tests that involve read-after-write scenarios.
     * 
     * Industry Best Practice: Handle eventual consistency in distributed systems
     */
    protected void waitForEventualConsistency() {
        try {
            // Small delay to allow replication lag
            Thread.sleep(100); // 100ms is usually sufficient for local test environments
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for eventual consistency", e);
        }
    }
    
    /**
     * Waits for a condition to be met with timeout and retry logic.
     * Industry Best Practice: Implement proper retry mechanisms for flaky operations
     */
    protected <T> T waitForCondition(Supplier<T> condition, Duration timeout, String description) {
        long startTime = System.currentTimeMillis();
        long timeoutMs = timeout.toMillis();
        
        Exception lastException = null;
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                T result = condition.get();
                if (result != null) {
                    return result;
                }
            } catch (Exception e) {
                lastException = e;
            }
            
            try {
                Thread.sleep(50); // 50ms between retries
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for condition: " + description, e);
            }
        }
        
        throw new RuntimeException("Timeout waiting for condition: " + description + 
                                 " (waited " + timeout.toMillis() + "ms)", lastException);
    }
    
    /**
     * Waits for a wallet to have a specific balance, handling eventual consistency.
     * Industry Best Practice: Specific wait conditions for business logic
     */
    protected void waitForWalletBalance(String walletId, BigDecimal expectedBalance, Duration timeout) {
        waitForCondition(() -> {
            try {
                Response response = getWallet(walletId);
                if (response.statusCode() == 200) {
                    float actualBalance = response.jsonPath().getFloat("balance");
                    if (Math.abs(actualBalance - expectedBalance.floatValue()) < 0.01f) {
                        return true;
                    }
                }
                return null;
            } catch (Exception e) {
                return null;
            }
        }, timeout, "wallet " + walletId + " to have balance " + expectedBalance);
    }
}
