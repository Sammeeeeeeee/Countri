package dev.sam.countri.ui.passport

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.sam.countri.ui.share.PassportShareSheet
import dev.sam.countri.domain.CountryWithState
import dev.sam.countri.ui.AtlasViewModel
import dev.sam.countri.ui.components.CountriIcons
import dev.sam.countri.ui.components.LocalHaptics
import dev.sam.countri.ui.theme.Countri
import dev.sam.countri.ui.theme.CountriType
import dev.sam.countri.ui.theme.Springs
import dev.sam.countri.ui.theme.pressScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Deterministic stamp tilt, straight from the country code. */
fun stampRotation(iso2: String): Float =
    ((iso2[0].code + iso2[1].code) % 11 - 5).toFloat()

@Composable
fun PassportScreen(
    viewModel: AtlasViewModel,
    onCountryClick: (String) -> Unit,
) {
    val palette = Countri.palette
    val countries by viewModel.countries.collectAsState()
    val stamps = remember(countries) {
        countries.filter { it.isVisited }
            .sortedWith(compareBy({ it.firstVisitYear ?: Int.MAX_VALUE }, { it.country.name }))
    }

    var showShare by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text("Passport", style = CountriType.title, color = palette.textPrimary)
                Text(
                    text = when (stamps.size) {
                        0 -> "No stamps yet"
                        1 -> "1 stamp"
                        else -> "${stamps.size} stamps"
                    },
                    style = CountriType.bodySmall,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
            if (stamps.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .pressScale(0.9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.surface1)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { showShare = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        CountriIcons.Share,
                        contentDescription = "Share passport",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        if (stamps.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 40.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    CountriIcons.Passport,
                    contentDescription = null,
                    tint = palette.textFaint,
                    modifier = Modifier.size(36.dp),
                )
                Text(
                    "Your first stamp is one trip away.",
                    style = CountriType.body,
                    color = palette.textFaint,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 18.dp, end = 18.dp, top = 14.dp, bottom = 24.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                itemsIndexed(stamps, key = { _, it -> it.country.iso2 }) { index, entry ->
                    Stamp(
                        entry = entry,
                        index = index,
                        onClick = { onCountryClick(entry.country.iso2) },
                    )
                }
            }
        }
    }

    if (showShare) {
        PassportShareSheet(
            stamps = stamps,
            onDismiss = { showShare = false },
        )
    }
}

@Composable
private fun Stamp(
    entry: CountryWithState,
    index: Int,
    onClick: () -> Unit,
) {
    val palette = Countri.palette
    val haptics = LocalHaptics.current
    val accent = palette.visited

    // Stamp hit: starts big and transparent, slams down with overshoot.
    val scale = remember(entry.country.iso2) { Animatable(2.3f) }
    val alpha = remember(entry.country.iso2) { Animatable(0f) }
    LaunchedEffect(entry.country.iso2) {
        delay(index.coerceAtMost(17) * 55L)
        if (index < 8) haptics.tick()
        launch { alpha.animateTo(1f, Springs.Fast) }
        scale.animateTo(1f, Springs.Bouncy)
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
                rotationZ = stampRotation(entry.country.iso2)
            }
            .pressScale(0.92f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            drawCircle(
                color = accent.copy(alpha = 0.5f),
                radius = r - 1.dp.toPx(),
                style = Stroke(width = 1.5.dp.toPx()),
            )
            drawCircle(
                color = accent.copy(alpha = 0.28f),
                radius = r - 7.dp.toPx(),
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(4.dp.toPx(), 4.dp.toPx()),
                    ),
                ),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                entry.country.iso2,
                style = CountriType.mono.copy(fontSize = 24.sp, letterSpacing = 0.04.em),
                color = accent,
            )
            Text(
                entry.firstVisitYear?.toString() ?: "—",
                style = CountriType.monoSmall,
                color = accent.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

