package com.anand8290.splitlite.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.anand8290.splitlite.domain.GroupPreview

private const val NEW_MEMBER = "__new__"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinScreen(vm: AppViewModel, nav: NavHostController, initialCode: String?) {
    val busy by vm.busy.collectAsState()
    var code by rememberSaveable { mutableStateOf(initialCode ?: "") }
    var found by rememberSaveable(stateSaver = previewSaver) { mutableStateOf<GroupPreview?>(null) }
    var notFound by rememberSaveable { mutableStateOf(false) }
    var choice by rememberSaveable { mutableStateOf<String?>(null) }
    var newName by rememberSaveable { mutableStateOf("") }

    fun lookUp() {
        notFound = false
        vm.run({ preview(code.trim()).also { found = it; notFound = it == null } })
    }
    LaunchedEffect(initialCode) { if (!initialCode.isNullOrBlank() && found == null) lookUp() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Join group") },
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
            val p = found
            if (p == null) {
                OutlinedTextField(
                    code, { code = it.trim().take(32) }, Modifier.fillMaxWidth(),
                    label = { Text("Invite code") }, singleLine = true,
                    isError = notFound,
                    supportingText = { if (notFound) Text("No group found for that code. Check it and try again.") },
                )
                Button(onClick = ::lookUp, enabled = code.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth()) {
                    Text("Find group")
                }
                return@Column
            }

            Text(p.name, style = MaterialTheme.typography.headlineSmall)
            Text("Which one are you?", style = MaterialTheme.typography.titleSmall)
            p.members.forEach { m ->
                ListItem(
                    modifier = Modifier.selectable(selected = choice == m.id, enabled = !m.claimed) { choice = m.id },
                    headlineContent = { Text(m.name) },
                    supportingContent = { if (m.claimed) Text("Already joined") },
                    leadingContent = { RadioButton(selected = choice == m.id, onClick = null, enabled = !m.claimed) },
                )
            }
            ListItem(
                modifier = Modifier.selectable(selected = choice == NEW_MEMBER) { choice = NEW_MEMBER },
                headlineContent = { Text("I'm not listed") },
                leadingContent = { RadioButton(selected = choice == NEW_MEMBER, onClick = null) },
            )
            if (choice == NEW_MEMBER) {
                OutlinedTextField(
                    newName, { newName = it.take(40) }, Modifier.fillMaxWidth(),
                    label = { Text("Your name") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
            }
            val ready = choice != null && (choice != NEW_MEMBER || newName.isNotBlank())
            Button(
                onClick = {
                    val memberId = choice.takeIf { it != NEW_MEMBER }
                    var groupId = ""
                    vm.run(
                        { groupId = join(code.trim(), memberId, newName.trim().takeIf { memberId == null }) },
                        onSuccess = { nav.openGroup(groupId) },
                    )
                },
                enabled = ready && !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val label = p.members.firstOrNull { it.id == choice }?.name ?: newName.trim().ifEmpty { null }
                Text(if (label != null) "Join as $label" else "Join")
            }
        }
    }
}

private val previewSaver = androidx.compose.runtime.saveable.Saver<GroupPreview?, String>(
    save = { it?.let { p -> kotlinx.serialization.json.Json.encodeToString(GroupPreview.serializer(), p) } },
    restore = { kotlinx.serialization.json.Json.decodeFromString(GroupPreview.serializer(), it) },
)
