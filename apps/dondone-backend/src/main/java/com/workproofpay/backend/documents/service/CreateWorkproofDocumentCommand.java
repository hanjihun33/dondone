package com.workproofpay.backend.documents.service;

import com.workproofpay.backend.documents.model.DocumentType;

import java.time.LocalDate;

public record CreateWorkproofDocumentCommand(
        DocumentType documentType,
        Long workplaceId,
        LocalDate startDate,
        LocalDate endDate,
        String idempotencyKey
) {
}
