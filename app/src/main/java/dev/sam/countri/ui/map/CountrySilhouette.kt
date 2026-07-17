package dev.sam.countri.ui.map

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import dev.sam.countri.data.catalog.CountryCatalog
import dev.sam.countri.data.map.DetailMapData
import dev.sam.countri.data.map.WorldMapData
import dev.sam.countri.ui.theme.LocalCountriPalette
import dev.sam.countri.ui.theme.Springs
import kotlinx.coroutines.launch

data class CityMarker(val name: String, val lat: Float, val lon: Float)

/**
 * One country, drawn huge: the detail page hero. Geometry comes from the
 * high-resolution 10m asset when available — real coastlines, fjords and
 * all — filled flat and clean. Visited cities appear as labeled dots
 * inside the shape.
 */
@Composable
fun CountrySilhouette(
    data: WorldMapData,
    iso2: String,
    modifier: Modifier = Modifier,
    cityMarkers: List<CityMarker> = emptyList(),
    detail: DetailMapData? = null,
) {
    val palette = LocalCountriPalette.current
    val country = CountryCatalog.byIso2[iso2] ?: return
    val hue = palette.continentColor(country.continent)
    val index = CountryCatalog.indexOf(iso2)

    // Geometry in lon/lat space, normalized across the dateline once.
    val geometry = remember(data, detail, iso2) { buildGeometry(data, detail, iso2, index) }

    val entrance = remember(iso2) { Animatable(0f) }
    LaunchedEffect(iso2) {
        launch { entrance.animateTo(1f, Springs.Gentle) }
    }

    val density = LocalDensity.current.density
    val fillPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    }
    val dotPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
    val labelPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
    val drawPath = remember { Path() }
    val matrix = remember { Matrix() }
    val point = remember { FloatArray(2) }

    Canvas(modifier) {
        if (geometry == null) return@Canvas
        val canvas = drawContext.canvas.nativeCanvas
        val w = size.width
        val h = size.height
        val pad = 34f * density

        // Fit the country's bounds into the canvas, centered, with entrance
        // scale riding the spring.
        val bounds = geometry.bounds
        val scale = minOf(
            (w - pad * 2f) / bounds.width(),
            (h - pad * 2f) / bounds.height(),
        ) * (0.86f + 0.14f * entrance.value)
        matrix.reset()
        matrix.postTranslate(-bounds.centerX(), -bounds.centerY())
        matrix.postScale(scale, scale)
        matrix.postTranslate(w / 2f, h / 2f)
        geometry.path.transform(matrix, drawPath)

        val alpha = entrance.value

        // Solid ink shape on clean canvas — flat, precise, no noise.
        fillPaint.color = hue.toArgb()
        fillPaint.alpha = (243 * alpha).toInt()
        canvas.drawPath(drawPath, fillPaint)

        // ---- visited cities as pins: canvas-colored holes in the ink ----
        if (cityMarkers.isNotEmpty()) {
            labelPaint.textSize = 10.5f * density
            labelPaint.color = palette.textFaint.toArgb()
            labelPaint.alpha = (255 * alpha).toInt()
            dotPaint.color = palette.canvas.toArgb()
            dotPaint.alpha = (255 * alpha).toInt()
            // Labels place themselves greedily — right of the dot first,
            // then left, below, above — and drop out rather than overlap.
            val placed = ArrayList<RectF>(cityMarkers.size)
            val projected = cityMarkers.map { marker ->
                point[0] = geometry.unwrap(marker.lon) * geometry.lonScale
                point[1] = -marker.lat
                matrix.mapPoints(point)
                Triple(marker.name, point[0], point[1])
            }.sortedBy { it.third }
            projected.forEach { (name, px, py) ->
                canvas.drawCircle(px, py, 3.4f * density, dotPaint)
                val tw = labelPaint.measureText(name)
                val th = 11f * density
                val candidates = arrayOf(
                    floatArrayOf(px + 6.5f * density, py + 3.5f * density),
                    floatArrayOf(px - 6.5f * density - tw, py + 3.5f * density),
                    floatArrayOf(px - tw / 2f, py + 15f * density),
                    floatArrayOf(px - tw / 2f, py - 8f * density),
                )
                val spot = candidates.firstOrNull { (x, base) ->
                    val r = RectF(x - 2f, base - th, x + tw + 2f, base + 3f * density)
                    r.left >= 0f && r.right <= w && r.top >= 0f && r.bottom <= h &&
                        placed.none { RectF.intersects(it, r) }
                }
                if (spot != null) {
                    placed.add(
                        RectF(
                            spot[0] - 2f, spot[1] - th,
                            spot[0] + tw + 2f, spot[1] + 3f * density,
                        )
                    )
                    canvas.drawText(name, spot[0], spot[1], labelPaint)
                }
            }
        }
    }
}

private class CountryGeometry(
    val path: Path,
    val bounds: RectF,
    val lonScale: Float,
    private val wrap: Boolean,
) {
    fun unwrap(lon: Float): Float = if (wrap && lon < 0f) lon + 360f else lon
}

private class RingBox(
    val points: FloatArray, // flat lon/lat pairs
    val minLon: Float,
    val maxLon: Float,
    val minLat: Float,
    val maxLat: Float,
) {
    val span: Float get() = maxOf(maxLon - minLon, maxLat - minLat)
}

/** This country's raw rings: the 10m asset when it knows the country, else the world map. */
private fun countryRings(
    data: WorldMapData,
    detail: DetailMapData?,
    iso2: String,
    index: Int,
): List<FloatArray> {
    detail?.ringsFor(iso2)?.takeIf { it.isNotEmpty() }?.let { return it }
    if (index <= 0) return emptyList()
    val out = ArrayList<FloatArray>()
    for (r in 0 until data.ringCount) {
        if (data.ringCountry[r] != index) continue
        val start = data.ringStart[r]
        val n = data.ringSize[r]
        val ring = FloatArray(n * 2)
        for (i in 0 until n) {
            ring[i * 2] = data.lon[start + i]
            ring[i * 2 + 1] = data.lat[start + i]
        }
        out.add(ring)
    }
    return out
}

private fun buildGeometry(
    data: WorldMapData,
    detail: DetailMapData?,
    iso2: String,
    index: Int,
): CountryGeometry? {
    val raw = countryRings(data, detail, iso2, index)
    if (raw.isEmpty()) return null

    var globalMinLon = Float.MAX_VALUE
    var globalMaxLon = -Float.MAX_VALUE
    val rings = ArrayList<RingBox>(raw.size)
    for (ring in raw) {
        var minLon = Float.MAX_VALUE; var maxLon = -Float.MAX_VALUE
        var minLat = Float.MAX_VALUE; var maxLat = -Float.MAX_VALUE
        var i = 0
        while (i < ring.size) {
            val lon = ring[i]; val lat = ring[i + 1]
            if (lon < minLon) minLon = lon
            if (lon > maxLon) maxLon = lon
            if (lat < minLat) minLat = lat
            if (lat > maxLat) maxLat = lat
            i += 2
        }
        if (minLon > maxLon) continue
        rings.add(
            RingBox(
                points = ring,
                minLon = minLon,
                maxLon = maxLon,
                minLat = minLat,
                maxLat = maxLat,
            )
        )
        if (minLon < globalMinLon) globalMinLon = minLon
        if (maxLon > globalMaxLon) globalMaxLon = maxLon
    }
    if (rings.isEmpty()) return null
    val wrap = globalMaxLon - globalMinLon > 180f
    fun unwrap(lon: Float) = if (wrap && lon < 0f) lon + 360f else lon

    // The hero shows the recognizable country: the biggest landmass, any
    // second landmass in its size class (East Malaysia, Alaska), and the
    // islands that hug the kept coast. Distant specks — Svalbard, the
    // Canaries, Galápagos — stay off the plate instead of stretching it.
    val main = rings.maxBy { it.span }
    val keptMask = BooleanArray(rings.size)
    rings.forEachIndexed { i, r ->
        if (r === main || r.span >= main.span * 0.45f) keptMask[i] = true
    }

    fun gap2(a: RingBox, b: RingBox): Float {
        val dx = maxOf(
            0f,
            unwrap(a.minLon) - unwrap(b.maxLon),
            unwrap(b.minLon) - unwrap(a.maxLon),
        )
        val dy = maxOf(0f, a.minLat - b.maxLat, b.minLat - a.maxLat)
        return dx * dx + dy * dy
    }

    // Chain outward: anything within 2 degrees of the kept coast joins.
    var grew = true
    while (grew) {
        grew = false
        for (i in rings.indices) {
            if (keptMask[i]) continue
            for (j in rings.indices) {
                if (!keptMask[j]) continue
                if (gap2(rings[i], rings[j]) <= 4f) {
                    keptMask[i] = true
                    grew = true
                    break
                }
            }
        }
    }
    val kept = rings.filterIndexed { i, _ -> keptMask[i] }

    var minLat = Float.MAX_VALUE
    var maxLat = -Float.MAX_VALUE
    kept.forEach { info ->
        var i = 1
        while (i < info.points.size) {
            val lat = info.points[i]
            if (lat < minLat) minLat = lat
            if (lat > maxLat) maxLat = lat
            i += 2
        }
    }

    // Equirectangular squashes east-west away from the equator; scale x by
    // cos(mid-latitude) so shapes keep their familiar proportions.
    val lonScale = kotlin.math.cos(
        Math.toRadians(((minLat + maxLat) / 2f).toDouble())
    ).toFloat().coerceAtLeast(0.2f)

    val path = Path().apply { fillType = Path.FillType.EVEN_ODD }
    kept.forEach { info ->
        var i = 0
        while (i < info.points.size) {
            val x = unwrap(info.points[i]) * lonScale
            val y = -info.points[i + 1]
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            i += 2
        }
        path.close()
    }
    val bounds = RectF()
    @Suppress("DEPRECATION")
    path.computeBounds(bounds, true)
    if (bounds.width() <= 0f || bounds.height() <= 0f) return null
    return CountryGeometry(path, bounds, lonScale, wrap)
}
