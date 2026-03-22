package com.workproofpay.backend.documents.api.dto.response;

import com.workproofpay.backend.documents.service.WorkproofDocumentPreviewResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Preview summary for a period-based workproof statement document")
public record WorkproofDocumentPreviewResponse(
        @Schema(description = "Document type to be generated", example = "WORKPROOF_STATEMENT")
        String documentType,

        @Schema(description = "Owned workplace ID used for the preview", example = "2")
        Long workplaceId,

        @Schema(description = "Workplace name shown in the document preview", example = "Proof Pack Cafe")
        String workplaceName,

        @Schema(description = "Preview start date", example = "2026-01-01")
        LocalDate startDate,

        @Schema(description = "Preview end date", example = "2026-02-23")
        LocalDate endDate,

        @Schema(description = "Total workproof records inside the selected period", example = "18")
        int totalRecordCount,

        @Schema(description = "Count of reflected records inside the selected period", example = "16")
        int reflectedCount,

        @Schema(description = "Count of records that still require review", example = "1")
        int needsReviewCount,

        @Schema(description = "Count of edited records inside the selected period", example = "2")
        int editedCount,

        @Schema(description = "Total attachment count across the selected records", example = "3")
        int attachmentCount,

        @Schema(description = "Total worked minutes across the selected records", example = "7590")
        long totalWorkedMinutes,

        @Schema(description = "Human-readable total worked time text", example = "126시간 30분")
        String totalWorkedHoursText
) {
    public static WorkproofDocumentPreviewResponse from(WorkproofDocumentPreviewResult result) {
        return new WorkproofDocumentPreviewResponse(
                result.documentType(),
                result.workplaceId(),
                result.workplaceName(),
                result.startDate(),
                result.endDate(),
                result.totalRecordCount(),
                result.reflectedCount(),
                result.needsReviewCount(),
                result.editedCount(),
                result.attachmentCount(),
                result.totalWorkedMinutes(),
                result.totalWorkedHoursText()
        );
    }
}
