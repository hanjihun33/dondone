package com.workproofpay.backend.wage.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record WageSummaryResponse(
        String yearMonth,
        LocalDate asOf,
        int workDays,
        long totalWorkedMinutes,
        BigDecimal totalWorkedHours,
        long overtimeMinutes,
        BigDecimal overtimeHours,
        long nightMinutes,
        BigDecimal nightHours,
        long normalizedHourlyWage,
        long estimatedBaseAmount,
        long estimatedOvertimePremiumAmount,
        long estimatedNightPremiumAmount,
        long estimatedTotalAmount,
        Long actualDepositAmount,
        LocalDate actualDepositRecordedDate,
        Integer actualDepositRecordedDay,
        boolean deductionsKnown,
        int paydayDay,
        Long differenceAmount,
        long anomalyTriggerAmount,
        boolean anomalyDetected,
        String status,
        String disclaimer,
        int modifiedRecordCount,
        int reflectedRecordCount,
        int pendingRecordCount,
        List<Long> relatedWorkProofIds
) {
}
