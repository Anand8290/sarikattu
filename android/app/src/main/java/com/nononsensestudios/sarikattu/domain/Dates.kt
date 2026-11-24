package com.nononsensestudios.sarikattu.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "Today", "Yesterday", "Sat, 22 Nov", or "22 Nov 2025" for other years. Falls back to the raw text. */
fun friendlyDate(iso: String, today: LocalDate = LocalDate.now(), locale: Locale = Locale.getDefault()): String {
    val date = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return iso
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(
            DateTimeFormatter.ofPattern(if (date.year == today.year) "EEE, d MMM" else "d MMM yyyy", locale),
        )
    }
}
