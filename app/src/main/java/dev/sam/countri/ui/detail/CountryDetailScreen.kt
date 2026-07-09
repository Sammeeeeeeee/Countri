package dev.sam.countri.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import dev.sam.countri.domain.CountryStatus
import dev.sam.countri.domain.CountryWithState
import dev.sam.countri.ui.AtlasViewModel
import dev.sam.countri.ui.components.CountriIcons
import dev.sam.countri.ui.components.LocalHaptics
import dev.sam.countri.ui.components.SectionLabel
import dev.sam.countri.ui.components.StatusPill
import dev.sam.countri.ui.theme.Countri
import dev.sam.countri.ui.theme.CountriType
import dev.sam.countri.ui.theme.hairline
import dev.sam.countri.ui.theme.pressScale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CountryDetailScreen(
    viewModel: AtlasViewModel,
    iso2: String,
    onBack: () -> Unit,
) {
    val palette = Countri.palette
    val haptics = LocalHaptics.current
    val countries by viewModel.countries.collectAsState()
    val entry = countries.firstOrNull { it.country.iso2 == iso2 } ?: return
    var confirmClear by remember { mutableStateOf(false) }

    val accent = when (entry.status) {
        CountryStatus.VISITED -> palette.visited
        CountryStatus.WISHLIST -> palette.wishlist
        null -> palette.textSecondary
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ---- Hero: locator map (M4) under a scrim, name + code on top ----
        Box(
            Modifier
                .fillMaxWidth()
                .height(270.dp)
        ) {
            DetailHeroMap(entry)
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to palette.canvas.copy(alpha = 0.35f),
                            0.45f to Color.Transparent,
                            1f to palette.canvas,
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 8.dp)
                    .size(38.dp)
                    .pressScale(0.9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.canvas.copy(alpha = 0.55f))
                    .hairline(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    CountriIcons.Back,
                    contentDescription = "Back",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatusPill(
                        text = when (entry.status) {
                            CountryStatus.VISITED -> "Visited"
                            CountryStatus.WISHLIST -> "On the wishlist"
                            null -> "Not yet"
                        },
                        accent = accent,
                    )
                    Text(
                        entry.country.continent.displayName,
                        style = CountriType.bodySmall,
                        color = palette.textSecondary,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        entry.country.name,
                        style = CountriType.display,
                        color = palette.textPrimary,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        entry.country.iso2,
                        style = CountriType.subtitle.copy(fontFamily = dev.sam.countri.ui.theme.MonoFamily),
                        color = accent.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
                    )
                }
            }
        }

        // ---- Body ----
        Column(Modifier.padding(horizontal = 22.dp)) {
            if (entry.isVisited) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatCard("First visit", entry.firstVisitYear?.toString() ?: "—", Modifier.weight(1f))
                    StatCard("Trips", entry.trips.toString(), Modifier.weight(1f))
                    StatCard("Cities", entry.cities.size.toString(), Modifier.weight(1f))
                }
                if (!entry.note.isNullOrBlank()) {
                    Text(
                        "“${entry.note}”",
                        style = CountriType.subtitle.copy(fontStyle = FontStyle.Italic),
                        color = palette.textPrimary.copy(alpha = 0.82f),
                        modifier = Modifier.padding(top = 22.dp),
                    )
                }
                if (entry.cities.isNotEmpty()) {
                    SectionLabel("Cities", Modifier.padding(top = 26.dp, bottom = 12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        entry.cities.forEach { city ->
                            Text(
                                city,
                                style = CountriType.bodySmall,
                                color = palette.textPrimary,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(palette.surface1)
                                    .hairline(CircleShape)
                                    .padding(horizontal = 15.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            } else if (entry.isWishlist) {
                Text(
                    "Still on the list.",
                    style = CountriType.subtitle.copy(fontStyle = FontStyle.Italic),
                    color = palette.textPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 22.dp),
                )
            }

            // ---- Actions ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 26.dp, bottom = 26.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ActionButton(
                    text = "Visited",
                    accent = palette.visited,
                    dim = palette.visitedDim,
                    selected = entry.isVisited,
                    modifier = Modifier.weight(1f),
                ) {
                    haptics.confirm()
                    viewModel.setStatus(iso2, CountryStatus.VISITED)
                }
                ActionButton(
                    text = "Wishlist",
                    accent = palette.wishlist,
                    dim = palette.wishlistDim,
                    selected = entry.isWishlist,
                    modifier = Modifier.weight(1f),
                ) {
                    haptics.confirm()
                    viewModel.setStatus(iso2, CountryStatus.WISHLIST)
                }
                if (entry.status != null) {
                    Box(
                        modifier = Modifier
                            .size(width = 52.dp, height = 50.dp)
                            .pressScale(0.94f)
                            .clip(RoundedCornerShape(14.dp))
                            .hairline(RoundedCornerShape(14.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                val hasData = entry.note != null || entry.cities.isNotEmpty() ||
                                    (entry.isVisited && entry.trips > 1)
                                if (hasData) confirmClear = true else {
                                    haptics.reject()
                                    viewModel.clear(iso2)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            CountriIcons.Close,
                            contentDescription = "Remove from atlas",
                            tint = palette.textFaint,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
            Box(Modifier.navigationBarsPadding())
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = palette.surface2,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = { Text("Remove ${entry.country.name}?", style = CountriType.subtitle) },
            text = {
                Text(
                    "Your year, note and cities for this country will be deleted.",
                    style = CountriType.body,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    haptics.reject()
                    viewModel.clear(iso2)
                }) {
                    Text("Remove", style = CountriType.body, color = palette.wishlist)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("Keep", style = CountriType.body, color = palette.textSecondary)
                }
            },
        )
    }
}

/** Placeholder until the locator map lands in M4. */
@Composable
private fun DetailHeroMap(entry: CountryWithState) {
    val palette = Countri.palette
    Box(
        Modifier
            .fillMaxSize()
            .background(palette.globeShade)
    )
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    val palette = Countri.palette
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surface1)
            .hairline(RoundedCornerShape(16.dp))
            .padding(15.dp),
    ) {
        Text(
            value,
            style = CountriType.displaySmall.copy(fontSize = CountriType.title.fontSize),
            color = palette.visited,
        )
        Text(
            label,
            style = CountriType.monoSmall,
            color = palette.textSecondary,
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    accent: Color,
    dim: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(50.dp)
            .pressScale(0.96f)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) dim else Color.Transparent)
            .hairline(RoundedCornerShape(14.dp), accent.copy(alpha = if (selected) 0.4f else 0.2f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = CountriType.body, color = accent)
    }
}
