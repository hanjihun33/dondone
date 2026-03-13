package com.workproofpay.backend.workproof.api.dto.response;

import java.util.List;

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
