package com.workproofpay.backend.documents.service;

import java.time.LocalDate;

public record WorkproofDocumentPreviewResult(
        String documentType,
        Long workplaceId,
        String workplaceName,
        LocalDate startDate,
        LocalDate endDate,
        int totalRecordCount,
        int reflectedCount,
        int needsReviewCount,
        int editedCount,
        int attachmentCount,
        long totalWorkedMinutes,
        String totalWorkedHoursText
) {
}
