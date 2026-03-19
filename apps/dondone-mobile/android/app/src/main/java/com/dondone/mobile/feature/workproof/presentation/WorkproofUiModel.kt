package com.dondone.mobile.feature.workproof.presentation

import com.dondone.mobile.app.session.WorkproofActionUiState
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
    val verifiedDays: Int,
    val auditCount: Int,
    val todayInTime: String?,
    val todayOutTime: String?,
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
    val timeText: String,
    val tone: WorkproofRecordTone,
    val attachmentCount: Int,
    val modifiedReason: String?
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
    val audits: List<WorkproofAuditUiModel>
)

fun DemoState.toWorkproofUiModel(
    actionUiState: WorkproofActionUiState? = null
): WorkproofUiModel {
    val visibleRecords = WorkproofCalculator.visibleRecords(this)
    val verifiedSnapshot = WorkproofCalculator.verify(this)
    val workplaceRadiusMeters = workproof.allowedRadiusMeters
    val canSubmitAction = actionUiState?.isSubmitting != true
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
            dateText = formatDateText(demo.year, demo.month, record.day),
            timeText = "${record.inTime} - ${record.outTime}",
            tone = record.toRecordTone(),
            attachmentCount = record.attachments,
            modifiedReason = audit?.reason
        )
    }

    val auditItems = workproof.audit.map { audit ->
        WorkproofAuditUiModel(
            dateText = audit.at,
            changeText = "${audit.before} -> ${audit.after}",
            attachmentCount = audit.attachments,
            reasonText = audit.reason
        )
    }
    val todayDateText = formatDateText(demo.year, demo.month, demo.asOfDay)

    return WorkproofUiModel(
        calendarBaseYear = demo.year,
        calendarBaseMonth = demo.month,
        calendarCurrentDay = demo.asOfDay,
        calendarDayTones = dayTones,
        summary = WorkproofSummaryUiModel(
            canClockIn = workproof.today.clockIn == null && canSubmitAction,
            canClockOut = workproof.today.clockIn != null && workproof.today.clockOut == null && canSubmitAction,
            verifiedDays = verifiedSnapshot.verifiedDays,
            auditCount = workproof.audit.size,
            todayInTime = workproof.today.clockIn?.let { "$todayDateText · $it" },
            todayOutTime = workproof.today.clockOut?.let { "$todayDateText · $it" },
            workplaceLatitude = workproof.workplaceLatitude,
            workplaceLongitude = workproof.workplaceLongitude,
            currentLatitude = workproof.currentLatitude,
            currentLongitude = workproof.currentLongitude,
            workplaceRadiusMeters = workplaceRadiusMeters,
            isWithinWorkplaceRadius = isWithinWorkplaceRadius
        ),
        recentRecords = recentRecords,
        audits = auditItems
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

private fun String.isRecordedTime(): Boolean = this != UnrecordedTime

private fun formatDateText(
    year: Int,
    month: Int,
    day: Int
): String = "$year.${formatTwoDigits(month)}.${formatTwoDigits(day)}"

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
