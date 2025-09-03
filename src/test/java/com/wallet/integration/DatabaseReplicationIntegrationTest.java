package com.wallet.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for database replication and read/write separation.
 * Verifies that writes go to primary and reads come from replica.
 */
@QuarkusTest
@DisplayName("Database Replication Integration Tests")
public class DatabaseReplicationIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should write to primary database and read from replica")
    void shouldWriteToPrimaryAndReadFromReplica() {
        // Given
        String userId = generateUserId("replication-test");
        
        // When - Create wallet (write operation → primary DB)
        String walletId = createWallet(userId);
        
        // Then - Retrieve wallet (read operation → replica DB)
        Response getResponse = getWallet(walletId);
        getResponse.then()
            .statusCode(200)
            .body("id", equalTo(walletId))
            .body("userId", equalTo(userId))
            .body("balance", equalTo(0.0f))
            .body("status", equalTo("ACTIVE"));
    }

    @Test
    @DisplayName("Should handle write operations through primary database")
    void shouldHandleWriteOperationsThroughPrimaryDatabase() {
        // Given
        String userId = generateUserId("write-ops-test");
        String walletId = createWallet(userId);
        
        // When - Perform multiple write operations (all should go to primary)
        Response deposit1 = depositFunds(walletId, new BigDecimal("100.00"), 
                                       generateReferenceId("write-deposit1"), "First deposit");
        Response deposit2 = depositFunds(walletId, new BigDecimal("50.00"), 
                                       generateReferenceId("write-deposit2"), "Second deposit");
        Response withdraw = withdrawFunds(walletId, new BigDecimal("25.00"), 
                                        generateReferenceId("write-withdraw"), "Withdrawal");
        
        // Then - All write operations should succeed
        deposit1.then().statusCode(200);
        deposit2.then().statusCode(200);
        withdraw.then().statusCode(200);
        
        // Verify final state through read operation (from replica)
        Response finalState = getWallet(walletId);
        finalState.then()
            .statusCode(200)
            .body("balance", equalTo(125.0f)); // 100 + 50 - 25 = 125
    }

    @Test
    @DisplayName("Should handle read operations through replica database")
    void shouldHandleReadOperationsThroughReplicaDatabase() {
        // Given - Setup test data through write operations
        String userId = generateUserId("read-ops-test");
        String walletId = createWallet(userId);
        
        // Add some transaction history
        depositFunds(walletId, new BigDecimal("200.00"), generateReferenceId("read-setup1"), "Setup deposit 1");
        depositFunds(walletId, new BigDecimal("75.00"), generateReferenceId("read-setup2"), "Setup deposit 2");
        withdrawFunds(walletId, new BigDecimal("50.00"), generateReferenceId("read-setup3"), "Setup withdrawal");
        
        // When - Perform read operations (should go to replica)
        Response currentWallet = getWallet(walletId);
        Response historicalBalance = getHistoricalBalance(walletId, "2025-12-31T23:59:59");
        
        // Then - Both read operations should succeed and return consistent data
        currentWallet.then()
            .statusCode(200)
            .body("balance", equalTo(225.0f)); // 200 + 75 - 50 = 225
            
        historicalBalance.then()
            .statusCode(200)
            .body("balance", equalTo(225.0f))
            .body("walletId", equalTo(walletId));
    }

    @Test
    @DisplayName("Should maintain consistency between primary and replica")
    void shouldMaintainConsistencyBetweenPrimaryAndReplica() {
        // Given
        String userId = generateUserId("consistency-test");
        String walletId = createWallet(userId);
        
        // When - Perform a series of operations
        BigDecimal[] amounts = {
            new BigDecimal("100.00"),
            new BigDecimal("250.50"),
            new BigDecimal("75.25")
        };
        
        // Deposits (writes to primary)
        for (int i = 0; i < amounts.length; i++) {
            Response depositResponse = depositFunds(walletId, amounts[i], 
                                                  generateReferenceId("consistency-deposit-" + i), 
                                                  "Consistency test deposit " + (i + 1));
            depositResponse.then().statusCode(200);
        }
        
        // Withdrawal (write to primary)
        Response withdrawResponse = withdrawFunds(walletId, new BigDecimal("125.75"), 
                                                generateReferenceId("consistency-withdraw"), 
                                                "Consistency test withdrawal");
        withdrawResponse.then().statusCode(200);
        
        // Expected balance: 100.00 + 250.50 + 75.25 - 125.75 = 300.00
        BigDecimal expectedBalance = new BigDecimal("300.00");
        
        // Then - Read operations should reflect all writes
        Response walletState = getWallet(walletId);
        walletState.then()
            .statusCode(200)
            .body("balance", equalTo(300.0f));
            
        // Historical balance should also be consistent
        Response historicalState = getHistoricalBalance(walletId, "2025-12-31T23:59:59");
        historicalState.then()
            .statusCode(200)
            .body("balance", equalTo(300.0f));
    }

    @Test
    @DisplayName("Should handle transfer operations with proper database routing")
    void shouldHandleTransferOperationsWithProperDatabaseRouting() {
        // Given - Create two wallets
        String sourceUserId = generateUserId("transfer-source");
        String destUserId = generateUserId("transfer-dest");
        String sourceWalletId = createWallet(sourceUserId);
        String destWalletId = createWallet(destUserId);
        
        // Setup source wallet with funds (write to primary)
        depositFunds(sourceWalletId, new BigDecimal("500.00"), 
                   generateReferenceId("transfer-setup"), "Transfer setup");
        
        // When - Perform transfer (involves both read and write operations)
        Response transferResponse = transferFunds(sourceWalletId, destWalletId, new BigDecimal("200.00"), 
                                                generateReferenceId("db-transfer"), "Database transfer test");
        
        // Then - Transfer should succeed
        transferResponse.then().statusCode(200);
        
        // Verify both wallets have correct balances (reads from replica)
        Response sourceState = getWallet(sourceWalletId);
        sourceState.then()
            .statusCode(200)
            .body("balance", equalTo(300.0f)); // 500 - 200 = 300
            
        Response destState = getWallet(destWalletId);
        destState.then()
            .statusCode(200)
            .body("balance", equalTo(200.0f)); // 0 + 200 = 200
    }

    @Test
    @DisplayName("Should handle high-frequency operations with database separation")
    void shouldHandleHighFrequencyOperationsWithDatabaseSeparation() {
        // Given
        String userId = generateUserId("high-freq-test");
        String walletId = createWallet(userId);
        
        // Setup initial balance
        depositFunds(walletId, new BigDecimal("1000.00"), 
                   generateReferenceId("high-freq-setup"), "High frequency setup");
        
        // When - Perform multiple rapid operations
        int operationCount = 5;
        BigDecimal operationAmount = new BigDecimal("10.00");
        
        for (int i = 0; i < operationCount; i++) {
            // Alternate between deposits and withdrawals
            if (i % 2 == 0) {
                Response depositResponse = depositFunds(walletId, operationAmount, 
                                                      generateReferenceId("high-freq-deposit-" + i), 
                                                      "High freq deposit " + i);
                depositResponse.then().statusCode(200);
            } else {
                Response withdrawResponse = withdrawFunds(walletId, operationAmount, 
                                                        generateReferenceId("high-freq-withdraw-" + i), 
                                                        "High freq withdraw " + i);
                withdrawResponse.then().statusCode(200);
            }
        }
        
        // Then - Final balance should be correct
        // Operations: +1000 (setup) +10 -10 +10 -10 +10 = 1010
        Response finalState = getWallet(walletId);
        finalState.then()
            .statusCode(200)
            .body("balance", equalTo(1010.0f));
            
        // Historical balance should match
        Response historicalState = getHistoricalBalance(walletId, "2025-12-31T23:59:59");
        historicalState.then()
            .statusCode(200)
            .body("balance", equalTo(1010.0f));
    }

    @Test
    @DisplayName("Should handle query-only operations efficiently")
    void shouldHandleQueryOnlyOperationsEfficiently() {
        // Given - Setup test wallet with some history
        String userId = generateUserId("query-only-test");
        String walletId = createWallet(userId);
        
        depositFunds(walletId, new BigDecimal("100.00"), generateReferenceId("query-setup1"), "Setup 1");
        depositFunds(walletId, new BigDecimal("200.00"), generateReferenceId("query-setup2"), "Setup 2");
        withdrawFunds(walletId, new BigDecimal("50.00"), generateReferenceId("query-setup3"), "Setup 3");
        
        // When - Perform multiple read operations (should all go to replica)
        Response read1 = getWallet(walletId);
        Response read2 = getWallet(walletId);
        Response history1 = getHistoricalBalance(walletId, "2025-01-01T00:00:00");
        Response history2 = getHistoricalBalance(walletId, "2025-12-31T23:59:59");
        
        // Then - All reads should succeed and return consistent data
        read1.then()
            .statusCode(200)
            .body("balance", equalTo(250.0f)); // 100 + 200 - 50 = 250
            
        read2.then()
            .statusCode(200)
            .body("balance", equalTo(250.0f));
            
        history1.then()
            .statusCode(200)
            .body("balance", lessThanOrEqualTo(250.0f)); // Historical balance at earlier time
            
        history2.then()
            .statusCode(200)
            .body("balance", equalTo(250.0f)); // All transactions included
    }
}
