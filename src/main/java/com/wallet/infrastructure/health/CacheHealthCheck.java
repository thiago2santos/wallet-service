package com.wallet.infrastructure.health;

import com.wallet.infrastructure.cache.WalletStateCache;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;

/**
 * Custom health check for cache functionality.
 * Tests Redis connectivity and cache operations.
 */
@Readiness
@ApplicationScoped
public class CacheHealthCheck implements HealthCheck {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheHealthCheck.class);
    private static final String HEALTH_CHECK_KEY = "health-check-cache-test";
    
    @Inject
    WalletStateCache walletCache;
    
    @Override
    public HealthCheckResponse call() {
        var responseBuilder = HealthCheckResponse.named("Cache Operations");
        
        try {
            Instant start = Instant.now();
            
            // Test cache operations
            boolean cacheHealthy = testCacheOperations();
            responseBuilder.withData("cacheOperations", cacheHealthy ? "UP" : "DOWN");
            
            // Test response time
            Duration responseTime = Duration.between(start, Instant.now());
            responseBuilder.withData("responseTimeMs", responseTime.toMillis());
            
            // Overall health
            boolean isHealthy = cacheHealthy && responseTime.toMillis() < 2000;
            
            if (isHealthy) {
                responseBuilder.up();
                logger.debug("Cache health check passed in {}ms", responseTime.toMillis());
            } else {
                responseBuilder.down();
                logger.warn("Cache health check failed. CacheOps: {}, ResponseTime: {}ms", 
                           cacheHealthy, responseTime.toMillis());
            }
            
        } catch (Exception e) {
            logger.error("Cache health check failed with exception", e);
            responseBuilder.down()
                          .withData("error", e.getMessage())
                          .withData("errorType", e.getClass().getSimpleName());
        }
        
        return responseBuilder.build();
    }
    
    private boolean testCacheOperations() {
        try {
            // Test basic cache operations
            // 1. Try to get a non-existent key (should return null)
            walletCache.getWallet(HEALTH_CHECK_KEY)
                      .await().atMost(Duration.ofSeconds(1));
            
            // 2. Try to invalidate a key (should not fail)
            walletCache.invalidateWallet(HEALTH_CHECK_KEY)
                      .await().atMost(Duration.ofSeconds(1));
            
            return true;
        } catch (Exception e) {
            logger.warn("Cache operations health check failed", e);
            return false;
        }
    }
}
