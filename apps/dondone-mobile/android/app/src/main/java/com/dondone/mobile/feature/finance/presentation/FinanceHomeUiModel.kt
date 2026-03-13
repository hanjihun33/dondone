package com.dondone.mobile.feature.finance.presentation

import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.domain.calculator.AdvanceCalculator
import com.dondone.mobile.domain.calculator.VaultCalculator
import com.dondone.mobile.domain.calculator.WageEstimator
import com.dondone.mobile.domain.calculator.WorkproofCalculator
import com.dondone.mobile.domain.model.DemoState
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

data class FinanceAccountUiModel(
    val balanceText: String,
    val sendableAmountText: String,
    val selectedAccountText: String
)

enum class FinanceAdvanceCalendarTone {
    DEFAULT,
    COMPLETE,
    MODIFIED,
    TODAY
}

data class FinanceAmountOptionUiModel(
    val label: String,
    val selected: Boolean
)

data class FinanceAdvanceHistoryUiModel(
    val title: String,
    val metaText: String,
    val valueText: String
)

data class FinanceAdvanceCalendarDayUiModel(
    val day: Int,
    val tone: FinanceAdvanceCalendarTone
)

data class FinanceAdvanceDetailUiModel(
    val subtitleText: String,
    val availableText: String,
    val usedText: String,
    val repaymentDueText: String,
    val calendarSummaryText: String,
    val updatedAtText: String,
    val reflectedCountText: String,
    val reviewCountText: String,
    val unreflectedCountText: String,
    val progressHintText: String,
    val requestAmountText: String,
    val receiveAmountText: String,
    val feeText: String,
    val tierText: String,
    val amountOptions: List<FinanceAmountOptionUiModel>,
    val historyItems: List<FinanceAdvanceHistoryUiModel>,
    val calendarDays: List<FinanceAdvanceCalendarDayUiModel>
)

data class FinanceAdvanceUiModel(
    val availableText: String,
    val repaymentDueText: String,
    val statusText: String,
    val progress: Float,
    val progressHintText: String,
    val detail: FinanceAdvanceDetailUiModel
)

data class FinanceVaultDetailUiModel(
    val subtitleText: String,
    val isActive: Boolean,
    val availableText: String,
    val balanceText: String,
    val aprText: String,
    val accruedInterestText: String,
    val monthlyInterestText: String,
    val dailyInterestText: String,
    val defiMonthlyText: String,
    val feeShareText: String,
    val totalMonthlyText: String,
    val defiRatioText: String,
    val advanceRatioText: String,
    val advanceUsageText: String,
    val noteText: String,
    val actionText: String,
    val amountOptions: List<FinanceAmountOptionUiModel>
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
    val actionText: String,
    val detail: FinanceVaultDetailUiModel
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
    val verifiedSnapshot = WorkproofCalculator.verify(this)
    val visibleRecords = WorkproofCalculator.visibleRecords(this)
    val vaultSnapshot = VaultCalculator.calculate(this)
    val wageEstimate = WageEstimator.calculate(this)
    val livingCost = (wage.actualDeposit * 0.655).toInt()
    val familySend = 260_000
    val saveAmount = (wage.actualDeposit - livingCost - familySend).coerceAtLeast(0)
    val isRecorded = wage.actualDepositRecordedDay != null
    val formattedMonth = demo.month.toString().padStart(2, '0')
    val repaymentDueText = "${demo.year}-$formattedMonth-${demo.monthLength.toString().padStart(2, '0')}"
    val lastReflectedDay = visibleRecords.maxOfOrNull { it.day } ?: demo.asOfDay
    val unreflectedDays = max(0, wage.workDays - advanceSnapshot.verifiedDays)
    val recordedDays = visibleRecords.associateBy { it.day }
    val advanceProgressHintText = if (advanceSnapshot.nextTierInDays > 0) {
        "다음 구간까지 ${advanceSnapshot.nextTierInDays}일 · 예상 증가 ${formatKrw(advanceSnapshot.nextTierGain)}"
    } else {
        "이번 달 최고 구간에 도달했어요."
    }

    val advanceAmountOptions = buildAmountOptions(
        available = advanceSnapshot.available,
        selected = advanceSnapshot.requestAmount,
        presets = listOf(50_000, 100_000, 150_000, 200_000)
    )

    val vaultAvailable = selectedAccount.balance
    val vaultAmountPresets = listOf(100_000, 300_000, 500_000, 1_000_000)
    val vaultSelectedAmount = if (vault.userDeposit > 0) {
        vault.userDeposit
    } else {
        pickClosestAmountOption(
            target = vaultSnapshot.suggestedDeposit.coerceAtMost(vaultAvailable),
            presets = vaultAmountPresets,
            available = vaultAvailable
        )
    }
    val vaultAmountOptions = buildAmountOptions(
        available = vaultAvailable,
        selected = vaultSelectedAmount,
        presets = vaultAmountPresets,
        labelFormatter = ::formatTenThousandUnit
    )
    val userDepositForBreakdown = max(1, if (vault.userDeposit > 0) vault.userDeposit else vaultSelectedAmount)
    val defiMonthly = floor(userDepositForBreakdown * (vault.apr / 12)).toInt()
    val feeShare = floor(vault.monthlyFeeRevenue * (userDepositForBreakdown / max(1, vault.totalPool).toDouble())).toInt()

    return FinanceHomeUiModel(
        account = FinanceAccountUiModel(
            balanceText = formatKrw(selectedAccount.balance),
            sendableAmountText = formatKrw(remittance.draftAmountUsd * 1_450),
            selectedAccountText = "${selectedAccount.name} · ${selectedAccount.number}"
        ),
        advance = FinanceAdvanceUiModel(
            availableText = formatKrw(advanceSnapshot.available),
            repaymentDueText = repaymentDueText,
            statusText = "근무 반영 ${advanceSnapshot.verifiedDays}일 · 확인 필요 ${workproof.audit.size}건",
            progress = if (advanceSnapshot.progressTargetDays == 0) {
                1f
            } else {
                advanceSnapshot.verifiedDays / advanceSnapshot.progressTargetDays.toFloat()
            },
            progressHintText = advanceProgressHintText,
            detail = FinanceAdvanceDetailUiModel(
                subtitleText = "근무 기록 기반 한도로 급여일 전에 일부를 먼저 받습니다.",
                availableText = formatKrw(advanceSnapshot.available),
                usedText = formatKrw(advanceSnapshot.used),
                repaymentDueText = repaymentDueText,
                calendarSummaryText = "${advanceSnapshot.verifiedDays}일 · ${formatHours(verifiedSnapshot.verifiedHours)} · ${formatKrw(verifiedSnapshot.verifiedAmount)}",
                updatedAtText = "${demo.year}-$formattedMonth-${lastReflectedDay.toString().padStart(2, '0')}",
                reflectedCountText = "${advanceSnapshot.verifiedDays}일",
                reviewCountText = "${workproof.audit.size}건",
                unreflectedCountText = "${unreflectedDays}일",
                progressHintText = if (advanceSnapshot.nextTierInDays > 0) {
                    "다음 구간까지 ${advanceSnapshot.nextTierInDays}일 남았고, 구간이 열리면 ${formatKrw(advanceSnapshot.nextTierGain)} 더 받을 수 있어요."
                } else {
                    "이번 달은 최고 구간까지 열려 있어요."
                },
                requestAmountText = formatKrw(advanceSnapshot.requestAmount),
                receiveAmountText = formatKrw(advanceSnapshot.receiveAmount),
                feeText = formatKrw(advanceSnapshot.fee),
                tierText = advanceSnapshot.tierName,
                amountOptions = advanceAmountOptions,
                historyItems = buildAdvanceHistoryItems(
                    month = demo.month,
                    repaymentDueText = repaymentDueText,
                    used = advanceSnapshot.used,
                    previousRepaymentGood = advance.previousRepaymentGood,
                    bonusLimit = advance.bonusLimit
                ),
                calendarDays = (1..demo.monthLength).map { day ->
                    val record = recordedDays[day]
                    val tone = when {
                        day == demo.asOfDay -> FinanceAdvanceCalendarTone.TODAY
                        record?.modified == true -> FinanceAdvanceCalendarTone.MODIFIED
                        record != null && record.outTime != "-" -> FinanceAdvanceCalendarTone.COMPLETE
                        else -> FinanceAdvanceCalendarTone.DEFAULT
                    }
                    FinanceAdvanceCalendarDayUiModel(day = day, tone = tone)
                }
            )
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
                formatKrw(abs(wageEstimate.difference))
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
                "예치 시작 전 예상 이자와 수익 구성을 먼저 확인할 수 있어요."
            },
            actionText = if (vault.enabled && vault.userDeposit > 0) "보관 보기" else "신청",
            detail = FinanceVaultDetailUiModel(
                subtitleText = "스테이블 코인 예치 기준 예상 이자 현황입니다.",
                isActive = vault.enabled && vault.userDeposit > 0,
                availableText = formatKrw(vaultAvailable),
                balanceText = if (vault.userDeposit > 0) formatKrw(vault.userDeposit) else formatKrw(vaultSelectedAmount),
                aprText = String.format(Locale.US, "%.1f%%", vault.apr * 100),
                accruedInterestText = formatKrw(vault.accruedInterest),
                monthlyInterestText = formatKrw(vaultSnapshot.monthlyInterest),
                dailyInterestText = formatKrw(vaultSnapshot.dailyInterest),
                defiMonthlyText = formatKrw(defiMonthly),
                feeShareText = formatKrw(feeShare),
                totalMonthlyText = formatKrw(defiMonthly + feeShare),
                defiRatioText = "${((1 - vault.advanceRatio) * 100).toInt()}%",
                advanceRatioText = "${(vault.advanceRatio * 100).toInt()}%",
                advanceUsageText = "${(vault.advanceUtilization * 100).toInt()}%",
                noteText = if (vault.enabled && vault.userDeposit > 0) {
                    "이자 수치는 데모용 추정값이며 실제 수익을 보장하지 않습니다."
                } else {
                    "예치금은 Vault 풀에 들어가며, DeFi 운용 수익 + 가불 수수료 기여분으로 이자가 적립됩니다."
                },
                actionText = if (vault.enabled && vault.userDeposit > 0) "닫기" else "예치 시작하기",
                amountOptions = vaultAmountOptions
            )
        )
    )
}

private fun buildAmountOptions(
    available: Int,
    selected: Int,
    presets: List<Int>,
    labelFormatter: (Int) -> String = ::formatKrw
): List<FinanceAmountOptionUiModel> {
    if (available <= 0) return emptyList()

    val rawOptions = linkedSetOf<Int>()
    if (selected in 1..available) {
        rawOptions += selected
    }
    presets.forEach { amount ->
        if (amount in 1..available) {
            rawOptions += amount
        }
    }
    rawOptions += available

    return rawOptions
        .sorted()
        .takeLast(4)
        .map { amount ->
            FinanceAmountOptionUiModel(
                label = labelFormatter(amount),
                selected = amount == selected
            )
        }
}

private fun pickClosestAmountOption(
    target: Int,
    presets: List<Int>,
    available: Int
): Int {
    val candidates = presets.filter { it in 1..available }
    if (candidates.isEmpty()) return available
    return candidates.minByOrNull { abs(it - target) } ?: candidates.first()
}

private fun buildAdvanceHistoryItems(
    month: Int,
    repaymentDueText: String,
    used: Int,
    previousRepaymentGood: Boolean,
    bonusLimit: Int
): List<FinanceAdvanceHistoryUiModel> {
    val items = mutableListOf<FinanceAdvanceHistoryUiModel>()
    if (used > 0) {
        items += FinanceAdvanceHistoryUiModel(
            title = "${month}월 1차 미리받기",
            metaText = "상환 예정 $repaymentDueText",
            valueText = formatKrw(used)
        )
    }
    if (previousRepaymentGood) {
        items += FinanceAdvanceHistoryUiModel(
            title = "이전 회차 상환",
            metaText = "보너스 한도 ${formatKrw(bonusLimit)} 반영",
            valueText = "정상 상환"
        )
    }
    if (items.isEmpty()) {
        items += FinanceAdvanceHistoryUiModel(
            title = "이번 달 미리받기 이력 없음",
            metaText = "근무 반영이 쌓이면 바로 신청할 수 있어요.",
            valueText = "-"
        )
    }
    return items
}

private fun formatHours(hours: Double): String {
    val wholeHours = hours.toInt()
    return if (hours == wholeHours.toDouble()) {
        "${wholeHours}h"
    } else {
        String.format(Locale.US, "%.1fh", hours)
    }
}

private fun formatTenThousandUnit(amount: Int): String {
    val man = amount / 10_000
    return "${man}만"
}
