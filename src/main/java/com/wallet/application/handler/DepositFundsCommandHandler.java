package com.wallet.application.handler;

import com.wallet.application.command.DepositFundsCommand;
import com.wallet.core.command.CommandHandler;
import com.wallet.domain.event.WalletEvent;
import com.wallet.domain.event.WalletEventType;
import com.wallet.infrastructure.event.WalletEventStore;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;

@ApplicationScoped
public class DepositFundsCommandHandler implements CommandHandler<DepositFundsCommand, Void> {

    @Inject
    WalletEventStore eventStore;

    @Override
    public Uni<Void> handle(DepositFundsCommand command) {
        WalletEvent event = new WalletEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(WalletEventType.FUNDS_DEPOSITED);
        event.setWalletId(command.getWalletId());
        event.setAmount(command.getAmount());
        event.setTransactionId(command.getReferenceId());
        
        return eventStore.store(event);
    }
}