package com.wallet.infrastructure.resilience;

/**
 * Exception thrown when service is in a degraded state
 * 
 * This exception indicates that the service is operational but with reduced
 * functionality due to infrastructure issues. It's used to communicate
 * degradation states to clients while maintaining service availability.
 * 
 * Examples:
 * - READ_ONLY_MODE: Primary database unavailable
 * - CACHE_BYPASS: Redis unavailable, slower performance
 * - EVENT_PROCESSING_DEGRADED: Kafka unavailable, delayed audit trail
 * - RATE_LIMITED: High load protection active
 */
public class ServiceDegradedException extends RuntimeException {
    
    private final String degradationCode;
    
    public ServiceDegradedException(String message, String degradationCode) {
        super(message);
        this.degradationCode = degradationCode;
    }
    
    public ServiceDegradedException(String message, String degradationCode, Throwable cause) {
        super(message, cause);
        this.degradationCode = degradationCode;
    }
    
    public String getDegradationCode() {
        return degradationCode;
    }
    
    // Factory methods for common degradation scenarios
    
    public static ServiceDegradedException readOnlyMode() {
        return new ServiceDegradedException(
            "Service is temporarily in read-only mode. Write operations are disabled.",
            "READ_ONLY_MODE"
        );
    }
    
    public static ServiceDegradedException cacheBypass() {
        return new ServiceDegradedException(
            "Cache is temporarily unavailable. Service is operational with reduced performance.",
            "CACHE_BYPASS"
        );
    }
    
    public static ServiceDegradedException eventProcessingDegraded() {
        return new ServiceDegradedException(
            "Event processing is temporarily degraded. Operations continue but audit trail may be delayed.",
            "EVENT_PROCESSING_DEGRADED"
        );
    }
    
    public static ServiceDegradedException rateLimited() {
        return new ServiceDegradedException(
            "Service is temporarily rate limited due to high load. Please retry after a short delay.",
            "RATE_LIMITED"
        );
    }
}
