package com.dondone.mobile.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dondone.mobile.R

private val LightColors = lightColorScheme(
    primary = DawnPrimary,
    secondary = DawnAccent,
    tertiary = DawnSecondary,
    background = DawnBackground,
    surface = DawnSurface,
    onPrimary = DawnSurface,
    onBackground = DawnText,
    onSurface = DawnText,
    onSurfaceVariant = DawnTextSubtle,
    outline = DawnBorder
)

private val DonDoneFontFamily = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_black, FontWeight.Black)
)

private val DonDoneTypography = Typography(
    // Mirror the mockup's heavier Pretendard rhythm so the Compose app
    // feels closer to the HTML prototype at a glance.
    displaySmall = TextStyle(
        fontFamily = DonDoneFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 31.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.93).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = DonDoneFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.72).sp
    ),
    titleLarge = TextStyle(
        fontFamily = DonDoneFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.64).sp
    ),
    titleMedium = TextStyle(
        fontFamily = DonDoneFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.38).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = DonDoneFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.14).sp
    ),
    bodyMedium = TextStyle(
        fontFamily = DonDoneFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.16).sp
    ),
    labelLarge = TextStyle(
        fontFamily = DonDoneFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = (-0.12).sp
    ),
    labelMedium = TextStyle(
        fontFamily = DonDoneFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 10.sp,
        lineHeight = 12.sp
    )
)

@Composable
fun DonDoneTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = DonDoneTypography,
        content = content
    )
}
