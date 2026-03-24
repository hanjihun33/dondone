package com.dondone.mobile.data.advance

import java.math.BigDecimal

data class AdvanceEligibilitySnapshotPayload(
    val assetSymbol: String,
    val assetDecimals: Int,
    val exchangeRateSnapshot: BigDecimal,
    val availableAmountAtomic: Long,
    val availableDisplayKrwAmount: Long,
    val maxCapAmountAtomic: Long,
    val maxCapDisplayKrwAmount: Long,
    val policyRate: String,
    val reflectedWorkDays: Int,
    val reflectedWorkMinutes: Long,
    val needsReviewRecordCount: Int
) {
    val availableAmount: Long
        get() = availableDisplayKrwAmount

    val maxCap: Long
        get() = maxCapDisplayKrwAmount
}

data class AdvanceRequestDetailPayload(
    val requestId: Long,
    val workplaceId: Long,
    val assetSymbol: String,
    val assetDecimals: Int,
    val exchangeRateSnapshot: BigDecimal,
    val requestedAmountAtomic: Long,
    val requestedDisplayKrwAmount: Long,
    val approvedAmountAtomic: Long?,
    val approvedDisplayKrwAmount: Long?,
    val feeAmountAtomic: Long,
    val feeDisplayKrwAmount: Long,
    val status: String,
    val requestStatus: String,
    val payoutStatus: String?,
    val payoutTxHash: String?,
    val settlementStatus: String? = null,
    val settlementDueDate: String? = null,
    val repaymentDueDate: String,
    val eligibilitySnapshot: AdvanceEligibilitySnapshotPayload,
    val createdAt: String
) {
    val requestedAmount: Long
        get() = requestedDisplayKrwAmount

    val approvedAmount: Long?
        get() = approvedDisplayKrwAmount

    val feeAmount: Long
        get() = feeDisplayKrwAmount

    val effectiveSettlementDueDate: String
        get() = settlementDueDate ?: repaymentDueDate
}
