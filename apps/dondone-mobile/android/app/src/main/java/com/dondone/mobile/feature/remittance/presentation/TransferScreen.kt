package com.dondone.mobile.feature.remittance.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dondone.mobile.core.designsystem.DawnBorder
import com.dondone.mobile.core.designsystem.DawnPrimary
import com.dondone.mobile.core.designsystem.DawnSurface
import com.dondone.mobile.core.designsystem.DawnTextSubtle
import com.dondone.mobile.core.designsystem.PrimaryActionButton
import com.dondone.mobile.core.designsystem.SecondaryActionButton
import com.dondone.mobile.core.designsystem.SectionPanel
import com.dondone.mobile.core.designsystem.StatusBadge
import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus

@Composable
fun TransferScreen(
    uiModel: TransferUiModel,
    onSelectAccount: (String) -> Unit,
    onSelectRecipient: (String) -> Unit,
    onUpdateAmount: (Int) -> Unit,
    onChangeRecipient: () -> Unit,
    onChangeAccount: () -> Unit,
    onSubmitTransfer: () -> Unit,
    onConfirmTransfer: () -> Unit,
    onResetTransfer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (uiModel.flowStep) {
            TransferFlowStep.ACCOUNT -> AccountStepCard(
                uiModel = uiModel,
                onSelectAccount = onSelectAccount
            )

            TransferFlowStep.RECIPIENT -> RecipientStepCard(
                uiModel = uiModel,
                onSelectRecipient = onSelectRecipient,
                onChangeAccount = onChangeAccount
            )

            TransferFlowStep.AMOUNT -> AmountStepCard(
                uiModel = uiModel,
                onUpdateAmount = onUpdateAmount,
                onChangeRecipient = onChangeRecipient,
                onChangeAccount = onChangeAccount,
                onSubmitTransfer = onSubmitTransfer,
                onConfirmTransfer = onConfirmTransfer,
                onResetTransfer = onResetTransfer
            )
        }

        if (uiModel.showStatusCard) {
            TransferStatusCard(uiModel)
        }
    }
}

@Composable
private fun AccountStepCard(
    uiModel: TransferUiModel,
    onSelectAccount: (String) -> Unit
) {
    TransferContainerCard {
        Text(
            text = "보내는 계좌",
            style = MaterialTheme.typography.labelLarge,
            color = DawnTextSubtle
        )
        uiModel.accounts.forEach { account ->
            TransferRow(
                title = account.name,
                subtitle = "${account.number} · ${account.balanceText}",
                selected = account.selected,
                onClick = { onSelectAccount(account.id) }
            )
        }
        Text(
            text = uiModel.accountStepHintText,
            style = MaterialTheme.typography.bodySmall,
            color = DawnTextSubtle
        )
    }
}

@Composable
private fun RecipientStepCard(
    uiModel: TransferUiModel,
    onSelectRecipient: (String) -> Unit,
    onChangeAccount: () -> Unit
) {
    TransferContainerCard {
        Text(
            text = "받는 사람",
            style = MaterialTheme.typography.labelLarge,
            color = DawnTextSubtle
        )
        uiModel.recipients.forEach { recipient ->
            TransferRow(
                title = recipient.name,
                subtitle = recipient.address,
                selected = recipient.selected,
                onClick = { onSelectRecipient(recipient.id) }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "수신자를 선택하면 금액 입력 화면으로 이동합니다.",
                style = MaterialTheme.typography.bodySmall,
                color = DawnTextSubtle
            )
            Text(
                text = "계좌 변경",
                style = MaterialTheme.typography.labelLarge,
                color = DawnPrimary,
                modifier = Modifier.clickable(onClick = onChangeAccount)
            )
        }
    }
}

@Composable
private fun AmountStepCard(
    uiModel: TransferUiModel,
    onUpdateAmount: (Int) -> Unit,
    onChangeRecipient: () -> Unit,
    onChangeAccount: () -> Unit,
    onSubmitTransfer: () -> Unit,
    onConfirmTransfer: () -> Unit,
    onResetTransfer: () -> Unit
) {
    TransferContainerCard {
        SimpleInfoSection(
            label = "받는 사람",
            title = uiModel.selectedRecipientName,
            description = uiModel.selectedRecipientAddress,
            actionText = "변경",
            onAction = onChangeRecipient
        )
        SimpleInfoSection(
            label = "보내는 계좌",
            title = "${uiModel.selectedAccountName} · ${uiModel.selectedAccountNumber}",
            description = uiModel.selectedAccountBalanceText,
            actionText = "변경",
            onAction = onChangeAccount
        )
        SectionPanel {
            Text(
                text = "보낼 금액",
                style = MaterialTheme.typography.labelMedium,
                color = DawnTextSubtle
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiModel.amountUsd,
                onValueChange = { value ->
                    onUpdateAmount(value.filter(Char::isDigit).ifBlank { "0" }.toInt())
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                prefix = { Text(text = "$") },
                suffix = { Text(text = "USDC") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp)
            )
            Text(
                text = uiModel.amountSummaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = DawnTextSubtle
            )
            PrimaryActionButton(
                text = uiModel.primaryActionText,
                enabled = uiModel.transferStatus != TransferStatus.IDLE || uiModel.canSubmit,
                modifier = Modifier.fillMaxWidth(),
                onClick = when (uiModel.transferStatus) {
                    TransferStatus.IDLE -> onSubmitTransfer
                    TransferStatus.SUBMITTED -> onConfirmTransfer
                    TransferStatus.CONFIRMED -> onResetTransfer
                }
            )
        }
    }
}

@Composable
private fun TransferStatusCard(uiModel: TransferUiModel) {
    SectionPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "송금 진행 상태",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "등록된 수신자만 전송 가능하며, 현재는 데모 환경이라 실제 자금 이동은 없습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DawnTextSubtle
                )
            }
            StatusBadge(text = uiModel.status.label, tone = uiModel.status.tone)
        }
        SimpleMetaRow(label = "보내는 계좌", value = uiModel.selectedAccountName)
        SimpleMetaRow(label = "선택 수신자", value = uiModel.selectedRecipientName)
        SimpleMetaRow(label = "tx hash", value = uiModel.txHash)
    }
}

@Composable
private fun SimpleInfoSection(
    label: String,
    title: String,
    description: String,
    actionText: String,
    onAction: () -> Unit
) {
    SectionPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = DawnTextSubtle
            )
            SecondaryActionButton(text = actionText, onClick = onAction)
        }
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = DawnTextSubtle
        )
    }
}

@Composable
private fun SimpleMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = DawnTextSubtle
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun TransferContainerCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(28.dp))
            .border(1.dp, DawnBorder, RoundedCornerShape(28.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        content()
    }
}

@Composable
private fun TransferRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DawnSurface, RoundedCornerShape(22.dp))
            .border(
                1.dp,
                if (selected) DawnPrimary.copy(alpha = 0.7f) else DawnBorder,
                RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) DawnPrimary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = DawnTextSubtle
            )
        }
    }
}
