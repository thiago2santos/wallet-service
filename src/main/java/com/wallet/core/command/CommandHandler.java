package com.wallet.core.command;

import io.smallrye.mutiny.Uni;

public interface CommandHandler<C extends Command, R> {
    Uni<R> handle(C command);
}