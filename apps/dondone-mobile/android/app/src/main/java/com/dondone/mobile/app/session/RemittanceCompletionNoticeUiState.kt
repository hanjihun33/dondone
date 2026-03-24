package com.dondone.mobile.app.session

import com.dondone.mobile.domain.model.TransferStatus

data class RemittanceCompletionNoticeUiState(
    val transferId: String? = null,
    val status: TransferStatus? = null,
    val recipientName: String? = null,
    val amountAtomic: Long? = null,
    val assetSymbol: String? = null,
    val txHash: String? = null
) {
    val isVisible: Boolean
        get() = transferId != null && status != null
}

data class RemittanceLaunchRequest(
    val requestId: Long
)

enum class WorkproofLaunchTarget {
    PDF_CREATION
}

data class WorkproofLaunchRequest(
    val target: WorkproofLaunchTarget,
    val requestId: Long
)
