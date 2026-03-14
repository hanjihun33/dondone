package com.dondone.mobile.feature.remittance.presentation

import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.TransferDestinationMode
import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus

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

data class TransferUiModel(
    val flowStep: TransferFlowStep,
    val transferStatus: TransferStatus,
    val destinationMode: TransferDestinationMode,
    val selectedAccountName: String,
    val selectedAccountNumber: String,
    val selectedAccountBalanceText: String,
    val selectedRecipientName: String,
    val selectedRecipientAccountLabel: String,
    val selectedRecipientWalletLabel: String,
    val amountUsd: String,
    val confirmationAmountText: String,
    val accountStepHintText: String,
    val canSubmit: Boolean,
    val showReviewScreen: Boolean,
    val showTrackerScreen: Boolean,
    val recipientScreenTitle: String,
    val recipientSearchPlaceholderText: String,
    val accounts: List<TransferAccountUiModel>,
    val recipientSections: List<TransferRecipientSectionUiModel>
)

fun DemoState.toTransferUiModel(): TransferUiModel {
    val selectedAccount = remittance.selectedAccount()
    val selectedRecipient = remittance.selectedRecipientOrNull()
    val selectedRecipientName = selectedRecipient?.let { remittance.displayedRecipientName() } ?: "수신자를 선택해 주세요"
    val amountUsd = remittance.draftAmountUsd
    val amountKrw = amountUsd * 1_450
    val canSubmit = amountUsd > 0 && amountKrw <= selectedAccount.balance
    val recipientItems = remittance.recipients.mapIndexed { index, recipient ->
        TransferRecipientUiModel(
            id = recipient.id,
            name = recipient.name,
            address = recipient.address,
            relationship = recipient.relationship,
            accountLabel = buildRecipientAccountLabel(recipient.address),
            contactLabel = "${recipient.relationship} · ${recipient.address}",
            tone = recipientTone(index),
            selected = recipient.id == remittance.selectedRecipientId
        )
    }

    return TransferUiModel(
        flowStep = remittance.flowStep,
        transferStatus = remittance.status,
        destinationMode = remittance.destinationMode,
        selectedAccountName = selectedAccount.name,
        selectedAccountNumber = selectedAccount.number,
        selectedAccountBalanceText = formatKrw(selectedAccount.balance),
        selectedRecipientName = selectedRecipientName,
        selectedRecipientAccountLabel = selectedRecipient?.let { buildRecipientAccountLabel(it.address) }
            ?: "계좌를 선택해 주세요",
        selectedRecipientWalletLabel = selectedRecipient?.address ?: "등록된 지갑 주소가 있는 수신자를 선택해 주세요",
        amountUsd = amountUsd.toString(),
        confirmationAmountText = if (remittance.destinationMode == TransferDestinationMode.ACCOUNT) {
            formatKrw(amountKrw)
        } else {
            "$amountUsd USDC"
        },
        accountStepHintText = "먼저 송금에 사용할 계좌를 선택해 주세요.",
        canSubmit = canSubmit,
        showReviewScreen = remittance.status == TransferStatus.REVIEWING,
        showTrackerScreen = remittance.status == TransferStatus.SUBMITTED || remittance.status == TransferStatus.CONFIRMED,
        recipientScreenTitle = "어디로 돈을 보낼까요?",
        recipientSearchPlaceholderText = "계좌번호 입력",
        accounts = remittance.accounts.map { account ->
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
    val normalized = address.removePrefix("0x")
    return if (normalized.length <= 12) {
        normalized
    } else {
        normalized.chunked(4).joinToString("-")
    }
}

private fun recipientTone(index: Int): TransferRecipientTone =
    when (index % 4) {
        0 -> TransferRecipientTone.Amber
        1 -> TransferRecipientTone.Blue
        2 -> TransferRecipientTone.Indigo
        else -> TransferRecipientTone.Teal
    }
