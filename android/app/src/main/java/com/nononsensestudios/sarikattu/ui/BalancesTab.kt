package com.nononsensestudios.sarikattu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nononsensestudios.sarikattu.domain.GroupDetail
import com.nononsensestudios.sarikattu.domain.Transfer
import com.nononsensestudios.sarikattu.domain.computeBalances
import com.nononsensestudios.sarikattu.domain.formatMoney
import com.nononsensestudios.sarikattu.domain.minorToInput
import com.nononsensestudios.sarikattu.domain.parseMoney
import com.nononsensestudios.sarikattu.domain.settlementSplits
import com.nononsensestudios.sarikattu.domain.simplifyDebts
import java.time.LocalDate

/** The Balances tab of a group: suggested payments with Settle buttons, then everyone's balance. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BalancesTab(vm: AppViewModel, d: GroupDetail, modifier: Modifier = Modifier) {
    val groupId = d.group.id
    val currency = d.group.currency
    val balances = computeBalances(d.members, d.expenses)
    val transfers = simplifyDebts(balances)
    // You first, then whoever is furthest from zero, so big debts are easy to spot.
    val people = d.members.sortedWith(
        compareBy<com.nononsensestudios.sarikattu.domain.Member> { it.id != d.myMemberId }
            .thenByDescending { kotlin.math.abs(balances[it.id] ?: 0L) },
    )
    fun nameOf(id: String) = if (id == d.myMemberId) "You" else d.members.firstOrNull { it.id == id }?.name ?: "?"
    var settling by remember { mutableStateOf<Transfer?>(null) }
    val haptic = rememberConfirmHaptic()

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        if (transfers.isEmpty()) {
            item {
                BalanceHero(0, Modifier.padding(top = 8.dp)) {
                    Text("Everyone is settled up", style = MaterialTheme.typography.headlineSmall)
                    Text("Nothing to pay right now.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            item { SectionHeader("Suggested payments") }
            itemsIndexed(transfers) { i, t ->
                SegmentedListItem(
                    shapes = ListItemDefaults.segmentedShapes(i, transfers.size),
                    colors = segmentColors(),
                    modifier = Modifier.padding(top = if (i > 0) ListItemDefaults.SegmentedGap else 0.dp),
                    leadingContent = { Avatar(nameOf(t.from), t.from) },
                    supportingContent = {
                        Text(formatMoney(t.amountMinor, currency), style = MaterialTheme.typography.titleMedium)
                    },
                    trailingContent = { FilledTonalButton(onClick = { settling = t }) { Text("Settle") } },
                ) { Text("${nameOf(t.from)} → ${nameOf(t.to)}") }
            }
            item {
                Text(
                    "The fewest payments that clear every balance.",
                    Modifier.padding(start = 8.dp, top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { SectionHeader("Everyone") }
        itemsIndexed(people, key = { _, m -> m.id }) { i, m ->
            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(i, people.size),
                colors = segmentColors(),
                modifier = Modifier.padding(top = if (i > 0) ListItemDefaults.SegmentedGap else 0.dp),
                leadingContent = { Avatar(m.name, m.id) },
                trailingContent = { BalancePill(balances[m.id] ?: 0L, currency) },
            ) { Text(nameOf(m.id)) }
        }
        item { Spacer(Modifier.height(120.dp)) } // room for the floating toolbar
    }

    settling?.let { t ->
        SettleDialog(
            fromName = nameOf(t.from), toName = nameOf(t.to),
            initialAmount = minorToInput(t.amountMinor, currency), currency = currency,
            onDismiss = { settling = null },
            onConfirm = { amount ->
                settling = null
                vm.run(
                    {
                        saveExpense(
                            groupId = groupId, expenseId = null, title = "Payment",
                            amountMinor = amount, payerId = t.from, splits = settlementSplits(t.to, amount),
                            spentOn = LocalDate.now().toString(), isSettlement = true,
                        )
                    },
                    onSuccess = haptic,
                )
            },
        )
    }
}

@Composable
private fun SettleDialog(
    fromName: String, toName: String, initialAmount: String, currency: String,
    onDismiss: () -> Unit, onConfirm: (Long) -> Unit,
) {
    var text by remember { mutableStateOf(initialAmount) }
    val amount = parseMoney(text, currency)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$fromName paid $toName")
                OutlinedTextField(
                    text, { text = it.take(15) },
                    label = { Text("Amount ($currency)") }, singleLine = true,
                    shape = FieldShape,
                    isError = amount == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Text(
                    "Pay in cash or any UPI app, then record it here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { amount?.let(onConfirm) }, enabled = amount != null) { Text("Mark as paid") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
