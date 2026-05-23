package com.moncake94.pos.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val idLocale = Locale("id", "ID")
private val rupiah = NumberFormat.getNumberInstance(idLocale)
private val dateTime = SimpleDateFormat("dd MMM yyyy HH:mm", idLocale)
private val dateOnly = SimpleDateFormat("dd MMM yyyy", idLocale)

fun formatCurrency(value: Long): String = "Rp${rupiah.format(value)}"

fun formatThousandsInput(digits: String): String {
    val value = digits.filter { it.isDigit() }.toLongOrNull() ?: return ""
    return rupiah.format(value)
}

fun parseCurrency(value: String): Long {
    return value.filter { it.isDigit() }.toLongOrNull() ?: 0L
}

fun formatDateTime(epochMillis: Long): String = dateTime.format(Date(epochMillis))

fun formatDate(epochMillis: Long): String = dateOnly.format(Date(epochMillis))

fun formatTime(epochMillis: Long): String = SimpleDateFormat("HH:mm", idLocale).format(Date(epochMillis))
