package com.dondone.mobile.feature.wage.presentation

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.Icon
import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.designsystem.DawnBorder
import com.dondone.mobile.core.designsystem.DawnPrimary
import com.dondone.mobile.core.designsystem.DawnPrimaryDeep
import com.dondone.mobile.core.designsystem.DawnSuccess
import com.dondone.mobile.core.designsystem.DawnSurfaceAlt
import com.dondone.mobile.core.designsystem.DawnText
import com.dondone.mobile.core.designsystem.DawnTextSubtle
import com.dondone.mobile.core.designsystem.DawnWarning
import com.dondone.mobile.core.designsystem.StatusBadge
import com.dondone.mobile.core.designsystem.pressableScale
import com.dondone.mobile.core.designsystem.rememberDonDoneGrayRipple

private val WageCanvas = Color.White
private val WageDivider = Color(0xFFE8EBF0)
private val WageSurfaceMuted = Color(0xFFF5F6FA)
private val WageInfoBackground = Color(0xFFF8FAFC)
private val WageAccentSoft = Color(0xFFEFF4FF)
private val WageDisabledPrimary = Color(0xFFD8DDE6)
private val WageDisabledText = Color(0xFF9EA7B3)

private enum class WageScreenSection {
    SUMMARY,
    DETAILS,
    ACTIONS
}

private enum class WageFinalCtaType {
    CREATE_EVIDENCE,
    VIEW_HISTORY,
    COMPLETED
}

@Composable
fun WageScreen(
    uiModel: WageUiModel,
    onApplyActualDeposit: (Int) -> Unit,
    onRefresh: () -> Unit,
    onOpenWorkproofPdfCreation: () -> Unit
) {
    val scrollState = rememberScrollState()
    var selectedSection by rememberSaveable { mutableStateOf(WageScreenSection.SUMMARY) }
    val savedDepositInput = uiModel.deposit.actualDepositText.filter(Char::isDigit)
    var actualDepositInput by rememberSaveable(savedDepositInput) { mutableStateOf(savedDepositInput) }
    var appliedDepositInput by rememberSaveable { mutableStateOf<String?>(null) }
    val canStartWageCheck = appliedDepositInput != null &&
        appliedDepositInput == actualDepositInput &&
        !uiModel.deposit.isSubmitting

    LaunchedEffect(uiModel.surfaceState, selectedSection) {
        scrollState.scrollTo(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WageCanvas)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        if (uiModel.surfaceState != WageSurfaceState.CONTENT) {
            WageSurfaceStateCard(
                uiModel = uiModel,
                onRefresh = onRefresh
            )
        } else {
            WageHeader()
            WagePageFrame(
                section = selectedSection,
                canStartWageCheck = canStartWageCheck,
                onSelectSection = { selectedSection = it }
            ) {
                when (selectedSection) {
                    WageScreenSection.SUMMARY -> {
                        WageSummaryPage(
                            uiModel = uiModel,
                            actualDepositInput = actualDepositInput,
                            onActualDepositInputChange = {
                                actualDepositInput = it
                            },
                            onApplyInputCommitted = { appliedInput ->
                                appliedDepositInput = appliedInput
                            },
                            onApplyActualDeposit = onApplyActualDeposit
                        )
                    }

                    WageScreenSection.DETAILS -> {
                        WageDetailsPage(uiModel = uiModel)
                    }

                    WageScreenSection.ACTIONS -> {
                        WageActionsPage(
                            uiModel = uiModel,
                            difference = uiModel.difference,
                            onOpenWorkproofPdfCreation = onOpenWorkproofPdfCreation,
                            onComplete = { selectedSection = WageScreenSection.SUMMARY },
                            onBackToInput = { selectedSection = WageScreenSection.SUMMARY }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WageSurfaceStateCard(
    uiModel: WageUiModel,
    onRefresh: () -> Unit
) {
    WageSurfaceCard {
        WageSectionHeader(title = "급여 점검")
        Text(
            text = uiModel.surfaceMessage ?: "급여 데이터를 확인할 수 없습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = DawnTextSubtle
        )
        if (uiModel.surfaceActionText != null && uiModel.surfaceState != WageSurfaceState.LOADING) {
            WagePrimaryButton(
                text = uiModel.surfaceActionText,
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WageHeader(
) {
    WageSectionSurface {
        WageSectionHeader(title = "급여 점검")
    }
}

@Composable
private fun WageSummaryPage(
    uiModel: WageUiModel,
    actualDepositInput: String,
    onActualDepositInputChange: (String) -> Unit,
    onApplyInputCommitted: (String) -> Unit,
    onApplyActualDeposit: (Int) -> Unit
) {
    WageDepositCard(
        uiModel = uiModel,
        actualDepositInput = actualDepositInput,
        onActualDepositInputChange = onActualDepositInputChange,
        onApplyInputCommitted = onApplyInputCommitted,
        onApplyActualDeposit = onApplyActualDeposit
    )
}

@Composable
private fun WageDetailsPage(uiModel: WageUiModel) {
    WageOverviewCard(uiModel = uiModel)
    WageSectionDivider()
    WageEstimateCard(uiModel = uiModel)
    WageSectionDivider()
    WageEvidenceCard(difference = uiModel.difference)
}

@Composable
private fun WageActionsPage(
    uiModel: WageUiModel,
    difference: WageDifferenceUiModel,
    onOpenWorkproofPdfCreation: () -> Unit,
    onComplete: () -> Unit,
    onBackToInput: () -> Unit
) {
    var showNotice by rememberSaveable { mutableStateOf(false) }
    var hasViewedNotice by rememberSaveable { mutableStateOf(false) }
    val finalCtaType = when (difference.statusText) {
        "부족" -> WageFinalCtaType.CREATE_EVIDENCE
        "초과" -> WageFinalCtaType.VIEW_HISTORY
        else -> WageFinalCtaType.COMPLETED
    }

    WageDifferenceSummaryCard(difference = difference)
    if (!difference.locked) {
        when (finalCtaType) {
            WageFinalCtaType.CREATE_EVIDENCE -> {
                WagePrimaryButton(
                    text = "증빙 만들기",
                    onClick = onOpenWorkproofPdfCreation,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            WageFinalCtaType.VIEW_HISTORY -> Unit

            WageFinalCtaType.COMPLETED -> {
                WagePrimaryButton(
                    text = "확인 완료",
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    if (!difference.locked && finalCtaType == WageFinalCtaType.VIEW_HISTORY) {
        WageNoticeDisclosure(
            uiModel = uiModel,
            difference = difference,
            expanded = showNotice,
            onToggle = {
                val nextExpanded = !showNotice
                showNotice = nextExpanded
                if (nextExpanded) hasViewedNotice = true
            }
        )
        if (hasViewedNotice) {
            WagePrimaryButton(
                text = "확인 완료",
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth()
            )
        }
        WageInlineTextButton(
            text = "입금액 다시 입력",
            onClick = onBackToInput,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun WageNoticeDisclosure(
    uiModel: WageUiModel,
    difference: WageDifferenceUiModel,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WageSurfaceMuted)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberDonDoneGrayRipple(),
                onClick = onToggle
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "계산 근거",
                style = MaterialTheme.typography.titleMedium,
                color = DawnText
            )
            Text(
                text = if (expanded) "내역 닫기" else "내역 확인하기",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = DawnPrimaryDeep
            )
        }

        if (expanded) {
            WageOverviewCard(uiModel = uiModel)
            WageEstimateCard(uiModel = uiModel)
            WageEvidenceCard(difference = difference)
        }
    }
}

@Composable
private fun WageDepositCard(
    uiModel: WageUiModel,
    actualDepositInput: String,
    onActualDepositInputChange: (String) -> Unit,
    onApplyInputCommitted: (String) -> Unit,
    onApplyActualDeposit: (Int) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val actualDepositValue = actualDepositInput.toIntOrNull()
    val hasActualDepositInput = actualDepositInput.isNotBlank()
    val canAdjustDeposit = hasActualDepositInput && !uiModel.deposit.isSubmitting
    val canApplyDeposit = actualDepositValue != null && actualDepositValue > 0 && !uiModel.deposit.isSubmitting

    WageSurfaceCard {
        Text(
            text = uiModel.descriptionText,
            style = MaterialTheme.typography.bodyMedium,
            color = DawnTextSubtle
        )

        if (
            uiModel.deposit.headerText.isNotBlank() ||
            uiModel.deposit.descriptionText.isNotBlank() ||
            uiModel.deposit.metaText.isNotBlank() ||
            uiModel.deposit.statusText.isNotBlank()
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
                    if (uiModel.deposit.headerText.isNotBlank()) {
                        Text(
                            text = uiModel.deposit.headerText,
                            style = MaterialTheme.typography.titleMedium,
                            color = DawnText
                        )
                    }
                    if (uiModel.deposit.descriptionText.isNotBlank()) {
                        Text(
                            text = uiModel.deposit.descriptionText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DawnTextSubtle
                        )
                    }
                    if (uiModel.deposit.metaText.isNotBlank()) {
                        Text(
                            text = uiModel.deposit.metaText,
                            style = MaterialTheme.typography.labelMedium,
                            color = DawnTextSubtle
                        )
                    }
                }
                if (uiModel.deposit.statusText.isNotBlank()) {
                    StatusBadge(
                        text = uiModel.deposit.statusText,
                        tone = uiModel.deposit.statusTone
                    )
                }
            }
        }

        WageInfoPanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    value = actualDepositInput,
                    onValueChange = { value ->
                        onActualDepositInputChange(value.filter(Char::isDigit))
                    },
                    enabled = !uiModel.deposit.isSubmitting,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = {
                        Text(
                            text = "실수령 금액 입력",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DawnTextSubtle
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = DawnText
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        disabledContainerColor = Color(0xFFF8FAFC),
                        focusedBorderColor = DawnPrimary,
                        unfocusedBorderColor = DawnBorder,
                        disabledBorderColor = DawnBorder,
                        focusedTextColor = DawnText,
                        unfocusedTextColor = DawnText,
                        cursorColor = DawnPrimary
                    )
                )
                WageCompactButton(
                    text = "적용",
                    onClick = {
                        if (actualDepositValue != null && actualDepositValue > 0) {
                            onApplyActualDeposit(actualDepositValue)
                            onApplyInputCommitted(actualDepositInput)
                        }
                    },
                    enabled = canApplyDeposit
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WageSecondaryButton(
                    text = "-5만",
                    onClick = {
                        onActualDepositInputChange(
                            actualDepositValue
                                ?.minus(50_000)
                                ?.coerceAtLeast(0)
                                ?.toString()
                                ?: actualDepositInput
                        )
                    },
                    enabled = canAdjustDeposit,
                    modifier = Modifier.weight(1f)
                )
                WageSecondaryButton(
                    text = "+5만",
                    onClick = {
                        onActualDepositInputChange(
                            actualDepositValue
                                ?.plus(50_000)
                                ?.toString()
                                ?: actualDepositInput
                        )
                    },
                    enabled = canAdjustDeposit,
                    modifier = Modifier.weight(1f)
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WageMiniBadge(text = uiModel.deposit.deductionBadgeText)
                Text(
                    text = uiModel.deposit.thresholdBadgeText,
                    style = MaterialTheme.typography.labelLarge,
                    color = DawnTextSubtle
                )
            }
        }
    }
}

@Composable
private fun WageOverviewCard(uiModel: WageUiModel) {
    WageSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "월간 요약",
                style = MaterialTheme.typography.titleMedium,
                color = DawnText
            )
            Text(
                text = "근거 자료 기반",
                style = MaterialTheme.typography.labelLarge,
                color = DawnTextSubtle
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            uiModel.overviewItems.forEach { item ->
                WageMetricTile(
                    label = item.label,
                    value = item.value,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WageEstimateCard(uiModel: WageUiModel) {
    WageSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "참고용 추정 급여",
                style = MaterialTheme.typography.titleMedium,
                color = DawnText
            )
            Text(
                text = "근거 자료 기반",
                style = MaterialTheme.typography.labelLarge,
                color = DawnTextSubtle
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            uiModel.estimateItems.forEach { item ->
                WageValueRow(
                    label = item.label,
                    value = item.value,
                    emphasized = item.emphasized,
                    icon = item.icon?.toImageVector()
                )
            }
        }
    }
}

@Composable
private fun WageDifferenceSummaryCard(
    difference: WageDifferenceUiModel,
) {
    val titleColor = when (difference.statusTone) {
        BadgeTone.Warning -> DawnWarning
        BadgeTone.Info -> DawnPrimaryDeep
        BadgeTone.Success -> DawnSuccess
    }

    WageSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = difference.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = titleColor
                )
                Text(
                    text = difference.descriptionText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DawnTextSubtle
                )
            }
        }

        WageSummaryList(
            items = difference.summaryItems,
            accentColor = if (difference.statusTone == BadgeTone.Warning) DawnWarning else DawnPrimaryDeep
        )
    }
}

@Composable
private fun WageEvidenceCard(
    difference: WageDifferenceUiModel
) {
    WageSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "확인한 근거",
                style = MaterialTheme.typography.labelLarge,
                color = DawnText
            )
            difference.evidenceLines.forEach { line ->
                WageEvidenceLine(text = line)
            }
        }
    }
}

@Composable
private fun WagePageFrame(
    section: WageScreenSection,
    canStartWageCheck: Boolean,
    onSelectSection: (WageScreenSection) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            content = content
        )
        if (section == WageScreenSection.DETAILS) {
            HorizontalDivider(color = WageDivider)
        }
        if (section != WageScreenSection.ACTIONS) {
            WagePageNavigation(
                section = section,
                canStartWageCheck = canStartWageCheck,
                onSelectSection = onSelectSection
            )
        }
    }
}

@Composable
private fun WagePageNavigation(
    section: WageScreenSection,
    canStartWageCheck: Boolean,
    onSelectSection: (WageScreenSection) -> Unit
) {
    val previous = section.previous()
    val next = section.next()

    if (section == WageScreenSection.SUMMARY) {
        Button(
            onClick = { onSelectSection(WageScreenSection.ACTIONS) },
            enabled = canStartWageCheck,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DawnPrimary,
                disabledContainerColor = WageDisabledPrimary,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = "급여 점검 하기",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black)
            )
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = { previous?.let(onSelectSection) },
            enabled = previous != null,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, DawnBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = WageSurfaceMuted,
                contentColor = DawnText,
                disabledContainerColor = WageSurfaceMuted,
                disabledContentColor = WageDisabledText
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = previous?.navigationLabel() ?: "이전 없음",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        Button(
            onClick = { next?.let(onSelectSection) },
            enabled = next != null,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DawnPrimary,
                disabledContainerColor = WageDisabledPrimary,
                contentColor = Color.White,
                disabledContentColor = WageDisabledText
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = next?.navigationLabel() ?: "마지막 단계",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

private fun WageScreenSection.previous(): WageScreenSection? {
    val index = WageScreenSection.entries.indexOf(this)
    return WageScreenSection.entries.getOrNull(index - 1)
}

private fun WageScreenSection.next(): WageScreenSection? {
    val index = WageScreenSection.entries.indexOf(this)
    return WageScreenSection.entries.getOrNull(index + 1)
}

private fun WageScreenSection.navigationLabel(): String = when (this) {
    WageScreenSection.SUMMARY -> "결과 확인 보기"
    WageScreenSection.DETAILS -> "계산 근거 보기"
    WageScreenSection.ACTIONS -> "다음 행동 보기"
}

@Composable
private fun WageSurfaceCard(
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
private fun WageSectionHeader(
    title: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = DawnText
    )
}

@Composable
private fun WageSectionSurface(
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
private fun WageSectionDivider() {
    Spacer(modifier = Modifier.height(14.dp))
    HorizontalDivider(color = WageDivider)
    Spacer(modifier = Modifier.height(14.dp))
}

@Composable
private fun WageInfoPanel(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(WageInfoBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun WageMiniBadge(
    text: String,
    accent: Color = DawnPrimaryDeep,
    background: Color = DawnSurfaceAlt
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = accent
        )
    }
}

@Composable
private fun WageMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(WageSurfaceMuted)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = DawnTextSubtle
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = DawnText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun WageValueRow(
    label: String,
    value: String,
    emphasized: Boolean,
    icon: ImageVector? = null
) {
    val background = if (emphasized) WageAccentSoft else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (emphasized) Color.White else WageSurfaceMuted),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (emphasized) DawnPrimaryDeep else DawnTextSubtle,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (emphasized) DawnText else DawnTextSubtle
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (emphasized) FontWeight.Black else FontWeight.Bold
            ),
            color = DawnText
        )
    }
}

@Composable
private fun WageSummaryList(
    items: List<WageMetricItemUiModel>,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = DawnTextSubtle
                )
                Text(
                    text = item.value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = if (item.emphasized) accentColor else DawnText,
                    maxLines = 1
                )
            }
            if (index != items.lastIndex) {
                HorizontalDivider(color = DawnBorder)
            }
        }
    }
}

@Composable
private fun WageEvidenceLine(
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(DawnPrimaryDeep)
        )
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = DawnText
        )
    }
}

@Composable
private fun WagePrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.foundation.LocalIndication provides rememberDonDoneGrayRipple()
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.pressableScale(
                interactionSource = interactionSource,
                enabled = enabled
            ),
            interactionSource = interactionSource,
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DawnPrimary,
                contentColor = Color.White,
                disabledContainerColor = WageDisabledPrimary,
                disabledContentColor = WageDisabledText
            )
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black)
            )
        }
    }
}

@Composable
private fun WageSecondaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.foundation.LocalIndication provides rememberDonDoneGrayRipple()
    ) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.pressableScale(
                interactionSource = interactionSource,
                enabled = enabled
            ),
            interactionSource = interactionSource,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, DawnBorder),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = WageSurfaceMuted,
                contentColor = DawnText,
                disabledContainerColor = WageSurfaceMuted,
                disabledContentColor = WageDisabledText
            )
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black)
            )
        }
    }
}

@Composable
private fun WageCompactButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.foundation.LocalIndication provides rememberDonDoneGrayRipple()
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interactionSource,
            modifier = Modifier.pressableScale(
                interactionSource = interactionSource,
                enabled = enabled
            ),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DawnPrimary,
                contentColor = Color.White
            )
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
            )
        }
    }
}

@Composable
private fun WageInlineTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.foundation.LocalIndication provides rememberDonDoneGrayRipple()
    ) {
        TextButton(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier = modifier
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = DawnPrimaryDeep
            )
        }
    }
}

private fun WageMetricIcon.toImageVector(): ImageVector = when (this) {
    WageMetricIcon.BASE -> Icons.Default.Description
    WageMetricIcon.OVERTIME -> Icons.AutoMirrored.Filled.TrendingUp
    WageMetricIcon.NIGHT -> Icons.Default.NightsStay
    WageMetricIcon.TOTAL -> Icons.Default.AutoAwesome
}
