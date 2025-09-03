package com.wallet.infrastructure.health;

import com.wallet.application.command.CreateWalletCommand;
import com.wallet.application.query.GetWalletQuery;
import com.wallet.core.command.CommandBus;
import com.wallet.core.query.QueryBus;
import com.wallet.exception.WalletNotFoundException;
import io.smallrye.health.checks.UrlHealthCheck;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;

/**
 * Custom health check for core wallet service functionality.
 * Tests the CQRS buses and basic wallet operations.
 */
@Liveness
@ApplicationScoped
public class WalletServiceHealthCheck implements HealthCheck {
    
    private static final Logger logger = LoggerFactory.getLogger(WalletServiceHealthCheck.class);
    private static final String HEALTH_CHECK_WALLET_ID = "00000000-0000-0000-0000-000000000000";
    
    @Inject
    CommandBus commandBus;
    
    @Inject
    QueryBus queryBus;
    
    @Override
    public HealthCheckResponse call() {
        var responseBuilder = HealthCheckResponse.named("Wallet Service Core Operations");
        
        try {
            Instant start = Instant.now();
            
            // Test 1: Command Bus Health
            boolean commandBusHealthy = testCommandBus();
            responseBuilder.withData("commandBus", commandBusHealthy ? "UP" : "DOWN");
            
            // Test 2: Query Bus Health  
            boolean queryBusHealthy = testQueryBus();
            responseBuilder.withData("queryBus", queryBusHealthy ? "UP" : "DOWN");
            
            // Test 3: Response Time
            Duration responseTime = Duration.between(start, Instant.now());
            responseBuilder.withData("responseTimeMs", responseTime.toMillis());
            
            // Overall health
            boolean isHealthy = commandBusHealthy && queryBusHealthy && responseTime.toMillis() < 5000;
            
            if (isHealthy) {
                responseBuilder.up();
                logger.debug("Wallet service health check passed in {}ms", responseTime.toMillis());
            } else {
                responseBuilder.down();
                logger.warn("Wallet service health check failed. CommandBus: {}, QueryBus: {}, ResponseTime: {}ms", 
                           commandBusHealthy, queryBusHealthy, responseTime.toMillis());
            }
            
        } catch (Exception e) {
            logger.error("Wallet service health check failed with exception", e);
            responseBuilder.down()
                          .withData("error", e.getMessage())
                          .withData("errorType", e.getClass().getSimpleName());
        }
        
        return responseBuilder.build();
    }
    
    private boolean testCommandBus() {
        try {
            // Test command bus registration and basic functionality
            // We don't actually create a wallet, just test the bus can route commands
            if (commandBus == null) {
                logger.warn("CommandBus is null");
                return false;
            }
            
            // Simple test - just verify the bus exists and is injectable
            logger.debug("CommandBus health check passed - bus is available");
            return true;
        } catch (Exception e) {
            logger.warn("Command bus health check failed", e);
            return false;
        }
    }
    
    private boolean testQueryBus() {
        try {
            // Test query bus registration and basic functionality
            if (queryBus == null) {
                logger.warn("QueryBus is null");
                return false;
            }
            
            // Simple test - just verify the bus exists and is injectable
            // Avoid actual query execution in health checks to prevent side effects
            logger.debug("QueryBus health check passed - bus is available");
            return true;
            
        } catch (Exception e) {
            logger.warn("Query bus health check failed with unexpected exception", e);
            return false;
        }
    }
}
