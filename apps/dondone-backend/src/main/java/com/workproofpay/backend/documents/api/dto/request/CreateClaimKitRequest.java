package com.workproofpay.backend.documents.api.dto.request;

import com.workproofpay.backend.documents.model.DocumentFileFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Claim Kit도 verification snapshot을 anchor로 사용하되, 출력 옵션만 추가로 받는다.
 */
public record CreateClaimKitRequest(
        @NotNull(message = "wageVerificationId is required")
        @Min(value = 1, message = "wageVerificationId must be greater than 0")
        Long wageVerificationId,

        Boolean includeAttachments,

        DocumentFileFormat format
) {
}
