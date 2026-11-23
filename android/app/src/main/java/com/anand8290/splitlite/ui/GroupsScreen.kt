package com.anand8290.splitlite.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.anand8290.splitlite.domain.GroupSummary
import com.anand8290.splitlite.domain.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(vm: AppViewModel, nav: NavHostController, isConfigured: Boolean) {
    val groups by vm.repo.groups.collectAsState()
    val busy by vm.busy.collectAsState()

    LaunchedEffect(Unit) { if (isConfigured) vm.run({ refreshGroups() }) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SplitLite") },
                actions = {
                    IconButton(onClick = { nav.navigate(Routes.SETTINGS) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate(Routes.NEW_GROUP) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New group") },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = busy,
            onRefresh = { vm.run({ refreshGroups() }) },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            LazyColumn(Modifier.fillMaxSize()) {
                if (!isConfigured) item { NotConfiguredCard() }
                if (groups.isEmpty()) item { EmptyGroups() }
                items(groups, key = { it.id }) { group ->
                    GroupRow(group) { nav.navigate(Routes.group(group.id)) }
                    HorizontalDivider()
                }
                item {
                    TextButton(
                        onClick = { nav.navigate(Routes.join()) },
                        modifier = Modifier.padding(8.dp),
                    ) { Text("Have an invite code? Join a group") }
                    Spacer(Modifier.height(88.dp))
                }
            }
        }
    }
}

@Composable
private fun GroupRow(group: GroupSummary, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        content = { Text(group.name) },
        supportingContent = { Text("${group.memberCount} members") },
        trailingContent = { BalanceLabel(group.myBalance, group.currency) },
    )
}

@Composable
fun BalanceLabel(balance: Long, currency: String) {
    val text = when {
        balance > 0 -> "you're owed ${formatMoney(balance, currency)}"
        balance < 0 -> "you owe ${formatMoney(-balance, currency)}"
        else -> "settled up"
    }
    Text(text, color = balanceColor(balance), style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun EmptyGroups() {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Start your first group", style = MaterialTheme.typography.titleMedium)
        Text(
            "Create a group for a trip, your flat or a dinner, then share the invite link.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NotConfiguredCard() {
    Card(
        Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Text(
            "No server configured. Add SUPABASE_URL and SUPABASE_ANON_KEY to android/local.properties " +
                "and rebuild. See README.md.",
            Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}
