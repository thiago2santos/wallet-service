package com.wallet.application.handler;

import java.util.UUID;

import org.eclipse.microprofile.reactive.messaging.Channel;

import com.wallet.application.command.CreateWalletCommand;
import com.wallet.core.command.CommandHandler;
import com.wallet.domain.event.WalletCreatedEvent;
import com.wallet.infrastructure.event.WalletEventStore;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CreateWalletCommandHandler implements CommandHandler<CreateWalletCommand, String> {
    @Inject
    WalletEventStore eventStore;

    @Inject
    @Channel("wallet-events")
    MutinyEmitter<WalletCreatedEvent> eventEmitter;

    @Override
    public Uni<String> handle(CreateWalletCommand command) {
        String walletId = UUID.randomUUID().toString();
        
        // Create and emit event for event-driven wallet creation
        WalletCreatedEvent event = new WalletCreatedEvent(
            walletId,
            command.getUserId(),
            command.getCurrency()
        );

        // Use event sourcing approach - emit event instead of direct DB write
        return eventStore.store(event)
            .map(v -> walletId);
    }
}