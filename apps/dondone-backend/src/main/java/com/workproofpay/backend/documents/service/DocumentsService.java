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
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Documents lane 1은 실제 PDF 생성보다 verification anchor와 요청 계약을 먼저 고정한다.
 */
@Service
@RequiredArgsConstructor
public class DocumentsService {

    private static final String DUPLICATE_REQUEST_CONSTRAINT = "uk_document_generation_requests_user_type_key";

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
        DocumentGenerationRequest requestToSave = DocumentGenerationRequest.queueProofPack(
                verification.getUser(),
                verification.getId(),
                verification.getMonth(),
                verification.getWorkplaceId(),
                idempotencyKey
        );

        try {
            // The pre-check handles common retries; the flush closes the concurrent request race window.
            DocumentGenerationRequest saved = documentGenerationRequestRepository.saveAndFlush(requestToSave);
            return DocumentGenerationAcceptedResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateRequestViolation(e)) {
                throw new ApiException(ErrorCode.DOCUMENT_DUPLICATE_REQUEST);
            }
            throw e;
        }
    }

    private boolean isDuplicateRequestViolation(DataIntegrityViolationException e) {
        Throwable rootCause = NestedExceptionUtils.getMostSpecificCause(e);
        String message = rootCause == null ? e.getMessage() : rootCause.getMessage();
        return message != null && message.contains(DUPLICATE_REQUEST_CONSTRAINT);
    }
}
