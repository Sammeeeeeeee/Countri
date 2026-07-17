package dev.sam.countri.ui.atlas

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import dev.sam.countri.data.catalog.CountryCatalog
import dev.sam.countri.domain.CountryWithState
import dev.sam.countri.ui.AtlasViewModel
import dev.sam.countri.ui.components.CodeBadge
import dev.sam.countri.ui.components.CountriIcons
import dev.sam.countri.ui.components.LocalHaptics
import dev.sam.countri.ui.components.SectionLabel
import dev.sam.countri.ui.components.tapTarget
import dev.sam.countri.ui.map.MapMode
import dev.sam.countri.ui.map.WorldMap
import dev.sam.countri.ui.share.AtlasCardRenderer
import dev.sam.countri.ui.share.ShareStyle
import dev.sam.countri.ui.share.shareBitmap
import dev.sam.countri.ui.theme.Countri
import dev.sam.countri.ui.theme.CountriType
import dev.sam.countri.ui.theme.hairline
import dev.sam.countri.ui.theme.pressScale

@Composable
fun AtlasScreen(
    viewModel: AtlasViewModel,
    onCountryClick: (String) -> Unit,
    onSeePassport: () -> Unit,
    onBarCompact: (Boolean) -> Unit = {},
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
                .padding(top = 4.dp, bottom = 8.dp),
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Share the coverage map, in the card language.
                val context = androidx.compose.ui.platform.LocalContext.current
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .pressScale(0.9f)
                        .clip(CircleShape)
                        .background(palette.surface1)
                        .tapTarget(onClickLabel = "Share your atlas") {
                            haptics.tick()
                            val visitedIdx = countries
                                .filter { it.isVisited }
                                .map { CountryCatalog.indexOf(it.country.iso2) }
                                .toSet()
                            shareBitmap(
                                context,
                                AtlasCardRenderer.render(
                                    context,
                                    viewModel.worldMap,
                                    visitedIdx,
                                    stats,
                                    if (palette.isDark) ShareStyle.Dark else ShareStyle.Light,
                                ),
                                "countri-atlas.png",
                                "Share your atlas",
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        CountriIcons.Share,
                        contentDescription = "Share your atlas",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(17.dp),
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

        // ---- Recent journeys: a sheet with three detents, drag anywhere ----
        val density = LocalDensity.current
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        val lowPx = with(density) { 128.dp.toPx() }
        val midPx = with(density) { 344.dp.toPx() }
        val highPx = with(density) { (screenHeight * 0.66f).toPx() }
        val trayHeight = remember { Animatable(midPx) }
        val trayScope = rememberCoroutineScope()
        val allVisited = remember(countries) {
            countries.filter { it.isVisited }.sortedByDescending { it.firstYear ?: 0 }
        }
        val trayHigh by remember { derivedStateOf { trayHeight.value > midPx * 1.12f } }
        // Riding high compacts the nav pill, like scrolling does elsewhere.
        LaunchedEffect(trayHigh) { onBarCompact(trayHigh) }

        fun settleTray(velocity: Float) {
            val target = when {
                velocity < -900f -> highPx
                velocity > 900f -> if (trayHeight.value > midPx) midPx else lowPx
                else -> listOf(lowPx, midPx, highPx).minByOrNull {
                    kotlin.math.abs(it - trayHeight.value)
                } ?: midPx
            }
            trayScope.launch {
                trayHeight.animateTo(
                    target,
                    androidx.compose.animation.core.spring(
                        dampingRatio = 0.78f,
                        stiffness = 320f,
                    ),
                    initialVelocity = -velocity,
                )
            }
        }

        // The journeys list and the tray share every drag: pulling up grows
        // the tray first, and pulling down past the list's top shrinks it —
        // so the sheet comes down from anywhere, not just the handle.
        val trayNested = remember(lowPx, midPx, highPx) {
            object : NestedScrollConnection {
                fun nearDetent(): Boolean =
                    listOf(lowPx, midPx, highPx).any { abs(it - trayHeight.value) < 1f }

                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (source != NestedScrollSource.UserInput) return Offset.Zero
                    val dy = available.y
                    if (dy < 0f && trayHeight.value < highPx - 0.5f) {
                        val target = (trayHeight.value - dy).coerceAtMost(highPx)
                        trayScope.launch { trayHeight.snapTo(target) }
                        return Offset(0f, dy)
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (source != NestedScrollSource.UserInput) return Offset.Zero
                    val dy = available.y
                    if (dy > 0f && trayHeight.value > lowPx + 0.5f) {
                        val target = (trayHeight.value - dy).coerceAtLeast(lowPx)
                        trayScope.launch { trayHeight.snapTo(target) }
                        return Offset(0f, dy)
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (!nearDetent()) {
                        settleTray(available.y)
                        return available
                    }
                    return Velocity.Zero
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    if (!nearDetent()) settleTray(available.y)
                    return Velocity.Zero
                }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .height(with(density) { trayHeight.value.toDp() })
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(palette.surface1)
                .nestedScroll(trayNested)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        trayScope.launch {
                            trayHeight.snapTo(
                                (trayHeight.value - delta).coerceIn(lowPx * 0.9f, highPx * 1.03f)
                            )
                        }
                    },
                    onDragStopped = { velocity -> settleTray(velocity) },
                )
                .padding(horizontal = 20.dp)
        ) {
            // Drag handle — tap jumps between detents; any drag works too.
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(palette.textPrimary.copy(alpha = 0.15f))
                    .tapTarget {
                        trayScope.launch {
                            trayHeight.animateTo(
                                if (trayHigh) midPx else highPx,
                                androidx.compose.animation.core.spring(
                                    dampingRatio = 0.78f,
                                    stiffness = 320f,
                                ),
                            )
                        }
                    }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel(if (trayHigh) "All journeys" else "Recent journeys")
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .tapTarget { onSeePassport() }
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

            if (allVisited.isEmpty()) {
                Text(
                    "Countries you add will land here.",
                    style = CountriType.bodySmall,
                    color = palette.textFaint,
                    modifier = Modifier.padding(top = 6.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 100.dp),
                ) {
                    itemsIndexed(allVisited, key = { _, it -> it.country.iso2 }) { index, entry ->
                        RecentRow(
                            entry = entry,
                            showDivider = index < allVisited.lastIndex,
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
    val haptics = LocalHaptics.current
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
        onModeChange = {
            haptics.tick()
            viewModel.mapMode = it
        },
        modifier = Modifier.fillMaxSize(),
    )
}

/** Segmented control with a white thumb that slides between the options. */
@Composable
private fun ModeToggle(mode: MapMode, onMode: (MapMode) -> Unit) {
    val palette = Countri.palette
    val chipWidth = 64.dp
    val chipHeight = 32.dp
    val thumbOffset by animateDpAsState(
        targetValue = if (mode == MapMode.Flat) 0.dp else chipWidth,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.85f,
            stiffness = 550f,
        ),
        label = "toggleThumb",
    )
    Box(
        Modifier
            .clip(CircleShape)
            .background(if (palette.isDark) palette.recessed else palette.hairline.copy(alpha = 0.55f))
            .padding(3.dp)
    ) {
        // The sliding white highlight.
        Box(
            Modifier
                .offset(x = thumbOffset)
                .size(width = chipWidth, height = chipHeight)
                .clip(CircleShape)
                .background(palette.surface1)
        )
        Row {
            ToggleChip("Flat", mode == MapMode.Flat, chipWidth, chipHeight) { onMode(MapMode.Flat) }
            ToggleChip("Globe", mode == MapMode.Globe, chipWidth, chipHeight) { onMode(MapMode.Globe) }
        }
    }
}

@Composable
private fun ToggleChip(
    text: String,
    selected: Boolean,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    val palette = Countri.palette
    val fg by animateColorAsState(
        if (selected) palette.textPrimary else palette.textFaint,
        label = "chipFg",
    )
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(CircleShape)
            .tapTarget(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = CountriType.mono, color = fg)
    }
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
            .tapTarget(onClick = onClick)
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
