package com.dondone.mobile.feature.wage.presentation

import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.ui.formatAsOfLabel
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.domain.calculator.WageEstimator
import com.dondone.mobile.domain.model.DemoState
import kotlin.math.abs

<<<<<<< HEAD
enum class WageActionTarget {
    TRANSFER,
    WORKPROOF,
    MENU
}

data class WageMetricItemUiModel(
    val label: String,
    val value: String,
    val emphasized: Boolean = false
)

data class WageQuickActionUiModel(
    val label: String,
    val target: WageActionTarget
)

=======
enum class WageDepositPhase {
    RECORDED,
    UPCOMING,
    DUE,
    OVERDUE
}

>>>>>>> develop
data class WageDepositUiModel(
    val phase: WageDepositPhase,
    val statusText: String,
    val statusTone: BadgeTone,
<<<<<<< HEAD
    val headerText: String,
    val descriptionText: String,
    val metaText: String,
    val actionButtonText: String?,
    val actualDepositText: String,
    val inputHelperText: String,
    val deductionBadgeText: String,
    val thresholdBadgeText: String
=======
    val helperText: String,
    val actualDepositText: String,
    val phaseTitleText: String,
    val phaseDescriptionText: String,
    val phaseMetaText: String,
    val actionText: String?
>>>>>>> develop
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
<<<<<<< HEAD
    val lockedTitle: String,
    val lockedDescription: String,
    val summaryItems: List<WageMetricItemUiModel>,
=======
    val descriptionText: String,
    val estimatedText: String,
    val actualText: String,
    val differenceText: String,
>>>>>>> develop
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

data class WageUiModel(
    val chips: List<String>,
    val heroTitleText: String,
    val heroDescriptionText: String,
    val disclaimerLines: List<String>,
    val monthText: String,
    val titleText: String,
    val descriptionText: String,
    val modifiedCountText: String,
    val evidenceBadgeText: String,
    val deposit: WageDepositUiModel,
<<<<<<< HEAD
    val overviewItems: List<WageMetricItemUiModel>,
    val estimateItems: List<WageMetricItemUiModel>,
=======
    val summaryMonthText: String,
    val summaryHelperText: String,
    val auditSummaryText: String,
    val modifiedCountText: String,
    val evidenceReadyText: String,
    val workDaysText: String,
    val totalHoursText: String,
    val overtimeNightText: String,
    val baseText: String,
    val overtimePremiumText: String,
    val nightPremiumText: String,
    val estimatedTotalText: String,
    val deductionHintText: String,
    val thresholdHintText: String,
>>>>>>> develop
    val difference: WageDifferenceUiModel
)

fun DemoState.toWageUiModel(): WageUiModel {
    val estimate = WageEstimator.calculate(this)
    val monthText = "${demo.year}-${demo.month.toString().padStart(2, '0')}"
    val recordedDayText = wage.actualDepositRecordedDay?.toString()?.padStart(2, '0')
    val isRecorded = wage.actualDepositRecordedDay != null
<<<<<<< HEAD
    val hasDifference = estimate.difference != 0
    val differenceAmountText = formatKrw(abs(estimate.difference))

    val diffTitle = when {
        !isRecorded -> "차이 확인이 열릴 준비 중"
        !hasDifference -> "차이 없음"
        else -> "확인 필요한 차이"
    }
    val diffDescription = when {
        !isRecorded -> "실제 입금액을 입력한 뒤 예상과 비교해 보세요."
        estimate.difference > 0 -> "예상 대비 실입금이 낮아요. 근거와 함께 다음 행동으로 연결할 수 있어요."
        estimate.difference < 0 -> "예상 대비 실입금이 높아요. 근거와 함께 다음 행동으로 연결할 수 있어요."
        else -> "예상과 실입금 차이가 크지 않아 바로 다음 흐름으로 이어갈 수 있어요."
=======
    val depositPhase = when {
        isRecorded -> WageDepositPhase.RECORDED
        demo.asOfDay < wage.paydayDay -> WageDepositPhase.UPCOMING
        demo.asOfDay == wage.paydayDay -> WageDepositPhase.DUE
        else -> WageDepositPhase.OVERDUE
    }
    val phaseTitleText = when (depositPhase) {
        WageDepositPhase.RECORDED -> "이번 달 실입금이 반영됐어요"
        WageDepositPhase.UPCOMING -> "실제 입금액 입력을 준비해 주세요"
        WageDepositPhase.DUE -> "오늘 들어온 실입금을 확인해 주세요"
        WageDepositPhase.OVERDUE -> "실입금이 아직 입력되지 않았어요"
    }
    val phaseDescriptionText = when (depositPhase) {
        WageDepositPhase.RECORDED -> "이제 차액 확인과 다음 행동, 돈 나누기 계획까지 바로 이어서 볼 수 있어요."
        WageDepositPhase.UPCOMING -> "급여일이 오면 바로 입력할 수 있게 준비해 두면 이번 달 돈 상태를 더 정확히 볼 수 있어요."
        WageDepositPhase.DUE -> "실제 입금액을 기록하면 급여 점검, 송금 계획, 보관 계획이 함께 더 정확해집니다."
        WageDepositPhase.OVERDUE -> "실입금을 입력해야 차액과 추천 경로를 정확하게 열 수 있어요."
    }
    val phaseMetaText = when (depositPhase) {
        WageDepositPhase.RECORDED -> "입금 입력 완료 · 2026-03-${wage.actualDepositRecordedDay.toString().padStart(2, '0')}"
        WageDepositPhase.UPCOMING -> "${demo.formatAsOfLabel()} · 급여일까지 ${wage.paydayDay - demo.asOfDay}일"
        WageDepositPhase.DUE -> "${demo.formatAsOfLabel()} · 급여일 오늘"
        WageDepositPhase.OVERDUE -> "${demo.formatAsOfLabel()} · 급여일 이후 ${demo.asOfDay - wage.paydayDay}일"
>>>>>>> develop
    }

    return WageUiModel(
        chips = listOf("왜 차액이 생겼어?", "어떤 근거야?", "다음 행동은?"),
        heroTitleText = "근거와 차이를 한 번에 점검",
        heroDescriptionText = "실입금 입력부터 근거 확인, 다음 행동까지 급여점검 흐름을 한 화면에서 이어가세요.",
        disclaimerLines = listOf(
            "이 급여 계산은 참고용 추정입니다. 실제 지급과 공제는 근로계약, 급여명세서, 회사 규정에 따라 달라질 수 있어요.",
            "공제 항목은 아직 반영되지 않아 실제 수령액과 차이가 있을 수 있습니다."
        ),
        monthText = monthText,
        titleText = "이번 달 급여 확인",
        descriptionText = "먼저 실입금을 입력한 뒤 예상과 비교해 보세요.",
        modifiedCountText = "수정 ${workproof.audit.size}",
        evidenceBadgeText = "근거 준비",
        deposit = WageDepositUiModel(
            phase = depositPhase,
            statusText = if (isRecorded) "입금 입력 완료" else "입금 입력 전",
            statusTone = if (isRecorded) BadgeTone.Info else BadgeTone.Warning,
<<<<<<< HEAD
            headerText = if (isRecorded) "이번 달 실입금이 반영됐어요" else "실제 입금액을 먼저 확인해 주세요",
            descriptionText = if (isRecorded) {
=======
            helperText = if (isRecorded) {
>>>>>>> develop
                "입금값을 조정하면 아래 차이 확인과 추천 경로가 함께 갱신됩니다."
            } else {
                "입금 전에는 차이와 다음 행동이 잠겨 있고, 입력 후에 바로 이어집니다."
            },
<<<<<<< HEAD
            metaText = if (isRecorded) {
                "입금 입력 완료 · $monthText-$recordedDayText"
            } else {
                "급여일 ${wage.paydayDay}일 기준 입력 대기"
            },
            actionButtonText = if (isRecorded) null else "적용",
            actualDepositText = formatKrw(wage.actualDeposit),
            inputHelperText = if (isRecorded) {
                "입금액을 조정하면 예상과의 차이가 바로 갱신됩니다."
            } else {
                "입금 전에는 차이와 다음 행동이 잠겨 있고, 입력 후에 바로 이어집니다."
            },
            deductionBadgeText = if (wage.deductionsKnown) "공제 반영" else "공제 미반영",
            thresholdBadgeText = "임계값 완화 적용"
        ),
        overviewItems = listOf(
            WageMetricItemUiModel(label = "근무일", value = "${wage.workDays}일"),
            WageMetricItemUiModel(label = "총 시간", value = "${wage.totalHours}시간"),
            WageMetricItemUiModel(label = "연장/야간", value = "${wage.overtimeHours}h/${wage.nightHours}h")
        ),
        estimateItems = listOf(
            WageMetricItemUiModel(label = "기본", value = formatKrw(estimate.base)),
            WageMetricItemUiModel(label = "연장 프리미엄", value = formatKrw(estimate.overtimePremium)),
            WageMetricItemUiModel(label = "야간 프리미엄", value = formatKrw(estimate.nightPremium)),
            WageMetricItemUiModel(label = "추정 합계", value = formatKrw(estimate.total), emphasized = true)
        ),
=======
            actualDepositText = formatKrw(wage.actualDeposit),
            phaseTitleText = phaseTitleText,
            phaseDescriptionText = phaseDescriptionText,
            phaseMetaText = phaseMetaText,
            actionText = if (isRecorded) null else "실입금 반영"
        ),
        summaryMonthText = "${demo.year}-${demo.month.toString().padStart(2, '0')}",
        summaryHelperText = "먼저 실입금을 입력한 뒤 예상과 비교해 보세요.",
        auditSummaryText = "수정 기록 ${workproof.audit.size}건 · 근거 준비 완료",
        modifiedCountText = "수정 ${workproof.audit.size}건",
        evidenceReadyText = "근거 준비",
        workDaysText = "${wage.workDays}일",
        totalHoursText = "${wage.totalHours}시간",
        overtimeNightText = "연장 ${wage.overtimeHours}h · 야간 ${wage.nightHours}h",
        baseText = formatKrw(wage.hourly * wage.totalHours),
        overtimePremiumText = formatKrw(estimate.overtimePremium),
        nightPremiumText = formatKrw(estimate.nightPremium),
        estimatedTotalText = formatKrw(estimate.total),
        deductionHintText = if (wage.deductionsKnown) "공제 반영" else "공제 미반영",
        thresholdHintText = "임계값 완화 적용",
>>>>>>> develop
        difference = WageDifferenceUiModel(
            title = diffTitle,
            descriptionText = diffDescription,
            locked = !isRecorded,
<<<<<<< HEAD
            statusText = if (!isRecorded) "입금 대기" else if (hasDifference) "확인 필요한 차이" else "차이 없음",
            statusTone = when {
                !isRecorded -> BadgeTone.Warning
                hasDifference -> BadgeTone.Warning
                else -> BadgeTone.Success
            },
            lockedTitle = "실입금을 입력하면 다음 행동이 열려요",
            lockedDescription = "입금액을 입력한 뒤 차이 확인, 문서 생성, 신고 준비를 이어서 진행할 수 있어요.",
            summaryItems = listOf(
                WageMetricItemUiModel(label = "추정", value = formatKrw(estimate.total)),
                WageMetricItemUiModel(label = "실제", value = formatKrw(wage.actualDeposit)),
                WageMetricItemUiModel(label = "차액", value = differenceAmountText, emphasized = true)
            ),
=======
            statusText = if (estimate.difference == 0) "차이 없음" else "확인 필요한 차이",
            statusTone = if (estimate.difference == 0) BadgeTone.Success else BadgeTone.Warning,
            descriptionText = if (!isRecorded) {
                "실입금을 입력하면 차액과 근거를 함께 확인할 수 있어요."
            } else if (estimate.difference == 0) {
                "현재 입력 기준으로는 뚜렷한 차이가 보이지 않습니다."
            } else {
                "예상과 실제 입금액 사이에 차이가 보여요. 근거 확인부터 시작해 보세요."
            },
            estimatedText = formatKrw(estimate.total),
            actualText = formatKrw(wage.actualDeposit),
            differenceText = "차액 ${formatKrw(abs(estimate.difference))}",
>>>>>>> develop
            evidenceLines = listOf(
                "연장 ${wage.overtimeHours}h · 야간 ${wage.nightHours}h 포함",
                "수정 기록 ${workproof.audit.size}건(사유/첨부 포함)",
                "근거 자료 ID ${wage.workDays}건 연결"
            ),
            steps = listOf(
                WageDifferenceStepUiModel("1", "Proof Pack", if (workproof.records.isNotEmpty()) "준비됨" else "대기"),
                WageDifferenceStepUiModel("2", "근거 자료 묶음", if (documents.isNotEmpty()) "준비됨" else "대기"),
                WageDifferenceStepUiModel("3", "신고 준비", if (documents.isNotEmpty()) "준비됨" else "대기")
            ),
            primaryActionDescription = if (hasDifference) {
                "수정 기록과 근무 근거를 먼저 확인한 뒤 문서 정리나 송금 흐름으로 이어갈 수 있어요."
            } else {
                "차이가 크지 않아요. 테스트넷 송금 흐름과 영수증 확인까지 바로 이어가세요."
            },
            primaryActionStepText = if (hasDifference) "단계 1" else "단계 3",
            primaryActionButtonText = if (hasDifference) "근거 확인하기" else "송금으로 이어가기",
            primaryActionTarget = if (hasDifference) WageActionTarget.WORKPROOF else WageActionTarget.TRANSFER,
            secondaryActions = listOf(
                WageQuickActionUiModel(label = "Proof", target = WageActionTarget.WORKPROOF),
                WageQuickActionUiModel(label = "문서", target = WageActionTarget.MENU),
                WageQuickActionUiModel(label = "송금", target = WageActionTarget.TRANSFER)
            ),
            secondaryActionsToggleText = "다른 액션 보기",
            primaryHintText = "완료된 단계는 아래에서 다시 열 수 있어요.",
            disclaimerText = "이 안내는 참고용이며 법률 자문이 아닙니다. 필요하면 전문기관 상담을 이용해 주세요."
        )
    )
}
