package com.wallet.application.command;

import com.wallet.core.command.Command;
import java.math.BigDecimal;
import java.util.UUID;

public class DepositFundsCommand implements Command {
    private final String commandId;
    private final String walletId;
    private final BigDecimal amount;
    private final String referenceId;

    public DepositFundsCommand(String walletId, BigDecimal amount, String referenceId) {
        this.commandId = UUID.randomUUID().toString();
        this.walletId = walletId;
        this.amount = amount;
        this.referenceId = referenceId;
    }

    @Override
    public String getCommandId() {
        return commandId;
    }

    public String getWalletId() {
        return walletId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getReferenceId() {
        return referenceId;
    }
}