package dev.sam.countri.ui.map

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import dev.sam.countri.data.catalog.CountryCatalog
import dev.sam.countri.data.map.WorldMapData
import dev.sam.countri.domain.CountryStatus
import dev.sam.countri.ui.theme.LocalCountriPalette
import dev.sam.countri.ui.theme.Springs
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min

/**
 * The world, drawn as real country silhouettes. One composable serves the
 * Atlas hero (interactive: pinch-zoom/pan flat, spin globe, morphing), the
 * onboarding backdrop (auto-rotating globe), and static locator crops.
 */
@Composable
fun WorldMap(
    data: WorldMapData,
    statuses: Map<Int, CountryStatus>,
    mode: MapMode,
    modifier: Modifier = Modifier,
    state: WorldMapState = rememberWorldMapState(mode),
    fixedViewport: MapViewport? = null,
    interactive: Boolean = true,
    autoRotate: Boolean = true,
    introUnwrap: Boolean = false,
    onIntroConsumed: () -> Unit = {},
    justAddedIso: String? = null,
    selectedIso: String? = null,
    onCountryTap: ((String) -> Unit)? = null,
) {
    val palette = LocalCountriPalette.current
    val colors = remember(palette) { MapColors(palette) }
    val renderer = remember(data) { WorldMapRenderer(data) }
    val density = LocalDensity.current.density
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val scope = rememberCoroutineScope()
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    // ---- mode drives the morph spring; this IS the flat↔globe animation ----
    LaunchedEffect(mode, introUnwrap) {
        if (introUnwrap) {
            // Opening move: the globe unwraps into the flat atlas.
            onIntroConsumed()
            state.morph.snapTo(1f)
            state.rotationDeg = -75f
            launch {
                animate(-75f, WorldMapState.INITIAL_ROTATION, animationSpec = Springs.Gentle) { v, _ ->
                    state.rotationDeg = v
                }
            }
            state.morph.animateTo(if (mode == MapMode.Globe) 1f else 0f, Springs.Gentle)
        } else {
            val target = if (mode == MapMode.Globe) 1f else 0f
            if (state.morph.value != target) {
                // A slow, luxurious wrap — the signature move deserves time.
                state.morph.animateTo(
                    target,
                    androidx.compose.animation.core.spring(
                        dampingRatio = 0.82f,
                        stiffness = 200f,
                    ),
                )
            }
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

    // ---- frame loop: runs while the globe is live or the flat map coasts ----
    val loopActive by remember {
        derivedStateOf { state.morph.value > 0.01f || state.flatFlinging }
    }
    LaunchedEffect(loopActive, autoRotate) {
        if (!loopActive) {
            state.autoSpeed = 0f
            return@LaunchedEffect
        }
        var last = 0L
        while (isActive) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last != 0L) {
                    val dt = ((now - last) / 1e9f).coerceAtMost(0.05f)

                    // Flat map inertia: the pan keeps gliding after release.
                    if (state.morph.value <= 0.01f && state.flatFlinging && !state.dragging &&
                        canvasSize.width > 0f
                    ) {
                        val s = MapProjection.flatScale(canvasSize.width, canvasSize.height, state.zoom)
                        state.centerLon -= state.flatVelX * dt / s
                        state.centerLat += state.flatVelY * dt / s
                        state.clampCamera(canvasSize.width, canvasSize.height)
                        val decay = exp(-dt * 3.0f)
                        state.flatVelX *= decay
                        state.flatVelY *= decay
                        if (!state.flatFlinging) {
                            state.flatVelX = 0f
                            state.flatVelY = 0f
                        }
                    }

                    var v = state.flingVelocity
                    if (abs(v) > 0.5f) {
                        state.rotationDeg += v * dt
                        v *= exp(-dt * WorldMapState.FLING_FRICTION)
                        state.flingVelocity = if (abs(v) <= 0.5f) 0f else v
                    }
                    val idleMs = now / 1_000_000 - state.lastInteractionAt
                    val coasting = abs(state.flingVelocity) > 0.5f
                    if (state.morph.value > 0.01f && autoRotate && !state.dragging && !coasting &&
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

    // ---- gestures: pan/pinch the flat map, spin the globe, tap anywhere ----
    var gestured = modifier.onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
    if (interactive) {
        gestured = gestured
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    state.dragging = true
                    state.flingVelocity = 0f
                    state.flatVelX = 0f
                    state.flatVelY = 0f
                    state.noteInteraction(SystemClock.uptimeMillis())
                    var travelled = 0f
                    var tracking = false
                    var lastMs = SystemClock.uptimeMillis()
                    var velocityEma = 0f
                    var flatVelXEma = 0f
                    var flatVelYEma = 0f
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.none { it.pressed }) break
                        val pan = event.calculatePan()
                        val zoomChange = event.calculateZoom()
                        val centroid = event.calculateCentroid()
                        travelled += abs(pan.x) + abs(pan.y)
                        if (!tracking && (travelled > touchSlop || zoomChange != 1f)) tracking = true
                        if (!tracking) continue

                        val now = SystemClock.uptimeMillis()
                        val dtMs = (now - lastMs).coerceAtLeast(1)
                        lastMs = now
                        val w = canvasSize.width
                        val h = canvasSize.height
                        if (w <= 0f) continue

                        if (state.morph.value > 0.5f) {
                            val r = MapProjection.globeRadius(w, h)
                            val degrees = pan.x / r * 57.29578f
                            state.rotationDeg += degrees
                            velocityEma = velocityEma * 0.7f + (degrees / dtMs * 1000f) * 0.3f
                        } else {
                            val sOld = MapProjection.flatScale(w, h, state.zoom)
                            if (zoomChange != 1f && centroid.isSpecified) {
                                val geoLon = state.centerLon + (centroid.x - w / 2f) / sOld
                                val geoLat = state.centerLat - (centroid.y - h / 2f) / sOld
                                val rawZoom = state.zoom * zoomChange
                                state.zoom = when {
                                    rawZoom < 1f -> 1f - (1f - rawZoom) * 0.45f
                                    rawZoom > WorldMapState.MAX_ZOOM ->
                                        WorldMapState.MAX_ZOOM + (rawZoom - WorldMapState.MAX_ZOOM) * 0.35f
                                    else -> rawZoom
                                }.coerceIn(0.72f, WorldMapState.MAX_ZOOM * 1.18f)
                                val sNew = MapProjection.flatScale(w, h, state.zoom)
                                state.centerLon = geoLon - (centroid.x - w / 2f) / sNew
                                state.centerLat = geoLat + (centroid.y - h / 2f) / sNew
                            }
                            val s = MapProjection.flatScale(w, h, state.zoom)
                            state.centerLon -= pan.x / s
                            state.centerLat += pan.y / s
                            state.clampCamera(w, h)
                            flatVelXEma = flatVelXEma * 0.7f + (pan.x / dtMs * 1000f) * 0.3f
                            flatVelYEma = flatVelYEma * 0.7f + (pan.y / dtMs * 1000f) * 0.3f
                        }
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                        state.noteInteraction(now)
                    }
                    state.dragging = false
                    if (state.morph.value > 0.5f) {
                        if (abs(velocityEma) > 5f) state.flingVelocity = velocityEma
                    } else {
                        // Let the pan glide on with the finger's momentum.
                        state.flatVelX = flatVelXEma
                        state.flatVelY = flatVelYEma
                        // Overstretched zoom springs back into range.
                        if (state.zoom < 1f || state.zoom > WorldMapState.MAX_ZOOM) {
                            val clamped = state.zoom.coerceIn(1f, WorldMapState.MAX_ZOOM)
                            scope.launch {
                                animate(state.zoom, clamped, animationSpec = Springs.Bouncy) { v, _ ->
                                    state.zoom = v
                                    if (canvasSize.width > 0f) {
                                        state.clampCamera(canvasSize.width, canvasSize.height)
                                    }
                                }
                            }
                        }
                    }
                    state.noteInteraction(SystemClock.uptimeMillis())
                }
            }
            .pointerInput(onCountryTap == null) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        // Double tap: spring the flat map in (or back out from max).
                        if (state.morph.value <= 0.05f && canvasSize.width > 0f) {
                            val w = canvasSize.width
                            val h = canvasSize.height
                            val sOld = MapProjection.flatScale(w, h, state.zoom)
                            val geoLon = state.centerLon + (offset.x - w / 2f) / sOld
                            val geoLat = state.centerLat - (offset.y - h / 2f) / sOld
                            val targetZoom = if (state.zoom > 5.5f) 1f else state.zoom * 2.2f
                            scope.launch {
                                val fromZoom = state.zoom
                                animate(0f, 1f, animationSpec = Springs.Smooth) { t, _ ->
                                    state.zoom = fromZoom + (targetZoom.coerceIn(1f, WorldMapState.MAX_ZOOM) - fromZoom) * t
                                    val s = MapProjection.flatScale(w, h, state.zoom)
                                    state.centerLon = geoLon - (offset.x - w / 2f) / s
                                    state.centerLat = geoLat + (offset.y - h / 2f) / s
                                    state.clampCamera(w, h)
                                }
                            }
                        }
                    },
                ) { offset ->
                    val tap = onCountryTap ?: return@detectTapGestures
                    state.noteInteraction(SystemClock.uptimeMillis())
                    val m = state.morph.value
                    if (m > 0.05f && m < 0.95f) return@detectTapGestures // mid-morph
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val tolPx = 24f * density
                    val viewport = fixedViewport ?: state.flatViewport
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
            viewport = fixedViewport ?: state.flatViewport,
            rotationDeg = state.rotationDeg,
            morph = state.morph.value,
            pulseIndex = justAddedIndex,
            pulseProgress = pulse.value,
            selectedIndex = selectedIndex,
            density = density,
        )
    }
}
