package com.workproofpay.remittance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateTransferRequest(
        @NotBlank String recipientId,
        @Positive long amount,
        @NotBlank String asset,
        boolean highAmountConfirmed,
        String memo
) {}
