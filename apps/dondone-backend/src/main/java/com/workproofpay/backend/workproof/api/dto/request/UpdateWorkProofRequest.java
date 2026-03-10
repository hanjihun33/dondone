package com.workproofpay.backend.workproof.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;

public record UpdateWorkProofRequest(
        @NotNull(message = "clockInAt is required")
        LocalDateTime clockInAt,

        @NotNull(message = "clockOutAt is required")
        LocalDateTime clockOutAt,

        @NotBlank(message = "editReason is required")
        @Size(max = 500, message = "editReason must be 500 characters or less")
        String editReason,

        @Size(max = 500, message = "memo must be 500 characters or less")
        String memo,

        @Min(value = 0, message = "attachmentCount must be 0 or greater")
        @Max(value = 20, message = "attachmentCount must be 20 or less")
        Integer attachmentCount,

        @Valid
        @Size(max = 20, message = "attachments must be 20 or less")
        List<WorkProofAttachmentMetadataRequest> attachments
) {
    public int resolvedAttachmentCount(int currentAttachmentCount) {
        return attachmentCount != null ? attachmentCount : attachments == null ? currentAttachmentCount : attachments.size();
    }
}
