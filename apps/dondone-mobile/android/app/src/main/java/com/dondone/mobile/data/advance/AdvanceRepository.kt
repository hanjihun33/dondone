package com.dondone.mobile.data.advance

interface AdvanceRepository {
    suspend fun load(accessToken: String): AdvanceRemoteState
    suspend fun createRequest(
        accessToken: String,
        workplaceId: Long,
        requestedAmountAtomic: Long
    ): AdvanceCreateResult
    suspend fun getRequestDetail(
        accessToken: String,
        requestId: Long
    ): AdvanceRequestDetailPayload
}
