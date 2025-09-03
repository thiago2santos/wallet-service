package com.wallet.application.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TransferFundsCommand Tests")
class TransferFundsCommandTest {

    @Test
    @DisplayName("Should create command with valid data")
    void shouldCreateCommandWithValidData() {
        // Given
        String sourceWalletId = "wallet-source-123";
        String destinationWalletId = "wallet-dest-456";
        BigDecimal amount = new BigDecimal("150.75");
        String referenceId = "transfer-789";

        // When
        TransferFundsCommand command = new TransferFundsCommand(
            sourceWalletId, destinationWalletId, amount, referenceId);

        // Then
        assertNotNull(command);
        assertEquals(sourceWalletId, command.getSourceWalletId());
        assertEquals(destinationWalletId, command.getDestinationWalletId());
        assertEquals(amount, command.getAmount());
        assertEquals(referenceId, command.getReferenceId());
        assertNotNull(command.getCommandId());
        assertFalse(command.getCommandId().isEmpty());
    }

    @Test
    @DisplayName("Should generate unique command IDs")
    void shouldGenerateUniqueCommandIds() {
        // Given
        String sourceWalletId = "wallet-1";
        String destinationWalletId = "wallet-2";
        BigDecimal amount = new BigDecimal("100.00");
        String referenceId = "transfer-001";

        // When
        TransferFundsCommand command1 = new TransferFundsCommand(
            sourceWalletId, destinationWalletId, amount, referenceId);
        TransferFundsCommand command2 = new TransferFundsCommand(
            sourceWalletId, destinationWalletId, amount, referenceId);

        // Then
        assertNotEquals(command1.getCommandId(), command2.getCommandId());
    }

    @Test
    @DisplayName("Should handle different source wallet IDs")
    void shouldHandleDifferentSourceWalletIds() {
        // Given
        String[] sourceWalletIds = {"wallet-alpha", "wallet-beta", "wallet-gamma"};
        String destinationWalletId = "wallet-destination";
        BigDecimal amount = new BigDecimal("50.00");
        String referenceId = "transfer-test";

        // When & Then
        for (String sourceWalletId : sourceWalletIds) {
            TransferFundsCommand command = new TransferFundsCommand(
                sourceWalletId, destinationWalletId, amount, referenceId);
            assertEquals(sourceWalletId, command.getSourceWalletId());
        }
    }

    @Test
    @DisplayName("Should handle different destination wallet IDs")
    void shouldHandleDifferentDestinationWalletIds() {
        // Given
        String sourceWalletId = "wallet-source";
        String[] destinationWalletIds = {"wallet-dest-1", "wallet-dest-2", "wallet-dest-3"};
        BigDecimal amount = new BigDecimal("75.00");
        String referenceId = "transfer-test";

        // When & Then
        for (String destinationWalletId : destinationWalletIds) {
            TransferFundsCommand command = new TransferFundsCommand(
                sourceWalletId, destinationWalletId, amount, referenceId);
            assertEquals(destinationWalletId, command.getDestinationWalletId());
        }
    }

    @Test
    @DisplayName("Should handle different amounts")
    void shouldHandleDifferentAmounts() {
        // Given
        String sourceWalletId = "wallet-source";
        String destinationWalletId = "wallet-dest";
        String referenceId = "amount-test";
        BigDecimal[] amounts = {
            new BigDecimal("0.01"),
            new BigDecimal("1000.00"),
            new BigDecimal("999999.99")
        };

        // When & Then
        for (BigDecimal amount : amounts) {
            TransferFundsCommand command = new TransferFundsCommand(
                sourceWalletId, destinationWalletId, amount, referenceId);
            assertEquals(amount, command.getAmount());
        }
    }

    @Test
    @DisplayName("Should handle different reference IDs")
    void shouldHandleDifferentReferenceIds() {
        // Given
        String sourceWalletId = "wallet-source";
        String destinationWalletId = "wallet-dest";
        BigDecimal amount = new BigDecimal("200.00");
        String[] referenceIds = {"p2p-transfer", "bill-payment", "merchant-payment"};

        // When & Then
        for (String referenceId : referenceIds) {
            TransferFundsCommand command = new TransferFundsCommand(
                sourceWalletId, destinationWalletId, amount, referenceId);
            assertEquals(referenceId, command.getReferenceId());
        }
    }

    @Test
    @DisplayName("Should preserve amount precision")
    void shouldPreserveAmountPrecision() {
        // Given
        String sourceWalletId = "wallet-source";
        String destinationWalletId = "wallet-dest";
        String referenceId = "precision-test";
        BigDecimal preciseAmount = new BigDecimal("123.456789");

        // When
        TransferFundsCommand command = new TransferFundsCommand(
            sourceWalletId, destinationWalletId, preciseAmount, referenceId);

        // Then
        assertEquals(preciseAmount, command.getAmount());
        assertEquals(preciseAmount.scale(), command.getAmount().scale());
    }

    @Test
    @DisplayName("Should handle null reference ID")
    void shouldHandleNullReferenceId() {
        // Given
        String sourceWalletId = "wallet-source";
        String destinationWalletId = "wallet-dest";
        BigDecimal amount = new BigDecimal("100.00");
        String referenceId = null;

        // When
        TransferFundsCommand command = new TransferFundsCommand(
            sourceWalletId, destinationWalletId, amount, referenceId);

        // Then
        assertNull(command.getReferenceId());
        assertNotNull(command.getCommandId());
        assertEquals(sourceWalletId, command.getSourceWalletId());
        assertEquals(destinationWalletId, command.getDestinationWalletId());
        assertEquals(amount, command.getAmount());
    }

    @Test
    @DisplayName("Should handle same source and destination wallets")
    void shouldHandleSameSourceAndDestinationWallets() {
        // Given
        String walletId = "wallet-same";
        BigDecimal amount = new BigDecimal("50.00");
        String referenceId = "self-transfer";

        // When
        TransferFundsCommand command = new TransferFundsCommand(
            walletId, walletId, amount, referenceId);

        // Then
        assertEquals(walletId, command.getSourceWalletId());
        assertEquals(walletId, command.getDestinationWalletId());
        assertEquals(amount, command.getAmount());
        assertEquals(referenceId, command.getReferenceId());
    }

    @Test
    @DisplayName("Should maintain immutability")
    void shouldMaintainImmutability() {
        // Given
        String sourceWalletId = "wallet-source";
        String destinationWalletId = "wallet-dest";
        BigDecimal amount = new BigDecimal("100.00");
        String referenceId = "immutable-test";

        // When
        TransferFundsCommand command = new TransferFundsCommand(
            sourceWalletId, destinationWalletId, amount, referenceId);
        
        String originalCommandId = command.getCommandId();
        String originalSourceWalletId = command.getSourceWalletId();
        String originalDestinationWalletId = command.getDestinationWalletId();
        BigDecimal originalAmount = command.getAmount();
        String originalReferenceId = command.getReferenceId();

        // Then - Values should remain the same (immutable)
        assertEquals(originalCommandId, command.getCommandId());
        assertEquals(originalSourceWalletId, command.getSourceWalletId());
        assertEquals(originalDestinationWalletId, command.getDestinationWalletId());
        assertEquals(originalAmount, command.getAmount());
        assertEquals(originalReferenceId, command.getReferenceId());
    }

    @Test
    @DisplayName("Should handle zero amount")
    void shouldHandleZeroAmount() {
        // Given
        String sourceWalletId = "wallet-source";
        String destinationWalletId = "wallet-dest";
        BigDecimal amount = BigDecimal.ZERO;
        String referenceId = "zero-transfer";

        // When
        TransferFundsCommand command = new TransferFundsCommand(
            sourceWalletId, destinationWalletId, amount, referenceId);

        // Then
        assertEquals(BigDecimal.ZERO, command.getAmount());
        assertEquals(sourceWalletId, command.getSourceWalletId());
        assertEquals(destinationWalletId, command.getDestinationWalletId());
        assertEquals(referenceId, command.getReferenceId());
    }
}
