package com.workproofpay.backend.wage.service;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.wage.api.dto.request.CreateWageDepositRequest;
import com.workproofpay.backend.wage.api.dto.response.WageDepositResponse;
import com.workproofpay.backend.wage.api.dto.response.WageEstimateResponse;
import com.workproofpay.backend.wage.api.dto.response.WageMonthlySummaryResponse;
import com.workproofpay.backend.wage.api.dto.response.WageSummaryResponse;
import com.workproofpay.backend.wage.model.WageDeposit;
import com.workproofpay.backend.wage.repo.WageDepositRepository;
import com.workproofpay.backend.workproof.api.dto.response.CurrentContractResponse;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofMonthlySummaryContractResponse;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofRecordListItemResponse;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofRecordListResponse;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofReflectionStatus;
import com.workproofpay.backend.workproof.service.WorkProofLane1Service;
import com.workproofpay.backend.workproof.service.WorkProofMonthlyMetrics;
import com.workproofpay.backend.workproof.service.WorkProofService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * Wage lane 1에서는 WorkProof upstream 결과를 조합해 월간 요약과 참고용 예상 급여 조회를 먼저 연다.
 * 실제 입금 비교와 verification 생성은 기존 summary/deposit 흐름과 별도 후속 단계로 남긴다.
 */
public class WageService {

    private static final String REFERENCE_ONLY_DISCLAIMER =
            "Reference-only estimate. Actual payment and deductions can differ by contract, payroll rules, and payslip details.";
    private static final String WAGE_LANE1_RULE_VERSION = "WAGE_LANE1_V0";

    private final WageDepositRepository wageDepositRepository;
    private final UserRepository userRepository;
    private final WorkProofService workProofService;
    private final WorkProofLane1Service workProofLane1Service;
    private final WageSummaryCalculator wageSummaryCalculator;

    @Transactional
    public WageDepositResponse createDeposit(Long userId, CreateWageDepositRequest request) {
        YearMonth yearMonth = parseYearMonth(request.yearMonth());
        if (!YearMonth.from(request.depositDate()).equals(yearMonth)) {
            throw new ApiException(ErrorCode.INVALID_DEPOSIT_DATE);
        }

        WageDeposit saved = wageDepositRepository.save(WageDeposit.record(
                findUser(userId),
                yearMonth.toString(),
                request.depositDate(),
                request.actualDepositAmount(),
                request.deductionsKnown(),
                request.note()
        ));

        return WageDepositResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public WageSummaryResponse getSummary(Long userId,
                                          String yearMonth,
                                          LocalDate asOf,
                                          long normalizedHourlyWage,
                                          int paydayDay) {
        if (normalizedHourlyWage <= 0) {
            throw new ApiException(ErrorCode.INVALID_HOURLY_WAGE);
        }
        if (paydayDay < 1 || paydayDay > 31) {
            throw new ApiException(ErrorCode.INVALID_PAYDAY);
        }

        WorkProofMonthlyMetrics metrics = workProofService.getMonthlyMetrics(userId, yearMonth, asOf);
        WageDeposit latestDeposit = findLatestDeposit(userId, metrics.yearMonth(), metrics.asOf());
        WageSummaryCalculator.WageSummarySnapshot snapshot =
                wageSummaryCalculator.summarize(metrics, normalizedHourlyWage, latestDeposit);
        return WageSummaryResponse.from(metrics, snapshot, paydayDay, REFERENCE_ONLY_DISCLAIMER);
    }

    @Transactional(readOnly = true)
    /**
     * WorkProof lane 1 월간 집계와 활성 계약을 조합해 Wage 월간 요약 응답을 만든다.
     */
    public WageMonthlySummaryResponse getMonthlySummary(Long userId, String month, Long workplaceId) {
        WageLane1Context context = loadLane1Context(userId, month, workplaceId);
        return WageMonthlySummaryResponse.from(
                context.contract(),
                context.summary(),
                context.includedRecordIds(),
                context.excludedPendingRecordCount()
        );
    }

    @Transactional(readOnly = true)
    /**
     * 같은 upstream 입력축에서 참고용 급여 추정 스냅샷까지 계산해 Wage estimate 응답을 만든다.
     */
    public WageEstimateResponse getEstimate(Long userId, String month, Long workplaceId) {
        WageLane1Context context = loadLane1Context(userId, month, workplaceId);
        WageMonthlySummaryResponse summary = WageMonthlySummaryResponse.from(
                context.contract(),
                context.summary(),
                context.includedRecordIds(),
                context.excludedPendingRecordCount()
        );
        WageSummaryCalculator.WageEstimateSnapshot estimate =
                wageSummaryCalculator.estimate(context.contract(), context.summary());

        return WageEstimateResponse.from(
                month,
                workplaceId,
                context.contract(),
                summary,
                estimate,
                REFERENCE_ONLY_DISCLAIMER,
                WAGE_LANE1_RULE_VERSION
        );
    }

    private WageDeposit findLatestDeposit(Long userId, String yearMonth, LocalDate asOf) {
        if (asOf == null) {
            return wageDepositRepository.findFirstByUserIdAndYearMonthOrderByDepositDateDescCreatedAtDesc(userId, yearMonth)
                    .orElse(null);
        }
        return wageDepositRepository.findFirstByUserIdAndYearMonthAndDepositDateLessThanEqualOrderByDepositDateDescCreatedAtDesc(
                        userId,
                        yearMonth,
                        asOf
                )
                .orElse(null);
    }

    private YearMonth parseYearMonth(String yearMonth) {
        try {
            return YearMonth.parse(yearMonth);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.INVALID_YEAR_MONTH);
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    // lane 1은 WorkProof의 summary/current-contract/records를 한 번에 읽어 Wage read-model로 고정한다.
    private WageLane1Context loadLane1Context(Long userId, String month, Long workplaceId) {
        WorkProofMonthlySummaryContractResponse summary = workProofLane1Service.getMonthlySummary(userId, month, workplaceId);
        CurrentContractResponse contract = workProofLane1Service.getCurrentContract(userId, workplaceId);
        WorkProofRecordListResponse records = workProofLane1Service.getRecords(userId, month, workplaceId);

        List<Long> includedRecordIds = new ArrayList<>();
        int excludedPendingRecordCount = 0;
        for (WorkProofRecordListItemResponse record : records.records()) {
            if (isIncludedForWage(record)) {
                includedRecordIds.add(record.recordId());
                continue;
            }
            excludedPendingRecordCount++;
        }

        return new WageLane1Context(contract, summary, includedRecordIds, excludedPendingRecordCount);
    }

    private boolean isIncludedForWage(WorkProofRecordListItemResponse record) {
        return record.reflectionStatus() == WorkProofReflectionStatus.REFLECTED;
    }

    private record WageLane1Context(
            CurrentContractResponse contract,
            WorkProofMonthlySummaryContractResponse summary,
            List<Long> includedRecordIds,
            int excludedPendingRecordCount
    ) {
    }
}
