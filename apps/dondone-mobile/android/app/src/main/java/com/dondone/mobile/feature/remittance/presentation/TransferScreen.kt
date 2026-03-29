package com.dondone.mobile.feature.remittance.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dondone.mobile.core.designsystem.DawnBorder
import com.dondone.mobile.core.designsystem.DawnPrimary
import com.dondone.mobile.core.designsystem.DawnSurface
import com.dondone.mobile.core.designsystem.DawnText
import com.dondone.mobile.core.designsystem.DawnTextSubtle
import com.dondone.mobile.app.session.RemittanceActionUiState
import com.dondone.mobile.app.session.RemittanceSubmittingAction
import com.dondone.mobile.core.i18n.AppLanguage
import com.dondone.mobile.core.i18n.LocalAppLanguage
import com.dondone.mobile.core.i18n.text
import com.dondone.mobile.core.i18n.translate
import com.dondone.mobile.core.ui.formatKrw
import com.dondone.mobile.domain.model.TransferDestinationMode
import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus
import com.dondone.mobile.feature.recipient.presentation.RecipientWalletAddBottomSheet
import com.dondone.mobile.feature.recipient.presentation.resolveRecipientSheetErrorMessage
import com.dondone.mobile.feature.recipient.presentation.shouldCloseRecipientSheetAfterResult

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
private val TransferAmountActionBarHeight = 62.dp
private val TransferNumberPadHeight = 358.dp
private val TransferNumberPadBottomPadding = 12.dp
private val TransferAmountActionVerticalPadding = 14.dp
private val TransferScreenHorizontalPadding = 16.dp
private val TransferNumberPadRows = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9")
)
private const val TransferAmountMaxDigits = 9
private const val TransferKrwPerUsdc = 1_450.0
private data class TransferDestinationSummary(
    val title: String,
    val subtitle: String
)

private data class TransferAmountStepCopy(
    val promptText: String,
    val assistText: String,
    val destination: TransferDestinationSummary,
    val amountDisplay: String,
    val hasAmountInput: Boolean,
    val canUseBalanceShortcut: Boolean,
    val fullBalanceInput: String
)

private data class TransferTrackerCopy(
    val headline: String,
    val statusText: String
)

private data class TransferReviewCopy(
    val recipientDetail: String,
    val headline: String,
    val headlineSuffix: String,
    val recipientLabel: String,
    val recipientDetailLabel: String,
    val recipientDetailFontFamily: FontFamily?,
    val confirmActionText: String
)

private enum class TransferReviewOverlay {
    None,
    RecipientEditor,
    AccountSheet,
    WalletSheet
}

internal enum class TransferScreenMode {
    TRACKER,
    REVIEW,
    REMOTE_GATE,
    AMOUNT,
    PICKER
}

internal fun resolveTransferScreenMode(uiModel: TransferUiModel): TransferScreenMode {
    return when {
        uiModel.showTrackerScreen -> TransferScreenMode.TRACKER
        uiModel.showReviewScreen -> TransferScreenMode.REVIEW
        uiModel.remoteGate != null -> TransferScreenMode.REMOTE_GATE
        uiModel.flowStep == TransferFlowStep.AMOUNT -> TransferScreenMode.AMOUNT
        else -> TransferScreenMode.PICKER
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    uiModel: TransferUiModel,
    actionUiState: RemittanceActionUiState,
    onSelectAccount: (String) -> Unit,
    onSelectDestinationMode: (TransferDestinationMode) -> Unit,
    onSelectRecipient: (String) -> Unit,
    onUpdateRecipientDisplayName: (String) -> Unit,
    onUpdateAmount: (Int) -> Unit,
    onAddRecipient: (String, String, String, Long?) -> Unit,
    onSearchRecipientsByPhone: (String) -> Unit,
    onClearPhoneSearch: () -> Unit,
    onRefreshRemittance: () -> Unit,
    onChangeRecipient: () -> Unit,
    onChangeAccountFromAmount: () -> Unit,
    onSubmitTransfer: () -> Unit,
    onDismissTransferConfirmation: () -> Unit,
    onConfirmTransfer: () -> Unit,
    onResetTransfer: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, onRefreshRemittance) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefreshRemittance()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    when (resolveTransferScreenMode(uiModel)) {
        TransferScreenMode.TRACKER -> TransferTrackerScreen(
            uiModel = uiModel,
            onResetTransfer = onResetTransfer
        )

        TransferScreenMode.REVIEW -> TransferReviewScreen(
            uiModel = uiModel,
            onDismiss = onDismissTransferConfirmation,
            onConfirm = onConfirmTransfer,
            onUpdateRecipientDisplayName = onUpdateRecipientDisplayName
        )

        TransferScreenMode.REMOTE_GATE -> TransferRemoteGateScreen(
            gate = requireNotNull(uiModel.remoteGate),
            onRefresh = onRefreshRemittance
        )

        TransferScreenMode.AMOUNT -> AmountStepCard(
            uiModel = uiModel,
            onUpdateAmount = onUpdateAmount,
            onChangeRecipient = onChangeRecipient,
            onChangeAccount = onChangeAccountFromAmount,
            onPrimaryAction = onSubmitTransfer
        )

        TransferScreenMode.PICKER -> Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TransferScreenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiModel.flowStep == TransferFlowStep.ACCOUNT) {
                AccountStepCard(
                    uiModel = uiModel,
                    onSelectAccount = onSelectAccount
                )
            } else {
                RecipientStepCard(
                    uiModel = uiModel,
                    actionUiState = actionUiState,
                    onSelectDestinationMode = onSelectDestinationMode,
                    onSelectRecipient = onSelectRecipient,
                    onAddRecipient = onAddRecipient,
                    onSearchRecipientsByPhone = onSearchRecipientsByPhone,
                    onClearPhoneSearch = onClearPhoneSearch
                )
            }
        }
    }
}

@Composable
private fun AccountStepCard(
    uiModel: TransferUiModel,
    onSelectAccount: (String) -> Unit
) {
    val language = LocalAppLanguage.current
    TransferContainerCard {
        Text(
            text = language.text("transfer_sending_account"),
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
    actionUiState: RemittanceActionUiState,
    onSelectDestinationMode: (TransferDestinationMode) -> Unit,
    onSelectRecipient: (String) -> Unit,
    onAddRecipient: (String, String, String, Long?) -> Unit,
    onSearchRecipientsByPhone: (String) -> Unit,
    onClearPhoneSearch: () -> Unit
) {
    val language = LocalAppLanguage.current
    val selectedTab = uiModel.destinationMode.toRecipientPickerTab()
    var query by remember { mutableStateOf("") }
    var showCreateRecipientSheet by remember { mutableStateOf(false) }
    var awaitingRecipientCreation by remember { mutableStateOf(false) }
    val filteredSections = remember(uiModel.recipientSections, query, selectedTab) {
        filterRecipientSections(
            sections = uiModel.recipientSections,
            query = query,
            selectedTab = selectedTab
        )
    }
    val isRecipientSubmitting = actionUiState.isSubmitting &&
        actionUiState.submittingAction == RemittanceSubmittingAction.RECIPIENT_CREATE
    val submitErrorMessage = resolveRecipientSheetErrorMessage(
        isAwaitingResult = awaitingRecipientCreation,
        actionUiState = actionUiState
    )

    LaunchedEffect(
        awaitingRecipientCreation,
        actionUiState.isSubmitting,
        actionUiState.message,
        actionUiState.isError
    ) {
        if (!shouldCloseRecipientSheetAfterResult(awaitingRecipientCreation, actionUiState)) {
            return@LaunchedEffect
        }
        onClearPhoneSearch()
        showCreateRecipientSheet = false
        awaitingRecipientCreation = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TransferScreenCanvas)
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = uiModel.recipientScreenTitle,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = DawnText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (uiModel.showAddRecipientAction) {
                OutlinedButton(
                    onClick = { showCreateRecipientSheet = true },
                    modifier = Modifier.padding(start = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, TransferRecipientSelectedBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = DawnPrimary
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 14.dp,
                        vertical = 10.dp
                    )
                ) {
                    Text(
                        text = language.text("transfer_add_recipient"),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (!uiModel.isRemoteMode) {
            TransferRecipientSegmentedTabs(
                selectedTab = selectedTab,
                onSelectTab = { onSelectDestinationMode(it.toDestinationMode()) }
            )
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = {
                Text(
                    text = selectedTab.searchPlaceholder(
                        accountPlaceholder = uiModel.recipientSearchPlaceholderText,
                        language = language
                    ),
                    color = DawnTextSubtle
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = language.text("transfer_scan_account_number"),
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
            TransferRecipientEmptyState(
                description = if (uiModel.showAddRecipientAction) {
                    language.text("transfer_register_allowed_recipient_first")
                } else {
                    selectedTab.emptyStateDescription(language)
                },
                actionText = if (uiModel.showAddRecipientAction) language.text("transfer_register_recipient") else null,
                onAction = if (uiModel.showAddRecipientAction) {
                    { showCreateRecipientSheet = true }
                } else {
                    null
                }
            )
        } else {
            filteredSections.forEach { section ->
                TransferRecipientSection(
                    title = selectedTab.resolveSectionTitle(section.title, language),
                    recipients = section.items,
                    selectedTab = selectedTab,
                    onSelectRecipient = onSelectRecipient
                )
            }
        }

    }

    if (showCreateRecipientSheet) {
        RecipientWalletAddBottomSheet(
            phoneDirectory = uiModel.addRecipientPhoneDirectory,
            supportsRemotePhoneSearch = uiModel.addRecipientSupportsRemotePhoneSearch,
            phoneSearchResults = uiModel.addRecipientPhoneSearchResults,
            isPhoneSearchLoading = uiModel.addRecipientPhoneSearchLoading,
            phoneSearchErrorMessage = uiModel.addRecipientPhoneSearchErrorMessage,
            isSubmitting = isRecipientSubmitting,
            submitErrorMessage = submitErrorMessage,
            onDismiss = {
                if (!isRecipientSubmitting) {
                    onClearPhoneSearch()
                    showCreateRecipientSheet = false
                    awaitingRecipientCreation = false
                }
            },
            onSubmit = { alias, relation, walletAddress, targetUserId ->
                awaitingRecipientCreation = true
                onAddRecipient(alias, relation, walletAddress, targetUserId)
            },
            onSearchByPhone = onSearchRecipientsByPhone,
            onClearPhoneSearch = onClearPhoneSearch
        )
    }
}

@Composable
private fun TransferRecipientSegmentedTabs(
    selectedTab: RecipientPickerTab,
    onSelectTab: (RecipientPickerTab) -> Unit
) {
    val language = LocalAppLanguage.current
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
        val selectedOffset = (segmentWidth + itemSpacing) * selectedIndex

        Box(
            modifier = Modifier
                .offset(x = selectedOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .shadow(
                    elevation = 2.dp,
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
                val textAlpha = if (selected) 1f else 0.56f
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
                        text = language.translate(tab.label),
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
    selectedTab: RecipientPickerTab,
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
                selectedTab = selectedTab,
                onClick = { onSelectRecipient(recipient.id) }
            )
        }
    }
}

@Composable
private fun TransferRecipientRow(
    uiModel: TransferRecipientUiModel,
    selectedTab: RecipientPickerTab,
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
                text = uiModel.secondaryLabel(selectedTab),
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
private fun TransferRecipientEmptyState(
    description: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    val language = LocalAppLanguage.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TransferMutedCardBackground, RoundedCornerShape(20.dp))
            .border(1.dp, TransferMutedCardBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = language.text("transfer_no_search_results"),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
            color = DawnText,
            textAlign = TextAlign.Center
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = DawnTextSubtle,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TransferInlineNoticeCard(
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TransferMutedCardBackground, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
            color = DawnText
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = DawnTextSubtle
        )
    }
}

@Composable
private fun TransferRemoteGateScreen(
    gate: TransferRemoteGateUiModel,
    onRefresh: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TransferMutedCardBackground, RoundedCornerShape(24.dp))
                .border(1.dp, TransferMutedCardBorder, RoundedCornerShape(24.dp))
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (gate.isLoading) {
                CircularProgressIndicator(
                    color = DawnPrimary,
                    strokeWidth = 3.dp
                )
            }
            Text(
                text = gate.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = DawnText,
                textAlign = TextAlign.Center
            )
            Text(
                text = gate.description,
                style = MaterialTheme.typography.bodyMedium,
                color = DawnTextSubtle,
                textAlign = TextAlign.Center
            )
            if (gate.actionText != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DawnPrimary, RoundedCornerShape(18.dp))
                        .clickable(onClick = onRefresh)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = gate.actionText,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                }
            }
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
    val language = LocalAppLanguage.current
    var amountInput by remember(uiModel.destinationMode, uiModel.flowStep) {
        mutableStateOf(resolveInitialAmountInput(uiModel))
    }
    val amountStepCopy = resolveAmountStepCopy(uiModel = uiModel, amountInput = amountInput, language = language)
    val bottomContentReservedHeight = TransferAmountActionBarHeight + TransferNumberPadHeight

    fun syncAmount(nextInput: String) {
        amountInput = nextInput
        onUpdateAmount(convertTransferAmountInput(uiModel.destinationMode, nextInput))
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
                .padding(horizontal = TransferScreenHorizontalPadding)
                .padding(bottom = bottomContentReservedHeight)
        ) {
            TransferAmountSummaryBlock(
                title = language.text("transfer_from_my_account_format", uiModel.selectedAccountName),
                subtitle = language.text("transfer_balance_format", uiModel.selectedAccountBalanceText),
                onClick = onChangeAccount
            )
            Spacer(modifier = Modifier.height(28.dp))
            TransferAmountSummaryBlock(
                title = amountStepCopy.destination.title,
                subtitle = amountStepCopy.destination.subtitle,
                onClick = onChangeRecipient
            )
            Spacer(modifier = Modifier.height(36.dp))
            Text(
                text = if (amountStepCopy.hasAmountInput) amountStepCopy.amountDisplay else amountStepCopy.promptText,
                style = if (amountStepCopy.hasAmountInput) {
                    MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black)
                } else {
                    MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black)
                },
                color = if (amountStepCopy.hasAmountInput) DawnText else TransferAmountPrompt
            )
            Spacer(modifier = Modifier.height(14.dp))
            if (!amountStepCopy.hasAmountInput) {
                Text(
                    text = amountStepCopy.assistText,
                    modifier = Modifier
                        .background(TransferAmountChipBackground, RoundedCornerShape(10.dp))
                        .clickable(
                            enabled = amountStepCopy.canUseBalanceShortcut
                        ) {
                            syncAmount(amountStepCopy.fullBalanceInput)
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
                    .padding(bottom = TransferNumberPadBottomPadding),
                onNumberClick = { key ->
                    syncAmount(appendTransferAmountInput(amountInput, key))
                },
                onBackspace = {
                    syncAmount(removeLastTransferAmountDigit(amountInput))
                }
            )
            if (amountStepCopy.hasAmountInput) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(TransferAmountActionBackground)
                        .clickable(
                            enabled = uiModel.canSubmit && !uiModel.isActionSubmitting,
                            onClick = onPrimaryAction
                        )
                        .padding(vertical = TransferAmountActionVerticalPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiModel.isActionSubmitting) language.text("transfer_confirming") else language.text("transfer_next"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = Color.White.copy(
                            alpha = if (uiModel.canSubmit && !uiModel.isActionSubmitting) 1f else 0.52f
                        )
                    )
                }
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
        TransferNumberPadRows.forEach { row ->
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
    val language = LocalAppLanguage.current
    val trackerCopy = resolveTrackerCopy(uiModel, language)
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
                    imageVector = when (uiModel.transferStatus) {
                        TransferStatus.CONFIRMED -> Icons.Default.Check
                        TransferStatus.FAILED -> Icons.Default.Close
                        else -> Icons.AutoMirrored.Filled.Send
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
                    text = trackerCopy.headline,
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
                    text = trackerCopy.statusText,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = DawnText,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiModel.trackerDetailText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DawnTextSubtle,
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
                    text = language.text("transfer_leave_memo"),
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
            if (uiModel.transferStatus == TransferStatus.CONFIRMED || uiModel.transferStatus == TransferStatus.FAILED) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (uiModel.transferStatus == TransferStatus.FAILED) Color(0xFF1F2937) else DawnPrimary,
                            RoundedCornerShape(18.dp)
                        )
                        .clickable(onClick = onResetTransfer)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiModel.transferStatus == TransferStatus.FAILED) language.text("close") else language.text("transfer_confirm"),
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
    val language = LocalAppLanguage.current
    var activeOverlay by remember { mutableStateOf(TransferReviewOverlay.None) }
    var draftRecipientDisplayName by remember(uiModel.selectedRecipientName) {
        mutableStateOf(uiModel.selectedRecipientName)
    }
    val reviewSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val reviewCopy = resolveReviewCopy(uiModel, language)
    val context = LocalContext.current
    val clipboardManager = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    if (activeOverlay == TransferReviewOverlay.RecipientEditor) {
        TransferRecipientDisplayNameEditor(
            value = draftRecipientDisplayName,
            onValueChange = { draftRecipientDisplayName = it },
            onClose = {
                draftRecipientDisplayName = uiModel.selectedRecipientName
                activeOverlay = TransferReviewOverlay.None
            },
            onDone = {
                onUpdateRecipientDisplayName(draftRecipientDisplayName)
                activeOverlay = TransferReviewOverlay.None
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
                        text = reviewCopy.headline,
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
                        text = reviewCopy.headlineSuffix,
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
                    label = reviewCopy.recipientLabel,
                    value = uiModel.selectedRecipientName,
                    onClick = {
                        draftRecipientDisplayName = uiModel.selectedRecipientName
                        activeOverlay = TransferReviewOverlay.RecipientEditor
                    },
                    withDivider = false,
                    showTrailingIcon = true
                )
                TransferReviewInfoRow(
                    label = if (uiModel.destinationMode == TransferDestinationMode.WALLET) {
                        language.text("transfer_withdraw_wallet")
                    } else {
                        language.text("transfer_withdraw_account")
                    },
                    value = uiModel.selectedAccountName,
                    onClick = { activeOverlay = TransferReviewOverlay.AccountSheet },
                    withDivider = false,
                    showTrailingIcon = true
                )
                TransferReviewInfoRow(
                    label = reviewCopy.recipientDetailLabel,
                    value = reviewCopy.recipientDetail,
                    onClick = if (uiModel.destinationMode == TransferDestinationMode.WALLET) {
                        { activeOverlay = TransferReviewOverlay.WalletSheet }
                    } else {
                        null
                    },
                    withDivider = false,
                    showTrailingIcon = uiModel.destinationMode == TransferDestinationMode.WALLET,
                    valueFontFamily = reviewCopy.recipientDetailFontFamily
                )
                if (uiModel.reviewNotice != null) {
                    TransferInlineNoticeCard(
                        title = uiModel.reviewNotice.title,
                        description = uiModel.reviewNotice.description
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DawnPrimary, RoundedCornerShape(18.dp))
                        .clickable(
                            enabled = !uiModel.isActionSubmitting,
                            onClick = onConfirm
                        )
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiModel.isActionSubmitting) {
                            if (uiModel.destinationMode == TransferDestinationMode.WALLET) {
                                language.text("transfer_sending_now")
                            } else {
                                language.text("transfer_moving_now")
                            }
                        } else {
                            reviewCopy.confirmActionText
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = Color.White.copy(alpha = if (uiModel.isActionSubmitting) 0.72f else 1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (activeOverlay == TransferReviewOverlay.AccountSheet) {
        ModalBottomSheet(
            onDismissRequest = { activeOverlay = TransferReviewOverlay.None },
            sheetState = reviewSheetState,
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
                onConfirm = { activeOverlay = TransferReviewOverlay.None }
            )
        }
    }

    if (activeOverlay == TransferReviewOverlay.WalletSheet) {
        ModalBottomSheet(
            onDismissRequest = { activeOverlay = TransferReviewOverlay.None },
            sheetState = reviewSheetState,
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
            TransferWalletBottomSheet(
                recipientName = uiModel.selectedRecipientName,
                walletAddress = uiModel.selectedRecipientWalletFullLabel,
                clipboardManager = clipboardManager,
                context = context,
                onConfirm = { activeOverlay = TransferReviewOverlay.None }
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
    val language = LocalAppLanguage.current
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
                contentDescription = language.text("close"),
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
                    text = language.text("transfer_done"),
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
    val language = LocalAppLanguage.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = if (uiModel.destinationMode == TransferDestinationMode.WALLET) {
                language.text("transfer_withdraw_from_wallet")
            } else {
                language.text("transfer_withdraw_from_account")
            },
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
                .background(Color(0xFFE5E7EB), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = resolveAccountBadgeText(uiModel.selectedAccountName, language),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                color = Color(0xFF1F2937)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = language.text("transfer_my_account_name_format", uiModel.selectedAccountName),
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
            text = language.text("transfer_confirm"),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = Color.White
        )
    }
    Spacer(modifier = Modifier.height(28.dp))
}

@Composable
private fun TransferWalletBottomSheet(
    recipientName: String,
    walletAddress: String,
    clipboardManager: ClipboardManager,
    context: Context,
    onConfirm: () -> Unit
) {
    val language = LocalAppLanguage.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = language.text("transfer_send_to_wallet"),
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
                .background(Color(0xFFE5E7EB), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = language.text("wallet"),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                color = Color(0xFF1F2937)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = recipientName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = DawnText
            )
            Text(
                text = walletAddress,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = DawnTextSubtle
            )
        }
    }
    Spacer(modifier = Modifier.height(20.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(Color(0xFFF3F4F6), RoundedCornerShape(18.dp))
            .clickable {
                clipboardManager.setPrimaryClip(ClipData.newPlainText("wallet_address", walletAddress))
                Toast.makeText(context, language.text("transfer_wallet_address_copied"), Toast.LENGTH_SHORT).show()
            }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = language.text("transfer_copy_address"),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = DawnText
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
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
            text = language.text("transfer_confirm"),
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
    onClick: (() -> Unit)?,
    withDivider: Boolean = true,
    showTrailingIcon: Boolean = true,
    valueFontFamily: FontFamily? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
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
    Wallet("지갑")
}

private fun recipientToneColor(tone: TransferRecipientTone): Color =
    when (tone) {
        TransferRecipientTone.Amber -> TransferRecipientAmber
        TransferRecipientTone.Blue -> TransferRecipientBlue
        TransferRecipientTone.Indigo -> TransferRecipientIndigo
        TransferRecipientTone.Teal -> TransferRecipientTeal
    }

private fun RecipientPickerTab.toDestinationMode(): TransferDestinationMode =
    when (this) {
        RecipientPickerTab.Account -> TransferDestinationMode.ACCOUNT
        RecipientPickerTab.Wallet -> TransferDestinationMode.WALLET
    }

private fun TransferDestinationMode.toRecipientPickerTab(): RecipientPickerTab =
    when (this) {
        TransferDestinationMode.ACCOUNT -> RecipientPickerTab.Account
        TransferDestinationMode.WALLET -> RecipientPickerTab.Wallet
    }

private fun RecipientPickerTab.searchPlaceholder(
    accountPlaceholder: String,
    language: AppLanguage
): String =
    if (this == RecipientPickerTab.Account) {
        accountPlaceholder
    } else {
        language.text("transfer_enter_wallet_address")
    }

private fun RecipientPickerTab.emptyStateDescription(language: AppLanguage): String =
    if (this == RecipientPickerTab.Account) {
        language.text("transfer_search_again_account")
    } else {
        language.text("transfer_search_again_wallet")
    }

private fun RecipientPickerTab.resolveSectionTitle(
    baseTitle: String,
    language: AppLanguage
): String =
    when (baseTitle) {
        language.text("transfer_frequent_wallet") -> if (this == RecipientPickerTab.Account) language.text("transfer_frequent_account") else baseTitle
        language.text("transfer_recent_wallet") -> if (this == RecipientPickerTab.Account) language.text("transfer_recent_account") else baseTitle
        else -> baseTitle
    }

private fun filterRecipientSections(
    sections: List<TransferRecipientSectionUiModel>,
    query: String,
    selectedTab: RecipientPickerTab
): List<TransferRecipientSectionUiModel> {
    val keyword = query.trim()
    return sections.mapNotNull { section ->
        val items = section.items.filter { recipient ->
            keyword.isBlank() || buildRecipientSearchTarget(recipient, selectedTab)
                .contains(keyword, ignoreCase = true)
        }
        if (items.isEmpty()) {
            null
        } else {
            section.copy(items = items)
        }
    }
}

private fun buildRecipientSearchTarget(
    recipient: TransferRecipientUiModel,
    selectedTab: RecipientPickerTab
): String = buildString {
    append(recipient.name)
    append(' ')
    append(recipient.relationship)
    append(' ')
    append(recipient.secondaryLabel(selectedTab))
}

private fun TransferRecipientUiModel.secondaryLabel(selectedTab: RecipientPickerTab): String =
    if (selectedTab == RecipientPickerTab.Account) {
        accountLabel
    } else {
        contactLabel
    }

private fun resolveDestinationSummary(
    uiModel: TransferUiModel,
    language: AppLanguage
): TransferDestinationSummary =
    if (uiModel.destinationMode == TransferDestinationMode.ACCOUNT) {
        TransferDestinationSummary(
            title = language.text("transfer_to_my_account_format", uiModel.selectedRecipientName),
            subtitle = uiModel.selectedRecipientAccountLabel
        )
    } else {
        TransferDestinationSummary(
            title = language.text("transfer_to_wallet_format", uiModel.selectedRecipientName),
            subtitle = uiModel.selectedRecipientWalletLabel
        )
    }

private fun resolveAmountStepCopy(
    uiModel: TransferUiModel,
    amountInput: String,
    language: AppLanguage
): TransferAmountStepCopy {
    val hasAmountInput = amountInput.isNotBlank() && amountInput != "0"
    val promptText = if (uiModel.destinationMode == TransferDestinationMode.ACCOUNT) {
        language.text("transfer_how_much_move")
    } else {
        language.text("transfer_how_much_send")
    }
    val assistText = language.text("transfer_balance_input_format", uiModel.selectedAccountBalanceText)
    val amountDisplay = when (uiModel.destinationMode) {
        TransferDestinationMode.ACCOUNT -> amountInput.toIntOrNull()?.let { formatKrw(it, language) }.orEmpty()
        TransferDestinationMode.WALLET -> amountInput.ifBlank { "0" } + " USDC"
    }

    return TransferAmountStepCopy(
        promptText = promptText,
        assistText = assistText,
        destination = resolveDestinationSummary(uiModel, language),
        amountDisplay = amountDisplay,
        hasAmountInput = hasAmountInput,
        canUseBalanceShortcut = uiModel.destinationMode == TransferDestinationMode.ACCOUNT,
        fullBalanceInput = uiModel.selectedAccountBalanceText.filter(Char::isDigit)
    )
}

private fun resolveInitialAmountInput(uiModel: TransferUiModel): String =
    when (uiModel.destinationMode) {
        TransferDestinationMode.ACCOUNT -> uiModel.amountUsd
            .takeUnless { it == "0" }
            ?.toIntOrNull()
            ?.let { formatKrw(it * TransferKrwPerUsdc.toInt()).filter(Char::isDigit) }
            .orEmpty()
        TransferDestinationMode.WALLET -> uiModel.amountUsd.takeUnless { it == "0" }.orEmpty()
    }

private fun convertTransferAmountInput(
    destinationMode: TransferDestinationMode,
    amountInput: String
): Int =
    when (destinationMode) {
        TransferDestinationMode.ACCOUNT -> {
            val krw = amountInput.toIntOrNull() ?: 0
            (krw / TransferKrwPerUsdc).toInt()
        }
        TransferDestinationMode.WALLET -> amountInput.toIntOrNull() ?: 0
    }

private fun resolveTrackerCopy(
    uiModel: TransferUiModel,
    language: AppLanguage
): TransferTrackerCopy {
    val destination = resolveDestinationSummary(uiModel, language)
    val statusText = when (uiModel.transferStatus) {
        TransferStatus.REVIEWING -> {
            if (uiModel.destinationMode == TransferDestinationMode.ACCOUNT) {
                language.text("transfer_moving_in_progress")
            } else {
                language.text("transfer_sending_in_progress")
            }
        }
        TransferStatus.CONFIRMED -> {
            if (uiModel.destinationMode == TransferDestinationMode.ACCOUNT) {
                language.text("transfer_moved")
            } else {
                language.text("transfer_sent")
            }
        }

        TransferStatus.FAILED -> language.text("transfer_failed")
        else -> language.text("transfer_checking")
    }

    return TransferTrackerCopy(
        headline = destination.title,
        statusText = statusText
    )
}

private fun resolveReviewCopy(
    uiModel: TransferUiModel,
    language: AppLanguage
): TransferReviewCopy =
    if (uiModel.destinationMode == TransferDestinationMode.ACCOUNT) {
        TransferReviewCopy(
            recipientDetail = uiModel.selectedRecipientAccountLabel,
            headline = language.text("transfer_to_my_account_format", uiModel.selectedRecipientName),
            headlineSuffix = language.text("transfer_move_question"),
            recipientLabel = language.text("transfer_display_to_recipient"),
            recipientDetailLabel = language.text("transfer_deposit_account"),
            recipientDetailFontFamily = null,
            confirmActionText = language.text("transfer_move_action")
        )
    } else {
        TransferReviewCopy(
            recipientDetail = uiModel.selectedRecipientWalletLabel,
            headline = language.text("transfer_to_wallet_format", uiModel.selectedRecipientName),
            headlineSuffix = language.text("transfer_send_question"),
            recipientLabel = language.text("transfer_display_to_recipient"),
            recipientDetailLabel = language.text("transfer_wallet_address_label"),
            recipientDetailFontFamily = FontFamily.Monospace,
            confirmActionText = language.text("transfer_send_action")
        )
    }

private fun resolveAccountBadgeText(accountName: String, language: AppLanguage): String =
    accountName.trim().take(2).ifBlank { language.text("account") }

private fun appendTransferAmountInput(
    current: String,
    next: String
): String {
    val candidate = if (current == "0") {
        next
    } else {
        current + next
    }
    return candidate.trimStart('0').ifBlank { "0" }.take(TransferAmountMaxDigits)
}

private fun removeLastTransferAmountDigit(current: String): String =
    current
        .filter(Char::isDigit)
        .dropLast(1)
