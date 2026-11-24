package com.nononsensestudios.sarikattu.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Teal = Color(0xFF0F6E56)
private val TealLight = Color(0xFF5DCAA5)

@Composable
fun SariKattuTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme(primary = TealLight, secondary = TealLight)
        else -> lightColorScheme(primary = Teal, secondary = Teal)
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

/** Green for "you are owed", red for "you owe". */
@Composable
fun balanceColor(amountMinor: Long): Color = when {
    amountMinor > 0 -> if (isSystemInDarkTheme()) TealLight else Teal
    amountMinor < 0 -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
