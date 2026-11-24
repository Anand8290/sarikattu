package com.nononsensestudios.sarikattu.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nononsensestudios.sarikattu.domain.GroupSummary
import com.nononsensestudios.sarikattu.domain.formatMoney

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GroupsScreen(vm: AppViewModel, nav: NavHostController, isConfigured: Boolean) {
    val groups by vm.repo.groups.collectAsState()
    val busy by vm.busy.collectAsState()
    val listState = rememberLazyListState()
    val topBar = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val refresh = rememberPullToRefreshState()

    LaunchedEffect(Unit) { if (isConfigured) vm.run({ refreshGroups() }) }

    Scaffold(
        modifier = Modifier.nestedScroll(topBar.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("SariKattu") },
                actions = {
                    IconButton(onClick = { nav.navigate(Routes.SETTINGS) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                scrollBehavior = topBar,
            )
        },
        floatingActionButton = {
            if (groups.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { nav.navigate(Routes.NEW_GROUP) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New group") },
                    expanded = listState.isScrollingUp(),
                )
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = busy,
            onRefresh = { vm.run({ refreshGroups() }) },
            modifier = Modifier.fillMaxSize().padding(padding),
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
                Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                if (!isConfigured) item { NotConfiguredCard() }
                if (groups.isEmpty()) {
                    item {
                        EmptyState(
                            title = "Start your first group",
                            body = "Make a group for a trip, your flat or a dinner, then share the invite with everyone.",
                        ) {
                            Button(onClick = { nav.navigate(Routes.NEW_GROUP) }) {
                                Icon(Icons.Default.Add, null)
                                Text("New group", Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                } else {
                    item { OverviewCard(groups) }
                    item { SectionHeader("Your groups") }
                    itemsIndexed(groups, key = { _, g -> g.id }) { i, group ->
                        SegmentedListItem(
                            onClick = { nav.navigate(Routes.group(group.id)) },
                            shapes = ListItemDefaults.segmentedShapes(i, groups.size),
                            colors = segmentColors(),
                            modifier = Modifier.padding(top = if (i > 0) ListItemDefaults.SegmentedGap else 0.dp),
                            leadingContent = { GroupBadge(group.name, group.id) },
                            supportingContent = { Text(peopleCount(group.memberCount)) },
                            trailingContent = { BalancePill(group.myBalance, group.currency) },
                        ) { Text(group.name) }
                    }
                }
                item {
                    TextButton(
                        onClick = { nav.navigate(Routes.join()) },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    ) { Text("Have an invite code? Join a group") }
                    Spacer(Modifier.height(96.dp))
                }
            }
        }
    }
}

/** The one number people open the app for: what they're owed or owe across every group. */
@Composable
private fun OverviewCard(groups: List<GroupSummary>) {
    val totals = groups.groupBy { it.currency }
        .mapValues { (_, gs) -> gs.sumOf { it.myBalance } }
        .filterValues { it != 0L }
    val across = "across ${groups.size} ${if (groups.size == 1) "group" else "groups"}"
    when {
        totals.isEmpty() -> BalanceHero(0) {
            Text("Overall", style = MaterialTheme.typography.labelLarge)
            Text("You're all settled up", style = MaterialTheme.typography.headlineSmall)
        }
        totals.size == 1 -> {
            val (currency, total) = totals.entries.first()
            BalanceHero(total) {
                Text(
                    if (total > 0) "Overall, you're owed" else "Overall, you owe",
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(formatMoney(kotlin.math.abs(total), currency), style = MaterialTheme.typography.displaySmall)
                Text(across, style = MaterialTheme.typography.bodyMedium)
            }
        }
        // Different currencies can't be added together, so list each one.
        else -> BalanceHero(0) {
            Text("Overall", style = MaterialTheme.typography.labelLarge)
            totals.forEach { (currency, total) ->
                Text(
                    (if (total > 0) "You're owed " else "You owe ") + formatMoney(kotlin.math.abs(total), currency),
                    style = MaterialTheme.typography.titleLarge,
                    color = balanceColor(total),
                )
            }
            Text(across, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun NotConfiguredCard() {
    Surface(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = FieldShape,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Text(
            "No server configured. Add SUPABASE_URL and SUPABASE_ANON_KEY to android/local.properties " +
                "and rebuild. See README.md.",
            Modifier.padding(16.dp),
        )
    }
}
