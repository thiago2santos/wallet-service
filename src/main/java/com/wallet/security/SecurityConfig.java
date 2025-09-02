package com.wallet.security;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Security configuration for the Wallet Service.
 * 
 * This class defines the security roles and permissions as specified in the documentation:
 * - user: Can read wallets and perform transactions (deposit, withdraw, transfer)
 * - admin: Has all user permissions plus administrative functions (freeze, close, metrics)
 */
@ApplicationScoped
public class SecurityConfig {
    
    // Role definitions
    public static final String USER_ROLE = "user";
    public static final String ADMIN_ROLE = "admin";
    
    // Permission definitions based on documentation
    public static final String WALLET_READ = "wallet:read";
    public static final String WALLET_DEPOSIT = "wallet:deposit";
    public static final String WALLET_WITHDRAW = "wallet:withdraw";
    public static final String WALLET_TRANSFER = "wallet:transfer";
    public static final String WALLET_WRITE = "wallet:write";
    public static final String WALLET_FREEZE = "wallet:freeze";
    public static final String WALLET_CLOSE = "wallet:close";
    public static final String SYSTEM_METRICS = "system:metrics";
    
    /**
     * Check if a role has a specific permission.
     * Based on the RBAC configuration from security.md
     */
    public boolean hasPermission(String role, String permission) {
        if (ADMIN_ROLE.equals(role)) {
            // Admin has all permissions
            return WALLET_READ.equals(permission) ||
                   WALLET_WRITE.equals(permission) ||
                   WALLET_FREEZE.equals(permission) ||
                   WALLET_CLOSE.equals(permission) ||
                   SYSTEM_METRICS.equals(permission);
        } else if (USER_ROLE.equals(role)) {
            // User has limited permissions
            return WALLET_READ.equals(permission) ||
                   WALLET_DEPOSIT.equals(permission) ||
                   WALLET_WITHDRAW.equals(permission) ||
                   WALLET_TRANSFER.equals(permission);
        }
        return false;
    }
}
