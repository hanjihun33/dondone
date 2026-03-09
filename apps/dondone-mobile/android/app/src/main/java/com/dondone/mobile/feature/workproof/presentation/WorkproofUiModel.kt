package com.dondone.mobile.feature.workproof.presentation

import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.domain.calculator.WorkproofCalculator
import com.dondone.mobile.domain.model.DemoState

enum class CalendarDayTone {
    DEFAULT,
    COMPLETE,
    MODIFIED,
    TODAY
}

data class WorkproofSummaryUiModel(
    val verifiedDaysText: String,
    val auditCountText: String,
    val todayClockText: String,
    val canClockIn: Boolean,
    val canClockOut: Boolean
)

data class WorkproofCalendarDayUiModel(
    val day: Int,
    val tone: CalendarDayTone
)

data class WorkproofRecordUiModel(
    val dateText: String,
    val timeText: String,
    val modified: Boolean,
    val modifiedHintText: String?
)

data class WorkproofAuditUiModel(
    val timeRangeText: String,
    val reason: String,
    val metaText: String
)

data class WorkproofUiModel(
    val monthText: String,
    val summary: WorkproofSummaryUiModel,
    val calendarDays: List<WorkproofCalendarDayUiModel>,
    val records: List<WorkproofRecordUiModel>,
    val audits: List<WorkproofAuditUiModel>
)

fun DemoState.toWorkproofUiModel(): WorkproofUiModel {
    val records = WorkproofCalculator.visibleRecords(this)
    val recordedDays = records.associateBy { it.day }

    return WorkproofUiModel(
        monthText = "${demo.year}.${demo.month.toString().padStart(2, '0')}",
        summary = WorkproofSummaryUiModel(
            verifiedDaysText = "${records.size}일",
            auditCountText = "${workproof.audit.size}건",
            todayClockText = "기록 시간: 출근 ${workproof.today.clockIn ?: "-"} · 퇴근 ${workproof.today.clockOut ?: "-"}",
            canClockIn = workproof.today.clockIn == null,
            canClockOut = workproof.today.clockIn != null && workproof.today.clockOut == null
        ),
        calendarDays = (1..demo.monthLength).map { day ->
            val record = recordedDays[day]
            val tone = when {
                day == demo.asOfDay -> CalendarDayTone.TODAY
                record?.modified == true -> CalendarDayTone.MODIFIED
                record != null && record.outTime != "-" -> CalendarDayTone.COMPLETE
                else -> CalendarDayTone.DEFAULT
            }
            WorkproofCalendarDayUiModel(day = day, tone = tone)
        },
        records = records.map { record ->
            WorkproofRecordUiModel(
                dateText = "${demo.year}-${demo.month.toString().padStart(2, '0')}-${record.day.toString().padStart(2, '0')}",
                timeText = "출근 ${record.inTime} · 퇴근 ${record.outTime}",
                modified = record.modified,
                modifiedHintText = if (record.modified) "사유 기록과 첨부가 함께 남아 있어요." else null
            )
        },
        audits = workproof.audit.map { audit ->
            WorkproofAuditUiModel(
                timeRangeText = "${audit.before} -> ${audit.after}",
                reason = audit.reason,
                metaText = "첨부 ${audit.attachments}개 · ${audit.at}"
            )
        }
    )
}

fun WorkproofRecordUiModel.statusTone(): BadgeTone {
    return if (modified) BadgeTone.Warning else BadgeTone.Success
}
