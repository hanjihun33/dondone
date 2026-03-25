package com.dondone.mobile.data.advance

import java.math.BigDecimal

data class AdvanceCreateResult(
    val requestId: Long,
    val assetSymbol: String,
    val assetDecimals: Int,
    val exchangeRateSnapshot: BigDecimal,
    val status: String,
    val requestStatus: String,
    val payoutStatus: String?,
    val approvedAmountAtomic: Long?,
    val approvedDisplayKrwAmount: Long?,
    val feeAmountAtomic: Long,
    val feeDisplayKrwAmount: Long,
    val settlementStatus: String? = null,
    val settlementDueDate: String? = null,
    val repaymentDueDate: String,
    val eligibilitySnapshot: AdvanceEligibilitySnapshotPayload
) {
    val approvedAmount: Long?
        get() = approvedDisplayKrwAmount

    val feeAmount: Long
        get() = feeDisplayKrwAmount

    val effectiveSettlementDueDate: String
        get() = settlementDueDate ?: repaymentDueDate
}

class AdvanceUnauthorizedException(
    message: String = "세션이 만료되어 다시 로그인해 주세요."
) : IllegalStateException(message)
