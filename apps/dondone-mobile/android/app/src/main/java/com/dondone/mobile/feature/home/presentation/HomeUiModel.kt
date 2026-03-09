package com.dondone.mobile.feature.home.presentation

import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.domain.calculator.AdvanceCalculator
import com.dondone.mobile.domain.calculator.WageEstimator
import com.dondone.mobile.domain.model.DemoState
import kotlin.math.abs

data class HomeAccountUiModel(
    val balanceText: String,
    val sendableAmountText: String,
    val selectedAccountText: String,
    val hintText: String
)

data class HomeWorkUiModel(
    val dateText: String,
    val statusText: String,
    val statusTone: BadgeTone,
    val canClockIn: Boolean,
    val canClockOut: Boolean,
    val clockSummary: String,
    val impactText: String,
    val advanceAvailableText: String,
    val advanceProgressText: String,
    val advanceProgress: Float,
    val advanceHintText: String
)

data class HomeMoneyUiModel(
    val statusText: String,
    val statusTone: BadgeTone,
    val summaryText: String,
    val estimatedText: String,
    val actualText: String,
    val differenceText: String,
    val hintText: String
)

data class HomeUiModel(
    val account: HomeAccountUiModel,
    val work: HomeWorkUiModel,
    val money: HomeMoneyUiModel
)

fun DemoState.toHomeUiModel(): HomeUiModel {
    val selectedAccount = remittance.selectedAccount()
    val wageEstimate = WageEstimator.calculate(this)
    val advance = AdvanceCalculator.calculate(this)
    val progress = if (advance.progressTargetDays == 0) 1f else advance.verifiedDays / advance.progressTargetDays.toFloat()
    val clockIn = workproof.today.clockIn ?: "-"
    val clockOut = workproof.today.clockOut ?: "-"
    val workStatusText = when {
        workproof.today.clockOut != null -> "완료"
        workproof.today.clockIn != null -> "출근만"
        else -> "준비됨"
    }
    val workStatusTone = when (workStatusText) {
        "완료" -> BadgeTone.Success
        "출근만" -> BadgeTone.Warning
        else -> BadgeTone.Info
    }
    val moneyStatusText = if (wageEstimate.difference == 0) "차이 없음" else "확인 필요한 차이"
    val moneyStatusTone = if (wageEstimate.difference == 0) BadgeTone.Success else BadgeTone.Warning

    return HomeUiModel(
        account = HomeAccountUiModel(
            balanceText = formatKrw(selectedAccount.balance),
            sendableAmountText = formatKrw(remittance.draftAmountUsd * 1_450),
            selectedAccountText = "${selectedAccount.name} · ${selectedAccount.number}",
            hintText = "계좌를 먼저 확인한 뒤 송금을 시작해볼까요?"
        ),
        work = HomeWorkUiModel(
            dateText = "${demo.year}-${demo.month.toString().padStart(2, '0')}-${demo.asOfDay.toString().padStart(2, '0')} · ${workproof.workplaceName}",
            statusText = workStatusText,
            statusTone = workStatusTone,
            canClockIn = workproof.today.clockIn == null,
            canClockOut = workproof.today.clockIn != null && workproof.today.clockOut == null,
            clockSummary = "출근 $clockIn · 퇴근 $clockOut",
            impactText = "오늘 기록을 남기면 미리받기 한도와 월급 확인 근거가 쌓여요.",
            advanceAvailableText = formatKrw(advance.available),
            advanceProgressText = "${advance.verifiedDays}일 / ${advance.progressTargetDays}일",
            advanceProgress = progress,
            advanceHintText = if (advance.nextTierInDays > 0) {
                "다음 구간까지 ${advance.nextTierInDays}일 · 예상 증가 ${formatKrw(advance.nextTierGain)}"
            } else {
                "이번 달 최고 구간에 도달했어요."
            }
        ),
        money = HomeMoneyUiModel(
            statusText = moneyStatusText,
            statusTone = moneyStatusTone,
            summaryText = "추정 ${formatKrw(wageEstimate.total)} · 실제 ${formatKrw(wage.actualDeposit)}",
            estimatedText = formatKrw(wageEstimate.total),
            actualText = formatKrw(wage.actualDeposit),
            differenceText = "차액 ${formatKrw(abs(wageEstimate.difference))}",
            hintText = if (wageEstimate.difference == 0) {
                "지금까지는 차이가 보이지 않아요."
            } else {
                "확인 필요한 차이가 보여요. 급여 점검에서 근거부터 확인해볼까요?"
            }
        )
    )
}
