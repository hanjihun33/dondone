package com.dondone.mobile.feature.wage.presentation

import com.dondone.mobile.app.session.WageActionUiState
import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.data.wage.WageRemoteMode
import com.dondone.mobile.data.wage.WageRemotePayload
import com.dondone.mobile.data.wage.WageRemoteState
import com.dondone.mobile.data.wage.WageVerificationDetailPayload
import com.dondone.mobile.domain.calculator.WageEstimator
import com.dondone.mobile.domain.model.DemoState
import kotlin.math.abs

enum class WageSurfaceState {
    LOADING,
    UNAUTHENTICATED,
    ERROR,
    EMPTY,
    CONTENT
}

enum class WageSurfaceActionType {
    REFRESH,
    OPEN_MENU,
    OPEN_MENU_AND_REGISTRATION_CODE
}

enum class WageMetricIcon {
    BASE,
    OVERTIME,
    NIGHT,
    TOTAL
}

data class WageMetricItemUiModel(
    val label: String,
    val value: String,
    val emphasized: Boolean = false,
    val icon: WageMetricIcon? = null
)

data class WageDepositUiModel(
    val isSubmitting: Boolean,
    val statusText: String,
    val statusTone: BadgeTone,
    val headerText: String,
    val descriptionText: String,
    val metaText: String,
    val actualDepositText: String,
    val deductionBadgeText: String,
    val thresholdBadgeText: String
)

data class WageDifferenceUiModel(
    val title: String,
    val descriptionText: String,
    val locked: Boolean,
    val statusText: String,
    val statusTone: BadgeTone,
    val summaryItems: List<WageMetricItemUiModel>,
    val evidenceLines: List<String>
)

data class WageUiModel(
    val surfaceState: WageSurfaceState,
    val surfaceMessage: String?,
    val surfaceActionText: String?,
    val surfaceActionType: WageSurfaceActionType?,
    val descriptionText: String,
    val deposit: WageDepositUiModel,
    val overviewItems: List<WageMetricItemUiModel>,
    val estimateItems: List<WageMetricItemUiModel>,
    val difference: WageDifferenceUiModel
)

fun DemoState.toWageUiModel(
    remoteState: WageRemoteState? = null,
    actionUiState: WageActionUiState = WageActionUiState()
): WageUiModel {
    val surfaceState = when (remoteState?.mode) {
        WageRemoteMode.LOADING -> WageSurfaceState.LOADING
        WageRemoteMode.UNAUTHENTICATED -> WageSurfaceState.UNAUTHENTICATED
        WageRemoteMode.ERROR -> WageSurfaceState.ERROR
        WageRemoteMode.EMPTY -> WageSurfaceState.EMPTY
        WageRemoteMode.CONTENT, null -> WageSurfaceState.CONTENT
    }
    val payload = remoteState?.payload
    val estimate = payload?.estimate
    val summary = payload?.summary
    val verification = payload?.latestVerification
    val localEstimate = WageEstimator.calculate(this)

    val estimatedBase = estimate?.baseEstimate?.toInt() ?: localEstimate.base
    val estimatedOvertime = estimate?.overtimePremium?.toInt() ?: localEstimate.overtimePremium
    val estimatedNight = estimate?.nightPremium?.toInt() ?: localEstimate.nightPremium
    val estimatedTotal = estimate?.estimatedTotal?.toInt() ?: localEstimate.total
    val actualDeposit = summary?.actualDepositAmount?.toInt() ?: wage.actualDeposit
    val differenceAmount = verification?.differenceAmount?.toInt()
        ?: summary?.differenceAmount?.toInt()
        ?: estimatedTotal - actualDeposit
    val isRecorded = summary?.actualDepositAmount != null || wage.actualDepositRecordedDay != null
    val workDays = summary?.workDays ?: wage.workDays
    val totalHours = ((summary?.totalWorkedMinutes ?: wage.totalHours.toLong() * 60L) / 60L).toInt()
    val overtimeHours = ((summary?.overtimeMinutes ?: wage.overtimeHours.toLong() * 60L) / 60L).toInt()
    val nightHours = ((summary?.nightMinutes ?: wage.nightHours.toLong() * 60L) / 60L).toInt()
    val modifiedCount = summary?.modifiedRecordCount ?: workproof.audit.size
    val differenceStatusText = when {
        !isRecorded -> "입금 대기"
        differenceAmount == 0 -> "거의 일치"
        differenceAmount > 0 -> "부족"
        else -> "초과"
    }
    // Keep tone mapping aligned with the user-facing status text.
    val statusTone = when (differenceStatusText) {
        "부족" -> BadgeTone.Warning
        "초과" -> BadgeTone.Info
        "거의 일치" -> BadgeTone.Success
        else -> BadgeTone.Warning
    }
    val evidenceLines = buildEvidenceLines(
        state = this,
        payload = payload,
        verification = verification,
        overtimeHours = overtimeHours,
        nightHours = nightHours,
        modifiedCount = modifiedCount
    )
    val surfaceMessage = when (surfaceState) {
        WageSurfaceState.LOADING -> "급여 데이터를 불러오는 중입니다."
        WageSurfaceState.UNAUTHENTICATED -> remoteState?.errorMessage ?: "로그인 후 급여 점검을 확인할 수 있습니다."
        WageSurfaceState.ERROR -> remoteState?.errorMessage ?: "급여 데이터를 다시 불러오지 못했습니다."
        WageSurfaceState.EMPTY -> remoteState?.errorMessage ?: "표시할 급여 데이터가 없습니다."
        WageSurfaceState.CONTENT -> null
    }

    return WageUiModel(
        surfaceState = surfaceState,
        surfaceMessage = surfaceMessage,
        surfaceActionText = when (surfaceState) {
            WageSurfaceState.ERROR,
            WageSurfaceState.EMPTY -> {
                if (surfaceMessage.isActiveContractMissingMessage()) "근로계약 확인하기" else "다시 시도"
            }
            WageSurfaceState.UNAUTHENTICATED -> "로그인 필요"
            else -> null
        },
        surfaceActionType = when (surfaceState) {
            WageSurfaceState.ERROR,
            WageSurfaceState.EMPTY -> {
                if (surfaceMessage.isActiveContractMissingMessage()) {
                    WageSurfaceActionType.OPEN_MENU_AND_REGISTRATION_CODE
                } else {
                    WageSurfaceActionType.REFRESH
                }
            }
            WageSurfaceState.UNAUTHENTICATED -> WageSurfaceActionType.REFRESH
            else -> null
        },
        descriptionText = "실제 입금액을 입력하면 예상 급여와 비교해 차액을 확인할 수 있어요.",
        deposit = WageDepositUiModel(
            isSubmitting = actionUiState.isSubmittingDeposit,
            statusText = if (isRecorded) "" else "입금 기록 필요",
            statusTone = if (isRecorded) BadgeTone.Info else BadgeTone.Warning,
            headerText = if (isRecorded) "" else "실제 입금액을 입력해 주세요",
            descriptionText = if (isRecorded) {
                ""
            } else {
                "이번 달 실제로 들어온 금액을 입력하면 차액 여부를 바로 확인할 수 있어요."
            },
            metaText = if (isRecorded) {
                ""
            } else {
                "급여일 ${summary?.paydayDay ?: wage.paydayDay}일 기준"
            },
            actualDepositText = formatKrw(actualDeposit),
            deductionBadgeText = if ((summary?.deductionsKnown ?: wage.deductionsKnown)) "공제 반영" else "공제 여부 확인 전",
            thresholdBadgeText = verification?.threshold?.absoluteWon?.toInt()?.let {
                "임계값 ${formatKrw(it)}"
            } ?: ""
        ),
        overviewItems = listOf(
            WageMetricItemUiModel(label = "근무일", value = "${workDays}일"),
            WageMetricItemUiModel(label = "총 시간", value = "${totalHours}시간"),
            WageMetricItemUiModel(label = "연장/야간", value = "${overtimeHours}/${nightHours}")
        ),
        estimateItems = listOf(
            WageMetricItemUiModel("기본급", formatKrw(estimatedBase), icon = WageMetricIcon.BASE),
            WageMetricItemUiModel("연장 가산", formatKrw(estimatedOvertime), icon = WageMetricIcon.OVERTIME),
            WageMetricItemUiModel("야간 가산", formatKrw(estimatedNight), icon = WageMetricIcon.NIGHT),
            WageMetricItemUiModel("추정 합계", formatKrw(estimatedTotal), emphasized = true, icon = WageMetricIcon.TOTAL)
        ),
        difference = WageDifferenceUiModel(
            title = when {
                !isRecorded -> "입금 기록이 먼저 필요합니다"
                differenceAmount == 0 -> "추정과 실제가 거의 같습니다"
                differenceAmount > 0 -> "실제 입금액이 추정보다 적습니다"
                else -> "실제 입금액이 추정보다 많습니다"
            },
            descriptionText = when {
                !isRecorded -> "차액 비교를 하려면 실제 입금액이 필요합니다."
                differenceAmount == 0 -> "이번 달 급여는 큰 차이 없이 들어온 상태로 보입니다."
                differenceAmount > 0 -> "누락된 수당이나 공제 반영 여부를 먼저 확인해 보세요."
                else -> "추가 지급이나 공제 차이로 이런 결과가 나올 수 있습니다."
            },
            locked = !isRecorded,
            statusText = differenceStatusText,
            statusTone = statusTone,
            summaryItems = listOf(
                WageMetricItemUiModel("추정 급여", formatKrw(estimatedTotal)),
                WageMetricItemUiModel("실제 입금", formatKrw(actualDeposit)),
                WageMetricItemUiModel(
                    "차액",
                    when {
                        differenceAmount > 0 -> "-${formatKrw(abs(differenceAmount))}"
                        differenceAmount < 0 -> "+${formatKrw(abs(differenceAmount))}"
                        else -> formatKrw(0)
                    },
                    emphasized = true
                )
            ),
            evidenceLines = evidenceLines
        )
    )
}

private fun String?.isActiveContractMissingMessage(): Boolean {
    val message = this ?: return false
    return message.contains("활성 근로계약")
}

private fun buildEvidenceLines(
    state: DemoState,
    payload: WageRemotePayload?,
    verification: WageVerificationDetailPayload?,
    overtimeHours: Int,
    nightHours: Int,
    modifiedCount: Int
): List<String> {
    val remoteEvidence = verification?.evidence
    return buildList {
        payload?.summary?.reasons?.firstOrNull()?.let { reason ->
            add("가능한 원인: ${reason.title}")
        }
        add("연장 ${remoteEvidence?.overtimeMinutes?.div(60) ?: overtimeHours}시간 반영")
        add("야간 ${remoteEvidence?.nightMinutes?.div(60) ?: nightHours}시간 반영")
        add("수정 기록 ${remoteEvidence?.modifiedRecordCount ?: modifiedCount}건 확인")
        val relatedCount = verification?.relatedActions?.let {
            listOfNotNull(it.proofPackDocumentId, it.claimKitDocumentId, it.preparationId).size
        } ?: state.documents.size
        add("연결된 후속 문서/행동 ${relatedCount}건")
    }
}
