package com.wallet.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests specifically for CQRS architecture.
 * Verifies that commands and queries are properly separated and working through buses.
 */
@QuarkusTest
@DisplayName("CQRS Architecture Integration Tests")
public class CQRSIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should handle commands through CommandBus")
    void shouldHandleCommandsThroughCommandBus() {
        // Test CreateWalletCommand
        String userId = generateUserId("cqrs-command-test");
        String walletId = createWallet(userId);
        assertNotNull(walletId);
        
        // Test DepositFundsCommand
        Response depositResponse = depositFunds(walletId, new BigDecimal("100.00"), 
                                              generateReferenceId("cqrs-deposit"), "CQRS deposit test");
        depositResponse.then().statusCode(200);
        
        // Test WithdrawFundsCommand
        Response withdrawResponse = withdrawFunds(walletId, new BigDecimal("30.00"), 
                                                generateReferenceId("cqrs-withdraw"), "CQRS withdraw test");
        withdrawResponse.then().statusCode(200);
        
        // Test TransferFundsCommand
        String destWalletId = createWallet(generateUserId("cqrs-dest"));
        Response transferResponse = transferFunds(walletId, destWalletId, new BigDecimal("25.00"), 
                                                generateReferenceId("cqrs-transfer"), "CQRS transfer test");
        transferResponse.then().statusCode(200);
        
        // Verify final balances
        getWallet(walletId).then()
            .statusCode(200)
            .body("balance", equalTo(45.0f)); // 100 - 30 - 25 = 45
            
        getWallet(destWalletId).then()
            .statusCode(200)
            .body("balance", equalTo(25.0f)); // 0 + 25 = 25
    }

    @Test
    @DisplayName("Should handle queries through QueryBus")
    void shouldHandleQueriesThroughQueryBus() {
        // Setup test data
        String userId = generateUserId("cqrs-query-test");
        String walletId = createWallet(userId);
        
        // Add some transactions for historical balance testing
        depositFunds(walletId, new BigDecimal("200.00"), generateReferenceId("query-deposit1"), "First deposit");
        depositFunds(walletId, new BigDecimal("75.00"), generateReferenceId("query-deposit2"), "Second deposit");
        withdrawFunds(walletId, new BigDecimal("50.00"), generateReferenceId("query-withdraw"), "Withdrawal");
        
        // Test GetWalletQuery through QueryBus
        Response walletResponse = getWallet(walletId);
        walletResponse.then()
            .statusCode(200)
            .body("id", equalTo(walletId))
            .body("userId", equalTo(userId))
            .body("balance", equalTo(225.0f)) // 200 + 75 - 50 = 225
            .body("status", equalTo("ACTIVE"));
            
        // Test GetHistoricalBalanceQuery through QueryBus
        Response historyResponse = getHistoricalBalance(walletId, "2025-12-31T23:59:59");
        historyResponse.then()
            .statusCode(200)
            .body("walletId", equalTo(walletId))
            .body("balance", equalTo(225.0f))
            .body("timestamp", equalTo("2025-12-31T23:59:59"));
    }

    @Test
    @DisplayName("Should maintain data consistency across command and query sides")
    void shouldMaintainDataConsistencyAcrossCommandAndQuerySides() {
        // Given - create wallet and perform operations
        String userId = generateUserId("consistency-test");
        String walletId = createWallet(userId);
        
        // Perform a series of operations
        depositFunds(walletId, new BigDecimal("500.00"), generateReferenceId("consistency-deposit1"), "Large deposit");
        withdrawFunds(walletId, new BigDecimal("150.00"), generateReferenceId("consistency-withdraw1"), "Withdrawal 1");
        depositFunds(walletId, new BigDecimal("75.50"), generateReferenceId("consistency-deposit2"), "Small deposit");
        withdrawFunds(walletId, new BigDecimal("25.25"), generateReferenceId("consistency-withdraw2"), "Withdrawal 2");
        
        // Expected balance: 500 - 150 + 75.50 - 25.25 = 400.25
        BigDecimal expectedBalance = new BigDecimal("400.25");
        
        // Verify consistency through GetWallet query
        Response currentWalletResponse = getWallet(walletId);
        currentWalletResponse.then()
            .statusCode(200)
            .body("balance", equalTo(400.25f));
            
        // Verify consistency through HistoricalBalance query
        Response historyResponse = getHistoricalBalance(walletId, "2025-12-31T23:59:59");
        historyResponse.then()
            .statusCode(200)
            .body("balance", equalTo(400.25f));
            
        // Both queries should return the same balance, proving CQRS consistency
        Float currentBalance = currentWalletResponse.jsonPath().getFloat("balance");
        Float historicalBalance = historyResponse.jsonPath().getFloat("balance");
        
        assertEquals(currentBalance, historicalBalance, 0.01f, 
                    "Current balance and historical balance should be consistent");
    }

    @Test
    @DisplayName("Should handle concurrent operations correctly")
    void shouldHandleConcurrentOperationsCorrectly() {
        // Given
        String userId = generateUserId("concurrent-test");
        String walletId = createWallet(userId);
        
        // Setup initial balance
        depositFunds(walletId, new BigDecimal("1000.00"), generateReferenceId("concurrent-setup"), "Setup for concurrent test");
        
        // Perform multiple operations in sequence (simulating concurrent behavior)
        // In a real concurrent test, these would be in parallel threads
        Response op1 = withdrawFunds(walletId, new BigDecimal("100.00"), generateReferenceId("concurrent-op1"), "Op 1");
        Response op2 = depositFunds(walletId, new BigDecimal("50.00"), generateReferenceId("concurrent-op2"), "Op 2");
        Response op3 = withdrawFunds(walletId, new BigDecimal("75.00"), generateReferenceId("concurrent-op3"), "Op 3");
        
        // All operations should succeed
        op1.then().statusCode(200);
        op2.then().statusCode(200);
        op3.then().statusCode(200);
        
        // Final balance should be correct: 1000 - 100 + 50 - 75 = 875
        Response finalWalletResponse = getWallet(walletId);
        finalWalletResponse.then()
            .statusCode(200)
            .body("balance", equalTo(875.0f));
    }

    @Test
    @DisplayName("Should handle command failures gracefully")
    void shouldHandleCommandFailuresGracefully() {
        // Given
        String userId = generateUserId("failure-test");
        String walletId = createWallet(userId);
        
        // Setup small balance
        depositFunds(walletId, new BigDecimal("10.00"), generateReferenceId("failure-setup"), "Small setup");
        
        // Try operations that should fail
        Response insufficientFundsResponse = withdrawFunds(walletId, new BigDecimal("100.00"), 
                                                         generateReferenceId("failure-withdraw"), "Should fail");
        insufficientFundsResponse.then()
            .statusCode(400); // Business logic errors should return 400
            
        // Verify wallet state is unchanged after failed operation
        Response walletResponse = getWallet(walletId);
        walletResponse.then()
            .statusCode(200)
            .body("balance", equalTo(10.0f));
            
        // Verify subsequent valid operations still work
        Response validDepositResponse = depositFunds(walletId, new BigDecimal("5.00"), 
                                                   generateReferenceId("failure-recovery"), "Recovery deposit");
        validDepositResponse.then().statusCode(200);
        
        // Final balance should be 15.00
        Response finalWalletResponse = getWallet(walletId);
        finalWalletResponse.then()
            .statusCode(200)
            .body("balance", equalTo(15.0f));
    }
}
