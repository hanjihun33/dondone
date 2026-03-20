package com.workproofpay.backend.documents.api.dto.request;

import com.workproofpay.backend.documents.service.WorkproofDocumentPreviewQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record WorkproofDocumentPreviewRequest(
        @Schema(description = "Owned workplace ID used for the workproof statement preview", example = "2")
        @NotNull(message = "workplaceId is required")
        @Min(value = 1, message = "workplaceId must be greater than 0")
        Long workplaceId,

        @Schema(description = "Preview start date in YYYY-MM-DD format", example = "2026-01-01")
        @NotNull(message = "startDate is required")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,

        @Schema(description = "Preview end date in YYYY-MM-DD format", example = "2026-02-23")
        @NotNull(message = "endDate is required")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate
) {
    public WorkproofDocumentPreviewQuery toQuery() {
        return new WorkproofDocumentPreviewQuery(workplaceId, startDate, endDate);
    }
}
