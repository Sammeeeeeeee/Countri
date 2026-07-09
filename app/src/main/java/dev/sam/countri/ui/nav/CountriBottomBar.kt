package dev.sam.countri.ui.nav

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.sam.countri.ui.components.CountriIcons
import dev.sam.countri.ui.components.LocalHaptics
import dev.sam.countri.ui.theme.Countri
import dev.sam.countri.ui.theme.Springs
import dev.sam.countri.ui.theme.pressScale

enum class CountriTab { Atlas, Passport, Stats, Wishlist }

/**
 * Custom bottom bar: four line-icon tabs around a raised circular + button.
 * Flat surface, top hairline, no elevation — the FAB overlaps the bar edge.
 */
@Composable
fun CountriBottomBar(
    current: CountriTab?,
    onTab: (CountriTab) -> Unit,
    onAdd: () -> Unit,
) {
    val palette = Countri.palette
    val haptics = LocalHaptics.current

    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp) // room for the FAB overlap
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawRect(palette.canvas.copy(alpha = 0.97f))
                        drawRect(palette.hairline, size = size.copy(height = 1.dp.toPx()))
                    }
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(60.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TabItem(CountriIcons.Atlas, "Atlas", current == CountriTab.Atlas, Modifier.weight(1f)) {
                    haptics.tick(); onTab(CountriTab.Atlas)
                }
                TabItem(CountriIcons.Passport, "Passport", current == CountriTab.Passport, Modifier.weight(1f)) {
                    haptics.tick(); onTab(CountriTab.Passport)
                }
                Box(Modifier.weight(1f)) // FAB well
                TabItem(CountriIcons.Stats, "Stats", current == CountriTab.Stats, Modifier.weight(1f)) {
                    haptics.tick(); onTab(CountriTab.Stats)
                }
                TabItem(CountriIcons.Wishlist, "Wishlist", current == CountriTab.Wishlist, Modifier.weight(1f)) {
                    haptics.tick(); onTab(CountriTab.Wishlist)
                }
            }
        }

        // Center + button, overlapping the bar's top edge.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(56.dp)
                .pressScale(0.92f)
                .clip(CircleShape)
                .background(palette.visited)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    haptics.tick()
                    onAdd()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = CountriIcons.Plus,
                contentDescription = "Add a country",
                tint = palette.onVisited,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
private fun TabItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = Countri.palette
    val tint by animateColorAsState(
        targetValue = if (selected) palette.visited else palette.textFaint,
        label = "tabTint",
    )
    val dotSize by animateDpAsState(
        targetValue = if (selected) 3.dp else 0.dp,
        animationSpec = androidx.compose.animation.core.spring(0.55f, 500f),
        label = "tabDot",
    )
    Column(
        modifier = modifier
            .pressScale(0.94f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, style = dev.sam.countri.ui.theme.CountriType.monoSmall, color = tint)
        Box(
            Modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(palette.visited)
        )
    }
}
