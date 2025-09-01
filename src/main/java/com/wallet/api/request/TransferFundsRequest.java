package com.wallet.api.request;

import java.math.BigDecimal;

public class TransferFundsRequest {
    private String destinationWalletId;
    private BigDecimal amount;
    private String referenceId;
    private String description;

    public TransferFundsRequest() {}

    public TransferFundsRequest(String destinationWalletId, BigDecimal amount, String referenceId, String description) {
        this.destinationWalletId = destinationWalletId;
        this.amount = amount;
        this.referenceId = referenceId;
        this.description = description;
    }

    public String getDestinationWalletId() {
        return destinationWalletId;
    }

    public void setDestinationWalletId(String destinationWalletId) {
        this.destinationWalletId = destinationWalletId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
