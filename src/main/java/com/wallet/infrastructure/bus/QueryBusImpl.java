package com.wallet.infrastructure.bus;

import com.wallet.core.query.Query;
import com.wallet.core.query.QueryBus;
import com.wallet.core.query.QueryHandler;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class QueryBusImpl implements QueryBus {

    private final Map<Class<?>, QueryHandler<?, ?>> handlers = new HashMap<>();

    @Inject
    public QueryBusImpl(Instance<QueryHandler<?, ?>> handlerInstances) {
        for (QueryHandler<?, ?> handler : handlerInstances) {
            Class<?> queryType = getQueryType(handler.getClass());
            if (queryType != null) {
                handlers.put(queryType, handler);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Query<R>, R> Uni<R> dispatch(T query) {
        QueryHandler<T, R> handler = (QueryHandler<T, R>) handlers.get(query.getClass());
        if (handler == null) {
            return Uni.createFrom().failure(
                new IllegalArgumentException("No handler found for query: " + query.getClass().getSimpleName())
            );
        }
        return handler.handle(query);
    }

    private Class<?> getQueryType(Class<?> handlerClass) {
        Type[] genericInterfaces = handlerClass.getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericInterface;
                if (parameterizedType.getRawType().equals(QueryHandler.class)) {
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
