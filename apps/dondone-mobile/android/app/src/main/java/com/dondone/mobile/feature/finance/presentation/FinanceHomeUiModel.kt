package com.dondone.mobile.feature.finance.presentation

import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.app.session.AdvanceRequestUiState
import com.dondone.mobile.app.session.AdvanceRequestDetailUiState
import com.dondone.mobile.data.advance.AdvanceRemoteMode
import com.dondone.mobile.data.advance.AdvanceRemoteState
import com.dondone.mobile.data.wage.WageRemoteState
import com.dondone.mobile.domain.calculator.AdvanceCalculator
import com.dondone.mobile.domain.calculator.VaultCalculator
import com.dondone.mobile.domain.calculator.WageEstimator
import com.dondone.mobile.domain.calculator.WorkproofCalculator
import com.dondone.mobile.domain.advance.AdvanceSurfaceState
import com.dondone.mobile.domain.advance.toAdvanceContractState
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
    val amount: Int,
    val label: String,
    val selected: Boolean
)

data class FinanceAdvanceHistoryUiModel(
    val requestId: Long?,
    val title: String,
    val metaText: String,
    val valueText: String,
    val clickable: Boolean
)

data class FinanceAdvanceRequestDetailUiModel(
    val isVisible: Boolean,
    val isLoading: Boolean,
    val titleText: String,
    val stateText: String,
    val requestedAmountText: String,
    val approvedAmountText: String,
    val feeAmountText: String,
    val repaymentDueText: String,
    val createdAtText: String,
    val snapshotAvailableText: String,
    val snapshotCapText: String,
    val snapshotReflectedText: String,
    val snapshotReviewText: String,
    val errorMessage: String?
)

data class FinanceAdvanceCalendarDayUiModel(
    val day: Int,
    val tone: FinanceAdvanceCalendarTone
)

data class FinanceAdvanceDetailUiModel(
    val surfaceState: AdvanceSurfaceState,
    val subtitleText: String,
    val stateTitleText: String,
    val stateBodyText: String,
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
    val blockReasonTexts: List<String>,
    val disclaimerText: String,
    val canRequest: Boolean,
    val requestButtonText: String,
    val requestFeedbackText: String?,
    val requestFeedbackIsError: Boolean,
    val amountOptions: List<FinanceAmountOptionUiModel>,
    val historyItems: List<FinanceAdvanceHistoryUiModel>,
    val calendarDays: List<FinanceAdvanceCalendarDayUiModel>
)

data class FinanceAdvanceUiModel(
    val surfaceState: AdvanceSurfaceState,
    val availableText: String,
    val repaymentDueText: String,
    val statusText: String,
    val sourceLabelText: String,
    val stateBodyText: String,
    val progress: Float,
    val progressHintText: String,
    val actionText: String,
    val detail: FinanceAdvanceDetailUiModel,
    val requestDetail: FinanceAdvanceRequestDetailUiModel
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

fun DemoState.toFinanceHomeUiModel(
    remoteState: AdvanceRemoteState? = null,
    wageRemoteState: WageRemoteState? = null,
    selectedAdvanceAmount: Int? = null,
    advanceRequestUiState: AdvanceRequestUiState = AdvanceRequestUiState(),
    advanceRequestDetailUiState: AdvanceRequestDetailUiState = AdvanceRequestDetailUiState()
): FinanceHomeUiModel {
    val selectedAccount = remittance.selectedAccount()
    val advanceSnapshot = AdvanceCalculator.calculate(this)
    val advanceContractState = toAdvanceContractState(remoteState)
    val verifiedSnapshot = WorkproofCalculator.verify(this)
    val visibleRecords = WorkproofCalculator.visibleRecords(this)
    val vaultSnapshot = VaultCalculator.calculate(this)
    val wageEstimate = WageEstimator.calculate(this)
    val remoteWageSummary = wageRemoteState?.payload?.summary
    val remoteWageEstimate = wageRemoteState?.payload?.estimate
    val effectiveActualDeposit = remoteWageSummary?.actualDepositAmount?.toInt() ?: wage.actualDeposit
    val effectiveEstimatedTotal = remoteWageEstimate?.estimatedTotal?.toInt() ?: wageEstimate.total
    val effectiveDifference = remoteWageSummary?.differenceAmount?.toInt() ?: wageEstimate.difference
    val livingCost = (effectiveActualDeposit * 0.655).toInt()
    val familySend = 260_000
    val saveAmount = (effectiveActualDeposit - livingCost - familySend).coerceAtLeast(0)
    val isRecorded = remoteWageSummary?.actualDepositAmount != null || wage.actualDepositRecordedDay != null
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
    val usesRemoteAdvance = remoteState != null
    val remoteMode = remoteState?.mode
    val remoteRequests = if (remoteMode == AdvanceRemoteMode.CONTENT) remoteState.requests else emptyList()
    val latestRemoteRequest = remoteRequests.maxByOrNull { it.requestId }
    val remoteUsedAmount = remoteRequests.sumOf { it.approvedAmount ?: 0L }
    val remoteAvailableAmount = if (usesRemoteAdvance) {
        advanceContractState.availableAmountOverride ?: 0L
    } else {
        advanceSnapshot.available.toLong()
    }
    val remoteRepaymentDueText = when {
        advanceContractState.repaymentDateOverride != null -> advanceContractState.repaymentDateOverride
        usesRemoteAdvance -> "실연동 확인 필요"
        else -> repaymentDueText
    }
    val advanceProgress = if (usesRemoteAdvance) {
        when (advanceContractState.surfaceState) {
            AdvanceSurfaceState.SUCCESS -> 1f
            AdvanceSurfaceState.BLOCKED -> 0.2f
            else -> 0f
        }
    } else if (advanceSnapshot.progressTargetDays == 0) {
        1f
    } else {
        advanceSnapshot.verifiedDays / advanceSnapshot.progressTargetDays.toFloat()
    }
    val effectiveProgressHintText = if (usesRemoteAdvance) {
        advanceContractState.stateBodyText
    } else {
        advanceProgressHintText
    }
    val effectiveHistoryItems = if (usesRemoteAdvance) {
        buildRemoteAdvanceHistoryItems(
            requests = remoteRequests,
            emptyMessage = when (advanceContractState.surfaceState) {
                AdvanceSurfaceState.LOADING -> "이력도 함께 불러오는 중이에요."
                AdvanceSurfaceState.EMPTY -> "이번 달 실연동 미리받기 이력이 없어요."
                AdvanceSurfaceState.ERROR -> "이력을 다시 불러온 뒤 확인해 주세요."
                AdvanceSurfaceState.BLOCKED, AdvanceSurfaceState.SUCCESS -> "이번 달 실연동 미리받기 이력이 없어요."
            }
        )
    } else {
        buildAdvanceHistoryItems(
            month = demo.month,
            repaymentDueText = repaymentDueText,
            used = advanceSnapshot.used,
            previousRepaymentGood = advance.previousRepaymentGood,
            bonusLimit = advance.bonusLimit
        )
    }
    val detailCalendarSummaryText = if (usesRemoteAdvance) {
        when (advanceContractState.surfaceState) {
            AdvanceSurfaceState.SUCCESS, AdvanceSurfaceState.BLOCKED ->
                "실연동 기준 한도와 신청 이력을 표시합니다."

            AdvanceSurfaceState.LOADING ->
                "실연동 근거를 불러오는 중이에요."

            AdvanceSurfaceState.EMPTY ->
                "실연동 계정에서는 아직 미리받기 근거가 열리지 않았어요."

            AdvanceSurfaceState.ERROR ->
                "실연동 근거를 다시 확인해야 해요."
        }
    } else {
        "${advanceSnapshot.verifiedDays}일 · ${formatHours(verifiedSnapshot.verifiedHours)} · ${formatKrw(verifiedSnapshot.verifiedAmount)}"
    }
    val detailUpdatedAtText = if (usesRemoteAdvance) {
        when (advanceContractState.surfaceState) {
            AdvanceSurfaceState.LOADING -> "실연동 확인 중"
            AdvanceSurfaceState.EMPTY -> "실연동 비어 있음"
            AdvanceSurfaceState.ERROR -> "실연동 재확인 필요"
            AdvanceSurfaceState.BLOCKED, AdvanceSurfaceState.SUCCESS -> "실연동 최신 기준"
        }
    } else {
        "${demo.year}-$formattedMonth-${lastReflectedDay.toString().padStart(2, '0')}"
    }
    val detailReflectedCountText = if (usesRemoteAdvance) "-" else "${advanceSnapshot.verifiedDays}일"
    val detailUnreflectedCountText = if (usesRemoteAdvance) "-" else "${unreflectedDays}일"
    val detailRequestAmountText = when {
        latestRemoteRequest != null -> formatKrw(latestRemoteRequest.requestedAmount.toInt())
        usesRemoteAdvance -> "-"
        else -> formatKrw(advanceSnapshot.requestAmount)
    }
    val detailReceiveAmountText = when {
        latestRemoteRequest != null -> latestRemoteRequest.approvedAmount?.toInt()?.let(::formatKrw) ?: "승인 대기"
        usesRemoteAdvance -> "-"
        else -> formatKrw(advanceSnapshot.receiveAmount)
    }
    val detailFeeText = when {
        latestRemoteRequest != null ->
            latestRemoteRequest.approvedAmount?.let { approvedAmount ->
                formatKrw((latestRemoteRequest.requestedAmount - approvedAmount).toInt())
            } ?: "-"

        usesRemoteAdvance -> "-"
        else -> formatKrw(advanceSnapshot.fee)
    }

    val effectiveSelectedAdvanceAmount = if (usesRemoteAdvance) {
        selectedAdvanceAmount ?: remoteAvailableAmount.toInt()
    } else {
        selectedAdvanceAmount ?: advanceSnapshot.requestAmount
    }
    val advanceAmountOptions = buildAmountOptions(
        available = if (usesRemoteAdvance) remoteAvailableAmount.toInt() else advanceSnapshot.available,
        selected = effectiveSelectedAdvanceAmount,
        presets = listOf(50_000, 100_000, 150_000, 200_000)
    )
    val selectedRequestText = if (usesRemoteAdvance && advanceContractState.canRequest) {
        formatKrw(effectiveSelectedAdvanceAmount)
    } else {
        detailRequestAmountText
    }
    val selectedReceiveText = if (usesRemoteAdvance && advanceContractState.canRequest) {
        formatKrw(effectiveSelectedAdvanceAmount)
    } else {
        detailReceiveAmountText
    }
    val selectedFeeText = if (usesRemoteAdvance && advanceContractState.canRequest) {
        "-"
    } else {
        detailFeeText
    }
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
            surfaceState = advanceContractState.surfaceState,
            availableText = formatKrw(remoteAvailableAmount.toInt()),
            repaymentDueText = remoteRepaymentDueText,
            statusText = "${advanceContractState.stateTitleText} · ${advanceContractState.repaymentTier}",
            sourceLabelText = advanceContractState.sourceLabelText,
            stateBodyText = advanceContractState.stateBodyText,
            progress = advanceProgress,
            progressHintText = effectiveProgressHintText,
            actionText = advanceContractState.actionText,
            detail = FinanceAdvanceDetailUiModel(
                surfaceState = advanceContractState.surfaceState,
                subtitleText = "근무 기록 기반 한도로 급여일 전에 일부를 먼저 받습니다.",
                stateTitleText = advanceContractState.stateTitleText,
                stateBodyText = advanceContractState.stateBodyText,
                availableText = formatKrw(remoteAvailableAmount.toInt()),
                usedText = formatKrw(remoteUsedAmount.toInt()),
                repaymentDueText = remoteRepaymentDueText,
                calendarSummaryText = detailCalendarSummaryText,
                updatedAtText = detailUpdatedAtText,
                reflectedCountText = detailReflectedCountText,
                reviewCountText = "${advanceContractState.needsReviewRecordCount}건",
                unreflectedCountText = detailUnreflectedCountText,
                progressHintText = effectiveProgressHintText,
                requestAmountText = selectedRequestText,
                receiveAmountText = selectedReceiveText,
                feeText = selectedFeeText,
                tierText = advanceContractState.repaymentTier,
                blockReasonTexts = advanceContractState.blockReasonTexts,
                disclaimerText = advanceContractState.disclaimerText,
                canRequest = advanceContractState.canRequest && !advanceRequestUiState.isSubmitting,
                requestButtonText = when {
                    advanceRequestUiState.isSubmitting -> "신청 중..."
                    advanceContractState.canRequest -> "미리받기 신청"
                    else -> advanceContractState.actionText
                },
                requestFeedbackText = advanceRequestUiState.message,
                requestFeedbackIsError = advanceRequestUiState.isError,
                amountOptions = if (usesRemoteAdvance) {
                    buildAmountOptions(
                        available = remoteAvailableAmount.toInt(),
                        selected = effectiveSelectedAdvanceAmount,
                        presets = listOf(50_000, 100_000, 150_000, 200_000)
                    )
                } else {
                    advanceAmountOptions
                },
                historyItems = effectiveHistoryItems,
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
            ),
            requestDetail = advanceRequestDetailUiState.toUiModel()
        ),
        wage = FinanceWageUiModel(
            statusText = if (!isRecorded) {
                "입금 전"
            } else if (effectiveDifference == 0) {
                "차이 없음"
            } else {
                "확인 필요한 차이"
            },
            statusTone = if (!isRecorded) {
                BadgeTone.Info
            } else if (effectiveDifference == 0) {
                BadgeTone.Success
            } else {
                BadgeTone.Warning
            },
            differenceText = if (isRecorded) {
                formatKrw(abs(effectiveDifference))
            } else {
                "확인 전"
            },
            estimatedText = formatKrw(effectiveEstimatedTotal),
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
                amount = amount,
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
            requestId = null,
            title = "${month}월 1차 미리받기",
            metaText = "상환 예정 $repaymentDueText",
            valueText = formatKrw(used),
            clickable = false
        )
    }
    if (previousRepaymentGood) {
        items += FinanceAdvanceHistoryUiModel(
            requestId = null,
            title = "이전 회차 상환",
            metaText = "보너스 한도 ${formatKrw(bonusLimit)} 반영",
            valueText = "정상 상환",
            clickable = false
        )
    }
    if (items.isEmpty()) {
        items += FinanceAdvanceHistoryUiModel(
            requestId = null,
            title = "이번 달 미리받기 이력 없음",
            metaText = "근무 반영이 쌓이면 바로 신청할 수 있어요.",
            valueText = "-",
            clickable = false
        )
    }
    return items
}

private fun buildRemoteAdvanceHistoryItems(
    requests: List<com.dondone.mobile.data.advance.AdvanceRequestItemPayload>,
    emptyMessage: String
): List<FinanceAdvanceHistoryUiModel> {
    if (requests.isEmpty()) {
        return listOf(
            FinanceAdvanceHistoryUiModel(
                requestId = null,
                title = "실연동 미리받기 이력 없음",
                metaText = emptyMessage,
                valueText = "-",
                clickable = false
            )
        )
    }

    return requests
        .sortedByDescending { it.requestId }
        .take(3)
        .map { request ->
            FinanceAdvanceHistoryUiModel(
                requestId = request.requestId,
                title = "요청 #${request.requestId}",
                metaText = "${request.status} · 상환 예정 ${request.repaymentDueDate}",
                valueText = request.approvedAmount?.toInt()?.let(::formatKrw) ?: "승인 대기",
                clickable = true
            )
        }
}

private fun AdvanceRequestDetailUiState.toUiModel(): FinanceAdvanceRequestDetailUiModel {
    val detailValue = detail
    return FinanceAdvanceRequestDetailUiModel(
        isVisible = isLoading || detailValue != null || errorMessage != null,
        isLoading = isLoading,
        titleText = detailValue?.let { "요청 #${it.requestId}" } ?: "신청 상세",
        stateText = detailValue?.status ?: "-",
        requestedAmountText = detailValue?.requestedAmount?.toInt()?.let(::formatKrw) ?: "-",
        approvedAmountText = detailValue?.approvedAmount?.toInt()?.let(::formatKrw) ?: "승인 대기",
        feeAmountText = detailValue?.feeAmount?.toInt()?.let(::formatKrw) ?: "-",
        repaymentDueText = detailValue?.repaymentDueDate ?: "-",
        createdAtText = detailValue?.createdAt ?: "-",
        snapshotAvailableText = detailValue?.eligibilitySnapshot?.availableAmount?.toInt()?.let(::formatKrw) ?: "-",
        snapshotCapText = detailValue?.eligibilitySnapshot?.maxCap?.toInt()?.let(::formatKrw) ?: "-",
        snapshotReflectedText = detailValue?.eligibilitySnapshot?.reflectedWorkDays?.let { days ->
            "${days}일 · ${detailValue.eligibilitySnapshot.reflectedWorkMinutes}분"
        } ?: "-",
        snapshotReviewText = detailValue?.eligibilitySnapshot?.needsReviewRecordCount?.let { "${it}건" } ?: "-",
        errorMessage = errorMessage
    )
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
