package com.workproofpay.backend.claim.api.dto.response;

import com.workproofpay.backend.claim.model.ClaimPreparation;
import com.workproofpay.backend.claim.model.ClaimPreparationStatus;
import com.workproofpay.backend.documents.model.DocumentGenerationStatus;
import com.workproofpay.backend.documents.model.DocumentType;

import java.util.List;

/**
 * Claim preparation 생성 직후 응답이다.
 */
public record ClaimPreparationResponse(
        Long preparationId,
        ClaimPreparationStatus status,
        String summaryText,
        List<ChecklistItem> checklist,
        List<SuggestedRoute> suggestedRoutes,
        List<RelatedDocument> relatedDocuments
) {
    public record ChecklistItem(
            String code,
            String title,
            boolean required
    ) {
    }

    public record SuggestedRoute(
            String channel,
            String title,
            String description,
            String contact,
            String link
    ) {
    }

    public record RelatedDocument(
            Long documentId,
            DocumentType documentType,
            DocumentGenerationStatus status
    ) {
    }

    public static ClaimPreparationResponse from(ClaimPreparation preparation,
                                                List<ChecklistItem> checklist,
                                                List<SuggestedRoute> suggestedRoutes,
                                                List<RelatedDocument> relatedDocuments) {
        return new ClaimPreparationResponse(
                preparation.getId(),
                preparation.getStatus(),
                preparation.getSummaryText(),
                checklist,
                suggestedRoutes,
                relatedDocuments
        );
    }
}
