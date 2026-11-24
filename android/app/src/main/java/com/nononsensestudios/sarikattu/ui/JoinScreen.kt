package com.nononsensestudios.sarikattu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nononsensestudios.sarikattu.domain.GroupPreview

private const val NEW_MEMBER = "__new__"

/** Show a search box on "Which one are you?" once the list gets longer than this. */
private const val SEARCH_FROM = 9

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JoinScreen(vm: AppViewModel, nav: NavHostController, initialCode: String?) {
    val busy by vm.busy.collectAsState()
    val haptic = rememberConfirmHaptic()
    var code by rememberSaveable { mutableStateOf(initialCode ?: "") }
    var found by rememberSaveable(stateSaver = previewSaver) { mutableStateOf<GroupPreview?>(null) }
    var notFound by rememberSaveable { mutableStateOf(false) }
    var choice by rememberSaveable { mutableStateOf<String?>(null) }
    var newName by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }

    fun lookUp() {
        notFound = false
        vm.run({ preview(code.trim()).also { found = it; notFound = it == null } })
    }
    LaunchedEffect(initialCode) { if (!initialCode.isNullOrBlank() && found == null) lookUp() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Join group") },
                navigationIcon = { CloseButton { nav.popBackStack() } },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val p = found
            if (p == null) {
                Text(
                    "Enter the code from your invite. It's the last 12 characters of the link.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    code, { code = it.trim().take(32) }, Modifier.fillMaxWidth(),
                    label = { Text("Invite code") }, singleLine = true,
                    shape = FieldShape,
                    textStyle = MaterialTheme.typography.titleLarge,
                    isError = notFound,
                    supportingText = { if (notFound) Text("No group found for that code. Check it and try again.") },
                )
                Button(
                    onClick = ::lookUp,
                    enabled = code.isNotBlank() && !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) { Text("Find group") }
                return@Column
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                GroupBadge(p.name, p.id, size = 56.dp)
                Column(Modifier.padding(start = 16.dp)) {
                    Text(p.name, style = MaterialTheme.typography.headlineSmall)
                    Text(peopleCount(p.members.size), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            SectionHeader("Which one are you?")
            if (p.members.size >= SEARCH_FROM) {
                OutlinedTextField(
                    query, { query = it }, Modifier.fillMaxWidth(),
                    placeholder = { Text("Search names") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    shape = CircleShape,
                )
            }
            val shown = p.members.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
            val count = shown.size + 1 // plus "I'm not listed"
            Column {
                shown.forEachIndexed { i, m ->
                    SegmentedListItem(
                        selected = choice == m.id,
                        onClick = { choice = m.id },
                        shapes = ListItemDefaults.segmentedShapes(i, count),
                        colors = segmentColors(),
                        modifier = Modifier.padding(top = if (i > 0) ListItemDefaults.SegmentedGap else 0.dp),
                        enabled = !m.claimed,
                        leadingContent = { RadioButton(selected = choice == m.id, onClick = null, enabled = !m.claimed) },
                        supportingContent = { if (m.claimed) Text("Already joined") },
                    ) { Text(m.name) }
                }
                SegmentedListItem(
                    selected = choice == NEW_MEMBER,
                    onClick = { choice = NEW_MEMBER },
                    shapes = ListItemDefaults.segmentedShapes(count - 1, count),
                    colors = segmentColors(),
                    modifier = Modifier.padding(top = if (count > 1) ListItemDefaults.SegmentedGap else 0.dp),
                    leadingContent = { RadioButton(selected = choice == NEW_MEMBER, onClick = null) },
                ) { Text("I'm not listed") }
            }
            if (choice == NEW_MEMBER) {
                OutlinedTextField(
                    newName, { newName = it.take(40) }, Modifier.fillMaxWidth(),
                    label = { Text("Your name") }, singleLine = true,
                    shape = FieldShape,
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
                        onSuccess = { haptic(); nav.openGroup(groupId) },
                    )
                },
                enabled = ready && !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
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
