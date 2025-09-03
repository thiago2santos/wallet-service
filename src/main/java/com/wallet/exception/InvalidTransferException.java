package com.wallet.exception;

/**
 * Exception thrown when a transfer operation is invalid.
 * This is a business logic exception that should result in a 400 Bad Request response.
 */
public class InvalidTransferException extends BusinessException {
    
    public InvalidTransferException(String message) {
        super(message, "INVALID_TRANSFER");
    }
    
    public static InvalidTransferException sameWallet() {
        return new InvalidTransferException("Source and destination wallets cannot be the same");
    }
    
    public static InvalidTransferException invalidAmount() {
        return new InvalidTransferException("Transfer amount must be positive");
    }
}
