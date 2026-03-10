package com.dondone.mobile.feature.remittance.presentation

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
    val confirmationAccountText: String,
    val confirmationAmountText: String,
    val accountStepHintText: String,
    val canSubmit: Boolean,
    val showConfirmationSheet: Boolean,
    val showTrackerScreen: Boolean,
    val primaryActionText: String,
    val confirmationTitleText: String,
    val confirmationSubtitleText: String,
    val confirmationChecks: List<String>,
    val confirmationDisclaimerText: String,
    val trackerHeadlineText: String,
    val trackerSupportingText: String,
    val trackerSubmittedTitleText: String,
    val trackerSubmittedDetailText: String,
    val trackerConfirmedTitleText: String,
    val trackerConfirmedDetailText: String,
    val accounts: List<TransferAccountUiModel>,
    val recipients: List<TransferRecipientUiModel>,
    val txHash: String
)

fun DemoState.toTransferUiModel(): TransferUiModel {
    val selectedAccount = remittance.selectedAccount()
    val selectedRecipient = remittance.selectedRecipientOrNull()
    val selectedRecipientName = selectedRecipient?.name ?: "수신자를 선택해 주세요"
    val selectedRecipientAddress = selectedRecipient?.address ?: "등록된 지갑 주소가 있는 수신자를 선택해 주세요"
    val amountUsd = remittance.draftAmountUsd
    val amountKrw = amountUsd * 1_450
    val canSubmit = amountUsd > 0 && amountKrw <= selectedAccount.balance
    val isTrackerVisible = remittance.status == TransferStatus.SUBMITTED || remittance.status == TransferStatus.CONFIRMED

    val trackerHeadlineText = when (remittance.status) {
        TransferStatus.SUBMITTED -> "송금을 네트워크에 제출했습니다"
        TransferStatus.CONFIRMED -> "송금이 네트워크에서 확인되었습니다"
        else -> "송금 상태를 준비 중입니다"
    }
    val trackerSupportingText = when (remittance.status) {
        TransferStatus.SUBMITTED -> "테스트넷 처리 중입니다. 확인까지 잠시만 기다려 주세요."
        TransferStatus.CONFIRMED -> "확인 완료 후 잔액이 반영되었습니다. 필요하면 새 송금을 시작할 수 있습니다."
        else -> "송금 상태를 확인할 수 없습니다."
    }

    return TransferUiModel(
        flowStep = remittance.flowStep,
        transferStatus = remittance.status,
        selectedAccountName = selectedAccount.name,
        selectedAccountNumber = selectedAccount.number,
        selectedAccountBalanceText = formatKrw(selectedAccount.balance),
        selectedRecipientName = selectedRecipientName,
        selectedRecipientAddress = selectedRecipientAddress,
        amountUsd = amountUsd.toString(),
        amountSummaryText = "${selectedAccount.name} · $selectedRecipientName · ${formatKrw(amountKrw)}",
        confirmationAccountText = "${selectedAccount.name} · ${selectedAccount.number}",
        confirmationAmountText = "$amountUsd USDC",
        accountStepHintText = "먼저 송금에 사용할 계좌를 선택해 주세요.",
        canSubmit = canSubmit,
        showConfirmationSheet = remittance.status == TransferStatus.REVIEWING,
        showTrackerScreen = isTrackerVisible,
        primaryActionText = "송금 전 확인",
        confirmationTitleText = "송금 전 확인",
        confirmationSubtitleText = "이용 목록, 금액, 지갑 주소를 확인한 뒤 테스트넷 송금을 진행하세요.",
        confirmationChecks = listOf(
            "등록한 수신자 지갑 주소로만 송금할 수 있습니다.",
            "현재는 테스트넷 데모라 실제 자금 이동은 일어나지 않습니다."
        ),
        confirmationDisclaimerText = "확인 후 보내기를 눌러도 실제 원화나 외화 자금은 이동하지 않습니다.",
        trackerHeadlineText = trackerHeadlineText,
        trackerSupportingText = trackerSupportingText,
        trackerSubmittedTitleText = "SUBMITTED",
        trackerSubmittedDetailText = when (remittance.status) {
            TransferStatus.SUBMITTED -> "Submitted to network"
            TransferStatus.CONFIRMED -> "Network submission completed"
            else -> "Ready to submit"
        },
        trackerConfirmedTitleText = "CONFIRMED",
        trackerConfirmedDetailText = when (remittance.status) {
            TransferStatus.SUBMITTED -> "Confirming..."
            TransferStatus.CONFIRMED -> "Confirmed on testnet"
            else -> "Not confirmed yet"
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
        txHash = remittance.txHash
    )
}
