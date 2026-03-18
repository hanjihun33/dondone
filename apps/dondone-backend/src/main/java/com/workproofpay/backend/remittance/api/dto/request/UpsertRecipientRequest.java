package com.workproofpay.backend.remittance.api.dto.request;

import com.workproofpay.backend.remittance.model.RecipientRelation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpsertRecipientRequest(
        @NotBlank(message = "alias is required")
        String alias,
        @NotNull(message = "relation is required")
        RecipientRelation relation,
        @NotBlank(message = "walletAddress is required")
        @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "walletAddress must be a valid EVM address")
        String walletAddress,
        boolean allowed
) {
}
