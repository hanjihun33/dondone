package com.dondone.mobile.feature.menu.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dondone.mobile.app.session.MenuLaunchRequest
import com.dondone.mobile.app.session.MenuLaunchTarget
import com.dondone.mobile.app.session.ProfileUpdateUiState
import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.designsystem.DawnBorder
import com.dondone.mobile.core.designsystem.DawnPrimary
import com.dondone.mobile.core.designsystem.DawnPrimaryDeep
import com.dondone.mobile.core.designsystem.DawnSecondary
import com.dondone.mobile.core.designsystem.DawnSuccess
import com.dondone.mobile.core.designsystem.DawnSurface
import com.dondone.mobile.core.designsystem.DawnSurfaceAlt
import com.dondone.mobile.core.designsystem.DawnText
import com.dondone.mobile.core.designsystem.DawnTextSubtle
import com.dondone.mobile.core.designsystem.DonDoneLoadingPanel
import com.dondone.mobile.core.designsystem.PrimaryActionButton
import com.dondone.mobile.core.designsystem.SecondaryActionButton
import com.dondone.mobile.core.designsystem.StatusBadge
import com.dondone.mobile.core.designsystem.pressableScale
import com.dondone.mobile.core.designsystem.rememberDonDoneGrayRipple
import com.dondone.mobile.feature.workproof.presentation.WorkproofPdfFileAction
import com.dondone.mobile.feature.workproof.presentation.WorkproofPdfFileUiState

private data class MenuServiceAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

private data class MenuDocumentColors(
    val iconBackground: Color,
    val iconColor: Color
)

private enum class MenuOverlaySheet {
    Claim,
    Profile,
    Receipt,
    Settings
}

private enum class ClaimSummaryTone {
    Default,
    Firm,
    Short
}

private val MenuCanvas = Color.White
private val MenuDivider = Color(0xFFE8EBF0)
private val MenuReceiptHashBackground = Color(0xFFF1F5F9)
private val MenuReceiptHashBorder = Color(0xFFE2E8F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    uiModel: MenuUiModel,
    workproofPdfFileUiState: WorkproofPdfFileUiState,
    launchRequest: MenuLaunchRequest?,
    profileUpdateUiState: ProfileUpdateUiState,
    onOpenWage: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenWorkproofPdf: (Long) -> Unit,
    onShareWorkproofPdf: (Long) -> Unit,
    onClearPdfFileState: () -> Unit,
    onConsumeLaunchRequest: () -> Unit,
    onUpdateProfile: (String, String) -> Unit,
    onClearProfileUpdateMessage: () -> Unit,
    onLogout: () -> Unit,
    onShowToast: (String, BadgeTone) -> Unit
) {
    val context = LocalContext.current
    var activeSheet by rememberSaveable { mutableStateOf<MenuOverlaySheet?>(null) }
    var selectedLanguage by rememberSaveable { mutableStateOf("ko") }
    var selectedDocumentId by remember { mutableStateOf<String?>(null) }
    var awaitingProfileUpdateResult by remember { mutableStateOf(false) }

    val selectedDocument = uiModel.documents.firstOrNull { it.id == selectedDocumentId }
    val proofDocument = uiModel.documents.firstOrNull { it.accent == MenuDocumentAccent.Proof }
    val claimDocument = uiModel.documents.firstOrNull { it.accent == MenuDocumentAccent.Claim }
    val selectedReceipt = if (activeSheet == MenuOverlaySheet.Receipt) uiModel.receipt else null

    LaunchedEffect(workproofPdfFileUiState.fileUri, workproofPdfFileUiState.pendingAction) {
        val fileUri = workproofPdfFileUiState.fileUri ?: return@LaunchedEffect
        val action = workproofPdfFileUiState.pendingAction ?: return@LaunchedEffect
        val uri = Uri.parse(fileUri)
        when (action) {
            WorkproofPdfFileAction.OPEN -> openMenuWorkproofPdfFile(context, uri)
            WorkproofPdfFileAction.SHARE -> shareMenuWorkproofPdfFile(context, uri, workproofPdfFileUiState.fileName)
        }
        onClearPdfFileState()
    }

    LaunchedEffect(workproofPdfFileUiState.errorMessage) {
        val message = workproofPdfFileUiState.errorMessage ?: return@LaunchedEffect
        onShowToast(message, BadgeTone.Warning)
        onClearPdfFileState()
    }

    LaunchedEffect(launchRequest?.requestId, proofDocument?.id, claimDocument?.id) {
        val request = launchRequest ?: return@LaunchedEffect
        when (request.target) {
            MenuLaunchTarget.PROOF_DOCUMENT -> {
                val document = proofDocument ?: return@LaunchedEffect
                selectedDocumentId = document.id
            }
            MenuLaunchTarget.CLAIM_DOCUMENT -> {
                val document = claimDocument ?: return@LaunchedEffect
                selectedDocumentId = document.id
            }
            MenuLaunchTarget.CLAIM_SHEET -> {
                activeSheet = MenuOverlaySheet.Claim
            }
        }
        onConsumeLaunchRequest()
    }

    LaunchedEffect(
        awaitingProfileUpdateResult,
        profileUpdateUiState.isSubmitting,
        profileUpdateUiState.message,
        profileUpdateUiState.isError
    ) {
        if (!awaitingProfileUpdateResult || profileUpdateUiState.isSubmitting || profileUpdateUiState.message == null) {
            return@LaunchedEffect
        }
        if (profileUpdateUiState.isError) {
            awaitingProfileUpdateResult = false
            return@LaunchedEffect
        }
        activeSheet = null
        awaitingProfileUpdateResult = false
        onShowToast(profileUpdateUiState.message, BadgeTone.Success)
        onClearProfileUpdateMessage()
    }

    val serviceActions = buildList {
        add(MenuServiceAction("급여 점검", Icons.Default.Description, onOpenWage))
        add(MenuServiceAction("계좌 지갑 관리", Icons.Default.AccountBalanceWallet, onOpenAccount))
        add(MenuServiceAction("설정", Icons.Default.Settings) { activeSheet = MenuOverlaySheet.Settings })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MenuCanvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        if (uiModel.session != null) {
            MenuSessionSection(
                session = uiModel.session,
                onEditProfile = {
                    awaitingProfileUpdateResult = false
                    onClearProfileUpdateMessage()
                    activeSheet = MenuOverlaySheet.Profile
                },
                onLogout = onLogout
            )
            MenuSectionDivider()
        }
        MenuServicesSection(actions = serviceActions)
        MenuSectionDivider()
        MenuDocumentsSection(
            documents = uiModel.documents,
            onOpenDetail = { document ->
                when {
                    document.accent == MenuDocumentAccent.Receipt && uiModel.receipt != null -> {
                        activeSheet = MenuOverlaySheet.Receipt
                    }
                    document.accent == MenuDocumentAccent.Claim && document.statusTone != BadgeTone.Success -> {
                        activeSheet = MenuOverlaySheet.Claim
                    }
                    else -> {
                        selectedDocumentId = document.id
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (activeSheet == MenuOverlaySheet.Claim) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            containerColor = DawnSurface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            MenuClaimSheet(
                proofDocument = proofDocument,
                claimDocument = claimDocument,
                onOpenProofDocument = {
                    activeSheet = null
                    selectedDocumentId = proofDocument?.id
                },
                onOpenClaimDocument = {
                    activeSheet = null
                    selectedDocumentId = claimDocument?.id
                },
                onOpenWage = {
                    activeSheet = null
                    onOpenWage()
                },
                onDismiss = { activeSheet = null }
            )
        }
    }

    if (activeSheet == MenuOverlaySheet.Settings) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            containerColor = DawnSurface
        ) {
            MenuSettingsSheet(
                selectedLanguage = selectedLanguage,
                onSelectLanguage = { selectedLanguage = it },
                onDismiss = { activeSheet = null }
            )
        }
    }

    if (activeSheet == MenuOverlaySheet.Profile && uiModel.session != null) {
        ModalBottomSheet(
            onDismissRequest = {
                if (!profileUpdateUiState.isSubmitting) {
                    awaitingProfileUpdateResult = false
                    onClearProfileUpdateMessage()
                    activeSheet = null
                }
            },
            containerColor = DawnSurface
        ) {
            MenuProfileSheet(
                session = uiModel.session,
                uiState = profileUpdateUiState,
                onSubmit = { name, phoneNumber ->
                    awaitingProfileUpdateResult = true
                    onUpdateProfile(name, phoneNumber)
                },
                onDismiss = {
                    if (!profileUpdateUiState.isSubmitting) {
                        awaitingProfileUpdateResult = false
                        onClearProfileUpdateMessage()
                        activeSheet = null
                    }
                }
            )
        }
    }

    if (selectedDocument != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedDocumentId = null },
            containerColor = DawnSurface
        ) {
            MenuDocumentSheet(
                document = selectedDocument,
                onOpenProofDocument = {
                    val documentId = selectedDocument.documentId
                    if (documentId == null) {
                        onShowToast("아직 준비된 문서가 없어요.", BadgeTone.Warning)
                    } else {
                        selectedDocumentId = null
                        onOpenWorkproofPdf(documentId)
                    }
                },
                onShareProofDocument = {
                    val documentId = selectedDocument.documentId
                    if (documentId == null) {
                        onShowToast("아직 공유할 문서가 없어요.", BadgeTone.Warning)
                    } else {
                        selectedDocumentId = null
                        onShareWorkproofPdf(documentId)
                    }
                },
                onOpenClaimFlow = {
                    selectedDocumentId = null
                    activeSheet = MenuOverlaySheet.Claim
                },
                onDismiss = { selectedDocumentId = null }
            )
        }
    }

    if (selectedReceipt != null) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            containerColor = DawnSurface
        ) {
            MenuReceiptSheet(
                receipt = selectedReceipt,
                onOpenExplorer = {
                    if (!openMenuReceiptExplorer(context, selectedReceipt.explorerUrl)) {
                        onShowToast("Explorer를 열 수 없어요.", BadgeTone.Warning)
                    }
                },
                onShare = {
                    if (!shareMenuReceipt(context, selectedReceipt.shareText)) {
                        onShowToast("공유할 수 없어요.", BadgeTone.Warning)
                    }
                },
                onDismiss = { activeSheet = null }
            )
        }
    }
}

private fun openMenuReceiptExplorer(
    context: Context,
    url: String
) : Boolean {
    return runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }.isSuccess
}

private fun openMenuWorkproofPdfFile(
    context: Context,
    uri: Uri
): Boolean {
    return runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        )
    }.isSuccess
}

private fun shareMenuWorkproofPdfFile(
    context: Context,
    uri: Uri,
    fileName: String?
): Boolean {
    return runCatching {
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TITLE, fileName ?: "근무 기록 문서")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "근무 기록 문서 공유"
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        )
    }.isSuccess
}

private fun shareMenuReceipt(
    context: Context,
    shareText: String
) : Boolean {
    return runCatching {
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                },
                "영수증 공유"
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }.isSuccess
}

@Composable
private fun MenuSessionSection(
    session: MenuSessionUiModel,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit
) {
    MenuSectionSurface {
        MenuSectionHeader(title = "계정")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = session.name,
                style = MaterialTheme.typography.titleMedium,
                color = DawnText
            )
            Text(
                text = session.email,
                style = MaterialTheme.typography.bodyMedium,
                color = DawnTextSubtle
            )
            session.phoneNumber?.let { phoneNumber ->
                Text(
                    text = phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DawnTextSubtle
                )
            }
        }
        SecondaryActionButton(
            text = "내 정보 수정",
            onClick = onEditProfile,
            modifier = Modifier.fillMaxWidth()
        )
        SecondaryActionButton(
            text = "로그아웃",
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MenuProfileSheet(
    session: MenuSessionUiModel,
    uiState: ProfileUpdateUiState,
    onSubmit: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(session.name) { mutableStateOf(session.name) }
    var phoneNumber by remember(session.phoneNumber) { mutableStateOf(session.phoneNumber.orEmpty()) }
    val canSubmit = !uiState.isSubmitting && name.trim().isNotBlank() && phoneNumber.filter(Char::isDigit).length in 10..11

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "내 정보 수정",
            style = MaterialTheme.typography.titleLarge,
            color = DawnText
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = name,
            onValueChange = { name = it },
            singleLine = true,
            label = { Text("이름") },
            shape = RoundedCornerShape(16.dp)
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            singleLine = true,
            label = { Text("휴대폰 번호") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(16.dp)
        )
        if (uiState.message != null) {
            Text(
                text = uiState.message,
                style = MaterialTheme.typography.bodySmall,
                color = if (uiState.isError) Color(0xFFC93C37) else DawnSuccess
            )
        }
        PrimaryActionButton(
            text = if (uiState.isSubmitting) "저장 중..." else "저장하기",
            onClick = { onSubmit(name, phoneNumber) },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth()
        )
        SecondaryActionButton(
            text = "닫기",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MenuServicesSection(
    actions: List<MenuServiceAction>
) {
    MenuSectionSurface {
        MenuSectionHeader(title = "서비스")

        actions.forEachIndexed { index, action ->
            MenuServiceActionRow(
                action = action,
                showDivider = index != actions.lastIndex
            )
        }
    }
}

@Composable
private fun MenuServiceActionRow(
    action: MenuServiceAction,
    showDivider: Boolean
) {
    MenuSectionListRow(
        showDivider = showDivider,
        onClick = action.onClick
    ) {
        MenuServiceLeadingIcon(icon = action.icon)
        Text(
            text = action.label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = DawnTextSubtle,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun MenuServiceLeadingIcon(
    icon: ImageVector
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DawnSurfaceAlt),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DawnPrimary
        )
    }
}

@Composable
private fun MenuSectionListRow(
    showDivider: Boolean,
    onClick: () -> Unit,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .pressableScale(
                    interactionSource = interactionSource,
                    pressedScale = 0.99f
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = rememberDonDoneGrayRipple(bounded = true),
                    onClick = onClick
                )
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = verticalAlignment,
            content = content
        )

        if (showDivider) {
            HorizontalDivider(color = MenuDivider)
        }
    }
}

@Composable
private fun MenuDocumentsSection(
    documents: List<MenuDocumentUiModel>,
    onOpenDetail: (MenuDocumentUiModel) -> Unit
) {
    MenuSectionSurface {
        MenuSectionHeader(title = "문서")

        documents.forEachIndexed { index, document ->
            MenuDocumentRow(
                document = document,
                onOpenDetail = { onOpenDetail(document) },
                showDivider = index != documents.lastIndex
            )
        }
    }
}

@Composable
private fun MenuDocumentRow(
    document: MenuDocumentUiModel,
    onOpenDetail: () -> Unit,
    showDivider: Boolean
) {
    val colors = menuDocumentColors(document.accent)

    MenuSectionListRow(
        showDivider = showDivider,
        onClick = onOpenDetail,
        verticalAlignment = Alignment.Top
    ) {
        MenuDocumentAccentBox(document.accent, colors)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = DawnText
                    )
                    Text(
                        text = document.updatedAtText,
                        style = MaterialTheme.typography.labelLarge,
                        color = DawnTextSubtle
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(
                        text = document.statusText,
                        tone = document.statusTone
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = DawnTextSubtle,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = document.summaryText,
                style = MaterialTheme.typography.labelMedium,
                color = DawnTextSubtle,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MenuDocumentActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    primary: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val background = when {
        primary && enabled -> DawnSecondary
        primary -> DawnSurfaceAlt
        else -> DawnSurface
    }
    val contentColor = when {
        primary && enabled -> DawnPrimaryDeep
        enabled -> DawnText
        else -> DawnTextSubtle
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .pressableScale(
                interactionSource = interactionSource,
                enabled = enabled,
                pressedScale = 0.98f
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = rememberDonDoneGrayRipple(),
                onClick = onClick
            ),
        shape = RoundedCornerShape(18.dp),
        color = background,
        border = BorderStroke(1.dp, DawnBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MenuDocumentAccentBox(
    accent: MenuDocumentAccent,
    colors: MenuDocumentColors
) {
    val icon = when (accent) {
        MenuDocumentAccent.Proof -> Icons.Default.Description
        MenuDocumentAccent.Claim -> Icons.Default.Warning
        MenuDocumentAccent.Receipt -> Icons.Default.AccountBalanceWallet
    }

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.iconBackground),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.iconColor
        )
    }
}

@Composable
private fun MenuSectionSurface(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}

@Composable
private fun MenuSectionHeader(
    title: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = DawnText
    )
}

@Composable
private fun MenuSectionDivider() {
    Spacer(modifier = Modifier.height(14.dp))
    HorizontalDivider(color = MenuDivider)
    Spacer(modifier = Modifier.height(14.dp))
}

@Composable
private fun MenuSheetHeader(
    title: String,
    subtitle: String? = null,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = DawnText
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DawnTextSubtle
                )
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onDismiss)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "닫기",
                style = MaterialTheme.typography.labelLarge,
                color = DawnTextSubtle
            )
        }
    }
}

@Composable
private fun MenuSheetSection(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                color = DawnText
            )
        }
        content()
    }
}

private fun menuDocumentColors(accent: MenuDocumentAccent): MenuDocumentColors {
    return when (accent) {
        MenuDocumentAccent.Proof -> MenuDocumentColors(
            iconBackground = DawnSecondary,
            iconColor = DawnPrimaryDeep
        )
        MenuDocumentAccent.Claim -> MenuDocumentColors(
            iconBackground = Color(0xFFF1F5F9),
            iconColor = Color(0xFF475569)
        )
        MenuDocumentAccent.Receipt -> MenuDocumentColors(
            iconBackground = Color(0xFFECFDF3),
            iconColor = Color(0xFF047857)
        )
    }
}

@Composable
private fun MenuDocumentSheet(
    document: MenuDocumentUiModel,
    onOpenProofDocument: () -> Unit,
    onShareProofDocument: () -> Unit,
    onOpenClaimFlow: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        MenuSheetHeader(
            title = document.title,
            subtitle = document.summaryText,
            onDismiss = onDismiss
        )

        MenuSheetSection {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "현재 상태", style = MaterialTheme.typography.bodyLarge)
                StatusBadge(text = document.statusText, tone = document.statusTone)
            }
            Text(
                text = document.updatedAtText,
                style = MaterialTheme.typography.bodyMedium,
                color = DawnTextSubtle
            )
            Text(
                text = when (document.accent) {
                    MenuDocumentAccent.Proof -> "근무 탭에서 선택한 기간의 출퇴근 기록과 변경 이력을 다시 열고 공유할 수 있는 문서예요."
                    MenuDocumentAccent.Claim -> "차액 검토 결과를 토대로 신고 준비 흐름으로 이어지는 묶음 문서예요."
                    MenuDocumentAccent.Receipt -> "최근 테스트넷 송금 내역과 전송 해시를 확인할 수 있는 영수증 문서예요."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = DawnText
            )
        }

        if (document.accent == MenuDocumentAccent.Proof) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MenuDocumentActionButton(
                    text = "공유",
                    icon = Icons.Default.Share,
                    onClick = onShareProofDocument,
                    enabled = document.statusTone == BadgeTone.Success,
                    primary = false,
                    modifier = Modifier.weight(1f)
                )
                MenuDocumentActionButton(
                    text = "열기",
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    onClick = onOpenProofDocument,
                    enabled = document.statusTone == BadgeTone.Success,
                    primary = true,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            PrimaryActionButton(
                text = if (document.accent == MenuDocumentAccent.Claim) "신고 준비" else "확인",
                onClick = if (document.accent == MenuDocumentAccent.Claim) onOpenClaimFlow else onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MenuReceiptSheet(
    receipt: MenuReceiptUiModel,
    onOpenExplorer: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val statusTone = if (receipt.status == MenuReceiptStatus.Confirmed) {
        BadgeTone.Success
    } else {
        BadgeTone.Warning
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        MenuSheetHeader(
            title = receipt.title,
            subtitle = receipt.statusDetailText,
            onDismiss = onDismiss
        )

        MenuSheetSection {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "현재 상태", style = MaterialTheme.typography.bodyLarge)
                StatusBadge(text = receipt.statusText, tone = statusTone)
            }
            Text(
                text = receipt.updatedAtText,
                style = MaterialTheme.typography.bodyMedium,
                color = DawnTextSubtle
            )
        }

        MenuSheetSection {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(MenuReceiptHashBackground)
                    .border(1.dp, MenuReceiptHashBorder, RoundedCornerShape(22.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = receipt.txHashSectionTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = DawnTextSubtle
                    )
                    Text(
                        text = receipt.networkLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = DawnPrimaryDeep
                    )
                }
                Text(
                    text = receipt.txHashLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = DawnText
                )
                Text(
                    text = receipt.txHashFullText,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = DawnTextSubtle
                )
            }
            Text(
                text = receipt.helperText,
                style = MaterialTheme.typography.bodySmall,
                color = DawnTextSubtle
            )
        }

        receipt.pendingNoticeText?.let { pendingNoticeText ->
            MenuSheetSection {
                DonDoneLoadingPanel(
                    title = "네트워크 확인 중",
                    message = pendingNoticeText
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SecondaryActionButton(
                text = receipt.shareButtonText,
                onClick = onShare,
                modifier = Modifier.weight(1f)
            )
            PrimaryActionButton(
                text = receipt.explorerButtonText,
                onClick = onOpenExplorer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MenuClaimSheet(
    proofDocument: MenuDocumentUiModel?,
    claimDocument: MenuDocumentUiModel?,
    onOpenProofDocument: () -> Unit,
    onOpenClaimDocument: () -> Unit,
    onOpenWage: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTone by rememberSaveable { mutableStateOf(ClaimSummaryTone.Default) }
    val summaryText = when (selectedTone) {
        ClaimSummaryTone.Firm -> "근무 기록과 차액 근거를 바탕으로 접수 사실을 정중하게 정리한 초안입니다. 제출 전에 문장을 한 번 더 다듬어 주세요."
        ClaimSummaryTone.Short -> "차액과 근거를 짧게 정리해 신고 준비를 빠르게 시작할 수 있도록 요약한 문장입니다."
        ClaimSummaryTone.Default -> "자동 제출이 아니라 제출 전에 문장과 자료를 빠르게 정리해 보는 데모 단계예요."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        MenuSheetHeader(
            title = "신고 준비",
            subtitle = "필요한 문서와 접수 경로를 한 화면에서 정리해 둘 수 있어요.",
            onDismiss = onDismiss
        )

        MenuSheetSection {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "제출 문장 요약", style = MaterialTheme.typography.bodyLarge)
                    SecondaryActionButton(
                        text = "문장 만들기",
                        onClick = { selectedTone = ClaimSummaryTone.Default }
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MenuSheetChip("기본", selectedTone == ClaimSummaryTone.Default) {
                        selectedTone = ClaimSummaryTone.Default
                    }
                    MenuSheetChip("정중하게", selectedTone == ClaimSummaryTone.Firm) {
                        selectedTone = ClaimSummaryTone.Firm
                    }
                    MenuSheetChip("짧게", selectedTone == ClaimSummaryTone.Short) {
                        selectedTone = ClaimSummaryTone.Short
                    }
                }
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DawnText
                )
                SecondaryActionButton(
                    text = "복사",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "이 문장은 데모용 참고 문장입니다. 실제 제출 전에는 문장과 근거를 다시 확인해 주세요.",
                    style = MaterialTheme.typography.labelLarge,
                    color = DawnTextSubtle
                )
            }
        }

        MenuSheetSection(title = "파일") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = proofDocument != null) {
                        if (proofDocument != null) onOpenProofDocument()
                    }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "문서로 이동",
                    style = MaterialTheme.typography.labelLarge,
                    color = DawnPrimary
                )
            }
            proofDocument?.let { document ->
                MenuClaimFileCard(
                    title = "근무 기록 문서",
                    badgeText = document.statusText,
                    badgeTone = document.statusTone,
                    accentBackground = DawnSecondary,
                    accentColor = DawnPrimaryDeep,
                    accentIcon = Icons.Default.Description,
                    onShare = onOpenProofDocument,
                    onOpen = onOpenProofDocument
                )
            }
            claimDocument?.let { document ->
                MenuClaimFileCard(
                    title = document.title,
                    badgeText = document.statusText,
                    badgeTone = document.statusTone,
                    accentBackground = Color(0xFFF1F5F9),
                    accentColor = Color(0xFF475569),
                    accentIcon = Icons.Default.Warning,
                    onShare = onOpenClaimDocument,
                    onOpen = onOpenClaimDocument
                )
            }
        }

        MenuSheetSection(title = "접수 경로 안내") {
            MenuClaimPathCard(
                title = "온라인",
                description = "온라인 접수 전에는 필요한 항목과 제출 순서를 먼저 확인해 두는 편이 안전해요.",
                icon = Icons.Default.Language,
                accentBackground = DawnSecondary,
                accentColor = DawnPrimaryDeep
            )
            MenuClaimPathCard(
                title = "전화",
                description = "상담이나 접수 전에 준비할 문장과 자료를 빠르게 확인할 수 있도록 정리했어요.",
                icon = Icons.Default.Info,
                accentBackground = Color(0xFFF1F5F9),
                accentColor = Color(0xFF475569)
            )
            MenuClaimPathCard(
                title = "방문",
                description = "방문 접수 전에는 챙길 파일과 전달 순서를 체크해 두세요.",
                icon = Icons.Default.AccountBalanceWallet,
                accentBackground = DawnSecondary,
                accentColor = DawnPrimaryDeep
            )
        }

        MenuSheetSection(title = "체크리스트") {
            MenuChecklistRow(text = "근무 기록 문서 준비")
            MenuChecklistRow(text = "근거 자료 묶음 준비")
            MenuChecklistRow(text = "접수 경로 확인(온라인/전화/방문)", isInfo = true)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PrimaryActionButton(
                text = "근거 자료 묶음 열기",
                onClick = onOpenClaimDocument,
                modifier = Modifier.weight(1f)
            )
            SecondaryActionButton(
                text = "차액 화면",
                onClick = onOpenWage,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "이 안내는 참고용이며 법률 자문이 아닙니다. 필요하면 전문기관 상담도 함께 확인해 주세요.",
            style = MaterialTheme.typography.labelLarge,
            color = DawnTextSubtle
        )

    }
}

@Composable
private fun MenuClaimFileCard(
    title: String,
    badgeText: String,
    badgeTone: BadgeTone,
    accentBackground: Color,
    accentColor: Color,
    accentIcon: ImageVector,
    onShare: () -> Unit,
    onOpen: () -> Unit
) {
    val enabled = badgeTone == BadgeTone.Success

    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(accentBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = accentIcon,
                            contentDescription = null,
                            tint = accentColor
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = title, style = MaterialTheme.typography.bodyLarge)
                        StatusBadge(text = badgeText, tone = badgeTone)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MenuDocumentActionButton(
                    text = "공유",
                    icon = Icons.Default.Share,
                    onClick = onShare,
                    enabled = enabled,
                    primary = false,
                    modifier = Modifier.weight(1f)
                )
                MenuDocumentActionButton(
                    text = "열기",
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    onClick = onOpen,
                    enabled = enabled,
                    primary = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        HorizontalDivider(color = MenuDivider)
    }
}

@Composable
private fun MenuClaimPathCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentBackground: Color,
    accentColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DawnTextSubtle
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = DawnTextSubtle,
                modifier = Modifier.size(18.dp)
            )
        }
        HorizontalDivider(color = MenuDivider)
    }
}

@Composable
private fun MenuSettingsSheet(
    selectedLanguage: String,
    onSelectLanguage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        MenuSheetHeader(
            title = "설정",
            onDismiss = onDismiss
        )

        MenuSheetSection(title = "언어") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                MenuLanguageChip(
                    label = "한국어",
                    code = "ko",
                    selected = selectedLanguage == "ko",
                    onClick = { onSelectLanguage("ko") },
                    showDivider = true
                )
                MenuLanguageChip(
                    label = "English",
                    code = "en",
                    selected = selectedLanguage == "en",
                    onClick = { onSelectLanguage("en") },
                    showDivider = false
                )
            }
        }
    }
}

@Composable
private fun MenuChecklistRow(
    text: String,
    isInfo: Boolean = false
) {
    val tint = if (isInfo) DawnPrimaryDeep else DawnSuccess
    val icon = if (isInfo) Icons.Default.Info else Icons.Default.CheckCircle

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = DawnText
            )
        }
        HorizontalDivider(color = MenuDivider)
    }
}

@Composable
private fun MenuSheetChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) DawnSecondary else DawnSurfaceAlt,
        border = BorderStroke(1.dp, if (selected) DawnPrimary else DawnBorder)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) DawnPrimaryDeep else DawnTextSubtle
        )
    }
}

@Composable
private fun MenuLanguageChip(
    label: String,
    code: String,
    selected: Boolean,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .pressableScale(
                    interactionSource = interactionSource,
                    pressedScale = 0.99f
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = rememberDonDoneGrayRipple(bounded = true),
                    onClick = onClick
                )
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DawnText
                )
                Text(
                    text = code.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = DawnTextSubtle
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = DawnPrimary
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(color = MenuDivider)
        }
    }
}
