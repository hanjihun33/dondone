package com.dondone.mobile.feature.menu.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.dondone.mobile.core.designsystem.PrimaryActionButton
import com.dondone.mobile.core.designsystem.SecondaryActionButton
import com.dondone.mobile.core.designsystem.SectionPanel
import com.dondone.mobile.core.designsystem.StatusBadge

private data class MenuServiceAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

private data class MenuDocumentColors(
    val cardBackground: Color,
    val cardBorder: Color,
    val iconBackground: Color,
    val iconColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    uiModel: MenuUiModel,
    onShiftAsOf: (Int) -> Unit,
    onResetSeed: () -> Unit,
    onOpenWage: () -> Unit,
    onOpenAccount: () -> Unit
) {
    var showClaimSheet by rememberSaveable { mutableStateOf(false) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var selectedLanguage by rememberSaveable { mutableStateOf("ko") }
    var selectedDocumentId by remember { mutableStateOf<String?>(null) }

    val selectedDocument = uiModel.documents.firstOrNull { it.id == selectedDocumentId }
    val proofDocument = uiModel.documents.firstOrNull { it.accent == MenuDocumentAccent.Proof }
    val claimDocument = uiModel.documents.firstOrNull { it.accent == MenuDocumentAccent.Claim }

    val serviceActions = listOf(
        MenuServiceAction("급여 점검", Icons.Default.Description, onOpenWage),
        MenuServiceAction("신고 준비", Icons.Default.Warning) { showClaimSheet = true },
        MenuServiceAction("계좌 지갑 관리", Icons.Default.AccountBalanceWallet, onOpenAccount),
        MenuServiceAction("설정", Icons.Default.Settings) { showSettingsSheet = true }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MenuServicesCard(actions = serviceActions)

        Text(
            text = "문서",
            style = MaterialTheme.typography.titleLarge
        )

        uiModel.documents.forEach { document ->
            MenuDocumentCard(
                document = document,
                onOpenDetail = {
                    if (document.accent == MenuDocumentAccent.Claim && document.statusTone != BadgeTone.Success) {
                        showClaimSheet = true
                    } else {
                        selectedDocumentId = document.id
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    if (showClaimSheet) {
        ModalBottomSheet(
            onDismissRequest = { showClaimSheet = false },
            containerColor = DawnSurface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            MenuClaimSheet(
                proofDocument = proofDocument,
                claimDocument = claimDocument,
                onOpenProofDocument = {
                    showClaimSheet = false
                    selectedDocumentId = proofDocument?.id
                },
                onOpenClaimDocument = {
                    showClaimSheet = false
                    selectedDocumentId = claimDocument?.id
                },
                onOpenWage = {
                    showClaimSheet = false
                    onOpenWage()
                },
                onDismiss = { showClaimSheet = false }
            )
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = DawnSurface
        ) {
            MenuSettingsSheet(
                currentDateText = uiModel.currentDateText,
                selectedLanguage = selectedLanguage,
                onSelectLanguage = { selectedLanguage = it },
                onShiftAsOf = onShiftAsOf,
                onResetSeed = onResetSeed,
                onDismiss = { showSettingsSheet = false }
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
                onOpenClaimFlow = {
                    selectedDocumentId = null
                    showClaimSheet = true
                },
                onDismiss = { selectedDocumentId = null }
            )
        }
    }
}

@Composable
private fun MenuServicesCard(
    actions: List<MenuServiceAction>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = DawnSurface),
        border = BorderStroke(1.dp, DawnBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    Text(text = "서비스", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "급여 점검, 신고 준비, 계좌 관리를 여기에서 이어서 확인할 수 있어요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DawnTextSubtle
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(DawnSecondary)
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = null,
                        tint = DawnPrimary
                    )
                }
            }

            actions.chunked(2).forEach { rowActions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowActions.forEach { action ->
                        MenuGridActionButton(
                            modifier = Modifier.weight(1f),
                            label = action.label,
                            icon = action.icon,
                            onClick = action.onClick
                        )
                    }
                    if (rowActions.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuGridActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = DawnSurface,
        border = BorderStroke(1.dp, DawnBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
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
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MenuDocumentCard(
    document: MenuDocumentUiModel,
    onOpenDetail: () -> Unit
) {
    val colors = menuDocumentColors(document.accent)
    val isReady = document.statusTone == BadgeTone.Success

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = colors.cardBackground,
        border = BorderStroke(1.dp, colors.cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            MenuDocumentAccentBox(document.accent, colors)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = document.updatedAtText,
                            style = MaterialTheme.typography.labelLarge,
                            color = DawnTextSubtle
                        )
                    }
                    StatusBadge(
                        text = document.statusText,
                        tone = document.statusTone
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MenuDocumentActionButton(
                        text = "공유",
                        icon = Icons.Default.Share,
                        onClick = onOpenDetail,
                        enabled = isReady,
                        primary = false,
                        modifier = Modifier.weight(1f)
                    )
                    MenuDocumentActionButton(
                        text = "다운로드",
                        icon = Icons.Default.Download,
                        onClick = onOpenDetail,
                        enabled = isReady,
                        primary = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
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
            .clickable(enabled = enabled, onClick = onClick),
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
            .size(44.dp)
            .clip(RoundedCornerShape(18.dp))
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

private fun menuDocumentColors(accent: MenuDocumentAccent): MenuDocumentColors {
    return when (accent) {
        MenuDocumentAccent.Proof -> MenuDocumentColors(
            cardBackground = DawnSurface,
            cardBorder = DawnBorder,
            iconBackground = DawnSecondary,
            iconColor = DawnPrimaryDeep
        )
        MenuDocumentAccent.Claim -> MenuDocumentColors(
            cardBackground = DawnSurface,
            cardBorder = DawnBorder,
            iconBackground = Color(0xFFF1F5F9),
            iconColor = Color(0xFF475569)
        )
        MenuDocumentAccent.Receipt -> MenuDocumentColors(
            cardBackground = DawnSurface,
            cardBorder = DawnBorder,
            iconBackground = Color(0xFFE8F2FF),
            iconColor = DawnPrimaryDeep
        )
    }
}

@Composable
private fun MenuDocumentSheet(
    document: MenuDocumentUiModel,
    onOpenClaimFlow: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "DOCUMENT",
                style = MaterialTheme.typography.labelMedium,
                color = DawnPrimary
            )
            Text(
                text = document.title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = document.summaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = DawnTextSubtle
            )
        }

        SectionPanel {
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
                    MenuDocumentAccent.Proof -> "근무 기록과 차액 검토 근거가 반영된 최신 증빙 문서예요."
                    MenuDocumentAccent.Claim -> "차액 검토 결과를 토대로 신고 준비 흐름으로 이어지는 묶음 문서예요."
                    MenuDocumentAccent.Receipt -> "최근 송금 결과와 해시를 다시 확인할 수 있는 영수증 문서예요."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = DawnText
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SecondaryActionButton(
                text = "닫기",
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            PrimaryActionButton(
                text = if (document.accent == MenuDocumentAccent.Claim) "신고 준비" else "확인",
                onClick = if (document.accent == MenuDocumentAccent.Claim) onOpenClaimFlow else onDismiss,
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
    var selectedTone by rememberSaveable { mutableStateOf("default") }
    val summaryText = when (selectedTone) {
        "firm" -> "근무 기록과 차액 근거를 바탕으로 접수 사실을 정중하게 정리한 초안입니다. 제출 전에 문장을 한 번 더 다듬어 주세요."
        "short" -> "차액과 근거를 짧게 정리해 신고 준비를 빠르게 시작할 수 있도록 요약한 문장입니다."
        else -> "자동 제출이 아니라 제출 전에 문장과 자료를 빠르게 정리해 보는 데모 단계예요."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "INSTANT CLAIM v0",
                style = MaterialTheme.typography.labelMedium,
                color = DawnPrimary
            )
            Text(text = "신고 준비", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "mockup 기준 신고 준비 오버레이를 Android 바텀시트로 옮긴 데모 흐름입니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = DawnTextSubtle
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
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
                        onClick = { selectedTone = "default" }
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MenuSheetChip("기본", selectedTone == "default") { selectedTone = "default" }
                    MenuSheetChip("정중하게", selectedTone == "firm") { selectedTone = "firm" }
                    MenuSheetChip("짧게", selectedTone == "short") { selectedTone = "short" }
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

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "파일", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "문서로 이동",
                    modifier = Modifier.clickable {
                        if (proofDocument != null) onOpenProofDocument()
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = DawnPrimary
                )
            }
            proofDocument?.let { document ->
                MenuClaimFileCard(
                    title = "Proof Pack",
                    badgeText = document.statusText,
                    badgeTone = document.statusTone,
                    accentBackground = DawnSecondary,
                    accentColor = DawnPrimaryDeep,
                    accentIcon = Icons.Default.Description,
                    actionLabel = if (document.statusTone == BadgeTone.Success) "열기" else "준비",
                    onAction = onOpenProofDocument,
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
                    actionLabel = if (document.statusTone == BadgeTone.Success) "열기" else "준비",
                    onAction = onOpenClaimDocument,
                    onShare = onOpenClaimDocument,
                    onOpen = onOpenClaimDocument
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "접수 경로 안내", style = MaterialTheme.typography.bodyLarge)
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

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "체크리스트", style = MaterialTheme.typography.bodyLarge)
            MenuChecklistRow(text = "Proof Pack 준비")
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

        SecondaryActionButton(
            text = "닫기",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
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
    actionLabel: String,
    onAction: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit
) {
    val enabled = badgeTone == BadgeTone.Success

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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
                SecondaryActionButton(text = actionLabel, onClick = onAction)
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
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = onOpen,
                    enabled = enabled,
                    primary = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
            SecondaryActionButton(text = "바로가기", onClick = {})
        }
    }
}

@Composable
private fun MenuSettingsSheet(
    currentDateText: String,
    selectedLanguage: String,
    onSelectLanguage: (String) -> Unit,
    onShiftAsOf: (Int) -> Unit,
    onResetSeed: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "SETTINGS",
                style = MaterialTheme.typography.labelMedium,
                color = DawnPrimary
            )
            Text(text = "설정", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "데모 기준일과 언어를 빠르게 바꾸고 현재 세션 상태를 다시 불러올 수 있어요.",
                style = MaterialTheme.typography.bodyMedium,
                color = DawnTextSubtle
            )
        }

        SectionPanel {
            Text(text = "언어", style = MaterialTheme.typography.bodyLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MenuLanguageChip(
                    label = "한국어",
                    code = "ko",
                    selected = selectedLanguage == "ko",
                    onClick = { onSelectLanguage("ko") }
                )
                MenuLanguageChip(
                    label = "English",
                    code = "en",
                    selected = selectedLanguage == "en",
                    onClick = { onSelectLanguage("en") }
                )
            }
        }

        SectionPanel {
            Text(text = "기준일 이동", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = currentDateText,
                style = MaterialTheme.typography.bodyMedium,
                color = DawnTextSubtle
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SecondaryActionButton(
                    text = "-1일",
                    onClick = { onShiftAsOf(-1) },
                    modifier = Modifier.weight(1f)
                )
                SecondaryActionButton(
                    text = "+1일",
                    onClick = { onShiftAsOf(1) },
                    modifier = Modifier.weight(1f)
                )
                SecondaryActionButton(
                    text = "+7일",
                    onClick = { onShiftAsOf(7) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        SectionPanel {
            Text(text = "데모 상태", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "샘플 데이터를 처음 상태로 다시 불러옵니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = DawnTextSubtle
            )
            SecondaryActionButton(
                text = "데모 초기화",
                onClick = onResetSeed,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SecondaryActionButton(
            text = "닫기",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MenuChecklistRow(
    text: String,
    isInfo: Boolean = false
) {
    val tint = if (isInfo) DawnPrimaryDeep else DawnSuccess
    val background = if (isInfo) DawnSecondary else Color(0xFFEFFAF3)
    val icon = if (isInfo) Icons.Default.Info else Icons.Default.CheckCircle

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(background),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint
                )
            }
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = DawnText
            )
        }
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
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) DawnSecondary else DawnSurfaceAlt,
        border = BorderStroke(1.dp, if (selected) DawnPrimary else DawnBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = code.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) DawnPrimaryDeep else DawnTextSubtle
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = DawnText
            )
        }
    }
}
