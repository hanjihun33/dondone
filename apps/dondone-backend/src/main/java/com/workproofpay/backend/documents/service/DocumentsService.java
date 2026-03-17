package com.workproofpay.backend.documents.service;

import com.workproofpay.backend.documents.api.dto.request.CreateClaimKitRequest;
import com.workproofpay.backend.documents.api.dto.response.DocumentDetailResponse;
import com.workproofpay.backend.documents.api.dto.request.CreateProofPackRequest;
import com.workproofpay.backend.documents.api.dto.response.DocumentGenerationAcceptedResponse;
import com.workproofpay.backend.documents.api.dto.response.DocumentGenerationRequestStatusResponse;
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

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

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

    @Transactional
    public DocumentGenerationAcceptedResponse createClaimKit(Long userId,
                                                             String idempotencyKey,
                                                             CreateClaimKitRequest request) {
        if (documentGenerationRequestRepository.existsByUserIdAndDocumentTypeAndIdempotencyKey(
                userId,
                DocumentType.CLAIM_KIT,
                idempotencyKey
        )) {
            throw new ApiException(ErrorCode.DOCUMENT_DUPLICATE_REQUEST);
        }

        WageVerification verification = wageVerificationQueryService.getOwnedVerification(userId, request.wageVerificationId());
        DocumentGenerationRequest requestToSave = DocumentGenerationRequest.queueClaimKit(
                verification.getUser(),
                verification.getId(),
                verification.getMonth(),
                verification.getWorkplaceId(),
                request.format() == null ? com.workproofpay.backend.documents.model.DocumentFileFormat.PDF : request.format(),
                Boolean.TRUE.equals(request.includeAttachments()),
                idempotencyKey
        );

        try {
            DocumentGenerationRequest saved = documentGenerationRequestRepository.saveAndFlush(requestToSave);
            return DocumentGenerationAcceptedResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateRequestViolation(e)) {
                throw new ApiException(ErrorCode.DOCUMENT_DUPLICATE_REQUEST);
            }
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public DocumentGenerationRequestStatusResponse getRequestStatus(Long userId, String requestId) {
        DocumentGenerationRequest request = documentGenerationRequestRepository.findByRequestIdAndUserId(requestId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_NOT_FOUND));
        return DocumentGenerationRequestStatusResponse.from(request);
    }

    @Transactional(readOnly = true)
    public DocumentDetailResponse getDocumentDetail(Long userId, Long documentId) {
        DocumentGenerationRequest request = documentGenerationRequestRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_NOT_FOUND));
        WageVerification verification = wageVerificationQueryService.getOwnedVerification(userId, request.getWageVerificationId());

        return DocumentDetailResponse.from(
                request,
                buildTitle(request),
                buildSummary(request, verification),
                buildRelatedLinks(verification)
        );
    }

    private boolean isDuplicateRequestViolation(DataIntegrityViolationException e) {
        Throwable rootCause = NestedExceptionUtils.getMostSpecificCause(e);
        String message = rootCause == null ? e.getMessage() : rootCause.getMessage();
        return message != null && message.contains(DUPLICATE_REQUEST_CONSTRAINT);
    }

    private String buildTitle(DocumentGenerationRequest request) {
        return switch (request.getDocumentType()) {
            case PROOF_PACK -> request.getMonth() + " Proof Pack";
            case CLAIM_KIT -> request.getMonth() + " Claim Kit";
            case TRANSFER_RECEIPT -> "Transfer Receipt";
        };
    }

    private String buildSummary(DocumentGenerationRequest request, WageVerification verification) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.KOREA);
        String estimated = numberFormat.format(verification.getEstimatedTotal());
        String actual = numberFormat.format(verification.getActualDepositAmount());
        String difference = numberFormat.format(verification.getDifferenceAmount());

        return switch (request.getDocumentType()) {
            case PROOF_PACK ->
                    "%s 급여 확인 근거를 정리한 Proof Pack입니다. 참고용 예상 %s원, 실제 확인 %s원, 차이 %s원을 같은 verification snapshot 기준으로 설명합니다."
                            .formatted(request.getMonth(), estimated, actual, difference);
            case CLAIM_KIT ->
                    "%s 신고/상담 준비용 Claim Kit입니다. 출력 형식은 %s이며, 첨부 포함 여부는 %s입니다."
                            .formatted(
                                    request.getMonth(),
                                    request.getOutputFormat(),
                                    request.isIncludeAttachments() ? "포함" : "미포함"
                            );
            case TRANSFER_RECEIPT ->
                    "테스트넷 송금 영수증 요청입니다.";
        };
    }

    private List<DocumentDetailResponse.RelatedLink> buildRelatedLinks(WageVerification verification) {
        return List.of(new DocumentDetailResponse.RelatedLink(
                "급여 확인 결과 보기",
                "/api/wage/verifications/" + verification.getId()
        ));
    }
}
