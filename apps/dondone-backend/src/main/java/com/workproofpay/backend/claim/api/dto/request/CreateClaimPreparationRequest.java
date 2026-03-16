package com.workproofpay.backend.claim.api.dto.request;

import com.workproofpay.backend.claim.model.ClaimPreparationTone;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Claim preparation은 verification snapshot을 바탕으로 요약과 체크리스트를 만드는 요청이다.
 */
public record CreateClaimPreparationRequest(
        @NotNull(message = "wageVerificationId is required")
        @Min(value = 1, message = "wageVerificationId must be greater than 0")
        Long wageVerificationId,

        @Min(value = 1, message = "claimKitDocumentId must be greater than 0")
        Long claimKitDocumentId,

        @NotBlank(message = "locale is required")
        @Size(max = 20, message = "locale must be 20 characters or less")
        String locale,

        @NotNull(message = "tone is required")
        ClaimPreparationTone tone
) {
}
