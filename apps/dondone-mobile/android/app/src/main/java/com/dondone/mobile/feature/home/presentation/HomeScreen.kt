package com.dondone.mobile.feature.home.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dondone.mobile.core.designsystem.BadgeTone
import com.dondone.mobile.core.designsystem.DawnPrimary
import com.dondone.mobile.core.designsystem.DawnSecondary
import com.dondone.mobile.core.designsystem.pressableScale
import com.dondone.mobile.core.designsystem.rememberDonDoneGrayRipple

private val HomeCanvas = Color.White
private val HomeSurface = Color.White
private val HomeSurfaceMuted = Color(0xFFF5F6FA)
private val HomeDivider = Color(0xFFE8EBF0)
private val HomeTextPrimary = Color(0xFF1F2430)
private val HomeTextMuted = Color(0xFF8B95A1)
private val HomeAccent = DawnPrimary
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
                onOpenTransfer = onOpenTransfer
            )
            HomeSectionDivider()
            HomeWorkSection(
                uiModel = uiModel,
                onOpenWorkproof = onOpenWorkproof,
                onClockIn = onClockIn,
                onClockOut = onClockOut
            )
            if (uiModel.money.showWorkActionCard) {
                Column {
                    HomeSectionDivider()
                    HomeActionCallout(
                        message = uiModel.money.nextAction.message,
                        buttonText = uiModel.money.nextAction.buttonText,
                        onOpenWage = onOpenWage,
                        onActionClick = resolveAction(
                            target = uiModel.money.nextAction.actionTarget,
                            onOpenWage = onOpenWage,
                            onOpenFinance = onOpenFinance,
                            onOpenMenu = onOpenMenu
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HomeAccountHero(
    uiModel: HomeUiModel,
    onOpenAccount: () -> Unit,
    onOpenTransfer: () -> Unit
) {
    HomeSectionSurface {
        HomeSectionHeader(title = "지금 쓸 수 있는 돈")

        HomePressableCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenAccount,
            containerColor = HomeSurface
        ) {
            Text(
                text = "대표 계좌",
                style = MaterialTheme.typography.labelLarge,
                color = HomeTextMuted
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
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = HomeTextMuted
                )
            }
        }

        HomePrimaryButton(
            text = "송금하기",
            onClick = onOpenTransfer,
            modifier = Modifier.fillMaxWidth(),
            enablePressScale = false
        )
    }
}

@Composable
private fun HomeActionCallout(
    message: String,
    onOpenWage: () -> Unit,
    buttonText: String,
    onActionClick: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stackedLayout = maxWidth < 360.dp
        val interactionSource = remember { MutableInteractionSource() }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(HomeSurfaceMuted)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (stackedLayout) {
                HomeActionCalloutBody(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = rememberDonDoneGrayRipple(),
                            onClick = onOpenWage
                        )
                        .padding(vertical = 2.dp),
                    message = message
                )
                HomeMiniAccentButton(
                    text = buttonText,
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HomeActionCalloutBody(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = rememberDonDoneGrayRipple(),
                                onClick = onOpenWage
                            )
                            .padding(vertical = 2.dp),
                        message = message
                    )
                    HomeMiniAccentButton(
                        text = buttonText,
                        onClick = onActionClick
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeActionCalloutBody(
    modifier: Modifier = Modifier,
    message: String
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = HomeAccent
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "급여 점검",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = HomeTextPrimary
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = HomeTextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
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
        HomeSectionHeader(title = "오늘 근무")

        HomePressableCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenWorkproof,
            containerColor = HomeSurface
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = uiModel.work.dateText,
                        style = MaterialTheme.typography.labelLarge,
                        color = HomeTextMuted
                    )
                    Text(
                        text = "근무 기록 보기",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = HomeTextPrimary
                    )
                }
                HomeStatusPill(
                    text = uiModel.work.statusText,
                    tone = uiModel.work.statusTone
                )
            }

            HomeKeyValueRow(label = "출근", value = uiModel.work.clockInText)
            HomeKeyValueRow(label = "퇴근", value = uiModel.work.clockOutText)
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

    }
}

@Composable
private fun HomePressableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = HomeSurfaceMuted,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberDonDoneGrayRipple(),
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
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
    trailing: (@Composable () -> Unit)? = null
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
        trailing?.invoke()
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
    enabled: Boolean = true,
    enablePressScale: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.foundation.LocalIndication provides rememberDonDoneGrayRipple()
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = if (enablePressScale) {
                modifier.pressableScale(
                    interactionSource = interactionSource,
                    enabled = enabled
                )
            } else {
                modifier
            },
            interactionSource = interactionSource,
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
}

@Composable
private fun HomeSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
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
}

@Composable
private fun HomeSoftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
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
}

@Composable
private fun HomeMiniAccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.foundation.LocalIndication provides rememberDonDoneGrayRipple()
    ) {
        Button(
            onClick = onClick,
            modifier = modifier,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = HomeAccent,
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
