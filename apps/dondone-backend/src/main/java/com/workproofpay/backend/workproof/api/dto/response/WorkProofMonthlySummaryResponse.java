package com.workproofpay.backend.workproof.api.dto.response;

import com.workproofpay.backend.workproof.service.WorkProofMonthlyMetrics;

import java.time.LocalDate;
import java.util.List;

public record WorkProofMonthlySummaryResponse(
        String yearMonth,
        LocalDate asOf,
        int totalWorkDays,
        long totalWorkedMinutes,
        long totalOvertimeMinutes,
        long totalNightMinutes,
        int editedRecordCount,
        int reflectedRecordCount,
        int pendingRecordCount,
        List<Long> reflectedWorkProofIds
) {
    public static WorkProofMonthlySummaryResponse from(WorkProofMonthlyMetrics metrics) {
        return new WorkProofMonthlySummaryResponse(
                metrics.yearMonth(),
                metrics.asOf(),
                metrics.totalWorkDays(),
                metrics.totalWorkedMinutes(),
                metrics.totalOvertimeMinutes(),
                metrics.totalNightMinutes(),
                metrics.editedRecordCount(),
                metrics.reflectedRecordCount(),
                metrics.pendingRecordCount(),
                metrics.reflectedWorkProofIds()
        );
    }
}
