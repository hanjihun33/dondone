package com.workproofpay.backend.wage.api.dto.response;

import com.workproofpay.backend.wage.service.WageSummaryCalculator;
import com.workproofpay.backend.workproof.service.WorkProofMonthlyMetrics;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record WageSummaryResponse(
        @Schema(description = "Difference reasons derived from estimate and recorded actual deposit")
        List<WageDifferenceReasonResponse> reasons,
        @Schema(description = "Target wage month in YYYY-MM format", example = "2026-03")
        String yearMonth,
        @Schema(description = "Summary reference date", example = "2026-03-25")
        LocalDate asOf,
        @Schema(description = "Total work days included in the month summary", example = "22")
        int workDays,
        @Schema(description = "Total reflected worked minutes", example = "10560")
        long totalWorkedMinutes,
        @Schema(description = "Total reflected worked hours", example = "176.0")
        BigDecimal totalWorkedHours,
        @Schema(description = "Total overtime minutes", example = "600")
        long overtimeMinutes,
        @Schema(description = "Total overtime hours", example = "10.0")
        BigDecimal overtimeHours,
        @Schema(description = "Total night minutes", example = "240")
        long nightMinutes,
        @Schema(description = "Total night hours", example = "4.0")
        BigDecimal nightHours,
        @Schema(description = "Hourly wage normalized by the client for reference estimate", example = "12000")
        long normalizedHourlyWage,
        @Schema(description = "Reference-only estimated base amount in KRW", example = "2112000")
        long estimatedBaseAmount,
        @Schema(description = "Reference-only estimated overtime premium amount in KRW", example = "60000")
        long estimatedOvertimePremiumAmount,
        @Schema(description = "Reference-only estimated night premium amount in KRW", example = "24000")
        long estimatedNightPremiumAmount,
        @Schema(description = "Reference-only estimated total amount in KRW", example = "2196000")
        long estimatedTotalAmount,
        @Schema(description = "Latest worker-recorded actual deposit amount in KRW", example = "1740000", nullable = true)
        Long actualDepositAmount,
        @Schema(description = "Date when the actual deposit was recorded", example = "2026-03-25", nullable = true)
        LocalDate actualDepositRecordedDate,
        @Schema(description = "Day-of-month when the actual deposit was recorded", example = "25", nullable = true)
        Integer actualDepositRecordedDay,
        @Schema(description = "Whether deductions were known when the actual deposit was recorded", example = "false")
        boolean deductionsKnown,
        @Schema(description = "Configured payday day-of-month for this summary", example = "25")
        int paydayDay,
        @Schema(description = "Difference between reference-only estimate and latest actual deposit in KRW", example = "456000", nullable = true)
        Long differenceAmount,
        @Schema(description = "Minimum amount threshold that triggers anomaly preview", example = "50000")
        long anomalyTriggerAmount,
        @Schema(description = "Whether the current difference crosses anomaly preview threshold", example = "true")
        boolean anomalyDetected,
        @Schema(description = "Summary status derived from current estimate and recorded actual deposit", example = "ACTION_REQUIRED")
        String status,
        @Schema(description = "Reference-only disclaimer shown to clients")
        String disclaimer,
        @Schema(description = "Edited WorkProof record count included in the month summary", example = "2")
        int modifiedRecordCount,
        @Schema(description = "Reflected WorkProof record count included in the estimate", example = "20")
        int reflectedRecordCount,
        @Schema(description = "Pending WorkProof record count not yet reflected in the estimate", example = "1")
        int pendingRecordCount,
        @Schema(description = "Reflected WorkProof IDs linked to this summary")
        List<Long> relatedWorkProofIds
) {
    public record WageDifferenceReasonResponse(
            @Schema(description = "Difference reason code", example = "OVERTIME_NOT_REFLECTED")
            String code,
            @Schema(description = "Difference reason title", example = "Overtime may be missing")
            String title,
            @Schema(description = "Human-readable description for the reason")
            String description,
            @Schema(description = "Related WorkProof IDs for this reason")
            List<Long> relatedWorkProofIds
    ) {
        public static WageDifferenceReasonResponse from(WageSummaryCalculator.WageDifferenceReason reason) {
            return new WageDifferenceReasonResponse(
                    reason.code(),
                    reason.title(),
                    reason.description(),
                    reason.relatedWorkProofIds()
            );
        }
    }

    public static WageSummaryResponse from(WorkProofMonthlyMetrics metrics,
                                           WageSummaryCalculator.WageSummarySnapshot snapshot,
                                           int paydayDay,
                                           String disclaimer) {
        return new WageSummaryResponse(
                snapshot.reasons().stream()
                        .map(WageDifferenceReasonResponse::from)
                        .toList(),
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
