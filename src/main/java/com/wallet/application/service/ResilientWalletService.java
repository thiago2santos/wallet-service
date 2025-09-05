package com.wallet.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import io.smallrye.mutiny.Uni;
import java.math.BigDecimal;

/**
 * Resilient Wallet Service with comprehensive retry strategies
 * 
 * This service wraps the existing CQRS handlers with retry mechanisms for:
 * - Optimistic lock conflicts during concurrent balance updates
 * - Transient database failures
 * - Network timeouts and connection issues
 * 
 * Financial Service Requirements:
 * - Zero data loss during retries
 * - Idempotent operations to prevent duplicate transactions
 * - Comprehensive metrics for monitoring retry patterns
 * - Graceful degradation when retries are exhausted
 */
@ApplicationScoped
public class ResilientWalletService {

    private static final Logger logger = LoggerFactory.getLogger(ResilientWalletService.class);

    @Inject
    CreateWalletCommandHandler createWalletHandler;

    @Inject
    DepositFundsCommandHandler depositFundsHandler;

    @Inject
    WithdrawFundsCommandHandler withdrawFundsHandler;

    @Inject
    TransferFundsCommandHandler transferFundsHandler;

    @Inject
    GetWalletQueryHandler getWalletHandler;

    @Inject
    WalletMetrics walletMetrics;

    @Inject
    DegradationManager degradationManager;

    // ============================================================================
    // OPTIMISTIC LOCK RETRY OPERATIONS
    // ============================================================================

    /**
     * Deposit funds with optimistic lock retry
     * Critical for handling concurrent deposit operations on the same wallet
     */
    @Retry
    @Fallback(fallbackMethod = "depositFundsOptimisticLockFallback")
    public Uni<String> depositFundsWithRetry(String walletId, BigDecimal amount, String referenceId) {
        logger.debug("Attempting deposit with optimistic lock retry: walletId={}, amount={}, referenceId={}", 
                    walletId, amount, referenceId);
        
        DepositFundsCommand command = new DepositFundsCommand(walletId, amount, referenceId);
        return depositFundsHandler.handle(command)
                .onItem().invoke(transactionId -> {
                    walletMetrics.recordSuccessfulRetryOperation("deposit", "optimistic_lock");
                    logger.debug("Deposit successful after retry: walletId={}, transactionId={}", 
                               walletId, transactionId);
                })
                .onFailure().invoke(throwable -> {
                    walletMetrics.recordRetryAttempt("deposit", "optimistic_lock", throwable.getClass().getSimpleName());
                    logger.warn("Deposit retry attempt failed: walletId={}, error={}", 
                              walletId, throwable.getMessage());
                });
    }

    /**
     * Withdraw funds with optimistic lock retry
     * Critical for handling concurrent withdrawal operations
     */
    @Retry
    @Fallback(fallbackMethod = "withdrawFundsOptimisticLockFallback")
    public Uni<String> withdrawFundsWithRetry(String walletId, BigDecimal amount, String referenceId) {
        logger.debug("Attempting withdrawal with optimistic lock retry: walletId={}, amount={}, referenceId={}", 
                    walletId, amount, referenceId);
        
        WithdrawFundsCommand command = new WithdrawFundsCommand(walletId, amount, referenceId);
        return withdrawFundsHandler.handle(command)
                .onItem().invoke(transactionId -> {
                    walletMetrics.recordSuccessfulRetryOperation("withdrawal", "optimistic_lock");
                    logger.debug("Withdrawal successful after retry: walletId={}, transactionId={}", 
                               walletId, transactionId);
                })
                .onFailure().invoke(throwable -> {
                    walletMetrics.recordRetryAttempt("withdrawal", "optimistic_lock", throwable.getClass().getSimpleName());
                    logger.warn("Withdrawal retry attempt failed: walletId={}, error={}", 
                              walletId, throwable.getMessage());
                });
    }

    /**
     * Transfer funds with optimistic lock retry
     * Most critical operation - affects two wallets simultaneously
     */
    @Retry
    @Fallback(fallbackMethod = "transferFundsOptimisticLockFallback")
    public Uni<String> transferFundsWithRetry(String sourceWalletId, String destinationWalletId, 
                                            BigDecimal amount, String referenceId) {
        logger.debug("Attempting transfer with optimistic lock retry: source={}, destination={}, amount={}, referenceId={}", 
                    sourceWalletId, destinationWalletId, amount, referenceId);
        
        TransferFundsCommand command = new TransferFundsCommand(sourceWalletId, destinationWalletId, amount, referenceId);
        return transferFundsHandler.handle(command)
                .onItem().invoke(transactionId -> {
                    walletMetrics.recordSuccessfulRetryOperation("transfer", "optimistic_lock");
                    logger.debug("Transfer successful after retry: source={}, destination={}, transactionId={}", 
                               sourceWalletId, destinationWalletId, transactionId);
                })
                .onFailure().invoke(throwable -> {
                    walletMetrics.recordRetryAttempt("transfer", "optimistic_lock", throwable.getClass().getSimpleName());
                    logger.warn("Transfer retry attempt failed: source={}, destination={}, error={}", 
                              sourceWalletId, destinationWalletId, throwable.getMessage());
                });
    }

    // ============================================================================
    // TRANSIENT FAILURE RETRY OPERATIONS
    // ============================================================================

    /**
     * Get wallet with transient failure retry
     * Handles network timeouts and temporary database issues
     */
    @Retry
    @Fallback(fallbackMethod = "getWalletTransientFailureFallback")
    public Uni<Wallet> getWalletWithRetry(String walletId) {
        logger.debug("Getting wallet with transient failure retry: walletId={}", walletId);
        
        GetWalletQuery query = new GetWalletQuery(walletId);
        return getWalletHandler.handle(query)
                .onItem().invoke(wallet -> {
                    walletMetrics.recordSuccessfulRetryOperation("get_wallet", "transient_failure");
                })
                .onFailure().invoke(throwable -> {
                    walletMetrics.recordRetryAttempt("get_wallet", "transient_failure", throwable.getClass().getSimpleName());
                    logger.warn("Get wallet retry attempt failed: walletId={}, error={}", 
                              walletId, throwable.getMessage());
                });
    }

    /**
     * Create wallet with transient failure retry
     * Ensures wallet creation succeeds despite temporary issues
     */
    @Retry
    @Fallback(fallbackMethod = "createWalletTransientFailureFallback")
    public Uni<String> createWalletWithRetry(String userId) {
        logger.debug("Creating wallet with transient failure retry: userId={}", userId);
        
        CreateWalletCommand command = new CreateWalletCommand(userId);
        return createWalletHandler.handle(command)
                .onItem().invoke(walletId -> {
                    walletMetrics.recordSuccessfulRetryOperation("create_wallet", "transient_failure");
                    logger.debug("Wallet creation successful after retry: walletId={}, userId={}", 
                               walletId, userId);
                })
                .onFailure().invoke(throwable -> {
                    walletMetrics.recordRetryAttempt("create_wallet", "transient_failure", throwable.getClass().getSimpleName());
                    logger.warn("Create wallet retry attempt failed: userId={}, error={}", 
                              userId, throwable.getMessage());
                });
    }

    // ============================================================================
    // FALLBACK METHODS
    // ============================================================================

    /**
     * Fallback for optimistic lock retry exhaustion on deposits
     */
    public Uni<String> depositFundsOptimisticLockFallback(String walletId, BigDecimal amount, String referenceId) {
        logger.error("Optimistic lock retries exhausted for deposit: walletId={}, amount={}, referenceId={}", 
                    walletId, amount, referenceId);
        
        walletMetrics.recordRetryExhaustion("deposit", "optimistic_lock");
        degradationManager.recordOptimisticLockContention("deposit", walletId);
        
        return Uni.createFrom().failure(
            new ServiceDegradedException(
                "Unable to complete deposit due to high concurrency. Please try again.", 
                "OPTIMISTIC_LOCK_EXHAUSTED"
            )
        );
    }

    /**
     * Fallback for optimistic lock retry exhaustion on withdrawals
     */
    public Uni<String> withdrawFundsOptimisticLockFallback(String walletId, BigDecimal amount, String referenceId) {
        logger.error("Optimistic lock retries exhausted for withdrawal: walletId={}, amount={}, referenceId={}", 
                    walletId, amount, referenceId);
        
        walletMetrics.recordRetryExhaustion("withdrawal", "optimistic_lock");
        degradationManager.recordOptimisticLockContention("withdrawal", walletId);
        
        return Uni.createFrom().failure(
            new ServiceDegradedException(
                "Unable to complete withdrawal due to high concurrency. Please try again.", 
                "OPTIMISTIC_LOCK_EXHAUSTED"
            )
        );
    }

    /**
     * Fallback for optimistic lock retry exhaustion on transfers
     */
    public Uni<String> transferFundsOptimisticLockFallback(String sourceWalletId, String destinationWalletId, 
                                                         BigDecimal amount, String referenceId) {
        logger.error("Optimistic lock retries exhausted for transfer: source={}, destination={}, amount={}, referenceId={}", 
                    sourceWalletId, destinationWalletId, amount, referenceId);
        
        walletMetrics.recordRetryExhaustion("transfer", "optimistic_lock");
        degradationManager.recordOptimisticLockContention("transfer", sourceWalletId + "," + destinationWalletId);
        
        return Uni.createFrom().failure(
            new ServiceDegradedException(
                "Unable to complete transfer due to high concurrency. Please try again.", 
                "OPTIMISTIC_LOCK_EXHAUSTED"
            )
        );
    }

    /**
     * Fallback for transient failure retry exhaustion on wallet retrieval
     */
    public Uni<Wallet> getWalletTransientFailureFallback(String walletId) {
        logger.error("Transient failure retries exhausted for get wallet: walletId={}", walletId);
        
        walletMetrics.recordRetryExhaustion("get_wallet", "transient_failure");
        degradationManager.recordTransientFailurePattern("get_wallet");
        
        return Uni.createFrom().failure(
            new ServiceDegradedException(
                "Wallet service temporarily unavailable. Please try again later.", 
                "TRANSIENT_FAILURE_EXHAUSTED"
            )
        );
    }

    /**
     * Fallback for transient failure retry exhaustion on wallet creation
     */
    public Uni<String> createWalletTransientFailureFallback(String userId) {
        logger.error("Transient failure retries exhausted for create wallet: userId={}", userId);
        
        walletMetrics.recordRetryExhaustion("create_wallet", "transient_failure");
        degradationManager.recordTransientFailurePattern("create_wallet");
        
        return Uni.createFrom().failure(
            new ServiceDegradedException(
                "Wallet creation temporarily unavailable. Please try again later.", 
                "TRANSIENT_FAILURE_EXHAUSTED"
            )
        );
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    /**
     * Get retry statistics for monitoring
     */
    public RetryStatistics getRetryStatistics() {
        return new RetryStatistics(
            walletMetrics.getRetryAttempts("optimistic_lock"),
            walletMetrics.getRetryAttempts("transient_failure"),
            walletMetrics.getRetryExhaustions("optimistic_lock"),
            walletMetrics.getRetryExhaustions("transient_failure")
        );
    }

    /**
     * Statistics record for retry monitoring
     */
    public record RetryStatistics(
        long optimisticLockRetries,
        long transientFailureRetries,
        long optimisticLockExhaustions,
        long transientFailureExhaustions
    ) {}
}