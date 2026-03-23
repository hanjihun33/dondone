package com.dondone.mobile.data.vault

import java.time.LocalDateTime

enum class VaultActionType {
    DEPOSIT,
    WITHDRAW
}

data class VaultInterestPreviewPayload(
    val dailyEstimatedYieldAtomic: String,
    val monthlyEstimatedYieldAtomic: String,
    val yearlyEstimatedYieldAtomic: String,
    val apyBps: Int
)

data class VaultSummaryPayload(
    val walletAddress: String,
    val vaultAddress: String,
    val network: String,
    val assetSymbol: String,
    val assetDecimals: Int,
    val storedAmountAtomic: String,
    val accruedYieldAtomic: String,
    val walletTokenBalanceAtomic: String,
    val availableToStoreAmountAtomic: String,
    val shareBalance: String,
    val interestPreview: VaultInterestPreviewPayload,
    val disclaimer: String
)

data class VaultTransactionItemPayload(
    val requestId: String,
    val txType: String,
    val status: String,
    val amountAtomic: String,
    val shareDelta: String?,
    val txHash: String?,
    val failureCode: String?,
    val updatedAt: LocalDateTime?
)

data class VaultTransactionDetailPayload(
    val requestId: String,
    val txType: String,
    val status: String,
    val walletAddress: String,
    val vaultAddress: String,
    val assetSymbol: String,
    val amountAtomic: String,
    val shareDelta: String?,
    val txHash: String?,
    val failureCode: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val confirmedAt: LocalDateTime?
)

data class VaultCreateTransactionPayload(
    val requestId: String,
    val txType: String,
    val status: String,
    val detailPath: String?,
    val createdAt: LocalDateTime?
)

data class VaultRemotePayload(
    val summary: VaultSummaryPayload,
    val latestTransaction: VaultTransactionDetailPayload?
)

enum class VaultRemoteMode {
    LOADING,
    UNAUTHENTICATED,
    ERROR,
    CONTENT
}

data class VaultRemoteState(
    val mode: VaultRemoteMode,
    val payload: VaultRemotePayload? = null,
    val errorMessage: String? = null
) {
    val isAuthenticated: Boolean
        get() = mode != VaultRemoteMode.UNAUTHENTICATED

    val isLoading: Boolean
        get() = mode == VaultRemoteMode.LOADING

    companion object {
        fun loading() = VaultRemoteState(mode = VaultRemoteMode.LOADING)

        fun unauthenticated(message: String) = VaultRemoteState(
            mode = VaultRemoteMode.UNAUTHENTICATED,
            errorMessage = message
        )

        fun error(message: String) = VaultRemoteState(
            mode = VaultRemoteMode.ERROR,
            errorMessage = message
        )

        fun content(payload: VaultRemotePayload) = VaultRemoteState(
            mode = VaultRemoteMode.CONTENT,
            payload = payload
        )
    }
}
