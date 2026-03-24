package com.workproofpay.backend.workproof.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workproofpay.backend.correction.model.CorrectionRequest;
import com.workproofpay.backend.correction.model.CorrectionRequestReasonCode;
import com.workproofpay.backend.correction.model.CorrectionReviewReasonCode;
import com.workproofpay.backend.correction.model.CorrectionRequestStatus;
import com.workproofpay.backend.correction.repo.CorrectionRequestRepository;
import com.workproofpay.backend.employer.model.Company;
import com.workproofpay.backend.employer.model.EmploymentMembershipStatus;
import com.workproofpay.backend.employer.repo.CompanyRepository;
import com.workproofpay.backend.employer.repo.EmploymentMembershipRepository;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.workproof.api.dto.request.CreateWorkProofCorrectionRequest;
import com.workproofpay.backend.workproof.api.dto.request.UpdateWorkProofRequest;
import com.workproofpay.backend.workproof.api.dto.request.WorkProofAttachmentMetadataRequest;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofCorrectionRequestResponse;
import com.workproofpay.backend.workproof.model.WorkProof;
import com.workproofpay.backend.workproof.model.Workplace;
import com.workproofpay.backend.workproof.repo.WorkProofRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkProofCorrectionRequestService {

    private static final int LATE_CLOCK_OUT_GRACE_START_MINUTES = 5;
    private static final int LATE_CLOCK_OUT_GRACE_END_MINUTES = 30;
    private static final String AUTO_APPROVAL_MEMO = "AUTO_APPROVED_BY_ATTENDANCE_POLICY";

    private final WorkProofRepository workProofRepository;
    private final CorrectionRequestRepository correctionRequestRepository;
    private final CompanyRepository companyRepository;
    private final EmploymentMembershipRepository employmentMembershipRepository;
    private final WorkProofRequestValidator workProofRequestValidator;
    private final ObjectMapper objectMapper;

    @Transactional
    public WorkProofCorrectionRequestResponse create(Long userId,
                                                     Long workProofId,
                                                     CreateWorkProofCorrectionRequest request) {
        WorkProof workProof = workProofRepository.findByIdAndUserId(workProofId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKPROOF_NOT_FOUND));

        Workplace workplace = workProof.getWorkplace();
        if (workplace == null || workplace.getCompanyId() == null) {
            throw new ApiException(ErrorCode.CORRECTION_REQUEST_SCOPE_NOT_READY);
        }

        boolean hasActiveMembership = !employmentMembershipRepository.findActiveWorkerMembershipByScope(
                userId,
                workplace.getCompanyId(),
                workplace.getId(),
                EmploymentMembershipStatus.ACTIVE,
                workProof.getWorkDate()
        ).isEmpty();
        if (!hasActiveMembership) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        if (correctionRequestRepository.existsByWorkProofIdAndStatus(workProofId, CorrectionRequestStatus.PENDING)) {
            throw new ApiException(ErrorCode.CORRECTION_REQUEST_PENDING_EXISTS);
        }

        UpdateWorkProofRequest validationRequest = new UpdateWorkProofRequest(
                request.requestedClockInAt(),
                request.requestedClockOutAt(),
                request.reason(),
                request.memo(),
                request.attachmentCount(),
                request.attachments()
        );
        workProofRequestValidator.validateForUpdate(workProof, validationRequest);

        LocalDateTime currentRecognizedClockInAt = workProof.resolveRecognizedClockInAt();
        LocalDateTime currentRecognizedClockOutAt = workProof.resolveRecognizedClockOutAt();
        if (currentRecognizedClockInAt.equals(request.requestedClockInAt())
                && currentRecognizedClockOutAt.equals(request.requestedClockOutAt())) {
            throw new ApiException(ErrorCode.CORRECTION_REQUEST_NO_CHANGES);
        }

        Company company = companyRepository.findById(workplace.getCompanyId())
                .orElseThrow(() -> new ApiException(ErrorCode.COMPANY_NOT_FOUND));
        CorrectionPolicyDecision decision = decidePolicy(workProof, company, request);
        String attachmentMetadataJson = serializeAttachmentMetadata(request.attachments());

        CorrectionRequest correctionRequest;
        if (decision.autoApprove()) {
            workProof.updateRecognizedTimes(
                    decision.recognizedClockInAt(),
                    decision.recognizedClockOutAt()
            );
            correctionRequest = correctionRequestRepository.save(CorrectionRequest.createAutoApproved(
                    workProof,
                    userId,
                    userId,
                    workplace.getCompanyId(),
                    workplace.getId(),
                    workProof.getWorkDate(),
                    currentRecognizedClockInAt,
                    currentRecognizedClockOutAt,
                    decision.recognizedClockInAt(),
                    decision.recognizedClockOutAt(),
                    request.reasonCode(),
                    request.reason(),
                    request.memo(),
                    request.resolvedAttachmentCount(),
                    attachmentMetadataJson,
                    AUTO_APPROVAL_MEMO,
                    LocalDateTime.now()
            ));
        } else {
            correctionRequest = CorrectionRequest.create(
                    workProof,
                    userId,
                    userId,
                    workplace.getCompanyId(),
                    workplace.getId(),
                    workProof.getWorkDate(),
                    currentRecognizedClockInAt,
                    currentRecognizedClockOutAt,
                    request.requestedClockInAt(),
                    request.requestedClockOutAt(),
                    request.reasonCode(),
                    request.reason(),
                    request.memo(),
                    request.resolvedAttachmentCount(),
                    attachmentMetadataJson
            );
            correctionRequest.markNeedsReview(decision.reviewReasonCode());
            correctionRequest = correctionRequestRepository.save(correctionRequest);
        }

        return WorkProofCorrectionRequestResponse.from(correctionRequest);
    }

    private CorrectionPolicyDecision decidePolicy(WorkProof workProof,
                                                  Company company,
                                                  CreateWorkProofCorrectionRequest request) {
        LocalDateTime originalClockInAt = workProof.getClockInAt();
        LocalDateTime originalClockOutAt = workProof.getClockOutAt();
        LocalDateTime scheduledClockInAt = workProof.getWorkDate().atTime(company.getScheduledClockInTime());
        LocalDateTime scheduledClockOutAt = workProof.getWorkDate().atTime(company.getScheduledClockOutTime());

        if (workProof.isClockOutOutsideAllowedRadius()) {
            return CorrectionPolicyDecision.review(CorrectionReviewReasonCode.OUTSIDE_ALLOWED_RADIUS);
        }
        if (originalClockInAt.isAfter(scheduledClockInAt)) {
            return CorrectionPolicyDecision.review(CorrectionReviewReasonCode.LATE_CLOCK_IN_AFTER_SCHEDULE);
        }
        if (originalClockOutAt.isBefore(scheduledClockOutAt)) {
            if (request.memo() == null || request.memo().isBlank()) {
                throw new ApiException(ErrorCode.CORRECTION_REQUEST_MEMO_REQUIRED);
            }
            return CorrectionPolicyDecision.review(CorrectionReviewReasonCode.EARLY_CLOCK_OUT_BEFORE_SCHEDULE);
        }
        if (isLateButtonPressAutoApproval(workProof, request, scheduledClockOutAt)) {
            return CorrectionPolicyDecision.autoApproved(
                    resolveRecognizedClockInAt(workProof, scheduledClockInAt, request.reasonCode()),
                    scheduledClockOutAt
            );
        }
        if (originalClockOutAt.isAfter(scheduledClockOutAt.plusMinutes(LATE_CLOCK_OUT_GRACE_END_MINUTES))) {
            return CorrectionPolicyDecision.review(CorrectionReviewReasonCode.LATE_CLOCK_OUT_AFTER_GRACE);
        }
        return CorrectionPolicyDecision.review(CorrectionReviewReasonCode.OTHER);
    }

    private boolean isLateButtonPressAutoApproval(WorkProof workProof,
                                                  CreateWorkProofCorrectionRequest request,
                                                  LocalDateTime scheduledClockOutAt) {
        if (request.reasonCode() != CorrectionRequestReasonCode.LATE_BUTTON_PRESS) {
            return false;
        }
        if (!request.requestedClockInAt().equals(workProof.resolveRecognizedClockInAt())) {
            return false;
        }
        if (!request.requestedClockOutAt().equals(scheduledClockOutAt)) {
            return false;
        }
        LocalDateTime originalClockOutAt = workProof.getClockOutAt();
        return !originalClockOutAt.isBefore(scheduledClockOutAt.plusMinutes(LATE_CLOCK_OUT_GRACE_START_MINUTES))
                && !originalClockOutAt.isAfter(scheduledClockOutAt.plusMinutes(LATE_CLOCK_OUT_GRACE_END_MINUTES));
    }

    private LocalDateTime resolveRecognizedClockInAt(WorkProof workProof,
                                                     LocalDateTime scheduledClockInAt,
                                                     CorrectionRequestReasonCode reasonCode) {
        if (reasonCode == CorrectionRequestReasonCode.LATE_CLOCK_IN) {
            return scheduledClockInAt;
        }
        return workProof.resolveRecognizedClockInAt();
    }

    private String serializeAttachmentMetadata(List<WorkProofAttachmentMetadataRequest> attachments) {
        if (attachments == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attachments);
        } catch (JsonProcessingException e) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private record CorrectionPolicyDecision(
            boolean autoApprove,
            LocalDateTime recognizedClockInAt,
            LocalDateTime recognizedClockOutAt,
            CorrectionReviewReasonCode reviewReasonCode
    ) {
        private static CorrectionPolicyDecision autoApproved(LocalDateTime recognizedClockInAt,
                                                             LocalDateTime recognizedClockOutAt) {
            return new CorrectionPolicyDecision(true, recognizedClockInAt, recognizedClockOutAt, null);
        }

        private static CorrectionPolicyDecision review(CorrectionReviewReasonCode reviewReasonCode) {
            return new CorrectionPolicyDecision(false, null, null, reviewReasonCode);
        }
    }
}
