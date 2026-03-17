package com.workproofpay.backend.wage.service;

import com.workproofpay.backend.claim.repo.ClaimPreparationRepository;
import com.workproofpay.backend.documents.model.DocumentGenerationRequest;
import com.workproofpay.backend.documents.model.DocumentType;
import com.workproofpay.backend.documents.repo.DocumentGenerationRequestRepository;
import com.workproofpay.backend.wage.api.dto.response.WageVerificationDetailResponse;
import com.workproofpay.backend.wage.model.WageVerification;
import com.workproofpay.backend.wage.model.WageVerificationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * verification detail에서 downstream 문서/Claim 연결 상태를 한 곳에서 읽는다.
 */
@Service
@RequiredArgsConstructor
public class WageVerificationRelatedActionsService {

    private final DocumentGenerationRequestRepository documentGenerationRequestRepository;
    private final ClaimPreparationRepository claimPreparationRepository;

    @Transactional(readOnly = true)
    public WageVerificationDetailResponse.RelatedActionsSnapshot build(Long userId, WageVerification verification) {
        boolean actionable = verification.getStatus() == WageVerificationStatus.CHECK_REQUIRED;

        Long proofPackDocumentId = findLatestDocumentId(userId, verification.getId(), DocumentType.PROOF_PACK);
        Long claimKitDocumentId = findLatestDocumentId(userId, verification.getId(), DocumentType.CLAIM_KIT);
        Long preparationId = claimPreparationRepository
                .findFirstByUserIdAndWageVerificationIdOrderByCreatedAtDesc(userId, verification.getId())
                .map(preparation -> preparation.getId())
                .orElse(null);

        return new WageVerificationDetailResponse.RelatedActionsSnapshot(
                actionable,
                actionable,
                actionable,
                proofPackDocumentId,
                claimKitDocumentId,
                preparationId
        );
    }

    private Long findLatestDocumentId(Long userId, Long verificationId, DocumentType documentType) {
        return documentGenerationRequestRepository
                .findFirstByUserIdAndWageVerificationIdAndDocumentTypeOrderByCreatedAtDesc(userId, verificationId, documentType)
                .map(DocumentGenerationRequest::getId)
                .orElse(null);
    }
}
