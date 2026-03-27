package com.workproofpay.backend.advance;

import com.workproofpay.backend.advance.api.dto.response.AdvanceEligibilityResponse;
import com.workproofpay.backend.advance.service.AdvancePolicyDefaults;
import com.workproofpay.backend.advance.service.AdvancePolicyEngine;
import com.workproofpay.backend.workproof.api.dto.response.WorkProofMonthlySummaryContractResponse;
import com.workproofpay.backend.workproof.model.WorkContract;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdvancePolicyEngineTest {

    private static final long AVAILABLE_B_ATOMIC = 103_448_275L;
    private static final long REDUCED_CAP_ATOMIC = 34_482_758L;
    private static final long ZERO_ATOMIC = 0L;
    private static final long AVAILABLE_B_REFERENCE_KRW = 150_000L;
    private static final long REDUCED_CAP_REFERENCE_KRW = 50_000L;
    private static final long FEE_ATOMIC = 3_448_275L;
    private static final long FEE_REFERENCE_KRW = 5_000L;

    private final AdvancePolicyEngine engine = new AdvancePolicyEngine();
    private final com.workproofpay.backend.advance.model.AdvancePolicy policy = AdvancePolicyDefaults.createDefault();

    @Test
    void blocksEligibilityWhenVerifiedWorkIsMissing() {
        AdvanceEligibilityResponse response = engine.evaluate(
                policy,
                1L,
                contractWithHourlyWage(12_000),
                summary(0, 0, 0, 0, 0),
                0L,
                0L,
                false,
                LocalDate.of(2026, 3, 16),
                YearMonth.of(2026, 3)
        );

        assertThat(response.availableAmountAtomic()).isEqualTo(ZERO_ATOMIC);
        assertThat(response.availableDisplayKrwAmount()).isZero();
        assertThat(response.blockReasonCodes()).contains("INSUFFICIENT_VERIFIED_WORK");
        assertThat(engine.isHardBlocked(response)).isTrue();
    }

    @Test
    void calculatesAvailableAmountFromTierRatioAndCap() {
        AdvanceEligibilityResponse response = engine.evaluate(
                policy,
                1L,
                contractWithHourlyWage(10_000),
                summary(12, 5_760, 5_760, 0, 0),
                0L,
                0L,
                false,
                LocalDate.of(2026, 3, 1),
                YearMonth.of(2026, 3)
        );

        assertThat(response.repaymentTier()).isEqualTo("B");
        assertThat(response.assetSymbol()).isEqualTo("dUSDC");
        assertThat(response.assetDecimals()).isEqualTo(6);
        assertThat(response.exchangeRateSnapshot()).isEqualByComparingTo("1450");
        assertThat(response.reflectedEarnedDisplayKrwAmount()).isEqualTo(960_000L);
        assertThat(response.alreadyAdvancedDisplayKrwAmount()).isZero();
        assertThat(response.availableAmountAtomic()).isEqualTo(AVAILABLE_B_ATOMIC);
        assertThat(response.availableDisplayKrwAmount()).isEqualTo(AVAILABLE_B_REFERENCE_KRW);
        assertThat(response.estimatedFeeAmountAtomic()).isEqualTo(FEE_ATOMIC);
        assertThat(response.estimatedFeeDisplayKrwAmount()).isEqualTo(FEE_REFERENCE_KRW);
        assertThat(response.currentTierName()).isEqualTo("B");
        assertThat(response.nextTierName()).isEqualTo("A");
        assertThat(response.progressToNextTier()).isEqualByComparingTo("0.6000");
        assertThat(response.remainingWorkDaysToNextTier()).isEqualTo(8);
        assertThat(response.nextTierExpectedCapDisplayKrw()).isEqualTo(300_000L);
        assertThat(response.blockReasonCodes()).isEmpty();
        assertThat(response.noticeReasonCodes()).isEmpty();
        assertThat(engine.isHardBlocked(response)).isFalse();
    }

    @Test
    void exposesPendingReviewAsNoticeInsteadOfBlockReason() {
        AdvanceEligibilityResponse response = engine.evaluate(
                policy,
                1L,
                contractWithHourlyWage(10_000),
                summary(12, 5_760, 5_760, 120, 1),
                0L,
                0L,
                false,
                LocalDate.of(2026, 3, 1),
                YearMonth.of(2026, 3)
        );

        assertThat(response.availableAmountAtomic()).isEqualTo(AVAILABLE_B_ATOMIC);
        assertThat(response.availableDisplayKrwAmount()).isEqualTo(AVAILABLE_B_REFERENCE_KRW);
        assertThat(response.blockReasonCodes()).isEmpty();
        assertThat(response.noticeReasonCodes()).contains("PENDING_WORKPROOF_REVIEW");
        assertThat(engine.isHardBlocked(response)).isFalse();
    }

    @Test
    void blocksEligibilityWhenOutstandingAdvanceExists() {
        AdvanceEligibilityResponse response = engine.evaluate(
                policy,
                1L,
                contractWithHourlyWage(10_000),
                summary(12, 5_760, 5_760, 0, 0),
                0L,
                0L,
                true,
                LocalDate.of(2026, 3, 16),
                YearMonth.of(2026, 3)
        );

        assertThat(response.availableAmountAtomic()).isEqualTo(ZERO_ATOMIC);
        assertThat(response.availableDisplayKrwAmount()).isZero();
        assertThat(response.blockReasonCodes()).contains("EXISTING_OUTSTANDING_ADVANCE");
        assertThat(response.noticeReasonCodes()).isEmpty();
        assertThat(engine.isHardBlocked(response)).isTrue();
    }

    @Test
    void blocksEligibilityOnlyOnRepaymentDate() {
        AdvanceEligibilityResponse response = engine.evaluate(
                policy,
                1L,
                contractWithHourlyWage(10_000),
                summary(12, 5_760, 5_760, 0, 0),
                0L,
                0L,
                false,
                LocalDate.of(2026, 3, AdvancePolicyDefaults.DEFAULT_PAYDAY_DAY),
                YearMonth.of(2026, 3)
        );

        assertThat(response.availableAmountAtomic()).isEqualTo(ZERO_ATOMIC);
        assertThat(response.availableDisplayKrwAmount()).isZero();
        assertThat(response.blockReasonCodes()).contains("ADVANCE_WINDOW_CLOSED_TODAY");
        assertThat(response.noticeReasonCodes()).isEmpty();
        assertThat(engine.isHardBlocked(response)).isTrue();
    }

    @Test
    void doesNotBlockDayBeforeRepaymentDate() {
        AdvanceEligibilityResponse response = engine.evaluate(
                policy,
                1L,
                contractWithHourlyWage(10_000),
                summary(12, 5_760, 5_760, 0, 0),
                0L,
                0L,
                false,
                LocalDate.of(2026, 3, AdvancePolicyDefaults.DEFAULT_PAYDAY_DAY - 1),
                YearMonth.of(2026, 3)
        );

        assertThat(response.availableAmountAtomic()).isEqualTo(REDUCED_CAP_ATOMIC);
        assertThat(response.availableDisplayKrwAmount()).isEqualTo(REDUCED_CAP_REFERENCE_KRW);
        assertThat(response.blockReasonCodes()).doesNotContain("ADVANCE_WINDOW_CLOSED_TODAY");
        assertThat(engine.isHardBlocked(response)).isFalse();
    }

    @Test
    void deductsAlreadyAdvancedAmountFromAvailableAmount() {
        AdvanceEligibilityResponse response = engine.evaluate(
                policy,
                1L,
                contractWithHourlyWage(10_000),
                summary(12, 5_760, 5_760, 0, 0),
                34_482_758L,
                50_000L,
                false,
                LocalDate.of(2026, 3, 1),
                YearMonth.of(2026, 3)
        );

        assertThat(response.alreadyAdvancedAmountAtomic()).isEqualTo(34_482_758L);
        assertThat(response.alreadyAdvancedDisplayKrwAmount()).isEqualTo(50_000L);
        assertThat(response.availableAmountAtomic()).isEqualTo(68_965_517L);
        assertThat(response.availableDisplayKrwAmount()).isEqualTo(100_000L);
    }

    @Test
    void marksTopTierAsFullyProgressedWithoutNextTier() {
        AdvanceEligibilityResponse response = engine.evaluate(
                policy,
                1L,
                contractWithHourlyWage(10_000),
                summary(20, 9_600, 9_600, 0, 0),
                0L,
                0L,
                false,
                LocalDate.of(2026, 3, 16),
                YearMonth.of(2026, 3)
        );

        assertThat(response.currentTierName()).isEqualTo("A");
        assertThat(response.nextTierName()).isNull();
        assertThat(response.progressToNextTier()).isEqualByComparingTo("1");
        assertThat(response.remainingWorkDaysToNextTier()).isZero();
    }

    @Test
    void keepsCurrentMonthOnMonthEndWhenPaydayUsesLastDaySemantics() {
        YearMonth targetMonth = engine.resolveTargetMonth(
                policy,
                LocalDate.of(2026, 4, 30),
                YearMonth.of(2026, 3)
        );

        assertThat(targetMonth).isEqualTo(YearMonth.of(2026, 4));
    }

    @Test
    void rollsTargetMonthForwardAfterExplicitPayday() {
        com.workproofpay.backend.advance.model.AdvancePolicy fifteenthPolicy =
                com.workproofpay.backend.advance.model.AdvancePolicy.global(
                        true,
                        15,
                        AdvancePolicyDefaults.SAME_DAY_ADVANCE_ALLOWED,
                        AdvancePolicyDefaults.REDUCED_CAP_DAYS_BEFORE_PAYDAY,
                        AdvancePolicyDefaults.ASSET_SYMBOL,
                        AdvancePolicyDefaults.ASSET_DECIMALS,
                        AdvancePolicyDefaults.REFERENCE_KRW_PER_ASSET,
                        AdvancePolicyDefaults.MAX_CAP_DISPLAY_KRW_AMOUNT,
                        AdvancePolicyDefaults.NEAR_PAYDAY_MAX_CAP_DISPLAY_KRW_AMOUNT,
                        com.workproofpay.backend.advance.model.AdvanceFeeType.FLAT,
                        AdvancePolicyDefaults.FLAT_FEE_DISPLAY_KRW_AMOUNT,
                        com.workproofpay.backend.advance.model.AdvanceSettlementMode.PAYDAY_AUTO_OFFSET,
                        false,
                        AdvancePolicyDefaults.DISCLAIMER
                );

        YearMonth targetMonth = engine.resolveTargetMonth(
                fifteenthPolicy,
                LocalDate.of(2026, 3, 26),
                YearMonth.of(2026, 3)
        );

        assertThat(targetMonth).isEqualTo(YearMonth.of(2026, 4));
    }

    @Test
    void keepsFutureWorkedMonthWhenItIsAlreadyNextCycle() {
        YearMonth targetMonth = engine.resolveTargetMonth(
                policy,
                LocalDate.of(2026, 3, 26),
                YearMonth.of(2026, 4)
        );

        assertThat(targetMonth).isEqualTo(YearMonth.of(2026, 4));
    }

    private WorkContract contractWithHourlyWage(long hourlyWage) {
        WorkContract contract = mock(WorkContract.class);
        when(contract.getNormalizedHourlyWage()).thenReturn(BigDecimal.valueOf(hourlyWage));
        return contract;
    }

    private WorkProofMonthlySummaryContractResponse summary(
            int reflectedWorkDays,
            long reflectedMinutes,
            long verifiedMinutes,
            long pendingMinutes,
            int needsReviewRecordCount
    ) {
        return new WorkProofMonthlySummaryContractResponse(
                "2026-03",
                1L,
                reflectedWorkDays,
                reflectedMinutes,
                0,
                0,
                0,
                new WorkProofMonthlySummaryContractResponse.ReflectionSummary(
                        reflectedWorkDays,
                        needsReviewRecordCount,
                        0
                ),
                new WorkProofMonthlySummaryContractResponse.IntegritySummary(
                        reflectedWorkDays,
                        reflectedWorkDays,
                        verifiedMinutes,
                        pendingMinutes,
                        List.of()
                ),
                new WorkProofMonthlySummaryContractResponse.FinanceReadinessSummary(
                        reflectedWorkDays,
                        reflectedWorkDays
                )
        );
    }
}
