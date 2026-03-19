package com.workproofpay.backend.documents.api.dto.response;

import com.workproofpay.backend.documents.model.DocumentGenerationRequest;
import com.workproofpay.backend.documents.model.DocumentGenerationStatus;
import com.workproofpay.backend.documents.model.DocumentType;

/**
 * poll 응답은 requestId 기준 진행 상태와 실제 detail 경로를 함께 돌려준다.
 */
public record DocumentGenerationRequestStatusResponse(
        String requestId,
        Long documentId,
        DocumentType documentType,
        DocumentGenerationStatus status,
        String pollUrl,
        String documentUrl
) {
    public static DocumentGenerationRequestStatusResponse from(DocumentGenerationRequest request) {
        return new DocumentGenerationRequestStatusResponse(
                request.getRequestId(),
                request.getId(),
                request.getDocumentType(),
                request.getStatus(),
                "/api/documents/requests/" + request.getRequestId(),
                request.getStatus() == DocumentGenerationStatus.READY
                        ? "/api/documents/" + request.getId() + "/download"
                        : null
        );
    }
}
