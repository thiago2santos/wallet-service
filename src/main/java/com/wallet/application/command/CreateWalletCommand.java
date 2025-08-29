package com.wallet.application.command;

import com.wallet.core.command.Command;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.UUID;

@RegisterForReflection
public class CreateWalletCommand implements Command {
    private final String commandId;
    private final String userId;
    private final String currency;

    public CreateWalletCommand(String userId, String currency) {
        this.commandId = UUID.randomUUID().toString();
        this.userId = userId;
        this.currency = currency;
    }

    @Override
    public String getCommandId() {
        return commandId;
    }

    public String getUserId() {
        return userId;
    }

    public String getCurrency() {
        return currency;
    }
}
