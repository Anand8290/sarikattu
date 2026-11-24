package com.nononsensestudios.sarikattu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nononsensestudios.sarikattu.domain.formatMoney

/** Rounded text fields used on every form. */
val FieldShape = RoundedCornerShape(16.dp)

/** Shows a group's people count as "1 person" / "5 people". */
fun peopleCount(n: Int) = if (n == 1) "1 person" else "$n people"

fun initialOf(name: String) = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

@Composable
fun BackButton(onClick: () -> Unit) =
    IconButton(onClick = onClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }

@Composable
fun CloseButton(onClick: () -> Unit) =
    IconButton(onClick = onClick) { Icon(Icons.Default.Close, "Close") }

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.padding(start = 8.dp, top = 20.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

/** "+₹1,800" in green, "−₹350" in red, or "settled" in grey. */
@Composable
fun BalancePill(balance: Long, currency: String, modifier: Modifier = Modifier) {
    val text = when {
        balance > 0 -> "+${formatMoney(balance, currency)}"
        balance < 0 -> "−${formatMoney(-balance, currency)}"
        else -> "settled"
    }
    Surface(
        modifier,
        shape = CircleShape,
        color = balanceContainer(balance),
        contentColor = onBalanceContainer(balance),
    ) {
        Text(text, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
    }
}

/** A stable colour pair per person or group, picked from the theme so it follows the wallpaper. */
@Composable
private fun paletteFor(key: String): Pair<Color, Color> {
    val c = MaterialTheme.colorScheme
    val options = listOf(
        c.primaryContainer to c.onPrimaryContainer,
        c.secondaryContainer to c.onSecondaryContainer,
        c.tertiaryContainer to c.onTertiaryContainer,
        c.surfaceContainerHighest to c.onSurfaceVariant,
    )
    return options[(key.hashCode() and Int.MAX_VALUE) % options.size]
}

/** Round initial for a person. [dimmed] shows someone who isn't included. */
@Composable
fun Avatar(name: String, key: String, modifier: Modifier = Modifier, size: Dp = 40.dp, dimmed: Boolean = false) {
    val (bg, fg) = paletteFor(key)
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(if (dimmed) MaterialTheme.colorScheme.surfaceContainerHigh else bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initialOf(name),
            color = if (dimmed) MaterialTheme.colorScheme.outline else fg,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val groupShapes = listOf(
    MaterialShapes.Cookie9Sided,
    MaterialShapes.Clover4Leaf,
    MaterialShapes.Sunny,
    MaterialShapes.Cookie6Sided,
    MaterialShapes.Pentagon,
    MaterialShapes.SoftBurst,
)

/** A group's badge: its initial in one of the Expressive shapes, chosen from the group id. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GroupBadge(name: String, key: String, size: Dp = 48.dp, container: Color? = null, content: Color? = null) {
    val (paletteBg, paletteFg) = paletteFor(key)
    val bg = container ?: paletteBg
    val fg = content ?: paletteFg
    val shape = groupShapes[(key.hashCode() and Int.MAX_VALUE) % groupShapes.size].toShape()
    Box(Modifier.size(size).clip(shape).background(bg), contentAlignment = Alignment.Center) {
        Text(initialOf(name), color = fg, style = MaterialTheme.typography.titleMedium)
    }
}

/** Big rounded card tinted by a balance: green when owed, red when owing, grey when settled. */
@Composable
fun BalanceHero(balance: Long, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = balanceContainer(balance),
        contentColor = onBalanceContainer(balance),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(2.dp), content = content)
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, action: @Composable (() -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            body,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        action?.let {
            Box(Modifier.padding(top = 8.dp)) { it() }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScreenLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
}

/** A short vibration for confirmed actions: saving, settling up, joining. */
@Composable
fun rememberConfirmHaptic(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { { haptic.performHapticFeedback(HapticFeedbackType.Confirm) } }
}

/** True while the list is scrolled to the top or moving up; used to expand the FAB. */
@Composable
fun LazyListState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            val up = if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousOffset >= firstVisibleItemScrollOffset
            }
            previousIndex = firstVisibleItemIndex
            previousOffset = firstVisibleItemScrollOffset
            up
        }
    }.value
}

/** Tile colour for grouped list rows, one step above the page so the rounded tiles stand out. */
@Composable
fun segmentColors() = androidx.compose.material3.ListItemDefaults.segmentedColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
)
