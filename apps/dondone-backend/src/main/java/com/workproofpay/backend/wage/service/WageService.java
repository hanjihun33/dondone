package com.workproofpay.backend.wage.service;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.wage.api.dto.request.CreateWageDepositRequest;
import com.workproofpay.backend.wage.api.dto.request.CreateWageVerificationRequest;
import com.workproofpay.backend.wage.api.dto.response.WageDepositResponse;
import com.workproofpay.backend.wage.api.dto.response.WageEstimateResponse;
import com.workproofpay.backend.wage.api.dto.response.WageMonthlySummaryResponse;
import com.workproofpay.backend.wage.api.dto.response.WageSummaryResponse;
import com.workproofpay.backend.wage.api.dto.response.WageVerificationCreatedResponse;
import com.workproofpay.backend.wage.api.dto.response.WageVerificationDetailResponse;
import com.workproofpay.backend.wage.model.WageDeposit;
import com.workproofpay.backend.wage.model.WageVerification;
import com.workproofpay.backend.wage.model.WageVerificationDraft;
import com.workproofpay.backend.wage.model.WageVerificationPossibleCauseSnapshot;
import com.workproofpay.backend.wage.model.WageVerificationStatus;
import com.workproofpay.backend.wage.repo.WageDepositRepository;
import com.workproofpay.backend.wage.repo.WageVerificationRepository;
import com.workproofpay.backend.workproof.api.dto.response.CurrentContractResponse;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofMonthlySummaryContractResponse;
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

/**
 * WorkProof 입력을 조합해 Wage 화면과 이후 문서 흐름에서 쓰는 읽기 모델을 만든다.
 * verification은 문서/Claim의 스냅샷 기준점이라서 기존 summary/deposit 흐름과 분리해 둔다.
 */
@Service
@RequiredArgsConstructor
public class WageService {

    private static final String REFERENCE_ONLY_DISCLAIMER =
            "Reference-only estimate. Actual payment and deductions can differ by contract, payroll rules, and payslip details.";
    private static final String WAGE_LANE1_RULE_VERSION = "WAGE_LANE1_V0";
    private static final String NEXT_ACTION_VIEW_EVIDENCE = "VIEW_EVIDENCE";
    private static final String NEXT_ACTION_REQUEST_EMPLOYER_CONFIRMATION = "REQUEST_EMPLOYER_CONFIRMATION";
    private static final String NEXT_ACTION_PREPARE_PROOF_PACK = "PREPARE_PROOF_PACK";
    private static final String EMPLOYER_SUPPORT_NOT_REQUESTED = "NOT_REQUESTED";
    private static final String EMPLOYER_SUPPORT_REQUEST_RECOMMENDED = "REQUEST_RECOMMENDED";

    private final WageDepositRepository wageDepositRepository;
    private final WageVerificationRepository wageVerificationRepository;
    private final UserRepository userRepository;
    private final WorkProofService workProofService;
    private final WorkProofLane1Service workProofLane1Service;
    private final WageSummaryCalculator wageSummaryCalculator;
    private final WageVerificationRelatedActionsService wageVerificationRelatedActionsService;

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

    /**
     * worker self-check 결과를 verification 스냅샷으로 저장해,
     * 이후 문서/Claim 흐름이 변경 가능한 upstream 데이터를 다시 계산하지 않고 같은 근거를 재사용하게 한다.
     */
    @Transactional
    public WageVerificationCreatedResponse createVerification(Long userId, CreateWageVerificationRequest request) {
        String month = parseYearMonth(request.month()).toString();
        WageLane1Context context = loadLane1Context(userId, month, request.workplaceId());
        WageSummaryCalculator.WageVerificationSnapshot verificationSnapshot = wageSummaryCalculator.verify(
                context.contract(),
                context.summary(),
                request.actualDepositAmount(),
                request.deductionsKnown()
        );

        WageVerification verification = wageVerificationRepository.save(WageVerification.record(
                findUser(userId),
                toVerificationDraft(request, month, context, verificationSnapshot)
        ));

        return WageVerificationCreatedResponse.from(verification, buildNextActions(verification));
    }

    @Transactional(readOnly = true)
    public WageSummaryResponse getSummary(Long userId,
                                          String yearMonth,
                                          LocalDate asOf) {
        CurrentContractResponse contract = workProofLane1Service.getPrimaryCurrentContract(userId);
        WorkProofMonthlyMetrics metrics = workProofService.getMonthlyMetrics(userId, yearMonth, asOf);
        WageDeposit latestDeposit = findLatestDeposit(userId, metrics.yearMonth(), metrics.asOf());
        WageSummaryCalculator.WageSummarySnapshot snapshot =
                wageSummaryCalculator.summarize(
                        metrics,
                        contract.normalizedHourlyWage().longValue(),
                        latestDeposit
                );
        return WageSummaryResponse.from(metrics, snapshot, contract.paydayDay(), REFERENCE_ONLY_DISCLAIMER);
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

    /**
     * WorkProof lane 1 월간 summary를 Wage 월간 summary 읽기 모델로 변환한다.
     */
    @Transactional(readOnly = true)
    public WageMonthlySummaryResponse getMonthlySummary(Long userId, String month, Long workplaceId) {
        WageLane1Context context = loadLane1Context(userId, month, workplaceId);
        return toMonthlySummaryResponse(context);
    }

    /**
     * monthly summary와 같은 lane 1 입력축을 쓰되, 참고용 estimate를 함께 계산해 응답한다.
     */
    @Transactional(readOnly = true)
    public WageEstimateResponse getEstimate(Long userId, String month, Long workplaceId) {
        WageLane1Context context = loadLane1Context(userId, month, workplaceId);
        WageMonthlySummaryResponse summary = toMonthlySummaryResponse(context);
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

    @Transactional(readOnly = true)
    public WageVerificationDetailResponse getVerification(Long userId, Long verificationId) {
        WageVerification verification = wageVerificationRepository.findByIdAndUserId(verificationId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.WAGE_VERIFICATION_NOT_FOUND));

        return WageVerificationDetailResponse.from(
                verification,
                buildEmployerSupport(verification),
                buildRelatedActions(userId, verification)
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

    /**
     * aggregate 생성 전에 request, upstream 근거, 계산 결과를 draft 하나로 평탄화해
     * entity 경계를 안정적으로 유지한다.
     */
    private WageVerificationDraft toVerificationDraft(CreateWageVerificationRequest request,
                                                      String month,
                                                      WageLane1Context context,
                                                      WageSummaryCalculator.WageVerificationSnapshot verificationSnapshot) {
        return new VerificationDraftAssembler(
                month,
                request,
                context.summary(),
                context.includedRecordIds(),
                verificationSnapshot
        ).assemble();
    }

    /**
     * cause 문구도 verification 스냅샷에 함께 저장해,
     * 이후 calculator 문구가 바뀌더라도 detail/doc 생성이 당시 설명을 그대로 재사용하게 한다.
     */
    private List<WageVerificationPossibleCauseSnapshot> toPossibleCauseSnapshots(
            WageSummaryCalculator.WageVerificationSnapshot verificationSnapshot
    ) {
        return verificationSnapshot.possibleCauses().stream()
                .map(cause -> WageVerificationPossibleCauseSnapshot.of(cause.code(), cause.title(), cause.detail()))
                .toList();
    }

    /**
     * P0에서는 별도 상태 기계 대신 verification 결과만으로 다음 액션을 파생한다.
     */
    private List<String> buildNextActions(WageVerification verification) {
        List<String> nextActions = new ArrayList<>();
        nextActions.add(NEXT_ACTION_VIEW_EVIDENCE);
        if (requiresEmployerFollowUp(verification.getStatus())) {
            nextActions.add(NEXT_ACTION_REQUEST_EMPLOYER_CONFIRMATION);
            nextActions.add(NEXT_ACTION_PREPARE_PROOF_PACK);
        }
        return List.copyOf(nextActions);
    }

    private WageVerificationDetailResponse.EmployerSupportSnapshot buildEmployerSupport(WageVerification verification) {
        boolean recommended = requiresEmployerFollowUp(verification.getStatus());
        return new WageVerificationDetailResponse.EmployerSupportSnapshot(
                false,
                recommended,
                recommended ? EMPLOYER_SUPPORT_REQUEST_RECOMMENDED : EMPLOYER_SUPPORT_NOT_REQUESTED
        );
    }

    private WageVerificationDetailResponse.RelatedActionsSnapshot buildRelatedActions(Long userId, WageVerification verification) {
        return wageVerificationRelatedActionsService.build(userId, verification);
    }

    private WageMonthlySummaryResponse toMonthlySummaryResponse(WageLane1Context context) {
        return WageMonthlySummaryResponse.from(
                context.contract(),
                context.summary(),
                context.includedRecordIds(),
                context.excludedPendingRecordCount()
        );
    }

    /**
     * lane 1에서는 WorkProof summary, current contract, records를 조합해 Wage 소유의 입력 묶음을 만든다.
     */
    private WageLane1Context loadLane1Context(Long userId, String month, Long workplaceId) {
        WorkProofLane1Service.WageLane1Snapshot lane1Snapshot =
                workProofLane1Service.getWageLane1Snapshot(userId, month, workplaceId);

        List<Long> includedRecordIds = new ArrayList<>();
        int excludedPendingRecordCount = 0;
        for (WorkProofLane1Service.WageLane1RecordSnapshot record : lane1Snapshot.records()) {
            if (isIncludedForWage(record)) {
                includedRecordIds.add(record.recordId());
                continue;
            }
            excludedPendingRecordCount++;
        }

        return new WageLane1Context(
                lane1Snapshot.contract(),
                lane1Snapshot.summary(),
                includedRecordIds,
                excludedPendingRecordCount
        );
    }

    private boolean isIncludedForWage(WorkProofLane1Service.WageLane1RecordSnapshot record) {
        return record.reflectionStatus() == WorkProofReflectionStatus.REFLECTED;
    }

    private boolean requiresEmployerFollowUp(WageVerificationStatus status) {
        return status == WageVerificationStatus.CHECK_REQUIRED;
    }

    private record WageLane1Context(
            CurrentContractResponse contract,
            WorkProofMonthlySummaryContractResponse summary,
            List<Long> includedRecordIds,
            int excludedPendingRecordCount
    ) {
    }

    /**
     * 필드가 많은 WageVerificationDraft 생성자를 읽기 쉽게 유지하려고
     * 스냅샷 source별 값을 한 번씩만 펼쳐 조립한다.
     */
    private final class VerificationDraftAssembler {

        private final String month;
        private final CreateWageVerificationRequest request;
        private final WorkProofMonthlySummaryContractResponse summary;
        private final List<Long> includedRecordIds;
        private final WageSummaryCalculator.WageVerificationSnapshot verificationSnapshot;

        private VerificationDraftAssembler(String month,
                                           CreateWageVerificationRequest request,
                                           WorkProofMonthlySummaryContractResponse summary,
                                           List<Long> includedRecordIds,
                                           WageSummaryCalculator.WageVerificationSnapshot verificationSnapshot) {
            this.month = month;
            this.request = request;
            this.summary = summary;
            this.includedRecordIds = includedRecordIds;
            this.verificationSnapshot = verificationSnapshot;
        }

        private WageVerificationDraft assemble() {
            WageSummaryCalculator.WageEstimateSnapshot estimate = verificationSnapshot.estimate();
            WageSummaryCalculator.ThresholdSnapshot threshold = verificationSnapshot.threshold();

            return new WageVerificationDraft(
                    month,
                    request.workplaceId(),
                    summary.overtimeMinutes(),
                    summary.nightMinutes(),
                    summary.modifiedRecordCount(),
                    request.actualDepositAmount(),
                    request.deductionsKnown(),
                    request.memo(),
                    verificationSnapshot.status(),
                    verificationSnapshot.resolutionStage(),
                    estimate.baseEstimate(),
                    estimate.overtimePremium(),
                    estimate.nightPremium(),
                    estimate.estimatedTotal(),
                    verificationSnapshot.differenceAmount(),
                    verificationSnapshot.differenceRate(),
                    threshold.absoluteWon(),
                    threshold.relativePercent(),
                    threshold.deductionRelaxed(),
                    includedRecordIds,
                    toPossibleCauseSnapshots(verificationSnapshot)
            );
        }
    }
}
