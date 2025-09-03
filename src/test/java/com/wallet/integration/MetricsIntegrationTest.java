package com.wallet.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for custom business metrics.
 * Verifies that Prometheus metrics are properly recorded and exposed.
 */
@QuarkusTest
@DisplayName("Metrics Integration Tests")
public class MetricsIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should expose wallet creation metrics")
    void shouldExposeWalletCreationMetrics() {
        // Given - Get initial metrics
        Response initialMetrics = getMetrics();
        String initialMetricsText = initialMetrics.getBody().asString();
        
        // When - Create a wallet
        String userId = generateUserId("metrics-creation-test");
        String walletId = createWallet(userId);
        
        // Then - Verify metrics were updated
        Response updatedMetrics = getMetrics();
        updatedMetrics.then().statusCode(200);
        
        String metricsText = updatedMetrics.getBody().asString();
        
        // Check that wallet creation counter increased
        assertTrue(metricsText.contains("wallet_operations_created_total"), 
                  "Should contain wallet creation counter");
        assertTrue(metricsText.contains("wallet_total_count"), 
                  "Should contain total wallet count gauge");
        assertTrue(metricsText.contains("wallet_operations_creation_duration_seconds"), 
                  "Should contain creation duration timer");
    }

    @Test
    @DisplayName("Should track deposit metrics correctly")
    void shouldTrackDepositMetricsCorrectly() {
        // Given
        String userId = generateUserId("metrics-deposit-test");
        String walletId = createWallet(userId);
        
        // When - Perform deposit
        BigDecimal depositAmount = new BigDecimal("150.75");
        Response depositResponse = depositFunds(walletId, depositAmount, 
                                              generateReferenceId("metrics-deposit"), "Metrics test deposit");
        depositResponse.then().statusCode(200);
        
        // Then - Check metrics
        Response metrics = getMetrics();
        metrics.then().statusCode(200);
        
        String metricsText = metrics.getBody().asString();
        
        // Verify deposit metrics are present
        assertTrue(metricsText.contains("wallet_operations_deposits_total"), 
                  "Should contain deposit counter");
        assertTrue(metricsText.contains("wallet_operations_deposit_duration_seconds"), 
                  "Should contain deposit duration timer");
        assertTrue(metricsText.contains("wallet_money_deposited_total"), 
                  "Should contain money deposited counter");
    }

    @Test
    @DisplayName("Should track withdrawal metrics correctly")
    void shouldTrackWithdrawalMetricsCorrectly() {
        // Given
        String userId = generateUserId("metrics-withdrawal-test");
        String walletId = createWallet(userId);
        
        // Setup funds for withdrawal
        depositFunds(walletId, new BigDecimal("200.00"), 
                   generateReferenceId("metrics-setup"), "Setup for withdrawal test");
        
        // When - Perform withdrawal
        BigDecimal withdrawAmount = new BigDecimal("75.50");
        Response withdrawResponse = withdrawFunds(walletId, withdrawAmount, 
                                                generateReferenceId("metrics-withdraw"), "Metrics test withdrawal");
        withdrawResponse.then().statusCode(200);
        
        // Then - Check metrics
        Response metrics = getMetrics();
        metrics.then().statusCode(200);
        
        String metricsText = metrics.getBody().asString();
        
        // Verify withdrawal metrics are present
        assertTrue(metricsText.contains("wallet_operations_withdrawals_total"), 
                  "Should contain withdrawal counter");
        assertTrue(metricsText.contains("wallet_operations_withdrawal_duration_seconds"), 
                  "Should contain withdrawal duration timer");
        assertTrue(metricsText.contains("wallet_money_withdrawn_total"), 
                  "Should contain money withdrawn counter");
    }

    @Test
    @DisplayName("Should track transfer metrics correctly")
    void shouldTrackTransferMetricsCorrectly() {
        // Given
        String sourceUserId = generateUserId("metrics-transfer-source");
        String destUserId = generateUserId("metrics-transfer-dest");
        String sourceWalletId = createWallet(sourceUserId);
        String destWalletId = createWallet(destUserId);
        
        // Setup funds for transfer
        depositFunds(sourceWalletId, new BigDecimal("300.00"), 
                   generateReferenceId("metrics-transfer-setup"), "Setup for transfer test");
        
        // When - Perform transfer
        BigDecimal transferAmount = new BigDecimal("125.25");
        Response transferResponse = transferFunds(sourceWalletId, destWalletId, transferAmount, 
                                                generateReferenceId("metrics-transfer"), "Metrics test transfer");
        transferResponse.then().statusCode(200);
        
        // Then - Check metrics
        Response metrics = getMetrics();
        metrics.then().statusCode(200);
        
        String metricsText = metrics.getBody().asString();
        
        // Verify transfer metrics are present
        assertTrue(metricsText.contains("wallet_operations_transfers_total"), 
                  "Should contain transfer counter");
        assertTrue(metricsText.contains("wallet_operations_transfer_duration_seconds"), 
                  "Should contain transfer duration timer");
        assertTrue(metricsText.contains("wallet_money_transferred_total"), 
                  "Should contain money transferred counter");
    }

    @Test
    @DisplayName("Should track query metrics correctly")
    void shouldTrackQueryMetricsCorrectly() {
        // Given
        String userId = generateUserId("metrics-query-test");
        String walletId = createWallet(userId);
        
        // Add some transaction history
        depositFunds(walletId, new BigDecimal("100.00"), 
                   generateReferenceId("metrics-query-setup"), "Setup for query test");
        
        // When - Perform queries
        Response walletResponse = getWallet(walletId);
        walletResponse.then().statusCode(200);
        
        Response historyResponse = getHistoricalBalance(walletId, "2025-12-31T23:59:59");
        historyResponse.then().statusCode(200);
        
        // Then - Check metrics
        Response metrics = getMetrics();
        metrics.then().statusCode(200);
        
        String metricsText = metrics.getBody().asString();
        
        // Verify query metrics are present
        assertTrue(metricsText.contains("wallet_operations_queries_total"), 
                  "Should contain query counter");
        assertTrue(metricsText.contains("wallet_operations_query_duration_seconds"), 
                  "Should contain query duration timer");
    }

    @Test
    @DisplayName("Should track Kafka event metrics")
    void shouldTrackKafkaEventMetrics() {
        // Given - Create wallet which should publish Kafka event
        String userId = generateUserId("metrics-kafka-test");
        String walletId = createWallet(userId);
        
        // When - Check metrics after wallet creation
        Response metrics = getMetrics();
        metrics.then().statusCode(200);
        
        String metricsText = metrics.getBody().asString();
        
        // Then - Verify Kafka event metrics are present
        assertTrue(metricsText.contains("wallet_events_published_total"), 
                  "Should contain events published counter");
        
        // Should contain WALLET_CREATED event type
        assertTrue(metricsText.contains("event_type=\"WALLET_CREATED\""), 
                  "Should contain WALLET_CREATED event type metric");
    }

    @Test
    @DisplayName("Should track error metrics for failed operations")
    void shouldTrackErrorMetricsForFailedOperations() {
        // Given
        String userId = generateUserId("metrics-error-test");
        String walletId = createWallet(userId);
        
        // Setup small balance
        depositFunds(walletId, new BigDecimal("10.00"), 
                   generateReferenceId("metrics-error-setup"), "Small setup for error test");
        
        // When - Attempt operation that should fail (insufficient funds)
        Response failedWithdrawResponse = withdrawFunds(walletId, new BigDecimal("100.00"), 
                                                      generateReferenceId("metrics-error-withdraw"), 
                                                      "Should fail withdrawal");
        failedWithdrawResponse.then()
            .statusCode(anyOf(equalTo(400), equalTo(500))); // Should fail
        
        // Then - Check that error metrics are tracked
        Response metrics = getMetrics();
        metrics.then().statusCode(200);
        
        String metricsText = metrics.getBody().asString();
        
        // Verify error metrics are present
        assertTrue(metricsText.contains("wallet_operations_failed_total"), 
                  "Should contain failed operations counter");
    }

    @Test
    @DisplayName("Should provide comprehensive business metrics")
    void shouldProvideComprehensiveBusinessMetrics() {
        // Given - Perform various operations to generate metrics
        String userId = generateUserId("metrics-comprehensive-test");
        String walletId = createWallet(userId);
        
        // Perform multiple operations
        depositFunds(walletId, new BigDecimal("500.00"), generateReferenceId("comp-deposit1"), "Deposit 1");
        depositFunds(walletId, new BigDecimal("250.00"), generateReferenceId("comp-deposit2"), "Deposit 2");
        withdrawFunds(walletId, new BigDecimal("100.00"), generateReferenceId("comp-withdraw"), "Withdrawal");
        
        // Create another wallet for transfer
        String destWalletId = createWallet(generateUserId("metrics-comp-dest"));
        transferFunds(walletId, destWalletId, new BigDecimal("150.00"), 
                    generateReferenceId("comp-transfer"), "Transfer");
        
        // Perform queries
        getWallet(walletId);
        getHistoricalBalance(walletId, "2025-12-31T23:59:59");
        
        // When - Get comprehensive metrics
        Response metrics = getMetrics();
        metrics.then().statusCode(200);
        
        String metricsText = metrics.getBody().asString();
        
        // Then - Verify all expected metric types are present
        String[] expectedMetrics = {
            "wallet_operations_created_total",
            "wallet_operations_deposits_total", 
            "wallet_operations_withdrawals_total",
            "wallet_operations_transfers_total",
            "wallet_operations_queries_total",
            "wallet_total_count",
            "wallet_transactions_total",
            "wallet_money_deposited_total",
            "wallet_money_withdrawn_total", 
            "wallet_money_transferred_total",
            "wallet_events_published_total",
            "wallet_operations_creation_duration_seconds",
            "wallet_operations_deposit_duration_seconds",
            "wallet_operations_withdrawal_duration_seconds",
            "wallet_operations_transfer_duration_seconds",
            "wallet_operations_query_duration_seconds"
        };
        
        for (String expectedMetric : expectedMetrics) {
            assertTrue(metricsText.contains(expectedMetric), 
                      "Should contain metric: " + expectedMetric);
        }
    }

    /**
     * Helper method to get Prometheus metrics
     */
    private Response getMetrics() {
        return RestAssured
            .given()
            .when()
                .get("/metrics")
            .then()
                .extract().response();
    }
}
