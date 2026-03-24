package com.dondone.mobile.data.advance

import java.math.BigDecimal
import java.math.RoundingMode

data class AdvanceEligibilityPayload(
    val workplaceId: Long,
    val assetSymbol: String,
    val assetDecimals: Int,
    val exchangeRateSnapshot: BigDecimal,
    val availableAmountAtomic: Long,
    val availableDisplayKrwAmount: Long,
    val maxCapAmountAtomic: Long,
    val maxCapDisplayKrwAmount: Long,
    val repaymentTier: String,
    val blockReasonCodes: List<String>,
    val noticeReasonCodes: List<String>,
    val estimatedFeeAmountAtomic: Long,
    val estimatedFeeDisplayKrwAmount: Long,
    val estimatedRepaymentDate: String,
    val disclaimer: String,
    val needsReviewRecordCount: Int
) {
    val availableAmount: Long
        get() = availableDisplayKrwAmount

    val availableAmountInWholeAssetUnits: Int
        get() = availableAmountAtomic.toWholeAssetUnits(assetDecimals)
}

data class AdvanceRequestItemPayload(
    val requestId: Long,
    val workplaceId: Long,
    val assetSymbol: String,
    val assetDecimals: Int,
    val exchangeRateSnapshot: BigDecimal,
    val requestedAmountAtomic: Long,
    val requestedDisplayKrwAmount: Long,
    val approvedAmountAtomic: Long?,
    val approvedDisplayKrwAmount: Long?,
    val status: String,
    val requestStatus: String,
    val payoutStatus: String?,
    val payoutTxHash: String?,
    val repaymentDueDate: String,
    val requestedAt: String
) {
    val requestedAmount: Long
        get() = requestedDisplayKrwAmount

    val approvedAmount: Long?
        get() = approvedDisplayKrwAmount
}

enum class AdvanceRemoteMode {
    LOADING,
    UNAUTHENTICATED,
    EMPTY,
    ERROR,
    CONTENT
}

data class AdvanceRemoteState(
    val mode: AdvanceRemoteMode,
    val workplaceName: String? = null,
    val eligibility: AdvanceEligibilityPayload? = null,
    val requests: List<AdvanceRequestItemPayload> = emptyList(),
    val requestDetailsById: Map<Long, AdvanceRequestDetailPayload> = emptyMap(),
    val errorMessage: String? = null
) {
    val isLoading: Boolean
        get() = mode == AdvanceRemoteMode.LOADING

    val isAuthenticated: Boolean
        get() = mode != AdvanceRemoteMode.UNAUTHENTICATED

    companion object {
        fun loading() = AdvanceRemoteState(mode = AdvanceRemoteMode.LOADING)
        fun unauthenticated(message: String) = AdvanceRemoteState(
            mode = AdvanceRemoteMode.UNAUTHENTICATED,
            errorMessage = message
        )

        fun empty(message: String) = AdvanceRemoteState(
            mode = AdvanceRemoteMode.EMPTY,
            errorMessage = message
        )

        fun error(message: String) = AdvanceRemoteState(
            mode = AdvanceRemoteMode.ERROR,
            errorMessage = message
        )

        fun content(
            workplaceName: String?,
            eligibility: AdvanceEligibilityPayload,
            requests: List<AdvanceRequestItemPayload>,
            requestDetailsById: Map<Long, AdvanceRequestDetailPayload> = emptyMap()
        ) = AdvanceRemoteState(
            mode = AdvanceRemoteMode.CONTENT,
            workplaceName = workplaceName,
            eligibility = eligibility,
            requests = requests,
            requestDetailsById = requestDetailsById,
            errorMessage = null
        )
    }
}

private fun Long.toWholeAssetUnits(decimals: Int): Int {
    if (decimals <= 0) {
        return coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    }
    val wholeAmount = BigDecimal.valueOf(this)
        .movePointLeft(decimals)
        .setScale(0, RoundingMode.DOWN)
        .toLong()
    return wholeAmount.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
}
