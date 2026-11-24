package com.nononsensestudios.sarikattu.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Teal = Color(0xFF0F6E56)
private val TealLight = Color(0xFF5DCAA5)
private val OwedContainerLight = Color(0xFFD5F2E7)
private val OwedContainerDark = Color(0xFF0B3D31)
private val OnOwedContainerLight = Color(0xFF085041)
private val OnOwedContainerDark = Color(0xFF9FE1CB)

/** Material 3 Expressive: wallpaper colours on Android 12+, springy motion everywhere. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    MaterialExpressiveTheme(colorScheme = scheme, motionScheme = MotionScheme.expressive(), content = content)
}

/** Green for "you are owed", red for "you owe". Fixed colours, so they mean the same on every wallpaper. */
@Composable
fun balanceColor(amountMinor: Long): Color = when {
    amountMinor > 0 -> if (isSystemInDarkTheme()) TealLight else Teal
    amountMinor < 0 -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun balanceContainer(amountMinor: Long): Color = when {
    amountMinor > 0 -> if (isSystemInDarkTheme()) OwedContainerDark else OwedContainerLight
    amountMinor < 0 -> MaterialTheme.colorScheme.errorContainer
    else -> MaterialTheme.colorScheme.surfaceContainerHigh
}

@Composable
fun onBalanceContainer(amountMinor: Long): Color = when {
    amountMinor > 0 -> if (isSystemInDarkTheme()) OnOwedContainerDark else OnOwedContainerLight
    amountMinor < 0 -> MaterialTheme.colorScheme.onErrorContainer
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
