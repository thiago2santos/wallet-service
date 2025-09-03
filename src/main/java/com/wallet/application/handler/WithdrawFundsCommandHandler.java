package com.wallet.application.handler;

import java.math.BigDecimal;
import java.util.UUID;

import com.wallet.application.command.WithdrawFundsCommand;
import com.wallet.core.command.CommandHandler;
import com.wallet.domain.model.Transaction;
import com.wallet.domain.model.TransactionStatus;
import com.wallet.domain.model.TransactionType;
import com.wallet.infrastructure.cache.WalletStateCache;
import com.wallet.infrastructure.persistence.TransactionRepository;
import com.wallet.infrastructure.persistence.WalletReadRepository;
import com.wallet.infrastructure.persistence.WalletRepository;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.exception.InsufficientFundsException;
import com.wallet.exception.InvalidTransferException;

import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class WithdrawFundsCommandHandler implements CommandHandler<WithdrawFundsCommand, String> {

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

    @Override
    @Transactional
    public Uni<String> handle(WithdrawFundsCommand command) {
        String transactionId = UUID.randomUUID().toString();

        // Validate amount is positive
        if (command.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Uni.createFrom().failure(
                InvalidTransferException.invalidAmount()
            );
        }

        // First, check if wallet exists and get current balance
        return walletReadRepository.findById(command.getWalletId())
            .onItem().ifNull().failWith(() -> new WalletNotFoundException(command.getWalletId()))
            .chain(wallet -> {
                System.out.println("WithdrawFundsCommandHandler: Found wallet with balance: " + wallet.getBalance());
                
                // Check if sufficient funds are available
                if (wallet.getBalance().compareTo(command.getAmount()) < 0) {
                    return Uni.createFrom().failure(
                        new InsufficientFundsException(wallet.getBalance(), command.getAmount())
                    );
                }
                
                // Update wallet balance
                wallet.setBalance(wallet.getBalance().subtract(command.getAmount()));
                wallet.setUpdatedAt(java.time.Instant.now());
                
                System.out.println("WithdrawFundsCommandHandler: Updated balance to: " + wallet.getBalance());

                // Create transaction record
                Transaction transaction = new Transaction(
                    transactionId,
                    command.getWalletId(),
                    TransactionType.WITHDRAWAL,
                    command.getAmount(),
                    command.getReferenceId(),
                    TransactionStatus.COMPLETED
                );
                transaction.setDescription("Withdrawal from wallet");

                // Persist both wallet and transaction, then invalidate cache
                return walletWriteRepository.persist(wallet)
                    .chain(() -> {
                        System.out.println("WithdrawFundsCommandHandler: Wallet persisted, now persisting transaction");
                        return transactionRepository.persist(transaction);
                    })
                    .chain(() -> {
                        System.out.println("WithdrawFundsCommandHandler: Transaction persisted, now invalidating cache");
                        return walletCache.invalidateWallet(command.getWalletId());
                    })
                    .map(v -> {
                        System.out.println("WithdrawFundsCommandHandler: Cache invalidated, returning ID: " + transactionId);
                        return transactionId;
                    });
            });
    }
}
