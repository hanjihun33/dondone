package com.workproofpay.backend.workproof.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WorkProofAttachmentMetadataRequest(
        @NotNull(message = "attachment type is required")
        AttachmentType type,

        @Size(max = 255, message = "fileName must be 255 characters or less")
        String fileName,

        @NotBlank(message = "fileRef is required")
        @Size(max = 255, message = "fileRef must be 255 characters or less")
        String fileRef
) {
    public enum AttachmentType {
        PHOTO,
        MEMO,
        DOCUMENT,
        OTHER
    }
}
