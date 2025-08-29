package com.wallet.exception;

public class WalletException extends RuntimeException {
    
    private final String code;

    public WalletException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static WalletException walletNotFound(String walletId) {
        return new WalletException("Wallet not found: " + walletId, "WALLET_NOT_FOUND");
    }

    public static WalletException insufficientFunds(String walletId) {
        return new WalletException("Insufficient funds in wallet: " + walletId, "INSUFFICIENT_FUNDS");
    }

    public static WalletException walletFrozen(String walletId) {
        return new WalletException("Wallet is frozen: " + walletId, "WALLET_FROZEN");
    }

    public static WalletException walletClosed(String walletId) {
        return new WalletException("Wallet is closed: " + walletId, "WALLET_CLOSED");
    }

    public static WalletException duplicateWallet(String userId, String currency) {
        return new WalletException(
            "Wallet already exists for user " + userId + " with currency " + currency,
            "DUPLICATE_WALLET"
        );
    }

    public static WalletException duplicateTransaction(String referenceId) {
        return new WalletException(
            "Transaction already exists with reference ID: " + referenceId,
            "DUPLICATE_TRANSACTION"
        );
    }

    public static WalletException invalidCurrency(String sourceCurrency, String targetCurrency) {
        return new WalletException(
            "Currency mismatch: cannot transfer between " + sourceCurrency + " and " + targetCurrency,
            "INVALID_CURRENCY"
        );
    }
}
