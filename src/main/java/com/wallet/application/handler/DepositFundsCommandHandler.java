package com.wallet.application.handler;

import java.util.UUID;

import com.wallet.application.command.DepositFundsCommand;
import com.wallet.core.command.CommandHandler;
import com.wallet.domain.model.Transaction;
import com.wallet.domain.model.TransactionStatus;
import com.wallet.domain.model.TransactionType;
import com.wallet.infrastructure.persistence.TransactionRepository;
import com.wallet.infrastructure.persistence.WalletReadRepository;
import com.wallet.infrastructure.persistence.WalletRepository;
import com.wallet.infrastructure.cache.WalletStateCache;
import com.wallet.infrastructure.metrics.WalletMetrics;
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

    @Override
    @Transactional
    public Uni<String> handle(DepositFundsCommand command) {
        // Start metrics timer
        var timer = walletMetrics.startDepositTimer();
        
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

                // Persist both wallet and transaction, then invalidate cache
                return walletWriteRepository.persist(wallet)
                    .chain(() -> {
                        System.out.println("DepositFundsCommandHandler: Wallet persisted, now persisting transaction");
                        return transactionRepository.persist(transaction);
                    })
                    .chain(() -> {
                        System.out.println("DepositFundsCommandHandler: Transaction persisted, now invalidating cache");
                        return walletCache.invalidateWallet(command.getWalletId());
                    })
                    .map(v -> {
                        System.out.println("DepositFundsCommandHandler: Cache invalidated, returning ID: " + transactionId);
                        // Record successful deposit metrics
                        walletMetrics.incrementDeposits();
                        walletMetrics.recordDepositAmount(command.getAmount());
                        walletMetrics.recordDeposit(timer);
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