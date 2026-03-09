package com.dondone.mobile.feature.remittance.presentation

import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus

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
    val stepTitle: String,
    val stepDescription: String,
    val stepBadgeText: String,
    val flowStep: TransferFlowStep,
    val transferStatus: TransferStatus,
    val selectedAccountName: String,
    val selectedAccountNumber: String,
    val selectedAccountBalanceText: String,
    val selectedRecipientName: String,
    val selectedRecipientAddress: String,
    val amountUsd: String,
    val amountSummaryText: String,
    val canSubmit: Boolean,
    val showStatusCard: Boolean,
    val primaryActionText: String,
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
        stepTitle = if (remittance.flowStep == TransferFlowStep.RECIPIENT) "받는 사람 선택" else "금액 입력",
        stepDescription = if (remittance.flowStep == TransferFlowStep.RECIPIENT) {
            "등록된 수신자 중 한 명을 고르면 금액 입력으로 이어집니다."
        } else {
            "받는 사람, 보내는 계좌, 금액을 함께 확인한 뒤 테스트넷 송금을 진행합니다."
        },
        stepBadgeText = if (remittance.flowStep == TransferFlowStep.RECIPIENT) "STEP 2" else "STEP 3",
        flowStep = remittance.flowStep,
        transferStatus = remittance.status,
        selectedAccountName = selectedAccount.name,
        selectedAccountNumber = selectedAccount.number,
        selectedAccountBalanceText = formatKrw(selectedAccount.balance),
        selectedRecipientName = selectedRecipient?.name ?: "선택 전",
        selectedRecipientAddress = selectedRecipient?.address ?: "등록된 수신자를 선택해 주세요",
        amountUsd = amountUsd.toString(),
        amountSummaryText = "${selectedAccount.name} → ${selectedRecipient?.name ?: "선택 전"} · ${formatKrw(amountKrw)}",
        canSubmit = canSubmit,
        showStatusCard = remittance.status != TransferStatus.IDLE,
        primaryActionText = when (remittance.status) {
            TransferStatus.IDLE -> "송금 제출"
            TransferStatus.SUBMITTED -> "확인 완료로 전환"
            TransferStatus.CONFIRMED -> "새 송금 준비"
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
