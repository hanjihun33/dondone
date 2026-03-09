package com.dondone.mobile.feature.remittance.presentation

import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus

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
    val selected: Boolean
)

data class TransferStatusUiModel(
    val label: String,
    val tone: BadgeTone
)

data class TransferUiModel(
    val flowStep: TransferFlowStep,
    val transferStatus: TransferStatus,
    val selectedAccountName: String,
    val selectedAccountNumber: String,
    val selectedAccountBalanceText: String,
    val selectedRecipientName: String,
    val selectedRecipientAddress: String,
    val amountUsd: String,
    val amountSummaryText: String,
    val accountStepHintText: String,
    val canSubmit: Boolean,
    val showStatusCard: Boolean,
    val primaryActionText: String,
    val accounts: List<TransferAccountUiModel>,
    val recipients: List<TransferRecipientUiModel>,
    val status: TransferStatusUiModel,
    val txHash: String
)

fun DemoState.toTransferUiModel(): TransferUiModel {
    val selectedAccount = remittance.selectedAccount()
    val selectedRecipient = remittance.selectedRecipientOrNull()
    val amountUsd = remittance.draftAmountUsd
    val amountKrw = amountUsd * 1_450
    val canSubmit = amountUsd > 0 && amountKrw <= selectedAccount.balance

    return TransferUiModel(
        flowStep = remittance.flowStep,
        transferStatus = remittance.status,
        selectedAccountName = selectedAccount.name,
        selectedAccountNumber = selectedAccount.number,
        selectedAccountBalanceText = formatKrw(selectedAccount.balance),
        selectedRecipientName = selectedRecipient?.name ?: "선택 전",
        selectedRecipientAddress = selectedRecipient?.address ?: "등록된 수신자를 선택해 주세요",
        amountUsd = amountUsd.toString(),
        amountSummaryText = "${selectedAccount.name} → ${selectedRecipient?.name ?: "선택 전"} · ${formatKrw(amountKrw)}",
        accountStepHintText = "먼저 송금에 사용할 계좌를 선택해 주세요.",
        canSubmit = canSubmit,
        showStatusCard = remittance.status != TransferStatus.IDLE,
        primaryActionText = when (remittance.status) {
            TransferStatus.IDLE -> "송금 제출"
            TransferStatus.SUBMITTED -> "확인 완료로 전환"
            TransferStatus.CONFIRMED -> "새 송금 준비"
        },
        accounts = remittance.accounts.map { account ->
            TransferAccountUiModel(
                id = account.id,
                name = account.name,
                number = account.number,
                balanceText = formatKrw(account.balance),
                selected = account.id == remittance.selectedAccountId
            )
        },
        recipients = remittance.recipients.map { recipient ->
            TransferRecipientUiModel(
                id = recipient.id,
                name = recipient.name,
                address = recipient.address,
                selected = recipient.id == remittance.selectedRecipientId
            )
        },
        status = TransferStatusUiModel(
            label = when (remittance.status) {
                TransferStatus.IDLE -> "대기"
                TransferStatus.SUBMITTED -> "전송 제출"
                TransferStatus.CONFIRMED -> "확인 완료"
            },
            tone = when (remittance.status) {
                TransferStatus.IDLE -> BadgeTone.Info
                TransferStatus.SUBMITTED -> BadgeTone.Warning
                TransferStatus.CONFIRMED -> BadgeTone.Success
            }
        ),
        txHash = remittance.txHash
    )
}
