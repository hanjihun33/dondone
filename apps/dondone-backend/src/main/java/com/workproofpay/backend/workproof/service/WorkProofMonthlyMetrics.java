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
}
