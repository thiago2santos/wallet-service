package com.wallet.exception;

/**
 * Exception for technical/infrastructure failures.
 * These exceptions represent unexpected system errors and should result in 5xx responses.
 */
public class TechnicalException extends RuntimeException {
    
    private final String errorCode;
    
    public TechnicalException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public TechnicalException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public static TechnicalException databaseError(Throwable cause) {
        return new TechnicalException("Database operation failed", "DATABASE_ERROR", cause);
    }
    
    public static TechnicalException kafkaError(Throwable cause) {
        return new TechnicalException("Event publishing failed", "KAFKA_ERROR", cause);
    }
    
    public static TechnicalException cacheError(Throwable cause) {
        return new TechnicalException("Cache operation failed", "CACHE_ERROR", cause);
    }
}
