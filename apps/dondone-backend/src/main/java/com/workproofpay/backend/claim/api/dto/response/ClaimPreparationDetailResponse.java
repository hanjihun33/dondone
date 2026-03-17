package com.workproofpay.backend.claim.api.dto.response;

import com.workproofpay.backend.claim.model.ClaimPreparation;
import com.workproofpay.backend.claim.model.ClaimPreparationStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Instant Claim preparation 재조회 응답이다.
 */
public record ClaimPreparationDetailResponse(
        Long preparationId,
        ClaimPreparationStatus status,
        String summaryText,
        List<ClaimPreparationResponse.ChecklistItem> checklist,
        List<ClaimPreparationResponse.SuggestedRoute> suggestedRoutes,
        List<ClaimPreparationResponse.RelatedDocument> relatedDocuments,
        LocalDateTime createdAt
) {
    public static ClaimPreparationDetailResponse from(ClaimPreparation preparation,
                                                      List<ClaimPreparationResponse.ChecklistItem> checklist,
                                                      List<ClaimPreparationResponse.SuggestedRoute> suggestedRoutes,
                                                      List<ClaimPreparationResponse.RelatedDocument> relatedDocuments) {
        return new ClaimPreparationDetailResponse(
                preparation.getId(),
                preparation.getStatus(),
                preparation.getSummaryText(),
                checklist,
                suggestedRoutes,
                relatedDocuments,
                preparation.getCreatedAt()
        );
    }
}
