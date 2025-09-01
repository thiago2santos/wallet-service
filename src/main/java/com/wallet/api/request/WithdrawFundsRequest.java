package com.wallet.api.request;

import java.math.BigDecimal;

public class WithdrawFundsRequest {
    private BigDecimal amount;
    private String referenceId;
    private String description;

    public WithdrawFundsRequest() {}

    public WithdrawFundsRequest(BigDecimal amount, String referenceId, String description) {
        this.amount = amount;
        this.referenceId = referenceId;
        this.description = description;
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
