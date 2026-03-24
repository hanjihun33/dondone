package com.dondone.mobile.feature.workproof.presentation

import com.dondone.mobile.app.session.WorkproofActionUiState
import com.dondone.mobile.data.workproof.WorkproofRemoteMode
import com.dondone.mobile.data.workproof.WorkproofRemoteState
import com.dondone.mobile.domain.calculator.WorkproofCalculator
import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.WorkRecord
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
private const val UnrecordedTime = "-"

enum class WorkproofCalendarTone {
    UNAVAILABLE,
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
    val workplaceLatitude: Double,
    val workplaceLongitude: Double,
    val currentLatitude: Double,
    val currentLongitude: Double,
    val workplaceRadiusMeters: Int,
    val isWithinWorkplaceRadius: Boolean
)

data class WorkproofCalendarCellUiModel(
    val dayLabel: String,
    val tone: WorkproofCalendarTone?,
    val isCurrent: Boolean
)

data class WorkproofRecordUiModel(
    val id: String,
    val dateText: String,
    val clockInText: String,
    val clockOutText: String,
    val timeText: String,
    val tone: WorkproofRecordTone,
    val statusText: String,
    val attachmentCount: Int,
    val detailText: String?
)

data class WorkproofAuditUiModel(
    val dateText: String,
    val changeText: String,
    val attachmentCount: Int,
    val reasonText: String
)

data class WorkproofUiModel(
    val calendarBaseYear: Int,
    val calendarBaseMonth: Int,
    val calendarCurrentDay: Int,
    val calendarDayTones: Map<Int, WorkproofCalendarTone>,
    val summary: WorkproofSummaryUiModel,
    val recentRecords: List<WorkproofRecordUiModel>,
    val audits: List<WorkproofAuditUiModel>,
    val fallbackNoticeTitle: String? = null,
    val fallbackNoticeMessage: String? = null
)

fun DemoState.toWorkproofUiModel(
    actionUiState: WorkproofActionUiState? = null,
    remoteState: WorkproofRemoteState = WorkproofRemoteState.unauthenticated(""),
    isAuthenticated: Boolean = false
): WorkproofUiModel {
    val visibleRecords = WorkproofCalculator.visibleRecords(this)
    val workplaceRadiusMeters = workproof.allowedRadiusMeters
    val canSubmitAction = actionUiState?.isSubmitting != true
    val usesFallbackData = remoteState.mode != WorkproofRemoteMode.CONTENT
    val allowRemoteActions = !isAuthenticated || remoteState.mode == WorkproofRemoteMode.CONTENT
    val isWithinWorkplaceRadius = isWithinWorkplaceRadius(
        startLatitude = workproof.currentLatitude,
        startLongitude = workproof.currentLongitude,
        endLatitude = workproof.workplaceLatitude,
        endLongitude = workproof.workplaceLongitude,
        radiusMeters = workplaceRadiusMeters
    )
    val dayTones = (1..demo.monthLength).associateWith { day ->
        when {
            day > demo.asOfDay -> WorkproofCalendarTone.UNAVAILABLE
            else -> visibleRecords.firstOrNull { it.day == day }?.toCalendarTone() ?: WorkproofCalendarTone.MISSING
        }
    }

    val recentRecords = visibleRecords.take(4).map { record ->
        val audit = workproof.audit.firstOrNull { it.id == record.id }
        WorkproofRecordUiModel(
            id = record.id,
            dateText = formatDateText(record.workDate),
            clockInText = record.inTime,
            clockOutText = record.outTime,
            timeText = "${record.inTime} - ${record.outTime}",
            tone = record.toRecordTone(),
            statusText = record.toStatusText(),
            attachmentCount = record.attachments,
            detailText = audit?.let { "수정 사유: ${it.reason}" } ?: record.toDetailText()
        )
    }

    val auditItems = if (workproof.audit.isNotEmpty()) {
        workproof.audit.map { audit ->
            WorkproofAuditUiModel(
                dateText = audit.at,
                changeText = "${audit.before} -> ${audit.after}",
                attachmentCount = audit.attachments,
                reasonText = audit.reason
            )
        }
    } else {
        visibleRecords
            .filter { it.modified }
            .map { record ->
                WorkproofAuditUiModel(
                    dateText = formatDateText(record.workDate),
                    changeText = record.toAuditChangeText(),
                    attachmentCount = record.attachments,
                    reasonText = record.toAuditReasonText()
                )
            }
    }
    return WorkproofUiModel(
        calendarBaseYear = demo.year,
        calendarBaseMonth = demo.month,
        calendarCurrentDay = demo.asOfDay,
        calendarDayTones = dayTones,
        summary = WorkproofSummaryUiModel(
            canClockIn = allowRemoteActions && workproof.today.clockIn == null && canSubmitAction,
            canClockOut = allowRemoteActions &&
                workproof.today.clockIn != null &&
                workproof.today.clockOut == null &&
                canSubmitAction,
            workplaceLatitude = workproof.workplaceLatitude,
            workplaceLongitude = workproof.workplaceLongitude,
            currentLatitude = workproof.currentLatitude,
            currentLongitude = workproof.currentLongitude,
            workplaceRadiusMeters = workplaceRadiusMeters,
            isWithinWorkplaceRadius = isWithinWorkplaceRadius
        ),
        recentRecords = recentRecords,
        audits = auditItems,
        fallbackNoticeTitle = if (usesFallbackData) "가상 예시 데이터" else null,
        fallbackNoticeMessage = if (usesFallbackData) {
            "현재 보이는 근무 기록은 데모 데이터입니다. 회사 등록 후 실제 기록으로 전환됩니다."
        } else {
            null
        }
    )
}

internal fun WorkRecord.toCalendarTone(): WorkproofCalendarTone {
    return when {
        modified -> WorkproofCalendarTone.MODIFIED
        inTime.isRecordedTime() && outTime.isRecordedTime() -> WorkproofCalendarTone.COMPLETE
        inTime.isRecordedTime() || outTime.isRecordedTime() -> WorkproofCalendarTone.PARTIAL
        else -> WorkproofCalendarTone.MISSING
    }
}

internal fun WorkRecord.toRecordTone(): WorkproofRecordTone {
    return when {
        modified -> WorkproofRecordTone.MODIFIED
        !inTime.isRecordedTime() || !outTime.isRecordedTime() -> WorkproofRecordTone.ACTIVE
        else -> WorkproofRecordTone.DEFAULT
    }
}

internal fun WorkRecord.toStatusText(): String {
    return when (reflectionStatus) {
        "NEEDS_REVIEW" -> "검토 중"
        "REFLECTED" -> "반영됨"
        "EXCLUDED" -> "제외됨"
        else -> if (!inTime.isRecordedTime() || !outTime.isRecordedTime()) "출근만" else "기록"
    }
}

internal fun WorkRecord.toDetailText(): String? {
    return when (reflectionStatus) {
        "NEEDS_REVIEW" -> "검토 상태: 사업장 검토 중"
        "REFLECTED" -> {
            val recognizedRange = buildRecognizedTimeRange() ?: return "검토 상태: 반영 완료"
            if (recognizedRange == "$inTime - $outTime") {
                "검토 상태: 반영 완료"
            } else {
                "인정 시간: $recognizedRange"
            }
        }

        "EXCLUDED" -> "검토 상태: 제외됨"
        else -> null
    }
}

internal fun WorkRecord.toAuditChangeText(): String {
    val actualRange = "$inTime - $outTime"
    val recognizedRange = buildRecognizedTimeRange()
    return if (recognizedRange != null && recognizedRange != actualRange) {
        "$actualRange -> $recognizedRange"
    } else {
        actualRange
    }
}

internal fun WorkRecord.toAuditReasonText(): String {
    return when (reflectionStatus) {
        "NEEDS_REVIEW" -> "사업장 검토 대기 중"
        "REFLECTED" -> "인정 시간에 반영됨"
        "EXCLUDED" -> "정산 대상에서 제외됨"
        else -> "수정 요청이 접수됨"
    }
}

private fun String.isRecordedTime(): Boolean = this != UnrecordedTime

private fun WorkRecord.buildRecognizedTimeRange(): String? {
    val recognizedIn = recognizedInTime ?: return null
    val recognizedOut = recognizedOutTime ?: return null
    return "$recognizedIn - $recognizedOut"
}

private fun formatDateText(date: java.time.LocalDate): String =
    "${date.year}.${formatTwoDigits(date.monthValue)}.${formatTwoDigits(date.dayOfMonth)}"

private fun formatTwoDigits(value: Int): String = value.toString().padStart(2, '0')

private fun isWithinWorkplaceRadius(
    startLatitude: Double,
    startLongitude: Double,
    endLatitude: Double,
    endLongitude: Double,
    radiusMeters: Int
): Boolean {
    return haversineDistanceMeters(
        startLatitude,
        startLongitude,
        endLatitude,
        endLongitude
    ) <= radiusMeters
}

private fun haversineDistanceMeters(
    startLatitude: Double,
    startLongitude: Double,
    endLatitude: Double,
    endLongitude: Double
): Double {
    val earthRadiusMeters = 6_371_000.0
    val latitudeDelta = Math.toRadians(endLatitude - startLatitude)
    val longitudeDelta = Math.toRadians(endLongitude - startLongitude)
    val startLatitudeRadians = Math.toRadians(startLatitude)
    val endLatitudeRadians = Math.toRadians(endLatitude)

    val a = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
        cos(startLatitudeRadians) * cos(endLatitudeRadians) *
        sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusMeters * c
}
