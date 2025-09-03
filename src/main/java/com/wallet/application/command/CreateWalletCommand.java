package com.wallet.application.command;

import com.wallet.core.command.Command;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.UUID;

@RegisterForReflection
public class CreateWalletCommand implements Command {
    private final String commandId;
    private final String userId;

    public CreateWalletCommand(String userId) {
        this.commandId = UUID.randomUUID().toString();
        this.userId = userId;
    }

    @Override
    public String getCommandId() {
        return commandId;
    }

    public String getUserId() {
        return userId;
    }


}
