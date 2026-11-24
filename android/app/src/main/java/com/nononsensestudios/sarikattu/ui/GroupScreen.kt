package com.nononsensestudios.sarikattu.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nononsensestudios.sarikattu.BuildConfig
import com.nononsensestudios.sarikattu.domain.Expense
import com.nononsensestudios.sarikattu.domain.GroupDetail
import com.nononsensestudios.sarikattu.domain.computeBalances
import com.nononsensestudios.sarikattu.domain.formatMoney
import com.nononsensestudios.sarikattu.domain.friendlyDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GroupScreen(vm: AppViewModel, nav: NavHostController, groupId: String) {
    val detail by vm.repo.group(groupId).collectAsState()
    val busy by vm.busy.collectAsState()
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    val topBar = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val toolbar = FloatingToolbarDefaults.exitAlwaysScrollBehavior(exitDirection = FloatingToolbarExitDirection.Bottom)
    val refresh = rememberPullToRefreshState()

    LaunchedEffect(groupId) { vm.run({ refreshGroup(groupId) }) }

    Scaffold(
        modifier = Modifier.nestedScroll(topBar.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(detail?.group?.name ?: "") },
                navigationIcon = { BackButton { nav.popBackStack() } },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "More") }
                        DropdownMenu(menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Leave group") },
                                onClick = { menuOpen = false; confirmLeave = true },
                            )
                        }
                    }
                },
                scrollBehavior = topBar,
            )
        },
    ) { padding ->
        val d = detail
        if (d == null) {
            ScreenLoading(Modifier.padding(padding))
            return@Scaffold
        }
        Box(Modifier.fillMaxSize().padding(padding)) {
            PullToRefreshBox(
                isRefreshing = busy,
                onRefresh = { vm.run({ refreshGroup(groupId) }) },
                modifier = Modifier.fillMaxSize(),
                state = refresh,
                indicator = {
                    PullToRefreshDefaults.LoadingIndicator(
                        state = refresh,
                        isRefreshing = busy,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                },
            ) {
                LazyColumn(
                    Modifier.fillMaxSize().nestedScroll(toolbar),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    item { GroupHero(d) { nav.navigate(Routes.balances(groupId)) } }
                    val sorted = d.expenses.sortedWith(
                        compareByDescending<Expense> { it.spentOn }.thenByDescending { it.createdAt },
                    )
                    if (sorted.isEmpty()) {
                        item {
                            EmptyState(
                                title = "No expenses yet",
                                body = "Add the first one with the + button. Everyone in the group sees it.",
                            )
                        }
                    }
                    sorted.groupBy { it.spentOn }.forEach { (date, dayExpenses) ->
                        item(key = "h-$date") { SectionHeader(friendlyDate(date)) }
                        itemsIndexed(dayExpenses, key = { _, e -> e.id }) { i, e ->
                            ExpenseRow(
                                e, d,
                                index = i, count = dayExpenses.size,
                            ) { nav.navigate(Routes.expense(groupId, e.id)) }
                        }
                    }
                    item { Spacer(Modifier.height(120.dp)) }
                }
            }
            HorizontalFloatingToolbar(
                expanded = true,
                floatingActionButton = {
                    FloatingToolbarDefaults.VibrantFloatingActionButton(
                        onClick = { nav.navigate(Routes.expense(groupId)) },
                    ) { Icon(Icons.Default.Add, "Add expense") }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .offset(y = (-16).dp),
                scrollBehavior = toolbar,
            ) {
                IconButton(onClick = { context.startActivity(shareInvite(d)) }) {
                    Icon(Icons.Default.Share, "Share invite")
                }
                TextButton(onClick = { nav.navigate(Routes.balances(groupId)) }) {
                    Icon(Icons.Default.Check, null)
                    Text("Settle up", Modifier.padding(start = 8.dp))
                }
            }
        }
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Leave this group?") },
            text = { Text("You'll stop seeing it on this phone. Expenses stay for the others. You can rejoin with the invite link.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLeave = false
                    vm.run({ leaveGroup(groupId) }, onSuccess = { nav.popBackStack(Routes.GROUPS, inclusive = false) })
                }) { Text("Leave") }
            },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun GroupHero(d: GroupDetail, onBalances: () -> Unit) {
    val mine = d.myMemberId?.let { computeBalances(d.members, d.expenses)[it] } ?: 0L
    val currency = d.group.currency
    BalanceHero(mine, Modifier.padding(top = 8.dp)) {
        Text(peopleCount(d.members.size), style = MaterialTheme.typography.labelLarge)
        Text(
            when {
                mine > 0 -> "You're owed ${formatMoney(mine, currency)}"
                mine < 0 -> "You owe ${formatMoney(-mine, currency)}"
                else -> "You're settled up"
            },
            style = MaterialTheme.typography.headlineSmall,
        )
        FilledTonalButton(onClick = onBalances, modifier = Modifier.padding(top = 8.dp)) { Text("See balances") }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpenseRow(e: Expense, d: GroupDetail, index: Int, count: Int, onClick: () -> Unit) {
    val names = d.members.associate { it.id to it.name }
    val currency = d.group.currency
    val payer = if (e.payerId == d.myMemberId) "You" else names[e.payerId] ?: "Someone"
    val shapes = ListItemDefaults.segmentedShapes(index, count)
    val gap = Modifier.padding(top = if (index > 0) ListItemDefaults.SegmentedGap else 0.dp)

    if (e.isSettlement) {
        val to = e.splits.firstOrNull()?.memberId
        val toName = if (to == d.myMemberId) "you" else names[to] ?: "someone"
        SegmentedListItem(
            onClick = onClick,
            shapes = shapes,
            colors = segmentColors(),
            modifier = gap,
            leadingContent = {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            supportingContent = { Text("Payment") },
            trailingContent = { Text(formatMoney(e.amountMinor, currency), style = MaterialTheme.typography.labelLarge) },
        ) { Text("$payer paid $toName") }
        return
    }

    val myShare = e.splits.firstOrNull { it.memberId == d.myMemberId }?.amountMinor ?: 0L
    val effect = (if (e.payerId == d.myMemberId) e.amountMinor else 0L) - myShare
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        colors = segmentColors(),
        modifier = gap,
        leadingContent = { Avatar(names[e.payerId] ?: "?", e.payerId) },
        supportingContent = { Text("$payer paid ${formatMoney(e.amountMinor, currency)}") },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    when {
                        effect > 0 -> "you lent"
                        effect < 0 -> "you owe"
                        else -> "not involved"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = balanceColor(effect),
                )
                if (effect != 0L) {
                    Text(
                        formatMoney(kotlin.math.abs(effect), currency),
                        style = MaterialTheme.typography.labelLarge,
                        color = balanceColor(effect),
                        textAlign = TextAlign.End,
                    )
                }
            }
        },
    ) { Text(e.title) }
}

private fun shareInvite(d: GroupDetail): Intent {
    val code = d.group.inviteCode
    val text = "Join \"${d.group.name}\" on SariKattu: ${BuildConfig.JOIN_URL}?c=$code\n" +
        "Or open SariKattu and enter the code $code"
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    return Intent.createChooser(send, "Share invite link")
}
