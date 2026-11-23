package com.anand8290.splitlite.domain

/**
 * Splits [amountMinor] equally between [memberIds].
 *
 * Leftover minor units (e.g. ₹100.00 / 3) go one at a time to members in
 * sorted id order, so every device computes exactly the same split.
 */
fun splitEqually(amountMinor: Long, memberIds: Collection<String>): List<Split> {
    require(amountMinor > 0) { "Amount must be positive" }
    require(memberIds.isNotEmpty()) { "Pick at least one member" }
    val sorted = memberIds.distinct().sorted()
    val base = amountMinor / sorted.size
    val remainder = amountMinor % sorted.size
    return sorted.mapIndexed { i, id -> Split(id, base + if (i < remainder) 1 else 0) }
}

/** A settlement is an expense paid by [from] and owed entirely by [to]. */
fun settlementSplits(to: String, amountMinor: Long): List<Split> = listOf(Split(to, amountMinor))
