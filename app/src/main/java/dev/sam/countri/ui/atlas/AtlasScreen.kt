package dev.sam.countri.ui.atlas

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import dev.sam.countri.domain.CountryWithState
import dev.sam.countri.ui.AtlasViewModel
import dev.sam.countri.ui.components.CodeBadge
import dev.sam.countri.ui.components.CountriIcons
import dev.sam.countri.ui.components.LocalHaptics
import dev.sam.countri.ui.components.SectionLabel
import dev.sam.countri.ui.map.MapMode
import dev.sam.countri.ui.theme.Countri
import dev.sam.countri.ui.theme.CountriType
import dev.sam.countri.ui.theme.hairline
import dev.sam.countri.ui.theme.pressScale

@Composable
fun AtlasScreen(
    viewModel: AtlasViewModel,
    onCountryClick: (String) -> Unit,
    onSeePassport: () -> Unit,
) {
    val palette = Countri.palette
    val haptics = LocalHaptics.current
    val countries by viewModel.countries.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val recent = remember(countries) {
        countries.filter { it.isVisited }
            .sortedByDescending { it.firstVisitYear ?: 0 }
            .take(4)
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // ---- Header ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(top = 14.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text("Your Atlas", style = CountriType.title, color = palette.textPrimary)
                Text(
                    "${stats.visitedCount} countries · ${stats.continentsVisited} continents",
                    style = CountriType.bodySmall,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
            ModeToggle(
                mode = viewModel.mapMode,
                onMode = {
                    haptics.tick()
                    viewModel.mapMode = it
                },
            )
        }

        // ---- Map hero ----
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AtlasMap(viewModel)

            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 22.dp, bottom = 18.dp)
            ) {
                Text(
                    "${stats.percentOfWorld}%",
                    style = CountriType.displayLarge,
                    color = palette.visited,
                )
                Text(
                    "of the world",
                    style = CountriType.bodySmall,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            Column(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                horizontalAlignment = Alignment.End,
            ) {
                LegendRow("Visited", palette.visited)
                LegendRow("Wishlist", palette.wishlist)
            }
        }

        // ---- Recent journeys ----
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(palette.surface1)
                .drawBehind {
                    drawRect(palette.hairline, size = size.copy(height = 1.dp.toPx()))
                }
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel("Recent journeys")
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSeePassport() }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Passport", style = CountriType.mono, color = palette.visited)
                    Icon(
                        CountriIcons.Chevron,
                        contentDescription = null,
                        tint = palette.visited,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            if (recent.isEmpty()) {
                Text(
                    "Countries you add will land here.",
                    style = CountriType.bodySmall,
                    color = palette.textFaint,
                    modifier = Modifier.padding(vertical = 18.dp),
                )
            } else {
                Column(Modifier.padding(top = 6.dp)) {
                    recent.forEachIndexed { index, entry ->
                        RecentRow(
                            entry = entry,
                            showDivider = index < recent.lastIndex,
                        ) { onCountryClick(entry.country.iso2) }
                    }
                }
            }
        }
    }
}

/** Placeholder until the polygon renderer lands in M4. */
@Composable
private fun AtlasMap(viewModel: AtlasViewModel) {
    val palette = Countri.palette
    Box(
        Modifier
            .fillMaxSize()
            .background(palette.canvas)
    )
}

@Composable
private fun ModeToggle(mode: MapMode, onMode: (MapMode) -> Unit) {
    val palette = Countri.palette
    Row(
        Modifier
            .clip(RoundedCornerShape(11.dp))
            .background(palette.surface1)
            .hairline(RoundedCornerShape(11.dp))
            .padding(3.dp)
    ) {
        ToggleChip("Flat", mode == MapMode.Flat) { onMode(MapMode.Flat) }
        ToggleChip("Globe", mode == MapMode.Globe) { onMode(MapMode.Globe) }
    }
}

@Composable
private fun ToggleChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val palette = Countri.palette
    val bg by animateColorAsState(
        if (selected) palette.surface2 else androidx.compose.ui.graphics.Color.Transparent,
        label = "chipBg",
    )
    val fg by animateColorAsState(
        if (selected) palette.visited else palette.textSecondary,
        label = "chipFg",
    )
    Text(
        text = text,
        style = CountriType.mono,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 13.dp, vertical = 7.dp),
    )
}

@Composable
private fun LegendRow(label: String, color: androidx.compose.ui.graphics.Color) {
    val palette = Countri.palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(label, style = CountriType.monoSmall, color = palette.textSecondary)
    }
}

@Composable
private fun RecentRow(
    entry: CountryWithState,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    val palette = Countri.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(0.98f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .drawBehind {
                if (showDivider) {
                    drawRect(
                        palette.textPrimary.copy(alpha = 0.04f),
                        topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - 1.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()),
                    )
                }
            }
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CodeBadge(entry.country.iso2, entry.status)
        Column(Modifier.weight(1f)) {
            Text(entry.country.name, style = CountriType.body, color = palette.textPrimary)
            Text(
                "${entry.country.continent.displayName} · ${entry.firstVisitYear ?: "—"}",
                style = CountriType.bodySmall,
                color = palette.textFaint,
            )
        }
        Icon(
            CountriIcons.Chevron,
            contentDescription = null,
            tint = palette.textFaint,
            modifier = Modifier.size(16.dp),
        )
    }
}
