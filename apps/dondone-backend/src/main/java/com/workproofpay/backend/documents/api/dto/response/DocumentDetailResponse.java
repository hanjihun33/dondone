package com.workproofpay.backend.documents.api.dto.response;

import com.workproofpay.backend.documents.model.DocumentGenerationRequest;
import com.workproofpay.backend.documents.model.DocumentGenerationStatus;
import com.workproofpay.backend.documents.model.DocumentType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 현재 문서 detail은 생성 요청 엔티티를 read model로 재사용해 최소 문서 inbox 계약을 연다.
 */
public record DocumentDetailResponse(
        Long documentId,
        DocumentType type,
        DocumentGenerationStatus status,
        String title,
        String summary,
        List<RelatedLink> relatedLinks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean downloadable
) {
    public record RelatedLink(
            String title,
            String href
    ) {
    }

    public static DocumentDetailResponse from(DocumentGenerationRequest request,
                                              String title,
                                              String summary,
                                              List<RelatedLink> relatedLinks) {
        return new DocumentDetailResponse(
                request.getId(),
                request.getDocumentType(),
                request.getStatus(),
                title,
                summary,
                relatedLinks,
                request.getCreatedAt(),
                request.getUpdatedAt(),
                request.getStatus() == DocumentGenerationStatus.READY
        );
    }
}
