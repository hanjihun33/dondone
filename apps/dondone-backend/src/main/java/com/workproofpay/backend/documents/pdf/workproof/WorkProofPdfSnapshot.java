package com.workproofpay.backend.documents.pdf.workproof;

import java.util.List;

public record WorkProofPdfSnapshot(
        DocumentMeta meta,
        StatementInfo statement,
        WorkerInfo worker,
        WorkplaceInfo workplace,
        ContractInfo contract,
        PeriodInfo period,
        SummaryInfo summary,
        List<WorkProofRecordItem> records,
        List<WorkProofAuditItem> audits,
        List<String> notices
) {
    public record DocumentMeta(
            String documentType,
            String documentNumber,
            String templateVersion,
            String generatedAt,
            String timezone,
            String locale
    ) {
    }

    public record StatementInfo(
            String title,
            String subtitle,
            String intendedUseNotice
    ) {
    }

    public record WorkerInfo(
            Long userId,
            String name,
            String email
    ) {
    }

    public record WorkplaceInfo(
            Long workplaceId,
            String name,
            String address,
            String mapLabel
    ) {
    }

    public record ContractInfo(
            String payUnit,
            String basePayAmount,
            String normalizedHourlyWage,
            Integer dailyWorkMinutes,
            Integer monthlyWorkMinutes,
            String effectiveFrom,
            String effectiveTo
    ) {
    }

    public record PeriodInfo(
            String startDate,
            String endDate,
            String yearMonth,
            String periodLabel
    ) {
    }

    public record SummaryInfo(
            int totalRecordCount,
            int totalWorkDayCount,
            int reflectedCount,
            int needsReviewCount,
            int editedCount,
            int totalAttachmentCount,
            long totalWorkedMinutes,
            String totalWorkedHoursLabel,
            String totalWorkDayCountLabel
    ) {
    }

    public record WorkProofRecordItem(
            Long recordId,
            String workDate,
            String clockInAt,
            String clockOutAt,
            long workedMinutes,
            String workedHoursLabel,
            String clockInLocationLabel,
            String clockOutLocationLabel,
            String financialStatus,
            String financialStatusLabel,
            String financialStatusTone,
            boolean edited,
            boolean outsideAllowedRadius,
            String outsideAllowedRadiusLabel,
            String locationSummaryLabel,
            String editReason,
            String memo,
            String memoOrReason,
            int attachmentCount,
            String createdAt,
            String updatedAt
    ) {
    }

    public record WorkProofAuditItem(
            Long auditId,
            Long recordId,
            String editedAt,
            String beforeClockInAt,
            String beforeClockOutAt,
            String afterClockInAt,
            String afterClockOutAt,
            String beforeMemo,
            String afterMemo,
            String beforeEditReason,
            String afterEditReason,
            String changeSummary
    ) {
    }
}
