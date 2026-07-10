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
 * Map view state that outlives recomposition: the flat pan/zoom camera,
 * globe rotation, fling velocity, auto-rotation ramp, and the flat↔globe
 * morph parameter.
 */
class WorldMapState(initialMode: MapMode) {
    /** 0 = flat, 1 = globe; animated with a spring when the mode changes. */
    val morph = Animatable(if (initialMode == MapMode.Globe) 1f else 0f)

    // ---- flat camera ----
    var centerLon by mutableFloatStateOf(MapViewport.World.centerLon)
    var centerLat by mutableFloatStateOf(MapViewport.World.centerLat)
    var zoom by mutableFloatStateOf(1f)

    val flatViewport: MapViewport get() = MapViewport(centerLon, centerLat, zoom)

    /** Keeps the camera inside the world band for the current canvas size. */
    fun clampCamera(w: Float, h: Float) {
        zoom = zoom.coerceIn(1f, MAX_ZOOM)
        val s = MapProjection.flatScale(w, h, zoom)
        val halfLon = w / (2f * s)
        val halfLat = h / (2f * s)
        centerLon = clampCentered(centerLon, -180f + halfLon, 180f - halfLon, 11f)
        centerLat = clampCentered(
            centerLat,
            MapProjection.MIN_LAT + halfLat,
            MapProjection.MAX_LAT - halfLat,
            16f,
        )
    }

    private fun clampCentered(v: Float, min: Float, max: Float, fallback: Float): Float =
        if (min > max) fallback else v.coerceIn(min, max)

    /** Flat-map pan inertia, px/second. */
    var flatVelX by mutableFloatStateOf(0f)
    var flatVelY by mutableFloatStateOf(0f)

    val flatFlinging: Boolean get() = kotlin.math.abs(flatVelX) + kotlin.math.abs(flatVelY) > 30f

    // ---- globe ----
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
        const val MAX_ZOOM = 8f
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
