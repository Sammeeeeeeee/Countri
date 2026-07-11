package dev.sam.countri.ui.stats

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sam.countri.data.catalog.WORLD_COUNTRY_COUNT
import dev.sam.countri.domain.AtlasStats
import dev.sam.countri.ui.AtlasViewModel
import dev.sam.countri.ui.components.CountriIcons
import dev.sam.countri.ui.components.LocalHaptics
import dev.sam.countri.ui.components.SectionLabel
import dev.sam.countri.ui.components.flagEmoji
import dev.sam.countri.ui.share.StatsCardRenderer
import dev.sam.countri.ui.share.shareBitmap
import dev.sam.countri.ui.theme.Countri
import dev.sam.countri.ui.theme.CountriType
import dev.sam.countri.ui.theme.MonoFamily
import dev.sam.countri.ui.theme.pressScale
import dev.sam.countri.ui.theme.staggeredEnter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val CountUpEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

@Composable
fun StatsScreen(viewModel: AtlasViewModel) {
    val palette = Countri.palette
    val haptics = LocalHaptics.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stats by viewModel.stats.collectAsState()

    // One driver for every number on screen: they land together.
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1200, easing = CountUpEasing))
    }
    val p = progress.value

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text("Stats", style = CountriType.title, color = palette.textPrimary)
                Text(
                    "The world, counted.",
                    style = CountriType.bodySmall,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .pressScale(0.9f)
                    .clip(CircleShape)
                    .background(palette.surface1)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        haptics.tick()
                        shareBitmap(
                            context,
                            StatsCardRenderer.render(context, stats),
                            "countri-stats.png",
                            "Share your stats",
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    CountriIcons.Share,
                    contentDescription = "Share stats",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // ---- Hero: ring + counters ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(top = 24.dp, bottom = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            ProgressRing(
                fraction = stats.visitedCount / WORLD_COUNTRY_COUNT.toFloat() * p,
                label = "${(stats.percentOfWorld * p).roundToInt()}%",
                sublabel = "of the world",
            )
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${(stats.visitedCount * p).roundToInt()}",
                        style = CountriType.displaySmall,
                        color = palette.textPrimary,
                    )
                    Text(
                        " / 195",
                        style = CountriType.subtitle,
                        color = palette.textFaint,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
                Text(
                    "Countries",
                    style = CountriType.monoSmall,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(top = 3.dp, bottom = 14.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "${(stats.continentsVisited * p).roundToInt()}",
                                style = CountriType.subtitle,
                                color = palette.textPrimary,
                            )
                            Text(" /7", style = CountriType.bodySmall, color = palette.textFaint)
                        }
                        Text(
                            "Continents",
                            style = CountriType.monoSmall,
                            color = palette.textSecondary,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                    Column {
                        Text(
                            "${(stats.placeTotal * p).roundToInt()}",
                            style = CountriType.subtitle,
                            color = palette.textPrimary,
                        )
                        Text(
                            "Places",
                            style = CountriType.monoSmall,
                            color = palette.textSecondary,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                }
            }
        }

        // ---- By continent ----
        SectionLabel("By continent", Modifier.padding(horizontal = 22.dp))
        Column(
            Modifier
                .padding(horizontal = 22.dp)
                .padding(top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            stats.byContinent.forEachIndexed { index, stat ->
                Column(Modifier.staggeredEnter(index)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            stat.continent.displayName,
                            style = CountriType.bodySmall,
                            color = palette.continentColor(stat.continent)
                                .copy(alpha = if (stat.visited > 0) 1f else 0.45f),
                        )
                        Text(
                            "${stat.visited} / ${stat.continent.total}",
                            style = CountriType.monoSmall,
                            color = palette.textFaint,
                        )
                    }
                    Box(
                        Modifier
                            .padding(top = 6.dp)
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(palette.textPrimary.copy(alpha = 0.06f))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(
                                    (stat.fraction * 3f).coerceAtMost(1f) * p
                                )
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(palette.continentColor(stat.continent))
                        )
                    }
                }
            }
        }

        // ---- Timeline ----
        if (stats.timeline.isNotEmpty()) {
            SectionLabel("Timeline", Modifier.padding(horizontal = 22.dp))
            Column(Modifier.padding(horizontal = 22.dp).padding(top = 14.dp, bottom = 30.dp)) {
                stats.timeline.forEachIndexed { index, group ->
                    Row(Modifier.staggeredEnter(index)) {
                        Text(
                            group.year.toString(),
                            style = CountriType.subtitle.copy(fontFamily = MonoFamily, fontSize = 17.sp),
                            color = palette.visited,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(52.dp),
                        )
                        TimelineSpine(isLast = index == stats.timeline.lastIndex)
                        Column(Modifier.padding(bottom = 18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (group.isoCodes.size == 1) "1 country"
                                    else "${group.isoCodes.size} countries",
                                    style = CountriType.bodySmall,
                                    color = palette.textPrimary.copy(alpha = 0.85f),
                                )
                                if (group.totalDays > 0) {
                                    Text(
                                        "  ·  ${group.totalDays} days",
                                        style = CountriType.monoSmall,
                                        color = palette.textFaint,
                                    )
                                }
                            }
                            Text(
                                group.isoCodes.joinToString(" ") { flagEmoji(it) },
                                style = CountriType.body,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                            if (group.cities.isNotEmpty()) {
                                Text(
                                    group.cities.joinToString(" · "),
                                    style = CountriType.bodySmall,
                                    color = palette.textFaint,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Box(Modifier.padding(horizontal = 22.dp, vertical = 30.dp)) {
                Text(
                    "Add your first country and the timeline starts here.",
                    style = CountriType.bodySmall,
                    color = palette.textFaint,
                )
            }
        }
        // ---- Data: export lands wherever the picker points (incl. Drive) ----
        SectionLabel("Data", Modifier.padding(horizontal = 22.dp, vertical = 6.dp))
        val exportLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            if (uri != null) scope.launch {
                val text = viewModel.exportBackup()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(text.toByteArray())
                    }
                }
                haptics.confirm()
            }
        }
        val importLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) scope.launch {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                }
                if (text != null && viewModel.importBackup(text)) haptics.confirm()
                else haptics.reject()
            }
        }
        Column(
            Modifier
                .padding(horizontal = 22.dp, vertical = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(palette.surface1)
        ) {
            DataRow("Back up", "Save everything as a file — Drive, anywhere") {
                exportLauncher.launch("countri-backup.json")
            }
            DataRow("Restore", "Replace this atlas with a backup file") {
                importLauncher.launch(arrayOf("application/json"))
            }
        }

        Box(Modifier.height(110.dp))
    }
}

@Composable
private fun DataRow(title: String, subtitle: String, onClick: () -> Unit) {
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
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = CountriType.body, color = palette.textPrimary)
            Text(
                subtitle,
                style = CountriType.bodySmall,
                color = palette.textFaint,
                modifier = Modifier.padding(top = 1.dp),
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

@Composable
private fun TimelineSpine(isLast: Boolean) {
    val palette = Countri.palette
    Canvas(
        Modifier
            .padding(horizontal = 16.dp)
            .width(10.dp)
            .height(if (isLast) 24.dp else 44.dp)
    ) {
        drawCircle(
            color = palette.visited,
            radius = 4.dp.toPx(),
            center = Offset(size.width / 2f, 7.dp.toPx()),
        )
        if (!isLast) {
            drawRect(
                color = palette.textPrimary.copy(alpha = 0.10f),
                topLeft = Offset(size.width / 2f - 0.75.dp.toPx(), 13.dp.toPx()),
                size = Size(1.5.dp.toPx(), size.height - 13.dp.toPx()),
            )
        }
    }
}

@Composable
private fun ProgressRing(fraction: Float, label: String, sublabel: String) {
    val palette = Countri.palette
    Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 13.dp.toPx()
            val inset = stroke / 2f + 1.dp.toPx()
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            drawArc(
                color = palette.textPrimary.copy(alpha = 0.07f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            if (fraction > 0.002f) {
                // Sweep through the aurora, starting from 12 o'clock.
                rotate(-90f) {
                    drawArc(
                        color = palette.visited,
                        startAngle = 0f,
                        sweepAngle = 360f * fraction.coerceAtMost(1f),
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                label,
                style = CountriType.displaySmall,
                color = palette.visited,
            )
            Text(
                sublabel,
                style = CountriType.monoSmall,
                color = palette.textSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
