package com.dondone.mobile.feature.workproof.presentation

import com.dondone.mobile.domain.calculator.WorkproofCalculator
import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.WorkRecord

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
    val distanceToWorkplaceMeters: Int,
    val isWithinWorkplaceRadius: Boolean,
    val locationStatusText: String,
    val locationStatusDetailText: String
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
    val auditPreview: WorkproofAuditUiModel?,
    val audits: List<WorkproofAuditUiModel>
)

fun DemoState.toWorkproofUiModel(): WorkproofUiModel {
    val visibleRecords = WorkproofCalculator.visibleRecords(this)
    val verifiedSnapshot = WorkproofCalculator.verify(this)
    val workplaceRadiusMeters = 100
    val distanceToWorkplaceMeters = calculateDistanceMeters(
        startLatitude = workproof.currentLatitude,
        startLongitude = workproof.currentLongitude,
        endLatitude = workproof.workplaceLatitude,
        endLongitude = workproof.workplaceLongitude
    )
    val isWithinWorkplaceRadius = distanceToWorkplaceMeters <= workplaceRadiusMeters
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
            canClockIn = workproof.today.clockIn == null,
            canClockOut = workproof.today.clockIn != null && workproof.today.clockOut == null,
            verifiedDays = verifiedSnapshot.verifiedDays,
            auditCount = workproof.audit.size,
            todayInTime = workproof.today.clockIn?.let { "$todayDateText · $it" },
            todayOutTime = workproof.today.clockOut?.let { "$todayDateText · $it" },
            workplaceLatitude = workproof.workplaceLatitude,
            workplaceLongitude = workproof.workplaceLongitude,
            currentLatitude = workproof.currentLatitude,
            currentLongitude = workproof.currentLongitude,
            workplaceRadiusMeters = workplaceRadiusMeters,
            distanceToWorkplaceMeters = distanceToWorkplaceMeters,
            isWithinWorkplaceRadius = isWithinWorkplaceRadius,
            locationStatusText = if (isWithinWorkplaceRadius) {
                "근무지 반경 안에 있어요"
            } else {
                "근무지 반경 밖이에요"
            },
            locationStatusDetailText = if (isWithinWorkplaceRadius) {
                "현재 위치가 근무지 기준 ${workplaceRadiusMeters}m 안에 있어 출퇴근 버튼을 사용할 수 있어요."
            } else {
                "현재 위치가 근무지에서 ${distanceToWorkplaceMeters}m 떨어져 있어요. 반경 ${workplaceRadiusMeters}m 안에 들어오면 출퇴근 버튼이 활성화돼요."
            }
        ),
        recentRecords = recentRecords,
        auditPreview = auditItems.firstOrNull(),
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

private fun calculateDistanceMeters(
    startLatitude: Double,
    startLongitude: Double,
    endLatitude: Double,
    endLongitude: Double
): Int {
    val earthRadiusMeters = 6_371_000.0
    val startLatitudeRadians = Math.toRadians(startLatitude)
    val endLatitudeRadians = Math.toRadians(endLatitude)
    val latitudeDeltaRadians = Math.toRadians(endLatitude - startLatitude)
    val longitudeDeltaRadians = Math.toRadians(endLongitude - startLongitude)

    val haversine = kotlin.math.sin(latitudeDeltaRadians / 2).let { it * it } +
        kotlin.math.cos(startLatitudeRadians) *
        kotlin.math.cos(endLatitudeRadians) *
        kotlin.math.sin(longitudeDeltaRadians / 2).let { it * it }

    val arc = 2 * kotlin.math.atan2(kotlin.math.sqrt(haversine), kotlin.math.sqrt(1 - haversine))
    return (earthRadiusMeters * arc).toInt()
}
