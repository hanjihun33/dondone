package com.workproofpay.backend.documents.service;

import com.workproofpay.backend.documents.model.DocumentGenerationStatus;
import com.workproofpay.backend.documents.model.DocumentType;

public record WorkproofDocumentAcceptedResult(
        String requestId,
        DocumentType documentType,
        DocumentGenerationStatus status,
        String pollUrl
) {
}
