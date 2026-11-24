package com.nononsensestudios.sarikattu.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

object Routes {
    const val GROUPS = "groups"
    const val NEW_GROUP = "new-group"
    const val GROUP = "group/{groupId}"
    const val EXPENSE = "expense/{groupId}?expenseId={expenseId}"
    const val BALANCES = "balances/{groupId}"
    const val JOIN = "join?code={code}"
    const val SETTINGS = "settings"

    fun group(id: String) = "group/$id"
    fun expense(groupId: String, expenseId: String? = null) =
        "expense/$groupId" + (expenseId?.let { "?expenseId=$it" } ?: "")
    fun balances(groupId: String) = "balances/$groupId"
    fun join(code: String? = null) = "join" + (code?.let { "?code=${Uri.encode(it)}" } ?: "")
}

/** Opens a group with the groups list as the only screen behind it. */
fun NavHostController.openGroup(id: String) = navigate(Routes.group(id)) {
    popUpTo(Routes.GROUPS)
}

@Composable
fun AppNav(
    vm: AppViewModel,
    isConfigured: Boolean,
    pendingJoinCode: String?,
    onJoinCodeConsumed: () -> Unit,
) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val error by vm.error.collectAsState()

    LaunchedEffect(error) {
        error?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
    }
    LaunchedEffect(pendingJoinCode) {
        pendingJoinCode?.let {
            nav.navigate(Routes.join(it))
            onJoinCodeConsumed()
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(nav, startDestination = Routes.GROUPS) {
            composable(Routes.GROUPS) { GroupsScreen(vm, nav, isConfigured) }
            composable(Routes.NEW_GROUP) { NewGroupScreen(vm, nav) }
            composable(Routes.GROUP, arguments = listOf(navArgument("groupId") { type = NavType.StringType })) {
                GroupScreen(vm, nav, it.arguments!!.getString("groupId")!!)
            }
            composable(
                Routes.EXPENSE,
                arguments = listOf(
                    navArgument("groupId") { type = NavType.StringType },
                    navArgument("expenseId") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) {
                ExpenseScreen(vm, nav, it.arguments!!.getString("groupId")!!, it.arguments?.getString("expenseId"))
            }
            composable(Routes.BALANCES, arguments = listOf(navArgument("groupId") { type = NavType.StringType })) {
                BalancesScreen(vm, nav, it.arguments!!.getString("groupId")!!)
            }
            composable(
                Routes.JOIN,
                arguments = listOf(navArgument("code") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) {
                JoinScreen(vm, nav, it.arguments?.getString("code"))
            }
            composable(Routes.SETTINGS) { SettingsScreen(vm, nav) }
        }
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 80.dp),
        )
    }
}
