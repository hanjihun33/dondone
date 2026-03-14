package com.dondone.mobile.feature.workproof.presentation

import com.dondone.mobile.domain.calculator.WorkproofCalculator
import com.dondone.mobile.domain.model.DemoState

enum class WorkproofCalendarTone {
    MISSING,
    PARTIAL,
    COMPLETE,
    MODIFIED
}

enum class WorkproofRecordTone {
    DEFAULT,
    ACTIVE,
    MODIFIED
}

data class WorkproofSummaryUiModel(
    val canClockIn: Boolean,
    val canClockOut: Boolean,
    val verifiedDaysText: String,
    val auditCountText: String,
    val todayMetaText: String,
    val todayInText: String,
    val todayOutText: String
)

data class WorkproofCalendarCellUiModel(
    val dayLabel: String,
    val tone: WorkproofCalendarTone?,
    val isCurrent: Boolean
)

data class WorkproofRecordUiModel(
    val id: String,
    val dateText: String,
    val timeText: String,
    val statusText: String,
    val tone: WorkproofRecordTone,
    val attachmentText: String,
    val modifiedHintText: String?
)

data class WorkproofAuditUiModel(
    val dateText: String,
    val changeText: String,
    val attachmentText: String,
    val reasonText: String
)

data class WorkproofUiModel(
    val calendarBaseYear: Int,
    val calendarBaseMonth: Int,
    val calendarCurrentDay: Int,
    val calendarDayTones: Map<Int, WorkproofCalendarTone>,
    val summary: WorkproofSummaryUiModel,
    val recentRecords: List<WorkproofRecordUiModel>,
    val auditPreview: WorkproofAuditUiModel?,
    val audits: List<WorkproofAuditUiModel>
)

fun DemoState.toWorkproofUiModel(): WorkproofUiModel {
    val visibleRecords = WorkproofCalculator.visibleRecords(this)
    val dayTones = (1..demo.monthLength).associateWith { day ->
        val record = visibleRecords.firstOrNull { it.day == day }
        when {
            record == null -> WorkproofCalendarTone.MISSING
            record.modified -> WorkproofCalendarTone.MODIFIED
            record.inTime != "-" && record.outTime != "-" -> WorkproofCalendarTone.COMPLETE
            record.inTime != "-" || record.outTime != "-" -> WorkproofCalendarTone.PARTIAL
            else -> WorkproofCalendarTone.MISSING
        }
    }

    val recentRecords = visibleRecords.take(4).map { record ->
        val audit = workproof.audit.firstOrNull { it.id == record.id }
        val tone = when {
            record.modified -> WorkproofRecordTone.MODIFIED
            record.inTime == "-" || record.outTime == "-" -> WorkproofRecordTone.ACTIVE
            else -> WorkproofRecordTone.DEFAULT
        }
        val statusText = when {
            record.modified -> "\uC218\uC815"
            record.inTime == "-" || record.outTime == "-" -> "\uCD9C\uADFC\uB9CC"
            else -> "\uAE30\uB85D"
        }

        WorkproofRecordUiModel(
            id = record.id,
            dateText = formatDateText(demo.year, demo.month, record.day),
            timeText = "${record.inTime} - ${record.outTime}",
            statusText = statusText,
            tone = tone,
            attachmentText = if (record.attachments > 0) {
                "\uCCA8\uBD80 ${record.attachments}\uAC1C"
            } else {
                "\uCCA8\uBD80 \uC5C6\uC74C"
            },
            modifiedHintText = audit?.let { "\uC218\uC815 \uC0AC\uC720: ${it.reason}" }
        )
    }

    val auditItems = workproof.audit.map { audit ->
        WorkproofAuditUiModel(
            dateText = audit.at,
            changeText = "${audit.before} -> ${audit.after}",
            attachmentText = "\uCCA8\uBD80 ${audit.attachments}\uAC1C",
            reasonText = audit.reason
        )
    }

    return WorkproofUiModel(
        calendarBaseYear = demo.year,
        calendarBaseMonth = demo.month,
        calendarCurrentDay = demo.asOfDay,
        calendarDayTones = dayTones,
        summary = WorkproofSummaryUiModel(
            canClockIn = workproof.today.clockIn == null,
            canClockOut = workproof.today.clockIn != null && workproof.today.clockOut == null,
            verifiedDaysText = "${visibleRecords.count { it.inTime != "-" && it.outTime != "-" }}\uC77C",
            auditCountText = "${workproof.audit.size}\uAC74",
            todayMetaText = "${demo.year}.${formatTwoDigits(demo.month)}.${formatTwoDigits(demo.asOfDay)} / ${workproof.workplaceName}",
            todayInText = workproof.today.clockIn ?: "-",
            todayOutText = workproof.today.clockOut ?: "-"
        ),
        recentRecords = recentRecords,
        auditPreview = auditItems.firstOrNull(),
        audits = auditItems
    )
}

private fun formatDateText(
    year: Int,
    month: Int,
    day: Int
): String = "$year.${formatTwoDigits(month)}.${formatTwoDigits(day)}"

private fun formatTwoDigits(value: Int): String = value.toString().padStart(2, '0')
