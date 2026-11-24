package com.nononsensestudios.sarikattu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import java.util.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGroupScreen(vm: AppViewModel, nav: NavHostController) {
    val busy by vm.busy.collectAsState()
    var name by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable {
        mutableStateOf(runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrDefault("INR"))
    }
    var myName by rememberSaveable { mutableStateOf("") }
    val others = rememberSaveable(
        saver = listSaver<SnapshotStateList<String>, String>(save = { it.toList() }, restore = { it.toMutableStateList() }),
    ) { mutableStateListOf("") }
    var showErrors by rememberSaveable { mutableStateOf(false) }

    val currencyValid = runCatching { Currency.getInstance(currency.uppercase()) }.isSuccess
    val valid = name.isNotBlank() && myName.isNotBlank() && currencyValid

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New group") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.Close, "Close") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                name, { name = it.take(80) }, Modifier.fillMaxWidth(),
                label = { Text("Group name") }, placeholder = { Text("Goa trip") }, singleLine = true,
                shape = FieldShape, textStyle = MaterialTheme.typography.titleLarge,
                isError = showErrors && name.isBlank(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            OutlinedTextField(
                currency, { currency = it.uppercase().take(3) }, Modifier.fillMaxWidth(),
                label = { Text("Currency code") }, singleLine = true, shape = FieldShape,
                isError = !currencyValid,
                supportingText = { if (!currencyValid) Text("Use a 3-letter code like INR or USD") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            )
            SectionHeader("Members")
            OutlinedTextField(
                myName, { myName = it.take(40) }, Modifier.fillMaxWidth(),
                label = { Text("Your name") }, singleLine = true, shape = FieldShape,
                isError = showErrors && myName.isBlank(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            others.forEachIndexed { i, member ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        member, { others[i] = it.take(40) }, Modifier.weight(1f),
                        label = { Text("Member ${i + 2}") }, singleLine = true, shape = FieldShape,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    )
                    IconButton(onClick = { others.removeAt(i) }) { Icon(Icons.Default.Close, "Remove member") }
                }
            }
            TextButton(onClick = { others.add("") }) {
                Icon(Icons.Default.Add, null)
                Text("Add member")
            }
            Text(
                "Friends who aren't listed can add themselves when they open the invite link.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    showErrors = true
                    if (!valid) return@Button
                    val names = listOf(myName.trim()) + others.map { it.trim() }.filter { it.isNotEmpty() }
                    var newId = ""
                    vm.run(
                        { newId = createGroup(name.trim(), currency.uppercase(), names, myIndex = 0) },
                        onSuccess = { nav.openGroup(newId) },
                    )
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text("Create group", style = MaterialTheme.typography.titleMedium) }
        }
    }
}
