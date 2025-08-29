package com.wallet.api.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Provider
public class WalletExceptionMapper implements ExceptionMapper<RuntimeException> {
    
    @Override
    public Response toResponse(RuntimeException exception) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", getStatus(exception));
        response.put("error", exception.getClass().getSimpleName());
        response.put("message", exception.getMessage());

        return Response
            .status(getStatus(exception))
            .entity(response)
            .build();
    }

    private int getStatus(RuntimeException exception) {
        if (exception instanceof IllegalArgumentException) {
            return Response.Status.BAD_REQUEST.getStatusCode();
        }
        if (exception instanceof IllegalStateException) {
            return Response.Status.CONFLICT.getStatusCode();
        }
        return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    }
}
