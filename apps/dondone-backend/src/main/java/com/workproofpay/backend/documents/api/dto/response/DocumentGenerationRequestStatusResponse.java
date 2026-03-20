package com.workproofpay.backend.documents.api.dto.response;

import com.workproofpay.backend.documents.model.DocumentGenerationRequest;
import com.workproofpay.backend.documents.model.DocumentGenerationStatus;
import com.workproofpay.backend.documents.model.DocumentType;

/**
 * 문서 요청 상태 응답은 requestId 기준 진행 상태와 실제 접근 경로를 함께 돌려준다.
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
                resolveDocumentUrl(request)
        );
    }

    private static String resolveDocumentUrl(DocumentGenerationRequest request) {
        if (request.getDocumentType().usesOnDemandDownload()
                || request.getStatus() == DocumentGenerationStatus.READY) {
            return "/api/documents/" + request.getId() + "/download";
        }
        return null;
    }
}
