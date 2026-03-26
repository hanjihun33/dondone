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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
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
private val FinanceAdvanceSheetDefaultText = Color(0xFF94A3B8)
private val FinanceReflectedBackground = Color(0xFFE9DEFF)
private val FinanceReflectedBorder = Color(0xFFB89BFF)
private val FinanceReflectedText = Color(0xFF5E3CC5)
private val FinanceReviewBackground = Color(0xFFF7E4F4)
private val FinanceReviewBorder = Color(0xFFD98FD0)
private val FinanceReviewText = Color(0xFFAA3E96)
private val FinanceTodayBackground = Color(0xFFF2F3FF)
private val FinanceTodayBorder = Color(0xFFDDE4FF)
private val FinanceCalendarDefaultBorder = Color(0xFFF4F6F8)
private val FinanceCalendarInactiveText = Color(0xFFCBD5E1)
private val FinanceWeekdays = listOf("일", "월", "화", "수", "목", "금", "토")

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
    onOpenWorkproof: () -> Unit,
    onOpenWorkerRegistrationCode: () -> Unit
) {
    val advanceSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val advanceRequestDetailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val vaultSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val advanceTierGuideSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAdvanceSheet by remember { mutableStateOf(false) }
    var showAdvanceRequestDetailSheet by remember { mutableStateOf(false) }
    var showVaultSheet by remember { mutableStateOf(false) }
    var showAdvanceTierGuideSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiModel.vault.shouldDismissDetailSheet) {
        if (uiModel.vault.shouldDismissDetailSheet) {
            showVaultSheet = false
        }
    }

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
                onOpenWorkproof = onOpenWorkproof,
                onOpenWorkerRegistrationCode = onOpenWorkerRegistrationCode,
                onOpenTierGuide = { showAdvanceTierGuideSheet = true }
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

        if (showAdvanceTierGuideSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAdvanceTierGuideSheet = false },
                sheetState = advanceTierGuideSheetState,
                containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = null
            ) {
                FinanceAdvanceTierGuideSheet(
                    uiModel = uiModel.advance,
                    onDismiss = { showAdvanceTierGuideSheet = false }
                )
            }
        }
}

@Composable
private fun FinanceAdvanceSection(
    uiModel: FinanceAdvanceUiModel,
    onOpenSheet: () -> Unit,
    onOpenWorkproof: () -> Unit,
    onOpenWorkerRegistrationCode: () -> Unit,
    onOpenTierGuide: () -> Unit
) {
    FinanceBlockSection(
        title = "미리받기",
        description = ""
    ) {
        if (uiModel.sourceLabelText.isNotBlank()) {
            Text(
                text = uiModel.sourceLabelText,
                style = MaterialTheme.typography.labelSmall,
                color = FinanceTextMuted
            )
        }
        FinanceAdvanceSuccessSummary(
            title = uiModel.stateTitleText,
            body = uiModel.stateBodyText,
            amountText = uiModel.heroAmountText,
            repaymentDueText = uiModel.repaymentDueText
        )
        if (uiModel.noticeTitleText != null && uiModel.noticeBodyText != null) {
            FinanceAdvanceNoticePanel(
                title = uiModel.noticeTitleText,
                body = uiModel.noticeBodyText
            )
        }
        if (uiModel.showProgress) {
            FinanceInnerPanel {
                val showSummaryMetrics =
                    uiModel.progressPrimaryMetricLabel != null &&
                        uiModel.progressPrimaryMetricText != null &&
                        uiModel.progressSecondaryMetricLabel != null &&
                        uiModel.progressSecondaryMetricText != null
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiModel.progressTitle,
                            style = MaterialTheme.typography.labelLarge,
                            color = FinanceTextMuted
                        )
                        FinanceHelpButton(onClick = onOpenTierGuide)
                    }
                    Text(
                        text = "${(uiModel.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                        color = FinanceTextPrimary
                    )
                }
                DonDoneProgressBar(progress = uiModel.progress)
                if (!showSummaryMetrics) {
                    Text(
                        text = uiModel.progressHintText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = FinanceTextMuted
                    )
                }
                if (showSummaryMetrics) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FinanceProgressMetric(
                            label = uiModel.progressPrimaryMetricLabel,
                            value = uiModel.progressPrimaryMetricText,
                            modifier = Modifier.weight(1f)
                        )
                        FinanceProgressMetric(
                            label = uiModel.progressSecondaryMetricLabel,
                            value = uiModel.progressSecondaryMetricText,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        FinancePrimaryButton(
            text = uiModel.actionText,
            modifier = Modifier.fillMaxWidth(),
            onClick = if (uiModel.primaryActionOpensWorkerRegistration) {
                onOpenWorkerRegistrationCode
            } else {
                onOpenSheet
            }
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
private fun FinanceAmountKeyValueRow(
    label: String,
    value: String
) {
    val parts = value.split("·", limit = 2).map { it.trim() }
    val primaryValue = parts.firstOrNull().orEmpty()
    val secondaryValue = parts.getOrNull(1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = FinanceTextMuted
        )
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                color = FinanceTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!secondaryValue.isNullOrBlank()) {
                Text(
                    text = secondaryValue,
                    style = MaterialTheme.typography.labelMedium,
                    color = FinanceTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FinanceDetailKeyValueRow(
    label: String,
    value: String,
    valueColor: Color = FinanceTextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.34f),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = DawnTextSubtle
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.66f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
            color = valueColor
        )
    }
}

@Composable
private fun FinanceDetailAmountRow(
    label: String,
    value: String
) {
    val parts = value.split("·", limit = 2).map { it.trim() }
    val primaryValue = parts.firstOrNull().orEmpty()
    val secondaryValue = parts.getOrNull(1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.34f),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = DawnTextSubtle
        )
        Column(
            modifier = Modifier.weight(0.66f),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = primaryValue,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = FinanceTextPrimary
            )
            if (!secondaryValue.isNullOrBlank()) {
                Text(
                    text = secondaryValue,
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = FinanceTextMuted
                )
            }
        }
    }
}

@Composable
private fun FinanceDetailGroupPanel(
    content: @Composable ColumnScope.() -> Unit
) {
    FinanceSheetPanel(
        backgroundColor = Color.White,
        borderColor = FinanceDivider
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp), content = content)
    }
}

@Composable
private fun FinanceDetailHeroAmount(
    label: String,
    value: String
) {
    val parts = value.split("·", limit = 2).map { it.trim() }
    val primaryValue = parts.firstOrNull().orEmpty()
    val secondaryValue = parts.getOrNull(1)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = FinanceTextMuted
        )
        Text(
            text = primaryValue,
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
            color = FinanceTextPrimary
        )
        if (!secondaryValue.isNullOrBlank()) {
            Text(
                text = secondaryValue,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = FinanceTextMuted
            )
        }
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
            Spacer(modifier = Modifier.height(6.dp))
            if (uiModel.hasCurrentRequest) {
                FinanceSummaryAmountCard(
                    label = "이번 달 받은 금액",
                    value = uiModel.summaryAmountText,
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider(color = FinanceDivider)
                FinanceSummaryAmountCard(
                    label = "추가 신청 가능 금액",
                    value = uiModel.availableText,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FinanceSummaryMetricCard(
                        label = "정산일",
                        value = uiModel.repaymentDueText,
                        modifier = Modifier.weight(1f)
                    )
                    FinanceSummaryMetricCard(
                        label = "지급 상태",
                        value = uiModel.summaryStatusText,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                FinanceAdvanceStatePanel(
                    surfaceState = uiModel.surfaceState,
                    title = uiModel.stateTitleText,
                    body = uiModel.stateBodyText
                )
            }
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
                    borderColor = FinanceReflectedBorder,
                    labelColor = FinanceReflectedText,
                    valueColor = FinanceTextPrimary,
                    backgroundColor = FinanceReflectedBackground,
                    modifier = Modifier.weight(1f)
                )
                FinanceAdvanceStatusCard(
                    label = "확인 필요",
                    value = uiModel.reviewCountText,
                    borderColor = FinanceReviewBorder,
                    labelColor = FinanceReviewText,
                    valueColor = FinanceTextPrimary,
                    backgroundColor = FinanceReviewBackground,
                    modifier = Modifier.weight(1f)
                )
                FinanceAdvanceStatusCard(
                    label = "미반영",
                    value = uiModel.unreflectedCountText,
                    borderColor = FinanceAdvanceSheetMutedBorder,
                    labelColor = FinanceTextMuted,
                    valueColor = DawnText,
                    backgroundColor = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }
            FinanceSheetPanel(
                backgroundColor = Color.White,
                borderColor = FinanceDivider
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (uiModel.calendarDays.isNotEmpty()) {
                        FinanceAdvanceCalendarGrid(days = uiModel.calendarDays)
                    } else {
                        Text(
                            text = uiModel.calendarSummaryText,
                            style = MaterialTheme.typography.bodySmall,
                            color = FinanceTextMuted
                        )
                    }
                }
            }
        }

        if (uiModel.canRequestAdditional && uiModel.amountOptions.isNotEmpty()) {
            FinanceBottomSheetDivider()
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

        if (!uiModel.canRequestAdditional && uiModel.blockReasonTexts.isNotEmpty()) {
            FinanceBottomSheetDivider()
            FinanceBottomSheetSection {
                FinanceBottomSheetHeader(title = "신청 제한")
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
                FinanceBottomSheetHeader(title = "이번 달 이력")
                uiModel.historyItems.forEachIndexed { index, item ->
                    if (item.isEmptyState) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FinanceSurfaceMuted, RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = FinanceTextPrimary
                            )
                            Text(
                                text = item.metaText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = FinanceTextMuted
                            )
                            if (!item.detailText.isNullOrBlank()) {
                                Text(
                                    text = item.detailText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FinanceTextMuted
                                )
                            }
                        }
                    } else {
                        val shape = RoundedCornerShape(14.dp)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .background(FinanceSurfaceMuted)
                                .then(
                                    if (item.clickable && item.requestId != null) {
                                        Modifier.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = rememberDonDoneGrayRipple(bounded = true)
                                        ) {
                                            item.requestId.let(onOpenHistoryDetail)
                                        }
                                    } else Modifier
                                )
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (item.valueText.isNotBlank() && item.valueText != "-") {
                                FinanceHistoryAmountSummary(
                                    value = item.valueText
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = item.metaText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = FinanceTextMuted
                                    )
                                    if (!item.detailText.isNullOrBlank()) {
                                        Text(
                                            text = item.detailText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = FinanceTextMuted
                                        )
                                    }
                                }
                                if (item.statusLabel.isNotBlank()) {
                                    Text(
                                        text = item.statusLabel,
                                        modifier = Modifier
                                            .background(
                                                FinanceAccent.copy(alpha = 0.1f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = FinanceAccent
                                    )
                                }
                            }
                        }
                    }
                    if (index < uiModel.historyItems.lastIndex) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        if (uiModel.canRequestAdditional || !uiModel.hasCurrentRequest || uiModel.secondaryActionText != null || uiModel.requestFeedbackText != null) {
            FinanceBottomSheetDivider()
            FinanceBottomSheetSection {
                if (uiModel.canRequestAdditional && uiModel.requestFeedbackText != null) {
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
                if (uiModel.canRequestAdditional || !uiModel.hasCurrentRequest) {
                    FinancePrimaryButton(
                        text = uiModel.requestButtonText,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiModel.canRequest || uiModel.requestButtonText == "닫기",
                        onClick = if (uiModel.canRequest) onRequestAdvance else onDismiss
                    )
                }
                if (uiModel.secondaryActionText != null) {
                    FinanceSoftButton(
                        text = uiModel.secondaryActionText,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onOpenWorkproof
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun FinanceHistoryAmountSummary(value: String) {
    val parts = value.split("·", limit = 2).map { it.trim() }
    val primaryValue = parts.firstOrNull().orEmpty()
    val secondaryValue = parts.getOrNull(1)
    val amountParts = primaryValue.split(" ", limit = 2)
    val amountValue = amountParts.firstOrNull().orEmpty()
    val amountUnit = amountParts.getOrNull(1)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = MaterialTheme.typography.titleLarge.fontSize
                    )
                ) {
                    append(amountValue)
                }
                if (!amountUnit.isNullOrBlank()) {
                    append(" ")
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    ) {
                        append(amountUnit)
                    }
                }
            },
            style = MaterialTheme.typography.titleLarge,
            color = FinanceTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!secondaryValue.isNullOrBlank()) {
            Text(
                text = secondaryValue,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = FinanceTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = uiModel.titleText,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = FinanceTextPrimary
                    )
                }
                FinanceLinkButton(text = "닫기", onClick = onDismiss)
            }
            Spacer(modifier = Modifier.height(8.dp))

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
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val summaryAmountText =
                        if (uiModel.approvedAmountText != "승인 대기") uiModel.approvedAmountText else uiModel.requestedAmountText
                    val summaryAmountLabel =
                        if (uiModel.approvedAmountText != "승인 대기") "지급 금액" else "신청 금액"
                    FinanceAdvanceRequestSummaryCard(
                        amountLabel = summaryAmountLabel,
                        amountText = summaryAmountText
                    )
                    FinanceBottomSheetHeader(title = "상세 정보")
                    FinanceDetailGroupPanel {
                        FinanceDetailKeyValueRow(label = "지급 상태", value = uiModel.payoutStatusText)
                        HorizontalDivider(color = FinanceDivider)
                        FinanceDetailKeyValueRow(label = "정산 상태", value = uiModel.settlementStatusText)
                        HorizontalDivider(color = FinanceDivider)
                        uiModel.payoutTxHashText?.let {
                            FinanceDetailKeyValueRow(
                                label = "거래 확인",
                                value = "블록체인 거래 확인",
                                valueColor = FinanceAccent
                            )
                            HorizontalDivider(color = FinanceDivider)
                        }
                        FinanceDetailKeyValueRow(label = "신청 시각", value = uiModel.createdAtText)
                    }
                }
            }
        }

        if (!uiModel.isLoading && uiModel.errorMessage == null) {
            FinanceBottomSheetDivider()
            FinanceBottomSheetSection {
                FinanceBottomSheetHeader(title = "계산 기준")
                FinanceDetailGroupPanel {
                    FinanceAmountKeyValueRow(label = "당시 신청 가능 금액", value = uiModel.snapshotAvailableText)
                    HorizontalDivider(color = FinanceDivider)
                    FinanceDetailKeyValueRow(label = "반영 근무", value = uiModel.snapshotReflectedText)
                    HorizontalDivider(color = FinanceDivider)
                    FinanceDetailKeyValueRow(label = "확인 필요 기록", value = uiModel.snapshotReviewText)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun FinanceAdvanceRequestSummaryCard(
    amountLabel: String,
    amountText: String
) {
    val parts = amountText.split("·", limit = 2).map { it.trim() }
    val primaryValue = parts.firstOrNull().orEmpty()
    val secondaryValue = parts.getOrNull(1)

    FinanceSheetPanel(
        backgroundColor = FinanceAdvanceSheetMutedBackground,
        borderColor = FinanceAdvanceSheetMutedBorder
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = amountLabel,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = FinanceTextMuted
            )
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                color = FinanceTextPrimary
            )
            if (!secondaryValue.isNullOrBlank()) {
                Text(
                    text = secondaryValue,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = FinanceTextMuted
                )
            }
        }
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
        AdvanceSurfaceState.BLOCKED -> FinanceAdvanceSheetMutedBorder
        else -> FinanceDivider
    }
    val backgroundColor = when (surfaceState) {
        AdvanceSurfaceState.SUCCESS -> Color.White
        AdvanceSurfaceState.BLOCKED -> FinanceAdvanceSheetMutedBackground
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
        if (body.isNotBlank()) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = FinanceTextMuted
            )
        }
    }
}

@Composable
private fun FinanceAdvanceSuccessSummary(
    title: String,
    body: String = "",
    amountText: String,
    repaymentDueText: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = FinanceTextPrimary
            )
            if (body.isNotBlank()) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = FinanceTextMuted
                )
            }
        }
        FinanceAdvanceHeroAmount(amountText = amountText)
        HorizontalDivider(color = FinanceDivider)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "정산일",
                style = MaterialTheme.typography.labelLarge,
                color = FinanceTextMuted
            )
            Text(
                text = repaymentDueText,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = FinanceTextPrimary
            )
        }
    }
}

@Composable
private fun FinanceAdvanceHeroCard(
    surfaceState: AdvanceSurfaceState,
    title: String,
    body: String,
    amountLabel: String,
    amountText: String,
    repaymentDueText: String
) {
    val backgroundColor = when (surfaceState) {
        AdvanceSurfaceState.BLOCKED -> FinanceAdvanceSheetMutedBackground
        else -> FinanceSurfaceMuted
    }
    FinanceSheetPanel(
        backgroundColor = backgroundColor,
        borderColor = Color.Transparent
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = FinanceTextPrimary
        )
        if (body.isNotBlank()) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = FinanceTextMuted
            )
        }
        FinanceAdvanceHeroAmount(amountText = amountText)
        HorizontalDivider(color = FinanceDivider)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "정산일",
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
private fun FinanceAdvanceHeroAmount(amountText: String) {
    val parts = amountText.split(" · 약 ", limit = 2)
    val primaryAmount = parts.firstOrNull().orEmpty()
    val referenceAmount = parts.getOrNull(1)
    val amountParts = primaryAmount.split(" ", limit = 2)
    val amountValue = amountParts.firstOrNull().orEmpty()
    val amountUnit = amountParts.getOrNull(1)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Black,
                        fontSize = MaterialTheme.typography.displaySmall.fontSize
                    )
                ) {
                    append(amountValue)
                }
                if (!amountUnit.isNullOrBlank()) {
                    append(" ")
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    ) {
                        append(amountUnit)
                    }
                }
            },
            style = MaterialTheme.typography.displaySmall,
            color = FinanceTextPrimary
        )
        if (!referenceAmount.isNullOrBlank()) {
            Text(
                text = "약 $referenceAmount",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = FinanceTextMuted
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
        backgroundColor = FinanceAdvanceSheetMutedBackground,
        borderColor = FinanceAdvanceSheetMutedBorder
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
    val displayedMonth = YearMonth.now()
    val today = LocalDate.now()
    val firstDayOffset = displayedMonth.atDay(1).dayOfWeek.value % 7
    val dayMap = days.associateBy { it.day }
    val cellValues = buildList<FinanceAdvanceCalendarDayUiModel?> {
        repeat(firstDayOffset) { add(null) }
        (1..displayedMonth.lengthOfMonth()).forEach { day ->
            add(dayMap[day] ?: FinanceAdvanceCalendarDayUiModel(day = day, tone = FinanceAdvanceCalendarTone.DEFAULT))
        }
        while (size % 7 != 0) {
            add(null)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = displayedMonth.format(DateTimeFormatter.ofPattern("yyyy.MM")),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = FinanceTextPrimary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FinanceWeekdays.forEach { label ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = FinanceTextMuted
                    )
                }
            }
        }

        cellValues.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                week.forEach { day ->
                    if (day == null) {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        return@forEach
                    }
                    val backgroundColor = when (day.tone) {
                        FinanceAdvanceCalendarTone.COMPLETE -> FinanceReflectedBackground
                        FinanceAdvanceCalendarTone.MODIFIED -> FinanceReviewBackground
                        FinanceAdvanceCalendarTone.TODAY -> FinanceTodayBackground
                        FinanceAdvanceCalendarTone.DEFAULT -> Color.Transparent
                    }
                    val borderColor = when (day.tone) {
                        FinanceAdvanceCalendarTone.COMPLETE -> FinanceReflectedBorder
                        FinanceAdvanceCalendarTone.MODIFIED -> FinanceReviewBorder
                        FinanceAdvanceCalendarTone.TODAY -> FinanceTodayBorder
                        FinanceAdvanceCalendarTone.DEFAULT -> Color.Transparent
                    }
                    val textColor = when (day.tone) {
                        FinanceAdvanceCalendarTone.COMPLETE -> FinanceReflectedText
                        FinanceAdvanceCalendarTone.MODIFIED -> FinanceReviewText
                        FinanceAdvanceCalendarTone.TODAY -> DawnPrimary
                        FinanceAdvanceCalendarTone.DEFAULT -> if (day.day > today.dayOfMonth) FinanceCalendarInactiveText else FinanceAdvanceSheetDefaultText
                    }
                    val shape = when (day.tone) {
                        FinanceAdvanceCalendarTone.COMPLETE,
                        FinanceAdvanceCalendarTone.MODIFIED,
                        FinanceAdvanceCalendarTone.TODAY -> CircleShape
                        FinanceAdvanceCalendarTone.DEFAULT -> RoundedCornerShape(999.dp)
                    }
                    val showMarker = day.tone != FinanceAdvanceCalendarTone.DEFAULT
                    Box(
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (showMarker) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(backgroundColor, shape)
                                    .border(1.dp, borderColor, shape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.day.toString(),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = textColor
                                )
                            }
                        } else {
                            Text(
                                text = day.day.toString(),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                color = textColor
                            )
                        }
                    }
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
        verticalArrangement = Arrangement.spacedBy(6.dp)
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
    valueColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor
        )
        Text(
            text = value,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = valueColor
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
            .background(FinanceAdvanceSheetMutedBackground, RoundedCornerShape(14.dp))
            .border(1.dp, FinanceAdvanceSheetMutedBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = FinanceTextMuted
        )
        Text(
            text = value,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = FinanceTextPrimary
        )
    }
}

@Composable
private fun FinanceSummaryAmountCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val parts = value.split("·", limit = 2).map { it.trim() }
    val primaryValue = parts.firstOrNull().orEmpty()
    val secondaryValue = parts.getOrNull(1)
    val amountParts = primaryValue.split(" ", limit = 2)
    val amountValue = amountParts.firstOrNull().orEmpty()
    val amountUnit = amountParts.getOrNull(1)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = FinanceTextMuted
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = MaterialTheme.typography.titleLarge.fontSize
                        )
                    ) {
                        append(amountValue)
                    }
                    if (!amountUnit.isNullOrBlank()) {
                        append(" ")
                        withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        ) {
                            append(amountUnit)
                        }
                    }
                },
                style = MaterialTheme.typography.titleLarge,
                color = FinanceTextPrimary
            )
            if (!secondaryValue.isNullOrBlank()) {
                Text(
                    text = secondaryValue,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = FinanceTextMuted
                )
            }
        }
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
            .background(FinanceSurfaceMuted, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content()
    }
}

@Composable
private fun FinanceProgressMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val parts = value.split("·", limit = 2).map { it.trim() }
    val primaryValue = parts.firstOrNull().orEmpty()

    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FinanceDivider, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = FinanceTextMuted
        )
        Text(
            text = primaryValue,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = FinanceTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FinanceHelpButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White)
            .border(1.dp, FinanceDivider, RoundedCornerShape(999.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = rememberDonDoneGrayRipple(bounded = false),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "?",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
            color = FinanceTextMuted
        )
    }
}

@Composable
private fun FinanceAdvanceTierGuideSheet(
    uiModel: FinanceAdvanceUiModel,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        FinanceBottomSheetSection {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "한도 구간 안내",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = FinanceTextPrimary
                    )
                }
                FinanceLinkButton(text = "닫기", onClick = onDismiss)
            }
        }

        FinanceBottomSheetDivider()
        FinanceBottomSheetSection {
            if (
                uiModel.hasCurrentRequest &&
                uiModel.progressPrimaryMetricText != null &&
                uiModel.progressSecondaryMetricText != null
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "현재 내 한도",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = FinanceTextPrimary
                    )
                    FinanceGuideMetricOverview(
                        currentLimit = uiModel.progressSecondaryMetricText,
                        usedAmount = uiModel.progressPrimaryMetricText,
                        availableAmount = uiModel.availableText
                    )
                    Text(
                        text = "출근 기록이 더 반영되면 같은 달에도 한도가 늘어날 수 있어요.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = FinanceTextMuted
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "구간별 최대 한도",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = FinanceTextPrimary
                )
                FinanceTierGuideRow(title = "반영 근무 0~4일", body = "아직 신청할 수 없어요")
                FinanceTierGuideRow(title = "반영 근무 5일 이상", body = "일한 금액의 10%까지", cap = "최대 5만원")
                FinanceTierGuideRow(title = "반영 근무 10일 이상", body = "일한 금액의 20%까지", cap = "최대 15만원")
                FinanceTierGuideRow(title = "반영 근무 20일 이상", body = "일한 금액의 30%까지", cap = "최대 30만원")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun FinanceTierGuideRow(
    title: String,
    body: String,
    cap: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FinanceSurfaceMuted, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = FinanceTextPrimary
            )
            if (cap != null) {
                Text(
                    text = cap,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = FinanceAccent
                )
            }
        }
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = FinanceTextMuted
        )
    }
}

@Composable
private fun FinanceGuideMetricOverview(
    currentLimit: String,
    usedAmount: String,
    availableAmount: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        FinanceGuideMetricRow(label = "현재 한도", value = currentLimit)
        FinanceGuideMetricRow(label = "이번 달 받은 금액", value = usedAmount)
        FinanceGuideMetricRow(label = "남은 추가 신청 가능 금액", value = availableAmount, showDivider = false)
    }
}

@Composable
private fun FinanceGuideMetricRow(
    label: String,
    value: String,
    showDivider: Boolean = true
) {
    val parts = value.split("·", limit = 2).map { it.trim() }
    val primaryValue = parts.firstOrNull().orEmpty()
    val secondaryValue = parts.getOrNull(1)

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = FinanceTextMuted
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = primaryValue,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = FinanceTextPrimary
                )
                if (!secondaryValue.isNullOrBlank()) {
                    Text(
                        text = secondaryValue,
                        style = MaterialTheme.typography.labelMedium,
                        color = FinanceTextMuted
                    )
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(color = FinanceDivider)
        }
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
