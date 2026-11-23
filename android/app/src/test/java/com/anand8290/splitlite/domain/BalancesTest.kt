package com.anand8290.splitlite.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BalancesTest {

    private val members = listOf("a", "b", "c").map { Member(it, it.uppercase()) }

    private fun expense(id: String, payer: String, amount: Long, among: List<String>, deleted: Boolean = false) =
        Expense(
            id = id, title = id, amountMinor = amount, payerId = payer, spentOn = "2026-11-23",
            deletedAt = if (deleted) "2026-11-23T00:00:00Z" else null,
            splits = splitEqually(amount, among),
        )

    private fun settlement(id: String, from: String, to: String, amount: Long) =
        Expense(id, "Payment", amount, from, isSettlement = true, spentOn = "2026-11-23",
            splits = settlementSplits(to, amount))

    @Test
    fun `payer is owed the others' shares`() {
        val b = computeBalances(members, listOf(expense("e1", "a", 3_000, listOf("a", "b", "c"))))
        assertEquals(mapOf("a" to 2_000L, "b" to -1_000L, "c" to -1_000L), b)
    }

    @Test
    fun `balances always sum to zero`() {
        val b = computeBalances(members, listOf(
            expense("e1", "a", 10_000, listOf("a", "b", "c")),
            expense("e2", "b", 777, listOf("a", "c")),
            expense("e3", "c", 1, listOf("a", "b", "c")),
        ))
        assertEquals(0L, b.values.sum())
    }

    @Test
    fun `deleted expenses are ignored`() {
        val b = computeBalances(members, listOf(expense("e1", "a", 3_000, listOf("a", "b", "c"), deleted = true)))
        assertTrue(b.values.all { it == 0L })
    }

    @Test
    fun `settlement clears a debt`() {
        val b = computeBalances(members, listOf(
            expense("e1", "a", 2_000, listOf("a", "b")),
            settlement("s1", from = "b", to = "a", amount = 1_000),
        ))
        assertTrue(b.values.all { it == 0L })
    }

    @Test
    fun `simplify with two people`() {
        assertEquals(listOf(Transfer("b", "a", 500)), simplifyDebts(mapOf("a" to 500L, "b" to -500L)))
    }

    @Test
    fun `simplify with three people needs at most two payments`() {
        val t = simplifyDebts(mapOf("a" to 2_000L, "b" to -1_000L, "c" to -1_000L))
        assertEquals(listOf(Transfer("b", "a", 1_000), Transfer("c", "a", 1_000)), t)
    }

    @Test
    fun `simplify returns nothing when everyone is settled`() {
        assertTrue(simplifyDebts(mapOf("a" to 0L, "b" to 0L)).isEmpty())
    }

    @Test
    fun `applying the suggested payments settles everyone`() {
        val balances = mapOf("a" to 4_100L, "b" to -2_600L, "c" to -900L, "d" to 700L, "e" to -1_300L)
        val after = balances.toMutableMap()
        val transfers = simplifyDebts(balances)
        for (t in transfers) {
            after[t.from] = after.getValue(t.from) + t.amountMinor
            after[t.to] = after.getValue(t.to) - t.amountMinor
        }
        assertTrue(after.values.all { it == 0L })
        assertTrue(transfers.size <= balances.size - 1)
    }
}
