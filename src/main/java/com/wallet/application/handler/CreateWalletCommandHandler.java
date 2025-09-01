package com.wallet.application.handler;

import com.wallet.application.command.CreateWalletCommand;
import com.wallet.core.command.CommandHandler;
import com.wallet.domain.model.Wallet;
import com.wallet.domain.model.WalletStatus;
import com.wallet.domain.event.WalletCreatedEvent;
import com.wallet.infrastructure.persistence.WalletRepository;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Channel;
import java.math.BigDecimal;
import java.util.UUID;

@ApplicationScoped
public class CreateWalletCommandHandler implements CommandHandler<CreateWalletCommand, String> {
    @Inject
    @ReactiveDataSource("write")
    WalletRepository writeRepository;

    // Temporarily disabled for create wallet focus
    // @Inject
    // @Channel("wallet-events")
    // MutinyEmitter<WalletCreatedEvent> eventEmitter;

    @Override
    @Transactional
    public Uni<String> handle(CreateWalletCommand command) {
        String walletId = UUID.randomUUID().toString();
        
        // Create event for future use (currently not emitted)
        WalletCreatedEvent event = new WalletCreatedEvent(
            walletId,
            command.getUserId(),
            command.getCurrency()
        );

        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setUserId(command.getUserId());
        wallet.setCurrency(command.getCurrency());
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setStatus(WalletStatus.ACTIVE.name());
        wallet.setCreatedAt(java.time.Instant.now());
        wallet.setUpdatedAt(java.time.Instant.now());

        return writeRepository.persist(wallet)
            // .chain(() -> eventEmitter.send(event)) // Disabled for now
            .map(v -> wallet.getId());
    }
}