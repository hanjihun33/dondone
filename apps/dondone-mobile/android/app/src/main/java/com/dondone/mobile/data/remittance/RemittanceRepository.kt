package com.dondone.mobile.data.remittance

interface RemittanceRepository {
    suspend fun load(accessToken: String): RemittanceRemoteState

    suspend fun searchRecipientsByPhone(
        accessToken: String,
        phoneNumber: String
    ): List<RemittanceRecipientSearchPayload>

    suspend fun createRecipient(
        accessToken: String,
        alias: String,
        relation: String,
        walletAddress: String,
        targetUserId: Long? = null
    ): RemittanceRecipientPayload

    suspend fun updateRecipient(
        accessToken: String,
        recipientId: String,
        alias: String,
        relation: String,
        walletAddress: String
    ): RemittanceRecipientPayload

    suspend fun precheck(
        accessToken: String,
        recipientId: String,
        amountAtomic: Long,
        highAmountConfirmed: Boolean,
        recentRecipientConfirmed: Boolean
    ): RemittanceTransferPrecheckPayload

    suspend fun createTransfer(
        accessToken: String,
        recipientId: String,
        amountAtomic: Long,
        highAmountConfirmed: Boolean,
        recentRecipientConfirmed: Boolean
    ): RemittanceCreateTransferPayload

    suspend fun getTransferDetail(
        accessToken: String,
        transferId: String
    ): RemittanceTransferDetailPayload
}
