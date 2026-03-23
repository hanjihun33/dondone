package com.workproofpay.backend.advance;

import com.workproofpay.backend.advance.api.dto.response.AdvanceEligibilityResponse;
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

    private final AdvancePolicyEngine engine = new AdvancePolicyEngine();

    @Test
    void blocksEligibilityWhenVerifiedWorkIsMissing() {
        AdvanceEligibilityResponse response = engine.evaluate(
                1L,
                contractWithHourlyWage(12_000),
                summary(0, 0, 0, 0, 0),
                false,
                LocalDate.of(2026, 3, 16),
                YearMonth.of(2026, 3)
        );

        assertThat(response.availableAmount()).isZero();
        assertThat(response.blockReasonCodes()).contains("INSUFFICIENT_VERIFIED_WORK");
        assertThat(engine.isHardBlocked(response)).isTrue();
    }

    @Test
    void calculatesAvailableAmountFromTierRatioAndCap() {
        AdvanceEligibilityResponse response = engine.evaluate(
                1L,
                contractWithHourlyWage(10_000),
                summary(12, 5_760, 5_760, 0, 0),
                false,
                LocalDate.of(2026, 3, 16),
                YearMonth.of(2026, 3)
        );

        assertThat(response.repaymentTier()).isEqualTo("B");
        assertThat(response.availableAmount()).isEqualTo(150_000L);
        assertThat(response.blockReasonCodes()).isEmpty();
        assertThat(engine.isHardBlocked(response)).isFalse();
    }

    @Test
    void blocksEligibilityWhenOutstandingAdvanceExists() {
        AdvanceEligibilityResponse response = engine.evaluate(
                1L,
                contractWithHourlyWage(10_000),
                summary(12, 5_760, 5_760, 0, 0),
                true,
                LocalDate.of(2026, 3, 16),
                YearMonth.of(2026, 3)
        );

        assertThat(response.availableAmount()).isZero();
        assertThat(response.blockReasonCodes()).contains("EXISTING_OUTSTANDING_ADVANCE");
        assertThat(engine.isHardBlocked(response)).isTrue();
    }

    @Test
    void blocksEligibilityOnlyOnRepaymentDate() {
        AdvanceEligibilityResponse response = engine.evaluate(
                1L,
                contractWithHourlyWage(10_000),
                summary(12, 5_760, 5_760, 0, 0),
                false,
                LocalDate.of(2026, 3, 25),
                YearMonth.of(2026, 3)
        );

        assertThat(response.availableAmount()).isZero();
        assertThat(response.blockReasonCodes()).contains("PAYDAY_TOO_CLOSE");
        assertThat(engine.isHardBlocked(response)).isTrue();
    }

    @Test
    void doesNotBlockDayBeforeRepaymentDate() {
        AdvanceEligibilityResponse response = engine.evaluate(
                1L,
                contractWithHourlyWage(10_000),
                summary(12, 5_760, 5_760, 0, 0),
                false,
                LocalDate.of(2026, 3, 24),
                YearMonth.of(2026, 3)
        );

        assertThat(response.availableAmount()).isEqualTo(50_000L);
        assertThat(response.blockReasonCodes()).doesNotContain("PAYDAY_TOO_CLOSE");
        assertThat(engine.isHardBlocked(response)).isFalse();
    }

    @Test
    void rollsTargetMonthForwardAfterPayday() {
        YearMonth targetMonth = engine.resolveTargetMonth(
                LocalDate.of(2026, 3, 26),
                YearMonth.of(2026, 3)
        );

        assertThat(targetMonth).isEqualTo(YearMonth.of(2026, 4));
    }

    @Test
    void keepsFutureWorkedMonthWhenItIsAlreadyNextCycle() {
        YearMonth targetMonth = engine.resolveTargetMonth(
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
