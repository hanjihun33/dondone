package com.dondone.mobile.feature.workproof.presentation

import com.dondone.mobile.app.session.WorkproofActionUiState
import com.dondone.mobile.app.session.WorkproofCurrentLocationStatus
import com.dondone.mobile.app.session.WorkproofCurrentLocationUiState
import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.i18n.AppLanguage
import com.dondone.mobile.core.i18n.AppTextKeys
import com.dondone.mobile.core.i18n.text
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
    REVIEW,
    MODIFIED
}

enum class WorkproofRecordTone {
    DEFAULT,
    ACTIVE,
    REVIEW,
    MODIFIED
}

data class WorkproofSummaryUiModel(
    val canClockIn: Boolean,
    val canClockOut: Boolean,
    val dateText: String,
    val statusText: String,
    val statusTone: BadgeTone,
    val clockInText: String,
    val clockOutText: String,
    val workplaceLatitude: Double,
    val workplaceLongitude: Double,
    val currentLatitude: Double,
    val currentLongitude: Double,
    val workplaceRadiusMeters: Int,
    val isWithinWorkplaceRadius: Boolean,
    val isCurrentLocationLoading: Boolean,
    val currentLocationStatus: WorkproofCurrentLocationStatus?,
    val isUsingFallbackCoordinates: Boolean
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
    language: AppLanguage = AppLanguage.KOREAN,
    actionUiState: WorkproofActionUiState? = null,
    remoteState: WorkproofRemoteState = WorkproofRemoteState.unauthenticated(""),
    isAuthenticated: Boolean = false,
    currentLocationUiState: WorkproofCurrentLocationUiState = WorkproofCurrentLocationUiState()
): WorkproofUiModel {
    val visibleRecords = WorkproofCalculator.visibleRecords(this)
    val workplaceRadiusMeters = workproof.allowedRadiusMeters
    val currentDateText = "${demo.year}-${formatTwoDigits(demo.month)}-${formatTwoDigits(demo.asOfDay)}"
    val clockInText = workproof.today.clockIn?.let { "$currentDateText · $it" } ?: UnrecordedTime
    val clockOutText = workproof.today.clockOut?.let { "$currentDateText · $it" } ?: UnrecordedTime

    val workStatus = when {
        workproof.today.clockOut != null -> WorkproofDayStatus.COMPLETE
        workproof.today.clockIn != null -> WorkproofDayStatus.PARTIAL
        else -> WorkproofDayStatus.READY
    }
    val canSubmitAction = actionUiState?.isSubmitting != true
    val usesFallbackData = remoteState.mode != WorkproofRemoteMode.CONTENT
    val allowRemoteActions = !isAuthenticated || remoteState.mode == WorkproofRemoteMode.CONTENT
    val requiresLiveCurrentLocation = isAuthenticated && remoteState.mode == WorkproofRemoteMode.CONTENT
    val hasUsableCurrentLocation = !requiresLiveCurrentLocation || currentLocationUiState.hasUsableLocation
    val isWithinWorkplaceRadius = hasUsableCurrentLocation && isWithinWorkplaceRadius(
        startLatitude = workproof.currentLatitude,
        startLongitude = workproof.currentLongitude,
        endLatitude = workproof.workplaceLatitude,
        endLongitude = workproof.workplaceLongitude,
        radiusMeters = workplaceRadiusMeters
    )
    val currentLocationStatus = currentLocationUiState.status
        .takeIf { isAuthenticated && it != WorkproofCurrentLocationStatus.READY && it != WorkproofCurrentLocationStatus.IDLE }
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
            statusText = record.toStatusText(language),
            attachmentCount = record.attachments,
            detailText = audit?.let { language.text("workproof_modified_reason_value", it.reason) } ?: record.toDetailText(language)
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
                    reasonText = record.toAuditReasonText(language)
                )
            }
    }

    return WorkproofUiModel(
        calendarBaseYear = demo.year,
        calendarBaseMonth = demo.month,
        calendarCurrentDay = demo.asOfDay,
        calendarDayTones = dayTones,
        summary = WorkproofSummaryUiModel(
            canClockIn = allowRemoteActions && workproof.today.clockIn == null && canSubmitAction && hasUsableCurrentLocation,
            canClockOut = allowRemoteActions &&
                workproof.today.clockIn != null &&
                workproof.today.clockOut == null &&
                canSubmitAction &&
                hasUsableCurrentLocation,
            dateText = currentDateText,
            statusText = workStatus.toText(language),
            statusTone = workStatus.toTone(),
            clockInText = clockInText,
            clockOutText = clockOutText,
            workplaceLatitude = workproof.workplaceLatitude,
            workplaceLongitude = workproof.workplaceLongitude,
            currentLatitude = workproof.currentLatitude,
            currentLongitude = workproof.currentLongitude,
            workplaceRadiusMeters = workplaceRadiusMeters,
            isWithinWorkplaceRadius = isWithinWorkplaceRadius,
            isCurrentLocationLoading = currentLocationUiState.status == WorkproofCurrentLocationStatus.LOADING,
            currentLocationStatus = currentLocationStatus,
            isUsingFallbackCoordinates = isAuthenticated && usesFallbackData
        ),
        recentRecords = recentRecords,
        audits = auditItems,
        fallbackNoticeTitle = if (usesFallbackData) language.text("workproof_demo_sample_data") else null,
        fallbackNoticeMessage = if (usesFallbackData) {
            language.text("workproof_demo_fallback_message")
        } else {
            null
        }
    )
}

private enum class WorkproofDayStatus {
    COMPLETE,
    PARTIAL,
    READY
}

private fun WorkproofDayStatus.toText(language: AppLanguage): String = when (this) {
    WorkproofDayStatus.COMPLETE -> language.text("completed")
    WorkproofDayStatus.PARTIAL -> language.text(AppTextKeys.HOME_CLOCK_IN_RECORDED)
    WorkproofDayStatus.READY -> language.text(AppTextKeys.HOME_READY)
}

private fun WorkproofDayStatus.toTone(): BadgeTone = when (this) {
    WorkproofDayStatus.COMPLETE -> BadgeTone.Success
    WorkproofDayStatus.PARTIAL -> BadgeTone.Warning
    WorkproofDayStatus.READY -> BadgeTone.Info
}

internal fun WorkRecord.toCalendarTone(): WorkproofCalendarTone {
    return when {
        reflectionStatus == "NEEDS_REVIEW" -> WorkproofCalendarTone.REVIEW
        modified -> WorkproofCalendarTone.MODIFIED
        inTime.isRecordedTime() && outTime.isRecordedTime() -> WorkproofCalendarTone.COMPLETE
        inTime.isRecordedTime() || outTime.isRecordedTime() -> WorkproofCalendarTone.PARTIAL
        else -> WorkproofCalendarTone.MISSING
    }
}

internal fun WorkRecord.toRecordTone(): WorkproofRecordTone {
    return when {
        reflectionStatus == "NEEDS_REVIEW" -> WorkproofRecordTone.REVIEW
        modified -> WorkproofRecordTone.MODIFIED
        !inTime.isRecordedTime() || !outTime.isRecordedTime() -> WorkproofRecordTone.ACTIVE
        else -> WorkproofRecordTone.DEFAULT
    }
}

internal fun WorkRecord.toStatusText(language: AppLanguage): String {
    return when (reflectionStatus) {
        "NEEDS_REVIEW" -> language.text("workproof_reviewing")
        "REFLECTED" -> language.text("workproof_reflected")
        "EXCLUDED" -> language.text("workproof_rejected")
        else -> if (!inTime.isRecordedTime() || !outTime.isRecordedTime()) {
            language.text("workproof_partial_recorded")
        } else {
            language.text("workproof_recorded")
        }
    }
}

internal fun WorkRecord.toDetailText(language: AppLanguage): String? {
    return when (reflectionStatus) {
        "NEEDS_REVIEW" -> null
        "REFLECTED" -> {
            val recognizedRange = buildRecognizedTimeRange() ?: return null
            if (recognizedRange == "$inTime - $outTime") {
                null
            } else {
                language.text("workproof_recognized_time_value", recognizedRange)
            }
        }

        "EXCLUDED" -> {
            val rejectMemo = decisionMemo?.takeIf { it.isNotBlank() }
            rejectMemo?.let { language.text("workproof_rejection_reason_value", it) }
        }

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

internal fun WorkRecord.toAuditReasonText(language: AppLanguage): String {
    return when (reflectionStatus) {
        "NEEDS_REVIEW" -> language.text("workproof_waiting_for_workplace_review")
        "REFLECTED" -> language.text("workproof_reflected_in_recognized_time")
        "EXCLUDED" -> language.text("workproof_rejected_by_workplace")
        else -> language.text("workproof_edit_request_submitted")
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
