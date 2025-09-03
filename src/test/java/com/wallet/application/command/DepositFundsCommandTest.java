package com.wallet.application.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DepositFundsCommand Tests")
class DepositFundsCommandTest {

    @Test
    @DisplayName("Should create command with valid data")
    void shouldCreateCommandWithValidData() {
        // Given
        String walletId = "wallet-123";
        BigDecimal amount = new BigDecimal("100.50");
        String referenceId = "ref-456";

        // When
        DepositFundsCommand command = new DepositFundsCommand(walletId, amount, referenceId);

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
        String referenceId = "ref-789";

        // When
        DepositFundsCommand command1 = new DepositFundsCommand(walletId, amount, referenceId);
        DepositFundsCommand command2 = new DepositFundsCommand(walletId, amount, referenceId);

        // Then
        assertNotEquals(command1.getCommandId(), command2.getCommandId());
    }

    @Test
    @DisplayName("Should handle different wallet IDs")
    void shouldHandleDifferentWalletIds() {
        // Given
        String[] walletIds = {"wallet-1", "wallet-2", "wallet-3"};
        BigDecimal amount = new BigDecimal("25.75");
        String referenceId = "ref-001";

        // When & Then
        for (String walletId : walletIds) {
            DepositFundsCommand command = new DepositFundsCommand(walletId, amount, referenceId);
            assertEquals(walletId, command.getWalletId());
        }
    }

    @Test
    @DisplayName("Should handle different amounts")
    void shouldHandleDifferentAmounts() {
        // Given
        String walletId = "wallet-123";
        String referenceId = "ref-001";
        BigDecimal[] amounts = {
            new BigDecimal("0.01"),
            new BigDecimal("100.00"),
            new BigDecimal("999999.99")
        };

        // When & Then
        for (BigDecimal amount : amounts) {
            DepositFundsCommand command = new DepositFundsCommand(walletId, amount, referenceId);
            assertEquals(amount, command.getAmount());
        }
    }

    @Test
    @DisplayName("Should handle different reference IDs")
    void shouldHandleDifferentReferenceIds() {
        // Given
        String walletId = "wallet-123";
        BigDecimal amount = new BigDecimal("50.00");
        String[] referenceIds = {"ref-1", "payment-123", "transaction-xyz"};

        // When & Then
        for (String referenceId : referenceIds) {
            DepositFundsCommand command = new DepositFundsCommand(walletId, amount, referenceId);
            assertEquals(referenceId, command.getReferenceId());
        }
    }

    @Test
    @DisplayName("Should preserve amount precision")
    void shouldPreserveAmountPrecision() {
        // Given
        String walletId = "wallet-123";
        String referenceId = "ref-001";
        BigDecimal preciseAmount = new BigDecimal("123.456789");

        // When
        DepositFundsCommand command = new DepositFundsCommand(walletId, preciseAmount, referenceId);

        // Then
        assertEquals(preciseAmount, command.getAmount());
        assertEquals(preciseAmount.scale(), command.getAmount().scale());
    }

    @Test
    @DisplayName("Should handle null reference ID")
    void shouldHandleNullReferenceId() {
        // Given
        String walletId = "wallet-123";
        BigDecimal amount = new BigDecimal("50.00");
        String referenceId = null;

        // When
        DepositFundsCommand command = new DepositFundsCommand(walletId, amount, referenceId);

        // Then
        assertNull(command.getReferenceId());
        assertNotNull(command.getCommandId());
        assertEquals(walletId, command.getWalletId());
        assertEquals(amount, command.getAmount());
    }
}
