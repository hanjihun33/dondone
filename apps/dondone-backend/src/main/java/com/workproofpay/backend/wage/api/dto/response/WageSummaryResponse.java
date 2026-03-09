package com.workproofpay.backend.wage.api.dto.response;

import com.workproofpay.backend.wage.service.WageSummaryCalculator;
import com.workproofpay.backend.workproof.service.WorkProofMonthlyMetrics;

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
    public static WageSummaryResponse from(WorkProofMonthlyMetrics metrics,
                                           WageSummaryCalculator.WageSummarySnapshot snapshot,
                                           int paydayDay,
                                           String disclaimer) {
        return new WageSummaryResponse(
                metrics.yearMonth(),
                metrics.asOf(),
                metrics.totalWorkDays(),
                metrics.totalWorkedMinutes(),
                snapshot.totalWorkedHours(),
                metrics.totalOvertimeMinutes(),
                snapshot.overtimeHours(),
                metrics.totalNightMinutes(),
                snapshot.nightHours(),
                snapshot.normalizedHourlyWage(),
                snapshot.estimatedBaseAmount(),
                snapshot.estimatedOvertimePremiumAmount(),
                snapshot.estimatedNightPremiumAmount(),
                snapshot.estimatedTotalAmount(),
                snapshot.actualDepositAmount(),
                snapshot.actualDepositRecordedDate(),
                snapshot.actualDepositRecordedDay(),
                snapshot.deductionsKnown(),
                paydayDay,
                snapshot.differenceAmount(),
                snapshot.anomalyTriggerAmount(),
                snapshot.anomalyDetected(),
                snapshot.status(),
                disclaimer,
                metrics.editedRecordCount(),
                metrics.reflectedRecordCount(),
                metrics.pendingRecordCount(),
                metrics.reflectedWorkProofIds()
        );
    }
}
