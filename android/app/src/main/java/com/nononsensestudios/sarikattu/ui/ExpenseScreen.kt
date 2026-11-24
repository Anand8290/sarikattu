package com.nononsensestudios.sarikattu.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nononsensestudios.sarikattu.domain.GroupDetail
import com.nononsensestudios.sarikattu.domain.Member
import com.nononsensestudios.sarikattu.domain.formatMoney
import com.nononsensestudios.sarikattu.domain.friendlyDate
import com.nononsensestudios.sarikattu.domain.minorToInput
import com.nononsensestudios.sarikattu.domain.parseMoney
import com.nononsensestudios.sarikattu.domain.splitEqually
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Currency
import java.util.Locale

/** Groups up to this size pick people inline; bigger groups use summary rows and sheets. */
private const val INLINE_MAX_MEMBERS = 6

private val AMOUNT_CHARS = Regex("""\d*([.,]\d*)?""")

private enum class Sheet { PAYER, SPLIT, DATE }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpenseScreen(vm: AppViewModel, nav: NavHostController, groupId: String, expenseId: String?) {
    val detail by vm.repo.group(groupId).collectAsState()
    val busy by vm.busy.collectAsState()
    val d = detail ?: return ScreenLoading()
    val existing = expenseId?.let { id -> d.expenses.firstOrNull { it.id == id } }
    val currency = d.group.currency
    val haptic = rememberConfirmHaptic()

    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var amountText by rememberSaveable { mutableStateOf(existing?.let { minorToInput(it.amountMinor, currency) } ?: "") }
    var payerId by rememberSaveable { mutableStateOf(existing?.payerId ?: d.myMemberId ?: d.members.first().id) }
    var among by rememberSaveable {
        mutableStateOf(existing?.splits?.map { it.memberId } ?: d.members.map { it.id })
    }
    var date by rememberSaveable { mutableStateOf(existing?.spentOn ?: LocalDate.now().toString()) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var sheet by rememberSaveable { mutableStateOf<Sheet?>(null) }
    var pickDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val amount = parseMoney(amountText, currency)
    val valid = title.isNotBlank() && amount != null && among.isNotEmpty()
    val shares = if (amount != null && among.isNotEmpty()) {
        splitEqually(amount, among).associate { it.memberId to it.amountMinor }
    } else {
        emptyMap()
    }
    val people = sortedMembers(d)
    fun nameOf(id: String) = if (id == d.myMemberId) "You" else d.members.firstOrNull { it.id == id }?.name ?: "?"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            existing?.isSettlement == true -> "Payment"
                            existing != null -> "Edit expense"
                            else -> "New expense"
                        },
                    )
                },
                navigationIcon = { CloseButton { nav.popBackStack() } },
                actions = {
                    if (existing != null) {
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Delete") }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (existing?.isSettlement == true) {
                // Payments are only recorded or deleted, never edited.
                val to = existing.splits.firstOrNull()?.memberId
                Text(
                    formatMoney(existing.amountMinor, currency),
                    Modifier.fillMaxWidth().padding(top = 24.dp),
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "${nameOf(existing.payerId)} paid ${to?.let(::nameOf) ?: "?"} · ${friendlyDate(existing.spentOn)}",
                    Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "Delete it if it was recorded by mistake.",
                    Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            AmountField(amountText, currency, isError = showErrors && amount == null) {
                // Only digits and one decimal separator; the number keyboard alone doesn't guarantee that.
                if (it.length <= 15 && it.matches(AMOUNT_CHARS)) amountText = it
            }
            if (showErrors && amount == null) {
                Text(
                    "Enter an amount above zero",
                    Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedTextField(
                title, { title = it.take(80) }, Modifier.fillMaxWidth(),
                placeholder = { Text("What was it for?") },
                singleLine = true,
                shape = CircleShape,
                isError = showErrors && title.isBlank(),
                supportingText = { if (showErrors && title.isBlank()) Text("Add a short title, like Dinner") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )

            if (d.members.size <= INLINE_MAX_MEMBERS) {
                InlinePeople(
                    people = people, me = d.myMemberId, payerId = payerId, among = among, shares = shares,
                    currency = currency,
                    onPayer = { payerId = it },
                    onToggle = { id -> among = if (id in among) among - id else among + id },
                )
                SegmentedListItem(
                    onClick = { sheet = Sheet.DATE },
                    shapes = ListItemDefaults.segmentedShapes(0, 1),
                    colors = segmentColors(),
                    leadingContent = { Icon(Icons.Default.DateRange, null) },
                    overlineContent = { Text("Date") },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                ) { Text(friendlyDate(date)) }
            } else {
                SummaryRows(
                    d = d, people = people, payerName = nameOf(payerId), among = among, amount = amount,
                    date = date, currency = currency,
                    onPayer = { sheet = Sheet.PAYER }, onSplit = { sheet = Sheet.SPLIT }, onDate = { sheet = Sheet.DATE },
                )
            }
            if (among.isEmpty()) {
                Text(
                    "Pick at least one person to split with",
                    color = if (showErrors) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(8.dp))
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
                        onSuccess = { haptic(); nav.popBackStack() },
                    )
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text("Save", style = MaterialTheme.typography.titleMedium) }
        }
    }

    when (sheet) {
        Sheet.PAYER -> PayerSheet(
            d = d, people = people, selected = payerId,
            onPick = { payerId = it; sheet = null },
            onDismiss = { sheet = null },
        )
        Sheet.SPLIT -> SplitSheet(
            people = people, me = d.myMemberId, among = among, shares = shares, currency = currency,
            onChange = { among = it },
            onDismiss = { sheet = null },
        )
        Sheet.DATE -> DateSheet(
            date = date,
            onPick = { date = it; sheet = null },
            onPickCustom = { sheet = null; pickDate = true },
            onDismiss = { sheet = null },
        )
        null -> Unit
    }

    if (pickDate) {
        val today = LocalDate.now()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                // No future dates: an expense is always something already paid.
                override fun isSelectableDate(utcTimeMillis: Long) =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(today)

                override fun isSelectableYear(year: Int) = year <= today.year
            },
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

/** You first, then everyone else alphabetically. */
private fun sortedMembers(d: GroupDetail): List<Member> =
    d.members.sortedWith(compareBy<Member> { it.id != d.myMemberId }.thenBy { it.name.lowercase() })

private fun label(m: Member, me: String?) = if (m.id == me) "You" else m.name

/** The amount as the hero of the screen, calculator style, with the currency symbol in front. */
@Composable
private fun AmountField(value: String, currency: String, isError: Boolean, onChange: (String) -> Unit) {
    val symbol = remember(currency) {
        runCatching { Currency.getInstance(currency).getSymbol(Locale.getDefault()) }.getOrDefault(currency)
    }
    val style = MaterialTheme.typography.displayLarge.copy(
        textAlign = TextAlign.Center,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
    )
    BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        textStyle = style,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        visualTransformation = PrefixTransformation(symbol),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center) {
                if (value.isEmpty()) {
                    Text("${symbol}0", style = style.copy(color = MaterialTheme.colorScheme.outlineVariant))
                }
                inner()
            }
        },
    )
}

/** Shows "₹" in front of the typed amount without making it part of the text. */
private class PrefixTransformation(private val prefix: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (text.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = offset + prefix.length
            override fun transformedToOriginal(offset: Int) = (offset - prefix.length).coerceIn(0, text.length)
        }
        return TransformedText(AnnotatedString(prefix) + text, mapping)
    }
}

/** Small groups: tap a name to pick the payer, tap faces to include or leave people out. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
private fun InlinePeople(
    people: List<Member>, me: String?, payerId: String, among: List<String>, shares: Map<String, Long>,
    currency: String, onPayer: (String) -> Unit, onToggle: (String) -> Unit,
) {
    SectionHeader("Paid by", Modifier.padding(top = 0.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        people.forEach { m ->
            ToggleButton(checked = payerId == m.id, onCheckedChange = { onPayer(m.id) }) { Text(label(m, me)) }
        }
    }
    SectionHeader("Split equally between")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        people.forEach { m ->
            val included = m.id in among
            Column(
                Modifier.width(56.dp).clickable { onToggle(m.id) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Avatar(m.name, m.id, size = 48.dp, dimmed = !included)
                Text(label(m, me), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                Text(
                    shares[m.id]?.let { formatMoney(it, currency) } ?: "—",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Big groups: three summary rows that open sheets, so the screen looks the same for 7 or 300 people. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SummaryRows(
    d: GroupDetail, people: List<Member>, payerName: String, among: List<String>, amount: Long?,
    date: String, currency: String, onPayer: () -> Unit, onSplit: () -> Unit, onDate: () -> Unit,
) {
    val chevron: @Composable () -> Unit = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
    val gap = Modifier.padding(top = ListItemDefaults.SegmentedGap)
    Column {
        SegmentedListItem(
            onClick = onPayer,
            shapes = ListItemDefaults.segmentedShapes(0, 3),
            colors = segmentColors(),
            leadingContent = { Icon(Icons.Default.Person, null) },
            overlineContent = { Text("Paid by") },
            trailingContent = chevron,
        ) { Text(payerName) }
        SegmentedListItem(
            onClick = onSplit,
            shapes = ListItemDefaults.segmentedShapes(1, 3),
            colors = segmentColors(),
            modifier = gap,
            leadingContent = { Icon(Icons.Default.Face, null) },
            overlineContent = { Text("Split equally") },
            supportingContent = { StackedAvatars(people.filter { it.id in among }) },
            trailingContent = {
                if (amount != null && among.isNotEmpty()) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatMoney(amount / among.size, currency), style = MaterialTheme.typography.labelLarge)
                        Text("each", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    chevron()
                }
            },
        ) {
            Text(
                when (among.size) {
                    d.members.size -> "Everyone · ${among.size}"
                    1 -> people.first { it.id == among.first() }.let { label(it, d.myMemberId) }
                    else -> "${among.size} of ${d.members.size} people"
                },
            )
        }
        SegmentedListItem(
            onClick = onDate,
            shapes = ListItemDefaults.segmentedShapes(2, 3),
            colors = segmentColors(),
            modifier = gap,
            leadingContent = { Icon(Icons.Default.DateRange, null) },
            overlineContent = { Text("Date") },
            trailingContent = chevron,
        ) { Text(friendlyDate(date)) }
    }
}

/** Three overlapping faces and "+27". */
@Composable
private fun StackedAvatars(members: List<Member>) {
    if (members.isEmpty()) return
    val shown = members.take(3)
    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width((28 + 16 * (shown.size - 1)).dp)) {
            shown.forEachIndexed { i, m ->
                Surface(Modifier.offset(x = (16 * i).dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                    Avatar(m.name, m.id, Modifier.padding(2.dp), size = 24.dp)
                }
            }
        }
        if (members.size > shown.size) {
            Text(
                "+${members.size - shown.size}",
                Modifier.padding(start = 6.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        query, onChange, Modifier.fillMaxWidth(),
        placeholder = { Text("Search names") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        singleLine = true,
        shape = CircleShape,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
    )
}

private fun List<Member>.matching(query: String) =
    if (query.isBlank()) this else filter { it.name.contains(query.trim(), ignoreCase = true) }

/** Who paid: recent payers as one-tap chips, then everyone, searchable. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
private fun PayerSheet(d: GroupDetail, people: List<Member>, selected: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val recentIds = (listOfNotNull(d.myMemberId) +
        d.expenses.filter { !it.isSettlement }.sortedByDescending { it.createdAt }.map { it.payerId })
        .distinct().take(4)
    val recent = recentIds.mapNotNull { id -> people.firstOrNull { it.id == id } }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Who paid?", style = MaterialTheme.typography.titleLarge)
            SearchField(query) { query = it }
            if (query.isBlank() && recent.isNotEmpty()) {
                SectionHeader("Recent", Modifier.padding(top = 0.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    recent.forEach { m ->
                        ToggleButton(checked = selected == m.id, onCheckedChange = { onPick(m.id) }) {
                            Text(label(m, d.myMemberId))
                        }
                    }
                }
                SectionHeader("Everyone")
            }
        }
        LazyColumn(Modifier.fillMaxWidth()) {
            items(people.matching(query), key = { it.id }) { m ->
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onPick(m.id) },
                    leadingContent = { RadioButton(selected = selected == m.id, onClick = null) },
                ) { Text(label(m, d.myMemberId)) }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/** Who shares the cost: search, select all or clear, and each person's share updating as you tick. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitSheet(
    people: List<Member>, me: String?, among: List<String>, shares: Map<String, Long>, currency: String,
    onChange: (List<String>) -> Unit, onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Split between", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        "${among.size} of ${people.size}",
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            SearchField(query) { query = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onChange(people.map { it.id }) }) { Text("Select all") }
                OutlinedButton(onClick = { onChange(emptyList()) }) { Text("Clear") }
            }
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false)) {
            items(people.matching(query), key = { it.id }) { m ->
                val included = m.id in among
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onChange(if (included) among - m.id else among + m.id) },
                    leadingContent = { Checkbox(checked = included, onCheckedChange = null) },
                    trailingContent = {
                        Text(shares[m.id]?.let { formatMoney(it, currency) } ?: "—", style = MaterialTheme.typography.labelLarge)
                    },
                ) { Text(label(m, me)) }
            }
        }
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 52.dp),
        ) { Text("Done") }
    }
}

/** When: Today, Yesterday or 2 days ago in one tap, or the full calendar. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
private fun DateSheet(date: String, onPick: (String) -> Unit, onPickCustom: () -> Unit, onDismiss: () -> Unit) {
    val today = LocalDate.now()
    val quick = listOf("Today" to today, "Yesterday" to today.minusDays(1), "2 days ago" to today.minusDays(2))
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("When was it?", style = MaterialTheme.typography.titleLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quick.forEach { (text, day) ->
                    ToggleButton(checked = date == day.toString(), onCheckedChange = { onPick(day.toString()) }) { Text(text) }
                }
            }
            OutlinedButton(onClick = onPickCustom, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.DateRange, null)
                Text("Pick a date", Modifier.padding(start = 8.dp))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
