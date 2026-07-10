package dev.sam.countri.ui.atlas

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import dev.sam.countri.data.catalog.CountryCatalog
import dev.sam.countri.domain.CountryWithState
import dev.sam.countri.ui.AtlasViewModel
import dev.sam.countri.ui.components.CodeBadge
import dev.sam.countri.ui.components.CountriIcons
import dev.sam.countri.ui.components.LocalHaptics
import dev.sam.countri.ui.components.SectionLabel
import dev.sam.countri.ui.map.MapMode
import dev.sam.countri.ui.map.WorldMap
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
            .sortedByDescending { it.firstYear ?: 0 }
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
            AtlasMap(viewModel, onCountryClick)

            // The one chromatic element in the whole app: the cobalt ribbon.
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 22.dp, bottom = 18.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(palette.aurora))
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val pctProgress = remember { Animatable(0f) }
                LaunchedEffect(stats.percentOfWorld) {
                    pctProgress.animateTo(1f, tween(1100, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)))
                }
                Text(
                    "${(stats.percentOfWorld * pctProgress.value).roundToInt()}% of the world",
                    style = CountriType.subtitle,
                    color = androidx.compose.ui.graphics.Color.White,
                )
            }

            Column(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                horizontalAlignment = Alignment.End,
            ) {
                LegendSwatch("Visited", filled = true)
                LegendSwatch("Wishlist", filled = false)
            }
        }

        // ---- Recent journeys ----
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
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

@Composable
private fun AtlasMap(
    viewModel: AtlasViewModel,
    onCountryClick: (String) -> Unit,
) {
    val countries by viewModel.countries.collectAsState()
    val justAdded by viewModel.justAdded.collectAsState()
    val statuses = remember(countries) {
        buildMap {
            countries.forEach { entry ->
                entry.status?.let { put(CountryCatalog.indexOf(entry.country.iso2), it) }
            }
        }
    }
    WorldMap(
        data = viewModel.worldMap,
        statuses = statuses,
        mode = viewModel.mapMode,
        introUnwrap = !viewModel.introPlayed,
        onIntroConsumed = { viewModel.introPlayed = true },
        justAddedIso = justAdded,
        onCountryTap = onCountryClick,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun ModeToggle(mode: MapMode, onMode: (MapMode) -> Unit) {
    val palette = Countri.palette
    Row(
        Modifier
            .clip(CircleShape)
            .background(palette.surface1)
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
            .clip(CircleShape)
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
private fun LegendSwatch(label: String, filled: Boolean) {
    val palette = Countri.palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (filled) {
            Box(
                Modifier
                    .size(width = 18.dp, height = 10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(palette.visited)
            )
        } else {
            Canvas(Modifier.size(width = 18.dp, height = 10.dp)) {
                drawRoundRect(
                    color = palette.wishlist.copy(alpha = 0.75f),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                    style = Stroke(
                        width = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(3.dp.toPx(), 2.5.dp.toPx()),
                        ),
                    ),
                )
            }
        }
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
                "${entry.country.continent.displayName} · ${entry.firstYear ?: "—"}",
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
