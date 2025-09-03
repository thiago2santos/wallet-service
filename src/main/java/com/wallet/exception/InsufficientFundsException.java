package com.wallet.exception;

import java.math.BigDecimal;

/**
 * Exception thrown when a wallet has insufficient funds for an operation.
 * This is a business logic exception that should result in a 400 Bad Request response.
 */
public class InsufficientFundsException extends BusinessException {
    
    private final BigDecimal availableAmount;
    private final BigDecimal requestedAmount;
    
    public InsufficientFundsException(BigDecimal availableAmount, BigDecimal requestedAmount) {
        super(String.format("Insufficient funds. Available: %s, Requested: %s", 
              availableAmount, requestedAmount), "INSUFFICIENT_FUNDS");
        this.availableAmount = availableAmount;
        this.requestedAmount = requestedAmount;
    }
    
    public BigDecimal getAvailableAmount() {
        return availableAmount;
    }
    
    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }
}
