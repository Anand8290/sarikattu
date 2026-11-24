package com.nononsensestudios.sarikattu.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class DatesTest {
    private val today = LocalDate.of(2026, 11, 24)
    private fun label(iso: String) = friendlyDate(iso, today, Locale.UK)

    @Test fun todayAndYesterday() {
        assertEquals("Today", label("2026-11-24"))
        assertEquals("Yesterday", label("2026-11-23"))
    }

    @Test fun sameYearShowsWeekday() = assertEquals("Sun, 22 Nov", label("2026-11-22"))

    @Test fun otherYearShowsYear() = assertEquals("31 Dec 2025", label("2025-12-31"))

    @Test fun badInputIsReturnedAsIs() = assertEquals("soon", label("soon"))
}
