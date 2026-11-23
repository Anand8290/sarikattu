package com.anand8290.splitlite.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.anand8290.splitlite.BuildConfig

private const val SOURCE_URL = "https://github.com/Anand8290/splitlite"
private const val PRIVACY_URL = "https://anand8290.github.io/splitlite/privacy-policy"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavHostController) {
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    fun open(url: String) = context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            ListItem(
                modifier = Modifier.clickable { nav.navigate(Routes.join()) },
                content = { Text("Enter invite code") },
                supportingContent = { Text("If an invite link didn't open the app") },
            )
            HorizontalDivider()
            ListItem(
                modifier = Modifier.clickable { open(SOURCE_URL) },
                content = { Text("Source code") },
                supportingContent = { Text("GitHub · GPL-3.0") },
            )
            HorizontalDivider()
            ListItem(
                modifier = Modifier.clickable { open(PRIVACY_URL) },
                content = { Text("Privacy policy") },
            )
            HorizontalDivider()
            ListItem(
                modifier = Modifier.clickable { confirmDelete = true },
                content = { Text("Delete my data", color = MaterialTheme.colorScheme.error) },
                supportingContent = { Text("Leave every group and erase this phone's account") },
            )
            HorizontalDivider()
            Text(
                "SplitLite ${BuildConfig.VERSION_NAME}",
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
