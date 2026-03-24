package com.dondone.mobile.data.remittance

import java.time.LocalDateTime

data class RemittanceWalletPayload(
    val walletAddress: String,
    val fundingStatus: String,
    val fundingFailureReason: String?,
    val fundedAt: LocalDateTime?,
    val createdAt: LocalDateTime?
)

data class RemittanceWalletBalancePayload(
    val walletAddress: String,
    val assetSymbol: String,
    val assetDecimals: Int,
    val tokenBalanceAtomic: String,
    val nativeBalanceWei: String
)

data class RemittanceRecipientPayload(
    val recipientId: String,
    val alias: String,
    val relation: String,
    val walletAddress: String,
    val allowed: Boolean,
    val recentlyUpdated: Boolean,
    val updatedAt: LocalDateTime?
)

data class RemittanceRecipientSearchPayload(
    val candidateUserId: Long,
    val displayName: String,
    val maskedPhoneNumber: String,
    val walletAddressMasked: String,
    val alreadyRegistered: Boolean
)

data class RemittanceTransferSummaryPayload(
    val transferId: String,
    val direction: String = "OUTBOUND",
    val status: String,
    val assetSymbol: String,
    val amountAtomic: Long,
    val senderAddress: String = "",
    val senderName: String? = null,
    val recipientId: String,
    val recipientAlias: String?,
    val recipientAddress: String,
    val txHash: String?,
    val networkFeeWei: String? = null,
    val networkFeeAssetSymbol: String? = null,
    val updatedAt: LocalDateTime?
)

data class RemittanceTransferDetailPayload(
    val transferId: String,
    val direction: String = "OUTBOUND",
    val status: String,
    val assetSymbol: String,
    val amountAtomic: Long,
    val senderAddress: String,
    val senderName: String? = null,
    val recipientId: String,
    val recipientAlias: String?,
    val recipientAddress: String,
    val txHash: String?,
    val networkFeeWei: String? = null,
    val networkFeeAssetSymbol: String? = null,
    val failureCode: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
)

data class RemittanceTransferPrecheckPayload(
    val allowed: Boolean,
    val policyCode: String?,
    val assetSymbol: String,
    val highAmountThresholdAtomic: Long,
    val recentRecipientConfirmationRequired: Boolean,
    val recentRecipientUpdatedAt: LocalDateTime?,
    val walletAddress: String,
    val currentTokenBalanceAtomic: String,
    val currentNativeBalanceWei: String
)

data class RemittanceCreateTransferPayload(
    val transferId: String,
    val status: String,
    val assetSymbol: String,
    val amountAtomic: Long,
    val recipientId: String,
    val createdAt: LocalDateTime?
)

data class RemittanceRemotePayload(
    val wallet: RemittanceWalletPayload,
    val balance: RemittanceWalletBalancePayload?,
    val recipients: List<RemittanceRecipientPayload>,
    val transfers: List<RemittanceTransferSummaryPayload>,
    val activeTransfer: RemittanceTransferDetailPayload?
)

enum class RemittanceRemoteMode {
    LOADING,
    UNAUTHENTICATED,
    ERROR,
    CONTENT
}

data class RemittanceRemoteState(
    val mode: RemittanceRemoteMode,
    val payload: RemittanceRemotePayload? = null,
    val errorMessage: String? = null
) {
    val isAuthenticated: Boolean
        get() = mode != RemittanceRemoteMode.UNAUTHENTICATED

    val isLoading: Boolean
        get() = mode == RemittanceRemoteMode.LOADING

    companion object {
        fun loading() = RemittanceRemoteState(mode = RemittanceRemoteMode.LOADING)

        fun unauthenticated(message: String) = RemittanceRemoteState(
            mode = RemittanceRemoteMode.UNAUTHENTICATED,
            errorMessage = message
        )

        fun error(message: String) = RemittanceRemoteState(
            mode = RemittanceRemoteMode.ERROR,
            errorMessage = message
        )

        fun content(payload: RemittanceRemotePayload) = RemittanceRemoteState(
            mode = RemittanceRemoteMode.CONTENT,
            payload = payload
        )
    }
}
