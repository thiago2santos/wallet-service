package com.wallet.application.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WithdrawFundsCommand Tests")
class WithdrawFundsCommandTest {

    @Test
    @DisplayName("Should create command with valid data")
    void shouldCreateCommandWithValidData() {
        // Given
        String walletId = "wallet-123";
        BigDecimal amount = new BigDecimal("75.25");
        String referenceId = "withdrawal-456";

        // When
        WithdrawFundsCommand command = new WithdrawFundsCommand(walletId, amount, referenceId);

        // Then
        assertNotNull(command);
        assertEquals(walletId, command.getWalletId());
        assertEquals(amount, command.getAmount());
        assertEquals(referenceId, command.getReferenceId());
        assertNotNull(command.getCommandId());
        assertFalse(command.getCommandId().isEmpty());
    }

    @Test
    @DisplayName("Should generate unique command IDs")
    void shouldGenerateUniqueCommandIds() {
        // Given
        String walletId = "wallet-123";
        BigDecimal amount = new BigDecimal("50.00");
        String referenceId = "withdrawal-789";

        // When
        WithdrawFundsCommand command1 = new WithdrawFundsCommand(walletId, amount, referenceId);
        WithdrawFundsCommand command2 = new WithdrawFundsCommand(walletId, amount, referenceId);

        // Then
        assertNotEquals(command1.getCommandId(), command2.getCommandId());
    }

    @Test
    @DisplayName("Should handle different wallet IDs")
    void shouldHandleDifferentWalletIds() {
        // Given
        String[] walletIds = {"wallet-alpha", "wallet-beta", "wallet-gamma"};
        BigDecimal amount = new BigDecimal("30.00");
        String referenceId = "withdrawal-001";

        // When & Then
        for (String walletId : walletIds) {
            WithdrawFundsCommand command = new WithdrawFundsCommand(walletId, amount, referenceId);
            assertEquals(walletId, command.getWalletId());
        }
    }

    @Test
    @DisplayName("Should handle different amounts")
    void shouldHandleDifferentAmounts() {
        // Given
        String walletId = "wallet-123";
        String referenceId = "withdrawal-001";
        BigDecimal[] amounts = {
            new BigDecimal("0.01"),
            new BigDecimal("500.00"),
            new BigDecimal("1000000.00")
        };

        // When & Then
        for (BigDecimal amount : amounts) {
            WithdrawFundsCommand command = new WithdrawFundsCommand(walletId, amount, referenceId);
            assertEquals(amount, command.getAmount());
        }
    }

    @Test
    @DisplayName("Should handle different reference IDs")
    void shouldHandleDifferentReferenceIds() {
        // Given
        String walletId = "wallet-123";
        BigDecimal amount = new BigDecimal("100.00");
        String[] referenceIds = {"atm-withdrawal", "online-purchase", "transfer-out"};

        // When & Then
        for (String referenceId : referenceIds) {
            WithdrawFundsCommand command = new WithdrawFundsCommand(walletId, amount, referenceId);
            assertEquals(referenceId, command.getReferenceId());
        }
    }

    @Test
    @DisplayName("Should preserve amount precision")
    void shouldPreserveAmountPrecision() {
        // Given
        String walletId = "wallet-123";
        String referenceId = "precise-withdrawal";
        BigDecimal preciseAmount = new BigDecimal("99.123456");

        // When
        WithdrawFundsCommand command = new WithdrawFundsCommand(walletId, preciseAmount, referenceId);

        // Then
        assertEquals(preciseAmount, command.getAmount());
        assertEquals(preciseAmount.scale(), command.getAmount().scale());
    }

    @Test
    @DisplayName("Should handle null reference ID")
    void shouldHandleNullReferenceId() {
        // Given
        String walletId = "wallet-123";
        BigDecimal amount = new BigDecimal("25.00");
        String referenceId = null;

        // When
        WithdrawFundsCommand command = new WithdrawFundsCommand(walletId, amount, referenceId);

        // Then
        assertNull(command.getReferenceId());
        assertNotNull(command.getCommandId());
        assertEquals(walletId, command.getWalletId());
        assertEquals(amount, command.getAmount());
    }

    @Test
    @DisplayName("Should handle zero amount")
    void shouldHandleZeroAmount() {
        // Given
        String walletId = "wallet-123";
        BigDecimal amount = BigDecimal.ZERO;
        String referenceId = "zero-withdrawal";

        // When
        WithdrawFundsCommand command = new WithdrawFundsCommand(walletId, amount, referenceId);

        // Then
        assertEquals(BigDecimal.ZERO, command.getAmount());
        assertEquals(walletId, command.getWalletId());
        assertEquals(referenceId, command.getReferenceId());
    }

    @Test
    @DisplayName("Should maintain immutability")
    void shouldMaintainImmutability() {
        // Given
        String walletId = "wallet-123";
        BigDecimal amount = new BigDecimal("50.00");
        String referenceId = "immutable-test";

        // When
        WithdrawFundsCommand command = new WithdrawFundsCommand(walletId, amount, referenceId);
        String originalCommandId = command.getCommandId();
        String originalWalletId = command.getWalletId();
        BigDecimal originalAmount = command.getAmount();
        String originalReferenceId = command.getReferenceId();

        // Then - Values should remain the same (immutable)
        assertEquals(originalCommandId, command.getCommandId());
        assertEquals(originalWalletId, command.getWalletId());
        assertEquals(originalAmount, command.getAmount());
        assertEquals(originalReferenceId, command.getReferenceId());
    }
}
