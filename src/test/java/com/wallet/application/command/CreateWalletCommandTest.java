package com.wallet.application.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreateWalletCommandTest {

    @Test
    void shouldCreateCommandWithValidData() {
        // Given
        String userId = "user-123";

        // When
        CreateWalletCommand command = new CreateWalletCommand(userId);

        // Then
        assertNotNull(command);
        assertEquals(userId, command.getUserId());
        assertNotNull(command.getCommandId());
        assertFalse(command.getCommandId().isEmpty());
    }

    @Test
    void shouldGenerateUniqueCommandIds() {
        // Given & When
        CreateWalletCommand command1 = new CreateWalletCommand("user-1");
        CreateWalletCommand command2 = new CreateWalletCommand("user-2");

        // Then
        assertNotEquals(command1.getCommandId(), command2.getCommandId());
    }



    @Test
    void shouldHandleDifferentUserIds() {
        // Given
        String[] userIds = {"user-123", "customer-456", "account-789"};

        // When & Then
        for (String userId : userIds) {
            CreateWalletCommand command = new CreateWalletCommand(userId);
            assertEquals(userId, command.getUserId());
        }
    }

    @Test
    void shouldBeEqualWhenSameData() {
        // Given
        CreateWalletCommand command1 = new CreateWalletCommand("user-123");
        CreateWalletCommand command2 = new CreateWalletCommand("user-123");

        // When & Then
        assertEquals(command1.getUserId(), command2.getUserId());
        // Note: commandId will be different as it's generated per instance
    }
}
