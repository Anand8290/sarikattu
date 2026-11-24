package com.nononsensestudios.sarikattu.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SplittingTest {

    @Test
    fun `even split has no remainder`() {
        val splits = splitEqually(30_000, listOf("a", "b", "c"))
        assertEquals(listOf(10_000L, 10_000L, 10_000L), splits.map { it.amountMinor })
    }

    @Test
    fun `remainder goes to members in sorted id order`() {
        val splits = splitEqually(10_000, listOf("c", "a", "b"))
        assertEquals(listOf("a", "b", "c"), splits.map { it.memberId })
        assertEquals(listOf(3_334L, 3_333L, 3_333L), splits.map { it.amountMinor })
        assertEquals(10_000L, splits.sumOf { it.amountMinor })
    }

    @Test
    fun `split always sums to the amount`() {
        for (amount in 1L..500L) for (n in 1..7) {
            val ids = (1..n).map { "m$it" }
            assertEquals(amount, splitEqually(amount, ids).sumOf { it.amountMinor })
        }
    }

    @Test
    fun `duplicate ids are counted once`() {
        assertEquals(2, splitEqually(100, listOf("a", "a", "b")).size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty member list is rejected`() {
        splitEqually(100, emptyList())
    }
}
