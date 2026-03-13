package com.workproofpay.backend.workproof.api.dto.response;

import java.util.List;

/**
 * Wage/Advance가 raw record 대신 재사용할 수 있도록 만든 WorkProof lane 1 월간 요약 응답이다.
 */
public record WorkProofMonthlySummaryContractResponse(
        String month,
        Long workplaceId,
        int workDayCount,
        long totalWorkMinutes,
        long overtimeMinutes,
        long nightMinutes,
        int modifiedRecordCount,
        ReflectionSummary reflection,
        IntegritySummary integrity,
        FinanceReadinessSummary financeReadiness
) {
    public record ReflectionSummary(
            int reflectedRecordCount,
            int needsReviewRecordCount,
            int excludedRecordCount
    ) {
    }

    public record IntegritySummary(
            int recordedWorkDays,
            int reflectedWorkDays,
            long verifiedMinutes,
            long pendingMinutes,
            List<String> workproofRiskFlags
    ) {
    }

    public record FinanceReadinessSummary(
            int advanceEligibleWorkDays,
            int wageUsableWorkDays
    ) {
    }
}
