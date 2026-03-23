package com.dondone.mobile.feature.finance.presentation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dondone.mobile.app.session.RemittanceActionUiState
import com.dondone.mobile.app.session.RemittanceSubmittingAction
import com.dondone.mobile.core.designsystem.DawnPrimary
import com.dondone.mobile.core.designsystem.DawnText
import com.dondone.mobile.core.designsystem.DawnTextSubtle
import com.dondone.mobile.feature.recipient.presentation.RecipientWalletAddBottomSheet
import com.dondone.mobile.feature.recipient.presentation.resolveRecipientSheetErrorMessage
import com.dondone.mobile.feature.recipient.presentation.shouldCloseRecipientSheetAfterResult
import kotlin.math.absoluteValue

private val AccountManageCanvas = Color.White
private val AccountManageDivider = Color(0xFFE8EBF0)
private val AccountManageButtonSurface = Color(0xFFF2F4F7)
private val AccountManageButtonText = Color(0xFF646F7C)
private val AccountManageSummaryLabel = Color(0xFF9098A4)
private val AccountManageAccentSurface = Color(0xFFE9F2FF)
private val AccountManageInputBorder = Color(0xFFD9E1EA)
private val AccountManageError = Color(0xFFC93C37)
private val AccountManageSuccessSurface = Color(0xFFF5F8FC)
private val AccountManagePalette = listOf(
    Color(0xFF1376D3),
    Color(0xFF0E9B7D),
    Color(0xFF0D4A94),
    Color(0xFFF5B313),
    Color(0xFFFFCC00),
    Color(0xFFA9CFFF),
    Color(0xFF2563EB)
)
private val RecipientRelationOptions = listOf(
    "FAMILY" to "가족",
    "SPOUSE" to "배우자",
    "PARENT" to "부모",
    "CHILD" to "자녀",
    "SIBLING" to "형제자매",
    "FRIEND" to "친구",
    "OTHER" to "기타"
)

private enum class RecipientWalletSheetMode {
    ADD,
    EDIT
}

@Composable
fun AccountManageScreen(
    uiModel: AccountManageUiModel,
    actionUiState: RemittanceActionUiState,
    onSelectAccount: (String) -> Unit,
    onAddRecipient: (String, String, String, Long?) -> Unit,
    onUpdateRecipient: (String, String, String, String) -> Unit,
    onSearchRecipientsByPhone: (String) -> Unit,
    onClearPhoneSearch: () -> Unit
) {
    var activeSheetMode by remember { mutableStateOf<RecipientWalletSheetMode?>(null) }
    var editingRecipientId by remember { mutableStateOf<String?>(null) }
    var awaitingSubmissionAction by remember { mutableStateOf<RemittanceSubmittingAction?>(null) }
    val editingRecipient = remember(uiModel.recipientWallets, editingRecipientId) {
        uiModel.recipientWallets.firstOrNull { it.id == editingRecipientId }
    }
    val isRecipientSubmitting = actionUiState.isSubmitting && (
        actionUiState.submittingAction == RemittanceSubmittingAction.RECIPIENT_CREATE ||
            actionUiState.submittingAction == RemittanceSubmittingAction.RECIPIENT_UPDATE
    )
    val sheetErrorMessage = resolveRecipientSheetErrorMessage(
        isAwaitingResult = awaitingSubmissionAction != null,
        actionUiState = actionUiState
    )

    LaunchedEffect(
        awaitingSubmissionAction,
        actionUiState.isSubmitting,
        actionUiState.message,
        actionUiState.isError
    ) {
        if (!shouldCloseRecipientSheetAfterResult(awaitingSubmissionAction != null, actionUiState)) {
            return@LaunchedEffect
        }
        onClearPhoneSearch()
        activeSheetMode = null
        editingRecipientId = null
        awaitingSubmissionAction = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AccountManageCanvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        AccountSummary(
            label = uiModel.totalBalanceLabel,
            amount = uiModel.totalBalanceAmountText,
            unit = uiModel.totalBalanceUnitText
        )

        Spacer(modifier = Modifier.height(28.dp))

        ManageSection(
            title = uiModel.accountSectionTitle,
            actionText = uiModel.accountActionText
        ) {
            uiModel.accounts.forEachIndexed { index, account ->
                ManageRow(
                    title = account.name,
                    subtitle = account.number,
                    valueText = account.balanceText,
                    actionText = if (account.selected) "대표" else "선택",
                    copyText = account.copyNumber,
                    onClick = { onSelectAccount(account.id) }
                )
                if (index != uiModel.accounts.lastIndex) {
                    HorizontalDivider(color = AccountManageDivider)
                }
            }
        }

        ManageSectionDivider()

        ManageSection(
            title = uiModel.recipientSectionTitle,
            actionText = uiModel.recipientActionText,
            onActionClick = {
                awaitingSubmissionAction = null
                editingRecipientId = null
                activeSheetMode = RecipientWalletSheetMode.ADD
            }
        ) {
            if (uiModel.recipientWallets.isEmpty()) {
                EmptyRecipientState(
                    onAddWallet = {
                        awaitingSubmissionAction = null
                        editingRecipientId = null
                        activeSheetMode = RecipientWalletSheetMode.ADD
                    }
                )
            } else {
                uiModel.recipientWallets.forEachIndexed { index, wallet ->
                    ManageRow(
                        title = wallet.name,
                        subtitle = "${wallet.relationLabel} · ${wallet.address.toShortWalletAddress()}",
                        valueText = if (wallet.selected) "기본 수신자" else null,
                        actionText = "수정",
                        copyText = wallet.address,
                        onClick = {
                            awaitingSubmissionAction = null
                            editingRecipientId = wallet.id
                            activeSheetMode = RecipientWalletSheetMode.EDIT
                        }
                    )
                    if (index != uiModel.recipientWallets.lastIndex) {
                        HorizontalDivider(color = AccountManageDivider)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (activeSheetMode == RecipientWalletSheetMode.ADD) {
        RecipientWalletAddBottomSheet(
            phoneDirectory = uiModel.phoneDirectory,
            supportsRemotePhoneSearch = uiModel.supportsRemotePhoneSearch,
            phoneSearchResults = uiModel.phoneSearchResults,
            isPhoneSearchLoading = uiModel.isPhoneSearchLoading,
            phoneSearchErrorMessage = uiModel.phoneSearchErrorMessage,
            isSubmitting = isRecipientSubmitting,
            submitErrorMessage = sheetErrorMessage,
            onDismiss = {
                if (!isRecipientSubmitting) {
                    onClearPhoneSearch()
                    activeSheetMode = null
                    editingRecipientId = null
                    awaitingSubmissionAction = null
                }
            },
            onSubmit = { alias, relation, walletAddress, targetUserId ->
                awaitingSubmissionAction = RemittanceSubmittingAction.RECIPIENT_CREATE
                onAddRecipient(alias, relation, walletAddress, targetUserId)
            },
            onSearchByPhone = onSearchRecipientsByPhone,
            onClearPhoneSearch = onClearPhoneSearch
        )
    }

    if (activeSheetMode == RecipientWalletSheetMode.EDIT && editingRecipient != null) {
        EditRecipientWalletBottomSheet(
            recipient = editingRecipient,
            isSubmitting = isRecipientSubmitting,
            submitErrorMessage = sheetErrorMessage,
            onDismiss = {
                if (!isRecipientSubmitting) {
                    activeSheetMode = null
                    editingRecipientId = null
                    awaitingSubmissionAction = null
                }
            },
            onSubmit = { alias, relation, walletAddress ->
                awaitingSubmissionAction = RemittanceSubmittingAction.RECIPIENT_UPDATE
                onUpdateRecipient(editingRecipient.id, alias, relation, walletAddress)
            }
        )
    }
}

@Composable
private fun AccountSummary(
    label: String,
    amount: String,
    unit: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = AccountManageSummaryLabel
        )
        Text(
            text = buildAnnotatedString {
                append(amount)
                unit?.let {
                    append(" ")
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    ) {
                        append(it)
                    }
                }
            },
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
            color = DawnText
        )
    }
}

@Composable
private fun ManageSection(
    title: String,
    actionText: String?,
    onActionClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = DawnText
                )
                if (actionText != null) {
                    Text(
                        modifier = if (onActionClick != null) {
                            Modifier.clickable(onClick = onActionClick)
                        } else {
                            Modifier
                        },
                        text = actionText,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = if (onActionClick != null) DawnPrimary else DawnTextSubtle
                    )
                }
            }
            content()
        }
    )
}

@Composable
private fun ManageSectionDivider() {
    Spacer(modifier = Modifier.height(20.dp))
    HorizontalDivider(color = AccountManageDivider)
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
private fun EmptyRecipientState(
    onAddWallet: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AccountManageSuccessSurface, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "등록된 수신 지갑이 없어요.",
            style = MaterialTheme.typography.bodyMedium,
            color = DawnText
        )
        Text(
            text = "휴대폰 번호로 먼저 찾고, 없으면 지갑 주소를 직접 입력할 수 있어요.",
            style = MaterialTheme.typography.bodySmall,
            color = AccountManageSummaryLabel
        )
        Box(
            modifier = Modifier
                .background(AccountManageAccentSurface, RoundedCornerShape(12.dp))
                .clickable(onClick = onAddWallet)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "지갑 추가",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                color = DawnPrimary
            )
        }
    }
}

@Composable
private fun ManageRow(
    title: String,
    subtitle: String,
    valueText: String?,
    actionText: String?,
    copyText: String?,
    onClick: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = copyText?.let {
                    {
                        clipboardManager.setText(AnnotatedString(it))
                        Toast.makeText(context, "지갑 주소를 복사했어요.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccountBadge(title = title)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccountManageSummaryLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (valueText != null) {
                    Text(
                        text = valueText,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = DawnText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = DawnTextSubtle,
                    overflow = TextOverflow.Clip
                )
            }
        }
        if (actionText != null) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 0.dp, minHeight = 0.dp)
                    .background(AccountManageButtonSurface, RoundedCornerShape(8.dp))
                    .combinedClickable(onClick = onClick)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = AccountManageButtonText
                )
            }
        }
    }
}

@Composable
private fun AccountBadge(title: String) {
    val color = AccountManagePalette[title.hashCode().absoluteValue % AccountManagePalette.size]
    val badgeText = title.trim().take(1).ifBlank { "•" }

    Box(
        modifier = Modifier
            .size(48.dp)
            .background(color = color, shape = RoundedCornerShape(999.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = badgeText,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = Color.White
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditRecipientWalletBottomSheet(
    recipient: RecipientWalletUiModel,
    isSubmitting: Boolean,
    submitErrorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var alias by remember(recipient.id) { mutableStateOf(recipient.name) }
    var walletAddress by remember(recipient.id) { mutableStateOf(recipient.address) }
    var relation by remember(recipient.id) { mutableStateOf(recipient.relationCode) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val trimmedAlias = alias.trim()
    val trimmedWalletAddress = walletAddress.trim()
    val addressValid = remember(trimmedWalletAddress) { trimmedWalletAddress.isLikelyWalletAddress() }
    val hasChanges = trimmedAlias != recipient.name ||
        trimmedWalletAddress != recipient.address ||
        relation != recipient.relationCode
    val canSubmit = !isSubmitting && trimmedAlias.isNotBlank() && addressValid && hasChanges

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.32f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "수신 지갑 수정",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = DawnText
            )
            Text(
                text = "표시 이름, 관계, 지갑 주소를 현재 디자인 흐름 안에서 바로 업데이트할 수 있어요.",
                style = MaterialTheme.typography.bodyMedium,
                color = DawnTextSubtle
            )

            ManualAddressForm(
                alias = alias,
                onAliasChange = { alias = it },
                walletAddress = walletAddress,
                onWalletAddressChange = { walletAddress = it }
            )

            RelationSelector(
                selectedRelation = relation,
                onSelectRelation = { relation = it }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AccountManageSuccessSurface, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "안내",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccountManageSummaryLabel
                    )
                    Text(
                        text = "지갑 주소를 바꾸면 다음 송금에서 다시 확인 문구가 보일 수 있어요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DawnTextSubtle
                    )
                }
            }

            if (submitErrorMessage != null) {
                Text(
                    text = submitErrorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = AccountManageError
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (canSubmit) DawnPrimary else AccountManageInputBorder,
                        RoundedCornerShape(18.dp)
                    )
                    .clickable(enabled = canSubmit) {
                        onSubmit(trimmedAlias, relation, trimmedWalletAddress)
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "변경 저장하기",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = if (canSubmit) Color.White else AccountManageButtonText
                    )
                }
            }

            Text(
                text = "현재는 테스트넷 데모입니다. 실제 자금 이동이 발생하지 않습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = AccountManageSummaryLabel
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ManualAddressForm(
    alias: String,
    onAliasChange: (String) -> Unit,
    walletAddress: String,
    onWalletAddressChange: (String) -> Unit
) {
    val addressValid = walletAddress.trim().isLikelyWalletAddress()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = alias,
            onValueChange = onAliasChange,
            singleLine = true,
            label = { Text("별칭") },
            shape = RoundedCornerShape(16.dp)
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = walletAddress,
            onValueChange = onWalletAddressChange,
            singleLine = true,
            label = { Text("지갑 주소") },
            placeholder = { Text("0x...") },
            supportingText = {
                Text(
                    text = if (walletAddress.isBlank() || addressValid) {
                        "테스트넷 EVM 지갑 주소를 입력해 주세요."
                    } else {
                        "0x로 시작하는 42자리 주소를 입력해 주세요."
                    },
                    color = if (walletAddress.isBlank() || addressValid) AccountManageSummaryLabel else AccountManageError
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun RelationSelector(
    selectedRelation: String,
    onSelectRelation: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "관계",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
            color = DawnText
        )
        RecipientRelationOptions.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { (code, label) ->
                    val selected = selectedRelation == code
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selected) AccountManageAccentSurface else Color.White,
                                RoundedCornerShape(14.dp)
                            )
                            .border(
                                1.dp,
                                if (selected) DawnPrimary else AccountManageInputBorder,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectRelation(code) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = if (selected) DawnPrimary else DawnTextSubtle
                        )
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun String.toShortWalletAddress(): String {
    return if (length <= 14) {
        this
    } else {
        "${take(8)}...${takeLast(6)}"
    }
}

private fun String.isLikelyWalletAddress(): Boolean {
    return matches(Regex("^0x[a-fA-F0-9]{40}$"))
}
