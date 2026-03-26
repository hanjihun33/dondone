package com.dondone.mobile.core.ui

import com.dondone.mobile.core.i18n.AppLanguage
import com.dondone.mobile.core.i18n.numberFormat
import com.dondone.mobile.domain.model.DemoInfo
import kotlin.math.abs

fun formatKrw(value: Int): String = formatKrw(value, AppLanguage.fromDefault())

fun formatKrw(
    value: Int,
    language: AppLanguage
): String = "₩${language.numberFormat().format(value)}"

fun formatSignedKrw(value: Int): String = formatSignedKrw(value, AppLanguage.fromDefault())

fun formatSignedKrw(
    value: Int,
    language: AppLanguage
): String {
    val sign = if (value > 0) "+" else if (value < 0) "-" else ""
    return sign + formatKrw(abs(value), language)
}

fun DemoInfo.formatAsOfLabel(
    language: AppLanguage = AppLanguage.fromDefault()
): String {
    val monthText = month.toString().padStart(2, '0')
    val dayText = asOfDay.toString().padStart(2, '0')
    return when (language) {
        AppLanguage.KOREAN -> "$year.$monthText.$dayText"
        AppLanguage.ENGLISH -> "$year.$monthText.$dayText"
    }
}
