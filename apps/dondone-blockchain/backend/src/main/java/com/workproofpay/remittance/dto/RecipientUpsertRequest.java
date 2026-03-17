package com.workproofpay.remittance.dto;

import jakarta.validation.constraints.NotBlank;

public record RecipientUpsertRequest(
        @NotBlank String alias,
        @NotBlank String walletAddress,
        @NotBlank String relation,
        boolean allowed
) {}
