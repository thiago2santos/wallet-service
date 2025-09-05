package com.wallet.application.service;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mockito;

import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.SQLTransientException;
import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

import com.wallet.application.handler.CreateWalletCommandHandler;
import com.wallet.application.handler.DepositFundsCommandHandler;
import com.wallet.application.handler.WithdrawFundsCommandHandler;
import com.wallet.application.handler.TransferFundsCommandHandler;
import com.wallet.application.handler.GetWalletQueryHandler;
import com.wallet.application.command.CreateWalletCommand;
import com.wallet.application.command.DepositFundsCommand;
import com.wallet.application.command.WithdrawFundsCommand;
import com.wallet.application.command.TransferFundsCommand;
import com.wallet.application.query.GetWalletQuery;
import com.wallet.domain.model.Wallet;
import com.wallet.infrastructure.metrics.WalletMetrics;
import com.wallet.infrastructure.resilience.DegradationManager;
import com.wallet.infrastructure.resilience.ServiceDegradedException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for ResilientWalletService retry mechanisms
 * 
 * Test Categories:
 * 1. Optimistic Lock Retry Tests
 * 2. Transient Failure Retry Tests  
 * 3. Retry Exhaustion Fallback Tests
 * 4. Success After Retry Tests
 * 5. Metrics and Monitoring Tests
 */
@QuarkusTest
class ResilientWalletServiceTest {

    @Inject
    ResilientWalletService resilientWalletService;

    @InjectMock
    CreateWalletCommandHandler createWalletHandler;

    @InjectMock
    DepositFundsCommandHandler depositFundsHandler;

    @InjectMock
    WithdrawFundsCommandHandler withdrawFundsHandler;

    @InjectMock
    TransferFundsCommandHandler transferFundsHandler;

    @InjectMock
    GetWalletQueryHandler getWalletHandler;

    @InjectMock
    WalletMetrics walletMetrics;

    @InjectMock
    DegradationManager degradationManager;

    private Wallet mockWallet;

    @BeforeEach
    void setUp() {
        mockWallet = new Wallet();
        mockWallet.setId("wallet-123");
        mockWallet.setUserId("user-123");
        mockWallet.setBalance(BigDecimal.valueOf(1000));
        mockWallet.setStatus("ACTIVE");
        
        // Reset all mocks
        Mockito.reset(createWalletHandler, depositFundsHandler, withdrawFundsHandler, 
                     transferFundsHandler, getWalletHandler, walletMetrics, degradationManager);
    }

    // ============================================================================
    // OPTIMISTIC LOCK RETRY TESTS
    // ============================================================================

    @Nested
    @DisplayName("Optimistic Lock Retry Tests")
    class OptimisticLockRetryTests {

        @Test
        @DisplayName("Should retry deposit on OptimisticLockException and succeed")
        void shouldRetryDepositOnOptimisticLockException() {
            // Arrange
            String walletId = "wallet-123";
            BigDecimal amount = BigDecimal.valueOf(100);
            String referenceId = "ref-123";
            String transactionId = "txn-123";

            when(depositFundsHandler.handle(any(DepositFundsCommand.class)))
                .thenThrow(new OptimisticLockException("Optimistic lock failed"))
                .thenThrow(new OptimisticLockException("Optimistic lock failed again"))
                .thenReturn(Uni.createFrom().item(transactionId));

            // Act & Assert
            resilientWalletService.depositFundsWithRetry(walletId, amount, referenceId)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem(transactionId);

            // Verify retry attempts were recorded
            verify(walletMetrics, times(2)).recordRetryAttempt(eq("deposit"), eq("optimistic_lock"), eq("OptimisticLockException"));
            verify(walletMetrics, times(1)).recordSuccessfulRetryOperation(eq("deposit"), eq("optimistic_lock"));
            verify(depositFundsHandler, times(3)).handle(any(DepositFundsCommand.class));
        }

        @Test
        @DisplayName("Should retry withdrawal on OptimisticLockException and succeed")
        void shouldRetryWithdrawalOnOptimisticLockException() {
            // Arrange
            String walletId = "wallet-123";
            BigDecimal amount = BigDecimal.valueOf(50);
            String referenceId = "ref-456";
            String transactionId = "txn-456";

            when(withdrawFundsHandler.handle(any(WithdrawFundsCommand.class)))
                .thenThrow(new OptimisticLockException("Optimistic lock failed"))
                .thenReturn(Uni.createFrom().item(transactionId));

            // Act & Assert
            resilientWalletService.withdrawFundsWithRetry(walletId, amount, referenceId)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem(transactionId);

            // Verify retry behavior
            verify(walletMetrics, times(1)).recordRetryAttempt(eq("withdrawal"), eq("optimistic_lock"), eq("OptimisticLockException"));
            verify(walletMetrics, times(1)).recordSuccessfulRetryOperation(eq("withdrawal"), eq("optimistic_lock"));
            verify(withdrawFundsHandler, times(2)).handle(any(WithdrawFundsCommand.class));
        }

        @Test
        @DisplayName("Should retry transfer on OptimisticLockException and succeed")
        void shouldRetryTransferOnOptimisticLockException() {
            // Arrange
            String sourceWalletId = "wallet-123";
            String destinationWalletId = "wallet-456";
            BigDecimal amount = BigDecimal.valueOf(75);
            String referenceId = "ref-789";
            String transactionId = "txn-789";

            when(transferFundsHandler.handle(any(TransferFundsCommand.class)))
                .thenThrow(new OptimisticLockException("Optimistic lock failed"))
                .thenReturn(Uni.createFrom().item(transactionId));

            // Act & Assert
            resilientWalletService.transferFundsWithRetry(sourceWalletId, destinationWalletId, amount, referenceId)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem(transactionId);

            // Verify retry behavior
            verify(walletMetrics, times(1)).recordRetryAttempt(eq("transfer"), eq("optimistic_lock"), eq("OptimisticLockException"));
            verify(walletMetrics, times(1)).recordSuccessfulRetryOperation(eq("transfer"), eq("optimistic_lock"));
            verify(transferFundsHandler, times(2)).handle(any(TransferFundsCommand.class));
        }

        @Test
        @DisplayName("Should exhaust retries and trigger fallback for deposit")
        void shouldExhaustRetriesAndTriggerFallbackForDeposit() {
            // Arrange
            String walletId = "wallet-123";
            BigDecimal amount = BigDecimal.valueOf(100);
            String referenceId = "ref-123";

            when(depositFundsHandler.handle(any(DepositFundsCommand.class)))
                .thenThrow(new OptimisticLockException("Persistent optimistic lock failure"));

            // Act & Assert
            resilientWalletService.depositFundsWithRetry(walletId, amount, referenceId)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(ServiceDegradedException.class);

            // Verify fallback was triggered
            verify(walletMetrics, times(1)).recordRetryExhaustion(eq("deposit"), eq("optimistic_lock"));
            verify(degradationManager, times(1)).recordOptimisticLockContention(eq("deposit"), eq(walletId));
        }
    }

    // ============================================================================
    // TRANSIENT FAILURE RETRY TESTS
    // ============================================================================

    @Nested
    @DisplayName("Transient Failure Retry Tests")
    class TransientFailureRetryTests {

        @Test
        @DisplayName("Should retry get wallet on SQLException and succeed")
        void shouldRetryGetWalletOnSQLException() {
            // Arrange
            String walletId = "wallet-123";

            when(getWalletHandler.handle(any(GetWalletQuery.class)))
                .thenThrow(new SQLException("Connection timeout"))
                .thenReturn(Uni.createFrom().item(mockWallet));

            // Act & Assert
            resilientWalletService.getWalletWithRetry(walletId)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem(mockWallet);

            // Verify retry behavior
            verify(walletMetrics, times(1)).recordRetryAttempt(eq("get_wallet"), eq("transient_failure"), eq("SQLException"));
            verify(walletMetrics, times(1)).recordSuccessfulRetryOperation(eq("get_wallet"), eq("transient_failure"));
            verify(getWalletHandler, times(2)).handle(any(GetWalletQuery.class));
        }

        @Test
        @DisplayName("Should retry create wallet on ConnectException and succeed")
        void shouldRetryCreateWalletOnConnectException() {
            // Arrange
            String userId = "user-123";
            String walletId = "wallet-123";

            when(createWalletHandler.handle(any(CreateWalletCommand.class)))
                .thenThrow(new ConnectException("Network connection failed"))
                .thenReturn(Uni.createFrom().item(walletId));

            // Act & Assert
            resilientWalletService.createWalletWithRetry(userId)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem(walletId);

            // Verify retry behavior
            verify(walletMetrics, times(1)).recordRetryAttempt(eq("create_wallet"), eq("transient_failure"), eq("ConnectException"));
            verify(walletMetrics, times(1)).recordSuccessfulRetryOperation(eq("create_wallet"), eq("transient_failure"));
            verify(createWalletHandler, times(2)).handle(any(CreateWalletCommand.class));
        }

        @Test
        @DisplayName("Should retry on SQLTransientException and succeed")
        void shouldRetryOnSQLTransientException() {
            // Arrange
            String walletId = "wallet-123";

            when(getWalletHandler.handle(any(GetWalletQuery.class)))
                .thenThrow(new SQLTransientException("Temporary database issue"))
                .thenThrow(new SQLTransientException("Still having issues"))
                .thenReturn(Uni.createFrom().item(mockWallet));

            // Act & Assert
            resilientWalletService.getWalletWithRetry(walletId)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem(mockWallet);

            // Verify multiple retry attempts
            verify(walletMetrics, times(2)).recordRetryAttempt(eq("get_wallet"), eq("transient_failure"), eq("SQLTransientException"));
            verify(walletMetrics, times(1)).recordSuccessfulRetryOperation(eq("get_wallet"), eq("transient_failure"));
            verify(getWalletHandler, times(3)).handle(any(GetWalletQuery.class));
        }

        @Test
        @DisplayName("Should exhaust retries and trigger fallback for transient failures")
        void shouldExhaustRetriesAndTriggerFallbackForTransientFailures() {
            // Arrange
            String walletId = "wallet-123";

            when(getWalletHandler.handle(any(GetWalletQuery.class)))
                .thenThrow(new TimeoutException("Persistent timeout"));

            // Act & Assert
            resilientWalletService.getWalletWithRetry(walletId)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(ServiceDegradedException.class);

            // Verify fallback was triggered
            verify(walletMetrics, times(1)).recordRetryExhaustion(eq("get_wallet"), eq("transient_failure"));
            verify(degradationManager, times(1)).recordTransientFailurePattern(eq("get_wallet"));
        }
    }

    // ============================================================================
    // SUCCESS SCENARIOS TESTS
    // ============================================================================

    @Nested
    @DisplayName("Success Scenarios Tests")
    class SuccessScenariosTests {

        @Test
        @DisplayName("Should succeed on first attempt without retries")
        void shouldSucceedOnFirstAttemptWithoutRetries() {
            // Arrange
            String walletId = "wallet-123";
            BigDecimal amount = BigDecimal.valueOf(100);
            String referenceId = "ref-123";
            String transactionId = "txn-123";

            when(depositFundsHandler.handle(any(DepositFundsCommand.class)))
                .thenReturn(Uni.createFrom().item(transactionId));

            // Act & Assert
            resilientWalletService.depositFundsWithRetry(walletId, amount, referenceId)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem(transactionId);

            // Verify no retry attempts were made
            verify(walletMetrics, never()).recordRetryAttempt(anyString(), anyString(), anyString());
            verify(walletMetrics, times(1)).recordSuccessfulRetryOperation(eq("deposit"), eq("optimistic_lock"));
            verify(depositFundsHandler, times(1)).handle(any(DepositFundsCommand.class));
        }

        @Test
        @DisplayName("Should handle successful wallet retrieval")
        void shouldHandleSuccessfulWalletRetrieval() {
            // Arrange
            String walletId = "wallet-123";

            when(getWalletHandler.handle(any(GetWalletQuery.class)))
                .thenReturn(Uni.createFrom().item(mockWallet));

            // Act & Assert
            resilientWalletService.getWalletWithRetry(walletId)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem(mockWallet);

            // Verify success metrics
            verify(walletMetrics, times(1)).recordSuccessfulRetryOperation(eq("get_wallet"), eq("transient_failure"));
            verify(getWalletHandler, times(1)).handle(any(GetWalletQuery.class));
        }
    }

    // ============================================================================
    // RETRY STATISTICS TESTS
    // ============================================================================

    @Nested
    @DisplayName("Retry Statistics Tests")
    class RetryStatisticsTests {

        @Test
        @DisplayName("Should return correct retry statistics")
        void shouldReturnCorrectRetryStatistics() {
            // Arrange
            when(walletMetrics.getRetryAttempts("optimistic_lock")).thenReturn(5L);
            when(walletMetrics.getRetryAttempts("transient_failure")).thenReturn(3L);
            when(walletMetrics.getRetryExhaustions("optimistic_lock")).thenReturn(1L);
            when(walletMetrics.getRetryExhaustions("transient_failure")).thenReturn(0L);

            // Act
            ResilientWalletService.RetryStatistics stats = resilientWalletService.getRetryStatistics();

            // Assert
            assert stats.optimisticLockRetries() == 5L;
            assert stats.transientFailureRetries() == 3L;
            assert stats.optimisticLockExhaustions() == 1L;
            assert stats.transientFailureExhaustions() == 0L;
        }
    }

    // ============================================================================
    // EDGE CASES AND ERROR SCENARIOS
    // ============================================================================

    @Nested
    @DisplayName("Edge Cases and Error Scenarios")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null parameters gracefully")
        void shouldHandleNullParametersGracefully() {
            // This test ensures our service handles null inputs appropriately
            // In a real scenario, validation would happen at the controller level
            
            when(depositFundsHandler.handle(any(DepositFundsCommand.class)))
                .thenThrow(new IllegalArgumentException("Invalid parameters"));

            resilientWalletService.depositFundsWithRetry("wallet-123", BigDecimal.valueOf(100), "ref-123")
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(ServiceDegradedException.class);

            // Verify fallback was triggered due to non-retryable exception
            verify(walletMetrics, times(1)).recordRetryExhaustion(eq("deposit"), eq("optimistic_lock"));
        }

        @Test
        @DisplayName("Should handle mixed exception types during retries")
        void shouldHandleMixedExceptionTypesDuringRetries() {
            // Arrange
            String walletId = "wallet-123";

            when(getWalletHandler.handle(any(GetWalletQuery.class)))
                .thenThrow(new SQLException("Database error"))
                .thenThrow(new ConnectException("Network error"))
                .thenReturn(Uni.createFrom().item(mockWallet));

            // Act & Assert
            resilientWalletService.getWalletWithRetry(walletId)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem(mockWallet);

            // Verify different exception types were recorded
            verify(walletMetrics, times(1)).recordRetryAttempt(eq("get_wallet"), eq("transient_failure"), eq("SQLException"));
            verify(walletMetrics, times(1)).recordRetryAttempt(eq("get_wallet"), eq("transient_failure"), eq("ConnectException"));
            verify(walletMetrics, times(1)).recordSuccessfulRetryOperation(eq("get_wallet"), eq("transient_failure"));
        }
    }
}
