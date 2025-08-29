package com.wallet.infrastructure.bus;

import com.wallet.core.command.Command;
import com.wallet.core.command.CommandBus;
import com.wallet.core.command.CommandHandler;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class CommandBusImpl implements CommandBus {

    private final Map<Class<?>, CommandHandler<?, ?>> handlers = new HashMap<>();

    @Inject
    public CommandBusImpl(Instance<CommandHandler<?, ?>> handlerInstances) {
        System.out.println("CommandBusImpl: Initializing with handlers...");
        for (CommandHandler<?, ?> handler : handlerInstances) {
            Class<?> commandType = getCommandType(handler.getClass());
            System.out.println("CommandBusImpl: Found handler " + handler.getClass().getSimpleName() + " for command type: " + commandType);
            if (commandType != null) {
                handlers.put(commandType, handler);
            }
        }
        System.out.println("CommandBusImpl: Total handlers registered: " + handlers.size());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Command, R> Uni<R> dispatch(T command) {
        System.out.println("CommandBusImpl: Dispatching command: " + command.getClass().getSimpleName());
        System.out.println("CommandBusImpl: Available handlers: " + handlers.keySet());
        CommandHandler<T, R> handler = (CommandHandler<T, R>) handlers.get(command.getClass());
        if (handler == null) {
            return Uni.createFrom().failure(
                new IllegalArgumentException("No handler found for command: " + command.getClass().getSimpleName())
            );
        }
        return handler.handle(command);
    }

    private Class<?> getCommandType(Class<?> handlerClass) {
        Type[] genericInterfaces = handlerClass.getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericInterface;
                if (parameterizedType.getRawType().equals(CommandHandler.class)) {
                    Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    if (typeArguments.length > 0 && typeArguments[0] instanceof Class) {
                        return (Class<?>) typeArguments[0];
                    }
                }
            }
        }
        return null;
    }
}
