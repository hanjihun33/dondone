package com.dondone.mobile.feature.remittance.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dondone.mobile.core.designsystem.DawnBorder
import com.dondone.mobile.core.designsystem.DawnPrimary
import com.dondone.mobile.core.designsystem.DawnSurface
import com.dondone.mobile.core.designsystem.DawnText
import com.dondone.mobile.core.designsystem.DawnTextSubtle
import com.dondone.mobile.core.designsystem.PrimaryActionButton
import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus

private val TransferMutedCardBackground = Color(0xFFF8FAFC)
private val TransferMutedCardBorder = Color(0xFFE2E8F0)
private val TransferTrackerHeroBackground = Color(0xFFF2EEFF)
private val TransferTrackerHeroBorder = Color(0xFFE3D7FF)
private val TransferTrackerLineColor = Color(0xFFA99AF6)
private val TransferTrackerDarkSurface = Color(0xFF0F172A)
private val TransferTrackerPending = Color(0xFFE2E8F0)
private val TransferTrackerPendingText = Color(0xFF94A3B8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    uiModel: TransferUiModel,
    onSelectAccount: (String) -> Unit,
    onSelectRecipient: (String) -> Unit,
    onUpdateAmount: (Int) -> Unit,
    onChangeRecipient: () -> Unit,
    onChangeAccountFromRecipient: () -> Unit,
    onChangeAccountFromAmount: () -> Unit,
    onSubmitTransfer: () -> Unit,
    onDismissTransferConfirmation: () -> Unit,
    onConfirmTransfer: () -> Unit,
    onResetTransfer: () -> Unit
) {
    val confirmationSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (uiModel.showTrackerScreen) {
        TransferTrackerScreen(
            uiModel = uiModel,
            onResetTransfer = onResetTransfer
        )
    } else {
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
                    onChangeAccount = onChangeAccountFromRecipient
                )

                TransferFlowStep.AMOUNT -> AmountStepCard(
                    uiModel = uiModel,
                    onUpdateAmount = onUpdateAmount,
                    onChangeRecipient = onChangeRecipient,
                    onChangeAccount = onChangeAccountFromAmount,
                    onPrimaryAction = onSubmitTransfer
                )
            }
        }
    }

    if (uiModel.showConfirmationSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismissTransferConfirmation,
            sheetState = confirmationSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = null
        ) {
            TransferConfirmationSheet(
                uiModel = uiModel,
                onDismiss = onDismissTransferConfirmation,
                onConfirm = onConfirmTransfer
            )
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
            style = MaterialTheme.typography.labelMedium,
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
            style = MaterialTheme.typography.bodyMedium,
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
            style = MaterialTheme.typography.labelMedium,
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "수신자를 선택하면 금액 입력으로 이동합니다.",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                softWrap = false,
                color = DawnTextSubtle
            )
            Text(
                text = "계좌변경",
                style = MaterialTheme.typography.labelLarge,
                color = DawnPrimary,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .clickable(onClick = onChangeAccount)
                    .padding(vertical = 2.dp)
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
    onPrimaryAction: () -> Unit
) {
    TransferContainerCard {
        TransferInfoCard(
            label = "받는 사람",
            title = uiModel.selectedRecipientName,
            description = uiModel.selectedRecipientAddress,
            actionText = "변경",
            onAction = onChangeRecipient
        )
        TransferInfoCard(
            label = "보내는 계좌",
            title = uiModel.confirmationAccountText,
            description = uiModel.selectedAccountBalanceText,
            actionText = "변경",
            onAction = onChangeAccount
        )
        TransferAmountCard(
            amountUsd = uiModel.amountUsd,
            amountSummaryText = uiModel.amountSummaryText,
            onUpdateAmount = onUpdateAmount
        )
        PrimaryActionButton(
            text = uiModel.primaryActionText,
            enabled = uiModel.canSubmit,
            modifier = Modifier.fillMaxWidth(),
            onClick = onPrimaryAction
        )
    }
}

@Composable
private fun TransferTrackerScreen(
    uiModel: TransferUiModel,
    onResetTransfer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TransferTrackerHeroCard()

        TransferContainerCard {
            Text(
                text = uiModel.trackerHeadlineText,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = DawnText
            )
            Text(
                text = uiModel.trackerSupportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = DawnTextSubtle
            )
        }

        TransferContainerCard {
            TransferTrackerStep(
                title = uiModel.trackerSubmittedTitleText,
                subtitle = uiModel.trackerSubmittedDetailText,
                state = when (uiModel.transferStatus) {
                    TransferStatus.SUBMITTED -> TransferTrackerStepState.Active
                    TransferStatus.CONFIRMED -> TransferTrackerStepState.Completed
                    else -> TransferTrackerStepState.Pending
                },
                showConnector = true
            )
            TransferTrackerStep(
                title = uiModel.trackerConfirmedTitleText,
                subtitle = uiModel.trackerConfirmedDetailText,
                state = when (uiModel.transferStatus) {
                    TransferStatus.CONFIRMED -> TransferTrackerStepState.Completed
                    else -> TransferTrackerStepState.Pending
                },
                showConnector = false
            )
        }

        TransferMutedCard {
            TransferConfirmationRow(label = "보내는 계좌", value = uiModel.confirmationAccountText)
            TransferConfirmationRow(label = "받는 사람", value = uiModel.selectedRecipientName)
            TransferConfirmationRow(
                label = "지갑 주소",
                value = uiModel.selectedRecipientAddress,
                valueFontFamily = FontFamily.Monospace
            )
            TransferConfirmationRow(
                label = "보낼 금액",
                value = uiModel.confirmationAmountText,
                emphasized = true,
                withDivider = false
            )
        }

        TransferHashCard(txHash = uiModel.txHash)

        if (uiModel.transferStatus == TransferStatus.CONFIRMED) {
            PrimaryActionButton(
                text = "확인",
                modifier = Modifier.fillMaxWidth(),
                onClick = onResetTransfer
            )
        }
    }
}

@Composable
private fun TransferTrackerHeroCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(TransferTrackerHeroBackground, RoundedCornerShape(36.dp))
            .border(1.dp, TransferTrackerHeroBorder, RoundedCornerShape(36.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 30.dp)
        ) {
            val start = Offset(size.width * 0.14f, size.height * 0.74f)
            val control = Offset(size.width * 0.48f, size.height * 0.06f)
            val end = Offset(size.width * 0.86f, size.height * 0.38f)

            val path = Path().apply {
                moveTo(start.x, start.y)
                quadraticTo(control.x, control.y, end.x, end.y)
            }

            drawPath(
                path = path,
                color = TransferTrackerLineColor,
                style = Stroke(
                    width = 8f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))
                )
            )
            drawCircle(color = DawnPrimary, radius = 14f, center = start)
            drawCircle(color = TransferTrackerPending, radius = 14f, center = end)
        }

        Text(
            text = "HANOI",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 32.dp, end = 28.dp),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
            color = TransferTrackerPendingText
        )
        Text(
            text = "SEOUL",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 28.dp, bottom = 28.dp),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
            color = DawnText
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .background(Color.White.copy(alpha = 0.92f), CircleShape)
                .border(1.dp, TransferTrackerHeroBorder, CircleShape)
                .padding(18.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                tint = DawnPrimary,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@Composable
private fun TransferTrackerStep(
    title: String,
    subtitle: String,
    state: TransferTrackerStepState,
    showConnector: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransferTrackerStepIndicator(state = state)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                    color = DawnText
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (state == TransferTrackerStepState.Pending) {
                        TransferTrackerPendingText
                    } else {
                        DawnTextSubtle
                    }
                )
            }
        }
        if (showConnector) {
            Box(
                modifier = Modifier
                    .padding(start = 20.dp)
                    .size(width = 2.dp, height = 24.dp)
                    .background(TransferMutedCardBorder)
            )
        }
    }
}

@Composable
private fun TransferTrackerStepIndicator(state: TransferTrackerStepState) {
    when (state) {
        TransferTrackerStepState.Active -> Box(
            modifier = Modifier
                .size(40.dp)
                .background(TransferTrackerHeroBackground, CircleShape)
                .border(1.dp, TransferTrackerHeroBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(DawnPrimary, CircleShape)
            )
        }

        TransferTrackerStepState.Completed -> Box(
            modifier = Modifier
                .size(40.dp)
                .background(DawnPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White
            )
        }

        TransferTrackerStepState.Pending -> Box(
            modifier = Modifier
                .size(40.dp)
                .background(TransferTrackerPending, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = TransferTrackerPendingText
            )
        }
    }
}

@Composable
private fun TransferHashCard(txHash: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TransferTrackerDarkSurface, RoundedCornerShape(32.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "TX HASH",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
            color = Color.White.copy(alpha = 0.6f)
        )
        Text(
            text = txHash,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = Color.White.copy(alpha = 0.86f)
        )
    }
}

@Composable
private fun TransferInfoCard(
    label: String,
    title: String,
    description: String,
    actionText: String,
    onAction: () -> Unit
) {
    TransferMutedCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = DawnTextSubtle
            )
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                color = DawnPrimary,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = DawnText
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = DawnTextSubtle
        )
    }
}

@Composable
private fun TransferAmountCard(
    amountUsd: String,
    amountSummaryText: String,
    onUpdateAmount: (Int) -> Unit
) {
    TransferMutedCard {
        Text(
            text = "보낼 금액",
            style = MaterialTheme.typography.labelMedium,
            color = DawnTextSubtle
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = amountUsd,
            onValueChange = { value ->
                onUpdateAmount(value.filter(Char::isDigit).ifBlank { "0" }.toInt())
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            prefix = {
                Text(
                    text = "$",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = DawnPrimary
                )
            },
            suffix = {
                Text(
                    text = "USDC",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                    color = DawnTextSubtle
                )
            },
            textStyle = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Black,
                color = DawnText
            ),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                focusedTextColor = DawnText,
                unfocusedTextColor = DawnText,
                cursorColor = DawnPrimary
            )
        )
        Text(
            text = amountSummaryText,
            style = MaterialTheme.typography.bodyMedium,
            color = DawnTextSubtle
        )
    }
}

@Composable
private fun TransferConfirmationSheet(
    uiModel: TransferUiModel,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = uiModel.confirmationTitleText,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = DawnText
                )
                Text(
                    text = uiModel.confirmationSubtitleText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DawnTextSubtle
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "닫기",
                    tint = DawnTextSubtle
                )
            }
        }

        TransferMutedCard {
            TransferConfirmationRow(label = "보내는 계좌", value = uiModel.confirmationAccountText)
            TransferConfirmationRow(label = "받는 사람", value = uiModel.selectedRecipientName)
            TransferConfirmationRow(
                label = "지갑 주소",
                value = uiModel.selectedRecipientAddress,
                valueFontFamily = FontFamily.Monospace
            )
            TransferConfirmationRow(
                label = "보낼 금액",
                value = uiModel.confirmationAmountText,
                emphasized = true,
                withDivider = false
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TransferCheckItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = DawnPrimary
                    )
                },
                text = uiModel.confirmationChecks.first()
            )
            TransferCheckItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = DawnPrimary
                    )
                },
                text = uiModel.confirmationChecks.last()
            )
        }

        Text(
            text = uiModel.confirmationDisclaimerText,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            color = DawnTextSubtle
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryActionButton(
                text = "확인 후 보내기",
                modifier = Modifier.weight(1f),
                onClick = onConfirm
            )
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onDismiss,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, DawnBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = TransferMutedCardBackground,
                    contentColor = DawnText
                )
            ) {
                Text(text = "닫기")
            }
        }
    }
}

@Composable
private fun TransferConfirmationRow(
    label: String,
    value: String,
    valueFontFamily: FontFamily? = null,
    emphasized: Boolean = false,
    withDivider: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = DawnTextSubtle
            )
            Text(
                text = value,
                style = if (emphasized) {
                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                } else {
                    MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black)
                },
                color = DawnText,
                fontFamily = valueFontFamily,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        if (withDivider) {
            HorizontalDivider(color = TransferMutedCardBorder)
        }
    }
}

@Composable
private fun TransferCheckItem(
    icon: @Composable () -> Unit,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TransferMutedCardBackground, RoundedCornerShape(18.dp))
            .border(1.dp, TransferMutedCardBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = DawnText
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
private fun TransferMutedCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TransferMutedCardBackground, RoundedCornerShape(22.dp))
            .border(1.dp, TransferMutedCardBorder, RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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

private enum class TransferTrackerStepState {
    Active,
    Completed,
    Pending
}
