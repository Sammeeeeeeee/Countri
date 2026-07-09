package dev.sam.countri.ui.map

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import dev.sam.countri.data.map.WorldMapData
import dev.sam.countri.domain.CountryStatus
import dev.sam.countri.ui.theme.CountriPalette
import kotlin.math.sqrt

/** Palette snapshot as ARGB ints, ready for android.graphics.Paint. */
class MapColors(palette: CountriPalette) {
    val isDark = palette.isDark
    val land = palette.mapLand.toArgb()
    val border = palette.mapBorder.toArgb()
    val visited = palette.visited.toArgb()
    val wishlist = palette.wishlist.toArgb()
    val sphereCenter = palette.globeShade.toArgb()
    val sphereEdge = palette.canvas.copy(alpha = 0f).toArgb()
}

/**
 * Draws the whole world as filled country silhouettes on a Compose Canvas.
 * One instance per WorldMapData; every buffer is preallocated, and the only
 * steady-state allocation per frame is zero.
 */
class WorldMapRenderer(private val data: WorldMapData) {

    private val n = data.vertexCount
    private val x = FloatArray(n)
    private val y = FloatArray(n)
    private val depth = FloatArray(n)

    // Reused Path per catalog index (0 = untagged land). Even-odd fill keeps
    // enclave holes (Lesotho) open.
    private val countryCount = 196
    private val paths = Array(countryCount) { Path().apply { fillType = Path.FillType.EVEN_ODD } }
    private val pathUsed = BooleanArray(countryCount)
    private val depthSum = FloatArray(countryCount)
    private val depthCount = IntArray(countryCount)

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }
    private val spherePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var sphereShaderKey = 0f

    /**
     * @param morph 0 flat … 1 globe
     * @param pulseIndex catalog index playing the just-added pulse, or 0
     * @param pulseProgress 0..1 phase of the expanding pulse ring
     * @param selectedIndex highlighted country (detail locator), or 0
     */
    fun draw(
        scope: DrawScope,
        colors: MapColors,
        statuses: Map<Int, CountryStatus>,
        viewport: MapViewport,
        rotationDeg: Float,
        morph: Float,
        pulseIndex: Int = 0,
        pulseProgress: Float = 0f,
        selectedIndex: Int = 0,
        density: Float,
    ) {
        val w = scope.size.width
        val h = scope.size.height
        if (w <= 0f || h <= 0f) return
        val canvas = scope.drawContext.canvas.nativeCanvas

        MapProjection.projectAll(data, viewport, w, h, rotationDeg, morph, x, y, depth)

        // ---- sphere disk under the globe ----
        if (morph > 0.01f) {
            val r = MapProjection.globeRadius(w, h)
            if (sphereShaderKey != r) {
                sphereShaderKey = r
                spherePaint.shader = RadialGradient(
                    w / 2f, h / 2f, r * 1.05f,
                    intArrayOf(colors.sphereCenter, colors.sphereCenter, colors.sphereEdge),
                    floatArrayOf(0f, 0.82f, 1f),
                    Shader.TileMode.CLAMP,
                )
            }
            spherePaint.alpha = (morph * 255).toInt()
            canvas.drawCircle(w / 2f, h / 2f, r * 1.05f, spherePaint)
        }

        buildPaths(w, h, morph)

        // ---- fills ----
        for (c in 0 until countryCount) {
            if (!pathUsed[c]) continue
            val alpha = countryAlpha(c, morph)
            if (alpha < 0.02f) continue
            val status = statuses[c]
            fillPaint.color = when (status) {
                CountryStatus.VISITED -> colors.visited
                CountryStatus.WISHLIST -> colors.wishlist
                null -> colors.land
            }
            fillPaint.alpha = (alpha * when (status) {
                CountryStatus.VISITED -> if (colors.isDark) 82 else 60
                CountryStatus.WISHLIST -> if (colors.isDark) 74 else 56
                null -> 255
            }).toInt()
            canvas.drawPath(paths[c], fillPaint)
        }

        // ---- hairline borders ----
        strokePaint.strokeWidth = 1f * density
        strokePaint.color = colors.border
        for (c in 0 until countryCount) {
            if (!pathUsed[c]) continue
            val alpha = countryAlpha(c, morph)
            if (alpha < 0.02f) continue
            strokePaint.alpha = (alpha * 40).toInt()
            canvas.drawPath(paths[c], strokePaint)
        }

        // ---- accent strokes + glow for tracked countries ----
        for ((c, status) in statuses) {
            if (c !in 1 until countryCount || !pathUsed[c]) continue
            val alpha = countryAlpha(c, morph)
            if (alpha < 0.02f) continue
            val accent = if (status == CountryStatus.VISITED) colors.visited else colors.wishlist
            if (colors.isDark) {
                strokePaint.color = accent
                strokePaint.strokeWidth = 4.5f * density
                strokePaint.alpha = (alpha * 38).toInt()
                canvas.drawPath(paths[c], strokePaint)
            }
            strokePaint.color = accent
            strokePaint.strokeWidth = 1.2f * density
            strokePaint.alpha = (alpha * 220).toInt()
            canvas.drawPath(paths[c], strokePaint)
        }

        // ---- selection emphasis (detail locator) ----
        if (selectedIndex in 1 until countryCount && pathUsed[selectedIndex]) {
            val status = statuses[selectedIndex]
            val accent = when (status) {
                CountryStatus.WISHLIST -> colors.wishlist
                else -> colors.visited
            }
            fillPaint.color = accent
            fillPaint.alpha = if (colors.isDark) 96 else 70
            canvas.drawPath(paths[selectedIndex], fillPaint)
            strokePaint.color = accent
            strokePaint.strokeWidth = 1.6f * density
            strokePaint.alpha = 255
            canvas.drawPath(paths[selectedIndex], strokePaint)
        }

        // ---- just-added pulse ----
        if (pulseIndex in 1 until countryCount && pathUsed[pulseIndex] && pulseProgress > 0f) {
            val status = statuses[pulseIndex]
            val accent = if (status == CountryStatus.WISHLIST) colors.wishlist else colors.visited
            strokePaint.color = accent
            strokePaint.strokeWidth = (1.5f + 9f * pulseProgress) * density
            strokePaint.alpha = ((1f - pulseProgress) * 170).toInt()
            canvas.drawPath(paths[pulseIndex], strokePaint)
        }
    }

    private fun countryAlpha(c: Int, morph: Float): Float {
        if (morph <= 0.01f) return 1f
        val count = depthCount[c]
        if (count == 0) return 1f
        val avg = depthSum[c] / count
        // smoothstep(-0.12, 0.18, avg): fade countries as they roll behind.
        val t = ((avg + 0.12f) / 0.30f).coerceIn(0f, 1f)
        val vis = t * t * (3f - 2f * t)
        return 1f + (vis - 1f) * morph
    }

    private fun buildPaths(w: Float, h: Float, morph: Float) {
        java.util.Arrays.fill(pathUsed, false)
        java.util.Arrays.fill(depthSum, 0f)
        java.util.Arrays.fill(depthCount, 0)

        val cx = w / 2f
        val cy = h / 2f
        val r = MapProjection.globeRadius(w, h)
        val clampToLimb = morph > 0.5f

        for (ring in 0 until data.ringCount) {
            val c = data.ringCountry[ring]
            val start = data.ringStart[ring]
            val size = data.ringSize[ring]

            var maxDepth = -2f
            for (i in start until start + size) {
                val d = depth[i]
                if (d > maxDepth) maxDepth = d
                depthSum[c] += d
                depthCount[c]++
            }
            // Fully behind the sphere: not part of this frame.
            if (morph > 0.5f && maxDepth < -0.05f) continue

            val path = paths[c]
            if (!pathUsed[c]) {
                path.rewind()
                pathUsed[c] = true
            }
            for (i in start until start + size) {
                var px = x[i]
                var py = y[i]
                if (clampToLimb && depth[i] < 0f) {
                    // Push behind-the-limb vertices onto the sphere edge so
                    // limb-straddling shapes hug the silhouette.
                    val dx = px - cx
                    val dy = py - cy
                    val len = sqrt(dx * dx + dy * dy)
                    if (len > 0.0001f) {
                        val k = r / len
                        px = cx + dx * k
                        py = cy + dy * k
                    }
                }
                if (i == start) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
        }
    }
}
