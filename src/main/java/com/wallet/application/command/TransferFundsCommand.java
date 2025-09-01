package com.wallet.application.command;

import java.math.BigDecimal;
import java.util.UUID;

import com.wallet.core.command.Command;

public class TransferFundsCommand implements Command {
    private final String commandId;
    private final String sourceWalletId;
    private final String destinationWalletId;
    private final BigDecimal amount;
    private final String referenceId;

    public TransferFundsCommand(String sourceWalletId, String destinationWalletId, BigDecimal amount, String referenceId) {
        this.commandId = UUID.randomUUID().toString();
        this.sourceWalletId = sourceWalletId;
        this.destinationWalletId = destinationWalletId;
        this.amount = amount;
        this.referenceId = referenceId;
    }

    @Override
    public String getCommandId() {
        return commandId;
    }

    public String getSourceWalletId() {
        return sourceWalletId;
    }

    public String getDestinationWalletId() {
        return destinationWalletId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getReferenceId() {
        return referenceId;
    }
}
