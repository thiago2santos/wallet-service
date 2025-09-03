package com.wallet.application.handler;

import java.math.BigDecimal;
import java.util.UUID;

import com.wallet.application.command.DepositFundsCommand;
import com.wallet.core.command.CommandHandler;
import com.wallet.domain.event.FundsDepositedEvent;
import com.wallet.domain.model.Transaction;
import com.wallet.domain.model.TransactionStatus;
import com.wallet.domain.model.TransactionType;
import com.wallet.infrastructure.persistence.TransactionRepository;
import com.wallet.infrastructure.persistence.WalletReadRepository;
import com.wallet.infrastructure.persistence.WalletRepository;
import com.wallet.infrastructure.cache.WalletStateCache;
import com.wallet.infrastructure.metrics.WalletMetrics;
import com.wallet.infrastructure.outbox.OutboxEventService;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class DepositFundsCommandHandler implements CommandHandler<DepositFundsCommand, String> {

    @Inject
    @ReactiveDataSource("read")
    WalletReadRepository walletReadRepository;

    @Inject
    @ReactiveDataSource("write")
    WalletRepository walletWriteRepository;

    @Inject
    @ReactiveDataSource("write")
    TransactionRepository transactionRepository;

    @Inject
    WalletStateCache walletCache;
    
    @Inject
    WalletMetrics walletMetrics;

    @Inject
    OutboxEventService outboxEventService;

    @Override
    @Transactional
    public Uni<String> handle(DepositFundsCommand command) {
        // Start metrics timer
        var timer = walletMetrics.startDepositTimer();
        
        // Validate amount is positive
        if (command.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Uni.createFrom().failure(
                new IllegalArgumentException("Deposit amount must be positive")
            );
        }
        
        String transactionId = UUID.randomUUID().toString();

        // First, check if wallet exists and get current balance
        return walletReadRepository.findById(command.getWalletId())
            .onItem().ifNull().failWith(() -> new IllegalArgumentException("Wallet not found: " + command.getWalletId()))
            .chain(wallet -> {
                System.out.println("DepositFundsCommandHandler: Found wallet with balance: " + wallet.getBalance());
                
                // Update wallet balance
                wallet.setBalance(wallet.getBalance().add(command.getAmount()));
                wallet.setUpdatedAt(java.time.Instant.now());
                
                System.out.println("DepositFundsCommandHandler: Updated balance to: " + wallet.getBalance());

                // Create transaction record
                Transaction transaction = new Transaction(
                    transactionId,
                    command.getWalletId(),
                    TransactionType.DEPOSIT,
                    command.getAmount(),
                    command.getReferenceId(),
                    TransactionStatus.COMPLETED
                );
                transaction.setDescription("Deposit to wallet");

                // Create event for publishing
                FundsDepositedEvent event = new FundsDepositedEvent(
                    command.getWalletId(),
                    transactionId,
                    command.getAmount(),
                    command.getReferenceId(),
                    command.getDescription()
                );

                // Persist wallet, transaction, and event in same database transaction
                return walletWriteRepository.persist(wallet)
                    .chain(() -> {
                        System.out.println("DepositFundsCommandHandler: Wallet persisted, now persisting transaction");
                        return transactionRepository.persist(transaction);
                    })
                    .chain(() -> {
                        System.out.println("DepositFundsCommandHandler: Transaction persisted, now storing event");
                        return outboxEventService.storeWalletEvent(
                            command.getWalletId(),
                            "FundsDeposited",
                            event
                        );
                    })
                    .chain(() -> {
                        System.out.println("DepositFundsCommandHandler: Event stored, now invalidating cache");
                        return walletCache.invalidateWallet(command.getWalletId());
                    })
                    .map(v -> {
                        System.out.println("DepositFundsCommandHandler: Cache invalidated, returning ID: " + transactionId);
                        // Record successful deposit metrics
                        walletMetrics.incrementDeposits();
                        walletMetrics.recordDepositAmount(command.getAmount());
                        walletMetrics.recordDeposit(timer);
                        walletMetrics.recordEventPublished("FUNDS_DEPOSITED");
                        return transactionId;
                    });
            })
            .onFailure().invoke(throwable -> {
                // Record failed deposit
                walletMetrics.incrementFailedOperations("deposit");
                walletMetrics.recordDeposit(timer);
            });
    }
}