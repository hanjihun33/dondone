package com.workproofpay.backend.employer.api.dto.request;

import jakarta.validation.constraints.Size;

public record EmployerApproveCorrectionRequest(
        @Size(max = 500, message = "decisionMemo must be 500 characters or less")
        String decisionMemo
) {
}
