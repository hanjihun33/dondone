package com.dondone.mobile.domain.calculator

import com.dondone.mobile.domain.model.DemoState

data class WageEstimate(
    val base: Int,
    val overtimePremium: Int,
    val nightPremium: Int,
    val total: Int,
    val difference: Int
)

object WageEstimator {
    fun calculate(state: DemoState): WageEstimate {
        val wage = state.wage
        val base = wage.hourly * wage.totalHours
        val overtimePremium = (wage.hourly * wage.overtimeHours * 0.5).toInt()
        val nightPremium = (wage.hourly * wage.nightHours * 0.5).toInt()
        val total = base + overtimePremium + nightPremium
        return WageEstimate(
            base = base,
            overtimePremium = overtimePremium,
            nightPremium = nightPremium,
            total = total,
            difference = total - wage.actualDeposit
        )
    }
}

