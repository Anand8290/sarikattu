package com.nononsensestudios.sarikattu.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Number of minor-unit digits for a currency (INR 2, JPY 0). */
fun fractionDigits(currency: String): Int =
    runCatching { Currency.getInstance(currency).defaultFractionDigits }.getOrNull()
        ?.takeIf { it >= 0 } ?: 2

/** Formats minor units as a currency string, e.g. 123450 INR -> "₹1,234.50". */
fun formatMoney(amountMinor: Long, currency: String, locale: Locale = Locale.getDefault()): String {
    val digits = fractionDigits(currency)
    val format = NumberFormat.getCurrencyInstance(locale).apply {
        runCatching { this.currency = Currency.getInstance(currency) }
        minimumFractionDigits = digits
        maximumFractionDigits = digits
    }
    return format.format(BigDecimal.valueOf(amountMinor, digits))
}

/**
 * Parses user input like "1,234.5" into minor units. Returns null for
 * empty, negative, zero or over-precise input (e.g. "1.234" for INR).
 */
fun parseMoney(input: String, currency: String): Long? {
    val cleaned = input.trim().replace(",", "").replace(" ", "")
    if (cleaned.isEmpty()) return null
    val value = cleaned.toBigDecimalOrNull() ?: return null
    if (value.signum() <= 0) return null
    val digits = fractionDigits(currency)
    return runCatching {
        value.setScale(digits, RoundingMode.UNNECESSARY).movePointRight(digits).longValueExact()
    }.getOrNull()
}

/** Minor units as a plain editable string, e.g. 123450 -> "1234.50". */
fun minorToInput(amountMinor: Long, currency: String): String =
    BigDecimal.valueOf(amountMinor, fractionDigits(currency)).toPlainString()
