package com.workproofpay.backend.documents.service;

import com.workproofpay.backend.documents.api.dto.request.CreateProofPackRequest;
import com.workproofpay.backend.documents.api.dto.response.DocumentGenerationAcceptedResponse;
import com.workproofpay.backend.documents.model.DocumentGenerationRequest;
import com.workproofpay.backend.documents.model.DocumentType;
import com.workproofpay.backend.documents.repo.DocumentGenerationRequestRepository;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.wage.model.WageVerification;
import com.workproofpay.backend.wage.service.WageVerificationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Documents lane 1은 실제 PDF 생성보다 verification anchor와 요청 계약을 먼저 고정한다.
 */
@Service
@RequiredArgsConstructor
public class DocumentsService {

    private final DocumentGenerationRequestRepository documentGenerationRequestRepository;
    private final WageVerificationQueryService wageVerificationQueryService;

    /**
     * Proof Pack 요청은 verification snapshot과 idempotency key를 먼저 고정해
     * 실제 PDF/job 구현 전에도 계약과 ownership 규칙을 열 수 있게 한다.
     */
    @Transactional
    public DocumentGenerationAcceptedResponse createProofPack(Long userId,
                                                              String idempotencyKey,
                                                              CreateProofPackRequest request) {
        if (documentGenerationRequestRepository.existsByUserIdAndDocumentTypeAndIdempotencyKey(
                userId,
                DocumentType.PROOF_PACK,
                idempotencyKey
        )) {
            throw new ApiException(ErrorCode.DOCUMENT_DUPLICATE_REQUEST);
        }

        WageVerification verification = wageVerificationQueryService.getOwnedVerification(userId, request.wageVerificationId());
        DocumentGenerationRequest saved = documentGenerationRequestRepository.save(
                DocumentGenerationRequest.queueProofPack(
                        verification.getUser(),
                        verification.getId(),
                        verification.getMonth(),
                        verification.getWorkplaceId(),
                        idempotencyKey
                )
        );
        return DocumentGenerationAcceptedResponse.from(saved);
    }
}
