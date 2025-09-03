package com.wallet.application.handler;

import java.math.BigDecimal;
import java.util.UUID;

import com.wallet.application.command.CreateWalletCommand;
import com.wallet.core.command.CommandHandler;
import com.wallet.domain.event.WalletCreatedEvent;
import com.wallet.domain.model.Wallet;
import com.wallet.domain.model.WalletStatus;
import com.wallet.infrastructure.persistence.WalletRepository;
import com.wallet.infrastructure.metrics.WalletMetrics;
import com.wallet.infrastructure.outbox.OutboxEventService;

import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CreateWalletCommandHandler implements CommandHandler<CreateWalletCommand, String> {
    @Inject
    @ReactiveDataSource("write")
    WalletRepository writeRepository;

    @Inject
    OutboxEventService outboxEventService;
    
    @Inject
    WalletMetrics walletMetrics;

    @Override
    @Transactional
    public Uni<String> handle(CreateWalletCommand command) {
        // Start metrics timer
        var timer = walletMetrics.startWalletCreationTimer();
        
        String walletId = UUID.randomUUID().toString();
        
        // Create event for publishing to Kafka via outbox
        WalletCreatedEvent event = new WalletCreatedEvent(
            walletId,
            command.getUserId()
        );

        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setUserId(command.getUserId());

        wallet.setBalance(BigDecimal.ZERO);
        wallet.setStatus(WalletStatus.ACTIVE.name());
        wallet.setCreatedAt(java.time.Instant.now());
        wallet.setUpdatedAt(java.time.Instant.now());

        // FIXED: Use outbox pattern for reliable event publishing
        // Both wallet and event are stored in the same database transaction
        return writeRepository.persist(wallet)
            .chain(() -> outboxEventService.storeWalletEvent(
                walletId, 
                "WalletCreated", 
                event
            ))
            .map(v -> {
                // Record successful wallet creation
                walletMetrics.incrementWalletsCreated();
                walletMetrics.recordWalletCreation(timer);
                walletMetrics.recordEventPublished("WALLET_CREATED");
                return wallet.getId();
            })
            .onFailure().invoke(throwable -> {
                // Record failed operation
                walletMetrics.incrementFailedOperations("wallet_creation");
                walletMetrics.recordWalletCreation(timer);
            });
    }
}