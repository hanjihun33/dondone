package com.dondone.mobile.domain.advance

import com.dondone.mobile.data.advance.AdvanceRemoteState
import com.dondone.mobile.data.advance.AdvanceRemoteMode
import com.dondone.mobile.domain.calculator.AdvanceCalculator
import com.dondone.mobile.domain.model.DemoState
import kotlin.math.max

private const val STANDARD_WORKDAY_MINUTES = 480
private const val ADVANCE_DISCLAIMER =
    "미리받기 금액은 반영된 근무 기록 기준의 데모 시뮬레이션입니다. 실제 금융 서비스 제공을 의미하지 않습니다."

enum class AdvanceSurfaceState {
    LOADING,
    EMPTY,
    ERROR,
    BLOCKED,
    SUCCESS
}

data class AdvanceContractState(
    val surfaceState: AdvanceSurfaceState,
    val sourceLabelText: String,
    val pendingMinutes: Int,
    val needsReviewRecordCount: Int,
    val repaymentTier: String,
    val blockReasonCodes: List<String>,
    val blockReasonTexts: List<String>,
    val disclaimerText: String,
    val stateTitleText: String,
    val stateBodyText: String,
    val actionText: String,
    val canRequest: Boolean,
    val availableAmountOverride: Long? = null,
    val repaymentDateOverride: String? = null
)

fun DemoState.toAdvanceContractState(remoteState: AdvanceRemoteState? = null): AdvanceContractState {
    val snapshot = AdvanceCalculator.calculate(this)
    if (remoteState != null) {
        when (remoteState.mode) {
            AdvanceRemoteMode.LOADING -> {
                return AdvanceContractState(
                    surfaceState = AdvanceSurfaceState.LOADING,
                    sourceLabelText = "실연동 준비 중",
                    pendingMinutes = 0,
                    needsReviewRecordCount = 0,
                    repaymentTier = "-",
                    blockReasonCodes = emptyList(),
                    blockReasonTexts = emptyList(),
                    disclaimerText = ADVANCE_DISCLAIMER,
                    stateTitleText = "실연동 한도를 불러오는 중이에요",
                    stateBodyText = "저장된 세션으로 근무지와 미리받기 상태를 확인하고 있어요.",
                    actionText = "불러오는 중",
                    canRequest = false,
                    availableAmountOverride = 0L
                )
            }

            AdvanceRemoteMode.EMPTY -> {
                return AdvanceContractState(
                    surfaceState = AdvanceSurfaceState.EMPTY,
                    sourceLabelText = "실연동",
                    pendingMinutes = 0,
                    needsReviewRecordCount = 0,
                    repaymentTier = "-",
                    blockReasonCodes = emptyList(),
                    blockReasonTexts = emptyList(),
                    disclaimerText = ADVANCE_DISCLAIMER,
                    stateTitleText = "실연동 계정에서는 아직 열리지 않았어요",
                    stateBodyText = remoteState.errorMessage ?: "연결된 근무지나 신청 가능한 한도가 아직 없어요.",
                    actionText = "근거 보기",
                    canRequest = false,
                    availableAmountOverride = 0L
                )
            }

            AdvanceRemoteMode.ERROR -> {
                return AdvanceContractState(
                    surfaceState = AdvanceSurfaceState.ERROR,
                    sourceLabelText = "실연동 오류",
                    pendingMinutes = 0,
                    needsReviewRecordCount = 0,
                    repaymentTier = "-",
                    blockReasonCodes = emptyList(),
                    blockReasonTexts = emptyList(),
                    disclaimerText = ADVANCE_DISCLAIMER,
                    stateTitleText = "실연동 상태를 다시 확인해야 해요",
                    stateBodyText = remoteState.errorMessage ?: "백엔드 응답을 다시 불러온 뒤 시도해 주세요.",
                    actionText = "다시 시도",
                    canRequest = false,
                    availableAmountOverride = 0L
                )
            }

            AdvanceRemoteMode.CONTENT -> {
                val remoteEligibility = remoteState.eligibility
                    ?: return AdvanceContractState(
                        surfaceState = AdvanceSurfaceState.EMPTY,
                        sourceLabelText = "실연동",
                        pendingMinutes = 0,
                        needsReviewRecordCount = 0,
                        repaymentTier = "-",
                        blockReasonCodes = emptyList(),
                        blockReasonTexts = emptyList(),
                        disclaimerText = ADVANCE_DISCLAIMER,
                        stateTitleText = "실연동 계정에서는 아직 열리지 않았어요",
                        stateBodyText = "근무 반영이 더 쌓이면 실연동 한도를 다시 확인할 수 있어요.",
                        actionText = "근거 보기",
                        canRequest = false,
                        availableAmountOverride = 0L
                    )
                val blockReasonTexts = remoteEligibility.blockReasonCodes.map(::toReasonText)
                val isBlocked = remoteEligibility.availableAmount <= 0
                return AdvanceContractState(
                    surfaceState = if (isBlocked) AdvanceSurfaceState.BLOCKED else AdvanceSurfaceState.SUCCESS,
                    sourceLabelText = if (remoteState.workplaceName != null) {
                        "실연동 · ${remoteState.workplaceName}"
                    } else {
                        "실연동"
                    },
                    pendingMinutes = 0,
                    needsReviewRecordCount = remoteEligibility.needsReviewRecordCount,
                    repaymentTier = remoteEligibility.repaymentTier,
                    blockReasonCodes = remoteEligibility.blockReasonCodes,
                    blockReasonTexts = blockReasonTexts,
                    disclaimerText = remoteEligibility.disclaimer,
                    stateTitleText = if (isBlocked) "실제 계정 기준으로 지금은 신청이 잠겨 있어요" else "실제 계정 기준으로 신청 가능한 상태예요",
                    stateBodyText = when {
                        blockReasonTexts.isNotEmpty() -> blockReasonTexts.joinToString(" · ")
                        else -> "백엔드 응답 기준으로 현재 미리받기 한도를 불러왔어요."
                    },
                    actionText = if (isBlocked) "근거 보기" else "미리받기 보기",
                    canRequest = !isBlocked,
                    availableAmountOverride = remoteEligibility.availableAmount,
                    repaymentDateOverride = remoteEligibility.estimatedRepaymentDate
                )
            }

            AdvanceRemoteMode.UNAUTHENTICATED -> {
                return AdvanceContractState(
                    surfaceState = AdvanceSurfaceState.ERROR,
                    sourceLabelText = "실연동 필요",
                    pendingMinutes = 0,
                    needsReviewRecordCount = 0,
                    repaymentTier = "-",
                    blockReasonCodes = emptyList(),
                    blockReasonTexts = emptyList(),
                    disclaimerText = ADVANCE_DISCLAIMER,
                    stateTitleText = "로그인이 다시 필요해요",
                    stateBodyText = remoteState.errorMessage ?: "세션을 확인한 뒤 다시 로그인해 주세요.",
                    actionText = "다시 로그인",
                    canRequest = false,
                    availableAmountOverride = 0L
                )
            }
        }
    }

    val pendingMinutes = max(0, wage.workDays - snapshot.verifiedDays) * STANDARD_WORKDAY_MINUTES
    val needsReviewRecordCount = workproof.audit.size
    val blockReasonCodes = buildList {
        if (snapshot.available <= 0) {
            add("INSUFFICIENT_VERIFIED_WORK")
        }
        if (needsReviewRecordCount > 0 || pendingMinutes > 0) {
            add("PENDING_WORKPROOF_REVIEW")
        }
    }

    val surfaceState = when {
        snapshot.available > 0 -> AdvanceSurfaceState.SUCCESS
        blockReasonCodes.isNotEmpty() -> AdvanceSurfaceState.BLOCKED
        else -> AdvanceSurfaceState.EMPTY
    }
    val blockReasonTexts = blockReasonCodes.map(::toReasonText)

    val stateTitleText = when (surfaceState) {
        AdvanceSurfaceState.SUCCESS -> "지금 신청 가능한 상태예요"
        AdvanceSurfaceState.BLOCKED -> "지금은 신청이 잠겨 있어요"
        AdvanceSurfaceState.EMPTY -> "아직 열리지 않았어요"
        AdvanceSurfaceState.LOADING -> "불러오는 중이에요"
        AdvanceSurfaceState.ERROR -> "다시 확인이 필요해요"
    }
    val stateBodyText = when (surfaceState) {
        AdvanceSurfaceState.SUCCESS ->
            "반영된 근무와 한도 기준을 바탕으로 지금 신청 가능한 금액을 계산했어요."
        AdvanceSurfaceState.BLOCKED ->
            if (blockReasonTexts.isNotEmpty()) blockReasonTexts.joinToString(" · ") else "반영 상태를 먼저 확인해 주세요."
        AdvanceSurfaceState.EMPTY ->
            "반영된 근무가 더 쌓이면 미리받기 한도가 열릴 수 있어요."
        AdvanceSurfaceState.LOADING ->
            "근무 반영과 한도를 다시 계산하고 있어요."
        AdvanceSurfaceState.ERROR ->
            "계약과 근무 반영 상태를 다시 확인한 뒤 시도해 주세요."
    }
    val actionText = when (surfaceState) {
        AdvanceSurfaceState.SUCCESS -> "미리받기 보기"
        AdvanceSurfaceState.BLOCKED, AdvanceSurfaceState.EMPTY -> "근거 보기"
        AdvanceSurfaceState.LOADING -> "불러오는 중"
        AdvanceSurfaceState.ERROR -> "다시 시도"
    }

    return AdvanceContractState(
        surfaceState = surfaceState,
        sourceLabelText = "데모 상태",
        pendingMinutes = pendingMinutes,
        needsReviewRecordCount = needsReviewRecordCount,
        repaymentTier = snapshot.tierName,
        blockReasonCodes = blockReasonCodes,
        blockReasonTexts = blockReasonTexts,
        disclaimerText = ADVANCE_DISCLAIMER,
        stateTitleText = stateTitleText,
        stateBodyText = stateBodyText,
        actionText = actionText,
        canRequest = surfaceState == AdvanceSurfaceState.SUCCESS
    )
}

private fun toReasonText(code: String): String = when (code) {
    "INSUFFICIENT_VERIFIED_WORK" -> "반영된 근무가 더 필요해요"
    "PENDING_WORKPROOF_REVIEW" -> "확인 필요한 기록이 남아 있어요"
    else -> "추가 확인이 필요해요"
}
