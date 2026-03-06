package com.dondone.mobile.core.ui

import com.dondone.mobile.domain.model.DemoInfo
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

private val currencyFormat = NumberFormat.getNumberInstance(Locale.KOREA)

fun formatKrw(value: Int): String = "₩${currencyFormat.format(value)}"

fun formatSignedKrw(value: Int): String {
    val sign = if (value > 0) "+" else if (value < 0) "-" else ""
    return sign + formatKrw(abs(value))
}

fun DemoInfo.formatAsOfLabel(): String {
    return "$year.${month.toString().padStart(2, '0')}.${asOfDay.toString().padStart(2, '0')}"
}
