package com.workproofpay.backend.documents.api.dto.response;

import com.workproofpay.backend.documents.model.DocumentGenerationRequest;
import com.workproofpay.backend.documents.model.DocumentGenerationStatus;
import com.workproofpay.backend.documents.model.DocumentType;

/**
 * 비동기 문서 생성 접수 응답이다.
 */
public record DocumentGenerationAcceptedResponse(
        String requestId,
        DocumentType documentType,
        DocumentGenerationStatus status,
        String pollUrl
) {
    public static DocumentGenerationAcceptedResponse from(DocumentGenerationRequest request) {
        return new DocumentGenerationAcceptedResponse(
                request.getRequestId(),
                request.getDocumentType(),
                request.getStatus(),
                "/api/documents/requests/" + request.getRequestId()
        );
    }
}
