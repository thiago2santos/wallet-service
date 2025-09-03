package com.wallet.api;

import com.wallet.api.request.CreateWalletRequest;
import com.wallet.application.handler.*;
import com.wallet.application.command.*;
import com.wallet.application.query.*;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletResourceTest {

    @Mock
    CreateWalletCommandHandler createWalletHandler;
    
    @Mock
    GetWalletQueryHandler getWalletQueryHandler;
    
    @Mock
    DepositFundsCommandHandler depositFundsHandler;
    
    @Mock
    WithdrawFundsCommandHandler withdrawFundsHandler;
    
    @Mock
    TransferFundsCommandHandler transferFundsHandler;
    
    @Mock
    GetHistoricalBalanceQueryHandler historicalBalanceQueryHandler;

    @InjectMocks
    WalletResource walletResource;

    @Test
    void shouldCreateWalletSuccessfully() {
        // Given
        String walletId = "test-wallet-id-123";
        when(createWalletHandler.handle(any(CreateWalletCommand.class)))
            .thenReturn(Uni.createFrom().item(walletId));

        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId("user-123");
        // request.setCurrency("USD");

        // When
        Uni<Response> result = walletResource.createWallet(request);
        Response response = result.await().indefinitely();

        // Then
        assertEquals(201, response.getStatus());
        assertEquals(URI.create("/api/v1/wallets/" + walletId), response.getLocation());
        verify(createWalletHandler).handle(any(CreateWalletCommand.class));
    }

    @Test
    void shouldHandleHandlerFailure() {
        // Given
        when(createWalletHandler.handle(any(CreateWalletCommand.class)))
            .thenReturn(Uni.createFrom().failure(new RuntimeException("Database error")));

        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId("user-123");
        // request.setCurrency("USD");

        // When & Then
        Uni<Response> result = walletResource.createWallet(request);
        assertThrows(RuntimeException.class, () -> result.await().indefinitely());
        verify(createWalletHandler).handle(any(CreateWalletCommand.class));
    }



    @Test
    void shouldVerifyCommandCreation() {
        // Given
        String expectedWalletId = "test-wallet-123";
        when(createWalletHandler.handle(any(CreateWalletCommand.class)))
            .thenReturn(Uni.createFrom().item(expectedWalletId));

        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId("user-456");
        // request.setCurrency("EUR");

        // When
        Uni<Response> result = walletResource.createWallet(request);
        Response response = result.await().indefinitely();

        // Then
        assertEquals(201, response.getStatus());
        
        // Verify the command was created with correct parameters
        verify(createWalletHandler).handle(argThat(command -> 
            "user-456".equals(command.getUserId())
        ));
    }
}
