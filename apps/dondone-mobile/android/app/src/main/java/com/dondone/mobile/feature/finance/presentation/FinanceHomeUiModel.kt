package com.dondone.mobile.feature.finance.presentation

import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.domain.calculator.AdvanceCalculator
import com.dondone.mobile.domain.calculator.WageEstimator
import com.dondone.mobile.domain.model.DemoState
import java.util.Locale

data class FinanceAccountUiModel(
    val balanceText: String,
    val sendableAmountText: String,
    val selectedAccountText: String,
    val hintText: String
)

data class FinanceAdvanceUiModel(
    val availableText: String,
    val repaymentDueText: String,
    val statusText: String,
    val progress: Float,
    val progressHintText: String
)

data class FinanceWageUiModel(
    val statusText: String,
    val statusTone: BadgeTone,
    val differenceText: String,
    val estimatedText: String,
    val actualText: String,
    val hintText: String
)

data class MoneySplitUiModel(
    val livingCostText: String,
    val familySendText: String,
    val saveAmountText: String,
    val basisText: String
)

data class FinanceVaultUiModel(
    val depositStatusText: String,
    val accruedInterestText: String,
    val aprText: String,
    val helperText: String,
    val actionText: String
)

data class FinanceHomeUiModel(
    val account: FinanceAccountUiModel,
    val advance: FinanceAdvanceUiModel,
    val wage: FinanceWageUiModel,
    val moneySplit: MoneySplitUiModel?,
    val vault: FinanceVaultUiModel
)

fun DemoState.toFinanceHomeUiModel(): FinanceHomeUiModel {
    val selectedAccount = remittance.selectedAccount()
    val advanceSnapshot = AdvanceCalculator.calculate(this)
    val wageEstimate = WageEstimator.calculate(this)
    val livingCost = (wage.actualDeposit * 0.655).toInt()
    val familySend = 260_000
    val saveAmount = (wage.actualDeposit - livingCost - familySend).coerceAtLeast(0)
    val isRecorded = wage.actualDepositRecordedDay != null

    return FinanceHomeUiModel(
        account = FinanceAccountUiModel(
            balanceText = formatKrw(selectedAccount.balance),
            sendableAmountText = formatKrw(remittance.draftAmountUsd * 1_450),
            selectedAccountText = "${selectedAccount.name} · ${selectedAccount.number}",
            hintText = "계좌를 먼저 확인한 뒤 송금을 시작해볼까요?"
        ),
        advance = FinanceAdvanceUiModel(
            availableText = formatKrw(advanceSnapshot.available),
            repaymentDueText = "${demo.year}-${demo.month.toString().padStart(2, '0')}-${demo.monthLength.toString().padStart(2, '0')}",
            statusText = "근무 반영 ${advanceSnapshot.verifiedDays}일 · 확인 필요 ${workproof.audit.size}건",
            progress = if (advanceSnapshot.progressTargetDays == 0) 1f else advanceSnapshot.verifiedDays / advanceSnapshot.progressTargetDays.toFloat(),
            progressHintText = if (advanceSnapshot.nextTierInDays > 0) {
                "다음 구간까지 ${advanceSnapshot.nextTierInDays}일 · 예상 증가 ${formatKrw(advanceSnapshot.nextTierGain)}"
            } else {
                "이번 달 최고 구간에 도달했어요."
            }
        ),
        wage = FinanceWageUiModel(
            statusText = if (!isRecorded) {
                "입금 전"
            } else if (wageEstimate.difference == 0) {
                "차이 없음"
            } else {
                "확인 필요한 차이"
            },
            statusTone = if (!isRecorded) {
                BadgeTone.Info
            } else if (wageEstimate.difference == 0) {
                BadgeTone.Success
            } else {
                BadgeTone.Warning
            },
            differenceText = if (isRecorded) {
                formatKrw(kotlin.math.abs(wageEstimate.difference))
            } else {
                "확인 전"
            },
            estimatedText = formatKrw(wageEstimate.total),
            actualText = if (isRecorded) formatKrw(wage.actualDeposit) else "미입력",
            hintText = if (isRecorded) {
                "실입금이 반영돼 있어요. 차이와 근거를 바로 확인할 수 있습니다."
            } else {
                "실입금을 입력하기 전에는 차이 대신 준비 상태만 보여줍니다."
            }
        ),
        moneySplit = if (isRecorded) {
            MoneySplitUiModel(
                livingCostText = formatKrw(livingCost),
                familySendText = formatKrw(familySend),
                saveAmountText = formatKrw(saveAmount),
                basisText = "실입금 반영"
            )
        } else {
            null
        },
        vault = FinanceVaultUiModel(
            depositStatusText = if (vault.enabled && vault.userDeposit > 0) {
                formatKrw(vault.userDeposit)
            } else {
                "미신청"
            },
            accruedInterestText = formatKrw(vault.accruedInterest),
            aprText = String.format(Locale.US, "%.1f%%", vault.apr * 100),
            helperText = if (vault.enabled && vault.userDeposit > 0) {
                "예치 중인 금액과 누적 이자를 확인할 수 있어요."
            } else {
                "예치 신청 후 이자 예상과 수익 구성을 확인할 수 있어요."
            },
            actionText = if (vault.enabled && vault.userDeposit > 0) "보관 보기" else "신청"
        )
    )
}
