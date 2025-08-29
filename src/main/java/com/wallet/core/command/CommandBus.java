package com.wallet.core.command;

import io.smallrye.mutiny.Uni;

public interface CommandBus {
    <T extends Command, R> Uni<R> dispatch(T command);
}
