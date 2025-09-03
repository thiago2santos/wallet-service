package com.wallet.exception;

/**
 * Base class for all business logic exceptions.
 * These exceptions represent expected error conditions in the business domain
 * and should be handled gracefully with appropriate HTTP status codes.
 */
public abstract class BusinessException extends RuntimeException {
    
    private final String errorCode;
    
    protected BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    protected BusinessException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
