package com.workproofpay.backend.documents.api.dto.response;

import com.workproofpay.backend.documents.model.DocumentGenerationRequest;
import com.workproofpay.backend.documents.model.DocumentGenerationStatus;
import com.workproofpay.backend.documents.model.DocumentType;

/**
 * 문서 생성 접수 응답이다.
 */
public record DocumentGenerationAcceptedResponse(
        String requestId,
        DocumentType documentType,
        DocumentGenerationStatus status,
        String pollUrl,
        String documentUrl
) {
    public static DocumentGenerationAcceptedResponse from(DocumentGenerationRequest request) {
        return new DocumentGenerationAcceptedResponse(
                request.getRequestId(),
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
