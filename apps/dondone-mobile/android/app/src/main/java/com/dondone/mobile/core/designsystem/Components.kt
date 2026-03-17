package com.dondone.mobile.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val DonDoneGrayRipple = Color(0x1F4E5968)
private const val DonDoneToastDurationMillis = 2200L

@Suppress("UNUSED_PARAMETER")
fun Modifier.pressableScale(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = 0.97f
): Modifier = this

fun rememberDonDoneGrayRipple(
    bounded: Boolean = true,
    radius: Dp = Dp.Unspecified
) = ripple(
    bounded = bounded,
    radius = radius,
    color = DonDoneGrayRipple
)

@Composable
fun DonDoneWordmark(
    modifier: Modifier = Modifier
) {
    val logoText = buildAnnotatedString {
        withStyle(SpanStyle(brush = Brush.linearGradient(colors = DawnLogoGradient))) {
            append("Don")
        }
        withStyle(SpanStyle(color = DawnText)) {
            append("Done")
        }
    }

    Text(
        text = logoText,
        modifier = modifier,
        style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.72).sp,
            shadow = Shadow(
                color = Color(0x33171E5F),
                offset = Offset(0f, 10f),
                blurRadius = 24f
            )
        ),
        maxLines = 1
    )
}

@Composable
fun DonDoneCard(
    kicker: String,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = DawnSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, DawnBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = kicker, style = MaterialTheme.typography.labelMedium, color = DawnTextSubtle)
                Text(text = title, style = MaterialTheme.typography.titleLarge)
            }
            content()
        }
    }
}

@Composable
fun SectionPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(DawnSurfaceAlt)
            .border(BorderStroke(1.dp, DawnBorder), RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
fun MetricRow(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricBox(label = leftLabel, value = leftValue, modifier = Modifier.weight(1f))
        MetricBox(label = rightLabel, value = rightValue, modifier = Modifier.weight(1f))
    }
}

@Composable
fun MetricBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(DawnSurfaceAlt)
            .border(BorderStroke(1.dp, DawnBorder), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = DawnTextSubtle)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

enum class BadgeTone {
    Info, Success, Warning
}

@Composable
fun StatusBadge(text: String, tone: BadgeTone) {
    val background = when (tone) {
        BadgeTone.Info -> DawnSecondary
        BadgeTone.Success -> DawnSuccess.copy(alpha = 0.12f)
        BadgeTone.Warning -> DawnWarning.copy(alpha = 0.14f)
    }
    val foreground = when (tone) {
        BadgeTone.Info -> DawnPrimaryDeep
        BadgeTone.Success -> DawnSuccess
        BadgeTone.Warning -> DawnWarning
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(text = text, color = foreground, style = MaterialTheme.typography.labelMedium)
    }
}

internal data class DonDoneToastVisual(
    val id: Long,
    val message: String,
    val tone: BadgeTone
)

class DonDoneToastState {
    internal var current by mutableStateOf<DonDoneToastVisual?>(null)
        private set

    fun show(
        message: String,
        tone: BadgeTone = BadgeTone.Info
    ) {
        current = DonDoneToastVisual(
            id = System.currentTimeMillis(),
            message = message,
            tone = tone
        )
    }

    fun dismiss() {
        current = null
    }
}

@Composable
fun rememberDonDoneToastState(): DonDoneToastState = remember { DonDoneToastState() }

@Composable
fun DonDoneToastHost(
    state: DonDoneToastState,
    modifier: Modifier = Modifier
) {
    val current = state.current ?: return

    LaunchedEffect(current.id) {
        delay(DonDoneToastDurationMillis)
        state.dismiss()
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        val background = when (current.tone) {
            BadgeTone.Info -> DawnPrimaryDeep
            BadgeTone.Success -> DawnSuccess
            BadgeTone.Warning -> DawnWarning
        }

        Text(
            text = current.message,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(background)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun FeedbackPanel(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    tone: BadgeTone = BadgeTone.Info,
    leading: @Composable (() -> Unit)? = null
) {
    val accent = when (tone) {
        BadgeTone.Info -> DawnPrimaryDeep
        BadgeTone.Success -> DawnSuccess
        BadgeTone.Warning -> DawnWarning
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DawnSurfaceAlt)
            .border(BorderStroke(1.dp, DawnBorder), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        leading?.invoke()
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                color = DawnText
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = DawnTextSubtle
            )
        }
        if (actionLabel != null && onAction != null) {
            OutlinedButton(
                onClick = onAction,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, accent),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = DawnSurface)
            ) {
                Text(text = actionLabel, color = accent)
            }
        }
    }
}

@Composable
fun DonDoneLoadingPanel(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    FeedbackPanel(
        title = title,
        message = message,
        modifier = modifier,
        leading = {
            CircularProgressIndicator(
                color = DawnPrimaryDeep,
                strokeWidth = 2.5.dp
            )
        }
    )
}

@Composable
fun DonDoneEmptyPanel(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    FeedbackPanel(
        title = title,
        message = message,
        modifier = modifier
    )
}

@Composable
fun DonDoneErrorPanel(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    FeedbackPanel(
        title = title,
        message = message,
        modifier = modifier,
        actionLabel = actionLabel,
        onAction = onAction,
        tone = BadgeTone.Warning
    )
}

@Composable
fun DonDoneProgressBar(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(DawnSecondary)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(DawnPrimaryDeep, DawnPrimary, Color(0xFFB98DE8))
                    )
                )
        )
    }
}

@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    CompositionLocalProvider(
        LocalIndication provides rememberDonDoneGrayRipple()
    ) {
        Button(
            modifier = modifier
                .defaultMinSize(minHeight = 44.dp)
                .sizeIn(minWidth = 96.dp)
                .pressableScale(
                    interactionSource = interactionSource,
                    enabled = enabled
                ),
            enabled = enabled,
            onClick = onClick,
            interactionSource = interactionSource,
            colors = ButtonDefaults.buttonColors(containerColor = DawnPrimary),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(text = text)
        }
    }
}

@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    CompositionLocalProvider(
        LocalIndication provides rememberDonDoneGrayRipple()
    ) {
        OutlinedButton(
            modifier = modifier
                .defaultMinSize(minHeight = 44.dp)
                .sizeIn(minWidth = 96.dp)
                .pressableScale(
                    interactionSource = interactionSource,
                    enabled = enabled
                ),
            enabled = enabled,
            onClick = onClick,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, DawnBorder),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = DawnSurface)
        ) {
            Text(text = text)
        }
    }
}

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }

    CompositionLocalProvider(
        LocalIndication provides rememberDonDoneGrayRipple()
    ) {
        OutlinedButton(
            enabled = enabled,
            onClick = onClick,
            interactionSource = interactionSource,
            modifier = Modifier.pressableScale(
                interactionSource = interactionSource,
                enabled = enabled,
                pressedScale = 0.98f
            ),
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, DawnBorder),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = DawnSurface)
        ) {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
