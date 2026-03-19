package com.workproofpay.backend.documents.service;

import java.time.LocalDate;

public record WorkproofDocumentPreviewQuery(
        Long workplaceId,
        LocalDate startDate,
        LocalDate endDate
) {
}
