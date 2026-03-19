package com.workproofpay.backend.documents.api.dto.request;

import com.workproofpay.backend.documents.model.DocumentType;
import com.workproofpay.backend.documents.service.CreateWorkproofDocumentCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateWorkproofDocumentRequest(
        @Schema(description = "Document type to be generated", example = "WORKPROOF_STATEMENT", allowableValues = {"WORKPROOF_STATEMENT"})
        @NotNull(message = "documentType is required")
        DocumentType documentType,

        @Schema(description = "Owned workplace ID used for the workproof statement", example = "2")
        @NotNull(message = "workplaceId is required")
        @Min(value = 1, message = "workplaceId must be greater than 0")
        Long workplaceId,

        @Schema(description = "Document start date in YYYY-MM-DD format", example = "2026-01-01")
        @NotNull(message = "startDate is required")
        LocalDate startDate,

        @Schema(description = "Document end date in YYYY-MM-DD format", example = "2026-02-23")
        @NotNull(message = "endDate is required")
        LocalDate endDate
) {
    public CreateWorkproofDocumentCommand toCommand(String idempotencyKey) {
        return new CreateWorkproofDocumentCommand(documentType, workplaceId, startDate, endDate, idempotencyKey);
    }
}
