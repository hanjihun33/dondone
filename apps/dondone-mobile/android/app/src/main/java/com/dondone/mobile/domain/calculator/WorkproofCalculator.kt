package com.dondone.mobile.domain.calculator

import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.WorkRecord
import kotlin.math.floor
import kotlin.math.max

data class VerifiedWorkSnapshot(
    val verifiedDays: Int,
    val verifiedHours: Double,
    val verifiedAmount: Int
)

object WorkproofCalculator {
    fun visibleRecords(state: DemoState): List<WorkRecord> =
        state.workproof.records.filter { it.day <= state.demo.asOfDay }.sortedByDescending { it.day }

    fun verify(state: DemoState): VerifiedWorkSnapshot {
        val validRecords = visibleRecords(state).filter { it.inTime != "-" && it.outTime != "-" }
        var totalMinutes = 0
        validRecords.forEach { record ->
            val inMinutes = parseHourToMinutes(record.inTime)
            val outMinutes = parseHourToMinutes(record.outTime)
            if (inMinutes == null || outMinutes == null || outMinutes <= inMinutes) return@forEach
            totalMinutes += outMinutes - inMinutes
        }

        val recordHours = (totalMinutes / 60.0 * 10.0).toInt() / 10.0
        val verifiedDays = max(validRecords.size, state.wage.workDays)
        val verifiedHours = max(recordHours.toInt(), state.wage.totalHours).toDouble()
        val verifiedAmount = floor((totalMinutes / 60.0) * state.wage.hourly).toInt()
        val summaryAmount = state.wage.totalHours * state.wage.hourly

        return VerifiedWorkSnapshot(
            verifiedDays = verifiedDays,
            verifiedHours = verifiedHours,
            verifiedAmount = max(verifiedAmount, summaryAmount)
        )
    }

    private fun parseHourToMinutes(timeText: String): Int? {
        val parts = timeText.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return hour * 60 + minute
    }
}

