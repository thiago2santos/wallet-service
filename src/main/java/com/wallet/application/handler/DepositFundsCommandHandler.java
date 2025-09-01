package com.wallet.application.handler;

import com.wallet.application.command.DepositFundsCommand;
import com.wallet.core.command.CommandHandler;
import com.wallet.domain.model.Transaction;
import com.wallet.domain.model.TransactionStatus;
import com.wallet.domain.model.TransactionType;
import com.wallet.domain.model.Wallet;
import com.wallet.infrastructure.persistence.TransactionRepository;
import com.wallet.infrastructure.persistence.WalletRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class DepositFundsCommandHandler implements CommandHandler<DepositFundsCommand, String> {

    @Inject
    WalletRepository walletRepository;

    @Inject
    TransactionRepository transactionRepository;

    @Override
    @Transactional
    public Uni<String> handle(DepositFundsCommand command) {
        String transactionId = UUID.randomUUID().toString();

        // First, check if wallet exists and get current balance
        return walletRepository.find("id", command.getWalletId()).firstResult()
            .onItem().ifNull().failWith(() -> new IllegalArgumentException("Wallet not found: " + command.getWalletId()))
            .chain(wallet -> {
                // Update wallet balance
                wallet.setBalance(wallet.getBalance().add(command.getAmount()));
                wallet.setUpdatedAt(java.time.Instant.now());

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

                // Persist both wallet and transaction
                return walletRepository.persist(wallet)
                    .chain(() -> transactionRepository.persist(transaction))
                    .map(v -> transactionId);
            });
    }
}