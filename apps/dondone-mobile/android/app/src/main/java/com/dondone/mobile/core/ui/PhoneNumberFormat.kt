package com.dondone.mobile.core.ui

fun String.phoneDigits(): String = filter(Char::isDigit)

fun String.toDisplayPhoneNumber(): String {
    val digits = phoneDigits()
    return when (digits.length) {
        11 -> "${digits.take(3)}-${digits.substring(3, 7)}-${digits.takeLast(4)}"
        10 -> "${digits.take(3)}-${digits.substring(3, 6)}-${digits.takeLast(4)}"
        else -> this
    }
}

fun String.toMaskedPhoneNumber(): String {
    val digits = phoneDigits()
    return when (digits.length) {
        11 -> "${digits.take(3)}-****-${digits.takeLast(4)}"
        10 -> "${digits.take(3)}-***-${digits.takeLast(4)}"
        else -> this
    }
}
