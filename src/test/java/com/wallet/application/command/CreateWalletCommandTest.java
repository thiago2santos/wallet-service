package com.wallet.application.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreateWalletCommandTest {

    @Test
    void shouldCreateCommandWithValidData() {
        // Given
        String userId = "user-123";
        String currency = "USD";

        // When
        CreateWalletCommand command = new CreateWalletCommand(userId, currency);

        // Then
        assertNotNull(command);
        assertEquals(userId, command.getUserId());
        assertEquals(currency, command.getCurrency());
        assertNotNull(command.getCommandId());
        assertFalse(command.getCommandId().isEmpty());
    }

    @Test
    void shouldGenerateUniqueCommandIds() {
        // Given & When
        CreateWalletCommand command1 = new CreateWalletCommand("user-1", "USD");
        CreateWalletCommand command2 = new CreateWalletCommand("user-2", "EUR");

        // Then
        assertNotEquals(command1.getCommandId(), command2.getCommandId());
    }

    @Test
    void shouldHandleDifferentCurrencies() {
        // Given
        String[] currencies = {"USD", "EUR", "GBP", "JPY", "BRL"};

        // When & Then
        for (String currency : currencies) {
            CreateWalletCommand command = new CreateWalletCommand("user-test", currency);
            assertEquals(currency, command.getCurrency());
        }
    }

    @Test
    void shouldHandleDifferentUserIds() {
        // Given
        String[] userIds = {"user-123", "customer-456", "account-789"};

        // When & Then
        for (String userId : userIds) {
            CreateWalletCommand command = new CreateWalletCommand(userId, "USD");
            assertEquals(userId, command.getUserId());
        }
    }

    @Test
    void shouldBeEqualWhenSameData() {
        // Given
        CreateWalletCommand command1 = new CreateWalletCommand("user-123", "USD");
        CreateWalletCommand command2 = new CreateWalletCommand("user-123", "USD");

        // When & Then
        assertEquals(command1.getUserId(), command2.getUserId());
        assertEquals(command1.getCurrency(), command2.getCurrency());
        // Note: commandId will be different as it's generated per instance
    }
}
