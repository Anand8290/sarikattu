package com.nononsensestudios.sarikattu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.maxLengthTrim
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import java.util.Currency
import java.util.Locale

/** Shown first in the currency list; everything else follows alphabetically. */
private val POPULAR_CURRENCIES = listOf("INR", "USD", "EUR", "GBP", "AED", "SGD", "AUD", "CAD", "JPY")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewGroupScreen(vm: AppViewModel, nav: NavHostController) {
    val busy by vm.busy.collectAsState()
    val haptic = rememberConfirmHaptic()
    val nameState = rememberTextFieldState()
    var currency by rememberSaveable {
        mutableStateOf(runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrDefault("INR"))
    }
    val myNameState = rememberTextFieldState()
    val newMemberState = rememberTextFieldState()
    val name = nameState.text.toString()
    val myName = myNameState.text.toString()
    val newMember = newMemberState.text.toString()
    val others = rememberSaveable(
        saver = listSaver<SnapshotStateList<String>, String>(save = { it.toList() }, restore = { it.toMutableStateList() }),
    ) { mutableStateListOf() }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var pickCurrency by rememberSaveable { mutableStateOf(false) }

    val valid = name.isNotBlank() && myName.isNotBlank()

    fun addMember() {
        val n = newMember.trim()
        if (n.isNotEmpty()) others.add(n)
        newMemberState.clearText()
    }

    fun create() {
        showErrors = true
        if (!valid) return
        // A name typed but not yet added still counts.
        val names = listOf(myName.trim()) + (others + newMember).map { it.trim() }.filter { it.isNotEmpty() }
        var newId = ""
        vm.run(
            { newId = createGroup(name.trim(), currency, names, myIndex = 0) },
            onSuccess = { haptic(); nav.openGroup(newId) },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New group") },
                navigationIcon = { CloseButton { nav.popBackStack() } },
                actions = {
                    Button(onClick = ::create, enabled = !busy, modifier = Modifier.padding(end = 8.dp)) { Text("Create") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            GroupCard(
                nameState = nameState,
                currency = currency,
                onCurrency = { pickCurrency = true },
                isError = showErrors && name.isBlank(),
            )
            if (showErrors && name.isBlank()) {
                Text(
                    "Give the group a name",
                    Modifier.padding(start = 8.dp, top = 4.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            SectionHeader("Members · ${1 + others.size}")
            val count = 2 + others.size // you, the others, and the add row
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                SegmentedListItem(
                    shapes = ListItemDefaults.segmentedShapes(0, count),
                    colors = segmentColors(),
                    leadingContent = { Avatar(myName.ifBlank { "?" }, "me") },
                    overlineContent = { Text("You") },
                    supportingContent = if (showErrors && myName.isBlank()) {
                        { Text("Add your name so others know it's you", color = MaterialTheme.colorScheme.error) }
                    } else {
                        null
                    },
                ) {
                    InlineField(myNameState, "Your name", maxLength = 40)
                }
                others.forEachIndexed { i, member ->
                    SegmentedListItem(
                        shapes = ListItemDefaults.segmentedShapes(i + 1, count),
                        colors = segmentColors(),
                        leadingContent = { Avatar(member, "m$i-$member") },
                        trailingContent = {
                            IconButton(onClick = { others.removeAt(i) }) { Icon(Icons.Default.Close, "Remove $member") }
                        },
                    ) { Text(member) }
                }
                SegmentedListItem(
                    shapes = ListItemDefaults.segmentedShapes(count - 1, count),
                    colors = segmentColors(),
                    leadingContent = {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSecondaryContainer) }
                    },
                    trailingContent = {
                        if (newMember.isNotBlank()) {
                            IconButton(onClick = ::addMember) { Icon(Icons.Default.Check, "Add member") }
                        }
                    },
                ) {
                    InlineField(newMemberState, "Add a member", maxLength = 40, onDone = ::addMember)
                }
            }
            Text(
                "Friends who aren't listed can add themselves when they open the invite link.",
                Modifier.padding(start = 8.dp, top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (pickCurrency) {
        CurrencySheet(
            selected = currency,
            onPick = { currency = it; pickCurrency = false },
            onDismiss = { pickCurrency = false },
        )
    }
}

/** The group's name, big, next to a live preview of its badge, with the currency as a chip. */
@Composable
private fun GroupCard(nameState: TextFieldState, currency: String, onCurrency: () -> Unit, isError: Boolean) {
    val name = nameState.text.toString()
    val onCard = MaterialTheme.colorScheme.onSecondaryContainer
    Surface(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = onCard,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GroupBadge(
                    name.ifBlank { "?" }, name.trim().ifBlank { "new" }, size = 56.dp,
                    container = MaterialTheme.colorScheme.surface, content = MaterialTheme.colorScheme.onSurface,
                )
                Column(Modifier.padding(start = 16.dp).weight(1f)) {
                    Text("Group name", style = MaterialTheme.typography.labelLarge)
                    InlineField(
                        nameState, "Goa trip", maxLength = 80,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = if (isError) MaterialTheme.colorScheme.error else onCard,
                        ),
                        capitalization = KeyboardCapitalization.Sentences,
                    )
                }
            }
            Surface(onClick = onCurrency, shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)) {
                Row(
                    Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${symbolOf(currency)}  $currency · ${displayName(currency)}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Icon(Icons.Default.ArrowDropDown, "Change currency")
                }
            }
        }
    }
}

/** A borderless text field for use inside cards and list tiles. */
@Composable
private fun InlineField(
    state: TextFieldState,
    placeholder: String,
    maxLength: Int,
    style: TextStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Words,
    onDone: (() -> Unit)? = null,
) {
    BasicTextField(
        state = state,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        textStyle = style,
        lineLimits = TextFieldLineLimits.SingleLine,
        inputTransformation = InputTransformation.maxLengthTrim(maxLength),
        cursorBrush = SolidColor(if (style.color == Color.Unspecified) MaterialTheme.colorScheme.primary else style.color),
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            imeAction = if (onDone != null) ImeAction.Done else ImeAction.Next,
        ),
        // Enter on "Add a member" adds the name; elsewhere it moves to the next field as usual.
        onKeyboardAction = { default -> if (onDone != null) onDone() else default() },
        decorator = { inner ->
            Box {
                if (state.text.isEmpty()) Text(placeholder, style = style.copy(color = style.color.copy(alpha = 0.45f)))
                inner()
            }
        },
    )
}

private fun symbolOf(code: String) =
    runCatching { Currency.getInstance(code).getSymbol(Locale.getDefault()) }.getOrDefault(code)

private fun displayName(code: String) =
    runCatching { Currency.getInstance(code).getDisplayName(Locale.getDefault()) }.getOrDefault(code)

/** Every currency, popular ones first, searchable by code or name. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencySheet(selected: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val all = remember {
        val codes = Currency.getAvailableCurrencies().map { it.currencyCode }.toSet()
        POPULAR_CURRENCIES.filter { it in codes } + (codes - POPULAR_CURRENCIES.toSet()).sortedBy { displayName(it) }
    }
    val shown = if (query.isBlank()) all else all.filter {
        it.contains(query.trim(), ignoreCase = true) || displayName(it).contains(query.trim(), ignoreCase = true)
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberFullSheetState()) {
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Currency", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                query, { query = it }, Modifier.fillMaxWidth(),
                placeholder = { Text("Search, like rupee or USD") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = CircleShape,
            )
        }
        LazyColumn(Modifier.fillMaxWidth()) {
            items(shown, key = { it }) { code ->
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onPick(code) },
                    leadingContent = {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center,
                        ) { Text(symbolOf(code).take(3), style = MaterialTheme.typography.labelLarge) }
                    },
                    supportingContent = { Text(displayName(code)) },
                    trailingContent = { if (code == selected) Icon(Icons.Default.Check, "Selected") },
                ) { Text(code) }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
