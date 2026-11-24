package com.nononsensestudios.sarikattu.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nononsensestudios.sarikattu.BuildConfig
import com.nononsensestudios.sarikattu.domain.Expense
import com.nononsensestudios.sarikattu.domain.GroupDetail
import com.nononsensestudios.sarikattu.domain.computeBalances
import com.nononsensestudios.sarikattu.domain.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(vm: AppViewModel, nav: NavHostController, groupId: String) {
    val detail by vm.repo.group(groupId).collectAsState()
    val busy by vm.busy.collectAsState()
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) { vm.run({ refreshGroup(groupId) }) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.group?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    detail?.let { d ->
                        IconButton(onClick = { context.startActivity(shareInvite(d)) }) {
                            Icon(Icons.Default.Share, "Share invite link")
                        }
                    }
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
            )
        },
        floatingActionButton = {
            if (detail != null) {
                ExtendedFloatingActionButton(
                    onClick = { nav.navigate(Routes.expense(groupId)) },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Expense") },
                )
            }
        },
    ) { padding ->
        val d = detail
        if (d == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        PullToRefreshBox(
            isRefreshing = busy,
            onRefresh = { vm.run({ refreshGroup(groupId) }) },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            LazyColumn(Modifier.fillMaxSize()) {
                item { BalanceCard(d) { nav.navigate(Routes.balances(groupId)) } }
                if (d.expenses.isEmpty()) {
                    item {
                        Text(
                            "Add your first expense with the button below.",
                            Modifier.padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(d.expenses, key = { it.id }) { e ->
                    ExpenseRow(e, d) { nav.navigate(Routes.expense(groupId, e.id)) }
                    HorizontalDivider()
                }
                item { Spacer(Modifier.height(88.dp)) }
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
private fun BalanceCard(d: GroupDetail, onBalances: () -> Unit) {
    val mine = d.myMemberId?.let { computeBalances(d.members, d.expenses)[it] } ?: 0L
    Card(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Your balance", style = MaterialTheme.typography.labelMedium)
                BalanceLabel(mine, d.group.currency)
            }
            FilledTonalButton(onClick = onBalances) { Text("Balances") }
        }
    }
}

@Composable
private fun ExpenseRow(e: Expense, d: GroupDetail, onClick: () -> Unit) {
    val names = d.members.associate { it.id to it.name }
    val currency = d.group.currency
    val payer = if (e.payerId == d.myMemberId) "You" else names[e.payerId] ?: "Someone"
    if (e.isSettlement) {
        val to = e.splits.firstOrNull()?.memberId
        val toName = if (to == d.myMemberId) "you" else names[to] ?: "someone"
        ListItem(
            modifier = Modifier.clickable(onClick = onClick),
            content = { Text("$payer paid $toName") },
            supportingContent = { Text("Payment · ${e.spentOn}") },
            trailingContent = { Text(formatMoney(e.amountMinor, currency)) },
        )
        return
    }
    val myShare = e.splits.firstOrNull { it.memberId == d.myMemberId }?.amountMinor ?: 0L
    val effect = (if (e.payerId == d.myMemberId) e.amountMinor else 0L) - myShare
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        content = { Text(e.title) },
        supportingContent = { Text("$payer paid ${formatMoney(e.amountMinor, currency)} · ${e.spentOn}") },
        trailingContent = {
            val text = when {
                effect > 0 -> "you lent ${formatMoney(effect, currency)}"
                effect < 0 -> "you owe ${formatMoney(-effect, currency)}"
                else -> "not involved"
            }
            Text(text, color = balanceColor(effect), style = MaterialTheme.typography.bodyMedium)
        },
    )
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
