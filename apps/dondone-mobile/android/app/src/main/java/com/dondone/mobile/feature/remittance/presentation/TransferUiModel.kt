package com.dondone.mobile.feature.remittance.presentation

import com.dondone.mobile.app.session.RemittanceActionUiState
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.data.remittance.RemittanceRemoteMode
import com.dondone.mobile.data.remittance.RemittanceRemoteState
import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.TransferDestinationMode
import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus
import java.math.BigDecimal
import java.math.RoundingMode

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
    val accounts: List<TransferAccountUiModel>,
    val recipientSections: List<TransferRecipientSectionUiModel>
)

fun DemoState.toTransferUiModel(
    remoteState: RemittanceRemoteState,
    actionUiState: RemittanceActionUiState,
    isAuthenticated: Boolean
): TransferUiModel {
    val isRemoteMode = isAuthenticated
    val remotePayload = remoteState.payload
    val remoteAccount = if (isRemoteMode && remotePayload != null) {
        TransferAccountUiModel(
            id = REMITTANCE_WALLET_ID,
            name = "DonDone Wallet",
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
        amountUsd > 0 && remotePayload?.wallet?.fundingStatus == "FUNDED" &&
            remoteBalanceAtomic != null && amountAtomic <= remoteBalanceAtomic
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
    val remoteGate = if (isRemoteMode) {
        resolveRemoteGate(remoteState)
    } else {
        null
    }
    val reviewNotice = actionUiState.precheck?.toReviewNotice()
    val activeTransfer = remotePayload?.activeTransfer
    val trackerDetailText = when {
        remittance.status == TransferStatus.REVIEWING && actionUiState.isSubmitting -> "송금 요청을 보내는 중이에요."
        remittance.status == TransferStatus.CONFIRMED -> "테스트넷 확인이 완료됐어요."
        remittance.status == TransferStatus.FAILED -> {
            activeTransfer?.failureCode?.let { "전송이 실패했어요. 사유: $it" } ?: "전송을 완료하지 못했어요."
        }
        else -> activeTransfer?.status?.toTrackerDetailText() ?: "테스트넷 네트워크에 전송 중이에요."
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
            ?: "등록된 지갑 주소가 있는 수신자를 선택해 주세요",
        selectedRecipientWalletFullLabel = selectedRecipient?.address
            ?: "등록된 지갑 주소가 있는 수신자를 선택해 주세요",
        amountUsd = amountUsd.toString(),
        confirmationAmountText = if (!isRemoteMode && remittance.destinationMode == TransferDestinationMode.ACCOUNT) {
            formatKrw(amountKrw)
        } else {
            "${'$'}$amountUsd USDC"
        },
        accountStepHintText = if (isRemoteMode) {
            "DonDone 서버 지갑에서 테스트넷 dUSDC를 보냅니다."
        } else {
            "먼저 송금에 사용할 계좌를 선택해 주세요."
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
        recipientScreenTitle = if (isRemoteMode) "어느 지갑으로 보낼까요?" else "어디로 돈을 보낼까요?",
        recipientSearchPlaceholderText = if (isRemoteMode) "지갑 주소 검색" else "계좌번호 입력",
        showAddRecipientAction = isRemoteMode,
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
            TransferRecipientSectionUiModel(title = "최근 보낸 지갑", items = recipients)
        )
    }

    val frequent = recipients.take(1)
    val recent = recipients.drop(1)

    return buildList {
        if (frequent.isNotEmpty()) {
            add(TransferRecipientSectionUiModel(title = "자주 보내는 지갑", items = frequent))
        }
        if (recent.isNotEmpty()) {
            add(TransferRecipientSectionUiModel(title = "최근 보낸 지갑", items = recent))
        }
    }
}

private fun buildRecipientAccountLabel(address: String): String {
    return shortenWalletAddress(address)
}

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
            title = "송금 지갑을 준비 중이에요",
            description = "서버 지갑과 허용 목록을 불러오고 있어요.",
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
                    title = "지갑 시드 자금을 준비 중이에요",
                    description = "초기 테스트넷 토큰 지급이 끝나면 바로 송금할 수 있어요.",
                    actionText = "새로고침"
                )

                "FAILED" -> TransferRemoteGateUiModel(
                    title = "지갑 준비에 실패했어요",
                    description = wallet.fundingFailureReason ?: "다시 시도해 주세요.",
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
            description = "지갑 주소를 한 번 더 확인한 뒤 보내야 해요."
        )

        "HIGH_AMOUNT_CONFIRMATION_REQUIRED" -> TransferReviewNoticeUiModel(
            title = "고액 송금 재확인이 필요해요",
            description = "테스트넷 송금이지만 금액과 수신자를 다시 확인해 주세요."
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

private fun String.toTrackerDetailText(): String = when (this) {
    "REQUESTED" -> "송금 요청이 생성됐어요."
    "SIGNED" -> "전송 서명을 준비했어요."
    "BROADCASTED" -> "네트워크에 전송했어요. 확인을 기다리는 중이에요."
    "CONFIRMED" -> "테스트넷 확인이 완료됐어요."
    "FAILED" -> "전송을 완료하지 못했어요."
    "TIMED_OUT" -> "네트워크 확인이 지연되고 있어요."
    else -> "테스트넷 네트워크에 전송 중이에요."
}

private fun shortenWalletAddress(address: String): String {
    return if (address.length <= 14) {
        address
    } else {
        "${address.take(8)}...${address.takeLast(6)}"
    }
}
