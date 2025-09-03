package com.wallet.api.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception mapper for any unhandled exceptions.
 * This is the fallback mapper that catches any exceptions not handled by specific mappers.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionMapper.class);
    
    @Override
    public Response toResponse(Throwable exception) {
        // Generate unique error ID for tracking
        String errorId = UUID.randomUUID().toString();
        
        // Log all unhandled exceptions at ERROR level
        logger.error("Unhandled exception occurred [errorId: {}]: {}", 
                    errorId, exception.getMessage(), exception);
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        response.put("error", "Internal Server Error");
        response.put("message", "An unexpected error occurred. Please try again later.");
        response.put("errorId", errorId);
        
        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(response)
                .build();
    }
}
