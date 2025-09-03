package com.wallet.api.exception;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Exception mapper for JAX-RS specific exceptions.
 * Handles NotFoundException and other JAX-RS exceptions.
 */
@Provider
public class JaxRsExceptionMapper implements ExceptionMapper<NotFoundException> {
    
    private static final Logger logger = LoggerFactory.getLogger(JaxRsExceptionMapper.class);
    
    @Override
    public Response toResponse(NotFoundException exception) {
        logger.info("Resource not found: {}", exception.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", Response.Status.NOT_FOUND.getStatusCode());
        response.put("error", "NOT_FOUND");
        response.put("message", exception.getMessage());

        return Response
            .status(Response.Status.NOT_FOUND)
            .entity(response)
            .build();
    }
}
