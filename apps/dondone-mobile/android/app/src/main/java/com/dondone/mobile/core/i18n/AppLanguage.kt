package com.dondone.mobile.core.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import java.text.NumberFormat
import java.util.Locale

@Immutable
enum class AppLanguage(
    val code: String,
    val locale: Locale,
    val nativeLabel: String
) {
    KOREAN(code = "ko", locale = Locale.KOREAN, nativeLabel = "\uD55C\uAD6D\uC5B4"),
    ENGLISH(code = "en", locale = Locale.ENGLISH, nativeLabel = "English");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: KOREAN

        fun fromLocale(locale: Locale?): AppLanguage =
            fromCode(locale?.language)

        fun fromDefault(): AppLanguage = fromLocale(Locale.getDefault())
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.KOREAN }

fun AppLanguage.numberFormat(): NumberFormat =
    NumberFormat.getNumberInstance(locale)

@Composable
fun ProvideAppLanguage(
    language: AppLanguage,
    content: @Composable () -> Unit
) {
    SideEffect {
        Locale.setDefault(language.locale)
    }

    CompositionLocalProvider(
        LocalAppLanguage provides language,
        content = content
    )
}
