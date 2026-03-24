package com.workproofpay.backend.advance.service;

import com.workproofpay.backend.advance.api.dto.request.CreateAdvanceRequest;
import com.workproofpay.backend.advance.api.dto.response.*;
import com.workproofpay.backend.advance.model.AdvancePayout;
import com.workproofpay.backend.advance.model.AdvancePayoutStatus;
import com.workproofpay.backend.advance.model.AdvanceRequest;
import com.workproofpay.backend.advance.model.AdvanceRequestStatus;
import com.workproofpay.backend.advance.model.AdvanceSettlementStatus;
import com.workproofpay.backend.advance.repo.AdvancePayoutRepository;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdvanceService {

    private final AdvanceRequestRepository advanceRequestRepository;
    private final AdvancePayoutRepository advancePayoutRepository;
    private final UserRepository userRepository;
    private final WorkplaceRepository workplaceRepository;
    private final WorkContractRepository workContractRepository;
    private final WorkProofRepository workProofRepository;
    private final WorkProofLane1Service workProofLane1Service;
    private final AdvancePolicyResolver advancePolicyResolver;
    private final AdvancePolicyEngine advancePolicyEngine;
    private final AdvanceRequestViewStatusResolver advanceRequestViewStatusResolver;

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
            if (!existing.matches(idempotencyKey, request.workplaceId(), request.requestedAmountAtomic(), request.requestedAt())) {
                throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD);
            }
            return new AdvanceCreateResult(toRequestResponse(existing), true);
        }

        AdvanceEligibilityResult eligibility = evaluateEligibility(userId, request.workplaceId());
        if (eligibility.hardBlocked()) {
            throw new ApiException(ErrorCode.ADVANCE_NOT_ELIGIBLE);
        }
        if (request.requestedAmountAtomic() > eligibility.response().availableAmountAtomic()) {
            throw new ApiException(ErrorCode.REQUEST_AMOUNT_EXCEEDS_LIMIT);
        }

        AdvanceRequest saved = advanceRequestRepository.save(AdvanceRequest.submit(
                findUser(userId),
                eligibility.workplace(),
                eligibility.contract(),
                eligibility.yearMonth(),
                idempotencyKey,
                eligibility.response().assetSymbol(),
                eligibility.response().assetDecimals(),
                eligibility.response().exchangeRateSnapshot(),
                request.requestedAmountAtomic(),
                toDisplayKrwAmount(request.requestedAmountAtomic(), eligibility.response().exchangeRateSnapshot(), eligibility.response().assetDecimals()),
                eligibility.response().estimatedFeeAmountAtomic(),
                eligibility.response().estimatedFeeDisplayKrwAmount(),
                eligibility.response().settlementDueDate(),
                request.requestedAt(),
                eligibility.response().availableAmountAtomic(),
                eligibility.response().availableDisplayKrwAmount(),
                eligibility.response().maxCapAmountAtomic(),
                eligibility.response().maxCapDisplayKrwAmount(),
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
        List<AdvanceRequest> requests = advanceRequestRepository
                .findByUserIdAndYearMonthOrderByRequestedAtDescCreatedAtDesc(userId, month);
        Map<Long, AdvancePayout> payoutsByRequestId = mapPayoutsByRequestId(
                requests.stream().map(AdvanceRequest::getId).toList()
        );
        List<AdvanceRequestListItemResponse> items = requests.stream()
                .map(request -> toListItemResponse(request, payoutsByRequestId.get(request.getId())))
                .toList();
        return new AdvanceRequestListResponse(month, items);
    }

    @Transactional(readOnly = true)
    public AdvanceRequestDetailResponse getRequest(Long userId, Long requestId) {
        AdvanceRequest request = advanceRequestRepository.findByIdAndUserId(requestId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.ADVANCE_REQUEST_NOT_FOUND));
        AdvancePayout payout = advancePayoutRepository.findByAdvanceRequestId(request.getId()).orElse(null);
        return toDetailResponse(request, payout);
    }

    private AdvanceEligibilityResult evaluateEligibility(Long userId, Long workplaceId) {
        Workplace workplace = workplaceRepository.findByIdAndUserId(workplaceId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKPLACE_NOT_FOUND));
        WorkContract contract = workContractRepository
                .findFirstByWorkplaceIdAndWorkplaceUserIdAndEffectiveToIsNullOrderByEffectiveFromDesc(workplaceId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.ACTIVE_CONTRACT_REQUIRED));

        var policy = advancePolicyResolver.resolve();
        YearMonth targetMonth = resolveTargetMonth(userId, workplaceId, policy);
        WorkProofMonthlySummaryContractResponse summary = toMonthlySummary(userId, workplaceId, targetMonth);
        AdvanceCycleUsage cycleUsage = summarizeCycleUsage(
                advanceRequestRepository.findByUserIdAndWorkplaceIdAndYearMonthOrderByRequestedAtDescCreatedAtDesc(
                        userId,
                        workplaceId,
                        targetMonth.toString()
                )
        );

        AdvanceEligibilityResponse response = advancePolicyEngine.evaluate(
                policy,
                workplaceId,
                contract,
                summary,
                cycleUsage.alreadyAdvancedAmountAtomic(),
                cycleUsage.alreadyAdvancedDisplayKrwAmount(),
                cycleUsage.hasOutstandingAdvance(),
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

    private AdvanceCycleUsage summarizeCycleUsage(List<AdvanceRequest> cycleRequests) {
        if (cycleRequests.isEmpty()) {
            return new AdvanceCycleUsage(0L, 0L, false);
        }
        Map<Long, AdvancePayout> payoutsByRequestId = mapPayoutsByRequestId(
                cycleRequests.stream().map(AdvanceRequest::getId).toList()
        );

        long alreadyAdvancedAmountAtomic = 0L;
        long alreadyAdvancedDisplayKrwAmount = 0L;
        boolean hasOutstandingAdvance = false;
        for (AdvanceRequest request : cycleRequests) {
            if (request.getStatus() == AdvanceRequestStatus.SUBMITTED) {
                hasOutstandingAdvance = true;
                continue;
            }
            if (request.getStatus() != AdvanceRequestStatus.APPROVED) {
                continue;
            }

            AdvancePayout payout = payoutsByRequestId.get(request.getId());
            if (payout == null || isActivePayout(payout.getStatus())) {
                hasOutstandingAdvance = true;
                continue;
            }
            if (payout.getStatus() == AdvancePayoutStatus.CONFIRMED) {
                alreadyAdvancedAmountAtomic += nullSafe(request.getApprovedAmountAtomic());
                alreadyAdvancedDisplayKrwAmount += nullSafe(request.getApprovedDisplayKrwAmount());
            }
        }
        return new AdvanceCycleUsage(alreadyAdvancedAmountAtomic, alreadyAdvancedDisplayKrwAmount, hasOutstandingAdvance);
    }

    private boolean isActivePayout(AdvancePayoutStatus status) {
        return status == AdvancePayoutStatus.REQUESTED
                || status == AdvancePayoutStatus.SIGNED
                || status == AdvancePayoutStatus.BROADCASTED;
    }

    private long nullSafe(Long value) {
        return value == null ? 0L : value;
    }

    private WorkProofMonthlySummaryContractResponse toMonthlySummary(Long userId, Long workplaceId, YearMonth targetMonth) {
        return workProofLane1Service.getMonthlySummary(userId, targetMonth.toString(), workplaceId);
    }

    private YearMonth resolveTargetMonth(Long userId, Long workplaceId, com.workproofpay.backend.advance.model.AdvancePolicy policy) {
        YearMonth latestWorkedMonth = workProofRepository.findFirstByUserIdAndWorkplaceIdOrderByWorkDateDescClockInAtDesc(userId, workplaceId)
                .map(workProof -> YearMonth.from(workProof.getWorkDate()))
                .orElse(null);
        return advancePolicyEngine.resolveTargetMonth(policy, java.time.LocalDate.now(), latestWorkedMonth);
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
        AdvancePayout payout = advancePayoutRepository.findByAdvanceRequestId(request.getId()).orElse(null);
        AdvanceRequestViewStatusResolver.AdvanceRequestViewStatus viewStatus = advanceRequestViewStatusResolver.resolve(request, payout);
        return new AdvanceRequestResponse(
                request.getId(),
                request.getAssetSymbol(),
                request.getAssetDecimals(),
                request.getExchangeRateSnapshot(),
                viewStatus.status(),
                viewStatus.requestStatus(),
                viewStatus.payoutStatus(),
                request.getApprovedAmountAtomic(),
                request.getApprovedDisplayKrwAmount(),
                request.getFeeAmountAtomic(),
                request.getFeeDisplayKrwAmount(),
                toSettlementStatus(request),
                toSettlementDueDate(request),
                request.getRepaymentDueDate(),
                toSnapshotResponse(request)
        );
    }

    private AdvanceRequestListItemResponse toListItemResponse(AdvanceRequest request, AdvancePayout payout) {
        AdvanceRequestViewStatusResolver.AdvanceRequestViewStatus viewStatus = advanceRequestViewStatusResolver.resolve(request, payout);
        return new AdvanceRequestListItemResponse(
                request.getId(),
                request.getWorkplace().getId(),
                request.getAssetSymbol(),
                request.getAssetDecimals(),
                request.getExchangeRateSnapshot(),
                request.getRequestedAmountAtomic(),
                request.getRequestedDisplayKrwAmount(),
                request.getApprovedAmountAtomic(),
                request.getApprovedDisplayKrwAmount(),
                viewStatus.status(),
                viewStatus.requestStatus(),
                viewStatus.payoutStatus(),
                payout != null ? payout.getTxHash() : null,
                toSettlementStatus(request),
                toSettlementDueDate(request),
                request.getRepaymentDueDate(),
                request.getRequestedAt()
        );
    }

    private AdvanceRequestDetailResponse toDetailResponse(AdvanceRequest request, AdvancePayout payout) {
        AdvanceRequestViewStatusResolver.AdvanceRequestViewStatus viewStatus = advanceRequestViewStatusResolver.resolve(request, payout);
        return new AdvanceRequestDetailResponse(
                request.getId(),
                request.getWorkplace().getId(),
                request.getAssetSymbol(),
                request.getAssetDecimals(),
                request.getExchangeRateSnapshot(),
                request.getRequestedAmountAtomic(),
                request.getRequestedDisplayKrwAmount(),
                request.getApprovedAmountAtomic(),
                request.getApprovedDisplayKrwAmount(),
                request.getFeeAmountAtomic(),
                request.getFeeDisplayKrwAmount(),
                viewStatus.status(),
                viewStatus.requestStatus(),
                viewStatus.payoutStatus(),
                payout != null ? payout.getTxHash() : null,
                toSettlementStatus(request),
                toSettlementDueDate(request),
                request.getRepaymentDueDate(),
                toSnapshotResponse(request),
                request.getCreatedAt()
        );
    }

    private AdvanceSettlementStatus toSettlementStatus(AdvanceRequest request) {
        return request.getStatus() == AdvanceRequestStatus.REJECTED
                ? null
                : AdvanceSettlementStatus.SCHEDULED_FOR_PAYDAY;
    }

    private java.time.LocalDate toSettlementDueDate(AdvanceRequest request) {
        return toSettlementStatus(request) == null ? null : request.getRepaymentDueDate();
    }

    private Map<Long, AdvancePayout> mapPayoutsByRequestId(List<Long> requestIds) {
        if (requestIds.isEmpty()) {
            return Map.of();
        }
        return advancePayoutRepository.findByAdvanceRequestIdIn(requestIds).stream()
                .collect(Collectors.toMap(AdvancePayout::getAdvanceRequestId, Function.identity()));
    }

    private AdvanceEligibilitySnapshotResponse toSnapshotResponse(AdvanceRequest request) {
        return new AdvanceEligibilitySnapshotResponse(
                request.getAssetSymbol(),
                request.getAssetDecimals(),
                request.getExchangeRateSnapshot(),
                request.getSnapshotAvailableAmountAtomic(),
                request.getSnapshotAvailableDisplayKrwAmount(),
                request.getSnapshotMaxCapAmountAtomic(),
                request.getSnapshotMaxCapDisplayKrwAmount(),
                request.getSnapshotPolicyRate(),
                request.getSnapshotReflectedWorkDays(),
                request.getSnapshotReflectedWorkMinutes(),
                request.getSnapshotNeedsReviewRecordCount()
        );
    }

    private Long toDisplayKrwAmount(Long amountAtomic, java.math.BigDecimal exchangeRate, Integer assetDecimals) {
        if (amountAtomic == null || amountAtomic <= 0 || exchangeRate == null || assetDecimals == null) {
            return 0L;
        }
        return java.math.BigDecimal.valueOf(amountAtomic)
                .multiply(exchangeRate)
                .divide(java.math.BigDecimal.TEN.pow(assetDecimals), 0, java.math.RoundingMode.DOWN)
                .longValue();
    }

    private record AdvanceCycleUsage(
            long alreadyAdvancedAmountAtomic,
            long alreadyAdvancedDisplayKrwAmount,
            boolean hasOutstandingAdvance
    ) {
    }
}
