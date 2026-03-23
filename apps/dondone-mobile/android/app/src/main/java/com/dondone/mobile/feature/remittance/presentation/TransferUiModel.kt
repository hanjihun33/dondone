package com.dondone.mobile.feature.remittance.presentation

import com.dondone.mobile.app.session.RemittanceActionUiState
import com.dondone.mobile.app.session.RecipientPhoneSearchUiState
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.data.remittance.RemittanceRemoteMode
import com.dondone.mobile.data.remittance.RemittanceRemoteState
import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.TransferDestinationMode
import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus
import com.dondone.mobile.feature.recipient.presentation.buildDemoRecipientDirectory
import com.dondone.mobile.feature.recipient.presentation.RecipientDirectoryContactUiModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

private const val REMITTANCE_WALLET_ID = "remote-wallet"

enum class TransferRecipientTone {
    Amber,
    Blue,
    Indigo,
    Teal
}

data class TransferAccountUiModel(
    val id: String,
    val name: String,
    val number: String,
    val balanceText: String,
    val selected: Boolean
)

data class TransferRecipientUiModel(
    val id: String,
    val name: String,
    val address: String,
    val relationship: String,
    val accountLabel: String,
    val contactLabel: String,
    val tone: TransferRecipientTone,
    val selected: Boolean
)

data class TransferRecipientSectionUiModel(
    val title: String,
    val items: List<TransferRecipientUiModel>
)

data class TransferRemoteGateUiModel(
    val title: String,
    val description: String,
    val actionText: String? = null,
    val isLoading: Boolean = false
)

data class TransferReviewNoticeUiModel(
    val title: String,
    val description: String
)

data class TransferUiModel(
    val flowStep: TransferFlowStep,
    val transferStatus: TransferStatus,
    val isActionSubmitting: Boolean,
    val destinationMode: TransferDestinationMode,
    val isRemoteMode: Boolean,
    val selectedAccountName: String,
    val selectedAccountNumber: String,
    val selectedAccountBalanceText: String,
    val selectedRecipientName: String,
    val selectedRecipientAccountLabel: String,
    val selectedRecipientWalletLabel: String,
    val selectedRecipientWalletFullLabel: String,
    val amountUsd: String,
    val confirmationAmountText: String,
    val accountStepHintText: String,
    val canSubmit: Boolean,
    val showReviewScreen: Boolean,
    val showTrackerScreen: Boolean,
    val remoteGate: TransferRemoteGateUiModel?,
    val reviewNotice: TransferReviewNoticeUiModel?,
    val trackerDetailText: String,
    val trackerTxHashText: String?,
    val recipientScreenTitle: String,
    val recipientSearchPlaceholderText: String,
    val showAddRecipientAction: Boolean,
    val addRecipientPhoneDirectory: List<RecipientDirectoryContactUiModel>,
    val addRecipientSupportsRemotePhoneSearch: Boolean,
    val addRecipientPhoneSearchResults: List<RecipientDirectoryContactUiModel>,
    val addRecipientPhoneSearchLoading: Boolean,
    val addRecipientPhoneSearchErrorMessage: String?,
    val accounts: List<TransferAccountUiModel>,
    val recipientSections: List<TransferRecipientSectionUiModel>
)

fun DemoState.toTransferUiModel(
    remoteState: RemittanceRemoteState,
    actionUiState: RemittanceActionUiState,
    isAuthenticated: Boolean,
    recipientPhoneSearchUiState: RecipientPhoneSearchUiState = RecipientPhoneSearchUiState()
): TransferUiModel {
    val isRemoteMode = isAuthenticated
    val remotePayload = remoteState.payload
    val remoteAccount = if (isRemoteMode && remotePayload != null) {
        TransferAccountUiModel(
            id = REMITTANCE_WALLET_ID,
            name = "DonDone 지갑",
            number = shortenWalletAddress(remotePayload.wallet.walletAddress),
            balanceText = remotePayload.balance?.formatTokenBalance() ?: "잔액 확인 중",
            selected = true
        )
    } else {
        null
    }

    val selectedAccount = remittance.selectedAccount()
    val selectedRecipient = remittance.selectedRecipientOrNull()
    val selectedRecipientName = selectedRecipient?.let { remittance.displayedRecipientName() } ?: "수신자를 선택해 주세요"
    val amountUsd = remittance.draftAmountUsd
    val amountKrw = amountUsd * 1_450
    val remoteBalanceAtomic = remotePayload?.balance?.tokenBalanceAtomic?.toLongOrNull()
    val amountAtomic = amountUsd.toLong() * 1_000_000L
    val canSubmit = if (isRemoteMode) {
        amountUsd > 0 &&
            remotePayload?.wallet?.fundingStatus == "FUNDED" &&
            remoteBalanceAtomic != null &&
            amountAtomic <= remoteBalanceAtomic
    } else {
        amountUsd > 0 && amountKrw <= selectedAccount.balance
    }

    val recipientItems = remittance.recipients.mapIndexed { index, recipient ->
        TransferRecipientUiModel(
            id = recipient.id,
            name = recipient.name,
            address = recipient.address,
            relationship = recipient.relationship,
            accountLabel = buildRecipientAccountLabel(recipient.address),
            contactLabel = "${recipient.relationship} · ${shortenWalletAddress(recipient.address)}",
            tone = recipientTone(index),
            selected = recipient.id == remittance.selectedRecipientId
        )
    }

    val remoteGate = if (isRemoteMode) resolveRemoteGate(remoteState) else null
    val reviewNotice = actionUiState.precheck?.toReviewNotice()
    val activeTransfer = remotePayload?.activeTransfer
    val trackerDetailText = when {
        remittance.status == TransferStatus.REVIEWING && actionUiState.isSubmitting ->
            "송금 요청을 전송하고 있어요."

        remittance.status == TransferStatus.CONFIRMED ->
            "송금이 정상적으로 완료되었어요."

        remittance.status == TransferStatus.FAILED -> {
            activeTransfer?.failureCode?.let { "전송이 실패했어요. 사유 : $it" } ?: "송금을 완료하지 못했어요."
        }

        else ->
            activeTransfer?.status?.toTrackerDetailText() ?: "송금 진행 상태를 확인하고 있어요."
    }

    return TransferUiModel(
        flowStep = remittance.flowStep,
        transferStatus = remittance.status,
        isActionSubmitting = actionUiState.isSubmitting,
        destinationMode = if (isRemoteMode) TransferDestinationMode.WALLET else remittance.destinationMode,
        isRemoteMode = isRemoteMode,
        selectedAccountName = remoteAccount?.name ?: selectedAccount.name,
        selectedAccountNumber = remoteAccount?.number ?: selectedAccount.number,
        selectedAccountBalanceText = remoteAccount?.balanceText ?: formatKrw(selectedAccount.balance),
        selectedRecipientName = selectedRecipientName,
        selectedRecipientAccountLabel = selectedRecipient?.let { buildRecipientAccountLabel(it.address) }
            ?: "계좌를 선택해 주세요",
        selectedRecipientWalletLabel = selectedRecipient?.let { shortenWalletAddress(it.address) }
            ?: "받는 지갑을 선택해 주세요",
        selectedRecipientWalletFullLabel = selectedRecipient?.address ?: "받는 지갑을 선택해 주세요",
        amountUsd = amountUsd.toString(),
        confirmationAmountText = if (!isRemoteMode && remittance.destinationMode == TransferDestinationMode.ACCOUNT) {
            formatKrw(amountKrw)
        } else {
            "$$amountUsd USDC"
        },
        accountStepHintText = if (isRemoteMode) {
            "DonDone 지갑 잔액으로 dUSDC를 송금해요."
        } else {
            "송금할 계좌를 선택해 주세요."
        },
        canSubmit = canSubmit,
        showReviewScreen = remittance.status == TransferStatus.REVIEWING && !actionUiState.isSubmitting,
        showTrackerScreen = (remittance.status == TransferStatus.REVIEWING && actionUiState.isSubmitting) ||
            remittance.status == TransferStatus.SUBMITTED ||
            remittance.status == TransferStatus.CONFIRMED ||
            remittance.status == TransferStatus.FAILED,
        remoteGate = remoteGate,
        reviewNotice = reviewNotice,
        trackerDetailText = trackerDetailText,
        trackerTxHashText = activeTransfer?.txHash,
        recipientScreenTitle = if (isRemoteMode) "받을 지갑을 선택해 주세요" else "받을 계좌를 선택해 주세요",
        recipientSearchPlaceholderText = if (isRemoteMode) "지갑 주소 검색" else "계좌명 입력",
        showAddRecipientAction = isRemoteMode,
        addRecipientPhoneDirectory = if (isRemoteMode) {
            emptyList()
        } else {
            buildDemoRecipientDirectory(remittance.recipients.map { it.address })
        },
        addRecipientSupportsRemotePhoneSearch = isRemoteMode,
        addRecipientPhoneSearchResults = if (isRemoteMode) {
            recipientPhoneSearchUiState.results.map { candidate ->
                RecipientDirectoryContactUiModel(
                    id = "search-${candidate.candidateUserId}",
                    name = candidate.displayName,
                    maskedPhoneNumber = candidate.maskedPhoneNumber,
                    searchablePhoneNumber = "",
                    walletAddress = null,
                    walletAddressLabel = candidate.walletAddressMasked,
                    candidateUserId = candidate.candidateUserId,
                    alreadyRegistered = candidate.alreadyRegistered
                )
            }
        } else {
            emptyList()
        },
        addRecipientPhoneSearchLoading = isRemoteMode && recipientPhoneSearchUiState.isLoading,
        addRecipientPhoneSearchErrorMessage = if (isRemoteMode) {
            recipientPhoneSearchUiState.errorMessage
        } else {
            null
        },
        accounts = remoteAccount?.let(::listOf) ?: remittance.accounts.map { account ->
            TransferAccountUiModel(
                id = account.id,
                name = account.name,
                number = account.number,
                balanceText = formatKrw(account.balance),
                selected = account.id == remittance.selectedAccountId
            )
        },
        recipientSections = buildRecipientSections(recipientItems)
    )
}

private fun buildRecipientSections(
    recipients: List<TransferRecipientUiModel>
): List<TransferRecipientSectionUiModel> {
    if (recipients.isEmpty()) return emptyList()
    if (recipients.size == 1) {
        return listOf(
            TransferRecipientSectionUiModel(
                title = "최근 수신 지갑",
                items = recipients
            )
        )
    }

    val frequent = recipients.take(1)
    val recent = recipients.drop(1)

    return buildList {
        if (frequent.isNotEmpty()) {
            add(TransferRecipientSectionUiModel(title = "자주 쓰는 지갑", items = frequent))
        }
        if (recent.isNotEmpty()) {
            add(TransferRecipientSectionUiModel(title = "최근 수신 지갑", items = recent))
        }
    }
}

private fun buildRecipientAccountLabel(address: String): String = shortenWalletAddress(address)

private fun recipientTone(index: Int): TransferRecipientTone =
    when (index % 4) {
        0 -> TransferRecipientTone.Amber
        1 -> TransferRecipientTone.Blue
        2 -> TransferRecipientTone.Indigo
        else -> TransferRecipientTone.Teal
    }

private fun resolveRemoteGate(remoteState: RemittanceRemoteState): TransferRemoteGateUiModel? {
    return when (remoteState.mode) {
        RemittanceRemoteMode.LOADING -> TransferRemoteGateUiModel(
            title = "송금 지갑 정보를 불러오는 중",
            description = "연결된 지갑과 잔액 정보를 확인하고 있어요.",
            isLoading = true
        )

        RemittanceRemoteMode.ERROR -> TransferRemoteGateUiModel(
            title = "송금 정보를 불러오지 못했어요",
            description = remoteState.errorMessage ?: "잠시 후 다시 시도해 주세요.",
            actionText = "다시 시도"
        )

        RemittanceRemoteMode.CONTENT -> {
            val wallet = remoteState.payload?.wallet ?: return null
            when (wallet.fundingStatus) {
                "PENDING" -> TransferRemoteGateUiModel(
                    title = "지갑 준비가 진행 중이에요",
                    description = "초기 자금 충전이 끝나면 바로 송금할 수 있어요.",
                    actionText = "상태 확인"
                )

                "FAILED" -> TransferRemoteGateUiModel(
                    title = "지갑 준비에 실패했어요",
                    description = wallet.fundingFailureReason ?: "잠시 후 다시 시도해 주세요.",
                    actionText = "다시 시도"
                )

                else -> null
            }
        }

        RemittanceRemoteMode.UNAUTHENTICATED -> null
    }
}

private fun com.dondone.mobile.data.remittance.RemittanceTransferPrecheckPayload.toReviewNotice(): TransferReviewNoticeUiModel? {
    return when (policyCode) {
        "RECENT_RECIPIENT_CONFIRMATION_REQUIRED" -> TransferReviewNoticeUiModel(
            title = "최근 수정된 수신자예요",
            description = "지갑 주소를 한 번 더 확인한 뒤 송금해 주세요."
        )

        "HIGH_AMOUNT_CONFIRMATION_REQUIRED" -> TransferReviewNoticeUiModel(
            title = "고액 송금 확인이 필요해요",
            description = "송금 금액과 수신자 정보를 다시 확인해 주세요."
        )

        else -> null
    }
}

private fun com.dondone.mobile.data.remittance.RemittanceWalletBalancePayload.formatTokenBalance(): String {
    val normalized = tokenBalanceAtomic.toBigDecimalOrNull()
        ?.movePointLeft(assetDecimals)
        ?.setScale(2, RoundingMode.DOWN)
        ?: return "잔액 확인 중"
    return "${normalized.stripTrailingZeros().toPlainString()} $assetSymbol"
}

private fun String.toTrackerDetailText(): String = when (uppercase(Locale.ROOT)) {
    "REQUESTED" -> "송금 요청이 생성되었어요."
    "SIGNED" -> "송금 서명을 준비하고 있어요."
    "BROADCASTED" -> "블록체인 전송 결과를 기다리고 있어요."
    "CONFIRMED" -> "송금이 정상적으로 완료되었어요."
    "FAILED" -> "송금을 완료하지 못했어요."
    "TIMED_OUT" -> "블록체인 확인 시간이 지연되고 있어요."
    else -> "송금 진행 상태를 확인하고 있어요."
}

private fun shortenWalletAddress(address: String): String {
    return if (address.length <= 14) {
        address
    } else {
        "${address.take(8)}...${address.takeLast(6)}"
    }
}
