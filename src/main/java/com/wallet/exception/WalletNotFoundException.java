package com.wallet.exception;

/**
 * Exception thrown when a wallet is not found.
 * This is a business logic exception that should result in a 404 Not Found response.
 */
public class WalletNotFoundException extends BusinessException {
    
    public WalletNotFoundException(String walletId) {
        super("Wallet not found: " + walletId, "WALLET_NOT_FOUND");
    }
    
    public WalletNotFoundException(String walletId, Throwable cause) {
        super("Wallet not found: " + walletId, "WALLET_NOT_FOUND", cause);
    }
}
