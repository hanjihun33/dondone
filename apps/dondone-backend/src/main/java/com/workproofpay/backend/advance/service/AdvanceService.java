package com.workproofpay.backend.advance.service;

import com.workproofpay.backend.advance.api.dto.request.CreateAdvanceRequest;
import com.workproofpay.backend.advance.api.dto.response.*;
import com.workproofpay.backend.advance.model.AdvanceRequest;
import com.workproofpay.backend.advance.model.AdvanceRequestStatus;
import com.workproofpay.backend.advance.repo.AdvanceRequestRepository;
import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofMonthlySummaryContractResponse;
import com.workproofpay.backend.workproof.model.WorkContract;
import com.workproofpay.backend.workproof.model.Workplace;
import com.workproofpay.backend.workproof.repo.WorkContractRepository;
import com.workproofpay.backend.workproof.repo.WorkProofRepository;
import com.workproofpay.backend.workproof.repo.WorkplaceRepository;
import com.workproofpay.backend.workproof.service.WorkProofLane1Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdvanceService {

    private final AdvanceRequestRepository advanceRequestRepository;
    private final UserRepository userRepository;
    private final WorkplaceRepository workplaceRepository;
    private final WorkContractRepository workContractRepository;
    private final WorkProofRepository workProofRepository;
    private final WorkProofLane1Service workProofLane1Service;
    private final AdvancePolicyEngine advancePolicyEngine;

    @Transactional(readOnly = true)
    public AdvanceEligibilityResponse getEligibility(Long userId, Long workplaceId) {
        return evaluateEligibility(userId, workplaceId).response();
    }

    @Transactional
    public AdvanceCreateResult createRequest(Long userId, String idempotencyKey, CreateAdvanceRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }

        AdvanceRequest existing = advanceRequestRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey).orElse(null);
        if (existing != null) {
            if (!existing.matches(idempotencyKey, request.workplaceId(), request.requestedAmount(), request.requestedAt())) {
                throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD);
            }
            return new AdvanceCreateResult(toRequestResponse(existing), true);
        }

        AdvanceEligibilityResult eligibility = evaluateEligibility(userId, request.workplaceId());
        if (eligibility.hardBlocked()) {
            throw new ApiException(ErrorCode.ADVANCE_NOT_ELIGIBLE);
        }
        if (request.requestedAmount() > eligibility.response().availableAmount()) {
            throw new ApiException(ErrorCode.REQUEST_AMOUNT_EXCEEDS_LIMIT);
        }

        AdvanceRequest saved = advanceRequestRepository.save(AdvanceRequest.submit(
                findUser(userId),
                eligibility.workplace(),
                eligibility.contract(),
                eligibility.yearMonth(),
                idempotencyKey,
                request.requestedAmount(),
                eligibility.response().estimatedFee(),
                eligibility.response().estimatedRepaymentDate(),
                request.requestedAt(),
                eligibility.response().availableAmount(),
                eligibility.response().maxCap(),
                eligibility.response().policyRate(),
                eligibility.response().reflectedWorkDays(),
                eligibility.response().reflectedWorkMinutes(),
                eligibility.response().needsReviewRecordCount()
        ));

        return new AdvanceCreateResult(toRequestResponse(saved), false);
    }

    @Transactional(readOnly = true)
    public AdvanceRequestListResponse getRequests(Long userId, String month) {
        parseYearMonth(month);
        List<AdvanceRequestListItemResponse> requests = advanceRequestRepository
                .findByUserIdAndYearMonthOrderByRequestedAtDescCreatedAtDesc(userId, month)
                .stream()
                .map(this::toListItemResponse)
                .toList();
        return new AdvanceRequestListResponse(month, requests);
    }

    @Transactional(readOnly = true)
    public AdvanceRequestDetailResponse getRequest(Long userId, Long requestId) {
        AdvanceRequest request = advanceRequestRepository.findByIdAndUserId(requestId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.ADVANCE_REQUEST_NOT_FOUND));
        return toDetailResponse(request);
    }

    private AdvanceEligibilityResult evaluateEligibility(Long userId, Long workplaceId) {
        Workplace workplace = workplaceRepository.findByIdAndUserId(workplaceId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKPLACE_NOT_FOUND));
        WorkContract contract = workContractRepository
                .findFirstByWorkplaceIdAndWorkplaceUserIdAndEffectiveToIsNullOrderByEffectiveFromDesc(workplaceId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.ACTIVE_CONTRACT_REQUIRED));

        YearMonth targetMonth = resolveTargetMonth(userId, workplaceId);
        WorkProofMonthlySummaryContractResponse summary = toMonthlySummary(userId, workplaceId, targetMonth);
        boolean hasOpenAdvanceRequest = advanceRequestRepository.existsByUserIdAndWorkplaceIdAndYearMonthAndStatusIn(
                userId,
                workplaceId,
                targetMonth.toString(),
                List.of(AdvanceRequestStatus.SUBMITTED, AdvanceRequestStatus.APPROVED)
        );

        AdvanceEligibilityResponse response = advancePolicyEngine.evaluate(
                workplaceId,
                contract,
                summary,
                hasOpenAdvanceRequest,
                java.time.LocalDate.now(),
                targetMonth
        );

        return new AdvanceEligibilityResult(
                workplace,
                contract,
                targetMonth.toString(),
                response,
                advancePolicyEngine.isHardBlocked(response)
        );
    }

    private WorkProofMonthlySummaryContractResponse toMonthlySummary(Long userId, Long workplaceId, YearMonth targetMonth) {
        return workProofLane1Service.getMonthlySummary(userId, targetMonth.toString(), workplaceId);
    }

    private YearMonth resolveTargetMonth(Long userId, Long workplaceId) {
        return workProofRepository.findFirstByUserIdAndWorkplaceIdOrderByWorkDateDescClockInAtDesc(userId, workplaceId)
                .map(workProof -> YearMonth.from(workProof.getWorkDate()))
                .orElse(YearMonth.now());
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    private YearMonth parseYearMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.INVALID_YEAR_MONTH);
        }
    }

    private AdvanceRequestResponse toRequestResponse(AdvanceRequest request) {
        return new AdvanceRequestResponse(
                request.getId(),
                request.getStatus().name(),
                request.getApprovedAmount(),
                request.getFeeAmount(),
                request.getRepaymentDueDate(),
                toSnapshotResponse(request)
        );
    }

    private AdvanceRequestListItemResponse toListItemResponse(AdvanceRequest request) {
        return new AdvanceRequestListItemResponse(
                request.getId(),
                request.getWorkplace().getId(),
                request.getRequestedAmount(),
                request.getApprovedAmount(),
                request.getStatus().name(),
                request.getRepaymentDueDate(),
                request.getRequestedAt()
        );
    }

    private AdvanceRequestDetailResponse toDetailResponse(AdvanceRequest request) {
        return new AdvanceRequestDetailResponse(
                request.getId(),
                request.getWorkplace().getId(),
                request.getRequestedAmount(),
                request.getApprovedAmount(),
                request.getFeeAmount(),
                request.getStatus().name(),
                request.getRepaymentDueDate(),
                toSnapshotResponse(request),
                request.getCreatedAt()
        );
    }

    private AdvanceEligibilitySnapshotResponse toSnapshotResponse(AdvanceRequest request) {
        return new AdvanceEligibilitySnapshotResponse(
                request.getSnapshotAvailableAmount(),
                request.getSnapshotMaxCap(),
                request.getSnapshotPolicyRate(),
                request.getSnapshotReflectedWorkDays(),
                request.getSnapshotReflectedWorkMinutes(),
                request.getSnapshotNeedsReviewRecordCount()
        );
    }
}
