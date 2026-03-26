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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
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
import com.dondone.mobile.core.designsystem.DawnWarning
import com.dondone.mobile.core.designsystem.DonDoneNoticeBanner
import com.dondone.mobile.core.designsystem.PrimaryActionButton
import com.dondone.mobile.core.designsystem.SecondaryActionButton
import com.dondone.mobile.core.designsystem.StatusBadge
import com.dondone.mobile.core.designsystem.pressableScale
import com.dondone.mobile.core.designsystem.rememberDonDoneGrayRipple
import com.dondone.mobile.core.i18n.AppTextKeys
import com.dondone.mobile.core.i18n.AppLanguage
import com.dondone.mobile.core.i18n.LocalAppLanguage
import com.dondone.mobile.core.i18n.text
import com.dondone.mobile.core.i18n.translate
import com.dondone.mobile.feature.workproof.presentation.WorkproofPdfFileAction
import com.dondone.mobile.feature.workproof.presentation.WorkproofPdfFileUiState

private data class MenuServiceAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

private data class MenuProfileInfoItem(
    val label: String,
    val value: String
)

private enum class MenuOverlaySheet {
    Claim,
    Profile,
    Settings
}

private enum class ClaimSummaryTone {
    Default,
    Firm,
    Short
}

private val MenuCanvas = Color.White
private val MenuDivider = Color(0xFFE8EBF0)
private const val MENU_PROFILE_EMPTY_VALUE = "없음"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    uiModel: MenuUiModel,
    workproofPdfFileUiState: WorkproofPdfFileUiState,
    launchRequest: MenuLaunchRequest?,
    profileUpdateUiState: ProfileUpdateUiState,
    selectedLanguage: AppLanguage,
    onOpenWage: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenWorkproofPdfCreation: () -> Unit,
    onOpenWorkproofPdf: (Long) -> Unit,
    onShareWorkproofPdf: (Long) -> Unit,
    onClearPdfFileState: () -> Unit,
    onConsumeLaunchRequest: () -> Unit,
    onUpdateProfile: (String, String) -> Unit,
    onClearProfileUpdateMessage: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onOpenWorkerRegistrationCode: () -> Unit,
    onLogout: () -> Unit,
    onShowToast: (String, BadgeTone) -> Unit
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    var activeSheet by rememberSaveable { mutableStateOf<MenuOverlaySheet?>(null) }
    var isLogoutConfirmVisible by rememberSaveable { mutableStateOf(false) }
    var selectedDocumentId by remember { mutableStateOf<String?>(null) }
    var awaitingProfileUpdateResult by remember { mutableStateOf(false) }

    val selectedDocument = uiModel.documents.firstOrNull { it.id == selectedDocumentId }
    val proofDocument = uiModel.documents.firstOrNull { it.accent == MenuDocumentAccent.Proof }
    val claimDocument = uiModel.documents.firstOrNull { it.accent == MenuDocumentAccent.Claim }
    fun dismissProfileSheetIfIdle() {
        if (!profileUpdateUiState.isSubmitting) {
            awaitingProfileUpdateResult = false
            onClearProfileUpdateMessage()
            activeSheet = null
        }
    }

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
        add(MenuServiceAction(language.text(AppTextKeys.WAGE_REVIEW), Icons.Default.AttachMoney, onOpenWage))
        add(MenuServiceAction(language.text("wallet_management"), Icons.Default.AccountBalanceWallet, onOpenAccount))
        add(MenuServiceAction(language.text("generate_documents"), Icons.Default.Description, onOpenWorkproofPdfCreation))
        add(MenuServiceAction(language.text(AppTextKeys.SETTINGS), Icons.Default.Settings) { activeSheet = MenuOverlaySheet.Settings })
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
                }
            )
            MenuSectionDivider()
            MenuAccountLinkSection(
                onOpenWorkerRegistrationCode = onOpenWorkerRegistrationCode
            )
            MenuSectionDivider()
        }
        uiModel.fallbackNoticeMessage?.let { message ->
            DonDoneNoticeBanner(
                title = uiModel.fallbackNoticeTitle ?: language.text("demo_sample_data"),
                message = message
            )
            MenuSectionDivider()
        }
        MenuServicesSection(actions = serviceActions)
        if (uiModel.session != null) {
            MenuSectionDivider()
            MenuLogoutSection(
                onLogoutRequest = { isLogoutConfirmVisible = true }
            )
        }
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
                onSelectLanguage = onSelectLanguage,
                onDismiss = { activeSheet = null }
            )
        }
    }

    if (activeSheet == MenuOverlaySheet.Profile && uiModel.session != null) {
        ModalBottomSheet(
            onDismissRequest = ::dismissProfileSheetIfIdle,
            containerColor = DawnSurface
        ) {
            MenuProfileSheet(
                session = uiModel.session,
                uiState = profileUpdateUiState,
                onSubmit = { name, phoneNumber ->
                    awaitingProfileUpdateResult = true
                    onUpdateProfile(name, phoneNumber)
                },
                onDismiss = ::dismissProfileSheetIfIdle
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
                        onShowToast(language.text("menu_open_ready_document_missing"), BadgeTone.Warning)
                    } else {
                        selectedDocumentId = null
                        onOpenWorkproofPdf(documentId)
                    }
                },
                onShareProofDocument = {
                    val documentId = selectedDocument.documentId
                    if (documentId == null) {
                        onShowToast(language.text("menu_share_ready_document_missing"), BadgeTone.Warning)
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

    if (isLogoutConfirmVisible) {
        ModalBottomSheet(
            onDismissRequest = { isLogoutConfirmVisible = false },
            containerColor = DawnSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = language.text("log_out"),
                    style = MaterialTheme.typography.titleLarge,
                    color = DawnText
                )
                Text(
                    text = language.text("menu_logout_confirm_message"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DawnTextSubtle
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SecondaryActionButton(
                        text = language.text("cancel"),
                        onClick = { isLogoutConfirmVisible = false },
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryActionButton(
                        text = language.text("log_out"),
                        onClick = {
                            isLogoutConfirmVisible = false
                            onLogout()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
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
                    putExtra(Intent.EXTRA_TITLE, fileName ?: AppLanguage.fromDefault().text("workproof_work_record_document"))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                AppLanguage.fromDefault().text("workproof_share_work_record_document")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        )
    }.isSuccess
}

@Composable
private fun MenuSessionSection(
    session: MenuSessionUiModel,
    onEditProfile: () -> Unit
) {
    val language = LocalAppLanguage.current
    val organizationSummary = session.companyName.toProfileValue(language)

    MenuSectionSurface {
        MenuSectionHeader(title = language.text("account"))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = session.name,
                style = MaterialTheme.typography.titleMedium,
                color = DawnText
            )
            Text(
                text = organizationSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = DawnTextSubtle
            )
        }
        SecondaryActionButton(
            text = language.text("edit_profile"),
            onClick = onEditProfile,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MenuLogoutSection(
    onLogoutRequest: () -> Unit
) {
    val language = LocalAppLanguage.current
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = rememberDonDoneGrayRipple(),
                onClick = onLogoutRequest
            )
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = language.text("log_out"),
            style = MaterialTheme.typography.labelLarge,
            color = DawnWarning
        )
    }
}

@Composable
private fun MenuAccountLinkSection(
    onOpenWorkerRegistrationCode: () -> Unit
) {
    val language = LocalAppLanguage.current
    MenuSectionSurface {
        MenuSectionHeader(title = language.text("account_link"))
        MenuSectionListRow(
            showDivider = false,
            onClick = onOpenWorkerRegistrationCode
        ) {
            MenuServiceLeadingIcon(icon = Icons.Default.CheckCircle)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = language.text(AppTextKeys.ENTER_WORKER_REGISTRATION_CODE),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DawnText
                )
                Text(
                    text = language.text("you_can_register_your_workplace_link_code"),
                    style = MaterialTheme.typography.labelLarge,
                    color = DawnTextSubtle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = DawnTextSubtle,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun MenuProfileSheet(
    session: MenuSessionUiModel,
    uiState: ProfileUpdateUiState,
    onSubmit: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val language = LocalAppLanguage.current
    var name by remember(session.name) { mutableStateOf(session.name) }
    var phoneNumber by remember(session.phoneNumber) { mutableStateOf(session.phoneNumber.orEmpty()) }
    val readOnlyProfileInfoItems = listOf(
        MenuProfileInfoItem(
            label = language.text("email"),
            value = session.email.toProfileValue(language)
        ),
        MenuProfileInfoItem(
            label = language.text("company"),
            value = session.companyName.toProfileValue(language)
        ),
        MenuProfileInfoItem(
            label = language.text("workplace"),
            value = session.workplaceName.toProfileValue(language)
        )
    )
    val canSubmit = !uiState.isSubmitting && name.trim().isNotBlank() && phoneNumber.filter(Char::isDigit).length in 10..11

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MenuSheetHeader(
            title = language.text("edit_profile"),
            subtitle = language.text("menu_profile_subtitle"),
            onDismiss = onDismiss
        )
        MenuSheetSection(title = language.text("editable_information")) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(language.text("name")) },
                shape = RoundedCornerShape(16.dp)
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                singleLine = true,
                label = { Text(language.text("phone_number")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(16.dp)
            )
        }
        MenuSheetSection(title = language.text("read_only_information")) {
            Column(modifier = Modifier.fillMaxWidth()) {
                readOnlyProfileInfoItems.forEachIndexed { index, item ->
                    MenuProfileInfoRow(
                        item = item,
                        showDivider = index != readOnlyProfileInfoItems.lastIndex
                    )
                }
            }
        }
        Text(
            text = language.text("menu_readonly_notice"),
            style = MaterialTheme.typography.bodySmall,
            color = DawnTextSubtle
        )
        if (uiState.message != null) {
            Text(
                text = uiState.message,
                style = MaterialTheme.typography.bodySmall,
                color = if (uiState.isError) Color(0xFFC93C37) else DawnSuccess
            )
        }
        PrimaryActionButton(
            text = if (uiState.isSubmitting) language.text("saving") else language.text("save"),
            onClick = { onSubmit(name, phoneNumber) },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MenuProfileInfoRow(
    item: MenuProfileInfoItem,
    showDivider: Boolean
) {
    val language = LocalAppLanguage.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyMedium,
            color = DawnTextSubtle
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = item.value,
            style = MaterialTheme.typography.bodyLarge,
            color = if (item.value == language.text("none")) DawnTextSubtle else DawnText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    if (showDivider) {
        HorizontalDivider(color = MenuDivider)
    }
}

private fun String?.toProfileValue(language: AppLanguage = AppLanguage.fromDefault()): String {
    val value = this?.trim()
    return if (value.isNullOrBlank()) language.text("none") else value
}

private fun MenuSessionUiModel.toOrganizationSummary(language: AppLanguage = AppLanguage.fromDefault()): String {
    val company = companyName?.trim().orEmpty()
    val workplace = workplaceName?.trim().orEmpty()

    if (company.isBlank()) {
        return language.text("menu_no_affiliation_info")
    }
    return "$company · $workplace"
}

@Composable
private fun MenuServicesSection(
    actions: List<MenuServiceAction>
) {
    val language = LocalAppLanguage.current
    MenuSectionSurface {
        MenuSectionHeader(title = language.text("services"))

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
    val language = LocalAppLanguage.current
    MenuSectionListRow(
        showDivider = showDivider,
        onClick = action.onClick
    ) {
        MenuServiceLeadingIcon(icon = action.icon)
        Text(
            text = language.translate(action.label),
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
private fun MenuDocumentActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    primary: Boolean,
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
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
                text = language.translate(text),
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1
            )
        }
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
    val language = LocalAppLanguage.current
    Text(
        text = language.translate(title),
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
    val language = LocalAppLanguage.current
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
                text = language.translate(title),
                style = MaterialTheme.typography.titleLarge,
                color = DawnText
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = language.translate(subtitle),
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
                text = language.translate("닫기"),
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
    val language = LocalAppLanguage.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        title?.let {
            Text(
                text = language.translate(it),
                style = MaterialTheme.typography.bodyLarge,
                color = DawnText
            )
        }
        content()
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
    val language = LocalAppLanguage.current
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
                Text(text = language.text("current_status"), style = MaterialTheme.typography.bodyLarge)
                StatusBadge(text = document.statusText, tone = document.statusTone)
            }
            Text(
                text = document.updatedAtText,
                style = MaterialTheme.typography.bodyMedium,
                color = DawnTextSubtle
            )
            Text(
                text = when (document.accent) {
                    MenuDocumentAccent.Proof -> language.text("menu_document_proof_detail")
                    MenuDocumentAccent.Claim -> language.text("menu_document_claim_detail")
                    MenuDocumentAccent.Receipt -> language.text("menu_document_receipt_detail")
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
                    text = language.text("share"),
                    icon = Icons.Default.Share,
                    onClick = onShareProofDocument,
                    enabled = document.statusTone == BadgeTone.Success,
                    primary = false,
                    modifier = Modifier.weight(1f)
                )
                MenuDocumentActionButton(
                    text = language.text("open"),
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    onClick = onOpenProofDocument,
                    enabled = document.statusTone == BadgeTone.Success,
                    primary = true,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            PrimaryActionButton(
                text = if (document.accent == MenuDocumentAccent.Claim) language.text("claim_prep") else language.text("view"),
                onClick = if (document.accent == MenuDocumentAccent.Claim) onOpenClaimFlow else onDismiss,
                modifier = Modifier.fillMaxWidth()
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
    val language = LocalAppLanguage.current
    var selectedTone by rememberSaveable { mutableStateOf(ClaimSummaryTone.Default) }
    val summaryText = when (selectedTone) {
        ClaimSummaryTone.Firm -> language.text("menu_claim_summary_firm")
        ClaimSummaryTone.Short -> language.text("menu_claim_summary_short")
        ClaimSummaryTone.Default -> language.text("menu_claim_summary_default")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        MenuSheetHeader(
            title = language.text("claim_prep"),
            subtitle = language.text("menu_claim_sheet_subtitle"),
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
                    Text(text = language.text("menu_claim_summary_section"), style = MaterialTheme.typography.bodyLarge)
                    SecondaryActionButton(
                        text = language.text("generate_copy"),
                        onClick = { selectedTone = ClaimSummaryTone.Default }
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MenuSheetChip(language.text("default"), selectedTone == ClaimSummaryTone.Default) {
                        selectedTone = ClaimSummaryTone.Default
                    }
                    MenuSheetChip(language.text("formal"), selectedTone == ClaimSummaryTone.Firm) {
                        selectedTone = ClaimSummaryTone.Firm
                    }
                    MenuSheetChip(language.text("short"), selectedTone == ClaimSummaryTone.Short) {
                        selectedTone = ClaimSummaryTone.Short
                    }
                }
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DawnText
                )
                SecondaryActionButton(
                    text = language.text("copy"),
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = language.text("menu_claim_demo_notice"),
                    style = MaterialTheme.typography.labelLarge,
                    color = DawnTextSubtle
                )
            }
        }

        MenuSheetSection(title = language.text("files")) {
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
                    text = language.text("open_document"),
                    style = MaterialTheme.typography.labelLarge,
                    color = DawnPrimary
                )
            }
            proofDocument?.let { document ->
                MenuClaimFileCard(
                    title = language.text("work_record_document"),
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

        MenuSheetSection(title = language.text("submission_paths")) {
            MenuClaimPathCard(
                title = language.text("online"),
                description = language.text("menu_online_submission_desc"),
                icon = Icons.Default.Language,
                accentBackground = DawnSecondary,
                accentColor = DawnPrimaryDeep
            )
            MenuClaimPathCard(
                title = language.text("phone"),
                description = language.text("menu_phone_submission_desc"),
                icon = Icons.Default.Info,
                accentBackground = Color(0xFFF1F5F9),
                accentColor = Color(0xFF475569)
            )
            MenuClaimPathCard(
                title = language.text("visit"),
                description = language.text("menu_visit_submission_desc"),
                icon = Icons.Default.AccountBalanceWallet,
                accentBackground = DawnSecondary,
                accentColor = DawnPrimaryDeep
            )
        }

        MenuSheetSection(title = language.text("checklist")) {
            MenuChecklistRow(text = language.text("prepare_work_record_document"))
            MenuChecklistRow(text = language.text("prepare_evidence_bundle"))
            MenuChecklistRow(text = language.text("check_submission_path_online_phone_visit"), isInfo = true)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PrimaryActionButton(
                text = language.text("open_evidence_bundle"),
                onClick = onOpenClaimDocument,
                modifier = Modifier.weight(1f)
            )
            SecondaryActionButton(
                text = language.text("difference_screen"),
                onClick = onOpenWage,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = language.text("menu_claim_legal_notice"),
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
    val language = LocalAppLanguage.current
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
                        Text(text = language.translate(title), style = MaterialTheme.typography.bodyLarge)
                        StatusBadge(text = language.translate(badgeText), tone = badgeTone)
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
    val language = LocalAppLanguage.current
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
                Text(text = language.translate(title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = language.translate(description),
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
    selectedLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        val language = LocalAppLanguage.current
        MenuSheetHeader(
            title = language.text(AppTextKeys.SETTINGS),
            onDismiss = onDismiss
        )

        MenuSheetSection(title = language.text("language")) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                MenuLanguageChip(
                    label = AppLanguage.KOREAN.nativeLabel,
                    code = AppLanguage.KOREAN.code,
                    selected = selectedLanguage == AppLanguage.KOREAN,
                    onClick = { onSelectLanguage(AppLanguage.KOREAN) },
                    showDivider = true
                )
                MenuLanguageChip(
                    label = AppLanguage.ENGLISH.nativeLabel,
                    code = AppLanguage.ENGLISH.code,
                    selected = selectedLanguage == AppLanguage.ENGLISH,
                    onClick = { onSelectLanguage(AppLanguage.ENGLISH) },
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
    val language = LocalAppLanguage.current
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
                text = language.translate(text),
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
    val language = LocalAppLanguage.current
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) DawnSecondary else DawnSurfaceAlt,
        border = BorderStroke(1.dp, if (selected) DawnPrimary else DawnBorder)
    ) {
        Text(
            text = language.translate(text),
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
    val language = LocalAppLanguage.current
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
                    text = language.translate(label),
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
