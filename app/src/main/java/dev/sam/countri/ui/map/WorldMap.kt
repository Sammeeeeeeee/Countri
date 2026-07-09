package dev.sam.countri.ui.map

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import dev.sam.countri.data.catalog.CountryCatalog
import dev.sam.countri.data.map.WorldMapData
import dev.sam.countri.domain.CountryStatus
import dev.sam.countri.ui.theme.LocalCountriPalette
import dev.sam.countri.ui.theme.Springs
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min

/**
 * The world, drawn as real country silhouettes. One composable serves the
 * Atlas hero (interactive, morphing), the onboarding backdrop (auto-rotating
 * globe), and the detail locator (zoomed flat viewport).
 */
@Composable
fun WorldMap(
    data: WorldMapData,
    statuses: Map<Int, CountryStatus>,
    mode: MapMode,
    modifier: Modifier = Modifier,
    state: WorldMapState = rememberWorldMapState(mode),
    viewport: MapViewport = MapViewport.World,
    interactive: Boolean = true,
    autoRotate: Boolean = true,
    justAddedIso: String? = null,
    selectedIso: String? = null,
    onCountryTap: ((String) -> Unit)? = null,
) {
    val palette = LocalCountriPalette.current
    val colors = remember(palette) { MapColors(palette) }
    val renderer = remember(data) { WorldMapRenderer(data) }
    val density = LocalDensity.current.density
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    // ---- mode drives the morph spring; this IS the flat↔globe animation ----
    LaunchedEffect(mode) {
        val target = if (mode == MapMode.Globe) 1f else 0f
        if (state.morph.value != target) {
            state.morph.animateTo(target, Springs.Smooth)
        }
    }

    // ---- just-added pulse: two expanding rings around the new country ----
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(justAddedIso) {
        pulse.snapTo(0f)
        if (justAddedIso != null) {
            repeat(2) {
                pulse.snapTo(0f)
                pulse.animateTo(1f, tween(850, easing = LinearOutSlowInEasing))
            }
            pulse.snapTo(0f)
        }
    }

    // ---- frame loop: only runs while the globe is live ----
    val globeActive by remember {
        derivedStateOf { state.morph.value > 0.01f }
    }
    LaunchedEffect(globeActive, autoRotate) {
        if (!globeActive) {
            state.autoSpeed = 0f
            return@LaunchedEffect
        }
        var last = 0L
        while (isActive) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last != 0L) {
                    val dt = ((now - last) / 1e9f).coerceAtMost(0.05f)
                    var v = state.flingVelocity
                    if (abs(v) > 0.5f) {
                        state.rotationDeg += v * dt
                        v *= exp(-dt * WorldMapState.FLING_FRICTION)
                        state.flingVelocity = if (abs(v) <= 0.5f) 0f else v
                    }
                    val idleMs = now / 1_000_000 - state.lastInteractionAt
                    val coasting = abs(state.flingVelocity) > 0.5f
                    if (autoRotate && !state.dragging && !coasting &&
                        (idleMs > WorldMapState.IDLE_BEFORE_AUTO_MS || !interactive)
                    ) {
                        state.autoSpeed = min(
                            WorldMapState.AUTO_SPEED,
                            state.autoSpeed + dt * (WorldMapState.AUTO_SPEED / WorldMapState.AUTO_RAMP_SECONDS),
                        )
                        state.rotationDeg += state.autoSpeed * dt
                    }
                }
                last = now
            }
        }
    }

    // ---- gestures ----
    var gestured = modifier.onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
    if (interactive) {
        val dragState = rememberDraggableState { delta ->
            if (state.morph.value > 0.5f && canvasSize.width > 0f) {
                val r = MapProjection.globeRadius(canvasSize.width, canvasSize.height)
                state.rotationDeg += delta / r * 57.29578f
            }
        }
        gestured = gestured
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                onDragStarted = {
                    state.dragging = true
                    state.flingVelocity = 0f
                    state.noteInteraction(SystemClock.uptimeMillis())
                },
                onDragStopped = { velocity ->
                    state.dragging = false
                    if (state.morph.value > 0.5f && canvasSize.width > 0f) {
                        val r = MapProjection.globeRadius(canvasSize.width, canvasSize.height)
                        state.flingVelocity = velocity / r * 57.29578f
                    }
                    state.noteInteraction(SystemClock.uptimeMillis())
                },
            )
            .pointerInput(onCountryTap == null, viewport) {
                detectTapGestures { offset ->
                    val tap = onCountryTap ?: return@detectTapGestures
                    state.noteInteraction(SystemClock.uptimeMillis())
                    val m = state.morph.value
                    if (m > 0.05f && m < 0.95f) return@detectTapGestures // mid-morph
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val tolPx = 24f * density
                    val lonLat: Pair<Float, Float>?
                    val tolDeg: Float
                    if (m <= 0.05f) {
                        lonLat = MapProjection.inverseFlat(offset.x, offset.y, viewport, w, h)
                        tolDeg = tolPx / MapProjection.flatScale(w, h, viewport.zoom)
                    } else {
                        lonLat = MapProjection.inverseGlobe(offset.x, offset.y, w, h, state.rotationDeg)
                        tolDeg = tolPx / MapProjection.globeRadius(w, h) * 57.29578f
                    }
                    val (lon, lat) = lonLat ?: return@detectTapGestures
                    val idx = data.countryNear(lon, lat, tolDeg.coerceAtMost(6f))
                    if (idx != 0) tap(CountryCatalog.all[idx - 1].iso2)
                }
            }
    }

    val justAddedIndex = justAddedIso?.let { CountryCatalog.indexOf(it) } ?: 0
    val selectedIndex = selectedIso?.let { CountryCatalog.indexOf(it) } ?: 0

    Canvas(gestured) {
        renderer.draw(
            scope = this,
            colors = colors,
            statuses = statuses,
            viewport = viewport,
            rotationDeg = state.rotationDeg,
            morph = state.morph.value,
            pulseIndex = justAddedIndex,
            pulseProgress = pulse.value,
            selectedIndex = selectedIndex,
            density = density,
        )
    }
}
