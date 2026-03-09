package com.dondone.mobile.feature.wage.presentation

import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.domain.calculator.WageEstimator
import com.dondone.mobile.domain.model.DemoState
import kotlin.math.abs

data class WageDepositUiModel(
    val recorded: Boolean,
    val statusText: String,
    val statusTone: BadgeTone,
    val headerText: String,
    val helperText: String,
    val recordedDateText: String,
    val actualDepositText: String
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
    val workDaysText: String,
    val totalHoursText: String,
    val baseText: String,
    val overtimePremiumText: String,
    val nightPremiumText: String,
    val estimatedTotalText: String,
    val difference: WageDifferenceUiModel
)

fun DemoState.toWageUiModel(): WageUiModel {
    val estimate = WageEstimator.calculate(this)
    val isRecorded = wage.actualDepositRecordedDay != null

    return WageUiModel(
        chips = listOf("왜 차액이 생겼어?", "어떤 근거야?", "다음 행동은?"),
        deposit = WageDepositUiModel(
            recorded = isRecorded,
            statusText = if (isRecorded) "입금 입력 완료" else "입금 입력 전",
            statusTone = if (isRecorded) BadgeTone.Info else BadgeTone.Warning,
            headerText = if (isRecorded) "이번 달 실입금이 반영됐어요" else "먼저 실입금을 입력해 주세요",
            helperText = if (isRecorded) {
                "입금값을 조정하면 아래 차이 확인과 추천 경로가 함께 갱신됩니다."
            } else {
                "입금 전에는 차이 확인과 다음 행동이 잠겨 있어요."
            },
            recordedDateText = if (isRecorded) {
                "입금 입력 완료 · 2026-03-${wage.actualDepositRecordedDay.toString().padStart(2, '0')}"
            } else {
                "입금 전에는 차이 확인과 다음 행동이 잠겨 있어요."
            },
            actualDepositText = formatKrw(wage.actualDeposit)
        ),
        summaryMonthText = "${demo.year}-${demo.month.toString().padStart(2, '0')}",
        summaryHelperText = "먼저 실입금을 입력한 뒤 예상과 비교해 보세요.",
        auditSummaryText = "수정 기록 ${workproof.audit.size}건 · 근거 준비 완료",
        workDaysText = "${wage.workDays}일",
        totalHoursText = "${wage.totalHours}시간",
        baseText = formatKrw(wage.hourly * wage.totalHours),
        overtimePremiumText = formatKrw(estimate.overtimePremium),
        nightPremiumText = formatKrw(estimate.nightPremium),
        estimatedTotalText = formatKrw(estimate.total),
        difference = WageDifferenceUiModel(
            title = if (isRecorded) "확인 필요한 차이" else "차이 확인이 열릴 준비 중",
            locked = !isRecorded,
            statusText = if (estimate.difference == 0) "차이 없음" else "확인 필요한 차이",
            statusTone = if (estimate.difference == 0) BadgeTone.Success else BadgeTone.Warning,
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
