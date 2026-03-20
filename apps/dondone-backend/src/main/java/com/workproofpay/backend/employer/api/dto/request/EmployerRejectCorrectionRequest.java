package com.workproofpay.backend.employer.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmployerRejectCorrectionRequest(
        @Size(max = 500, message = "decisionMemo must be 500 characters or less")
        String decisionMemo,

        @NotBlank(message = "rejectReasonCode is required")
        @Size(max = 100, message = "rejectReasonCode must be 100 characters or less")
        String rejectReasonCode
) {
}
