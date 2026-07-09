package dev.sam.countri.ui.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * The whole app moves on four springs. No linear tweens for spatial motion —
 * everything settles with physics, tuned to feel at home on One UI/Pixel.
 */
object Springs {
    /** Toggles, selection states, press feedback. */
    val Fast: SpringSpec<Float> = spring(dampingRatio = 0.9f, stiffness = 900f)

    /** Screen elements arriving, the map morph. */
    val Smooth: SpringSpec<Float> = spring(dampingRatio = 0.85f, stiffness = 380f)

    /** Stamp hits, FAB pop, add-confirm. Deliberate overshoot. */
    val Bouncy: SpringSpec<Float> = spring(dampingRatio = 0.55f, stiffness = 500f)

    /** Large surfaces settling. */
    val Gentle: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 170f)

    /** Smooth spring for IntOffset-based slide transitions. */
    val SmoothOffset: SpringSpec<IntOffset> =
        spring(dampingRatio = 0.85f, stiffness = 380f, visibilityThreshold = IntOffset.VisibilityThreshold)
}

/**
 * Scales down slightly while pressed. Applied to every tappable surface;
 * composes with clickable without consuming events.
 */
fun Modifier.pressScale(scaleDown: Float = 0.965f): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleDown else 1f,
        animationSpec = Springs.Fast,
        label = "pressScale",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                waitForUpOrCancellation()
                pressed = false
            }
        }
}

/**
 * Staggered list entrance: fade + rise, one spring per item, [index] * 40ms
 * apart. Runs once per [key] change.
 */
fun Modifier.staggeredEnter(index: Int, key: Any? = Unit, riseDistance: Dp = 16.dp): Modifier =
    composed {
        var shown by remember(key) { mutableStateOf(false) }
        val progress by animateFloatAsState(
            targetValue = if (shown) 1f else 0f,
            animationSpec = Springs.Smooth,
            label = "staggeredEnter",
        )
        androidx.compose.runtime.LaunchedEffect(key) {
            delay(index.coerceAtMost(20) * 40L)
            shown = true
        }
        graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * riseDistance.toPx()
        }
    }
