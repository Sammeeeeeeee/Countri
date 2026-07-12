package dev.sam.countri.ui.nav

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * The tab glyphs, drawn live so they can move: selecting a tab doesn't just
 * pop the icon, it plays the glyph — the globe spins a meridian past, the
 * passport gets stamped, the bars find their levels, the bookmark flaps.
 *
 * [progress] runs 0→1 on selection (springy, may overshoot); 1 is rest.
 * Cutouts punch through to transparency via an offscreen layer, so the
 * glyphs stay true silhouettes on any background.
 */
@Composable
fun TabGlyph(
    tab: CountriTab,
    tint: Color,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        when (tab) {
            CountriTab.Atlas -> drawAtlas(tint, progress)
            CountriTab.Passport -> drawPassport(tint, progress)
            CountriTab.Stats -> drawStats(tint, progress)
            CountriTab.Wishlist -> drawWishlist(tint, progress)
        }
    }
}

/** Solid globe; the meridian slit sweeps across once, like a slow spin. */
private fun DrawScope.drawAtlas(tint: Color, t: Float) {
    val u = size.minDimension / 24f
    val center = Offset(12f * u, 12f * u)
    val r = 9.4f * u
    drawCircle(tint, r, center)

    val sphere = Path().apply {
        addOval(Rect(center.x - r, center.y - r, center.x + r, center.y + r))
    }
    clipPath(sphere) {
        // Inset from the rim so the equator doesn't sever the globe into halves.
        drawRect(
            Color.Black,
            Offset(3.6f * u, 11.1f * u),
            Size(16.8f * u, 1.8f * u),
            blendMode = BlendMode.Clear,
        )
        // m is where the meridian sits: -1 left limb, 0 center, +1 right limb.
        val rest = -0.12f
        val m = when {
            t >= 1f -> rest
            t < 0.45f -> rest + (1.15f - rest) * (t / 0.45f)
            t < 0.55f -> -1.15f
            else -> -1.15f + (rest + 1.15f) * ((t - 0.55f) / 0.45f)
        }
        if (abs(m) <= 1.02f) {
            val w = 1.8f * (1f - 0.45f * abs(m))
            val xEdge = 12f + m * 4.2f
            val xMid = 12f + m * 8.4f
            val band = Path().apply {
                moveTo((xEdge - w * 0.33f) * u, 2.9f * u)
                quadraticBezierTo((xMid - w * 0.5f) * u, 12f * u, (xEdge - w * 0.33f) * u, 21.1f * u)
                lineTo((xEdge + w * 0.67f) * u, 21.1f * u)
                quadraticBezierTo((xMid + w * 0.5f) * u, 12f * u, (xEdge + w * 0.67f) * u, 2.9f * u)
                close()
            }
            drawPath(band, Color.Black, blendMode = BlendMode.Clear)
        }
    }
}

/** The booklet takes a stamp: a wobble, the portrait pops in, the name line sweeps. */
private fun DrawScope.drawPassport(tint: Color, t: Float) {
    val u = size.minDimension / 24f
    val tc = t.coerceIn(0f, 1f)
    val wobble = -7f * sin(tc * PI.toFloat())
    rotate(wobble, pivot = Offset(12f * u, 12f * u)) {
        drawRoundRect(
            tint,
            Offset(4.6f * u, 2.6f * u),
            Size(14.8f * u, 18.8f * u),
            CornerRadius(2.4f * u),
        )
        val portrait = t.coerceIn(0f, 1.22f)
        if (portrait > 0.01f) {
            drawCircle(
                Color.Black,
                3.1f * u * portrait,
                Offset(12f * u, 9.7f * u),
                blendMode = BlendMode.Clear,
            )
        }
        val line = ((t - 0.35f) / 0.65f).coerceIn(0f, 1f)
        if (line > 0.01f) {
            drawRoundRect(
                Color.Black,
                Offset(8.6f * u, 15.4f * u),
                Size(6.8f * u * line, 1.8f * u),
                CornerRadius(0.9f * u),
                blendMode = BlendMode.Clear,
            )
        }
    }
}

/** Bars drop to a crouch and spring back up in a stagger. */
private fun DrawScope.drawStats(tint: Color, t: Float) {
    val u = size.minDimension / 24f
    val rests = floatArrayOf(9.5f, 17f, 7f)
    val xs = floatArrayOf(3.5f, 9.5f, 15.5f)
    for (i in 0..2) {
        val phase = ((t - 0.14f * i) / (1f - 0.14f * i)).coerceAtLeast(0f)
        val h = (rests[i] * (0.3f + 0.7f * phase)).coerceIn(2f, 17.9f)
        drawRoundRect(
            tint,
            Offset(xs[i] * u, (20.5f - h) * u),
            Size(5f * u, h * u),
            CornerRadius(0.9f * u),
        )
    }
}

/** The bookmark's ribbon flattens, then the notch springs back in. */
private fun DrawScope.drawWishlist(tint: Color, t: Float) {
    val u = size.minDimension / 24f
    val tc = t.coerceIn(0f, 1f)
    val squash = 1f - 0.08f * sin(tc * PI.toFloat())
    scale(1f, squash, pivot = Offset(12f * u, 21.4f * u)) {
        val tipY = 21.4f - 4.3f * t.coerceIn(0f, 1.3f)
        val path = Path().apply {
            moveTo(4.6f * u, 4.4f * u)
            quadraticBezierTo(4.6f * u, 2.6f * u, 6.4f * u, 2.6f * u)
            lineTo(17.6f * u, 2.6f * u)
            quadraticBezierTo(19.4f * u, 2.6f * u, 19.4f * u, 4.4f * u)
            lineTo(19.4f * u, 21.4f * u)
            lineTo(12f * u, tipY * u)
            lineTo(4.6f * u, 21.4f * u)
            close()
        }
        drawPath(path, tint)
    }
}
