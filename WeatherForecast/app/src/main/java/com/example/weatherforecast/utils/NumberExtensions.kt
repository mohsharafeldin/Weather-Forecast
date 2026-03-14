package com.example.weatherforecast.utils

import java.util.Locale

fun Number.formatLocal(): String {
    return String.format(Locale.getDefault(), "%s", this)
}

fun Double.formatLocal(digits: Int): String {
    return String.format(Locale.getDefault(), "%.${digits}f", this)
}

fun formatLocal(number: Number, digits: Int): String {
    return String.format(Locale.getDefault(), "%.${digits}f", number.toDouble())
}
