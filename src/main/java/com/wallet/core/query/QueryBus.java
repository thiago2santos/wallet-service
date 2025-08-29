package com.wallet.core.query;

import io.smallrye.mutiny.Uni;

public interface QueryBus {
    <T extends Query<R>, R> Uni<R> dispatch(T query);
}
