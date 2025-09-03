package com.wallet.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for core wallet operations.
 * Tests the complete flow: HTTP → CQRS → Database → Kafka events
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Wallet Operations Integration Tests")
public class WalletOperationsIntegrationTest extends BaseIntegrationTest {

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("Should create wallet successfully")
    void shouldCreateWalletSuccessfully() {
        // Given
        String userId = generateUserId("create-test");
        
        // When
        String walletId = createWallet(userId);
        
        // Then
        assertNotNull(walletId);
        assertFalse(walletId.isEmpty());
        
        // Verify wallet was created by retrieving it
        Response getResponse = getWallet(walletId);
        getResponse.then()
            .statusCode(200)
            .body("id", equalTo(walletId))
            .body("userId", equalTo(userId))
            .body("balance", equalTo(0.0f))
            .body("status", equalTo("ACTIVE"))
            .body("createdAt", notNullValue())
            .body("updatedAt", notNullValue());
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("Should handle wallet not found")
    void shouldHandleWalletNotFound() {
        // Given
        String nonExistentWalletId = "non-existent-wallet-id";
        
        // When
        Response response = getWallet(nonExistentWalletId);
        
        // Then
        response.then().statusCode(400); // Wallet not found returns 400 Bad Request
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("Should deposit funds successfully")
    void shouldDepositFundsSuccessfully() {
        // Given
        String userId = generateUserId("deposit-test");
        String walletId = createWallet(userId);
        BigDecimal depositAmount = new BigDecimal("150.75");
        String referenceId = generateReferenceId("deposit");
        
        // When
        Response depositResponse = depositFunds(walletId, depositAmount, referenceId, "Test deposit");
        
        // Then
        depositResponse.then()
            .statusCode(200)
            .header("Location", containsString("/api/v1/transactions/"));
            
        // Verify balance was updated
        Response walletResponse = getWallet(walletId);
        walletResponse.then()
            .statusCode(200)
            .body("balance", equalTo(150.75f));
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("Should withdraw funds successfully")
    void shouldWithdrawFundsSuccessfully() {
        // Given
        String userId = generateUserId("withdraw-test");
        String walletId = createWallet(userId);
        
        // First deposit some funds
        BigDecimal depositAmount = new BigDecimal("200.00");
        depositFunds(walletId, depositAmount, generateReferenceId("setup-deposit"), "Setup deposit");
        
        // When - withdraw some funds
        BigDecimal withdrawAmount = new BigDecimal("75.25");
        Response withdrawResponse = withdrawFunds(walletId, withdrawAmount, generateReferenceId("withdraw"), "Test withdrawal");
        
        // Then
        withdrawResponse.then()
            .statusCode(200)
            .header("Location", containsString("/api/v1/transactions/"));
            
        // Verify balance was updated (200.00 - 75.25 = 124.75)
        Response walletResponse = getWallet(walletId);
        walletResponse.then()
            .statusCode(200)
            .body("balance", equalTo(124.75f));
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("Should reject withdrawal with insufficient funds")
    void shouldRejectWithdrawalWithInsufficientFunds() {
        // Given
        String userId = generateUserId("insufficient-funds-test");
        String walletId = createWallet(userId);
        
        // Deposit small amount
        BigDecimal depositAmount = new BigDecimal("10.00");
        depositFunds(walletId, depositAmount, generateReferenceId("small-deposit"), "Small deposit");
        
        // When - try to withdraw more than available
        BigDecimal withdrawAmount = new BigDecimal("50.00");
        Response withdrawResponse = withdrawFunds(walletId, withdrawAmount, generateReferenceId("big-withdraw"), "Big withdrawal");
        
        // Then
        withdrawResponse.then()
            .statusCode(400) // Business logic errors should return 400
            .body("error", notNullValue());
            
        // Verify balance unchanged
        Response walletResponse = getWallet(walletId);
        walletResponse.then()
            .statusCode(200)
            .body("balance", equalTo(10.0f));
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    @DisplayName("Should transfer funds between wallets successfully")
    void shouldTransferFundsBetweenWalletsSuccessfully() {
        // Given
        String sourceUserId = generateUserId("transfer-source");
        String destUserId = generateUserId("transfer-dest");
        String sourceWalletId = createWallet(sourceUserId);
        String destWalletId = createWallet(destUserId);
        
        // Setup source wallet with funds
        BigDecimal initialAmount = new BigDecimal("300.00");
        depositFunds(sourceWalletId, initialAmount, generateReferenceId("setup"), "Setup funds");
        
        // When - transfer funds
        BigDecimal transferAmount = new BigDecimal("125.50");
        Response transferResponse = transferFunds(sourceWalletId, destWalletId, transferAmount, 
                                                generateReferenceId("transfer"), "Test transfer");
        
        // Then
        transferResponse.then()
            .statusCode(200)
            .header("Location", containsString("/api/v1/transactions/"));
            
        // Verify source wallet balance (300.00 - 125.50 = 174.50)
        Response sourceResponse = getWallet(sourceWalletId);
        sourceResponse.then()
            .statusCode(200)
            .body("balance", equalTo(174.50f));
            
        // Verify destination wallet balance (0.00 + 125.50 = 125.50)
        Response destResponse = getWallet(destWalletId);
        destResponse.then()
            .statusCode(200)
            .body("balance", equalTo(125.50f));
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    @DisplayName("Should reject transfer to same wallet")
    void shouldRejectTransferToSameWallet() {
        // Given
        String userId = generateUserId("same-wallet-test");
        String walletId = createWallet(userId);
        
        // Setup wallet with funds
        BigDecimal initialAmount = new BigDecimal("100.00");
        depositFunds(walletId, initialAmount, generateReferenceId("setup"), "Setup funds");
        
        // When - try to transfer to same wallet
        BigDecimal transferAmount = new BigDecimal("50.00");
        Response transferResponse = transferFunds(walletId, walletId, transferAmount, 
                                                generateReferenceId("self-transfer"), "Self transfer");
        
        // Then
        transferResponse.then()
            .statusCode(400) // Self transfer validation returns 400 Bad Request
            .body("error", notNullValue());
            
        // Verify balance unchanged
        Response walletResponse = getWallet(walletId);
        walletResponse.then()
            .statusCode(200)
            .body("balance", equalTo(100.0f));
    }

    @Test
    @org.junit.jupiter.api.Order(8)
    @DisplayName("Should get historical balance correctly")
    void shouldGetHistoricalBalanceCorrectly() {
        // Given
        String userId = generateUserId("historical-test");
        String walletId = createWallet(userId);
        
        // Perform multiple transactions
        depositFunds(walletId, new BigDecimal("100.00"), generateReferenceId("deposit1"), "First deposit");
        depositFunds(walletId, new BigDecimal("50.00"), generateReferenceId("deposit2"), "Second deposit");
        withdrawFunds(walletId, new BigDecimal("25.00"), generateReferenceId("withdraw1"), "First withdrawal");
        
        // When - get historical balance at a future timestamp
        Response historyResponse = getHistoricalBalance(walletId, "2025-12-31T23:59:59");
        
        // Then
        historyResponse.then()
            .statusCode(200)
            .body("walletId", equalTo(walletId))
            .body("balance", equalTo(125.0f)) // 100 + 50 - 25 = 125
            .body("timestamp", equalTo("2025-12-31T23:59:59"));
    }

    @Test
    @org.junit.jupiter.api.Order(9)
    @DisplayName("Should validate input parameters")
    void shouldValidateInputParameters() {
        // Given
        String userId = generateUserId("validation-test");
        String walletId = createWallet(userId);
        
        // Test negative deposit amount
        Response negativeDepositResponse = depositFunds(walletId, new BigDecimal("-10.00"), 
                                                      generateReferenceId("negative"), "Negative deposit");
        negativeDepositResponse.then()
            .statusCode(400); // Business logic errors should return 400
            
        // Test zero withdrawal amount
        Response zeroWithdrawResponse = withdrawFunds(walletId, BigDecimal.ZERO, 
                                                    generateReferenceId("zero"), "Zero withdrawal");
        zeroWithdrawResponse.then()
            .statusCode(400); // Business logic errors should return 400
            
        // Test missing timestamp for historical balance
        Response missingTimestampResponse = getHistoricalBalance(walletId, null);
        missingTimestampResponse.then()
            .statusCode(400);
    }
}
