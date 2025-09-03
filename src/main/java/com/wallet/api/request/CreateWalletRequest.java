package com.wallet.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateWalletRequest {
    @NotBlank(message = "User ID is required")
    @Size(min = 1, max = 100, message = "User ID must be between 1 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\-_@.]+$", message = "User ID can only contain letters, numbers, hyphens, underscores, @ and dots")
    private String userId;



    // Default constructor for JSON deserialization
    public CreateWalletRequest() {
    }

    public CreateWalletRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }


}
