package com.dondone.mobile.feature.workproof.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dondone.mobile.core.designsystem.DawnBorder
import com.dondone.mobile.core.designsystem.DawnPrimaryDeep
import com.dondone.mobile.core.designsystem.DawnSurfaceAlt
import com.dondone.mobile.core.i18n.LocalAppLanguage
import com.dondone.mobile.core.i18n.translate
import java.time.LocalTime

@Composable
internal fun WorkproofStatusPill(
    text: String,
    tone: WorkproofRecordTone
) {
    val language = LocalAppLanguage.current
    val background = when (tone) {
        WorkproofRecordTone.DEFAULT -> DawnSurfaceAlt
        WorkproofRecordTone.ACTIVE -> Color(0xFFFFF4DD)
        WorkproofRecordTone.REVIEW -> WorkproofReviewBackground
        WorkproofRecordTone.MODIFIED -> Color(0xFFFFE6EA)
    }
    val color = when (tone) {
        WorkproofRecordTone.DEFAULT -> DawnPrimaryDeep
        WorkproofRecordTone.ACTIVE -> WorkproofPartialText
        WorkproofRecordTone.REVIEW -> WorkproofReviewText
        WorkproofRecordTone.MODIFIED -> WorkproofModifiedText
    }

    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(999.dp))
            .border(1.dp, DawnBorder, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
                    .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
            )
            Text(
                text = language.translate(text),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                color = color
            )
        }
    }
}

internal fun String.isValidWorkproofTimeInput(): Boolean {
    return runCatching { LocalTime.parse(trim()) }.isSuccess
}

internal fun normalizeWorkproofTimeInput(
    previousInput: String,
    rawInput: String
): String {
    val sanitized = rawInput.filter { it.isDigit() || it == ':' }
    val digitsOnly = sanitized.filter(Char::isDigit).take(4)
    val isDeleting = sanitized.length < previousInput.length
    val firstColonIndex = sanitized.indexOf(':')
    if (firstColonIndex >= 0) {
        val hour = sanitized.substring(0, firstColonIndex).filter(Char::isDigit).take(2)
        val minute = sanitized.substring(firstColonIndex + 1).filter(Char::isDigit).take(2)
        if (shouldCollapseAutoInsertedColonOnDelete(previousInput, minute, isDeleting)) {
            return digitsOnly
        }
        return when {
            hour.isEmpty() && minute.isEmpty() -> ""
            hour.isEmpty() -> minute
            minute.isEmpty() -> "$hour:"
            else -> "$hour:$minute"
        }
    }

    return when {
        digitsOnly.length < 4 -> digitsOnly
        else -> digitsOnly.take(2) + ":" + digitsOnly.drop(2)
    }
}

private fun shouldCollapseAutoInsertedColonOnDelete(
    previousInput: String,
    minute: String,
    isDeleting: Boolean
): Boolean {
    if (!isDeleting || ':' !in previousInput) {
        return false
    }
    val previousMinute = previousInput.substringAfter(':').filter(Char::isDigit).take(2)
    if (previousMinute.length != 2 || minute.length != 1) {
        return false
    }
    return minute == previousMinute.dropLast(1)
}

internal data class WorkproofEditReasonOption(
    val key: String,
    val label: String
)

internal val WorkproofEditReasons = listOf(
    WorkproofEditReasonOption(
        key = "LATE_BUTTON_PRESS",
        label = "출근/퇴근 탭을 늦게 눌렀어요"
    ),
    WorkproofEditReasonOption(
        key = "LATE_CLOCK_IN",
        label = "출근 시간을 다시 인정받고 싶어요"
    ),
    WorkproofEditReasonOption(
        key = "EARLY_CLOCK_OUT",
        label = "퇴근 시간을 다시 인정받고 싶어요"
    ),
    WorkproofEditReasonOption(
        key = "OTHER",
        label = "기타 사유"
    )
)
