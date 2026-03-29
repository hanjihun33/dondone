package com.dondone.mobile.feature.workproof.presentation

import androidx.compose.ui.graphics.Color
import com.dondone.mobile.core.designsystem.DawnPrimaryDeep
import com.dondone.mobile.core.designsystem.DawnText
import com.dondone.mobile.core.i18n.AppLanguage
import com.dondone.mobile.core.i18n.text
import java.time.format.DateTimeFormatter

internal val WorkproofCanvas = Color.White
internal val WorkproofDivider = Color(0xFFE8EBF0)
internal val WorkproofGhostBorder = Color(0xFFF4F6F8)
internal val WorkproofCurrentDayBorder = Color(0xFFDDE4FF)
internal val WorkproofRowAccentBackground = Color(0xFFF2F3FF)
internal val WorkproofRowAccentTint = Color(0xFF6D68F5)
internal val WorkproofMissingBackground = Color.White
internal val WorkproofMissingBorder = Color.Transparent
internal val WorkproofMissingText = Color(0xFF64748B)
internal val WorkproofInactiveText = Color(0xFFCBD5E1)
internal val WorkproofPartialBackground = Color(0xFFEEE5FF)
internal val WorkproofPartialBorder = Color(0xFFC3A6FF)
internal val WorkproofPartialText = Color(0xFF6F42D9)
internal val WorkproofCompleteBackground = Color(0xFFE9DEFF)
internal val WorkproofCompleteBorder = Color(0xFFB89BFF)
internal val WorkproofCompleteText = Color(0xFF5E3CC5)
internal val WorkproofReviewBackground = Color(0xFFFFF4DD)
internal val WorkproofReviewBorder = Color(0xFFE7BC62)
internal val WorkproofReviewText = Color(0xFFB7791F)
internal val WorkproofModifiedBackground = Color(0xFFF7E4F4)
internal val WorkproofModifiedBorder = Color(0xFFD98FD0)
internal val WorkproofModifiedText = Color(0xFFAA3E96)
internal val WorkproofMapWorkplacePin = DawnText
internal val WorkproofMapCurrentPin = DawnPrimaryDeep
internal val WorkproofPdfPresetSelectedBackground = Color(0xFFF1ECFF)
internal val WorkproofPdfPresetSelectedBorder = Color(0xFFB89BFF)
internal fun workproofWeekdays(language: AppLanguage): List<String> = listOf(
    language.text("sun"),
    language.text("mon"),
    language.text("tue"),
    language.text("wed"),
    language.text("thu"),
    language.text("fri"),
    language.text("sat")
)
internal val WorkproofPdfDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
