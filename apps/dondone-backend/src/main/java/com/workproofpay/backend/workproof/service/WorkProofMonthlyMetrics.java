package com.workproofpay.backend.workproof.service;

import java.time.LocalDate;
import java.util.List;

public record WorkProofMonthlyMetrics(
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
    public static WorkProofMonthlyMetrics empty(String yearMonth, LocalDate asOf) {
        return new WorkProofMonthlyMetrics(
                yearMonth,
                asOf,
                0,
                0L,
                0L,
                0L,
                0,
                0,
                0,
                List.of()
        );
    }
}
