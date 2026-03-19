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

enum class WageActionTarget {
    TRANSFER,
    WORKPROOF,
    MENU
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

data class WageQuickActionUiModel(
    val label: String,
    val target: WageActionTarget
)

data class WageDepositUiModel(
    val recorded: Boolean,
    val statusText: String,
    val statusTone: BadgeTone,
    val headerText: String,
    val descriptionText: String,
    val metaText: String,
    val actionButtonText: String?,
    val actualDepositText: String,
    val inputHelperText: String,
    val deductionBadgeText: String,
    val thresholdBadgeText: String
)

data class WageDifferenceStepUiModel(
    val step: String,
    val label: String,
    val status: String
)

data class WageDifferenceUiModel(
    val title: String,
    val descriptionText: String,
    val locked: Boolean,
    val statusText: String,
    val statusTone: BadgeTone,
    val lockedTitle: String,
    val lockedDescription: String,
    val summaryItems: List<WageMetricItemUiModel>,
    val evidenceLines: List<String>,
    val steps: List<WageDifferenceStepUiModel>,
    val primaryActionDescription: String,
    val primaryActionStepText: String,
    val primaryActionButtonText: String,
    val primaryActionTarget: WageActionTarget,
    val primaryActionEnabled: Boolean,
    val primaryActionLoading: Boolean,
    val secondaryActions: List<WageQuickActionUiModel>,
    val secondaryActionsToggleText: String,
    val primaryHintText: String,
    val disclaimerText: String
)

data class WageCopilotChipUiModel(
    val label: String,
    val title: String,
    val description: String,
    val detailLines: List<String>
)

data class WageUiModel(
    val surfaceState: WageSurfaceState,
    val surfaceMessage: String?,
    val surfaceActionText: String?,
    val chips: List<WageCopilotChipUiModel>,
    val disclaimerLines: List<String>,
    val titleText: String,
    val descriptionText: String,
    val modifiedCountText: String,
    val evidenceBadgeText: String,
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
    val recordedDay = summary?.actualDepositRecordedDate?.dayOfMonth
        ?: summary?.actualDepositRecordedDay
        ?: wage.actualDepositRecordedDay
    val formattedRecordedDate = recordedDay?.let { day ->
        "${demo.year}-${demo.month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    }
    val workDays = summary?.workDays ?: wage.workDays
    val totalHours = ((summary?.totalWorkedMinutes ?: wage.totalHours.toLong() * 60L) / 60L).toInt()
    val overtimeHours = ((summary?.overtimeMinutes ?: wage.overtimeHours.toLong() * 60L) / 60L).toInt()
    val nightHours = ((summary?.nightMinutes ?: wage.nightHours.toLong() * 60L) / 60L).toInt()
    val modifiedCount = summary?.modifiedRecordCount ?: workproof.audit.size
    val disclaimer = (estimate?.disclaimer
        ?: summary?.disclaimer
        ?: "급여 계산 결과는 참고용 추정치입니다.")
        .toKoreanWageDisclaimer()
    val statusTone = when {
        !isRecorded -> BadgeTone.Warning
        differenceAmount != 0 -> BadgeTone.Warning
        else -> BadgeTone.Success
    }
    val evidenceLines = buildEvidenceLines(
        state = this,
        payload = payload,
        verification = verification,
        overtimeHours = overtimeHours,
        nightHours = nightHours,
        modifiedCount = modifiedCount
    )

    return WageUiModel(
        surfaceState = surfaceState,
        surfaceMessage = when (surfaceState) {
            WageSurfaceState.LOADING -> "급여 데이터를 불러오는 중입니다."
            WageSurfaceState.UNAUTHENTICATED -> remoteState?.errorMessage ?: "로그인 후 급여 점검을 확인할 수 있습니다."
            WageSurfaceState.ERROR -> remoteState?.errorMessage ?: "급여 데이터를 다시 불러오지 못했습니다."
            WageSurfaceState.EMPTY -> remoteState?.errorMessage ?: "표시할 급여 데이터가 없습니다."
            WageSurfaceState.CONTENT -> null
        },
        surfaceActionText = when (surfaceState) {
            WageSurfaceState.ERROR,
            WageSurfaceState.EMPTY -> "다시 시도"
            WageSurfaceState.UNAUTHENTICATED -> "로그인 필요"
            else -> null
        },
        chips = listOf(
            WageCopilotChipUiModel(
                label = "왜 차액이 있나요",
                title = "차액 판단 기준",
                description = if (isRecorded) {
                    "추정 급여 ${formatKrw(estimatedTotal)}와 실제 입금 ${formatKrw(actualDeposit)}를 비교했습니다."
                } else {
                    "실제 입금액이 없어서 현재는 추정 급여만 먼저 보여주고 있습니다."
                },
                detailLines = evidenceLines
            ),
            WageCopilotChipUiModel(
                label = "어떤 근거인가요",
                title = "사용한 근거",
                description = "근무 기록과 서버 급여 계산 결과를 함께 반영했습니다.",
                detailLines = listOf(
                    "근무일수 ${workDays}일 반영",
                    "총 ${totalHours}시간, 연장 ${overtimeHours}시간, 야간 ${nightHours}시간 반영",
                    "수정 이력 ${modifiedCount}건 반영"
                )
            ),
            WageCopilotChipUiModel(
                label = "다음 액션은요",
                title = "권장 진행 순서",
                description = if (isRecorded) {
                    "급여 확인을 생성하면 차액 사유와 관련 액션을 서버 기준으로 볼 수 있습니다."
                } else {
                    "먼저 실제 입금액을 기록한 뒤 급여 확인을 생성하는 흐름이 맞습니다."
                },
                detailLines = listOf(
                    if (isRecorded) "1. 급여 확인 생성" else "1. 실제 입금액 기록",
                    if (verification != null) "2. 최신 급여 확인 결과 검토" else "2. 차액 결과 확인",
                    "3. 근거 문서, 메뉴, 송금 흐름으로 이어가기"
                )
            )
        ),
        disclaimerLines = listOf(disclaimer),
        titleText = "이번 달 급여 점검",
        descriptionText = "실제 입금액과 서버 추정 급여를 비교해 차액 여부를 확인합니다.",
        modifiedCountText = "수정 ${modifiedCount}건",
        evidenceBadgeText = if (verification != null) "확인 결과 반영" else "근거 준비",
        deposit = WageDepositUiModel(
            recorded = isRecorded,
            statusText = if (isRecorded) "입금 기록 완료" else "입금 기록 필요",
            statusTone = if (isRecorded) BadgeTone.Info else BadgeTone.Warning,
            headerText = if (isRecorded) "입금 기록이 반영되었습니다" else "실제 입금액을 입력해 주세요",
            descriptionText = if (isRecorded) {
                "입금액을 다시 저장하면 차액 계산과 급여 확인 흐름이 함께 갱신됩니다."
            } else {
                "입금액을 기록하면 차액 분석과 다음 액션을 이어서 볼 수 있습니다."
            },
            metaText = formattedRecordedDate?.let { "기록일 $it" }
                ?: "급여일 ${summary?.paydayDay ?: wage.paydayDay}일 기준",
            actionButtonText = if (isRecorded) null else "적용",
            actualDepositText = formatKrw(actualDeposit),
            inputHelperText = if (actionUiState.isSubmittingDeposit) {
                "입금액을 저장하는 중입니다."
            } else {
                "숫자만 입력하면 바로 저장할 수 있습니다."
            },
            deductionBadgeText = if ((summary?.deductionsKnown ?: wage.deductionsKnown)) "공제 반영" else "공제 미확인",
            thresholdBadgeText = verification?.threshold?.absoluteWon?.toInt()?.let {
                "임계값 ${formatKrw(it)}"
            } ?: "임계값 자동 적용"
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
            statusText = when {
                !isRecorded -> "입금 대기"
                differenceAmount == 0 -> "거의 일치"
                else -> "확인 필요"
            },
            statusTone = statusTone,
            lockedTitle = "입금 기록 후 급여 확인을 생성할 수 있습니다",
            lockedDescription = "백엔드 급여 확인을 요청하려면 실제 입금액이 먼저 저장되어 있어야 합니다.",
            summaryItems = listOf(
                WageMetricItemUiModel("추정 급여", formatKrw(estimatedTotal)),
                WageMetricItemUiModel("실제 입금", formatKrw(actualDeposit)),
                WageMetricItemUiModel("차액", formatKrw(abs(differenceAmount)), emphasized = true)
            ),
            evidenceLines = evidenceLines,
            steps = buildVerificationSteps(verification),
            primaryActionDescription = if (verification != null) {
                "최신 기준으로 급여 확인을 다시 만들어 결과를 새로 반영합니다."
            } else {
                "급여 확인을 만들면 차액 사유와 다음 행동을 더 분명하게 볼 수 있습니다."
            },
            primaryActionStepText = if (verification != null) "재확인" else "확인 생성",
            primaryActionButtonText = when {
                actionUiState.isSubmittingVerification -> "생성 중..."
                verification != null -> "다시 확인하기"
                else -> "급여 확인하기"
            },
            primaryActionTarget = WageActionTarget.WORKPROOF,
            primaryActionEnabled = isRecorded && !actionUiState.isSubmitting,
            primaryActionLoading = actionUiState.isSubmittingVerification,
            secondaryActions = listOf(
                WageQuickActionUiModel("근무 기록", WageActionTarget.WORKPROOF),
                WageQuickActionUiModel("문서", WageActionTarget.MENU),
                WageQuickActionUiModel("송금", WageActionTarget.TRANSFER)
            ),
            secondaryActionsToggleText = "다른 액션 보기",
            primaryHintText = verification?.possibleCauses?.firstOrNull()?.title
                ?: "급여 확인은 서버 계산과 근거 기록을 함께 봅니다.",
            disclaimerText = disclaimer
        )
    )
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

private fun buildVerificationSteps(
    verification: WageVerificationDetailPayload?
): List<WageDifferenceStepUiModel> {
    if (verification == null) {
        return listOf(
            WageDifferenceStepUiModel("1", "급여 확인 생성", "대기"),
            WageDifferenceStepUiModel("2", "근거 검토", "대기"),
            WageDifferenceStepUiModel("3", "다음 액션 선택", "대기")
        )
    }

    val relatedActions = verification.relatedActions
    return listOf(
        WageDifferenceStepUiModel("1", "급여 확인", verification.status.toWageStatusText()),
        WageDifferenceStepUiModel(
            "2",
            "증빙 묶음",
            if (relatedActions.proofPackReady) "준비됨" else "대기"
        ),
        WageDifferenceStepUiModel(
            "3",
            "청구 키트",
            if (relatedActions.claimKitReady) "준비됨" else "대기"
        )
    )
}

private fun String.toWageStatusText(): String = when (this) {
    "MATCHED" -> "일치"
    "CHECK_REQUIRED" -> "확인 필요"
    "PENDING" -> "대기"
    "READY" -> "준비됨"
    "FAILED" -> "실패"
    else -> this
}

private fun String.toKoreanWageDisclaimer(): String {
    val normalized = trim()
    return when {
        normalized.isBlank() -> "급여 계산 결과는 참고용 추정치입니다."
        normalized.equals(
            "Reference-only estimate. Actual payment and deductions can differ by contract, payroll rules, and payslip details.",
            ignoreCase = true
        ) -> "이 급여 계산 결과는 참고용 추정치이며, 실제 지급액과 공제는 근로계약, 급여 규정, 급여명세서에 따라 달라질 수 있습니다."
        normalized.startsWith("Reference-only estimate", ignoreCase = true) ->
            "이 급여 계산 결과는 참고용 추정치이며, 실제 지급액과 공제는 근로계약과 급여명세서 기준으로 달라질 수 있습니다."
        else -> normalized
    }
}
