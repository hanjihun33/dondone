package com.workproofpay.backend.documents.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Proof Pack 생성은 verification snapshot을 anchor로 시작한다.
 */
public record CreateProofPackRequest(
        @NotNull(message = "wageVerificationId is required")
        @Min(value = 1, message = "wageVerificationId must be greater than 0")
        Long wageVerificationId
) {
}
