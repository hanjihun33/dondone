package com.dondone.mobile.data.vault

interface VaultRepository {
    suspend fun load(accessToken: String): VaultRemoteState

    suspend fun createDeposit(
        accessToken: String,
        amountAtomic: Long
    ): VaultCreateTransactionPayload

    suspend fun createWithdrawal(
        accessToken: String,
        amountAtomic: Long
    ): VaultCreateTransactionPayload

    suspend fun getTransactionDetail(
        accessToken: String,
        requestId: String
    ): VaultTransactionDetailPayload
}
