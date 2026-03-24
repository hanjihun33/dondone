package com.workproofpay.backend.workproof.api.dto.request;

import com.workproofpay.backend.correction.model.CorrectionRequestReasonCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record CreateWorkProofCorrectionRequest(
        @NotNull(message = "requestedClockInAt is required")
        LocalDateTime requestedClockInAt,

        @NotNull(message = "requestedClockOutAt is required")
        LocalDateTime requestedClockOutAt,

        @NotNull(message = "reasonCode is required")
        CorrectionRequestReasonCode reasonCode,

        @NotBlank(message = "reason is required")
        @Size(max = 500, message = "reason must be 500 characters or less")
        String reason,

        @Size(max = 500, message = "memo must be 500 characters or less")
        String memo,

        @Min(value = 0, message = "attachmentCount must be 0 or greater")
        @Max(value = 20, message = "attachmentCount must be 20 or less")
        Integer attachmentCount,

        @Valid
        @Size(max = 20, message = "attachments must be 20 or less")
        List<WorkProofAttachmentMetadataRequest> attachments
) {
    public int resolvedAttachmentCount() {
        return attachmentCount != null ? attachmentCount : attachments == null ? 0 : attachments.size();
    }
}
