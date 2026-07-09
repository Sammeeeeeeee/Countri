package dev.sam.countri.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.sam.countri.ui.AtlasViewModel
import dev.sam.countri.ui.components.LocalHaptics
import dev.sam.countri.ui.map.MapMode
import dev.sam.countri.ui.map.WorldMap
import dev.sam.countri.ui.theme.Countri
import dev.sam.countri.ui.theme.CountriType
import dev.sam.countri.ui.theme.Springs
import dev.sam.countri.ui.theme.pressScale

private data class Slide(val kicker: String, val title: String, val body: String, val cta: String)

private val slides = listOf(
    Slide(
        kicker = "COUNTRI",
        title = "Where have\nyou been?",
        body = "Mark the countries you've visited, and the ones you're saving up for.",
        cta = "Next",
    ),
    Slide(
        kicker = "01 · KEEP COUNT",
        title = "One tap\nper country.",
        body = "Add the year, the cities, and a line to remember it by.",
        cta = "Next",
    ),
    Slide(
        kicker = "02 · WATCH IT FILL",
        title = "195 countries.\nYour move.",
        body = "The map fills in as you go. The globe keeps score.",
        cta = "Open your atlas",
    ),
)

@Composable
fun OnboardingScreen(
    viewModel: AtlasViewModel,
    onDone: () -> Unit,
) {
    val palette = Countri.palette
    val haptics = LocalHaptics.current
    var step by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize().background(palette.canvas)) {
        // Rotating globe fills the upper two thirds, fading into the canvas.
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
                .offset(y = (-30).dp)
        ) {
            WorldMap(
                data = viewModel.worldMap,
                statuses = emptyMap(),
                mode = MapMode.Globe,
                interactive = false,
                autoRotate = true,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            0.45f to Color.Transparent,
                            1f to palette.canvas,
                        )
                    )
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.62f)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to palette.canvas,
                    )
                )
        )

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 30.dp)
                .navigationBarsPadding()
                .padding(bottom = 26.dp)
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (slideInHorizontally(Springs.SmoothOffset) { it / 5 } + fadeIn()) togetherWith
                        (slideOutHorizontally(Springs.SmoothOffset) { -it / 5 } + fadeOut())
                },
                label = "slide",
            ) { index ->
                val slide = slides[index]
                Column {
                    Text(
                        slide.kicker,
                        style = CountriType.sectionLabel.copy(letterSpacing = 0.42.em),
                        color = palette.visited,
                    )
                    Text(
                        slide.title,
                        style = CountriType.displayLarge.copy(fontSize = 50.sp, lineHeight = 50.sp),
                        color = palette.textPrimary,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Text(
                        slide.body,
                        style = CountriType.body,
                        color = palette.textSecondary,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(0.85f),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .padding(top = 28.dp)
                    .fillMaxWidth()
                    .height(56.dp)
                    .pressScale(0.97f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.visited)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        haptics.tick()
                        if (step >= slides.lastIndex) {
                            haptics.confirm()
                            onDone()
                        } else {
                            step++
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(slides[step].cta, style = CountriType.subtitle, color = palette.onVisited)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    repeat(slides.size) { i ->
                        val width by animateDpAsState(
                            targetValue = if (i == step) 20.dp else 7.dp,
                            label = "dot",
                        )
                        Box(
                            Modifier
                                .size(width = width, height = 7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i == step) palette.visited
                                    else palette.textPrimary.copy(alpha = 0.2f)
                                )
                        )
                    }
                }
                Text(
                    "Skip",
                    style = CountriType.bodySmall,
                    color = palette.textFaint,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onDone() }
                        .padding(8.dp),
                )
            }
        }
    }
}
