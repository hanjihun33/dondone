package com.workproofpay.backend.employer.service;

import com.workproofpay.backend.employer.api.dto.response.EmployerReviewRecordConfirmResponse;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofReflectionStatus;
import com.workproofpay.backend.workproof.model.WorkProof;
import com.workproofpay.backend.workproof.model.WorkProofFinancialStatus;
import com.workproofpay.backend.workproof.repo.WorkProofRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmployerIssueCommandService {

    private final EmployerAccessScopeService employerAccessScopeService;
    private final WorkProofRepository workProofRepository;

    @Transactional
    public EmployerReviewRecordConfirmResponse confirmReviewRecord(Long accountId, Long workProofId) {
        EmployerAccessScope scope = employerAccessScopeService.getRequiredScope(accountId);
        WorkProof workProof = workProofRepository.findByIdAndWorkplaceIdAndFinancialStatus(
                        workProofId,
                        scope.defaultWorkplaceId(),
                        WorkProofFinancialStatus.NEEDS_REVIEW
                )
                .orElseThrow(() -> new ApiException(ErrorCode.WORKPROOF_NOT_FOUND));

        workProof.confirmReview();

        return new EmployerReviewRecordConfirmResponse(
                workProof.getId(),
                WorkProofReflectionStatus.REFLECTED,
                LocalDateTime.now()
        );
    }
}
