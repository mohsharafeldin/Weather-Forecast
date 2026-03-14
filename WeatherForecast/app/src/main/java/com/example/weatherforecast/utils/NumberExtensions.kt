package com.example.weatherforecast.utils

import java.util.Locale

fun Number.formatLocal(): String {
    return String.format(Locale.getDefault(), "%s", this).localizeDigits()
}

fun Double.formatLocal(digits: Int): String {
    return String.format(Locale.getDefault(), "%.${digits}f", this).localizeDigits()
}

fun formatLocal(number: Number, digits: Int): String {
    return String.format(Locale.getDefault(), "%.${digits}f", number.toDouble()).localizeDigits()
}

fun String.localizeDigits(): String {
    if (Locale.getDefault().language != "ar") return this
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    val builder = java.lang.StringBuilder()
    for (char in this) {
        if (char.isDigit() && char in '0'..'9') {
            builder.append(arabicDigits[char - '0'])
        } else {
            builder.append(char)
        }
    }
    return builder.toString()
}
