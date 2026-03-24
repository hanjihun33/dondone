package com.dondone.mobile.data.advance

class FallbackAdvanceRepository : AdvanceRepository {
    override suspend fun load(accessToken: String): AdvanceRemoteState {
        return if (accessToken.isBlank()) {
            AdvanceRemoteState.unauthenticated("로그인 후 실연동 데이터를 불러옵니다.")
        } else {
            AdvanceRemoteState.empty("백엔드 실연동 데이터가 없어 데모 상태를 사용합니다.")
        }
    }

    override suspend fun createRequest(
        accessToken: String,
        workplaceId: Long,
        requestedAmountAtomic: Long
    ): AdvanceCreateResult {
        error("백엔드 실연동 데이터가 없어 미리받기 신청을 진행할 수 없습니다.")
    }

    override suspend fun getRequestDetail(
        accessToken: String,
        requestId: Long
    ): AdvanceRequestDetailPayload {
        error("백엔드 실연동 데이터가 없어 미리받기 상세를 불러올 수 없습니다.")
    }
}
