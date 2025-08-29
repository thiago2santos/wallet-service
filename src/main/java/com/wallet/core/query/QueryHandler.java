package com.wallet.core.query;

import io.smallrye.mutiny.Uni;

public interface QueryHandler<Q extends Query, R> {
    Uni<R> handle(Q query);
}