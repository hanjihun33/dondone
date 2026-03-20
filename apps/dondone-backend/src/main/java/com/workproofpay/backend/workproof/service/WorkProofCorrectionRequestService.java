package com.workproofpay.backend.workproof.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workproofpay.backend.correction.model.CorrectionRequest;
import com.workproofpay.backend.correction.model.CorrectionRequestStatus;
import com.workproofpay.backend.correction.repo.CorrectionRequestRepository;
import com.workproofpay.backend.employer.model.EmploymentMembershipStatus;
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

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkProofCorrectionRequestService {

    private final WorkProofRepository workProofRepository;
    private final CorrectionRequestRepository correctionRequestRepository;
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

        if (workProof.getClockInAt().equals(request.requestedClockInAt())
                && workProof.getClockOutAt().equals(request.requestedClockOutAt())) {
            throw new ApiException(ErrorCode.CORRECTION_REQUEST_NO_CHANGES);
        }

        CorrectionRequest correctionRequest = correctionRequestRepository.save(CorrectionRequest.create(
                workProof,
                userId,
                userId,
                workplace.getCompanyId(),
                workplace.getId(),
                workProof.getWorkDate(),
                workProof.getClockInAt(),
                workProof.getClockOutAt(),
                request.requestedClockInAt(),
                request.requestedClockOutAt(),
                request.reason(),
                request.memo(),
                request.resolvedAttachmentCount(),
                serializeAttachmentMetadata(request.attachments())
        ));

        return WorkProofCorrectionRequestResponse.from(correctionRequest);
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
}
