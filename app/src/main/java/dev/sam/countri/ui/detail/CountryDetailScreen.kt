package dev.sam.countri.ui.detail

import android.content.Intent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.sam.countri.data.wiki.WikiSearch
import dev.sam.countri.domain.CountryStatus
import dev.sam.countri.ui.AtlasViewModel
import androidx.compose.ui.unit.sp
import dev.sam.countri.ui.components.CountriIcons
import dev.sam.countri.ui.components.LocalHaptics
import dev.sam.countri.ui.components.SectionLabel
import dev.sam.countri.ui.components.flagEmoji
import dev.sam.countri.ui.map.CityMarker
import dev.sam.countri.ui.map.CountrySilhouette
import dev.sam.countri.ui.share.CountryCardRenderer
import dev.sam.countri.ui.share.shareBitmap
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
    val context = LocalContext.current
    val countries by viewModel.countries.collectAsState()
    val entry = countries.firstOrNull { it.country.iso2 == iso2 } ?: return
    var confirmClear by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<EditTarget?>(null) }
    var showAddVisit by remember { mutableStateOf(false) }
    var editVisit by remember { mutableStateOf<dev.sam.countri.domain.Visit?>(null) }

    val continentHue = palette.continentColor(entry.country.continent)
    val accent = when (entry.status) {
        CountryStatus.VISITED -> continentHue
        CountryStatus.WISHLIST -> palette.wishlist
        null -> palette.textSecondary
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ---- Hero: the country itself, drawn huge ----
        Box(
            Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            val markers = remember(entry.visits) {
                entry.allCities.mapNotNull { name ->
                    viewModel.cities.find(iso2, name)?.let { city ->
                        CityMarker(city.name, city.lat, city.lon)
                    }
                }
            }
            CountrySilhouette(
                data = viewModel.worldMap,
                iso2 = iso2,
                cityMarkers = markers,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 26.dp, vertical = 14.dp),
            )
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 8.dp)
                    .size(38.dp)
                    .pressScale(0.9f)
                    .clip(CircleShape)
                    .background(palette.surface1.copy(alpha = 0.92f))
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
            if (entry.status != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(end = 16.dp, top = 8.dp)
                        .size(38.dp)
                        .pressScale(0.9f)
                        .clip(CircleShape)
                        .background(palette.surface1.copy(alpha = 0.92f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            haptics.tick()
                            shareBitmap(
                                context,
                                CountryCardRenderer.render(context, entry),
                                "countri-country.png",
                                "Share " + entry.country.name,
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        CountriIcons.Share,
                        contentDescription = "Share country",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }

        // ---- Identity ----
        Column(Modifier.padding(horizontal = 22.dp)) {
            Text(
                text = when (entry.status) {
                    CountryStatus.VISITED -> "Visited"
                    CountryStatus.WISHLIST -> "On the wishlist"
                    null -> "Not yet"
                } + "  ·  " + entry.country.continent.displayName,
                style = CountriType.bodySmall,
                color = palette.textFaint,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
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
                    flagEmoji(entry.country.iso2),
                    style = CountriType.body.copy(fontSize = 30.sp),
                    modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
                )
            }

            // ---- Visits ----
            if (entry.isVisited) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatCard(
                        label = "First visit",
                        value = entry.firstYear?.toString() ?: "—",
                        accent = continentHue,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (entry.visits.isEmpty()) editTarget = EditTarget.YearOfVisit
                    }
                    StatCard(
                        label = if (entry.tripCount == 1) "Trip" else "Trips",
                        value = entry.tripCount.toString(),
                        accent = continentHue,
                        modifier = Modifier.weight(1f),
                    ) { showAddVisit = true }
                }
                if (entry.visits.isNotEmpty()) {
                    SectionLabel("Visits", Modifier.padding(top = 24.dp, bottom = 10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        entry.visits.sortedByDescending { it.start }.forEach { visit ->
                            VisitCard(
                                visit = visit,
                                accent = continentHue,
                                onClick = { editVisit = visit },
                                onDelete = {
                                    haptics.reject()
                                    viewModel.deleteVisit(visit.id)
                                },
                            )
                        }
                    }
                }
            }

            // ---- Places: things seen or worth seeing; tap one → Wikipedia ----
            if (entry.status != null) {
                SectionLabel(
                    if (entry.isVisited) "Places" else "Places to see",
                    Modifier.padding(top = 26.dp, bottom = 12.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    entry.places.forEach { place ->
                        Row(
                            modifier = Modifier
                                .pressScale(0.96f)
                                .clip(CircleShape)
                                .background(palette.surface1)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    haptics.tick()
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, WikiSearch.pageUrl(place).toUri())
                                    )
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(place, style = CountriType.bodySmall, color = palette.textPrimary)
                            Icon(
                                CountriIcons.Chevron,
                                contentDescription = null,
                                tint = continentHue.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .size(12.dp),
                            )
                        }
                    }
                    Text(
                        "+ Add",
                        style = CountriType.bodySmall,
                        color = palette.textPrimary,
                        modifier = Modifier
                            .pressScale(0.94f)
                            .clip(CircleShape)
                            .background(palette.surface1)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { editTarget = EditTarget.Places }
                            .padding(horizontal = 15.dp, vertical = 8.dp),
                    )
                }
            }

            // ---- Actions: filled ink primary, mist secondary ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 26.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ActionButton(
                    text = if (entry.isVisited) "+ Add visit" else "Visited",
                    primary = true,
                    modifier = Modifier.weight(1f),
                ) {
                    haptics.tick()
                    showAddVisit = true
                }
                // A country with recorded trips can't go back to dreaming.
                if (entry.visits.isEmpty() && !entry.isWishlist) {
                    ActionButton(
                        text = "Wishlist",
                        primary = false,
                        modifier = Modifier.weight(1f),
                    ) {
                        haptics.confirm()
                        viewModel.setStatus(iso2, CountryStatus.WISHLIST)
                    }
                }
                if (entry.status != null) {
                    Box(
                        modifier = Modifier
                            .size(width = 52.dp, height = 50.dp)
                            .pressScale(0.94f)
                            .clip(CircleShape)
                            .background(palette.surface1)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                val hasData = entry.places.isNotEmpty() ||
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

    if (showAddVisit) {
        AddVisitSheet(
            entry = entry,
            cityData = viewModel.cities,
            onDismiss = { showAddVisit = false },
            onSave = { start, end, visitCities ->
                viewModel.addVisit(iso2, start, end, visitCities)
                showAddVisit = false
            },
        )
    }

    editVisit?.let { visit ->
        AddVisitSheet(
            entry = entry,
            cityData = viewModel.cities,
            existing = visit,
            onDismiss = { editVisit = null },
            onSave = { start, end, visitCities ->
                viewModel.updateVisit(visit.id, iso2, start, end, visitCities)
                editVisit = null
            },
        )
    }

    editTarget?.let { target ->
        DetailEditSheet(
            target = target,
            entry = entry,
            onDismiss = { editTarget = null },
            onSaveYear = { year ->
                viewModel.updateDetails(iso2, firstVisitYear = year)
                editTarget = null
            },
            onSaveTrips = { trips ->
                haptics.confirm()
                viewModel.updateDetails(iso2, trips = trips)
                editTarget = null
            },
            onSavePlaces = { places ->
                haptics.confirm()
                viewModel.updateDetails(iso2, places = places)
                editTarget = null
            },
        )
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
                    "Your visits and places for this country will be deleted.",
                    style = CountriType.body,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    haptics.reject()
                    viewModel.clear(iso2)
                }) {
                    Text("Remove", style = CountriType.body, color = palette.textSecondary)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("Keep", style = CountriType.body, color = continentHue)
                }
            },
        )
    }
}

@Composable
private fun VisitCard(
    visit: dev.sam.countri.domain.Visit,
    accent: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = Countri.palette
    val formatter = remember { java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy") }
    Column(
        Modifier
            .fillMaxWidth()
            .pressScale(0.98f)
            .clip(RoundedCornerShape(20.dp))
            .background(palette.surface1)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${visit.start.format(formatter)} → ${visit.end.format(formatter)}",
                    style = CountriType.bodySmall,
                    color = palette.textPrimary,
                )
                Text(
                    if (visit.days == 1) "1 day" else "${visit.days} days",
                    style = CountriType.monoSmall,
                    color = accent,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .pressScale(0.88f)
                    .clip(CircleShape)
                    .background(palette.recessed)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDelete,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    CountriIcons.Close,
                    contentDescription = "Delete visit",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        if (visit.cities.isNotEmpty()) {
            Text(
                visit.cities.joinToString(" · "),
                style = CountriType.bodySmall,
                color = palette.textSecondary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val palette = Countri.palette
    Column(
        modifier = modifier
            .pressScale(0.97f)
            .clip(RoundedCornerShape(20.dp))
            .background(palette.surface1)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(15.dp),
    ) {
        Text(
            value,
            style = CountriType.title,
            color = accent,
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
    primary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = Countri.palette
    Box(
        modifier = modifier
            .height(50.dp)
            .pressScale(0.96f)
            .clip(CircleShape)
            .background(if (primary) palette.visited else palette.surface1)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = CountriType.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.W600),
            color = if (primary) palette.onVisited else palette.textPrimary,
        )
    }
}
