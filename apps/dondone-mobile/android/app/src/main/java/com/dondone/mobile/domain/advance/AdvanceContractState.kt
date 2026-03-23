package com.dondone.mobile.domain.advance

import com.dondone.mobile.data.advance.AdvanceRemoteState
import com.dondone.mobile.data.advance.AdvanceRemoteMode
import com.dondone.mobile.domain.calculator.AdvanceCalculator
import com.dondone.mobile.domain.model.DemoState
import java.time.LocalDate
import java.time.format.DateTimeParseException
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
    val noticeTitleText: String? = null,
    val noticeBodyText: String? = null,
    val disclaimerText: String,
    val stateTitleText: String,
    val stateBodyText: String,
    val actionText: String,
    val secondaryActionText: String? = null,
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
                    noticeTitleText = null,
                    noticeBodyText = null,
                    disclaimerText = ADVANCE_DISCLAIMER,
                    stateTitleText = "실연동 한도를 불러오는 중이에요",
                    stateBodyText = "저장된 세션으로 근무지와 미리받기 상태를 확인하고 있어요.",
                    actionText = "불러오는 중",
                    secondaryActionText = null,
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
                    noticeTitleText = null,
                    noticeBodyText = null,
                    disclaimerText = ADVANCE_DISCLAIMER,
                    stateTitleText = "실연동 계정에서는 아직 열리지 않았어요",
                    stateBodyText = remoteState.errorMessage ?: "연결된 근무지나 신청 가능한 한도가 아직 없어요.",
                    actionText = "근거 보기",
                    secondaryActionText = null,
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
                    noticeTitleText = null,
                    noticeBodyText = null,
                    disclaimerText = ADVANCE_DISCLAIMER,
                    stateTitleText = "실연동 상태를 다시 확인해야 해요",
                    stateBodyText = remoteState.errorMessage ?: "백엔드 응답을 다시 불러온 뒤 시도해 주세요.",
                    actionText = "다시 시도",
                    secondaryActionText = null,
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
                        noticeTitleText = null,
                        noticeBodyText = null,
                        disclaimerText = ADVANCE_DISCLAIMER,
                        stateTitleText = "실연동 계정에서는 아직 열리지 않았어요",
                        stateBodyText = "근무 반영이 더 쌓이면 실연동 한도를 다시 확인할 수 있어요.",
                        actionText = "근거 보기",
                        secondaryActionText = null,
                        canRequest = false,
                        availableAmountOverride = 0L
                    )
                val blockReasonTexts = remoteEligibility.blockReasonCodes.map(::toReasonText)
                val noticeReasonTexts = remoteEligibility.noticeReasonCodes.map(::toReasonText)
                val isBlocked = remoteEligibility.availableAmount <= 0
                val isClosedToday = remoteEligibility.blockReasonCodes.contains("ADVANCE_WINDOW_CLOSED_TODAY")
                val hasPendingReview = remoteEligibility.noticeReasonCodes.contains("PENDING_WORKPROOF_REVIEW")
                val isNextCycle = isNextRepaymentCycle(remoteEligibility.estimatedRepaymentDate)
                val noticeTitleText = if (hasPendingReview) {
                    "확인 필요한 기록이 남아 있어요"
                } else {
                    null
                }
                val noticeBodyText = if (hasPendingReview) {
                    val reviewCountText = remoteEligibility.needsReviewRecordCount
                        .takeIf { it > 0 }
                        ?.let { count -> count.toString() + "건" }
                    if (reviewCountText != null) {
                        "확인 필요한 기록 ${reviewCountText}이 남아 있어 현재 가능 금액은 반영 완료 기록 기준으로 계산됐어요."
                    } else {
                        "아직 반영되지 않은 기록이 있어 현재 가능 금액은 반영 완료 기록 기준으로 계산됐어요."
                    }
                } else {
                    null
                }
                val stateTitleText = when {
                    isBlocked && isClosedToday -> "오늘은 신청이 마감됐어요"
                    isBlocked -> "지금은 미리받기를 신청할 수 없어요"
                    else -> "미리받기를 신청할 수 있어요"
                }
                val stateBodyText = when {
                    isBlocked && isClosedToday && isNextCycle ->
                        "오늘 회차는 마감됐어요. 내일부터 다음 달 급여 회차 기준으로 확인할 수 있어요."
                    isBlocked && isClosedToday ->
                        "오늘은 신청이 마감됐어요. 다음 회차는 마감 이후 기준으로 확인해 주세요."
                    blockReasonTexts.isNotEmpty() ->
                        blockReasonTexts.joinToString(" · ")
                    isNextCycle ->
                        "다음 달 급여 회차 기준으로 신청 가능 금액을 불러왔어요."
                    else ->
                        "현재 가능 금액을 확인하고 신청할 수 있어요."
                }
                val actionText = when {
                    isBlocked && isClosedToday -> "마감 이유 보기"
                    isBlocked -> "신청 조건 보기"
                    isNextCycle -> "다음 회차 보기"
                    else -> "미리받기 보기"
                }
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
                    noticeTitleText = noticeTitleText,
                    noticeBodyText = noticeBodyText,
                    disclaimerText = remoteEligibility.disclaimer,
                    stateTitleText = stateTitleText,
                    stateBodyText = stateBodyText,
                    actionText = actionText,
                    secondaryActionText = if (hasPendingReview) "기록 확인" else null,
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
                    noticeTitleText = null,
                    noticeBodyText = null,
                    disclaimerText = ADVANCE_DISCLAIMER,
                    stateTitleText = "로그인이 다시 필요해요",
                    stateBodyText = remoteState.errorMessage ?: "세션을 확인한 뒤 다시 로그인해 주세요.",
                    actionText = "다시 로그인",
                    secondaryActionText = null,
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
    }
    val hasPendingReview = needsReviewRecordCount > 0 || pendingMinutes > 0

    val surfaceState = when {
        snapshot.available > 0 -> AdvanceSurfaceState.SUCCESS
        blockReasonCodes.isNotEmpty() -> AdvanceSurfaceState.BLOCKED
        else -> AdvanceSurfaceState.EMPTY
    }
    val blockReasonTexts = blockReasonCodes.map(::toReasonText)

    val stateTitleText = when (surfaceState) {
        AdvanceSurfaceState.SUCCESS -> "미리받기를 신청할 수 있어요"
        AdvanceSurfaceState.BLOCKED -> "지금은 신청이 잠겨 있어요"
        AdvanceSurfaceState.EMPTY -> "아직 열리지 않았어요"
        AdvanceSurfaceState.LOADING -> "불러오는 중이에요"
        AdvanceSurfaceState.ERROR -> "다시 확인이 필요해요"
    }
    val stateBodyText = when (surfaceState) {
        AdvanceSurfaceState.SUCCESS ->
            "현재 가능 금액을 확인하고 신청할 수 있어요."
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
    val noticeTitleText = if (hasPendingReview) {
        "확인 필요한 기록이 남아 있어요"
    } else {
        null
    }
    val noticeBodyText = if (hasPendingReview) {
        val reviewCountText = needsReviewRecordCount
            .takeIf { it > 0 }
            ?.let { count -> count.toString() + "건" }
        if (reviewCountText != null) {
            "확인 필요한 기록 ${reviewCountText}이 남아 있어 현재 가능 금액은 반영 완료 기록 기준으로 계산됐어요."
        } else {
            "아직 반영되지 않은 기록이 있어 현재 가능 금액은 반영 완료 기록 기준으로 계산됐어요."
        }
    } else {
        null
    }

    return AdvanceContractState(
        surfaceState = surfaceState,
        sourceLabelText = "데모 상태",
        pendingMinutes = pendingMinutes,
        needsReviewRecordCount = needsReviewRecordCount,
        repaymentTier = snapshot.tierName,
        blockReasonCodes = blockReasonCodes,
        blockReasonTexts = blockReasonTexts,
        noticeTitleText = noticeTitleText,
        noticeBodyText = noticeBodyText,
        disclaimerText = ADVANCE_DISCLAIMER,
        stateTitleText = stateTitleText,
        stateBodyText = stateBodyText,
        actionText = actionText,
        secondaryActionText = if (hasPendingReview) "기록 확인" else null,
        canRequest = surfaceState == AdvanceSurfaceState.SUCCESS
    )
}

private fun toReasonText(code: String): String = when (code) {
    "INSUFFICIENT_VERIFIED_WORK" -> "반영된 근무가 더 필요해요"
    "EXISTING_OUTSTANDING_ADVANCE" -> "이미 진행 중인 미리받기가 있어요"
    "ADVANCE_WINDOW_CLOSED_TODAY" -> "오늘은 신청이 마감됐어요"
    "PENDING_WORKPROOF_REVIEW" -> "확인 필요한 기록이 남아 있어요"
    else -> "추가 확인이 필요해요"
}

private fun isNextRepaymentCycle(repaymentDate: String?): Boolean {
    if (repaymentDate == null) return false
    return try {
        val parsed = LocalDate.parse(repaymentDate)
        val today = LocalDate.now()
        parsed.year > today.year || parsed.monthValue > today.monthValue
    } catch (_: DateTimeParseException) {
        false
    }
}
