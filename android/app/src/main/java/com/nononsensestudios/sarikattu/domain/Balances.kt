package com.nononsensestudios.sarikattu.domain

/**
 * Net balance per member: positive = the group owes them, negative = they owe.
 * Balances always sum to zero. Deleted expenses are ignored.
 */
fun computeBalances(members: List<Member>, expenses: List<Expense>): Map<String, Long> {
    val balances = members.associate { it.id to 0L }.toMutableMap()
    for (e in expenses) {
        if (e.deletedAt != null) continue
        balances[e.payerId] = (balances[e.payerId] ?: 0L) + e.amountMinor
        for (s in e.splits) {
            balances[s.memberId] = (balances[s.memberId] ?: 0L) - s.amountMinor
        }
    }
    return balances
}

/**
 * Turns balances into a short list of payments that settles everyone.
 * Greedy: the biggest debtor pays the biggest creditor until one is at zero.
 * Ties break on member id so the result is stable across devices.
 */
fun simplifyDebts(balances: Map<String, Long>): List<Transfer> {
    val debtors = balances.filter { it.value < 0 }.map { it.key to -it.value }.toMutableList()
    val creditors = balances.filter { it.value > 0 }.map { it.key to it.value }.toMutableList()
    val order = compareByDescending<Pair<String, Long>> { it.second }.thenBy { it.first }
    val transfers = mutableListOf<Transfer>()
    while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
        debtors.sortWith(order)
        creditors.sortWith(order)
        val (debtor, owes) = debtors[0]
        val (creditor, owed) = creditors[0]
        val amount = minOf(owes, owed)
        transfers += Transfer(debtor, creditor, amount)
        if (owes == amount) debtors.removeAt(0) else debtors[0] = debtor to owes - amount
        if (owed == amount) creditors.removeAt(0) else creditors[0] = creditor to owed - amount
    }
    return transfers
}
