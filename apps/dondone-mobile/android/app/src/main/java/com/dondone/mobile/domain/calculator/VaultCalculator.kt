package com.dondone.mobile.domain.calculator

import com.dondone.mobile.domain.model.DemoState
import kotlin.math.floor
import kotlin.math.max

data class VaultSnapshot(
    val suggestedDeposit: Int,
    val monthlyInterest: Int,
    val dailyInterest: Int
)

object VaultCalculator {
    fun calculate(state: DemoState): VaultSnapshot {
        val suggestedDeposit = max(100_000, floor(state.wage.actualDeposit * 0.35).toInt())
        val userDeposit = max(0, if (state.vault.userDeposit > 0) state.vault.userDeposit else suggestedDeposit)
        val poolTotal = max(1, state.vault.totalPool)
        val defiMonthly = floor(userDeposit * (state.vault.apr / 12)).toInt()
        val share = userDeposit / poolTotal.toDouble()
        val feeMonthly = floor(state.vault.monthlyFeeRevenue * share).toInt()
        val monthlyInterest = defiMonthly + feeMonthly
        return VaultSnapshot(
            suggestedDeposit = suggestedDeposit,
            monthlyInterest = monthlyInterest,
            dailyInterest = monthlyInterest / 30
        )
    }
}

