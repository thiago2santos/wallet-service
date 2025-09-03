package com.wallet.infrastructure.persistence;

import com.wallet.domain.model.Wallet;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ReactiveDataSource("read")  // Use replica database for read operations
public class WalletReadRepository implements PanacheRepositoryBase<Wallet, String> {
}