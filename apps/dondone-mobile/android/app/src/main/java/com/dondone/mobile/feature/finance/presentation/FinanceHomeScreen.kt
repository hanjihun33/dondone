package com.dondone.mobile.feature.finance.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.designsystem.DawnBorder
import com.dondone.mobile.core.designsystem.DawnPrimary
import com.dondone.mobile.core.designsystem.DawnText
import com.dondone.mobile.core.designsystem.DawnTextSubtle
import com.dondone.mobile.core.designsystem.DonDoneNoticeBanner
import com.dondone.mobile.core.designsystem.DonDoneProgressBar
import com.dondone.mobile.core.designsystem.pressableScale
import com.dondone.mobile.core.designsystem.rememberDonDoneGrayRipple
import com.dondone.mobile.domain.advance.AdvanceSurfaceState
import com.dondone.mobile.data.vault.VaultActionType

private val FinanceCanvas = Color.White
private val FinanceSurfaceMuted = Color(0xFFF5F6FA)
private val FinanceDivider = Color(0xFFE8EBF0)
private val FinanceTextPrimary = Color(0xFF1F2430)
private val FinanceTextMuted = Color(0xFF8B95A1)
private val FinanceAccent = DawnPrimary
private val FinanceAdvanceSheetHero = Color(0xFFF5F0FF)
private val FinanceAdvanceSheetHeroBorder = Color(0xFFE9DFFF)
private val FinanceAdvanceSheetMutedBackground = Color(0xFFF8FAFC)
private val FinanceAdvanceSheetMutedBorder = Color(0xFFE2E8F0)
private val FinanceAdvanceSheetWarningBorder = Color(0xFFFDE68A)
private val FinanceAdvanceSheetWarningBackground = Color(0xFFFFFBEB)
private val FinanceAdvanceSheetDefaultText = Color(0xFF94A3B8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceHomeScreen(
    uiModel: FinanceHomeUiModel,
    onSelectAdvanceAmount: (Int) -> Unit,
    onRequestAdvance: () -> Unit,
    onClearAdvanceMessage: () -> Unit,
    onSelectVaultAction: (VaultActionType) -> Unit,
    onSelectVaultAmount: (Int) -> Unit,
    onSubmitVaultAction: () -> Unit,
    onClearVaultMessage: () -> Unit,
    onOpenAdvanceRequestDetail: (Long) -> Unit,
    onCloseAdvanceRequestDetail: () -> Unit,
    onOpenWorkproof: () -> Unit
) {
    val advanceSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val advanceRequestDetailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val vaultSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAdvanceSheet by remember { mutableStateOf(false) }
    var showAdvanceRequestDetailSheet by remember { mutableStateOf(false) }
    var showVaultSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FinanceCanvas)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            FinanceAdvanceSection(
                uiModel = uiModel.advance,
                onOpenSheet = { showAdvanceSheet = true },
                onOpenWorkproof = onOpenWorkproof
            )
            FinanceSectionDivider()
            FinanceVaultSection(
                uiModel = uiModel.vault,
                onOpenSheet = { showVaultSheet = true }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showAdvanceSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                onClearAdvanceMessage()
                showAdvanceSheet = false
            },
            sheetState = advanceSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = null
        ) {
            FinanceAdvanceBottomSheet(
                uiModel = uiModel.advance.detail,
                onDismiss = {
                    onClearAdvanceMessage()
                    showAdvanceSheet = false
                },
                onSelectAmount = onSelectAdvanceAmount,
                onRequestAdvance = onRequestAdvance,
                onClearRequestMessage = onClearAdvanceMessage,
                onOpenWorkproof = {
                    onClearAdvanceMessage()
                    showAdvanceSheet = false
                    onOpenWorkproof()
                },
                onOpenHistoryDetail = { requestId ->
                    onOpenAdvanceRequestDetail(requestId)
                    showAdvanceRequestDetailSheet = true
                }
            )
        }
    }

    if (showAdvanceRequestDetailSheet && uiModel.advance.requestDetail.isVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                onCloseAdvanceRequestDetail()
                showAdvanceRequestDetailSheet = false
            },
            sheetState = advanceRequestDetailSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = null
        ) {
            FinanceAdvanceRequestDetailBottomSheet(
                uiModel = uiModel.advance.requestDetail,
                onDismiss = {
                    onCloseAdvanceRequestDetail()
                    showAdvanceRequestDetailSheet = false
                }
            )
        }
    }

    if (showVaultSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                onClearVaultMessage()
                showVaultSheet = false
            },
            sheetState = vaultSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = null
        ) {
            FinanceVaultBottomSheet(
                uiModel = uiModel.vault.detail,
                onDismiss = {
                    onClearVaultMessage()
                    showVaultSheet = false
                },
                onSelectAction = onSelectVaultAction,
                onSelectAmount = onSelectVaultAmount,
                onAction = onSubmitVaultAction
            )
        }
    }
}

@Composable
private fun FinanceAdvanceSection(
    uiModel: FinanceAdvanceUiModel,
    onOpenSheet: () -> Unit,
    onOpenWorkproof: () -> Unit
) {
    FinanceBlockSection(
        title = "미리받기",
        description = "",
        trailing = {
            if (uiModel.statusText != "-") {
                FinanceCapsule(text = "상환 ${uiModel.statusText}")
            } else {
                Spacer(modifier = Modifier.height(1.dp))
            }
        }
    ) {
        Text(
            text = uiModel.sourceLabelText,
            style = MaterialTheme.typography.labelSmall,
            color = FinanceTextMuted
        )
        FinanceAdvanceHeroCard(
            surfaceState = uiModel.surfaceState,
            title = uiModel.stateTitleText,
            body = uiModel.stateBodyText,
            availableText = uiModel.availableText,
            repaymentDueText = uiModel.repaymentDueText
        )
        if (uiModel.noticeTitleText != null && uiModel.noticeBodyText != null) {
            FinanceAdvanceNoticePanel(
                title = uiModel.noticeTitleText,
                body = uiModel.noticeBodyText
            )
        }
        FinanceInnerPanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "다음 한도 구간",
                    style = MaterialTheme.typography.labelLarge,
                    color = FinanceTextMuted
                )
                Text(
                    text = "${(uiModel.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                    color = FinanceTextPrimary
                )
            }
            DonDoneProgressBar(progress = uiModel.progress)
            Text(
                text = uiModel.progressHintText,
                style = MaterialTheme.typography.bodySmall,
                color = FinanceTextMuted
            )
        }
        FinancePrimaryButton(
            text = uiModel.actionText,
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenSheet
        )
        if (uiModel.secondaryActionText != null) {
            FinanceSoftButton(
                text = uiModel.secondaryActionText,
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenWorkproof
            )
        }
    }
}

@Composable
private fun FinanceVaultSection(
    uiModel: FinanceVaultUiModel,
    onOpenSheet: () -> Unit
) {
    var dismissedStatusKey by rememberSaveable { mutableStateOf<String?>(null) }
    val showStatusBanner =
        uiModel.latestStatusText != null &&
            uiModel.latestStatusKey != null &&
            uiModel.latestStatusKey != dismissedStatusKey
    val description = when {
        showStatusBanner -> ""
        uiModel.depositStatusText == "미신청" || uiModel.depositStatusText == "미예치" -> ""
        else -> uiModel.helperText
    }

    FinanceBlockSection(
        title = "예치 이자",
        description = description
    ) {
        FinanceKeyValueRow(label = "예치 잔액", value = uiModel.depositStatusText)
        FinanceKeyValueRow(label = "누적 이자(추정)", value = uiModel.accruedInterestText)
        FinanceKeyValueRow(label = "예상 연이율", value = uiModel.aprText)
        if (showStatusBanner) {
            FinanceVaultStatusBanner(
                title = uiModel.latestStatusText,
                body = uiModel.detail.statusBodyText ?: uiModel.helperText,
                tone = when {
                    uiModel.latestStatusIsError -> BadgeTone.Warning
                    uiModel.latestStatusText.contains("완료") -> BadgeTone.Success
                    else -> BadgeTone.Info
                },
                onDismiss = { dismissedStatusKey = uiModel.latestStatusKey }
            )
        }
        FinancePrimaryButton(
            text = uiModel.actionText,
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenSheet
        )
    }
}

@Composable
private fun FinanceBlockSection(
    title: String,
    description: String,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    FinanceSectionSurface {
        FinanceSectionHeader(
            title = title,
            trailing = {
                if (trailing != null) {
                    trailing()
                } else {
                    Spacer(modifier = Modifier.height(1.dp))
                }
            }
        )
        if (description.isNotBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = FinanceTextMuted
            )
        }
        content()
    }
}

@Composable
private fun FinanceBottomSheetSection(
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
private fun FinanceBottomSheetHeader(
    title: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
        color = FinanceTextPrimary
    )
}

@Composable
private fun FinanceBottomSheetDivider() {
    Spacer(modifier = Modifier.height(14.dp))
    HorizontalDivider(color = FinanceDivider)
    Spacer(modifier = Modifier.height(14.dp))
}

@Composable
private fun FinanceSectionSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}

@Composable
private fun FinanceSectionDivider() {
    Spacer(modifier = Modifier.height(14.dp))
    HorizontalDivider(color = FinanceDivider)
    Spacer(modifier = Modifier.height(14.dp))
}

@Composable
private fun FinanceSectionHeader(
    title: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = FinanceTextPrimary
        )
        trailing()
    }
}

@Composable
private fun FinanceKeyValueRow(
    label: String,
    value: String,
    valueColor: Color = FinanceTextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = FinanceTextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FinanceVaultStatusBanner(
    title: String,
    body: String,
    tone: BadgeTone,
    onDismiss: () -> Unit
) {
    DonDoneNoticeBanner(
        title = title,
        message = body,
        tone = tone,
        onDismiss = onDismiss
    )
}

@Composable
private fun FinanceAdvanceBottomSheet(
    uiModel: FinanceAdvanceDetailUiModel,
    onDismiss: () -> Unit,
    onSelectAmount: (Int) -> Unit,
    onRequestAdvance: () -> Unit,
    onClearRequestMessage: () -> Unit,
    onOpenWorkproof: () -> Unit,
    onOpenHistoryDetail: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "미리받기",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = FinanceTextPrimary
                    )
                }
                FinanceLinkButton(text = "닫기", onClick = onDismiss)
            }
            FinanceKeyValueRow(label = "지금 신청 가능", value = uiModel.availableText)
            FinanceKeyValueRow(label = "이번 달 사용", value = uiModel.usedText)
            FinanceKeyValueRow(label = "정산 예정일", value = uiModel.repaymentDueText)
            FinanceAdvanceStatePanel(
                surfaceState = uiModel.surfaceState,
                title = uiModel.stateTitleText,
                body = uiModel.stateBodyText
            )
            if (uiModel.noticeTitleText != null && uiModel.noticeBodyText != null) {
                FinanceAdvanceNoticePanel(
                    title = uiModel.noticeTitleText,
                    body = uiModel.noticeBodyText
                )
            }
        }

        FinanceBottomSheetDivider()
        FinanceBottomSheetSection {
            FinanceBottomSheetHeader(title = "근무 반영")
            Text(
                text = uiModel.calendarSummaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = FinanceTextMuted
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FinanceAdvanceStatusCard(
                    label = "반영",
                    value = uiModel.reflectedCountText,
                    borderColor = FinanceDivider,
                    labelColor = FinanceTextMuted,
                    modifier = Modifier.weight(1f)
                )
                FinanceAdvanceStatusCard(
                    label = "확인 필요",
                    value = uiModel.reviewCountText,
                    borderColor = FinanceDivider,
                    labelColor = FinanceTextMuted,
                    modifier = Modifier.weight(1f)
                )
                FinanceAdvanceStatusCard(
                    label = "미반영",
                    value = uiModel.unreflectedCountText,
                    borderColor = FinanceDivider,
                    labelColor = FinanceTextMuted,
                    modifier = Modifier.weight(1f)
                )
            }
            FinanceSheetPanel(
                backgroundColor = FinanceSurfaceMuted,
                borderColor = FinanceDivider
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "반영 기준 ${uiModel.updatedAtText}",
                        style = MaterialTheme.typography.labelMedium,
                        color = FinanceTextMuted
                    )
                    FinanceAdvanceCalendarGrid(days = uiModel.calendarDays)
                }
            }
        }

        FinanceBottomSheetDivider()
        if (uiModel.amountOptions.isNotEmpty()) {
            FinanceBottomSheetSection {
                FinanceBottomSheetHeader(title = "받을 금액")
                FinanceAmountOptionGrid(
                    options = uiModel.amountOptions,
                    onSelect = {
                        onClearRequestMessage()
                        onSelectAmount(it)
                    }
                )
                Text(
                    text = uiModel.progressHintText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FinanceTextMuted
                )
                FinanceKeyValueRow(label = "이번 수령 예정", value = uiModel.receiveAmountText)
                FinanceKeyValueRow(label = "수수료", value = uiModel.feeText)
                FinanceKeyValueRow(label = "선택 금액", value = uiModel.requestAmountText)
            }
        }

        if (uiModel.blockReasonTexts.isNotEmpty()) {
            FinanceBottomSheetDivider()
            FinanceBottomSheetSection {
                FinanceBottomSheetHeader(title = "지금 막힌 이유")
                uiModel.blockReasonTexts.forEach { reason ->
                    Text(
                        text = "- $reason",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FinanceTextPrimary
                    )
                }
            }
        }

        if (uiModel.historyItems.isNotEmpty()) {
            FinanceBottomSheetDivider()
            FinanceBottomSheetSection {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FinanceBottomSheetHeader(title = "이번 달 이력")
                    FinanceCapsule(text = uiModel.tierText)
                }
                uiModel.historyItems.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                enabled = item.clickable && item.requestId != null,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberDonDoneGrayRipple(bounded = true)
                            ) {
                                item.requestId?.let(onOpenHistoryDetail)
                            }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                color = FinanceTextPrimary
                            )
                            Text(
                                text = item.metaText,
                                style = MaterialTheme.typography.labelMedium,
                                color = FinanceTextMuted
                            )
                        }
                        Text(
                            text = item.valueText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                            color = FinanceTextPrimary
                        )
                    }
                }
            }
        }

        FinanceBottomSheetDivider()
        FinanceBottomSheetSection {
            Text(
                text = uiModel.disclaimerText,
                style = MaterialTheme.typography.bodySmall,
                color = FinanceTextMuted
            )
            if (uiModel.requestFeedbackText != null) {
                FinanceSheetPanel(
                    backgroundColor = if (uiModel.requestFeedbackIsError) Color(0xFFFFF5F5) else FinanceAdvanceSheetHero,
                    borderColor = if (uiModel.requestFeedbackIsError) Color(0xFFFECACA) else FinanceAdvanceSheetHeroBorder
                ) {
                    Text(
                        text = uiModel.requestFeedbackText,
                        style = MaterialTheme.typography.bodySmall,
                        color = FinanceTextPrimary
                    )
                }
            }
            FinancePrimaryButton(
                text = uiModel.requestButtonText,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiModel.canRequest,
                onClick = onRequestAdvance
            )
            if (uiModel.secondaryActionText != null) {
                FinanceSoftButton(
                    text = uiModel.secondaryActionText,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenWorkproof
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun FinanceAdvanceRequestDetailBottomSheet(
    uiModel: FinanceAdvanceRequestDetailUiModel,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        FinanceBottomSheetSection {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = uiModel.titleText,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = FinanceTextPrimary
                    )
                    Text(
                        text = uiModel.stateText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = FinanceTextMuted
                    )
                }
                FinanceLinkButton(text = "닫기", onClick = onDismiss)
            }

            if (uiModel.isLoading) {
                Text(
                    text = "신청 상세를 불러오는 중이에요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FinanceTextMuted
                )
            } else if (uiModel.errorMessage != null) {
                FinanceSheetPanel(
                    backgroundColor = Color(0xFFFFF5F5),
                    borderColor = Color(0xFFFECACA)
                ) {
                    Text(
                        text = uiModel.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = FinanceTextPrimary
                    )
                }
            } else {
                FinanceKeyValueRow(label = "요청 금액", value = uiModel.requestedAmountText)
                FinanceKeyValueRow(label = "승인 금액", value = uiModel.approvedAmountText)
                FinanceKeyValueRow(label = "수수료", value = uiModel.feeAmountText)
                FinanceKeyValueRow(label = "상환 예정일", value = uiModel.repaymentDueText)
                FinanceKeyValueRow(label = "생성 시각", value = uiModel.createdAtText)
            }
        }

        if (!uiModel.isLoading && uiModel.errorMessage == null) {
            FinanceBottomSheetDivider()
            FinanceBottomSheetSection {
                FinanceBottomSheetHeader(title = "신청 당시 기준")
                FinanceKeyValueRow(label = "당시 가능 금액", value = uiModel.snapshotAvailableText)
                FinanceKeyValueRow(label = "당시 최대 한도", value = uiModel.snapshotCapText)
                FinanceKeyValueRow(label = "반영 근무", value = uiModel.snapshotReflectedText)
                FinanceKeyValueRow(label = "확인 필요 기록", value = uiModel.snapshotReviewText)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun FinanceAdvanceStatePanel(
    surfaceState: AdvanceSurfaceState,
    title: String,
    body: String
) {
    val borderColor = when (surfaceState) {
        AdvanceSurfaceState.SUCCESS -> FinanceAdvanceSheetHeroBorder
        AdvanceSurfaceState.BLOCKED -> FinanceAdvanceSheetWarningBorder
        else -> FinanceDivider
    }
    val backgroundColor = when (surfaceState) {
        AdvanceSurfaceState.SUCCESS -> FinanceAdvanceSheetHero
        AdvanceSurfaceState.BLOCKED -> Color(0xFFFFFBEB)
        else -> FinanceSurfaceMuted
    }

    FinanceSheetPanel(
        backgroundColor = backgroundColor,
        borderColor = borderColor
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
            color = FinanceTextPrimary
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = FinanceTextMuted
        )
    }
}

@Composable
private fun FinanceAdvanceHeroCard(
    surfaceState: AdvanceSurfaceState,
    title: String,
    body: String,
    availableText: String,
    repaymentDueText: String
) {
    val borderColor = when (surfaceState) {
        AdvanceSurfaceState.SUCCESS -> FinanceAdvanceSheetHeroBorder
        AdvanceSurfaceState.BLOCKED -> FinanceAdvanceSheetWarningBorder
        else -> FinanceDivider
    }
    val backgroundColor = when (surfaceState) {
        AdvanceSurfaceState.SUCCESS -> FinanceAdvanceSheetHero
        AdvanceSurfaceState.BLOCKED -> FinanceAdvanceSheetWarningBackground
        else -> FinanceSurfaceMuted
    }

    FinanceSheetPanel(
        backgroundColor = backgroundColor,
        borderColor = borderColor
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = FinanceTextPrimary
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = FinanceTextMuted
        )
        Spacer(modifier = Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "지금 가능 금액",
                style = MaterialTheme.typography.labelLarge,
                color = FinanceTextMuted
            )
            Text(
                text = availableText,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = FinanceTextPrimary
            )
        }
        HorizontalDivider(color = borderColor)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "상환 예정일",
                style = MaterialTheme.typography.labelLarge,
                color = FinanceTextMuted
            )
            Text(
                text = repaymentDueText,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                color = FinanceTextPrimary
            )
        }
    }
}

@Composable
private fun FinanceAdvanceNoticePanel(
    title: String,
    body: String
) {
    FinanceSheetPanel(
        backgroundColor = FinanceAdvanceSheetWarningBackground,
        borderColor = FinanceAdvanceSheetWarningBorder
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
            color = FinanceTextPrimary
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = FinanceTextMuted
        )
    }
}

@Composable
private fun FinanceVaultBottomSheet(
    uiModel: FinanceVaultDetailUiModel,
    onDismiss: () -> Unit,
    onSelectAction: (VaultActionType) -> Unit,
    onSelectAmount: (Int) -> Unit,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        FinanceBottomSheetSection {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "예치 이자",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = FinanceTextPrimary
                    )
                }
                FinanceLinkButton(text = "닫기", onClick = onDismiss)
            }

            FinanceKeyValueRow(label = "내 지갑 잔액", value = uiModel.walletBalanceText)
            FinanceKeyValueRow(label = "현재 예치 잔액", value = uiModel.balanceText)
            FinanceKeyValueRow(label = "예상 연이율", value = uiModel.aprText)
        }

        FinanceBottomSheetDivider()
        FinanceBottomSheetSection {
            FinanceBottomSheetHeader(title = "요청 종류")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FinanceActionToggleButton(
                    text = "예치",
                    selected = uiModel.selectedActionType == VaultActionType.DEPOSIT,
                    enabled = uiModel.depositEnabled && !uiModel.actionInFlight,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectAction(VaultActionType.DEPOSIT) }
                )
                FinanceActionToggleButton(
                    text = "출금",
                    selected = uiModel.selectedActionType == VaultActionType.WITHDRAW,
                    enabled = uiModel.withdrawEnabled && !uiModel.actionInFlight,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectAction(VaultActionType.WITHDRAW) }
                )
            }
        }

        FinanceBottomSheetDivider()
        FinanceBottomSheetSection {
            FinanceBottomSheetHeader(title = "요청 금액")
            FinanceKeyValueRow(label = "지금 선택됨", value = uiModel.selectedAmountText)
            FinanceKeyValueRow(
                label = if (uiModel.selectedActionType == VaultActionType.WITHDRAW) "지금 출금 가능" else "지금 예치 가능",
                value = uiModel.availableText
            )
            FinanceAmountOptionGrid(
                options = uiModel.amountOptions,
                onSelect = onSelectAmount
            )
        }

        FinanceBottomSheetDivider()
        FinanceBottomSheetSection {
            FinanceBottomSheetHeader(title = "예상 수익")
            FinanceKeyValueRow(label = "월 예상 이자", value = uiModel.monthlyInterestText)
            FinanceKeyValueRow(label = "일 예상 이자", value = uiModel.dailyInterestText)
            FinanceKeyValueRow(label = "누적 이자(추정)", value = uiModel.accruedInterestText)
        }

        FinanceBottomSheetDivider()
        FinanceBottomSheetSection {
            FinancePrimaryButton(
                text = uiModel.actionText,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiModel.actionEnabled,
                onClick = onAction
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun FinanceVaultOnboardingSection(uiModel: FinanceVaultDetailUiModel) {
    FinanceSheetPanel(
        backgroundColor = FinanceAdvanceSheetMutedBackground,
        borderColor = FinanceAdvanceSheetMutedBorder
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "지금 예치 가능한 금액",
                    style = MaterialTheme.typography.labelLarge,
                    color = DawnTextSubtle
                )
                Text(
                    text = uiModel.availableText,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                    color = DawnText
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "예치 신청 금액",
                    style = MaterialTheme.typography.labelLarge,
                    color = DawnText
                )
                FinanceAmountOptionGrid(options = uiModel.amountOptions)
            }
        }
    }

    FinanceSheetPanel(
        backgroundColor = FinanceAdvanceSheetHero,
        borderColor = FinanceAdvanceSheetHeroBorder
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FinanceAdvanceLineItem(label = "예상 연이율", value = uiModel.aprText)
            FinanceAdvanceLineItem(label = "월 예상 이자", value = uiModel.monthlyInterestText)
            FinanceAdvanceLineItem(label = "일 예상 이자", value = uiModel.dailyInterestText)
        }
    }
}

@Composable
private fun FinanceVaultActiveSection(uiModel: FinanceVaultDetailUiModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FinanceSummaryMetricCard(
            label = "예치 잔액",
            value = uiModel.balanceText,
            modifier = Modifier.weight(1f)
        )
        FinanceSummaryMetricCard(
            label = "예상 연이율",
            value = uiModel.aprText,
            modifier = Modifier.weight(1f)
        )
    }

    FinanceSheetPanel(
        backgroundColor = FinanceAdvanceSheetHero,
        borderColor = FinanceAdvanceSheetHeroBorder
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FinanceAdvanceLineItem(label = "누적 이자(추정)", value = uiModel.accruedInterestText)
            FinanceAdvanceLineItem(label = "월 예상 이자", value = uiModel.monthlyInterestText)
            FinanceAdvanceLineItem(label = "일 예상 이자", value = uiModel.dailyInterestText)
        }
    }

    FinanceSheetPanel(
        backgroundColor = FinanceAdvanceSheetMutedBackground,
        borderColor = FinanceAdvanceSheetMutedBorder
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "월 수익 구성(추정)",
                style = MaterialTheme.typography.labelLarge,
                color = DawnText
            )
            FinanceAdvanceLineItem(label = "DeFi 운용 수익", value = uiModel.defiMonthlyText)
            FinanceAdvanceLineItem(label = "가불 수수료 기여분", value = uiModel.feeShareText)
            FinanceAdvanceLineItem(label = "합계", value = uiModel.totalMonthlyText)
        }
    }

    FinanceSheetPanel(
        backgroundColor = FinanceAdvanceSheetMutedBackground,
        borderColor = FinanceAdvanceSheetMutedBorder
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "풀 운용 현황",
                style = MaterialTheme.typography.labelLarge,
                color = DawnText
            )
            FinanceRatioBar(
                label = "DeFi 운용 비율",
                value = uiModel.defiRatioText,
                progress = ratioTextToFloat(uiModel.defiRatioText)
            )
            FinanceRatioBar(
                label = "가불 풀 비율",
                value = uiModel.advanceRatioText,
                progress = ratioTextToFloat(uiModel.advanceRatioText)
            )
            FinanceAdvanceLineItem(label = "가불 풀 사용률", value = uiModel.advanceUsageText)
        }
    }
}

@Composable
private fun FinanceAmountOptionGrid(
    options: List<FinanceAmountOptionUiModel>,
    onSelect: ((Int) -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(4).forEach { optionRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                optionRow.forEach { option ->
                    val backgroundColor = if (option.selected) FinanceAdvanceSheetHero else Color.White
                    val borderColor = if (option.selected) DawnPrimary else DawnBorder
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(backgroundColor, RoundedCornerShape(18.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(
                                enabled = onSelect != null,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberDonDoneGrayRipple(bounded = true)
                            ) {
                                onSelect?.invoke(option.amount)
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = DawnText
                        )
                    }
                }
                repeat(4 - optionRow.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FinanceAdvanceCalendarGrid(days: List<FinanceAdvanceCalendarDayUiModel>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        days.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                week.forEach { day ->
                    val backgroundColor = when (day.tone) {
                        FinanceAdvanceCalendarTone.COMPLETE -> Color.White
                        FinanceAdvanceCalendarTone.MODIFIED -> Color(0xFFFFFBEB)
                        FinanceAdvanceCalendarTone.TODAY -> FinanceAdvanceSheetHero
                        FinanceAdvanceCalendarTone.DEFAULT -> Color.White
                    }
                    val borderColor = when (day.tone) {
                        FinanceAdvanceCalendarTone.COMPLETE -> FinanceAdvanceSheetHeroBorder
                        FinanceAdvanceCalendarTone.MODIFIED -> FinanceAdvanceSheetWarningBorder
                        FinanceAdvanceCalendarTone.TODAY -> DawnPrimary
                        FinanceAdvanceCalendarTone.DEFAULT -> DawnBorder
                    }
                    val textColor = when (day.tone) {
                        FinanceAdvanceCalendarTone.COMPLETE -> DawnText
                        FinanceAdvanceCalendarTone.MODIFIED -> Color(0xFFB97711)
                        FinanceAdvanceCalendarTone.TODAY -> DawnPrimary
                        FinanceAdvanceCalendarTone.DEFAULT -> FinanceAdvanceSheetDefaultText
                    }
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(backgroundColor, RoundedCornerShape(14.dp))
                                .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.day.toString().padStart(2, '0'),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = textColor
                            )
                        }
                    }
                }
                repeat(7 - week.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FinanceSheetPanel(
    backgroundColor: Color,
    borderColor: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(24.dp))
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
private fun FinanceAdvanceStatusCard(
    label: String,
    value: String,
    borderColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
            color = DawnText
        )
    }
}

@Composable
private fun FinanceAdvanceLineItem(label: String, value: String) {
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
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
            color = DawnText
        )
    }
}

@Composable
private fun FinanceSummaryMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(FinanceAdvanceSheetMutedBackground, RoundedCornerShape(24.dp))
            .border(1.dp, FinanceAdvanceSheetMutedBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = DawnTextSubtle
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
            color = DawnText
        )
    }
}

@Composable
private fun FinanceRatioBar(
    label: String,
    value: String,
    progress: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                color = DawnText
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color(0xFFE2E8F0), RoundedCornerShape(999.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(8.dp)
                    .background(DawnPrimary, RoundedCornerShape(999.dp))
            )
        }
    }
}

private fun ratioTextToFloat(value: String): Float {
    val numeric = value.removeSuffix("%").toFloatOrNull() ?: return 0f
    return numeric / 100f
}

@Composable
private fun FinanceInnerPanel(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FinanceSurfaceMuted, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
private fun FinanceCapsule(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .background(FinanceSurfaceMuted, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelMedium,
        color = FinanceTextMuted
    )
}

@Composable
private fun FinanceLinkButton(text: String, onClick: () -> Unit) {
    FinanceLinkText(
        text = text,
        onClick = onClick
    )
}

@Composable
private fun FinanceLinkText(
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .pressableScale(
                interactionSource = interactionSource,
                pressedScale = 0.98f
            )
            .clickable(
                interactionSource = interactionSource,
                indication = rememberDonDoneGrayRipple(),
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
        color = FinanceAccent
    )
}

@Composable
private fun FinancePrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.foundation.LocalIndication provides rememberDonDoneGrayRipple()
    ) {
        Button(
            modifier = modifier.pressableScale(interactionSource = interactionSource),
            onClick = onClick,
            enabled = enabled,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FinanceAccent,
                contentColor = Color.White,
                disabledContainerColor = FinanceDivider,
                disabledContentColor = FinanceTextMuted
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black)
            )
        }
    }
}

@Composable
private fun FinanceSoftButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.foundation.LocalIndication provides rememberDonDoneGrayRipple()
    ) {
        OutlinedButton(
            modifier = modifier.pressableScale(interactionSource = interactionSource),
            onClick = onClick,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, FinanceDivider),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = FinanceSurfaceMuted,
                contentColor = FinanceTextPrimary
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black)
            )
        }
    }
}

@Composable
private fun FinanceActionToggleButton(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.foundation.LocalIndication provides rememberDonDoneGrayRipple()
    ) {
        OutlinedButton(
            modifier = modifier.pressableScale(interactionSource = interactionSource),
            onClick = onClick,
            enabled = enabled,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                width = 1.dp,
                color = if (selected) FinanceAccent else FinanceDivider
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (selected) FinanceAdvanceSheetHero else Color.White,
                contentColor = FinanceTextPrimary,
                disabledContainerColor = FinanceSurfaceMuted,
                disabledContentColor = FinanceTextMuted
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black)
            )
        }
    }
}

@Composable
private fun FinanceGhostButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FinanceSoftButton(text = text, modifier = modifier, onClick = onClick)
}
