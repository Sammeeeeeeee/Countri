package dev.sam.countri.ui.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.sam.countri.domain.AtlasStats
import dev.sam.countri.ui.components.LocalHaptics
import dev.sam.countri.ui.components.tapTarget
import dev.sam.countri.ui.theme.Countri
import dev.sam.countri.ui.theme.CountriType
import dev.sam.countri.ui.theme.pressScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Compose the stats card before sharing: pick the look, pick what's on it,
 * watch the preview update live.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsShareSheet(
    stats: AtlasStats,
    onDismiss: () -> Unit,
) {
    val palette = Countri.palette
    val haptics = LocalHaptics.current
    val context = LocalContext.current
    var options by remember {
        mutableStateOf(
            StatsCardOptions(style = if (palette.isDark) ShareStyle.Dark else ShareStyle.Light)
        )
    }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(options, stats) {
        preview = withContext(Dispatchers.Default) {
            StatsCardRenderer.render(context, stats, options).asImageBitmap()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surface2,
        contentWindowInsets = { WindowInsets(0) },
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 26.dp)
                .navigationBarsPadding()
        ) {
            Text("Share your stats", style = CountriType.title, color = palette.textPrimary)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(1080f / 1350f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.recessed),
                    contentAlignment = Alignment.Center,
                ) {
                    preview?.let {
                        Image(
                            bitmap = it,
                            contentDescription = "Card preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Text("Style", style = CountriType.sectionLabel, color = palette.textFaint)
                    ShareStyle.entries.forEach { style ->
                        OptionChip(
                            text = style.displayName,
                            selected = options.style == style,
                        ) {
                            haptics.tick()
                            options = options.copy(style = style)
                        }
                    }
                    Text(
                        "Include",
                        style = CountriType.sectionLabel,
                        color = palette.textFaint,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    OptionChip("Continents", options.showContinents) {
                        haptics.tick()
                        options = options.copy(showContinents = !options.showContinents)
                    }
                    OptionChip("Timeline", options.showTimeline) {
                        haptics.tick()
                        options = options.copy(showTimeline = !options.showTimeline)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth()
                    .height(56.dp)
                    .pressScale(0.97f)
                    .clip(CircleShape)
                    .background(palette.visited)
                    .tapTarget {
                        haptics.confirm()
                        shareBitmap(
                            context,
                            StatsCardRenderer.render(context, stats, options),
                            "countri-stats.png",
                            "Share your stats",
                        )
                        onDismiss()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("Share", style = CountriType.subtitle, color = palette.onVisited)
            }
        }
    }
}

@Composable
private fun OptionChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val palette = Countri.palette
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(0.97f)
            .clip(CircleShape)
            .background(if (selected) palette.visited else palette.recessed)
            .tapTarget(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            text,
            style = CountriType.bodySmall,
            color = if (selected) palette.onVisited else palette.textPrimary,
        )
    }
}
