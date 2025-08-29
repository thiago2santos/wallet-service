package com.wallet.infrastructure.persistence;

import com.wallet.domain.model.Wallet;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class WalletReadRepository implements PanacheRepositoryBase<Wallet, String> {
}