package com.wallet.infrastructure.event;

import com.wallet.domain.event.WalletEvent;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;

@ApplicationScoped
public class WalletEventStore {
    
    @Inject
    @Channel("wallet-events")
    MutinyEmitter<WalletEvent> eventEmitter;

    public Uni<Void> store(WalletEvent event) {
        return eventEmitter.send(event);
    }
}