package dev.sam.countri.ui.map

import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import dev.sam.countri.data.catalog.CountryCatalog
import dev.sam.countri.data.map.WorldMapData
import dev.sam.countri.domain.CountryStatus
import dev.sam.countri.ui.theme.CountriPalette
import kotlin.math.sqrt

/** Palette snapshot as ARGB ints, ready for android.graphics.Paint. */
class MapColors(palette: CountriPalette) {
    val isDark = palette.isDark
    val land = palette.mapLand.toArgb()
    val border = palette.mapBorder.toArgb()
    val wishlist = palette.wishlist.toArgb()
    val sphereCenter = palette.globeShade.toArgb()
    val sphereEdge = palette.canvas.copy(alpha = 0f).toArgb()
    val label = palette.textPrimary.toArgb()
    val labelHalo = palette.canvas.toArgb()
    /** Continent hues, ordinal-indexed — visited countries wear these. */
    val continents = IntArray(palette.continents.size) { palette.continents[it].toArgb() }
}

/**
 * Draws the whole world as filled country silhouettes on a Compose Canvas.
 * Visited countries fill with their continent's hue; wishlist countries are
 * dashed outlines — uncolored until the trip happens. Zero steady-state
 * allocation per frame.
 */
class WorldMapRenderer(private val data: WorldMapData) {

    private val n = data.vertexCount
    private val x = FloatArray(n)
    private val y = FloatArray(n)
    private val depth = FloatArray(n)

    private val countryCount = 196

    /** Catalog index (1-based) → continent ordinal; -1 for untagged land. */
    private val continentOf = IntArray(countryCount) { idx ->
        if (idx == 0) -1 else CountryCatalog.all[idx - 1].continent.ordinal
    }

    // Reused Path per catalog index (0 = untagged land). Even-odd fill keeps
    // enclave holes (Lesotho) open.
    private val paths = Array(countryCount) { Path().apply { fillType = Path.FillType.EVEN_ODD } }
    private val pathUsed = BooleanArray(countryCount)
    private val depthSum = FloatArray(countryCount)
    private val depthCount = IntArray(countryCount)

    /** Vertex mass per country — a cheap area proxy for label priority. */
    private val countryVerts = IntArray(countryCount).also { arr ->
        for (r in 0 until data.ringCount) arr[data.ringCountry[r]] += data.ringSize[r]
    }

    /** Big countries first: when labels compete for space, they win. */
    private val labelOrder: IntArray = (1 until countryCount)
        .sortedByDescending { countryVerts[it] }
        .toIntArray()
    private val labelRects = ArrayList<android.graphics.RectF>(64)

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }
    private val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }
    private var dashScale = 0f
    private val spherePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val labelHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }
    private var sphereShaderKey = 0f

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

        // ---- sphere disk + aurora wash under the globe ----
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

        // ---- land fills ----
        for (c in 0 until countryCount) {
            if (!pathUsed[c]) continue
            val alpha = countryAlpha(c, morph)
            if (alpha < 0.02f) continue
            val status = statuses[c]
            when (status) {
                CountryStatus.VISITED -> {
                    fillPaint.color = continentHue(colors, c)
                    fillPaint.alpha = (alpha * if (colors.isDark) 96 else 72).toInt()
                }
                CountryStatus.WISHLIST -> {
                    fillPaint.color = colors.wishlist
                    fillPaint.alpha = (alpha * 14).toInt()
                }
                null -> {
                    fillPaint.color = colors.land
                    fillPaint.alpha = (alpha * 255).toInt()
                }
            }
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

        // ---- graticule: faint atlas grid, appearing as the map closes in ----
        if (morph <= 0.01f) {
            val gFade = ((viewport.zoom - 1.5f) / 1.2f).coerceIn(0f, 1f)
            if (gFade > 0f) {
                val s = MapProjection.flatScale(w, h, viewport.zoom)
                gridPaint.color = colors.label
                gridPaint.alpha = (gFade * 15).toInt()
                gridPaint.strokeWidth = 1f
                var lon = -180
                while (lon <= 180) {
                    val gx = w / 2f + (lon - viewport.centerLon) * s
                    if (gx > -1f && gx < w + 1f) canvas.drawLine(gx, 0f, gx, h, gridPaint)
                    lon += 15
                }
                var lat = -60
                while (lat <= 75) {
                    val gy = h / 2f - (lat - viewport.centerLat) * s
                    if (gy > -1f && gy < h + 1f) canvas.drawLine(0f, gy, w, gy, gridPaint)
                    lat += 15
                }
            }
        }

        // ---- tracked-country chrome ----
        if (dashScale != density) {
            dashScale = density
            dashPaint.pathEffect = DashPathEffect(
                floatArrayOf(4.5f * density, 3.5f * density), 0f,
            )
        }
        for ((c, status) in statuses) {
            if (c !in 1 until countryCount || !pathUsed[c]) continue
            val alpha = countryAlpha(c, morph)
            if (alpha < 0.02f) continue
            if (status == CountryStatus.VISITED) {
                val hue = continentHue(colors, c)
                if (colors.isDark) {
                    strokePaint.color = hue
                    strokePaint.strokeWidth = 4.5f * density
                    strokePaint.alpha = (alpha * 40).toInt()
                    canvas.drawPath(paths[c], strokePaint)
                }
                strokePaint.color = hue
                strokePaint.strokeWidth = 1.2f * density
                strokePaint.alpha = (alpha * 225).toInt()
                canvas.drawPath(paths[c], strokePaint)
            } else {
                dashPaint.color = colors.wishlist
                dashPaint.strokeWidth = 1.3f * density
                dashPaint.alpha = (alpha * 165).toInt()
                canvas.drawPath(paths[c], dashPaint)
            }
        }

        // ---- selection emphasis (detail locator) ----
        if (selectedIndex in 1 until countryCount && pathUsed[selectedIndex]) {
            val hue = continentHue(colors, selectedIndex)
            fillPaint.color = hue
            fillPaint.alpha = if (colors.isDark) 110 else 84
            canvas.drawPath(paths[selectedIndex], fillPaint)
            strokePaint.color = hue
            strokePaint.strokeWidth = 1.6f * density
            strokePaint.alpha = 255
            canvas.drawPath(paths[selectedIndex], strokePaint)
        }

        // ---- just-added pulse ----
        if (pulseIndex in 1 until countryCount && pathUsed[pulseIndex] && pulseProgress > 0f) {
            strokePaint.color = continentHue(colors, pulseIndex)
            strokePaint.strokeWidth = (1.5f + 9f * pulseProgress) * density
            strokePaint.alpha = ((1f - pulseProgress) * 170).toInt()
            canvas.drawPath(paths[pulseIndex], strokePaint)
        }

        // ---- country names fade in as the flat map zooms ----
        if (morph <= 0.01f && viewport.zoom >= LABEL_MIN_ZOOM) {
            val fade = ((viewport.zoom - LABEL_MIN_ZOOM) / 0.8f).coerceIn(0f, 1f)
            val s = MapProjection.flatScale(w, h, viewport.zoom)
            // Type grows a touch as the map closes in.
            val ts = (10.5f + 1.8f * ((viewport.zoom - LABEL_MIN_ZOOM) / 3f).coerceIn(0f, 1f)) * density
            labelPaint.textSize = ts
            labelHaloPaint.textSize = ts
            labelHaloPaint.strokeWidth = 3f * density
            labelPaint.color = colors.label
            labelHaloPaint.color = colors.labelHalo
            // Visited countries claim their spots first, then the rest fill
            // in by size — and nothing ever prints over anything.
            labelRects.clear()
            for (pass in 0..1) {
                for (idx in labelOrder) {
                    val visited = statuses[idx] == CountryStatus.VISITED
                    if ((pass == 0) != visited) continue
                    val country = CountryCatalog.all[idx - 1]
                    val lx = w / 2f + (country.lon - viewport.centerLon) * s
                    val ly = h / 2f - (country.lat - viewport.centerLat) * s
                    if (lx < -60f || lx > w + 60f || ly < -20f || ly > h + 20f) continue
                    val tw = labelPaint.measureText(country.name)
                    val rect = android.graphics.RectF(
                        lx - tw / 2f - 4f, ly - ts, lx + tw / 2f + 4f, ly + 4f,
                    )
                    var clash = false
                    for (r in labelRects.indices) {
                        if (android.graphics.RectF.intersects(labelRects[r], rect)) {
                            clash = true
                            break
                        }
                    }
                    if (clash) continue
                    labelRects.add(rect)
                    labelPaint.alpha = (fade * if (visited) 240 else 195).toInt()
                    labelHaloPaint.alpha = (fade * 200).toInt()
                    canvas.drawText(country.name, lx, ly, labelHaloPaint)
                    canvas.drawText(country.name, lx, ly, labelPaint)
                }
            }
        }
    }

    private companion object {
        const val LABEL_MIN_ZOOM = 2.3f
    }

    private fun continentHue(colors: MapColors, c: Int): Int {
        val ord = continentOf[c]
        return if (ord >= 0) colors.continents[ord] else colors.land
    }

    private fun countryAlpha(c: Int, morph: Float): Float {
        if (morph <= 0.01f) return 1f
        val count = depthCount[c]
        if (count == 0) return 1f
        val avg = depthSum[c] / count
        // smoothstep(-0.05, 0.35, avg): fade countries out well before they
        // roll behind, so limb-clamped slivers never read as scratches.
        val t = ((avg + 0.05f) / 0.40f).coerceIn(0f, 1f)
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
