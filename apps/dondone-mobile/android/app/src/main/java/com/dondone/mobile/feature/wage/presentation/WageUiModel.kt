package com.dondone.mobile.feature.wage.presentation

import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.ui.formatAsOfLabel
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.domain.calculator.WageEstimator
import com.dondone.mobile.domain.model.DemoState
import kotlin.math.abs

enum class WageDepositPhase {
    RECORDED,
    UPCOMING,
    DUE,
    OVERDUE
}

data class WageDepositUiModel(
    val phase: WageDepositPhase,
    val statusText: String,
    val statusTone: BadgeTone,
    val helperText: String,
    val actualDepositText: String,
    val phaseTitleText: String,
    val phaseDescriptionText: String,
    val phaseMetaText: String,
    val actionText: String?
)

data class WageDifferenceStepUiModel(
    val step: String,
    val label: String,
    val status: String
)

data class WageDifferenceUiModel(
    val title: String,
    val locked: Boolean,
    val statusText: String,
    val statusTone: BadgeTone,
    val descriptionText: String,
    val estimatedText: String,
    val actualText: String,
    val differenceText: String,
    val evidenceLines: List<String>,
    val steps: List<WageDifferenceStepUiModel>,
    val nextActionText: String
)

data class WageUiModel(
    val chips: List<String>,
    val deposit: WageDepositUiModel,
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
    val difference: WageDifferenceUiModel
)

fun DemoState.toWageUiModel(): WageUiModel {
    val estimate = WageEstimator.calculate(this)
    val isRecorded = wage.actualDepositRecordedDay != null
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
    }

    return WageUiModel(
        chips = listOf("왜 차액이 생겼어?", "어떤 근거야?", "다음 행동은?"),
        deposit = WageDepositUiModel(
            phase = depositPhase,
            statusText = if (isRecorded) "입금 입력 완료" else "입금 입력 전",
            statusTone = if (isRecorded) BadgeTone.Info else BadgeTone.Warning,
            helperText = if (isRecorded) {
                "입금값을 조정하면 아래 차이 확인과 추천 경로가 함께 갱신됩니다."
            } else {
                "입금 전에는 차이 확인과 다음 행동이 잠겨 있어요."
            },
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
        difference = WageDifferenceUiModel(
            title = if (isRecorded) "확인 필요한 차이" else "차이 확인이 열릴 준비 중",
            locked = !isRecorded,
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
            evidenceLines = listOf(
                "연장 ${wage.overtimeHours}h · 야간 ${wage.nightHours}h 포함",
                "수정 기록 ${workproof.audit.size}건(사유/첨부 포함)",
                "근거 자료 ID ${wage.workDays}건 연결"
            ),
            steps = listOf(
                WageDifferenceStepUiModel("1", "Proof Pack", "완료"),
                WageDifferenceStepUiModel("2", "근거 자료 묶음", "준비됨"),
                WageDifferenceStepUiModel("3", "신고 준비", "준비됨")
            ),
            nextActionText = "송금 흐름으로 이어가며 테스트넷 영수증까지 확인해볼까요?"
        )
    )
}
