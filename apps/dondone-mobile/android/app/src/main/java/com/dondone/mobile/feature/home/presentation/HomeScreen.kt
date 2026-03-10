package com.dondone.mobile.feature.home.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dondone.mobile.core.designsystem.DawnBorder
import com.dondone.mobile.core.designsystem.DawnPrimary
import com.dondone.mobile.core.designsystem.DawnPrimaryDeep
import com.dondone.mobile.core.designsystem.DawnSecondary
import com.dondone.mobile.core.designsystem.DawnSurface
import com.dondone.mobile.core.designsystem.DawnSurfaceAlt
import com.dondone.mobile.core.designsystem.DawnText
import com.dondone.mobile.core.designsystem.DawnTextSubtle
import com.dondone.mobile.core.designsystem.DawnWarning
import com.dondone.mobile.core.designsystem.DonDoneProgressBar
import com.dondone.mobile.core.designsystem.StatusBadge

@Composable
fun HomeScreen(
    uiModel: HomeUiModel,
    onOpenTransfer: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenFinance: () -> Unit,
    onOpenWage: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenWorkproof: () -> Unit,
    onClockIn: () -> Unit,
    onClockOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AccountSummaryCard(
            uiModel = uiModel,
            onOpenAccount = onOpenAccount,
            onOpenTransfer = onOpenTransfer
        )

        TodayWorkCard(
            uiModel = uiModel,
            onOpenWorkproof = onOpenWorkproof,
            onClockIn = onClockIn,
            onClockOut = onClockOut
        )

        SettlementCard(
            uiModel = uiModel,
            onOpenWage = onOpenWage,
            onOpenFinance = onOpenFinance,
            onOpenMenu = onOpenMenu
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun AccountSummaryCard(
    uiModel: HomeUiModel,
    onOpenAccount: () -> Unit,
    onOpenTransfer: () -> Unit
) {
    HomeSurfaceCard(hero = true) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "계좌",
                style = MaterialTheme.typography.labelMedium,
                color = DawnPrimaryDeep
            )
            Text(
                text = "내 계좌",
                style = MaterialTheme.typography.titleLarge,
                color = DawnText
            )
        }

        Text(
            text = uiModel.account.balanceText,
            style = MaterialTheme.typography.displaySmall,
            color = DawnText
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeMetricTile(
                label = "송금 가능",
                value = uiModel.account.sendableAmountText,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            HomeMetricTile(
                label = "선택 계좌",
                value = uiModel.account.selectedAccountText,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HomePrimaryButton(
                text = "계좌 관리",
                onClick = onOpenAccount,
                modifier = Modifier.weight(1f)
            )
            HomeSecondaryButton(
                text = "송금하기",
                onClick = onOpenTransfer,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = uiModel.account.hintText,
            style = MaterialTheme.typography.bodyMedium,
            color = DawnTextSubtle
        )
    }
}

@Composable
private fun TodayWorkCard(
    uiModel: HomeUiModel,
    onOpenWorkproof: () -> Unit,
    onClockIn: () -> Unit,
    onClockOut: () -> Unit
) {
    HomeSurfaceCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                        text = "오늘 근무",
                        style = MaterialTheme.typography.labelMedium,
                        color = DawnPrimaryDeep
                    )
                    Text(
                        text = "오늘 출퇴근",
                        style = MaterialTheme.typography.titleLarge,
                        color = DawnText
                    )
                }
                HomeLinkAction(
                    text = "기록 보기",
                    onClick = onOpenWorkproof
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiModel.work.dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DawnTextSubtle,
                    maxLines = 1
                )
                StatusBadge(
                    text = uiModel.work.statusText,
                    tone = uiModel.work.statusTone
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HomePrimaryButton(
                text = "출근",
                onClick = onClockIn,
                enabled = uiModel.work.canClockIn,
                modifier = Modifier.weight(1f)
            )
            HomeSoftButton(
                text = "퇴근",
                onClick = onClockOut,
                enabled = uiModel.work.canClockOut,
                modifier = Modifier.weight(1f)
            )
        }

        HomeInfoPanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "출근",
                        style = MaterialTheme.typography.labelMedium,
                        color = DawnTextSubtle
                    )
                    Text(
                        text = uiModel.work.clockInText,
                        style = MaterialTheme.typography.labelLarge,
                        color = DawnText
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "퇴근",
                        style = MaterialTheme.typography.labelMedium,
                        color = DawnTextSubtle
                    )
                    Text(
                        text = uiModel.work.clockOutText,
                        style = MaterialTheme.typography.labelLarge,
                        color = DawnText
                    )
                }
            }
            Text(
                text = uiModel.work.impactText,
                style = MaterialTheme.typography.bodyMedium,
                color = DawnPrimaryDeep
            )
        }

        HorizontalDivider(color = DawnBorder)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "미리받기 진행도",
                style = MaterialTheme.typography.labelMedium,
                color = DawnPrimaryDeep
            )
            Text(
                text = "일한 만큼 한도가 열리고 있어요",
                style = MaterialTheme.typography.titleMedium,
                color = DawnText
            )
            Text(
                text = "근무가 더 반영될수록 다음 구간이 열립니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = DawnTextSubtle
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeMetricTile(
                label = "지금 가능 금액",
                value = uiModel.work.advanceAvailableText,
                modifier = Modifier.weight(1f)
            )
            HomeMetricTile(
                label = "다음 구간 진행도",
                value = uiModel.work.advanceProgressText,
                modifier = Modifier.weight(1f)
            )
        }

        DonDoneProgressBar(progress = uiModel.work.advanceProgress)

        Text(
            text = uiModel.work.advanceHintText,
            style = MaterialTheme.typography.bodyMedium,
            color = DawnTextSubtle
        )
    }
}

@Composable
private fun SettlementCard(
    uiModel: HomeUiModel,
    onOpenWage: () -> Unit,
    onOpenFinance: () -> Unit,
    onOpenMenu: () -> Unit
) {
    val paydayAction = resolveAction(
        target = uiModel.money.payday.actionTarget,
        onOpenWage = onOpenWage,
        onOpenFinance = onOpenFinance,
        onOpenMenu = onOpenMenu
    )
    val nextAction = resolveAction(
        target = uiModel.money.nextAction.actionTarget,
        onOpenWage = onOpenWage,
        onOpenFinance = onOpenFinance,
        onOpenMenu = onOpenMenu
    )

    HomeSurfaceCard(hero = true) {
        if (false) {
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
                    text = "급여 점검",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = DawnText
                )
            }
            SettlementAssistButton(
                text = uiModel.money.assistText,
                onClick = onOpenWage
            )
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            uiModel.money.questionChips.forEach { chip ->
                SettlementQuestionChip(
                    text = chip,
                    onClick = onOpenWage
                )
            }
        }

        SettlementNoticeCard(lines = uiModel.money.noticeLines)
        }

        if (true) {
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
                    text = "정산 상태",
                    style = MaterialTheme.typography.labelMedium,
                    color = DawnPrimaryDeep
                )
                Text(
                    text = "이번 달 정산 상태",
                    style = MaterialTheme.typography.titleLarge,
                    color = DawnText
                )
            }
            SettlementStatusPill(
                text = uiModel.money.statusText,
                tone = uiModel.money.statusTone
            )
        }

        Text(
            text = uiModel.money.briefText,
            style = MaterialTheme.typography.bodyMedium,
            color = DawnTextSubtle,
            maxLines = 1
        )

        SettlementSummaryList(
            estimatedText = uiModel.money.estimatedText,
            actualText = uiModel.money.actualText,
            differenceText = uiModel.money.differenceText
        )

        if (uiModel.money.showPaydayCard) {
            HomePaydayStateCard(
                payday = uiModel.money.payday,
                onClick = paydayAction
            )
        }

        SettlementNextActionCard(
            action = uiModel.money.nextAction,
            onClick = nextAction
        )
        }
    }
}

@Composable
private fun SettlementAssistButton(
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, DawnBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White.copy(alpha = 0.56f),
            contentColor = DawnPrimary
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
            )
        }
    }
}

@Composable
private fun SettlementQuestionChip(
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, DawnBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFFF7F2FF),
            contentColor = DawnPrimary
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
        )
    }
}

@Composable
private fun SettlementNoticeCard(
    lines: List<String>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.94f))
            .border(BorderStroke(1.dp, DawnBorder), RoundedCornerShape(28.dp))
            .padding(horizontal = 18.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(21.dp))
                .background(Color(0xFFD7DCE5)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = Color.White
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            lines.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = DawnText,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                )
            }
        }
    }
}

@Composable
private fun HomePaydayStateCard(
    payday: HomePaydayUiModel,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFF8FAFC))
            .border(BorderStroke(1.dp, DawnBorder), RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = payday.kicker,
                style = MaterialTheme.typography.labelMedium,
                color = DawnPrimaryDeep
            )
            Text(
                text = payday.title,
                style = MaterialTheme.typography.titleMedium,
                color = DawnText
            )
            Text(
                text = payday.description,
                style = MaterialTheme.typography.bodyMedium,
                color = DawnTextSubtle
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "기록일",
                    style = MaterialTheme.typography.labelMedium,
                    color = DawnTextSubtle
                )
                Text(
                    text = payday.metaText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = DawnText
                )
            }
            HomePrimaryButton(
                text = payday.buttonText,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun HomeSurfaceCard(
    modifier: Modifier = Modifier,
    hero: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val backgroundBrush = if (hero) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF8F4FF)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFFCFAFF)
            )
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = DawnSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, DawnBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundBrush)
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
private fun HomeMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(BorderStroke(1.dp, DawnBorder), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = DawnTextSubtle
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = DawnText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeInfoPanel(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF8FAFC))
            .border(BorderStroke(1.dp, DawnBorder), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
private fun SettlementNextActionCard(
    action: HomeNextActionUiModel,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.9f))
            .border(BorderStroke(1.dp, DawnBorder), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = action.title,
                style = MaterialTheme.typography.labelMedium,
                color = DawnPrimaryDeep
            )
            Text(
                text = action.message,
                style = MaterialTheme.typography.bodyMedium,
                color = DawnText
            )
        }

        HomePrimaryButton(
            text = action.buttonText,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SettlementStatusPill(
    text: String,
    tone: com.dondone.mobile.core.designsystem.BadgeTone
) {
    val background = when (tone) {
        com.dondone.mobile.core.designsystem.BadgeTone.Info -> DawnSurfaceAlt
        com.dondone.mobile.core.designsystem.BadgeTone.Success -> DawnSecondary
        com.dondone.mobile.core.designsystem.BadgeTone.Warning -> Color(0xFFFBF7EF)
    }
    val foreground = when (tone) {
        com.dondone.mobile.core.designsystem.BadgeTone.Info -> DawnTextSubtle
        com.dondone.mobile.core.designsystem.BadgeTone.Success -> DawnPrimaryDeep
        com.dondone.mobile.core.designsystem.BadgeTone.Warning -> Color(0xFF94612A)
    }

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
            color = foreground
        )
    }
}

@Composable
private fun SettlementSummaryList(
    estimatedText: String,
    actualText: String,
    differenceText: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.96f))
            .border(BorderStroke(1.dp, DawnBorder), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SettlementSummaryRow(
            label = "추정",
            value = estimatedText
        )
        HorizontalDivider(color = DawnBorder)
        SettlementSummaryRow(
            label = "실제",
            value = actualText
        )
        HorizontalDivider(color = DawnBorder)
        SettlementSummaryRow(
            label = "차액",
            value = differenceText,
            valueColor = DawnWarning
        )
    }
}

@Composable
private fun SettlementSummaryRow(
    label: String,
    value: String,
    valueColor: Color = DawnText
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
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
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = valueColor,
            maxLines = 1
        )
    }
}

@Composable
private fun HomePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
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
private fun HomeSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
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
private fun HomeSoftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DawnSecondary,
            contentColor = DawnPrimaryDeep
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black)
        )
    }
}

@Composable
private fun HomeLinkAction(
    text: String,
    onClick: () -> Unit
) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
        color = DawnPrimaryDeep
    )
}

private fun resolveAction(
    target: HomeActionTarget,
    onOpenWage: () -> Unit,
    onOpenFinance: () -> Unit,
    onOpenMenu: () -> Unit
): () -> Unit = when (target) {
    HomeActionTarget.WAGE -> onOpenWage
    HomeActionTarget.FINANCE -> onOpenFinance
    HomeActionTarget.MENU -> onOpenMenu
}
