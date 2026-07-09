package dev.sam.countri.ui.map

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Globe/morph state that outlives recomposition: rotation, fling velocity,
 * auto-rotation ramp, and the flat↔globe morph parameter.
 */
class WorldMapState(initialMode: MapMode) {
    /** 0 = flat, 1 = globe; animated with a spring when the mode changes. */
    val morph = Animatable(if (initialMode == MapMode.Globe) 1f else 0f)

    var rotationDeg by mutableFloatStateOf(INITIAL_ROTATION)

    /** Residual fling velocity in degrees/second. */
    var flingVelocity by mutableFloatStateOf(0f)

    var dragging by mutableStateOf(false)

    /** Uptime millis of the last user touch; drives the auto-rotate ramp. */
    var lastInteractionAt by mutableLongStateOf(0L)

    /** Current auto-rotation speed, ramped toward [AUTO_SPEED] when idle. */
    var autoSpeed by mutableFloatStateOf(0f)

    fun noteInteraction(now: Long) {
        lastInteractionAt = now
        autoSpeed = 0f
    }

    companion object {
        const val AUTO_SPEED = 4f // deg/s
        const val AUTO_RAMP_SECONDS = 1.5f
        const val IDLE_BEFORE_AUTO_MS = 3_000L
        const val FLING_FRICTION = 1.4f
        // Start centered between Europe and Africa so first look feels alive.
        const val INITIAL_ROTATION = -16f
    }
}

@Composable
fun rememberWorldMapState(initialMode: MapMode): WorldMapState =
    remember { WorldMapState(initialMode) }
