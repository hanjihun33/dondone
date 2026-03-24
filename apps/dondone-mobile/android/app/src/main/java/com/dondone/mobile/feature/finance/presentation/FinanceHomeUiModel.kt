package com.dondone.mobile.feature.finance.presentation

import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.app.session.AdvanceRequestUiState
import com.dondone.mobile.app.session.AdvanceRequestDetailUiState
import com.dondone.mobile.app.session.VaultMessagePresentation
import com.dondone.mobile.app.session.VaultActionUiState
import com.dondone.mobile.data.advance.AdvanceRemoteMode
import com.dondone.mobile.data.advance.AdvanceRemoteState
import com.dondone.mobile.data.remittance.RemittanceRemoteMode
import com.dondone.mobile.data.remittance.RemittanceRemoteState
import com.dondone.mobile.data.vault.VaultActionType
import com.dondone.mobile.data.vault.VaultRemoteMode
import com.dondone.mobile.data.vault.VaultRemoteState
import com.dondone.mobile.data.vault.VaultSummaryPayload
import com.dondone.mobile.data.vault.VaultTransactionDetailPayload
import com.dondone.mobile.data.wage.WageRemoteState
import com.dondone.mobile.domain.calculator.AdvanceCalculator
import com.dondone.mobile.domain.calculator.VaultCalculator
import com.dondone.mobile.domain.calculator.WageEstimator
import com.dondone.mobile.domain.calculator.WorkproofCalculator
import com.dondone.mobile.domain.advance.AdvanceSurfaceState
import com.dondone.mobile.domain.advance.toAdvanceContractState
import com.dondone.mobile.domain.model.DemoState
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

private const val DEFAULT_VAULT_DISCLAIMER =
    "예상 이자는 테스트넷 기준 데모 추정치이며 실제 수익을 보장하지 않습니다."

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
    val payoutStatusText: String,
    val payoutTxHashText: String?,
    val requestedAmountText: String,
    val approvedAmountText: String,
    val feeAmountText: String,
    val settlementStatusText: String,
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
    val noticeTitleText: String?,
    val noticeBodyText: String?,
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
    val secondaryActionText: String?,
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
    val stateTitleText: String,
    val stateBodyText: String,
    val noticeTitleText: String?,
    val noticeBodyText: String?,
    val progress: Float,
    val progressHintText: String,
    val actionText: String,
    val secondaryActionText: String?,
    val detail: FinanceAdvanceDetailUiModel,
    val requestDetail: FinanceAdvanceRequestDetailUiModel
)

data class FinanceVaultDetailUiModel(
    val subtitleText: String,
    val isLoading: Boolean,
    val isActive: Boolean,
    val selectedActionType: VaultActionType,
    val depositEnabled: Boolean,
    val withdrawEnabled: Boolean,
    val walletBalanceText: String,
    val availableText: String,
    val balanceText: String,
    val selectedAmountText: String,
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
    val statusTitleText: String?,
    val statusBodyText: String?,
    val statusIsError: Boolean,
    val statusIsTerminal: Boolean,
    val disclaimerText: String,
    val actionText: String,
    val actionEnabled: Boolean,
    val actionInFlight: Boolean,
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
    val latestStatusKey: String?,
    val depositStatusText: String,
    val accruedInterestText: String,
    val aprText: String,
    val helperText: String,
    val latestStatusText: String?,
    val latestStatusIsError: Boolean,
    val shouldDismissDetailSheet: Boolean,
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
    remittanceRemoteState: RemittanceRemoteState = RemittanceRemoteState.unauthenticated(""),
    vaultRemoteState: VaultRemoteState? = null,
    selectedAdvanceAmount: Int? = null,
    selectedVaultAmount: Int? = null,
    selectedVaultActionType: VaultActionType = VaultActionType.DEPOSIT,
    advanceRequestUiState: AdvanceRequestUiState = AdvanceRequestUiState(),
    advanceRequestDetailUiState: AdvanceRequestDetailUiState = AdvanceRequestDetailUiState(),
    vaultActionUiState: VaultActionUiState = VaultActionUiState()
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
    val remoteEligibility = remoteState?.eligibility
    val remoteAssetSymbol = remoteEligibility?.assetSymbol ?: latestRemoteRequest?.assetSymbol.orEmpty()
    val remoteAssetDecimals = remoteEligibility?.assetDecimals ?: latestRemoteRequest?.assetDecimals ?: 6
    val remoteUsedAmountAtomic = remoteRequests.sumOf { it.approvedAmountAtomic ?: 0L }
    val remoteUsedDisplayKrwAmount = remoteRequests.sumOf { it.approvedDisplayKrwAmount ?: 0L }
    val remoteAvailableAmountAtomic = remoteEligibility?.availableAmountAtomic ?: 0L
    val remoteAvailableDisplayKrwAmount = remoteEligibility?.availableDisplayKrwAmount ?: 0L
    val remoteAvailableAmount = if (usesRemoteAdvance) {
        remoteEligibility?.availableAmountInWholeAssetUnits?.toLong() ?: 0L
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
        if (advanceContractState.noticeTitleText != null) {
            "반영 완료 기록 기준으로 다음 구간 진행도를 계산했어요."
        } else {
            "반영된 근무 기준으로 다음 구간까지 보여줘요."
        }
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
        latestRemoteRequest != null -> formatAdvanceAmount(
            amountAtomic = latestRemoteRequest.requestedAmountAtomic,
            displayKrwAmount = latestRemoteRequest.requestedDisplayKrwAmount,
            assetDecimals = latestRemoteRequest.assetDecimals,
            assetSymbol = latestRemoteRequest.assetSymbol
        )
        usesRemoteAdvance -> "-"
        else -> formatKrw(advanceSnapshot.requestAmount)
    }
    val detailReceiveAmountText = when {
        latestRemoteRequest != null -> latestRemoteRequest.approvedAmountAtomic?.let { approvedAmountAtomic ->
            formatAdvanceAmount(
                amountAtomic = approvedAmountAtomic,
                displayKrwAmount = latestRemoteRequest.approvedDisplayKrwAmount,
                assetDecimals = latestRemoteRequest.assetDecimals,
                assetSymbol = latestRemoteRequest.assetSymbol
            )
        } ?: "승인 대기"
        usesRemoteAdvance -> "-"
        else -> formatKrw(advanceSnapshot.receiveAmount)
    }
    val detailFeeText = when {
        latestRemoteRequest != null -> latestRemoteRequest.approvedAmountAtomic?.let { approvedAmountAtomic ->
            val feeAmountAtomic = (latestRemoteRequest.requestedAmountAtomic - approvedAmountAtomic).coerceAtLeast(0L)
            val feeDisplayKrwAmount =
                (latestRemoteRequest.requestedDisplayKrwAmount - (latestRemoteRequest.approvedDisplayKrwAmount ?: 0L)).coerceAtLeast(0L)
            formatAdvanceAmount(
                amountAtomic = feeAmountAtomic,
                displayKrwAmount = feeDisplayKrwAmount,
                assetDecimals = latestRemoteRequest.assetDecimals,
                assetSymbol = latestRemoteRequest.assetSymbol
            )
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
        presets = if (usesRemoteAdvance) listOf(10, 25, 50, 100) else listOf(50_000, 100_000, 150_000, 200_000),
        labelFormatter = if (usesRemoteAdvance) {
            { amount -> "$amount $remoteAssetSymbol" }
        } else {
            ::formatKrw
        }
    )
    val selectedRequestText = if (usesRemoteAdvance && advanceContractState.canRequest) {
        formatWholeAdvanceUnits(
            amount = effectiveSelectedAdvanceAmount,
            assetSymbol = remoteAssetSymbol,
            displayKrwAmount = remoteEligibility?.let {
                if (it.availableAmountInWholeAssetUnits > 0) {
                    (it.availableDisplayKrwAmount * effectiveSelectedAdvanceAmount) / it.availableAmountInWholeAssetUnits
                } else {
                    0L
                }
            } ?: 0L
        )
    } else {
        detailRequestAmountText
    }
    val selectedReceiveText = if (usesRemoteAdvance && advanceContractState.canRequest) {
        formatWholeAdvanceUnits(
            amount = effectiveSelectedAdvanceAmount,
            assetSymbol = remoteAssetSymbol,
            displayKrwAmount = remoteEligibility?.let {
                if (it.availableAmountInWholeAssetUnits > 0) {
                    (it.availableDisplayKrwAmount * effectiveSelectedAdvanceAmount) / it.availableAmountInWholeAssetUnits
                } else {
                    0L
                }
            } ?: 0L
        )
    } else {
        detailReceiveAmountText
    }
    val selectedFeeText = if (usesRemoteAdvance && advanceContractState.canRequest) {
        "-"
    } else {
        detailFeeText
    }
    val remoteVaultSummary = vaultRemoteState?.payload?.summary
    val remoteLatestVaultTransaction = vaultRemoteState?.payload?.latestTransaction
    val remoteWalletBalance = if (remittanceRemoteState.mode == RemittanceRemoteMode.CONTENT) {
        remittanceRemoteState.payload?.balance
    } else {
        null
    }
    val vaultStatusMessage = remoteLatestVaultTransaction?.toVaultStatusMessage()
        ?: when (vaultRemoteState?.mode) {
            VaultRemoteMode.LOADING -> FinanceVaultStatusMessage(
                title = "실연동 확인 중",
                body = "지갑과 예치 상태를 불러오는 중이에요.",
                isError = false
            )

            VaultRemoteMode.ERROR -> FinanceVaultStatusMessage(
                title = "다시 확인 필요",
                body = vaultRemoteState.errorMessage ?: "예치 상태를 다시 불러와 주세요.",
                isError = true
            )

            VaultRemoteMode.UNAUTHENTICATED -> FinanceVaultStatusMessage(
                title = "로그인 필요",
                body = vaultRemoteState.errorMessage ?: "로그인 후 예치 실연동 데이터를 불러옵니다.",
                isError = true
            )

            else -> null
        }
    val usesRemoteVault = remoteVaultSummary != null
    val remoteVaultAssetSymbol = remoteVaultSummary?.assetSymbol.orEmpty()
    val remoteVaultDecimals = remoteVaultSummary?.assetDecimals ?: 6
    val remoteStoredUnits = remoteVaultSummary?.storedAmountAtomic?.toWholeAssetUnits(remoteVaultDecimals) ?: 0
    val remoteAvailableDepositUnits = remoteVaultSummary?.availableToStoreAmountAtomic?.toWholeAssetUnits(remoteVaultDecimals) ?: 0
    val remoteAvailableWithdrawUnits = remoteStoredUnits
    val effectiveVaultActionType = if (selectedVaultActionType == VaultActionType.WITHDRAW && remoteAvailableWithdrawUnits <= 0) {
        VaultActionType.DEPOSIT
    } else {
        selectedVaultActionType
    }
    val effectiveVaultSelectedAmount = when {
        usesRemoteVault -> {
            val availableUnits = if (effectiveVaultActionType == VaultActionType.DEPOSIT) {
                remoteAvailableDepositUnits
            } else {
                remoteAvailableWithdrawUnits
            }
            when {
                availableUnits <= 0 -> 0
                selectedVaultAmount != null && selectedVaultAmount in 1..availableUnits -> selectedVaultAmount
                else -> pickClosestAmountOption(
                    target = availableUnits,
                    presets = listOf(10, 25, 50, 100),
                    available = availableUnits
                )
            }
        }

        vault.userDeposit > 0 -> vault.userDeposit
        else -> {
            val vaultAvailable = selectedAccount.balance
            pickClosestAmountOption(
                target = vaultSnapshot.suggestedDeposit.coerceAtMost(vaultAvailable),
                presets = listOf(100_000, 300_000, 500_000, 1_000_000),
                available = vaultAvailable
            )
        }
    }
    val vaultAmountOptions = if (usesRemoteVault) {
        val availableUnits = if (effectiveVaultActionType == VaultActionType.DEPOSIT) {
            remoteAvailableDepositUnits
        } else {
            remoteAvailableWithdrawUnits
        }
        buildAmountOptions(
            available = availableUnits,
            selected = effectiveVaultSelectedAmount,
            presets = listOf(10, 25, 50, 100),
            labelFormatter = { amount -> "$amount $remoteVaultAssetSymbol" }
        )
    } else {
        val vaultAvailable = selectedAccount.balance
        buildAmountOptions(
            available = vaultAvailable,
            selected = effectiveVaultSelectedAmount,
            presets = listOf(100_000, 300_000, 500_000, 1_000_000),
            labelFormatter = ::formatTenThousandUnit
        )
    }
    val userDepositForBreakdown = max(
        1,
        if (usesRemoteVault) remoteStoredUnits.coerceAtLeast(effectiveVaultSelectedAmount) else {
            if (vault.userDeposit > 0) vault.userDeposit else effectiveVaultSelectedAmount
        }
    )
    val vaultAprRate = if (usesRemoteVault) {
        (remoteVaultSummary?.interestPreview?.apyBps ?: 0) / 10_000.0
    } else {
        vault.apr
    }
    val defiMonthly = floor(userDepositForBreakdown * (vaultAprRate / 12)).toInt()
    val feeShare = if (usesRemoteVault) {
        0
    } else {
        floor(vault.monthlyFeeRevenue * (userDepositForBreakdown / max(1, vault.totalPool).toDouble())).toInt()
    }
    val remoteProjectedPrincipalAtomic = if (usesRemoteVault) {
        val currentPrincipalAtomic = remoteVaultSummary!!.storedAmountAtomic.toBigIntegerSafe()
        val selectedAmountAtomic = effectiveVaultSelectedAmount.toAtomicBigInteger(remoteVaultDecimals)
        when (effectiveVaultActionType) {
            VaultActionType.DEPOSIT -> currentPrincipalAtomic.add(selectedAmountAtomic)
            VaultActionType.WITHDRAW -> currentPrincipalAtomic.subtract(selectedAmountAtomic).max(BigInteger.ZERO)
        }
    } else {
        BigInteger.ZERO
    }
    val remoteProjectedMonthlyYieldAtomic = if (usesRemoteVault) {
        projectVaultYieldAtomic(
            projectedPrincipalAtomic = remoteProjectedPrincipalAtomic,
            currentPrincipalAtomic = remoteVaultSummary!!.storedAmountAtomic.toBigIntegerSafe(),
            currentPreviewAtomic = remoteVaultSummary.interestPreview.monthlyEstimatedYieldAtomic,
            apyBps = remoteVaultSummary.interestPreview.apyBps,
            period = VaultYieldPeriod.MONTHLY
        )
    } else {
        BigInteger.ZERO
    }
    val remoteProjectedDailyYieldAtomic = if (usesRemoteVault) {
        projectVaultYieldAtomic(
            projectedPrincipalAtomic = remoteProjectedPrincipalAtomic,
            currentPrincipalAtomic = remoteVaultSummary!!.storedAmountAtomic.toBigIntegerSafe(),
            currentPreviewAtomic = remoteVaultSummary.interestPreview.dailyEstimatedYieldAtomic,
            apyBps = remoteVaultSummary.interestPreview.apyBps,
            period = VaultYieldPeriod.DAILY
        )
    } else {
        BigInteger.ZERO
    }

    return FinanceHomeUiModel(
        account = FinanceAccountUiModel(
            balanceText = formatKrw(selectedAccount.balance),
            sendableAmountText = formatKrw(remittance.draftAmountUsd * 1_450),
            selectedAccountText = "${selectedAccount.name} · ${selectedAccount.number}"
        ),
        advance = FinanceAdvanceUiModel(
            surfaceState = advanceContractState.surfaceState,
            availableText = if (usesRemoteAdvance) {
                formatAdvanceAmount(
                    amountAtomic = remoteAvailableAmountAtomic,
                    displayKrwAmount = remoteAvailableDisplayKrwAmount,
                    assetDecimals = remoteAssetDecimals,
                    assetSymbol = remoteAssetSymbol
                )
            } else {
                formatKrw(remoteAvailableAmount.toInt())
            },
            repaymentDueText = remoteRepaymentDueText,
            statusText = advanceContractState.repaymentTier,
            sourceLabelText = advanceContractState.sourceLabelText,
            stateTitleText = advanceContractState.stateTitleText,
            stateBodyText = advanceContractState.stateBodyText,
            noticeTitleText = advanceContractState.noticeTitleText,
            noticeBodyText = advanceContractState.noticeBodyText,
            progress = advanceProgress,
            progressHintText = effectiveProgressHintText,
            actionText = advanceContractState.actionText,
            secondaryActionText = advanceContractState.secondaryActionText,
            detail = FinanceAdvanceDetailUiModel(
                surfaceState = advanceContractState.surfaceState,
                subtitleText = "근무 기록 기반 한도로 급여일 전에 일부를 먼저 받습니다.",
                stateTitleText = advanceContractState.stateTitleText,
                stateBodyText = advanceContractState.stateBodyText,
                noticeTitleText = advanceContractState.noticeTitleText,
                noticeBodyText = advanceContractState.noticeBodyText,
                availableText = if (usesRemoteAdvance) {
                    formatAdvanceAmount(
                        amountAtomic = remoteAvailableAmountAtomic,
                        displayKrwAmount = remoteAvailableDisplayKrwAmount,
                        assetDecimals = remoteAssetDecimals,
                        assetSymbol = remoteAssetSymbol
                    )
                } else {
                    formatKrw(remoteAvailableAmount.toInt())
                },
                usedText = if (usesRemoteAdvance) {
                    formatAdvanceAmount(
                        amountAtomic = remoteUsedAmountAtomic,
                        displayKrwAmount = remoteUsedDisplayKrwAmount,
                        assetDecimals = remoteAssetDecimals,
                        assetSymbol = remoteAssetSymbol
                    )
                } else {
                    formatKrw(advanceSnapshot.used)
                },
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
                secondaryActionText = advanceContractState.secondaryActionText,
                requestFeedbackText = advanceRequestUiState.message,
                requestFeedbackIsError = advanceRequestUiState.isError,
                amountOptions = if (usesRemoteAdvance) {
                    buildAmountOptions(
                        available = remoteAvailableAmount.toInt(),
                        selected = effectiveSelectedAdvanceAmount,
                        presets = listOf(10, 25, 50, 100),
                        labelFormatter = { amount -> "$amount $remoteAssetSymbol" }
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
            latestStatusKey = remoteLatestVaultTransaction?.let { "${it.requestId}:${it.status}" },
            depositStatusText = when {
                usesRemoteVault && remoteStoredUnits > 0 ->
                    formatTokenAmount(
                        atomic = remoteVaultSummary!!.storedAmountAtomic,
                        decimals = remoteVaultDecimals,
                        symbol = remoteVaultAssetSymbol
                    )

                usesRemoteVault -> "미예치"
                vault.enabled && vault.userDeposit > 0 -> formatKrw(vault.userDeposit)
                else -> "미신청"
            },
            accruedInterestText = if (usesRemoteVault) {
                formatTokenAmount(
                    atomic = remoteVaultSummary!!.accruedYieldAtomic,
                    decimals = remoteVaultDecimals,
                    symbol = remoteVaultAssetSymbol
                )
            } else {
                formatKrw(vault.accruedInterest)
            },
            aprText = if (usesRemoteVault) {
                formatApy(remoteVaultSummary!!.interestPreview.apyBps)
            } else {
                String.format(Locale.US, "%.1f%%", vault.apr * 100)
            },
            helperText = when {
                usesRemoteVault && remoteStoredUnits > 0 -> "예치 잔액과 예상 이자를 바로 확인할 수 있어요."
                usesRemoteVault -> "예치 가능 금액과 예상 이자를 먼저 확인할 수 있어요."
                vault.enabled && vault.userDeposit > 0 -> "예치 중인 금액과 누적 이자를 확인할 수 있어요."
                else -> "예치 시작 전 예상 이자와 수익 구성을 먼저 확인할 수 있어요."
            },
            latestStatusText = vaultStatusMessage?.title,
            latestStatusIsError = vaultStatusMessage?.isError == true,
            shouldDismissDetailSheet =
                vaultActionUiState.message != null &&
                    vaultActionUiState.messagePresentation == VaultMessagePresentation.TOAST_ONLY,
            actionText = if (usesRemoteVault) "관리" else if (vault.enabled && vault.userDeposit > 0) "보관 보기" else "신청",
            detail = FinanceVaultDetailUiModel(
                subtitleText = if (usesRemoteVault) {
                    "${remoteVaultSummary!!.network} ${remoteVaultAssetSymbol} 예치 상태를 기준으로 표시합니다."
                } else {
                    "스테이블 코인 예치 기준 예상 이자 현황입니다."
                },
                isLoading = vaultRemoteState?.isLoading == true && !usesRemoteVault,
                isActive = if (usesRemoteVault) remoteStoredUnits > 0 else vault.enabled && vault.userDeposit > 0,
                selectedActionType = effectiveVaultActionType,
                depositEnabled = if (usesRemoteVault) remoteAvailableDepositUnits > 0 else true,
                withdrawEnabled = if (usesRemoteVault) remoteAvailableWithdrawUnits > 0 else vault.enabled && vault.userDeposit > 0,
                walletBalanceText = if (remoteWalletBalance != null) {
                    formatTokenAmount(
                        atomic = remoteWalletBalance.tokenBalanceAtomic,
                        decimals = remoteWalletBalance.assetDecimals,
                        symbol = remoteWalletBalance.assetSymbol
                    )
                } else if (usesRemoteVault) {
                    formatTokenAmount(
                        atomic = remoteVaultSummary!!.walletTokenBalanceAtomic,
                        decimals = remoteVaultDecimals,
                        symbol = remoteVaultAssetSymbol
                    )
                } else {
                    formatKrw(selectedAccount.balance)
                },
                availableText = if (usesRemoteVault) {
                    val availableAtomic = if (effectiveVaultActionType == VaultActionType.DEPOSIT) {
                        remoteVaultSummary!!.availableToStoreAmountAtomic
                    } else {
                        remoteVaultSummary!!.storedAmountAtomic
                    }
                    formatTokenAmount(
                        atomic = availableAtomic,
                        decimals = remoteVaultDecimals,
                        symbol = remoteVaultAssetSymbol
                    )
                } else {
                    formatKrw(selectedAccount.balance)
                },
                balanceText = if (usesRemoteVault) {
                    formatTokenAmount(
                        atomic = remoteVaultSummary!!.storedAmountAtomic,
                        decimals = remoteVaultDecimals,
                        symbol = remoteVaultAssetSymbol
                    )
                } else if (vault.userDeposit > 0) {
                    formatKrw(vault.userDeposit)
                } else {
                    formatKrw(effectiveVaultSelectedAmount)
                },
                selectedAmountText = if (usesRemoteVault) {
                    "${effectiveVaultSelectedAmount.coerceAtLeast(0)} $remoteVaultAssetSymbol"
                } else {
                    formatKrw(effectiveVaultSelectedAmount)
                },
                aprText = if (usesRemoteVault) {
                    formatApy(remoteVaultSummary!!.interestPreview.apyBps)
                } else {
                    String.format(Locale.US, "%.1f%%", vault.apr * 100)
                },
                accruedInterestText = if (usesRemoteVault) {
                    formatTokenAmount(
                        atomic = remoteVaultSummary!!.accruedYieldAtomic,
                        decimals = remoteVaultDecimals,
                        symbol = remoteVaultAssetSymbol
                    )
                } else {
                    formatKrw(vault.accruedInterest)
                },
                monthlyInterestText = if (usesRemoteVault) {
                    formatYieldTokenAmount(
                        atomic = remoteProjectedMonthlyYieldAtomic.toString(),
                        decimals = remoteVaultDecimals,
                        symbol = remoteVaultAssetSymbol
                    )
                } else {
                    formatKrw(vaultSnapshot.monthlyInterest)
                },
                dailyInterestText = if (usesRemoteVault) {
                    formatYieldTokenAmount(
                        atomic = remoteProjectedDailyYieldAtomic.toString(),
                        decimals = remoteVaultDecimals,
                        symbol = remoteVaultAssetSymbol
                    )
                } else {
                    formatKrw(vaultSnapshot.dailyInterest)
                },
                defiMonthlyText = if (usesRemoteVault) {
                    formatYieldTokenAmount(
                        atomic = remoteProjectedMonthlyYieldAtomic.toString(),
                        decimals = remoteVaultDecimals,
                        symbol = remoteVaultAssetSymbol
                    )
                } else {
                    formatKrw(defiMonthly)
                },
                feeShareText = if (usesRemoteVault) {
                    "시뮬레이션 제외"
                } else {
                    formatKrw(feeShare)
                },
                totalMonthlyText = if (usesRemoteVault) {
                    formatYieldTokenAmount(
                        atomic = remoteProjectedMonthlyYieldAtomic.toString(),
                        decimals = remoteVaultDecimals,
                        symbol = remoteVaultAssetSymbol
                    )
                } else {
                    formatKrw(defiMonthly + feeShare)
                },
                defiRatioText = if (usesRemoteVault) "demo" else "${((1 - vault.advanceRatio) * 100).toInt()}%",
                advanceRatioText = if (usesRemoteVault) "demo" else "${(vault.advanceRatio * 100).toInt()}%",
                advanceUsageText = if (usesRemoteVault) "demo" else "${(vault.advanceUtilization * 100).toInt()}%",
                statusTitleText = vaultStatusMessage?.title,
                statusBodyText = vaultStatusMessage?.body,
                statusIsError = vaultStatusMessage?.isError == true,
                statusIsTerminal = remoteLatestVaultTransaction?.isTerminalStatus() == true,
                disclaimerText = if (usesRemoteVault) {
                    normalizeVaultDisclaimer(remoteVaultSummary!!.disclaimer)
                } else {
                    DEFAULT_VAULT_DISCLAIMER
                },
                actionText = when {
                    vaultActionUiState.isSubmitting && effectiveVaultActionType == VaultActionType.WITHDRAW -> "출금 요청 중..."
                    vaultActionUiState.isSubmitting -> "예치 요청 중..."
                    effectiveVaultActionType == VaultActionType.WITHDRAW -> "출금 요청하기"
                    else -> "예치 요청하기"
                },
                actionEnabled = usesRemoteVault &&
                    !vaultActionUiState.isSubmitting &&
                    effectiveVaultSelectedAmount > 0 &&
                    vaultAmountOptions.isNotEmpty(),
                actionInFlight = vaultActionUiState.isSubmitting,
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
            metaText = "급여 정산 예정 $repaymentDueText",
            valueText = formatKrw(used),
            clickable = false
        )
    }
    if (previousRepaymentGood) {
        items += FinanceAdvanceHistoryUiModel(
            requestId = null,
            title = "이전 회차 정산",
            metaText = "보너스 한도 ${formatKrw(bonusLimit)} 반영",
            valueText = "정산 완료",
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
                metaText = buildString {
                    append(formatAdvanceStatusLabel(request.status, request.payoutStatus))
                    append(" · 급여 정산 예정 ")
                    append(request.effectiveSettlementDueDate)
                    request.payoutTxHash?.takeIf { it.isNotBlank() }?.let { txHash ->
                        append("\nTX ")
                        append(txHash.take(10))
                        append("...")
                    }
                },
                valueText = request.approvedAmountAtomic?.let { approvedAmountAtomic ->
                    formatAdvanceAmount(
                        amountAtomic = approvedAmountAtomic,
                        displayKrwAmount = request.approvedDisplayKrwAmount,
                        assetDecimals = request.assetDecimals,
                        assetSymbol = request.assetSymbol
                    )
                } ?: formatAdvanceAmount(
                    amountAtomic = request.requestedAmountAtomic,
                    displayKrwAmount = request.requestedDisplayKrwAmount,
                    assetDecimals = request.assetDecimals,
                    assetSymbol = request.assetSymbol
                ),
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
        payoutStatusText = detailValue?.let { formatAdvanceStatusLabel(it.status, it.payoutStatus) } ?: "-",
        payoutTxHashText = detailValue?.payoutTxHash,
        requestedAmountText = detailValue?.let {
            formatAdvanceAmount(
                amountAtomic = it.requestedAmountAtomic,
                displayKrwAmount = it.requestedDisplayKrwAmount,
                assetDecimals = it.assetDecimals,
                assetSymbol = it.assetSymbol
            )
        } ?: "-",
        approvedAmountText = detailValue?.approvedAmountAtomic?.let { approvedAmountAtomic ->
            formatAdvanceAmount(
                amountAtomic = approvedAmountAtomic,
                displayKrwAmount = detailValue.approvedDisplayKrwAmount,
                assetDecimals = detailValue.assetDecimals,
                assetSymbol = detailValue.assetSymbol
            )
        } ?: "승인 대기",
        feeAmountText = detailValue?.let {
            formatAdvanceAmount(
                amountAtomic = it.feeAmountAtomic,
                displayKrwAmount = it.feeDisplayKrwAmount,
                assetDecimals = it.assetDecimals,
                assetSymbol = it.assetSymbol
            )
        } ?: "-",
        settlementStatusText = detailValue?.settlementStatus?.let(::formatAdvanceSettlementStatusLabel) ?: "-",
        repaymentDueText = detailValue?.effectiveSettlementDueDate ?: "-",
        createdAtText = detailValue?.createdAt ?: "-",
        snapshotAvailableText = detailValue?.eligibilitySnapshot?.let {
            formatAdvanceAmount(
                amountAtomic = it.availableAmountAtomic,
                displayKrwAmount = it.availableDisplayKrwAmount,
                assetDecimals = it.assetDecimals,
                assetSymbol = it.assetSymbol
            )
        } ?: "-",
        snapshotCapText = detailValue?.eligibilitySnapshot?.let {
            formatAdvanceAmount(
                amountAtomic = it.maxCapAmountAtomic,
                displayKrwAmount = it.maxCapDisplayKrwAmount,
                assetDecimals = it.assetDecimals,
                assetSymbol = it.assetSymbol
            )
        } ?: "-",
        snapshotReflectedText = detailValue?.eligibilitySnapshot?.reflectedWorkDays?.let { days ->
            "${days}일 · ${detailValue.eligibilitySnapshot.reflectedWorkMinutes}분"
        } ?: "-",
        snapshotReviewText = detailValue?.eligibilitySnapshot?.needsReviewRecordCount?.let { "${it}건" } ?: "-",
        errorMessage = errorMessage
    )
}

private data class FinanceVaultStatusMessage(
    val title: String,
    val body: String,
    val isError: Boolean
)

private fun VaultTransactionDetailPayload.isTerminalStatus(): Boolean =
    status == "CONFIRMED" || status == "FAILED" || status == "TIMED_OUT"

private fun normalizeVaultDisclaimer(raw: String): String {
    val normalized = raw.trim()
    if (normalized.isBlank()) {
        return DEFAULT_VAULT_DISCLAIMER
    }

    val lower = normalized.lowercase(Locale.ROOT)
    return if (
        lower.startsWith("vault ") ||
        lower.startsWith("valut ") ||
        (lower.contains("demo") && lower.contains("profit"))
    ) {
        DEFAULT_VAULT_DISCLAIMER
    } else {
        normalized
    }
}

private fun VaultTransactionDetailPayload.toVaultStatusMessage(): FinanceVaultStatusMessage {
    val actionLabel = if (txType == "WITHDRAW") "출금" else "예치"
    return when (status) {
        "CONFIRMED" -> FinanceVaultStatusMessage(
            title = "${actionLabel} 완료",
            body = if (txType == "WITHDRAW") {
                "지갑 잔액으로 돌아왔어요."
            } else {
                "지갑 잔액이 예치 잔액으로 반영됐어요."
            },
            isError = false
        )

        "FAILED", "TIMED_OUT" -> FinanceVaultStatusMessage(
            title = "${actionLabel} 실패",
            body = failureCode ?: "${actionLabel}가 완료되지 않았어요. 잠시 후 다시 시도해 주세요.",
            isError = true
        )

        "BROADCASTED" -> FinanceVaultStatusMessage(
            title = "${actionLabel} 확인 대기",
            body = "체인 전송이 완료돼 확정을 기다리는 중입니다.",
            isError = false
        )

        else -> FinanceVaultStatusMessage(
            title = "${actionLabel} 처리 중",
            body = "요청이 접수돼 서명과 전송을 준비하는 중입니다.",
            isError = false
        )
    }
}

private fun formatApy(apyBps: Int): String =
    String.format(Locale.US, "%.2f%%", apyBps / 100.0)

private fun formatTokenAmount(
    atomic: String,
    decimals: Int,
    symbol: String,
    fractionDigits: Int = 2
): String {
    val normalized = atomic.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val divisor = BigDecimal.TEN.pow(decimals.coerceAtLeast(0))
    val major = if (divisor.compareTo(BigDecimal.ZERO) == 0) {
        normalized
    } else {
        normalized.divide(divisor, fractionDigits, RoundingMode.DOWN)
    }
    val text = major.stripTrailingZeros().toPlainString()
    return "$text $symbol"
}

private fun formatYieldTokenAmount(
    atomic: String,
    decimals: Int,
    symbol: String
): String = formatTokenAmount(
    atomic = atomic,
    decimals = decimals,
    symbol = symbol,
    fractionDigits = 4
)

private fun formatAdvanceAmount(
    amountAtomic: Long,
    displayKrwAmount: Long?,
    assetDecimals: Int,
    assetSymbol: String
): String {
    val assetText = formatTokenAmount(
        atomic = amountAtomic.toString(),
        decimals = assetDecimals,
        symbol = assetSymbol
    )
    val approxText = displayKrwAmount?.toInt()?.let(::formatKrw)
    return if (approxText != null) "$assetText · 약 $approxText" else assetText
}

private fun formatWholeAdvanceUnits(
    amount: Int,
    assetSymbol: String,
    displayKrwAmount: Long
): String {
    val assetText = "$amount $assetSymbol"
    return if (displayKrwAmount > 0) "$assetText · 약 ${formatKrw(displayKrwAmount.toInt())}" else assetText
}

private fun formatAdvanceStatusLabel(status: String, payoutStatus: String?): String = when (status) {
    "SUBMITTED" -> "신청됨"
    "APPROVED" -> "승인됨"
    "PAYING" -> when (payoutStatus) {
        "SIGNED" -> "서명 완료"
        "BROADCASTED" -> "지급중"
        else -> "지급 준비중"
    }
    "PAID" -> "지급완료"
    "PAYOUT_FAILED" -> "지급실패"
    "REJECTED" -> "반려됨"
    "NEEDS_REVIEW" -> "확인 필요"
    else -> status
}

private fun formatAdvanceSettlementStatusLabel(status: String): String = when (status) {
    "SCHEDULED_FOR_PAYDAY" -> "급여일 자동 정산 예정"
    "SETTLED" -> "정산 완료"
    "FAILED" -> "정산 실패"
    else -> status
}

private fun String.toWholeAssetUnits(decimals: Int): Int {
    val sanitized = trim().ifBlank { return 0 }
    if (sanitized == "0") return 0
    if (decimals <= 0) {
        return sanitized.toLongOrNull()
            ?.coerceIn(0L, Int.MAX_VALUE.toLong())
            ?.toInt()
            ?: 0
    }
    val wholePart = if (sanitized.length > decimals) sanitized.dropLast(decimals) else "0"
    return wholePart.toLongOrNull()
        ?.coerceIn(0L, Int.MAX_VALUE.toLong())
        ?.toInt()
        ?: 0
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

private enum class VaultYieldPeriod {
    DAILY,
    MONTHLY
}

private fun projectVaultYieldAtomic(
    projectedPrincipalAtomic: BigInteger,
    currentPrincipalAtomic: BigInteger,
    currentPreviewAtomic: String,
    apyBps: Int,
    period: VaultYieldPeriod
): BigInteger {
    if (projectedPrincipalAtomic.signum() <= 0 || apyBps <= 0) {
        return BigInteger.ZERO
    }
    if (currentPrincipalAtomic.signum() > 0) {
        val basePreviewAtomic = currentPreviewAtomic.toBigIntegerSafe()
        if (basePreviewAtomic.signum() > 0) {
            return basePreviewAtomic
                .multiply(projectedPrincipalAtomic)
                .divide(currentPrincipalAtomic)
        }
    }
    return estimateVaultYieldAtomic(
        principalAtomic = projectedPrincipalAtomic,
        apyBps = apyBps,
        period = period
    )
}

private fun estimateVaultYieldAtomic(
    principalAtomic: BigInteger,
    apyBps: Int,
    period: VaultYieldPeriod
): BigInteger {
    if (principalAtomic.signum() <= 0 || apyBps <= 0) {
        return BigInteger.ZERO
    }
    val yearlyYield = principalAtomic
        .multiply(BigInteger.valueOf(apyBps.toLong()))
        .divide(BigInteger.valueOf(10_000L))
    return when (period) {
        VaultYieldPeriod.DAILY -> yearlyYield.divide(BigInteger.valueOf(360L))
        VaultYieldPeriod.MONTHLY -> yearlyYield.divide(BigInteger.valueOf(12L))
    }
}

private fun Int.toAtomicBigInteger(decimals: Int): BigInteger {
    if (this <= 0) {
        return BigInteger.ZERO
    }
    return BigInteger.valueOf(toLong())
        .multiply(BigInteger.TEN.pow(decimals.coerceAtLeast(0)))
}

private fun String.toBigIntegerSafe(): BigInteger = toBigIntegerOrNull() ?: BigInteger.ZERO
