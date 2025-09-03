package com.wallet.api.exception;

import com.wallet.exception.BusinessException;
import com.wallet.exception.InsufficientFundsException;
import com.wallet.exception.WalletNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Exception mapper for business logic exceptions.
 * Maps business exceptions to appropriate HTTP status codes with detailed error information.
 */
@Provider
public class BusinessExceptionMapper implements ExceptionMapper<BusinessException> {
    
    private static final Logger logger = LoggerFactory.getLogger(BusinessExceptionMapper.class);
    
    @Override
    public Response toResponse(BusinessException exception) {
        // Log business exceptions at INFO level (expected errors)
        logger.info("Business exception occurred: {} - {}", 
                   exception.getErrorCode(), exception.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("error", exception.getErrorCode());
        response.put("message", exception.getMessage());
        
        // Add specific details for certain exception types
        if (exception instanceof InsufficientFundsException) {
            InsufficientFundsException ife = (InsufficientFundsException) exception;
            Map<String, Object> details = new HashMap<>();
            details.put("availableAmount", ife.getAvailableAmount());
            details.put("requestedAmount", ife.getRequestedAmount());
            response.put("details", details);
        }
        
        Response.Status status = getHttpStatus(exception);
        response.put("status", status.getStatusCode());
        
        return Response
                .status(status)
                .entity(response)
                .build();
    }
    
    private Response.Status getHttpStatus(BusinessException exception) {
        if (exception instanceof WalletNotFoundException) {
            return Response.Status.NOT_FOUND;
        }
        
        // Most business exceptions are client errors (400)
        return Response.Status.BAD_REQUEST;
    }
}
