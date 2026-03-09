package com.dondone.mobile.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    enabled: Boolean = true
) {
    Button(
        modifier = Modifier
            .defaultMinSize(minHeight = 44.dp)
            .sizeIn(minWidth = 96.dp),
        enabled = enabled,
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = DawnPrimary),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(text = text)
    }
}

@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    OutlinedButton(
        modifier = Modifier
            .defaultMinSize(minHeight = 44.dp)
            .sizeIn(minWidth = 96.dp),
        enabled = enabled,
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, DawnBorder),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = DawnSurface)
    ) {
        Text(text = text)
    }
}

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    OutlinedButton(
        enabled = enabled,
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, DawnBorder),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = DawnSurface)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
