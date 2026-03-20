package com.dondone.mobile.data.advance

data class AdvanceCreateResult(
    val requestId: Long,
    val status: String,
    val approvedAmount: Long?,
    val feeAmount: Long,
    val repaymentDueDate: String
)

class AdvanceUnauthorizedException(
    message: String = "세션이 만료되어 다시 로그인해 주세요."
) : IllegalStateException(message)
