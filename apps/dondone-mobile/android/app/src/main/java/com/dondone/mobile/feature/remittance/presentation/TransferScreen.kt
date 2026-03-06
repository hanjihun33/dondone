package com.dondone.mobile.feature.remittance.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.designsystem.DonDoneCard
import com.dondone.mobile.core.designsystem.MetricRow
import com.dondone.mobile.core.designsystem.PrimaryActionButton
import com.dondone.mobile.core.designsystem.SectionPanel
import com.dondone.mobile.core.designsystem.SecondaryActionButton
import com.dondone.mobile.core.designsystem.StatusBadge
import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus

@Composable
fun TransferScreen(
    uiModel: TransferUiModel,
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
        DonDoneCard(
            kicker = "송금",
            title = uiModel.stepTitle
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = uiModel.stepDescription,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatusBadge(
                    text = uiModel.stepBadgeText,
                    tone = BadgeTone.Info
                )
            }

            if (uiModel.flowStep == TransferFlowStep.RECIPIENT) {
                uiModel.recipients.forEach { recipient ->
                    SectionPanel(
                        modifier = Modifier.clickable { onSelectRecipient(recipient.id) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = recipient.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = recipient.address,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (recipient.selected) {
                                StatusBadge(text = "선택됨", tone = BadgeTone.Info)
                            }
                        }
                    }
                }
                SecondaryActionButton(text = "계좌 다시 선택", onClick = onChangeAccount)
            } else {
                SectionPanel {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "받는 사람", style = MaterialTheme.typography.labelMedium)
                            Text(text = uiModel.selectedRecipientName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = uiModel.selectedRecipientAddress,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        SecondaryActionButton(text = "변경", onClick = onChangeRecipient)
                    }
                }

                SectionPanel {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "보내는 계좌", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = "${uiModel.selectedAccountName} · ${uiModel.selectedAccountNumber}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "잔액 ${uiModel.selectedAccountBalanceText}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        SecondaryActionButton(text = "변경", onClick = onChangeAccount)
                    }
                }

                SectionPanel {
                    Text(text = "보낼 금액", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = uiModel.amountUsd,
                        onValueChange = { value ->
                            onUpdateAmount(value.filter(Char::isDigit).ifBlank { "0" }.toInt())
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        prefix = { Text(text = "$") },
                        suffix = { Text(text = "USDC") },
                        singleLine = true
                    )
                    Text(
                        text = uiModel.amountSummaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PrimaryActionButton(
                        text = uiModel.primaryActionText,
                        enabled = uiModel.transferStatus != TransferStatus.IDLE || uiModel.canSubmit,
                        onClick = when (uiModel.transferStatus) {
                            TransferStatus.IDLE -> onSubmitTransfer
                            TransferStatus.SUBMITTED -> onConfirmTransfer
                            TransferStatus.CONFIRMED -> onResetTransfer
                        }
                    )
                }
            }
        }

        if (uiModel.showStatusCard) {
            DonDoneCard(kicker = "상태", title = "송금 진행 상태") {
                StatusBadge(
                    text = uiModel.status.label,
                    tone = uiModel.status.tone
                )
                MetricRow(
                    leftLabel = "보내는 계좌",
                    leftValue = uiModel.selectedAccountName,
                    rightLabel = "선택 수신자",
                    rightValue = uiModel.selectedRecipientName
                )
                SectionPanel {
                    Text(text = "tx hash", style = MaterialTheme.typography.labelMedium)
                    Text(text = uiModel.txHash, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "등록된 수신자만 전송 가능하며, 현재는 데모 환경이라 실제 자금 이동은 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
