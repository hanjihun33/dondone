package com.dondone.mobile.feature.wage.presentation

import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.domain.calculator.WageEstimator
import com.dondone.mobile.domain.model.DemoState
import kotlin.math.abs

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

fun DemoState.toWageUiModel(): WageUiModel {
    val estimate = WageEstimator.calculate(this)
    val recordedDayText = wage.actualDepositRecordedDay?.toString()?.padStart(2, '0')
    val recordedDateText = recordedDayText?.let {
        "${demo.year}-${demo.month.toString().padStart(2, '0')}-$it"
    }
    val isRecorded = wage.actualDepositRecordedDay != null
    val hasDifference = estimate.difference != 0
    val differenceAmountText = formatKrw(abs(estimate.difference))
    val estimateTotalText = formatKrw(estimate.total)
    val actualDepositText = formatKrw(wage.actualDeposit)

    val diffTitle = when {
        !isRecorded -> "입금 확인이 필요해요"
        !hasDifference -> "차액이 없어요"
        else -> "차액 확인이 필요해요"
    }
    val diffDescription = when {
        !isRecorded -> "실입금을 입력하면 추정 급여와 비교 결과가 바로 갱신됩니다."
        estimate.difference > 0 -> "예상보다 적게 입금됐어요. 근거를 먼저 확인하고 다음 행동으로 이어가세요."
        estimate.difference < 0 -> "예상보다 많이 입금됐어요. 항목 반영 차이를 근거와 함께 점검해 보세요."
        else -> "예상 급여와 실제 입금이 일치합니다. 바로 다음 금융 흐름으로 이어갈 수 있어요."
    }

    val whyDiffDescription = if (isRecorded) {
        "현재 추정 총액은 ${estimateTotalText}이고 실제 입금액은 ${actualDepositText}입니다. 차액은 ${differenceAmountText}입니다."
    } else {
        "현재 추정 총액은 ${estimateTotalText}입니다. 실입금을 입력하면 차액 원인을 더 정확하게 비교할 수 있어요."
    }

    val nextDescription = when {
        !isRecorded -> "먼저 실입금을 기록하면 차액 분석과 다음 단계 추천이 정확해집니다."
        hasDifference -> "차액을 확인했다면 근거 문서를 먼저 정리하는 순서가 가장 안전합니다."
        else -> "차액이 없으니 급여 점검을 마무리하고 다음 금융 흐름으로 넘어갈 수 있어요."
    }

    return WageUiModel(
        chips = listOf(
            WageCopilotChipUiModel(
                label = "왜 차액이 생겼어?",
                title = "왜 차액이 생겼나요?",
                description = whyDiffDescription,
                detailLines = listOf(
                    "연장 ${wage.overtimeHours}시간, 야간 ${wage.nightHours}시간이 추정 급여에 반영되어 있어요.",
                    "수정 기록 ${workproof.audit.size}건과 첨부 여부까지 함께 확인합니다.",
                    "실제 지급에 수당, 공제, 정정분 반영 시차가 있으면 차액이 생길 수 있어요."
                )
            ),
            WageCopilotChipUiModel(
                label = "어떤 근거야?",
                title = "근거는 무엇인가요?",
                description = "급여 점검은 WorkProof 기록과 수정 이력을 근거로 계산합니다.",
                detailLines = listOf(
                    "출근/퇴근 시각과 위치 스냅샷",
                    "수정 사유, 전후 값, 첨부 유무",
                    "Proof Pack과 근거 자료 묶음에 동일한 표와 로그로 정리"
                )
            ),
            WageCopilotChipUiModel(
                label = "다음 행동은?",
                title = "지금 할 수 있는 다음 행동",
                description = nextDescription,
                detailLines = listOf(
                    if (isRecorded) "1. Proof Pack 만들기" else "1. 실입금 입력하기",
                    if (isRecorded) "2. 근거 자료 묶음 생성" else "2. 차액과 근거 확인하기",
                    if (hasDifference && isRecorded) "3. 신고 준비로 이어가기" else "3. 다음 금융 흐름 확인하기"
                )
            )
        ),
        disclaimerLines = listOf(
            "이 급여 계산은 참고용 추정입니다. 실제 지급과 공제는 근로계약, 급여명세서, 회사 규정에 따라 달라질 수 있습니다.",
            "공제 항목이 일부 입력되지 않았다면 현재는 보수적인 기준으로 계산됩니다."
        ),
        titleText = "이번 달 급여 확인",
        descriptionText = "먼저 실입금을 입력하고 추정 급여와 바로 비교해 보세요.",
        modifiedCountText = "수정 ${workproof.audit.size}",
        evidenceBadgeText = "근거 준비",
        deposit = WageDepositUiModel(
            recorded = isRecorded,
            statusText = if (isRecorded) "입금 입력 완료" else "입금 입력 전",
            statusTone = if (isRecorded) BadgeTone.Info else BadgeTone.Warning,
            headerText = if (isRecorded) "이번 달 실입금이 반영됐어요" else "실제 입금액을 먼저 확인해 주세요",
            descriptionText = if (isRecorded) {
                "금액을 조정하면 아래 차액 확인과 추천 경로가 함께 갱신됩니다."
            } else {
                "실입금을 입력하면 차액 분석과 다음 행동 추천이 이어집니다."
            },
            metaText = if (isRecorded) {
                "입금 기록일 · $recordedDateText"
            } else {
                "급여일 ${wage.paydayDay}일 기준 입력 대기"
            },
            actionButtonText = if (isRecorded) null else "적용",
            actualDepositText = actualDepositText,
            inputHelperText = if (isRecorded) {
                "금액을 조정하면 추정 급여와의 차액이 바로 갱신됩니다."
            } else {
                "실입금을 먼저 입력하면 차액 원인과 다음 단계가 정리됩니다."
            },
            deductionBadgeText = if (wage.deductionsKnown) "공제 반영" else "공제 미반영",
            thresholdBadgeText = "임계값 자동 적용"
        ),
        overviewItems = listOf(
            WageMetricItemUiModel(label = "근무일", value = "${wage.workDays}일"),
            WageMetricItemUiModel(label = "총 시간", value = "${wage.totalHours}시간"),
            WageMetricItemUiModel(label = "연장/야간", value = "${wage.overtimeHours}/${wage.nightHours}")
        ),
        estimateItems = listOf(
            WageMetricItemUiModel(label = "기본", value = formatKrw(estimate.base), icon = WageMetricIcon.BASE),
            WageMetricItemUiModel(label = "연장 프리미엄", value = formatKrw(estimate.overtimePremium), icon = WageMetricIcon.OVERTIME),
            WageMetricItemUiModel(label = "야간 프리미엄", value = formatKrw(estimate.nightPremium), icon = WageMetricIcon.NIGHT),
            WageMetricItemUiModel(label = "추정 합계", value = estimateTotalText, emphasized = true, icon = WageMetricIcon.TOTAL)
        ),
        difference = WageDifferenceUiModel(
            title = diffTitle,
            descriptionText = diffDescription,
            locked = !isRecorded,
            statusText = when {
                !isRecorded -> "입금 대기"
                hasDifference -> "차액 확인 필요"
                else -> "차액 없음"
            },
            statusTone = when {
                !isRecorded -> BadgeTone.Warning
                hasDifference -> BadgeTone.Warning
                else -> BadgeTone.Success
            },
            lockedTitle = "실입금을 입력하면 다음 행동이 열려요",
            lockedDescription = "입금액을 입력하면 차액 확인, 문서 생성, 신고 준비까지 이어서 진행할 수 있어요.",
            summaryItems = listOf(
                WageMetricItemUiModel(label = "추정", value = estimateTotalText),
                WageMetricItemUiModel(label = "실제", value = actualDepositText),
                WageMetricItemUiModel(label = "차액", value = differenceAmountText, emphasized = true)
            ),
            evidenceLines = listOf(
                "연장 ${wage.overtimeHours}시간, 야간 ${wage.nightHours}시간이 추정치에 포함돼 있어요.",
                "수정 기록 ${workproof.audit.size}건을 이유와 첨부 상태까지 함께 확인합니다.",
                "근거 자료 ${documents.size}건과 WorkProof 로그를 연결해 둔 상태예요."
            ),
            steps = listOf(
                WageDifferenceStepUiModel("1", "Proof Pack", if (workproof.records.isNotEmpty()) "준비됨" else "대기"),
                WageDifferenceStepUiModel("2", "근거 자료 묶음", if (documents.isNotEmpty()) "준비됨" else "대기"),
                WageDifferenceStepUiModel("3", "신고 준비", if (documents.isNotEmpty()) "준비됨" else "대기")
            ),
            primaryActionDescription = if (hasDifference) {
                "수정 기록과 근무 근거를 먼저 확인한 뒤 문서 준비 흐름으로 이어가세요."
            } else {
                "차액이 없으니 다음 금융 흐름으로 바로 넘어갈 수 있어요."
            },
            primaryActionStepText = if (hasDifference) "단계 1" else "단계 3",
            primaryActionButtonText = if (hasDifference) "근거 확인하기" else "송금으로 이어가기",
            primaryActionTarget = if (hasDifference) WageActionTarget.WORKPROOF else WageActionTarget.TRANSFER,
            secondaryActions = listOf(
                WageQuickActionUiModel(label = "Proof Pack", target = WageActionTarget.WORKPROOF),
                WageQuickActionUiModel(label = "근거 자료", target = WageActionTarget.MENU),
                WageQuickActionUiModel(label = "송금", target = WageActionTarget.TRANSFER)
            ),
            secondaryActionsToggleText = "다른 액션 보기",
            primaryHintText = "현재 상태에 맞는 다음 단계만 먼저 열어둘 수 있어요.",
            disclaimerText = "이 안내는 참고용이며 최종 판단은 계약서, 급여명세서, 회사 규정을 함께 확인해야 합니다."
        )
    )
}
