package com.anand8290.splitlite.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class MoneyTest {

    @Test
    fun `parses rupees into paise`() {
        assertEquals(123_450L, parseMoney("1,234.5", "INR"))
        assertEquals(100L, parseMoney("1", "INR"))
    }

    @Test
    fun `rejects bad input`() {
        assertNull(parseMoney("", "INR"))
        assertNull(parseMoney("abc", "INR"))
        assertNull(parseMoney("-5", "INR"))
        assertNull(parseMoney("0", "INR"))
        assertNull(parseMoney("1.234", "INR"))
    }

    @Test
    fun `zero-decimal currencies`() {
        assertEquals(500L, parseMoney("500", "JPY"))
        assertNull(parseMoney("500.5", "JPY"))
    }

    @Test
    fun `formats minor units`() {
        assertEquals("$1,234.50", formatMoney(123_450, "USD", Locale.US))
    }

    @Test
    fun `round trips through the input string`() {
        assertEquals(123_450L, parseMoney(minorToInput(123_450, "INR"), "INR"))
    }
}
