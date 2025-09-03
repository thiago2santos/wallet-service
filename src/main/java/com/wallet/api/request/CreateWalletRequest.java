package com.wallet.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Request to create a new digital wallet")
public class CreateWalletRequest {
    @Schema(
        description = "Unique identifier for the user who will own this wallet",
        example = "user123",
        required = true,
        minLength = 1,
        maxLength = 100,
        pattern = "^[a-zA-Z0-9\\-_@.]+$"
    )
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
