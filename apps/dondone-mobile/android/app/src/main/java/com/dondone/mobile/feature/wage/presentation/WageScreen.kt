package com.dondone.mobile.feature.wage.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.dondone.mobile.core.designsystem.DawnWarning
import com.dondone.mobile.core.designsystem.StatusBadge

@Composable
fun WageScreen(
    uiModel: WageUiModel,
    onRecordDeposit: () -> Unit,
    onIncreaseDeposit: () -> Unit,
    onDecreaseDeposit: () -> Unit,
    onOpenTransfer: () -> Unit,
    onOpenWorkproof: () -> Unit,
    onOpenMenu: () -> Unit
) {
    var showSecondaryActions by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        WageIntroCard(uiModel = uiModel)

        WageCheckCard(
            uiModel = uiModel,
            onRecordDeposit = onRecordDeposit,
            onIncreaseDeposit = onIncreaseDeposit,
            onDecreaseDeposit = onDecreaseDeposit
        )

        WageDifferenceCard(
            difference = uiModel.difference,
            showSecondaryActions = showSecondaryActions,
            onToggleSecondaryActions = { showSecondaryActions = !showSecondaryActions },
            onPrimaryAction = resolveAction(
                target = uiModel.difference.primaryActionTarget,
                onOpenTransfer = onOpenTransfer,
                onOpenWorkproof = onOpenWorkproof,
                onOpenMenu = onOpenMenu
            ),
            onSecondaryAction = { target ->
                resolveAction(
                    target = target,
                    onOpenTransfer = onOpenTransfer,
                    onOpenWorkproof = onOpenWorkproof,
                    onOpenMenu = onOpenMenu
                ).invoke()
            }
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun WageIntroCard(
    uiModel: WageUiModel
) {
    WageSurfaceCard(hero = true) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "급여 점검",
                style = MaterialTheme.typography.labelMedium,
                color = DawnPrimaryDeep
            )
            Text(
                text = uiModel.heroTitleText,
                style = MaterialTheme.typography.titleLarge,
                color = DawnText
            )
            Text(
                text = uiModel.heroDescriptionText,
                style = MaterialTheme.typography.bodyMedium,
                color = DawnTextSubtle
            )
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uiModel.chips.forEach { chip ->
                WageChip(text = chip)
            }
        }

        WageInfoPanel {
            uiModel.disclaimerLines.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DawnText
                )
            }
        }
    }
}

@Composable
private fun WageCheckCard(
    uiModel: WageUiModel,
    onRecordDeposit: () -> Unit,
    onIncreaseDeposit: () -> Unit,
    onDecreaseDeposit: () -> Unit
) {
    WageSurfaceCard {
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
                    text = uiModel.monthText,
                    style = MaterialTheme.typography.labelMedium,
                    color = DawnPrimaryDeep
                )
                Text(
                    text = uiModel.titleText,
                    style = MaterialTheme.typography.titleLarge,
                    color = DawnText
                )
                Text(
                    text = uiModel.descriptionText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DawnTextSubtle
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WageMiniBadge(text = uiModel.modifiedCountText)
                WageMiniBadge(
                    text = uiModel.evidenceBadgeText,
                    accent = DawnSuccess,
                    background = Color(0xFFEFFAF4)
                )
            }
        }

        WageInfoPanel {
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
                        text = "급여일 체크",
                        style = MaterialTheme.typography.labelMedium,
                        color = DawnPrimaryDeep
                    )
                    Text(
                        text = uiModel.deposit.headerText,
                        style = MaterialTheme.typography.titleMedium,
                        color = DawnText
                    )
                    Text(
                        text = uiModel.deposit.descriptionText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DawnTextSubtle
                    )
                }
                if (uiModel.deposit.actionButtonText != null) {
                    WageCompactButton(
                        text = uiModel.deposit.actionButtonText,
                        onClick = onRecordDeposit
                    )
                } else {
                    StatusBadge(
                        text = uiModel.deposit.statusText,
                        tone = uiModel.deposit.statusTone
                    )
                }
            }
            Text(
                text = uiModel.deposit.metaText,
                style = MaterialTheme.typography.labelMedium,
                color = DawnTextSubtle
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "실제 입금액(실수령)",
                style = MaterialTheme.typography.labelLarge,
                color = DawnTextSubtle
            )
            Text(
                text = uiModel.deposit.actualDepositText,
                style = MaterialTheme.typography.headlineSmall,
                color = DawnText
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WageSecondaryButton(
                    text = "-5만",
                    onClick = onDecreaseDeposit,
                    modifier = Modifier.weight(1f)
                )
                WageSecondaryButton(
                    text = "+5만",
                    onClick = onIncreaseDeposit,
                    modifier = Modifier.weight(1f)
                )
                if (uiModel.deposit.actionButtonText != null) {
                    WagePrimaryButton(
                        text = uiModel.deposit.actionButtonText,
                        onClick = onRecordDeposit,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Text(
                text = uiModel.deposit.inputHelperText,
                style = MaterialTheme.typography.bodyMedium,
                color = DawnTextSubtle
            )
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

        HorizontalDivider(color = DawnBorder)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    text = "근거 자료(WorkProof) 기반",
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

        HorizontalDivider(color = DawnBorder)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    text = "근거 자료(WorkProof) 기반",
                    style = MaterialTheme.typography.labelLarge,
                    color = DawnTextSubtle
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                uiModel.estimateItems.forEach { item ->
                    WageValueRow(
                        label = item.label,
                        value = item.value,
                        emphasized = item.emphasized
                    )
                }
            }
        }
    }
}

@Composable
private fun WageDifferenceCard(
    difference: WageDifferenceUiModel,
    showSecondaryActions: Boolean,
    onToggleSecondaryActions: () -> Unit,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: (WageActionTarget) -> Unit
) {
    val accent = when (difference.statusTone) {
        BadgeTone.Warning -> DawnWarning
        BadgeTone.Success -> DawnSuccess
        BadgeTone.Info -> DawnPrimaryDeep
    }
    val soft = when (difference.statusTone) {
        BadgeTone.Warning -> Color(0xFFFFF6ED)
        BadgeTone.Success -> Color(0xFFEFFAF4)
        BadgeTone.Info -> DawnSurfaceAlt
    }

    WageSurfaceCard(
        backgroundBrush = Brush.verticalGradient(
            colors = listOf(
                Color.White,
                soft.copy(alpha = 0.45f)
            )
        ),
        borderColor = soft
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
                    text = "REVIEW DIFF",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent
                )
                Text(
                    text = difference.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = DawnText
                )
                Text(
                    text = difference.descriptionText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DawnTextSubtle
                )
            }
            StatusBadge(
                text = difference.statusText,
                tone = difference.statusTone
            )
        }

        WageSummaryList(
            items = difference.summaryItems,
            accentColor = if (difference.statusTone == BadgeTone.Warning) DawnWarning else DawnPrimaryDeep
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "함께 확인된 근거",
                style = MaterialTheme.typography.labelLarge,
                color = DawnText
            )
            difference.evidenceLines.forEach { line ->
                WageEvidenceLine(text = line)
            }
        }

        if (difference.locked) {
            WageDashedPanel {
                Text(
                    text = difference.lockedTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = DawnText
                )
                Text(
                    text = difference.lockedDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DawnTextSubtle
                )
            }
        } else {
            WageInfoPanel {
                Text(
                    text = "추천 경로(선택)",
                    style = MaterialTheme.typography.labelLarge,
                    color = DawnText
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    difference.steps.forEach { step ->
                        WageStepItem(step = step)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.94f))
                    .border(BorderStroke(1.dp, DawnBorder), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            text = "다음 추천 행동",
                            style = MaterialTheme.typography.labelMedium,
                            color = DawnPrimaryDeep
                        )
                        Text(
                            text = difference.primaryActionDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DawnText
                        )
                    }
                    WageMiniBadge(text = difference.primaryActionStepText)
                }
                WagePrimaryButton(
                    text = difference.primaryActionButtonText,
                    onClick = onPrimaryAction,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = difference.primaryHintText,
                    style = MaterialTheme.typography.labelMedium,
                    color = DawnTextSubtle
                )
            }

            WageSecondaryButton(
                text = if (showSecondaryActions) "다른 액션 숨기기" else difference.secondaryActionsToggleText,
                onClick = onToggleSecondaryActions,
                modifier = Modifier.fillMaxWidth()
            )

            if (showSecondaryActions) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    difference.secondaryActions.forEach { action ->
                        WageSecondaryActionButton(
                            text = action.label,
                            onClick = { onSecondaryAction(action.target) }
                        )
                    }
                }
            }
        }

        Text(
            text = difference.disclaimerText,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelMedium,
            color = DawnTextSubtle
        )
    }
}

@Composable
private fun WageSurfaceCard(
    hero: Boolean = false,
    backgroundBrush: Brush? = null,
    borderColor: Color = DawnBorder,
    content: @Composable ColumnScope.() -> Unit
) {
    val brush = backgroundBrush ?: if (hero) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White,
                Color(0xFFF8F4FF)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White,
                Color(0xFFFCFAFF)
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = DawnSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content
            )
        }
    }
}

@Composable
private fun WageInfoPanel(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFF8FAFC))
            .border(BorderStroke(1.dp, DawnBorder), RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun WageDashedPanel(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF8FAFC))
            .border(BorderStroke(1.dp, DawnBorder), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content
    )
}

@Composable
private fun WageChip(
    text: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.9f))
            .border(BorderStroke(1.dp, DawnBorder), RoundedCornerShape(999.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = DawnTextSubtle
        )
    }
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
            .border(BorderStroke(1.dp, DawnBorder), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
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
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF8FAFC))
            .border(BorderStroke(1.dp, DawnBorder), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp),
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
    emphasized: Boolean
) {
    val background = if (emphasized) Color.White else Color(0xFFF8FAFC)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .border(BorderStroke(1.dp, DawnBorder), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (emphasized) DawnText else DawnTextSubtle
        )
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
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF8FAFC))
            .border(BorderStroke(1.dp, DawnBorder), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
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
private fun WageStepItem(
    step: WageDifferenceStepUiModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(BorderStroke(1.dp, DawnBorder), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(DawnSurfaceAlt),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = step.step,
                    style = MaterialTheme.typography.labelMedium,
                    color = DawnPrimaryDeep
                )
            }
            Text(
                text = step.label,
                style = MaterialTheme.typography.labelLarge,
                color = DawnText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        WageMiniBadge(text = step.status)
    }
}

@Composable
private fun WagePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DawnPrimary,
            contentColor = Color.White
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black)
        )
    }
}

@Composable
private fun WageSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, DawnBorder),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFFF8FAFC),
            contentColor = DawnText
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black)
        )
    }
}

@Composable
private fun WageCompactButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
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

@Composable
private fun WageSecondaryActionButton(
    text: String,
    onClick: () -> Unit
) {
    WageSecondaryButton(
        text = text,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun resolveAction(
    target: WageActionTarget,
    onOpenTransfer: () -> Unit,
    onOpenWorkproof: () -> Unit,
    onOpenMenu: () -> Unit
): () -> Unit = when (target) {
    WageActionTarget.TRANSFER -> onOpenTransfer
    WageActionTarget.WORKPROOF -> onOpenWorkproof
    WageActionTarget.MENU -> onOpenMenu
}
