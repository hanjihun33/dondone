package com.dondone.mobile.feature.finance.presentation

import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.domain.calculator.AdvanceCalculator
import com.dondone.mobile.domain.calculator.VaultCalculator
import com.dondone.mobile.domain.calculator.WageEstimator
import com.dondone.mobile.domain.model.DemoState

data class FinanceAccountUiModel(
    val balanceText: String,
    val sendableAmountText: String,
    val selectedAccountText: String,
    val hintText: String
)

data class FinanceAdvanceUiModel(
    val availableText: String,
    val feeText: String,
    val progress: Float,
    val progressHintText: String
)

data class FinanceWageUiModel(
    val statusText: String,
    val statusTone: BadgeTone,
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
    val suggestedDepositText: String,
    val monthlyInterestText: String,
    val dailyInterestText: String
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
    val vaultSnapshot = VaultCalculator.calculate(this)
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
            feeText = formatKrw(advanceSnapshot.fee),
            progress = if (advanceSnapshot.progressTargetDays == 0) 1f else advanceSnapshot.verifiedDays / advanceSnapshot.progressTargetDays.toFloat(),
            progressHintText = if (advanceSnapshot.nextTierInDays > 0) {
                "다음 구간까지 ${advanceSnapshot.nextTierInDays}일 · 예상 증가 ${formatKrw(advanceSnapshot.nextTierGain)}"
            } else {
                "이번 달 최고 구간에 도달했어요."
            }
        ),
        wage = FinanceWageUiModel(
            statusText = if (wageEstimate.difference == 0) "차이 없음" else "확인 필요한 차이",
            statusTone = if (wageEstimate.difference == 0) BadgeTone.Success else BadgeTone.Warning,
            estimatedText = formatKrw(wageEstimate.total),
            actualText = formatKrw(wage.actualDeposit),
            hintText = if (isRecorded) {
                "실입금이 반영돼 있어요. 차이와 근거를 바로 확인할 수 있습니다."
            } else {
                "실입금을 먼저 입력하면 차이와 다음 행동이 열립니다."
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
            suggestedDepositText = formatKrw(vaultSnapshot.suggestedDeposit),
            monthlyInterestText = formatKrw(vaultSnapshot.monthlyInterest),
            dailyInterestText = formatKrw(vaultSnapshot.dailyInterest)
        )
    )
}
