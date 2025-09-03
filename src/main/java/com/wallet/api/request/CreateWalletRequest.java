package com.wallet.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateWalletRequest {
    @NotBlank(message = "User ID is required")
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
