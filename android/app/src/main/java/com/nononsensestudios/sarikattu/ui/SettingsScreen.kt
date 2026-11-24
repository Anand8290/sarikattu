package com.nononsensestudios.sarikattu.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nononsensestudios.sarikattu.BuildConfig

private const val SOURCE_URL = "https://github.com/Anand8290/sarikattu"
private const val PRIVACY_URL = "https://anand8290.github.io/sarikattu/privacy-policy"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavHostController) {
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    fun open(url: String) = context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    val topBar = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val gap = Modifier.padding(top = ListItemDefaults.SegmentedGap)

    Scaffold(
        modifier = Modifier.nestedScroll(topBar.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings") },
                navigationIcon = { BackButton { nav.popBackStack() } },
                scrollBehavior = topBar,
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            SectionHeader("Groups")
            SegmentedListItem(
                onClick = { nav.navigate(Routes.join()) },
                shapes = ListItemDefaults.segmentedShapes(0, 1),
                colors = segmentColors(),
                leadingContent = { Icon(Icons.Default.Search, null) },
                supportingContent = { Text("If an invite link didn't open the app") },
            ) { Text("Enter invite code") }

            SectionHeader("About")
            SegmentedListItem(
                onClick = { open(SOURCE_URL) },
                shapes = ListItemDefaults.segmentedShapes(0, 2),
                colors = segmentColors(),
                leadingContent = { Icon(Icons.Default.Info, null) },
                supportingContent = { Text("GitHub · GPL-3.0") },
            ) { Text("Source code") }
            SegmentedListItem(
                onClick = { open(PRIVACY_URL) },
                shapes = ListItemDefaults.segmentedShapes(1, 2),
                colors = segmentColors(),
                modifier = gap,
                leadingContent = { Icon(Icons.Default.Lock, null) },
            ) { Text("Privacy policy") }

            SectionHeader("Your data")
            SegmentedListItem(
                onClick = { confirmDelete = true },
                shapes = ListItemDefaults.segmentedShapes(0, 1),
                colors = segmentColors(),
                leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                supportingContent = { Text("Leave every group and erase this phone's account") },
            ) { Text("Delete my data", color = MaterialTheme.colorScheme.error) }

            Text(
                "SariKattu ${BuildConfig.VERSION_NAME}",
                Modifier.fillMaxWidth().padding(24.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete your data?") },
            text = {
                Text(
                    "You'll leave every group and this phone's anonymous account is deleted. " +
                        "Expenses you added stay visible to the other members. This can't be undone.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.run({ deleteMyData() }, onSuccess = { nav.popBackStack(Routes.GROUPS, inclusive = false) })
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
