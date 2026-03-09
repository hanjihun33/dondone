package com.dondone.mobile.domain.calculator

import com.dondone.mobile.domain.model.DemoState
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

data class AdvanceSnapshot(
    val verifiedDays: Int,
    val tierName: String,
    val totalLimit: Int,
    val available: Int,
    val used: Int,
    val fee: Int,
    val requestAmount: Int,
    val receiveAmount: Int,
    val progressTargetDays: Int,
    val nextTierInDays: Int,
    val nextTierGain: Int
)

object AdvanceCalculator {
    fun calculate(state: DemoState): AdvanceSnapshot {
        val verified = WorkproofCalculator.verify(state)
        val tier = tierForDays(verified.verifiedDays)
        val rawLimit = floor(verified.verifiedAmount * state.advance.limitRate).toInt()
        val baseLimit = min(rawLimit, min(tier.cap, state.advance.maxCap))
        val bonus = if (state.advance.previousRepaymentGood) state.advance.bonusLimit else 0
        val totalLimit = min(state.advance.maxCap, baseLimit + bonus)
        val used = min(totalLimit, max(0, state.advance.used))
        val available = max(0, totalLimit - used)
        val requestAmount = min(state.advance.selectedRequest, available)
        val fee = if (requestAmount > 0) state.advance.flatFee else 0
        val nextTarget = nextTierTarget(verified.verifiedDays)

        return AdvanceSnapshot(
            verifiedDays = verified.verifiedDays,
            tierName = tier.name,
            totalLimit = totalLimit,
            available = available,
            used = used,
            fee = fee,
            requestAmount = requestAmount,
            receiveAmount = max(0, requestAmount - fee),
            progressTargetDays = nextTarget ?: verified.verifiedDays,
            nextTierInDays = if (nextTarget == null) 0 else max(0, nextTarget - verified.verifiedDays),
            nextTierGain = if (nextTarget == null) 0 else 100_000
        )
    }

    data class Tier(val name: String, val cap: Int)

    private fun tierForDays(days: Int): Tier = when {
        days >= 20 -> Tier("T3", 500_000)
        days >= 15 -> Tier("T2", 400_000)
        days >= 10 -> Tier("T1", 300_000)
        else -> Tier("T0", 200_000)
    }

    private fun nextTierTarget(days: Int): Int? = when {
        days < 10 -> 10
        days < 15 -> 15
        days < 20 -> 20
        else -> null
    }
}

