package com.dondone.mobile.data.advance

data class AdvanceEligibilitySnapshotPayload(
    val availableAmount: Long,
    val maxCap: Long,
    val policyRate: String,
    val reflectedWorkDays: Int,
    val reflectedWorkMinutes: Long,
    val needsReviewRecordCount: Int
)

data class AdvanceRequestDetailPayload(
    val requestId: Long,
    val workplaceId: Long,
    val requestedAmount: Long,
    val approvedAmount: Long?,
    val feeAmount: Long,
    val status: String,
    val repaymentDueDate: String,
    val eligibilitySnapshot: AdvanceEligibilitySnapshotPayload,
    val createdAt: String
)
