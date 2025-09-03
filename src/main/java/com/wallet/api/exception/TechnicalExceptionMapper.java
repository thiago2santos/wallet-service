package com.wallet.api.exception;

import com.wallet.exception.TechnicalException;
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
 * Exception mapper for technical/infrastructure exceptions.
 * Maps technical exceptions to 5xx HTTP status codes with error tracking.
 */
@Provider
public class TechnicalExceptionMapper implements ExceptionMapper<TechnicalException> {
    
    private static final Logger logger = LoggerFactory.getLogger(TechnicalExceptionMapper.class);
    
    @Override
    public Response toResponse(TechnicalException exception) {
        // Generate unique error ID for tracking
        String errorId = UUID.randomUUID().toString();
        
        // Log technical exceptions at ERROR level with full stack trace
        logger.error("Technical exception occurred [errorId: {}]: {} - {}", 
                    errorId, exception.getErrorCode(), exception.getMessage(), exception);
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        response.put("error", "Internal Server Error");
        response.put("message", "An internal error occurred. Please try again later.");
        response.put("errorId", errorId);
        response.put("errorCode", exception.getErrorCode());
        
        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(response)
                .build();
    }
}
