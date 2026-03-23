package com.dondone.mobile.data.advance

data class AdvanceEligibilityPayload(
    val workplaceId: Long,
    val availableAmount: Long,
    val repaymentTier: String,
    val blockReasonCodes: List<String>,
    val noticeReasonCodes: List<String>,
    val estimatedRepaymentDate: String,
    val disclaimer: String,
    val needsReviewRecordCount: Int
)

data class AdvanceRequestItemPayload(
    val requestId: Long,
    val requestedAmount: Long,
    val approvedAmount: Long?,
    val status: String,
    val repaymentDueDate: String
)

enum class AdvanceRemoteMode {
    LOADING,
    UNAUTHENTICATED,
    EMPTY,
    ERROR,
    CONTENT
}

data class AdvanceRemoteState(
    val mode: AdvanceRemoteMode,
    val workplaceName: String? = null,
    val eligibility: AdvanceEligibilityPayload? = null,
    val requests: List<AdvanceRequestItemPayload> = emptyList(),
    val requestDetailsById: Map<Long, AdvanceRequestDetailPayload> = emptyMap(),
    val errorMessage: String? = null
) {
    val isLoading: Boolean
        get() = mode == AdvanceRemoteMode.LOADING

    val isAuthenticated: Boolean
        get() = mode != AdvanceRemoteMode.UNAUTHENTICATED

    companion object {
        fun loading() = AdvanceRemoteState(mode = AdvanceRemoteMode.LOADING)
        fun unauthenticated(message: String) = AdvanceRemoteState(
            mode = AdvanceRemoteMode.UNAUTHENTICATED,
            errorMessage = message
        )

        fun empty(message: String) = AdvanceRemoteState(
            mode = AdvanceRemoteMode.EMPTY,
            errorMessage = message
        )

        fun error(message: String) = AdvanceRemoteState(
            mode = AdvanceRemoteMode.ERROR,
            errorMessage = message
        )

        fun content(
            workplaceName: String?,
            eligibility: AdvanceEligibilityPayload,
            requests: List<AdvanceRequestItemPayload>,
            requestDetailsById: Map<Long, AdvanceRequestDetailPayload> = emptyMap()
        ) = AdvanceRemoteState(
            mode = AdvanceRemoteMode.CONTENT,
            workplaceName = workplaceName,
            eligibility = eligibility,
            requests = requests,
            requestDetailsById = requestDetailsById,
            errorMessage = null
        )
    }
}
