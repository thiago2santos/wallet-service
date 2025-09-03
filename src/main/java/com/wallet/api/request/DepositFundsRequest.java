package com.wallet.api.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Request to deposit funds into a wallet")
public class DepositFundsRequest {
    @Schema(
        description = "Amount to deposit in BRL (Brazilian Real)",
        example = "150.75",
        required = true,
        minimum = "0.01",
        maximum = "1000000.00"
    )
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @DecimalMax(value = "1000000.00", message = "Amount cannot exceed 1,000,000.00")
    private BigDecimal amount;

    @Schema(
        description = "Unique reference identifier for this deposit transaction",
        example = "dep-2025-001",
        required = true,
        minLength = 1,
        maxLength = 100,
        pattern = "^[a-zA-Z0-9\\-_]+$"
    )
    @NotBlank(message = "Reference ID is required")
    @Size(min = 1, max = 100, message = "Reference ID must be between 1 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\-_]+$", message = "Reference ID can only contain letters, numbers, hyphens, and underscores")
    private String referenceId;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    // Default constructor for JSON deserialization
    public DepositFundsRequest() {
    }

    public DepositFundsRequest(BigDecimal amount, String referenceId, String description) {
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
