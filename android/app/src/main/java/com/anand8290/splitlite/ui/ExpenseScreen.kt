package com.anand8290.splitlite.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.anand8290.splitlite.domain.formatMoney
import com.anand8290.splitlite.domain.minorToInput
import com.anand8290.splitlite.domain.parseMoney
import com.anand8290.splitlite.domain.splitEqually
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpenseScreen(vm: AppViewModel, nav: NavHostController, groupId: String, expenseId: String?) {
    val detail by vm.repo.group(groupId).collectAsState()
    val busy by vm.busy.collectAsState()
    val d = detail ?: return
    val existing = expenseId?.let { id -> d.expenses.firstOrNull { it.id == id } }
    val currency = d.group.currency
    val names = d.members.associate { it.id to it.name }

    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var amountText by rememberSaveable { mutableStateOf(existing?.let { minorToInput(it.amountMinor, currency) } ?: "") }
    var payerId by rememberSaveable { mutableStateOf(existing?.payerId ?: d.myMemberId ?: d.members.first().id) }
    var among by rememberSaveable {
        mutableStateOf(existing?.splits?.map { it.memberId } ?: d.members.map { it.id })
    }
    var date by rememberSaveable { mutableStateOf(existing?.spentOn ?: LocalDate.now().toString()) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val amount = parseMoney(amountText, currency)
    val valid = title.isNotBlank() && amount != null && among.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing?.isSettlement == true) "Payment" else if (existing != null) "Edit expense" else "Add expense") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.Close, "Close") }
                },
                actions = {
                    if (existing != null) {
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Delete") }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (existing?.isSettlement == true) {
                // Payments are only recorded or deleted, never edited.
                val to = existing.splits.firstOrNull()?.memberId
                Text(
                    "${names[existing.payerId]} paid ${names[to]} ${formatMoney(existing.amountMinor, currency)} on ${existing.spentOn}.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text("Delete it if it was recorded by mistake.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            OutlinedTextField(
                title, { title = it.take(80) }, Modifier.fillMaxWidth(),
                label = { Text("Title") }, placeholder = { Text("Dinner") }, singleLine = true,
                isError = showErrors && title.isBlank(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            OutlinedTextField(
                amountText, { amountText = it.take(15) }, Modifier.fillMaxWidth(),
                label = { Text("Amount ($currency)") }, singleLine = true,
                isError = showErrors && amount == null,
                supportingText = { if (showErrors && amount == null) Text("Enter an amount above zero") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            Text("Paid by", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                d.members.forEach { m ->
                    FilterChip(
                        selected = payerId == m.id,
                        onClick = { payerId = m.id },
                        label = { Text(if (m.id == d.myMemberId) "You" else m.name) },
                    )
                }
            }
            Text("Split equally between", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                d.members.forEach { m ->
                    FilterChip(
                        selected = m.id in among,
                        onClick = { among = if (m.id in among) among - m.id else among + m.id },
                        label = { Text(if (m.id == d.myMemberId) "You" else m.name) },
                    )
                }
            }
            val perPerson = if (amount != null && among.isNotEmpty()) {
                "${formatMoney(amount / among.size, currency)} each"
            } else if (among.isEmpty()) "Pick at least one person" else ""
            Text(
                perPerson,
                color = if (among.isEmpty() && showErrors) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = { pickDate = true }, Modifier.fillMaxWidth()) { Text("Date: $date") }
            Button(
                onClick = {
                    showErrors = true
                    if (!valid || amount == null) return@Button
                    vm.run(
                        {
                            saveExpense(
                                groupId = groupId, expenseId = existing?.id, title = title.trim(),
                                amountMinor = amount, payerId = payerId,
                                splits = splitEqually(amount, among), spentOn = date,
                            )
                        },
                        onSuccess = { nav.popBackStack() },
                    )
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }
        }
    }

    if (pickDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    pickDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this ${if (existing.isSettlement) "payment" else "expense"}?") },
            text = { Text("Balances will be updated for everyone in the group.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.run({ deleteExpense(groupId, existing.id) }, onSuccess = { nav.popBackStack() })
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
