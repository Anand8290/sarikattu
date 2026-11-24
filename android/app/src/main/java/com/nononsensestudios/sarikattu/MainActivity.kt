package com.nononsensestudios.sarikattu

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nononsensestudios.sarikattu.ui.AppNav
import com.nononsensestudios.sarikattu.ui.AppViewModel
import com.nononsensestudios.sarikattu.ui.SariKattuTheme

class MainActivity : ComponentActivity() {
    /** Invite code from a sarikattu://join?c=CODE link, consumed by the nav graph. */
    private val pendingJoinCode = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)
        val container = (application as SariKattuApp).container
        setContent {
            SariKattuTheme {
                val vm = viewModel { AppViewModel(container.repository) }
                AppNav(
                    vm = vm,
                    isConfigured = container.isConfigured,
                    pendingJoinCode = pendingJoinCode.value,
                    onJoinCodeConsumed = { pendingJoinCode.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "sarikattu" && uri.host == "join") {
            uri.getQueryParameter("c")?.trim()?.takeIf { it.isNotEmpty() }?.let { pendingJoinCode.value = it }
        }
    }
}
