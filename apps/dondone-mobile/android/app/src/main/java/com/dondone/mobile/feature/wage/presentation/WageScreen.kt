package com.dondone.mobile.feature.wage.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.dondone.mobile.core.i18n.LocalAppLanguage
import com.dondone.mobile.core.i18n.text

private val WageCanvas = Color.White
private val WageDivider = Color(0xFFE8EBF0)
private val WageMuted = Color(0xFFF5F6FA)
private val WageInfo = Color(0xFFF8FAFC)
private val WageAccent = Color(0xFFEFF4FF)
private val WageDisabled = Color(0xFFD8DDE6)
private val WageDisabledText = Color(0xFF9EA7B3)

private enum class WageTab { SUMMARY, DETAILS, ACTIONS }

@Composable
fun WageScreen(
    uiModel: WageUiModel,
    onApplyActualDeposit: (Int) -> Unit,
    onRefresh: () -> Unit,
    onNavigateMenu: (openWorkerRegistrationSheet: Boolean) -> Unit,
    onOpenWorkproofPdfCreation: () -> Unit
) {
    val language = LocalAppLanguage.current
    val scroll = rememberScrollState()
    var tab by rememberSaveable { mutableStateOf(WageTab.SUMMARY) }
    val initialDigits = uiModel.deposit.actualDepositText.filter(Char::isDigit)
    var depositInput by rememberSaveable(initialDigits) { mutableStateOf(initialDigits) }
    var appliedInput by rememberSaveable { mutableStateOf<String?>(null) }
    val canStart = appliedInput != null && appliedInput == depositInput && !uiModel.deposit.isSubmitting

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WageCanvas)
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(language.text("wage_check_title"), style = MaterialTheme.typography.titleLarge, color = DawnText)

        if (uiModel.surfaceState != WageSurfaceState.CONTENT) {
            Panel {
                Text(uiModel.surfaceMessage ?: language.text("wage_error_message"), color = DawnTextSubtle)
                if (uiModel.surfaceActionText != null && uiModel.surfaceState != WageSurfaceState.LOADING) {
                    PrimaryButton(
                        text = uiModel.surfaceActionText,
                        onClick = when (uiModel.surfaceActionType) {
                            WageSurfaceActionType.OPEN_MENU -> { { onNavigateMenu(false) } }
                            WageSurfaceActionType.OPEN_MENU_AND_REGISTRATION_CODE -> { { onNavigateMenu(true) } }
                            else -> onRefresh
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            return@Column
        }

        when (tab) {
            WageTab.SUMMARY -> SummaryTab(
                uiModel = uiModel,
                depositInput = depositInput,
                onDepositInputChange = { depositInput = it.filter(Char::isDigit) },
                onApply = {
                    depositInput.toIntOrNull()?.takeIf { it > 0 }?.let {
                        onApplyActualDeposit(it)
                        appliedInput = depositInput
                    }
                }
            )
            WageTab.DETAILS -> DetailsTab(uiModel)
            WageTab.ACTIONS -> ActionsTab(uiModel, onOpenWorkproofPdfCreation) { tab = WageTab.SUMMARY }
        }

        if (tab != WageTab.ACTIONS) {
            if (tab == WageTab.SUMMARY) {
                PrimaryButton(language.text("start_wage_check"), { tab = WageTab.ACTIONS }, Modifier.fillMaxWidth(), canStart)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondaryButton(language.text("go_to_summary"), { tab = WageTab.SUMMARY }, Modifier.weight(1f))
                    PrimaryButton(language.text("go_to_actions"), { tab = WageTab.ACTIONS }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SummaryTab(
    uiModel: WageUiModel,
    depositInput: String,
    onDepositInputChange: (String) -> Unit,
    onApply: () -> Unit
) {
    val language = LocalAppLanguage.current
    val depositValue = depositInput.toIntOrNull()
    val canAdjust = depositInput.isNotBlank() && !uiModel.deposit.isSubmitting
    val canApply = depositValue != null && depositValue > 0 && !uiModel.deposit.isSubmitting
    Panel {
        Text(uiModel.descriptionText, color = DawnTextSubtle)
        if (uiModel.deposit.headerText.isNotBlank() || uiModel.deposit.descriptionText.isNotBlank() || uiModel.deposit.metaText.isNotBlank() || uiModel.deposit.statusText.isNotBlank()) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (uiModel.deposit.headerText.isNotBlank()) Text(uiModel.deposit.headerText, style = MaterialTheme.typography.titleMedium, color = DawnText)
                    if (uiModel.deposit.descriptionText.isNotBlank()) Text(uiModel.deposit.descriptionText, color = DawnTextSubtle)
                    if (uiModel.deposit.metaText.isNotBlank()) Text(uiModel.deposit.metaText, style = MaterialTheme.typography.labelMedium, color = DawnTextSubtle)
                }
                if (uiModel.deposit.statusText.isNotBlank()) StatusBadge(text = uiModel.deposit.statusText, tone = uiModel.deposit.statusTone)
            }
        }
        Box(Modifier.fillMaxWidth().background(WageInfo, RoundedCornerShape(18.dp)).padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = depositInput,
                        onValueChange = onDepositInputChange,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text(language.text("input_actual_deposit_placeholder"), color = DawnTextSubtle) },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = WageInfo, unfocusedContainerColor = WageInfo, focusedBorderColor = DawnPrimary, unfocusedBorderColor = DawnBorder)
                    )
                    PrimaryButton(language.text("apply"), onApply, enabled = canApply)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondaryButton(language.text("minus_50k"), { onDepositInputChange(((depositValue ?: 0) - 50_000).coerceAtLeast(0).toString()) }, Modifier.weight(1f), canAdjust)
                    SecondaryButton(language.text("plus_50k"), { onDepositInputChange(((depositValue ?: 0) + 50_000).toString()) }, Modifier.weight(1f), canAdjust)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Badge(uiModel.deposit.deductionBadgeText)
                    if (uiModel.deposit.thresholdBadgeText.isNotBlank()) Text(uiModel.deposit.thresholdBadgeText, color = DawnTextSubtle)
                }
            }
        }
    }
}

@Composable
private fun DetailsTab(uiModel: WageUiModel) {
    val language = LocalAppLanguage.current
    Panel {
        HeaderPair(language.text("work_days"), language.text("based_on_work_records"))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            uiModel.overviewItems.forEach { item -> MetricCard(item, Modifier.weight(1f)) }
        }
    }
    HorizontalDivider(color = WageDivider)
    Panel {
        HeaderPair(language.text("estimate_breakdown"), language.text("based_on_work_records"))
        uiModel.estimateItems.forEach { item -> ValueRow(item) }
    }
    HorizontalDivider(color = WageDivider)
    Panel {
        Text(language.text("evidence_summary"), style = MaterialTheme.typography.labelLarge, color = DawnText)
        uiModel.difference.evidenceLines.forEach { line -> Text("- $line", color = DawnText) }
    }
}

@Composable
private fun ActionsTab(
    uiModel: WageUiModel,
    onOpenWorkproofPdfCreation: () -> Unit,
    onComplete: () -> Unit
) {
    val language = LocalAppLanguage.current
    val diff = uiModel.difference
    val accent = when (diff.statusTone) {
        BadgeTone.Warning -> DawnWarning
        BadgeTone.Info -> DawnPrimaryDeep
        BadgeTone.Success -> DawnSuccess
    }
    Panel {
        Text(diff.title, style = MaterialTheme.typography.titleLarge, color = accent)
        Text(diff.descriptionText, color = DawnTextSubtle)
        diff.summaryItems.forEach { item ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.label, color = DawnTextSubtle)
                Text(item.value, color = if (item.emphasized) accent else DawnText, fontWeight = FontWeight.Black)
            }
            HorizontalDivider(color = DawnBorder)
        }
    }
    if (!diff.locked) {
        when (diff.state) {
            WageDifferenceState.UNDER -> PrimaryButton(language.text("create_evidence_document"), onOpenWorkproofPdfCreation, Modifier.fillMaxWidth())
            WageDifferenceState.OVER, WageDifferenceState.MATCH -> PrimaryButton(language.text("completed"), onComplete, Modifier.fillMaxWidth())
            WageDifferenceState.PENDING -> Unit
        }
    }
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}

@Composable private fun HeaderPair(title: String, caption: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(title, style = MaterialTheme.typography.titleMedium, color = DawnText); Text(caption, color = DawnTextSubtle) } }
@Composable private fun Badge(text: String) { Box(Modifier.background(DawnSurfaceAlt, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) { Text(text, color = DawnPrimaryDeep) } }

@Composable
private fun MetricCard(item: WageMetricItemUiModel, modifier: Modifier = Modifier) {
    Column(modifier.background(WageMuted, RoundedCornerShape(16.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(item.label, style = MaterialTheme.typography.labelMedium, color = DawnTextSubtle)
        Text(item.value, style = MaterialTheme.typography.bodyLarge, color = DawnText, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ValueRow(item: WageMetricItemUiModel) {
    val icon = when (item.icon) {
        WageMetricIcon.BASE -> Icons.Default.Description
        WageMetricIcon.OVERTIME -> Icons.AutoMirrored.Filled.TrendingUp
        WageMetricIcon.NIGHT -> Icons.Default.NightsStay
        WageMetricIcon.TOTAL -> Icons.Default.AutoAwesome
        null -> null
    }
    Row(
        modifier = Modifier.fillMaxWidth().background(if (item.emphasized) WageAccent else Color.Transparent, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            icon?.let { Icon(it, null, tint = if (item.emphasized) DawnPrimaryDeep else DawnTextSubtle) }
            Text(item.label, color = if (item.emphasized) DawnText else DawnTextSubtle)
        }
        Text(item.value, fontWeight = FontWeight.Black, color = DawnText)
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier, shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), colors = ButtonDefaults.buttonColors(containerColor = DawnPrimary, disabledContainerColor = WageDisabled, disabledContentColor = WageDisabledText)) {
        Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black))
    }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = WageMuted, disabledContainerColor = WageMuted, disabledContentColor = WageDisabledText)) {
        Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black))
    }
}
