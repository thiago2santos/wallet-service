package com.wallet.application.handler;

import java.math.BigDecimal;
import java.util.UUID;

import com.wallet.application.command.TransferFundsCommand;
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
public class TransferFundsCommandHandler implements CommandHandler<TransferFundsCommand, String> {

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
    public Uni<String> handle(TransferFundsCommand command) {
        String transactionId = UUID.randomUUID().toString();

        // Validate amount is positive
        if (command.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Uni.createFrom().failure(
                InvalidTransferException.invalidAmount()
            );
        }

        // Validate source and destination are different
        if (command.getSourceWalletId().equals(command.getDestinationWalletId())) {
            return Uni.createFrom().failure(
                InvalidTransferException.sameWallet()
            );
        }

        // First, verify source wallet exists and get its current balance
        return walletReadRepository.findById(command.getSourceWalletId())
            .chain(sourceWallet -> {
                if (sourceWallet == null) {
                    return Uni.createFrom().failure(
                        new WalletNotFoundException(command.getSourceWalletId())
                    );
                }

                // Then verify destination wallet exists
                return walletReadRepository.findById(command.getDestinationWalletId())
                    .chain(destinationWallet -> {
                        if (destinationWallet == null) {
                            return Uni.createFrom().failure(
                                new WalletNotFoundException(command.getDestinationWalletId())
                            );
                        }

                System.out.println("TransferFundsCommandHandler: Source wallet balance: " + sourceWallet.getBalance());
                System.out.println("TransferFundsCommandHandler: Destination wallet balance: " + destinationWallet.getBalance());

                // Check if sufficient funds are available in source wallet
                if (sourceWallet.getBalance().compareTo(command.getAmount()) < 0) {
                    return Uni.createFrom().failure(
                        new InsufficientFundsException(sourceWallet.getBalance(), command.getAmount())
                    );
                }

                // Update both wallet balances
                sourceWallet.setBalance(sourceWallet.getBalance().subtract(command.getAmount()));
                sourceWallet.setUpdatedAt(java.time.Instant.now());

                destinationWallet.setBalance(destinationWallet.getBalance().add(command.getAmount()));
                destinationWallet.setUpdatedAt(java.time.Instant.now());

                System.out.println("TransferFundsCommandHandler: Updated source balance to: " + sourceWallet.getBalance());
                System.out.println("TransferFundsCommandHandler: Updated destination balance to: " + destinationWallet.getBalance());

                // Create transaction record
                Transaction transaction = new Transaction(
                    transactionId,
                    command.getSourceWalletId(),
                    TransactionType.TRANSFER,
                    command.getAmount(),
                    command.getReferenceId(),
                    TransactionStatus.COMPLETED
                );
                transaction.setDestinationWalletId(command.getDestinationWalletId());
                transaction.setDescription("Transfer between wallets");

                // Persist both wallets and transaction sequentially, then invalidate cache for both wallets
                return walletWriteRepository.persist(sourceWallet)
                    .chain(() -> {
                        System.out.println("TransferFundsCommandHandler: Source wallet persisted");
                        return walletWriteRepository.persist(destinationWallet);
                    })
                    .chain(() -> {
                        System.out.println("TransferFundsCommandHandler: Destination wallet persisted, now persisting transaction");
                        return transactionRepository.persist(transaction);
                    })
                    .chain(() -> {
                        System.out.println("TransferFundsCommandHandler: Transaction persisted, now invalidating caches");
                        return walletCache.invalidateWallet(command.getSourceWalletId());
                    })
                    .chain(() -> {
                        return walletCache.invalidateWallet(command.getDestinationWalletId());
                    })
                    .map(v -> {
                        System.out.println("TransferFundsCommandHandler: Caches invalidated, returning ID: " + transactionId);
                        return transactionId;
                    });
                    });
            });
    }
}
