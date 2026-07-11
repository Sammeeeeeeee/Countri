package dev.sam.countri.ui.nav

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.sam.countri.ui.components.CountriIcons
import dev.sam.countri.ui.components.LocalHaptics
import dev.sam.countri.ui.theme.Countri
import dev.sam.countri.ui.theme.CountriType
import dev.sam.countri.ui.theme.pressScale

enum class CountriTab { Atlas, Passport, Stats, Wishlist }

/**
 * Revolut's floating nav: a white pill hovering above the bottom edge,
 * five items inside, the active one sitting in a recessed highlight.
 * Scrolling down minimizes it — labels collapse and the pill slims.
 */
@Composable
fun CountriBottomBar(
    current: CountriTab?,
    minimized: Boolean,
    onTab: (CountriTab) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Countri.palette
    val haptics = LocalHaptics.current

    val barHeight by animateDpAsState(
        targetValue = if (minimized) 52.dp else 66.dp,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f),
        label = "barHeight",
    )
    val labelHeight by animateDpAsState(
        targetValue = if (minimized) 0.dp else 14.dp,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f),
        label = "labelHeight",
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (minimized) 0f else 1f,
        animationSpec = spring(dampingRatio = 1f, stiffness = 500f),
        label = "labelAlpha",
    )

    BoxWithConstraints(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 14.dp)
            .padding(bottom = 6.dp)
            .fillMaxWidth()
            .height(barHeight)
            .shadow(
                elevation = if (palette.isDark) 0.dp else 16.dp,
                shape = CircleShape,
                ambientColor = Color(0x2E000000),
                spotColor = Color(0x2E000000),
            )
            .clip(CircleShape)
            .background(palette.surface1)
            .then(
                if (palette.isDark) Modifier.border(1.dp, palette.hairline, CircleShape)
                else Modifier
            )
            .padding(horizontal = 10.dp),
    ) {
        // The active-tab highlight slides between slots, Revolut-style.
        val slotWidth = maxWidth / 5
        val slotIndex = when (current) {
            CountriTab.Atlas -> 0
            CountriTab.Passport -> 1
            CountriTab.Stats -> 3
            CountriTab.Wishlist -> 4
            null -> 0
        }
        val highlightX by animateDpAsState(
            targetValue = slotWidth * slotIndex + (slotWidth - 46.dp) / 2,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 480f),
            label = "tabSlide",
        )
        val highlightYShift by animateDpAsState(
            targetValue = -(labelHeight + 2.dp) / 2,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f),
            label = "tabSlideY",
        )
        if (current != null) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = highlightX, y = highlightYShift)
                    .size(width = 46.dp, height = 28.dp)
                    .clip(CircleShape)
                    .background(palette.recessed)
            )
        }
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarItem(
            icon = CountriIcons.Atlas,
            label = "Atlas",
            selected = current == CountriTab.Atlas,
            labelHeight = labelHeight,
            labelAlpha = labelAlpha,
            modifier = Modifier.weight(1f),
        ) { haptics.tick(); onTab(CountriTab.Atlas) }
        BarItem(
            icon = CountriIcons.Passport,
            label = "Passport",
            selected = current == CountriTab.Passport,
            labelHeight = labelHeight,
            labelAlpha = labelAlpha,
            modifier = Modifier.weight(1f),
        ) { haptics.tick(); onTab(CountriTab.Passport) }

        // The + item: a filled ink coin, RevPoints-style.
        Column(
            modifier = Modifier
                .weight(1f)
                .pressScale(0.9f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { haptics.tick(); onAdd() },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(palette.visited),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    CountriIcons.Plus,
                    contentDescription = "Add a country",
                    tint = palette.onVisited,
                    modifier = Modifier.size(15.dp),
                )
            }
            Text(
                "Add",
                style = CountriType.monoSmall,
                color = palette.textPrimary,
                maxLines = 1,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .height(labelHeight)
                    .alpha(labelAlpha),
            )
        }

        BarItem(
            icon = CountriIcons.Stats,
            label = "Stats",
            selected = current == CountriTab.Stats,
            labelHeight = labelHeight,
            labelAlpha = labelAlpha,
            modifier = Modifier.weight(1f),
        ) { haptics.tick(); onTab(CountriTab.Stats) }
        BarItem(
            icon = CountriIcons.Wishlist,
            label = "Wishlist",
            selected = current == CountriTab.Wishlist,
            labelHeight = labelHeight,
            labelAlpha = labelAlpha,
            modifier = Modifier.weight(1f),
        ) { haptics.tick(); onTab(CountriTab.Wishlist) }
    }
    }
}

@Composable
private fun BarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    labelHeight: Dp,
    labelAlpha: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = Countri.palette
    val tint by animateColorAsState(
        targetValue = if (selected) palette.textPrimary else palette.textFaint,
        label = "tabTint",
    )
    // Little spring pop when the tab becomes active.
    val pop = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected) {
            pop.snapTo(0.72f)
            pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 600f))
        }
    }
    Column(
        modifier = modifier
            .pressScale(0.92f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(width = 44.dp, height = 26.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier
                    .size(21.dp)
                    .graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                    },
            )
        }
        Text(
            label,
            style = CountriType.monoSmall,
            color = tint,
            maxLines = 1,
            modifier = Modifier
                .padding(top = 2.dp)
                .height(labelHeight)
                .alpha(labelAlpha),
        )
    }
}
