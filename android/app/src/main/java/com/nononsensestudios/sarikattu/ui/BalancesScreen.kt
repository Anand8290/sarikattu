package com.nononsensestudios.sarikattu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nononsensestudios.sarikattu.domain.Transfer
import com.nononsensestudios.sarikattu.domain.computeBalances
import com.nononsensestudios.sarikattu.domain.formatMoney
import com.nononsensestudios.sarikattu.domain.minorToInput
import com.nononsensestudios.sarikattu.domain.parseMoney
import com.nononsensestudios.sarikattu.domain.settlementSplits
import com.nononsensestudios.sarikattu.domain.simplifyDebts
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalancesScreen(vm: AppViewModel, nav: NavHostController, groupId: String) {
    val detail by vm.repo.group(groupId).collectAsState()
    val d = detail ?: return
    val currency = d.group.currency
    val balances = computeBalances(d.members, d.expenses)
    val transfers = simplifyDebts(balances)
    fun nameOf(id: String) = if (id == d.myMemberId) "You" else d.members.firstOrNull { it.id == id }?.name ?: "?"
    var settling by remember { mutableStateOf<Transfer?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Balances") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(d.members, key = { it.id }) { m ->
                val b = balances[m.id] ?: 0L
                ListItem(
                    content = { Text(nameOf(m.id)) },
                    trailingContent = {
                        Text(
                            when {
                                b > 0 -> "+${formatMoney(b, currency)}"
                                b < 0 -> "−${formatMoney(-b, currency)}"
                                else -> "settled"
                            },
                            color = balanceColor(b),
                        )
                    },
                )
                HorizontalDivider()
            }
            item {
                Text(
                    if (transfers.isEmpty()) "Everyone is settled up." else "Suggested payments",
                    Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            items(transfers) { t ->
                ListItem(
                    content = { Text("${nameOf(t.from)} → ${nameOf(t.to)}") },
                    supportingContent = { Text(formatMoney(t.amountMinor, currency)) },
                    trailingContent = { FilledTonalButton(onClick = { settling = t }) { Text("Settle") } },
                )
            }
        }
    }

    settling?.let { t ->
        SettleDialog(
            fromName = nameOf(t.from), toName = nameOf(t.to),
            initialAmount = minorToInput(t.amountMinor, currency), currency = currency,
            onDismiss = { settling = null },
            onConfirm = { amount ->
                settling = null
                vm.run({
                    saveExpense(
                        groupId = groupId, expenseId = null, title = "Payment",
                        amountMinor = amount, payerId = t.from, splits = settlementSplits(t.to, amount),
                        spentOn = LocalDate.now().toString(), isSettlement = true,
                    )
                })
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
