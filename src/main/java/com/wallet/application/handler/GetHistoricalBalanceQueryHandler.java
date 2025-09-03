package com.wallet.application.handler;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.wallet.application.query.GetHistoricalBalanceQuery;
import com.wallet.core.query.QueryHandler;
import com.wallet.domain.model.Transaction;
import com.wallet.domain.model.TransactionType;
import com.wallet.dto.HistoricalBalanceResponse;
import com.wallet.infrastructure.persistence.TransactionRepository;
import com.wallet.infrastructure.persistence.WalletReadRepository;

import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GetHistoricalBalanceQueryHandler implements QueryHandler<GetHistoricalBalanceQuery, HistoricalBalanceResponse> {

    @Inject
    @ReactiveDataSource("write")
    WalletReadRepository walletReadRepository;

    @Inject
    @ReactiveDataSource("write")
    TransactionRepository transactionRepository;

    @Override
    public Uni<HistoricalBalanceResponse> handle(GetHistoricalBalanceQuery query) {
        System.out.println("GetHistoricalBalanceQueryHandler: Processing query for wallet " + query.getWalletId() + " at timestamp " + query.getTimestamp());

        // First, verify the wallet exists
        return walletReadRepository.findById(query.getWalletId())
            .onItem().ifNull().failWith(() -> new IllegalArgumentException("Wallet not found: " + query.getWalletId()))
            .chain(wallet -> {
                System.out.println("GetHistoricalBalanceQueryHandler: Found wallet");
                
                // Get all transactions up to the specified timestamp, ordered by creation time
                return transactionRepository.find(
                    "walletId = ?1 and createdAt <= ?2 order by createdAt asc", 
                    query.getWalletId(), 
                    query.getTimestamp()
                ).list()
                .map(transactions -> {
                    System.out.println("GetHistoricalBalanceQueryHandler: Found " + transactions.size() + " transactions up to " + query.getTimestamp());
                    
                    // Calculate balance by replaying all transactions chronologically
                    BigDecimal historicalBalance = calculateBalanceFromTransactions(transactions);
                    
                    System.out.println("GetHistoricalBalanceQueryHandler: Calculated historical balance: " + historicalBalance);
                    
                    return new HistoricalBalanceResponse(
                        query.getWalletId(),
                        historicalBalance,
                        query.getTimestamp()
                    );
                });
            });
    }

    /**
     * Calculates the balance by replaying transactions chronologically.
     * This is the core "transaction replay" logic.
     */
    private BigDecimal calculateBalanceFromTransactions(List<Transaction> transactions) {
        BigDecimal balance = BigDecimal.ZERO;
        
        for (Transaction transaction : transactions) {
            switch (transaction.getType()) {
                case DEPOSIT:
                    balance = balance.add(transaction.getAmount());
                    System.out.println("  + DEPOSIT: " + transaction.getAmount() + " -> Balance: " + balance);
                    break;
                case WITHDRAWAL:
                    balance = balance.subtract(transaction.getAmount());
                    System.out.println("  - WITHDRAWAL: " + transaction.getAmount() + " -> Balance: " + balance);
                    break;
                case TRANSFER:
                    // For transfers, we need to check if this wallet is source or destination
                    if (transaction.getDestinationWalletId() != null && 
                        transaction.getDestinationWalletId().equals(transaction.getWalletId())) {
                        // This wallet is the destination - add the amount
                        balance = balance.add(transaction.getAmount());
                        System.out.println("  + TRANSFER IN: " + transaction.getAmount() + " -> Balance: " + balance);
                    } else {
                        // This wallet is the source - subtract the amount
                        balance = balance.subtract(transaction.getAmount());
                        System.out.println("  - TRANSFER OUT: " + transaction.getAmount() + " -> Balance: " + balance);
                    }
                    break;
                default:
                    System.out.println("  ? UNKNOWN TYPE: " + transaction.getType() + " - skipping");
                    break;
            }
        }
        
        return balance;
    }
}
