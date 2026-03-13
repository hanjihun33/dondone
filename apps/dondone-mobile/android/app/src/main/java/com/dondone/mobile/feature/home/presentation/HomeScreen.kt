package com.dondone.mobile.feature.home.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.designsystem.DawnSecondary

private val HomeCanvas = Color.White
private val HomeSurface = Color.White
private val HomeSurfaceMuted = Color(0xFFF5F6FA)
private val HomeDivider = Color(0xFFE8EBF0)
private val HomeTextPrimary = Color(0xFF1F2430)
private val HomeTextMuted = Color(0xFF8B95A1)
private val HomeAccent = Color(0xFF6D68F5)
private val HomeAccentSoft = DawnSecondary.copy(alpha = 0.62f)
private val HomeSuccessMuted = Color(0xFFF3F5F8)
private val HomeWarningMuted = Color(0xFFF9F4EC)
private val HomeWarningText = Color(0xFFB67D39)
private val HomeSuccessText = Color(0xFF7E8896)

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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeCanvas)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            HomeAccountHero(
                uiModel = uiModel,
                onOpenAccount = onOpenAccount,
                onOpenFinance = onOpenFinance,
                onOpenMenu = onOpenMenu,
                onOpenTransfer = onOpenTransfer,
                onOpenWage = onOpenWage
            )
            HomeSectionDivider()
            HomeWorkSection(
                uiModel = uiModel,
                onOpenWorkproof = onOpenWorkproof,
                onClockIn = onClockIn,
                onClockOut = onClockOut
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HomeAccountHero(
    uiModel: HomeUiModel,
    onOpenAccount: () -> Unit,
    onOpenFinance: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenTransfer: () -> Unit,
    onOpenWage: () -> Unit
) {
    val nextAction = resolveAction(
        target = uiModel.money.nextAction.actionTarget,
        onOpenWage = onOpenWage,
        onOpenFinance = onOpenFinance,
        onOpenMenu = onOpenMenu
    )

    HomeSectionSurface {
        HomeSectionHeader(
            title = "이번 달 내 돈",
            trailing = {
                HomeLinkText(
                    text = "계좌 관리",
                    onClick = onOpenAccount
                )
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = uiModel.account.balanceText,
                style = MaterialTheme.typography.displaySmall,
                color = HomeTextPrimary
            )
            HomeSecondaryButton(
                text = uiModel.money.nextAction.buttonText,
                onClick = nextAction
            )
        }

        HomePrimaryButton(
            text = "송금하기",
            onClick = onOpenTransfer,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun HomeWorkSection(
    uiModel: HomeUiModel,
    onOpenWorkproof: () -> Unit,
    onClockIn: () -> Unit,
    onClockOut: () -> Unit
) {
    HomeSectionSurface {
        HomeSectionHeader(
            title = "오늘 근무",
            trailing = {
                HomeLinkText(
                    text = "기록 보기",
                    onClick = onOpenWorkproof
                )
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = uiModel.work.dateText,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = HomeTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(10.dp))
            HomeStatusPill(
                text = uiModel.work.statusText,
                tone = uiModel.work.statusTone
            )
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

        HomeKeyValueRow(label = "출근", value = uiModel.work.clockInText)
        HomeKeyValueRow(label = "퇴근", value = uiModel.work.clockOutText)
    }
}

@Composable
private fun HomeSectionSurface(
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
private fun HomeSectionDivider() {
    Spacer(modifier = Modifier.height(14.dp))
    HorizontalDivider(color = HomeDivider)
    Spacer(modifier = Modifier.height(14.dp))
}

@Composable
private fun HomeSectionHeader(
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
            color = HomeTextPrimary
        )
        trailing()
    }
}

@Composable
private fun HomeStatusPill(
    text: String,
    tone: BadgeTone
) {
    val background = when (tone) {
        BadgeTone.Info -> HomeAccentSoft
        BadgeTone.Success -> HomeSuccessMuted
        BadgeTone.Warning -> HomeWarningMuted
    }
    val foreground = when (tone) {
        BadgeTone.Info -> HomeAccent
        BadgeTone.Success -> HomeSuccessText
        BadgeTone.Warning -> HomeWarningText
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = foreground
        )
    }
}

@Composable
private fun HomeKeyValueRow(
    label: String,
    value: String,
    valueColor: Color = HomeTextPrimary
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
            color = HomeTextMuted
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
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = HomeAccent,
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
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, HomeDivider),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = HomeSurface,
            contentColor = HomeAccent
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
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = HomeSurfaceMuted,
            contentColor = HomeTextPrimary
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black)
        )
    }
}

@Composable
private fun HomeLinkText(
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
        color = HomeAccent
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
