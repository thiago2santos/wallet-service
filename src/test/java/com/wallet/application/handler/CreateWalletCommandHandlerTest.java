package com.wallet.application.handler;

import com.wallet.application.command.CreateWalletCommand;
import com.wallet.domain.model.Wallet;
import com.wallet.domain.model.WalletStatus;
import com.wallet.infrastructure.persistence.WalletRepository;
import com.wallet.infrastructure.metrics.WalletMetrics;
import com.wallet.infrastructure.outbox.OutboxEventService;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateWalletCommandHandlerTest {

    @InjectMocks
    CreateWalletCommandHandler handler;

    @Mock
    WalletRepository walletRepository;

    @Mock
    WalletMetrics walletMetrics;

    @Mock
    OutboxEventService outboxEventService;

    private CreateWalletCommand command;

    @BeforeEach
    void setUp() {
        command = new CreateWalletCommand("user-123");
        
        // Setup common mocks
        when(walletMetrics.startWalletCreationTimer())
            .thenReturn(() -> {}); // Mock timer that does nothing
        when(outboxEventService.storeEvent(any(), any(), any()))
            .thenReturn(Uni.createFrom().voidItem());
    }

    @Test
    void shouldCreateWalletSuccessfully() {
        // Given
        Wallet mockWallet = new Wallet();
        mockWallet.setId("test-wallet-id");
        when(walletRepository.persist(any(Wallet.class)))
            .thenReturn(Uni.createFrom().item(mockWallet));

        // When
        Uni<String> result = handler.handle(command);

        // Then
        String walletId = result.await().indefinitely();
        assertNotNull(walletId);
        assertFalse(walletId.isEmpty());

        // Verify wallet was persisted with correct data
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).persist(walletCaptor.capture());
        
        Wallet capturedWallet = walletCaptor.getValue();
        assertEquals("user-123", capturedWallet.getUserId());

        assertEquals(BigDecimal.ZERO, capturedWallet.getBalance());
        assertEquals(WalletStatus.ACTIVE.name(), capturedWallet.getStatus());
        assertNotNull(capturedWallet.getId());
        assertNotNull(capturedWallet.getCreatedAt());
        assertNotNull(capturedWallet.getUpdatedAt());
    }

    @Test
    void shouldGenerateUniqueWalletId() {
        // Given
        when(walletRepository.persist(any(Wallet.class)))
            .thenAnswer(invocation -> {
                Wallet wallet = invocation.getArgument(0);
                return Uni.createFrom().item(wallet);
            });

        // When
        Uni<String> result1 = handler.handle(command);
        Uni<String> result2 = handler.handle(command);

        // Then
        String walletId1 = result1.await().indefinitely();
        String walletId2 = result2.await().indefinitely();
        
        assertNotEquals(walletId1, walletId2);
    }



    @Test
    void shouldPropagateRepositoryFailure() {
        // Given
        RuntimeException repositoryException = new RuntimeException("Database connection failed");
        when(walletRepository.persist(any(Wallet.class)))
            .thenReturn(Uni.createFrom().failure(repositoryException));

        // When & Then
        Uni<String> result = handler.handle(command);
        
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> result.await().indefinitely());
        assertEquals("Database connection failed", exception.getMessage());
    }

    @Test
    void shouldSetInitialBalanceToZero() {
        // Given
        when(walletRepository.persist(any(Wallet.class)))
            .thenAnswer(invocation -> {
                Wallet wallet = invocation.getArgument(0);
                return Uni.createFrom().item(wallet);
            });

        // When
        handler.handle(command).await().indefinitely();

        // Then
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).persist(walletCaptor.capture());
        
        Wallet capturedWallet = walletCaptor.getValue();
        assertEquals(BigDecimal.ZERO, capturedWallet.getBalance());
    }

    @Test
    void shouldSetWalletStatusToActive() {
        // Given
        when(walletRepository.persist(any(Wallet.class)))
            .thenAnswer(invocation -> {
                Wallet wallet = invocation.getArgument(0);
                return Uni.createFrom().item(wallet);
            });

        // When
        handler.handle(command).await().indefinitely();

        // Then
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).persist(walletCaptor.capture());
        
        Wallet capturedWallet = walletCaptor.getValue();
        assertEquals(WalletStatus.ACTIVE.name(), capturedWallet.getStatus());
    }
}
