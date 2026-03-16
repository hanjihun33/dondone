package com.dondone.mobile.feature.remittance.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dondone.mobile.core.designsystem.DawnBorder
import com.dondone.mobile.core.designsystem.DawnPrimary
import com.dondone.mobile.core.designsystem.DawnSurface
import com.dondone.mobile.core.designsystem.DawnText
import com.dondone.mobile.core.designsystem.DawnTextSubtle
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.domain.model.TransferDestinationMode
import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus

private val TransferMutedCardBackground = Color(0xFFF8FAFC)
private val TransferMutedCardBorder = Color(0xFFE2E8F0)
private val TransferScreenCanvas = Color.White
private val TransferSegmentBackground = Color(0xFFEFEFF2)
private val TransferSegmentSelected = Color.White
private val TransferSegmentTextMuted = Color(0xFF8A8F98)
private val TransferSegmentText = Color(0xFF1F2430)
private val TransferSegmentSelectedBorder = Color(0xFFE4E6EB)
private val TransferSegmentContainerBorder = Color(0xFFE7E8EC)
private val TransferInputBorder = Color(0xFFE4E6EB)
private val TransferSectionHeaderText = Color(0xFF666B75)
private val TransferRecipientBlue = Color(0xFF3B82F6)
private val TransferRecipientAmber = Color(0xFFF4B400)
private val TransferRecipientIndigo = Color(0xFF312E81)
private val TransferRecipientTeal = Color(0xFF0F9D8A)
private val TransferRecipientRowBorder = Color(0xFFF0F1F4)
private val TransferRecipientSelectedBorder = Color(0xFFD8E5FF)
private val TransferRecipientSelectedBackground = Color(0xFFF8FBFF)
private val TransferCameraTint = Color(0xFF7A7E87)
private val TransferAmountPrompt = Color(0xFFB8C4D8)
private val TransferAmountChipBackground = Color(0xFFF1F3F5)
private val TransferAmountKeyText = Color(0xFF3C4453)
private val TransferAmountActionBackground = Color(0xFF6D68F5)
private val TransferReviewSheetScrim = Color(0x33000000)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    uiModel: TransferUiModel,
    onSelectAccount: (String) -> Unit,
    onSelectDestinationMode: (TransferDestinationMode) -> Unit,
    onSelectRecipient: (String) -> Unit,
    onUpdateRecipientDisplayName: (String) -> Unit,
    onUpdateAmount: (Int) -> Unit,
    onChangeRecipient: () -> Unit,
    onChangeAccountFromRecipient: () -> Unit,
    onChangeAccountFromAmount: () -> Unit,
    onSubmitTransfer: () -> Unit,
    onDismissTransferConfirmation: () -> Unit,
    onConfirmTransfer: () -> Unit,
    onResetTransfer: () -> Unit
) {
    if (uiModel.showTrackerScreen) {
        TransferTrackerScreen(
            uiModel = uiModel,
            onResetTransfer = onResetTransfer
        )
    } else if (uiModel.showReviewScreen) {
        TransferReviewScreen(
            uiModel = uiModel,
            onDismiss = onDismissTransferConfirmation,
            onConfirm = onConfirmTransfer,
            onUpdateRecipientDisplayName = onUpdateRecipientDisplayName
        )
    } else if (uiModel.flowStep == TransferFlowStep.AMOUNT) {
        AmountStepCard(
            uiModel = uiModel,
            onUpdateAmount = onUpdateAmount,
            onChangeRecipient = onChangeRecipient,
            onChangeAccount = onChangeAccountFromAmount,
            onPrimaryAction = onSubmitTransfer
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
                    onSelectDestinationMode = onSelectDestinationMode,
                    onSelectRecipient = onSelectRecipient,
                    onChangeAccount = onChangeAccountFromRecipient
                )

                TransferFlowStep.AMOUNT -> Unit
            }
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
    onSelectDestinationMode: (TransferDestinationMode) -> Unit,
    onSelectRecipient: (String) -> Unit,
    onChangeAccount: () -> Unit
) {
    var selectedTab by remember(uiModel.destinationMode) {
        mutableStateOf(
            if (uiModel.destinationMode == TransferDestinationMode.ACCOUNT) {
                RecipientPickerTab.Account
            } else {
                RecipientPickerTab.Contact
            }
        )
    }
    var query by remember { mutableStateOf("") }
    val filteredSections = remember(uiModel.recipientSections, query, selectedTab) {
        val keyword = query.trim()
        uiModel.recipientSections.mapNotNull { section ->
            val items = section.items.filter { recipient ->
                if (keyword.isBlank()) {
                    true
                } else {
                    val target = buildString {
                        append(recipient.name)
                        append(' ')
                        append(recipient.relationship)
                        append(' ')
                        append(
                            if (selectedTab == RecipientPickerTab.Account) {
                                recipient.accountLabel
                            } else {
                                recipient.contactLabel
                            }
                        )
                    }
                    target.contains(keyword, ignoreCase = true)
                }
            }
            if (items.isEmpty()) null else section.copy(items = items)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TransferScreenCanvas)
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = uiModel.recipientScreenTitle,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
            color = DawnText
        )

        TransferRecipientSegmentedTabs(
            selectedTab = selectedTab,
            onSelectTab = {
                selectedTab = it
                onSelectDestinationMode(
                    if (it == RecipientPickerTab.Account) {
                        TransferDestinationMode.ACCOUNT
                    } else {
                        TransferDestinationMode.WALLET
                    }
                )
            }
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = {
                Text(
                    text = if (selectedTab == RecipientPickerTab.Account) {
                        uiModel.recipientSearchPlaceholderText
                    } else {
                        "지갑주소 입력"
                    },
                    color = DawnTextSubtle
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "계좌번호 촬영",
                    tint = TransferCameraTint
                )
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedBorderColor = TransferInputBorder,
                unfocusedBorderColor = TransferInputBorder,
                disabledBorderColor = TransferInputBorder,
                focusedTextColor = DawnText,
                unfocusedTextColor = DawnText,
                cursorColor = DawnPrimary
            )
        )

        if (filteredSections.isEmpty()) {
            TransferRecipientEmptyState()
        } else {
            filteredSections.forEach { section ->
                TransferRecipientSection(
                    title = resolveRecipientSectionTitle(
                        baseTitle = section.title,
                        accountStyle = selectedTab == RecipientPickerTab.Account
                    ),
                    recipients = section.items,
                    accountStyle = selectedTab == RecipientPickerTab.Account,
                    onSelectRecipient = onSelectRecipient
                )
            }
        }

    }
}

@Composable
private fun TransferRecipientSegmentedTabs(
    selectedTab: RecipientPickerTab,
    onSelectTab: (RecipientPickerTab) -> Unit
) {
    val tabs = RecipientPickerTab.entries
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    val horizontalInset = 4.dp
    val itemSpacing = 6.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(TransferSegmentBackground, RoundedCornerShape(14.dp))
            .border(1.dp, TransferSegmentContainerBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = horizontalInset, vertical = 4.dp)
    ) {
        val segmentWidth = remember(maxWidth, tabs.size) {
            (maxWidth - itemSpacing * (tabs.size - 1)) / tabs.size
        }
        val animatedOffset by animateDpAsState(
            targetValue = (segmentWidth + itemSpacing) * selectedIndex,
            animationSpec = spring(
                dampingRatio = 0.78f,
                stiffness = 540f
            ),
            label = "recipientSegmentOffset"
        )
        val animatedShadow by animateDpAsState(
            targetValue = 2.dp,
            animationSpec = tween(durationMillis = 180),
            label = "recipientSegmentShadow"
        )

        Box(
            modifier = Modifier
                .offset(x = animatedOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .shadow(
                    elevation = animatedShadow,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = Color(0x12000000),
                    spotColor = Color(0x12000000)
                )
                .background(
                    color = TransferSegmentSelected,
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = TransferSegmentSelectedBorder,
                    shape = RoundedCornerShape(12.dp)
                )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            tabs.forEach { tab ->
                val selected = tab == selectedTab
                val textAlpha by animateFloatAsState(
                    targetValue = if (selected) 1f else 0.56f,
                    animationSpec = tween(durationMillis = 150),
                    label = "recipientTabAlpha"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (selected) Color.Transparent else TransferSegmentBackground,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectTab(tab) }
                        .padding(vertical = 11.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                        color = if (selected) {
                            TransferSegmentText.copy(alpha = textAlpha)
                        } else {
                            TransferSegmentTextMuted.copy(alpha = textAlpha)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransferRecipientSection(
    title: String,
    recipients: List<TransferRecipientUiModel>,
    accountStyle: Boolean,
    onSelectRecipient: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = TransferSectionHeaderText
        )
        recipients.forEach { recipient ->
            TransferRecipientRow(
                uiModel = recipient,
                accountStyle = accountStyle,
                onClick = { onSelectRecipient(recipient.id) }
            )
        }
    }
}

@Composable
private fun TransferRecipientRow(
    uiModel: TransferRecipientUiModel,
    accountStyle: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (uiModel.selected) TransferRecipientSelectedBackground else Color.White,
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = if (uiModel.selected) TransferRecipientSelectedBorder else TransferRecipientRowBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(recipientToneColor(uiModel.tone), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = uiModel.name.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = Color.White
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = uiModel.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = DawnText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (accountStyle) uiModel.accountLabel else uiModel.contactLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = DawnTextSubtle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = if (uiModel.selected) DawnPrimary else TransferRecipientBlue
        )
    }
}

@Composable
private fun TransferRecipientEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TransferMutedCardBackground, RoundedCornerShape(20.dp))
            .border(1.dp, TransferMutedCardBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "검색 결과가 없습니다",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
            color = DawnText
        )
        Text(
            text = "다른 이름이나 계좌번호 형식으로 다시 찾아보세요.",
            style = MaterialTheme.typography.bodySmall,
            color = DawnTextSubtle
        )
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
    var amountInput by remember(uiModel.destinationMode, uiModel.flowStep) {
        mutableStateOf(
            when (uiModel.destinationMode) {
                TransferDestinationMode.ACCOUNT -> ""
                TransferDestinationMode.WALLET -> uiModel.amountUsd.takeUnless { it == "0" }.orEmpty()
            }
        )
    }

    val promptText = if (uiModel.destinationMode == TransferDestinationMode.ACCOUNT) {
        "얼마나 옮길까요?"
    } else {
        "얼마나 보낼까요?"
    }
    val assistText = if (uiModel.destinationMode == TransferDestinationMode.ACCOUNT) {
        "잔액 · ${uiModel.selectedAccountBalanceText} 입력"
    } else {
        "지갑 송금 · USDC 입력"
    }
    val destinationTitle = if (uiModel.destinationMode == TransferDestinationMode.ACCOUNT) {
        "내 ${uiModel.selectedRecipientName} 계좌로"
    } else {
        "${uiModel.selectedRecipientName} 지갑으로"
    }
    val destinationSubtitle = if (uiModel.destinationMode == TransferDestinationMode.ACCOUNT) {
        uiModel.selectedRecipientAccountLabel
    } else {
        uiModel.selectedRecipientWalletLabel
    }
    val amountDisplay = when (uiModel.destinationMode) {
        TransferDestinationMode.ACCOUNT -> amountInput.toIntOrNull()?.let(::formatKrw).orEmpty()
        TransferDestinationMode.WALLET -> amountInput.ifBlank { "0" } + " USDC"
    }
    val hasAmountInput = amountInput.isNotBlank() && amountInput != "0"
    val actionBarReservedHeight = 62.dp
    val numberPadHeight = 358.dp
    val bottomContentReservedHeight = actionBarReservedHeight + numberPadHeight
    val actionBarOffset by animateDpAsState(
        targetValue = if (hasAmountInput) 0.dp else 20.dp,
        animationSpec = tween(durationMillis = 180),
        label = "amountActionBarOffset"
    )
    val actionBarAlpha by animateFloatAsState(
        targetValue = if (hasAmountInput) 1f else 0f,
        animationSpec = tween(durationMillis = 160),
        label = "amountActionBarAlpha"
    )

    fun syncAmount(nextInput: String) {
        amountInput = nextInput
        val nextAmount = when (uiModel.destinationMode) {
            TransferDestinationMode.ACCOUNT -> {
                val krw = nextInput.toIntOrNull() ?: 0
                (krw / 1_450.0).toInt()
            }
            TransferDestinationMode.WALLET -> nextInput.toIntOrNull() ?: 0
        }
        onUpdateAmount(nextAmount)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(horizontal = 16.dp)
                .padding(bottom = bottomContentReservedHeight)
        ) {
            TransferAmountSummaryBlock(
                title = "내 ${uiModel.selectedAccountName}에서",
                subtitle = "잔액 ${uiModel.selectedAccountBalanceText}",
                onClick = onChangeAccount
            )
            Spacer(modifier = Modifier.height(28.dp))
            TransferAmountSummaryBlock(
                title = destinationTitle,
                subtitle = destinationSubtitle,
                onClick = onChangeRecipient
            )
            Spacer(modifier = Modifier.height(36.dp))
            Text(
                text = if (hasAmountInput) amountDisplay else promptText,
                style = if (hasAmountInput) {
                    MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black)
                } else {
                    MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black)
                },
                color = if (hasAmountInput) DawnText else TransferAmountPrompt
            )
            Spacer(modifier = Modifier.height(14.dp))
            if (!hasAmountInput) {
                Text(
                    text = assistText,
                    modifier = Modifier
                        .background(TransferAmountChipBackground, RoundedCornerShape(10.dp))
                        .clickable(
                            enabled = uiModel.destinationMode == TransferDestinationMode.ACCOUNT
                        ) {
                            val fullBalance = uiModel.selectedAccountBalanceText.filter(Char::isDigit)
                            syncAmount(fullBalance)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                    color = DawnTextSubtle
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bottomContentReservedHeight)
                .align(Alignment.BottomCenter)
        ) {
            TransferNumberPad(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                onNumberClick = { key ->
                    syncAmount(appendTransferAmountInput(amountInput, key))
                },
                onBackspace = {
                    syncAmount(removeLastTransferAmountDigit(amountInput))
                }
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .offset(y = actionBarOffset)
                    .alpha(actionBarAlpha)
                    .background(TransferAmountActionBackground)
                    .clickable(
                        enabled = hasAmountInput && uiModel.canSubmit,
                        onClick = onPrimaryAction
                    )
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "다음",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = Color.White.copy(
                        alpha = if (hasAmountInput && uiModel.canSubmit) actionBarAlpha else 0.52f * actionBarAlpha
                    )
                )
            }
        }
    }
}

@Composable
private fun TransferAmountSummaryBlock(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
            color = DawnText
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = DawnTextSubtle
        )
    }
}

@Composable
private fun TransferNumberPad(
    modifier: Modifier = Modifier,
    onNumberClick: (String) -> Unit,
    onBackspace: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        ).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                row.forEach { key ->
                    TransferNumberPadKey(
                        text = key,
                        onClick = { onNumberClick(key) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            TransferNumberPadKey(
                text = "00",
                onClick = { onNumberClick("00") }
            )
            TransferNumberPadKey(
                text = "0",
                onClick = { onNumberClick("0") }
            )
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clickable(onClick = onBackspace),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "삭제",
                    tint = TransferAmountKeyText,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun TransferNumberPadKey(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Normal),
            color = TransferAmountKeyText,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TransferTrackerScreen(
    uiModel: TransferUiModel,
    onResetTransfer: () -> Unit
) {
    val completedHeadline = if (uiModel.destinationMode == TransferDestinationMode.ACCOUNT) {
        "내 ${uiModel.selectedRecipientName} 계좌로"
    } else {
        "${uiModel.selectedRecipientName} 지갑으로"
    }
    val completedSuffix = if (uiModel.transferStatus == TransferStatus.CONFIRMED) {
        if (uiModel.destinationMode == TransferDestinationMode.ACCOUNT) {
            "옮겼어요"
        } else {
            "보냈어요"
        }
    } else {
        "확인하고 있어요"
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-32).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White,
                                Color(0xFFD8DDF8),
                                Color(0xFFBCC6F6)
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(1.dp, Color(0xFFD4D9F6), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (uiModel.transferStatus == TransferStatus.CONFIRMED) {
                        Icons.Default.Check
                    } else {
                        Icons.AutoMirrored.Filled.Send
                    },
                    contentDescription = null,
                    tint = Color(0xFF5C6484),
                    modifier = Modifier.size(30.dp)
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = completedHeadline,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = DawnText,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = uiModel.confirmationAmountText,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                    color = DawnText,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = completedSuffix,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = DawnText,
                    textAlign = TextAlign.Center
                )
            }
            OutlinedButton(
                onClick = { },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, TransferMutedCardBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFFF7F8FA),
                    contentColor = DawnTextSubtle
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "메모 남기기",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (uiModel.transferStatus == TransferStatus.CONFIRMED) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DawnPrimary, RoundedCornerShape(18.dp))
                        .clickable(onClick = onResetTransfer)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "확인",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransferReviewScreen(
    uiModel: TransferUiModel,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onUpdateRecipientDisplayName: (String) -> Unit
) {
    var showAccountSheet by remember { mutableStateOf(false) }
    var showRecipientDisplayEditor by remember { mutableStateOf(false) }
    var draftRecipientDisplayName by remember(uiModel.selectedRecipientName) {
        mutableStateOf(uiModel.selectedRecipientName)
    }
    val accountSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val recipientDetail = if (uiModel.destinationMode == TransferDestinationMode.ACCOUNT) {
        uiModel.selectedRecipientAccountLabel
    } else {
        uiModel.selectedRecipientWalletLabel
    }
    val headlineSuffix = if (uiModel.destinationMode == TransferDestinationMode.ACCOUNT) {
        "옮길까요?"
    } else {
        "보낼까요?"
    }

    if (showRecipientDisplayEditor) {
        TransferRecipientDisplayNameEditor(
            value = draftRecipientDisplayName,
            onValueChange = { draftRecipientDisplayName = it },
            onClose = {
                draftRecipientDisplayName = uiModel.selectedRecipientName
                showRecipientDisplayEditor = false
            },
            onDone = {
                onUpdateRecipientDisplayName(draftRecipientDisplayName)
                showRecipientDisplayEditor = false
            }
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .offset(y = (-100).dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (uiModel.destinationMode == TransferDestinationMode.ACCOUNT) {
                            "내 ${uiModel.selectedRecipientName} 계좌로"
                        } else {
                            "${uiModel.selectedRecipientName} 지갑으로"
                        },
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = DawnText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = uiModel.confirmationAmountText,
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                        color = DawnPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = headlineSuffix,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = DawnText,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TransferReviewInfoRow(
                    label = if (uiModel.destinationMode == TransferDestinationMode.ACCOUNT) {
                        "받는 분에게 표시"
                    } else {
                        "받는 지갑"
                    },
                    value = uiModel.selectedRecipientName,
                    onClick = {
                        draftRecipientDisplayName = uiModel.selectedRecipientName
                        showRecipientDisplayEditor = true
                    },
                    withDivider = false,
                    showTrailingIcon = true
                )
                TransferReviewInfoRow(
                    label = "출금 계좌",
                    value = uiModel.selectedAccountName,
                    onClick = { showAccountSheet = true },
                    withDivider = false,
                    showTrailingIcon = true
                )
                TransferReviewInfoRow(
                    label = if (uiModel.destinationMode == TransferDestinationMode.ACCOUNT) {
                        "입금 계좌"
                    } else {
                        "지갑 주소"
                    },
                    value = recipientDetail,
                    onClick = onDismiss,
                    withDivider = false,
                    showTrailingIcon = false,
                    valueFontFamily = if (uiModel.destinationMode == TransferDestinationMode.ACCOUNT) null else FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DawnPrimary, RoundedCornerShape(18.dp))
                        .clickable(onClick = onConfirm)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiModel.destinationMode == TransferDestinationMode.ACCOUNT) "옮기기" else "보내기",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showAccountSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAccountSheet = false },
            sheetState = accountSheetState,
            containerColor = Color.White,
            scrimColor = TransferReviewSheetScrim,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 14.dp)
                        .width(92.dp)
                        .height(6.dp)
                        .background(Color(0xFFE5E7EB), RoundedCornerShape(999.dp))
                )
            }
        ) {
            TransferAccountBottomSheet(
                uiModel = uiModel,
                onConfirm = { showAccountSheet = false }
            )
        }
    }
}

@Composable
private fun TransferRecipientDisplayNameEditor(
    value: String,
    onValueChange: (String) -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "닫기",
                tint = DawnTextSubtle,
                modifier = Modifier
                    .size(36.dp)
                    .clickable(onClick = onClose)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onDone)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "완료",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                    color = DawnTextSubtle
                )
            }
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                color = DawnText,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun TransferAccountBottomSheet(
    uiModel: TransferUiModel,
    onConfirm: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "아래 계좌에서 출금돼요",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = DawnText
        )
    }
    Spacer(modifier = Modifier.height(28.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .background(Color(0xFFFFD54F), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "KB",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                color = Color(0xFF1F2937)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "내 ${uiModel.selectedAccountName}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = DawnText
            )
            Text(
                text = uiModel.selectedAccountNumber,
                style = MaterialTheme.typography.titleMedium,
                color = DawnTextSubtle
            )
        }
    }
    Spacer(modifier = Modifier.height(44.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(DawnPrimary, RoundedCornerShape(22.dp))
            .clickable(onClick = onConfirm)
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "확인",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = Color.White
        )
    }
    Spacer(modifier = Modifier.height(28.dp))
}

@Composable
private fun TransferReviewInfoRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    withDivider: Boolean = true,
    showTrailingIcon: Boolean = true,
    valueFontFamily: FontFamily? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = DawnTextSubtle
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                    color = DawnText,
                    fontFamily = valueFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (showTrailingIcon) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = DawnTextSubtle,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.size(18.dp))
                }
            }
        }
        if (withDivider) {
            HorizontalDivider(color = TransferMutedCardBorder)
        }
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

private enum class RecipientPickerTab(val label: String) {
    Account("계좌"),
    Contact("지갑")
}

private fun recipientToneColor(tone: TransferRecipientTone): Color =
    when (tone) {
        TransferRecipientTone.Amber -> TransferRecipientAmber
        TransferRecipientTone.Blue -> TransferRecipientBlue
        TransferRecipientTone.Indigo -> TransferRecipientIndigo
        TransferRecipientTone.Teal -> TransferRecipientTeal
    }

private fun appendTransferAmountInput(
    current: String,
    next: String
): String {
    val candidate = if (current == "0") {
        next
    } else {
        current + next
    }
    return candidate.trimStart('0').ifBlank { "0" }.take(9)
}

private fun removeLastTransferAmountDigit(current: String): String =
    current
        .filter(Char::isDigit)
        .dropLast(1)

private fun resolveRecipientSectionTitle(
    baseTitle: String,
    accountStyle: Boolean
): String =
    when (baseTitle) {
        "자주 보내는 지갑" -> if (accountStyle) "자주 보내는 계좌" else "자주 보내는 지갑"
        "최근 보낸 지갑" -> if (accountStyle) "최근 보낸 계좌" else "최근 보낸 지갑"
        else -> baseTitle
    }
