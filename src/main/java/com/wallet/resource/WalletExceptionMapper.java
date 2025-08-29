package com.wallet.resource;

import com.wallet.exception.WalletException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Provider
public class WalletExceptionMapper implements ExceptionMapper<WalletException> {
    
    @Override
    public Response toResponse(WalletException exception) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", exception.getCode());
        error.put("message", exception.getMessage());
        error.put("timestamp", LocalDateTime.now());

        int status;
        switch (exception.getCode()) {
            case "WALLET_NOT_FOUND":
                status = Response.Status.NOT_FOUND.getStatusCode();
                break;
            case "INSUFFICIENT_FUNDS":
            case "WALLET_FROZEN":
            case "WALLET_CLOSED":
                status = Response.Status.FORBIDDEN.getStatusCode();
                break;
            case "DUPLICATE_WALLET":
            case "DUPLICATE_TRANSACTION":
            case "INVALID_CURRENCY":
            case "INVALID_REQUEST":
                status = Response.Status.BAD_REQUEST.getStatusCode();
                break;
            default:
                status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        }

        return Response.status(status)
                .entity(error)
                .build();
    }
}
