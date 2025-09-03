package com.wallet.infrastructure.bus;

import com.wallet.core.command.Command;
import com.wallet.core.command.CommandBus;
import com.wallet.core.command.CommandHandler;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import com.wallet.infrastructure.metrics.WalletMetrics;
import io.micrometer.core.instrument.Timer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class CommandBusImpl implements CommandBus {

    private final Map<Class<?>, CommandHandler<?, ?>> handlers = new HashMap<>();
    
    @Inject
    WalletMetrics metrics;

    @Inject
    public CommandBusImpl(Instance<CommandHandler<?, ?>> handlerInstances) {
        for (CommandHandler<?, ?> handler : handlerInstances) {
            Class<?> commandType = getCommandType(handler.getClass());
            if (commandType != null) {
                handlers.put(commandType, handler);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Command, R> Uni<R> dispatch(T command) {
        CommandHandler<T, R> handler = (CommandHandler<T, R>) handlers.get(command.getClass());
        if (handler == null) {
            return Uni.createFrom().failure(
                new IllegalArgumentException("No handler found for command: " + command.getClass().getSimpleName())
            );
        }
        return handler.handle(command);
    }

    private Class<?> getCommandType(Class<?> handlerClass) {
        // Handle Quarkus CDI proxies by getting the superclass
        Class<?> actualClass = handlerClass;
        if (handlerClass.getSimpleName().contains("_ClientProxy")) {
            actualClass = handlerClass.getSuperclass();
        }
        
        
        // Check direct interfaces first
        Type[] genericInterfaces = actualClass.getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericInterface;
                if (parameterizedType.getRawType().equals(CommandHandler.class)) {
                    Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    if (typeArguments.length > 0 && typeArguments[0] instanceof Class) {
                        Class<?> commandType = (Class<?>) typeArguments[0];
                        return commandType;
                    }
                }
            }
        }
        
        // Check superclass interfaces if not found
        Class<?> superClass = actualClass.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            Type[] superInterfaces = superClass.getGenericInterfaces();
            for (Type genericInterface : superInterfaces) {
                if (genericInterface instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) genericInterface;
                    if (parameterizedType.getRawType().equals(CommandHandler.class)) {
                        Type[] typeArguments = parameterizedType.getActualTypeArguments();
                        if (typeArguments.length > 0 && typeArguments[0] instanceof Class) {
                            Class<?> commandType = (Class<?>) typeArguments[0];
                            return commandType;
                        }
                    }
                }
            }
        }
        
        return null;
    }
}
