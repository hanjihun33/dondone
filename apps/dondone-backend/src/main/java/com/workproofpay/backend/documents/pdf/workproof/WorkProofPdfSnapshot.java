package com.workproofpay.backend.documents.pdf.workproof;

import java.util.List;

public record WorkProofPdfSnapshot(
        DocumentMeta meta,
        StatementInfo statement,
        WorkerInfo worker,
        WorkplaceInfo workplace,
        PeriodInfo period,
        SummaryInfo summary,
        List<WorkProofRecordItem> records,
        List<WorkProofAuditItem> audits
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
            String subtitle
    ) {
    }

    public record WorkerInfo(
            String name,
            String email,
            String phoneNumber
    ) {
    }

    public record WorkplaceInfo(
            String name,
            String address
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
            int totalWorkDayCount,
            int editedCount,
            int issueCount,
            long totalWorkedMinutes,
            String totalWorkedHoursLabel,
            String totalWorkDayCountLabel,
            String issueCountLabel
    ) {
    }

    public record WorkProofRecordItem(
            Long recordId,
            String workDate,
            String clockInAt,
            String clockOutAt,
            long workedMinutes,
            String workedHoursLabel,
            String remarks
    ) {
    }

    public record WorkProofAuditItem(
            Long auditId,
            Long recordId,
            String workDate,
            String editedAt,
            String changeSummary,
            String editReason
    ) {
    }
}
