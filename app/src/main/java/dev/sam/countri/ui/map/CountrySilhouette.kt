package dev.sam.countri.ui.map

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import dev.sam.countri.data.catalog.CountryCatalog
import dev.sam.countri.data.map.DetailMapData
import dev.sam.countri.data.map.WorldMapData
import dev.sam.countri.ui.theme.LocalCountriPalette
import dev.sam.countri.ui.theme.Springs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CityMarker(val name: String, val lat: Float, val lon: Float)

/**
 * One country, drawn huge: the detail page hero. Geometry comes from the
 * high-resolution 10m asset when available (real coastlines, fjords and
 * all), and the fill is a quiet topography — distance-from-coast bands
 * that lighten toward the interior, like a well-set atlas plate. Visited
 * cities appear as labeled dots inside the shape.
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

    // The contour bands render off the main thread; until they land the
    // shape draws solid, so first paint is never blocked.
    var bands by remember(iso2) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(geometry, hue) {
        bands = geometry?.let { g ->
            withContext(Dispatchers.Default) { buildBands(g, hue.toArgb()) }
        }
    }

    val entrance = remember(iso2) { Animatable(0f) }
    LaunchedEffect(iso2) {
        launch { entrance.animateTo(1f, Springs.Gentle) }
    }

    val density = LocalDensity.current.density
    val fillPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    }
    val bandPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) }
    val dotPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
    val labelPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
    val drawPath = remember { Path() }
    val matrix = remember { Matrix() }
    val mapped = remember { RectF() }
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
        val topo = bands

        if (topo == null) {
            // Solid ink shape while the topography is still computing.
            fillPaint.color = hue.toArgb()
            fillPaint.alpha = (243 * alpha).toInt()
            canvas.drawPath(drawPath, fillPaint)
        } else {
            // Bands clipped to the crisp vector outline — the bitmap only
            // supplies tone, never the edge.
            matrix.mapRect(mapped, bounds)
            val save = canvas.save()
            canvas.clipPath(drawPath)
            bandPaint.alpha = (255 * alpha).toInt()
            canvas.drawBitmap(topo, null, mapped, bandPaint)
            canvas.restoreToCount(save)
        }

        // ---- visited cities as pins: canvas-colored holes in the ink ----
        if (cityMarkers.isNotEmpty()) {
            labelPaint.textSize = 10.5f * density
            labelPaint.color = palette.textFaint.toArgb()
            labelPaint.alpha = (255 * alpha).toInt()
            cityMarkers.forEach { marker ->
                point[0] = geometry.unwrap(marker.lon) * geometry.lonScale
                point[1] = -marker.lat
                matrix.mapPoints(point)
                val px = point[0]
                val py = point[1]
                dotPaint.color = palette.canvas.toArgb()
                dotPaint.alpha = (255 * alpha).toInt()
                canvas.drawCircle(px, py, 3.4f * density, dotPaint)
                canvas.drawText(marker.name, px + 6.5f * density, py + 3.5f * density, labelPaint)
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
    val centerLon: Float,
    val centerLat: Float,
    val span: Float,
)

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
                centerLon = (minLon + maxLon) / 2f,
                centerLat = (minLat + maxLat) / 2f,
                span = maxOf(maxLon - minLon, maxLat - minLat),
            )
        )
        if (minLon < globalMinLon) globalMinLon = minLon
        if (maxLon > globalMaxLon) globalMaxLon = maxLon
    }
    if (rings.isEmpty()) return null
    val wrap = globalMaxLon - globalMinLon > 180f
    fun unwrap(lon: Float) = if (wrap && lon < 0f) lon + 360f else lon

    // The hero shows the recognizable mainland: keep the biggest landmass
    // and anything near it (Corsica yes, French Guiana no).
    val main = rings.maxBy { it.span }
    val reach = maxOf(main.span * 1.6f, 14f)
    val kept = rings.filter { info ->
        val dLon = unwrap(info.centerLon) - unwrap(main.centerLon)
        val dLat = info.centerLat - main.centerLat
        dLon * dLon + dLat * dLat <= reach * reach
    }

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

/**
 * Distance-from-coast bands: rasterize the shape, chamfer-transform the
 * inside, and quantize into a few tones that lighten toward the interior.
 * Pure pixels — the vector clip in the draw pass keeps the edge exact.
 */
private fun buildBands(g: CountryGeometry, ink: Int): Bitmap? {
    val maxDim = 420
    val aspect = g.bounds.height() / g.bounds.width()
    var gw: Int
    var gh: Int
    if (aspect >= 1f) {
        gh = maxDim
        gw = (maxDim / aspect).toInt()
    } else {
        gw = maxDim
        gh = (maxDim * aspect).toInt()
    }
    if (gw < 8 || gh < 8) return null

    // Two-pixel apron so the border is genuinely "sea" for the transform.
    val apron = 2
    val tw = gw + apron * 2
    val th = gh + apron * 2

    val mask = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(mask)
    val m = Matrix()
    m.postTranslate(-g.bounds.left, -g.bounds.top)
    m.postScale(gw / g.bounds.width(), gh / g.bounds.height())
    m.postTranslate(apron.toFloat(), apron.toFloat())
    val p = Path()
    g.path.transform(m, p)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
    canvas.drawPath(p, paint)

    val px = IntArray(tw * th)
    mask.getPixels(px, 0, tw, 0, 0, tw, th)
    mask.recycle()

    val far = 1e9f
    val dist = FloatArray(tw * th) { if ((px[it] ushr 24) > 127) far else 0f }

    // 3-4 chamfer, forward then backward.
    for (y in 0 until th) {
        for (x in 0 until tw) {
            val i = y * tw + x
            if (dist[i] == 0f) continue
            var d = dist[i]
            if (x > 0) d = minOf(d, dist[i - 1] + 3f)
            if (y > 0) {
                d = minOf(d, dist[i - tw] + 3f)
                if (x > 0) d = minOf(d, dist[i - tw - 1] + 4f)
                if (x < tw - 1) d = minOf(d, dist[i - tw + 1] + 4f)
            }
            dist[i] = d
        }
    }
    var maxD = 0f
    for (y in th - 1 downTo 0) {
        for (x in tw - 1 downTo 0) {
            val i = y * tw + x
            if (dist[i] == 0f) continue
            var d = dist[i]
            if (x < tw - 1) d = minOf(d, dist[i + 1] + 3f)
            if (y < th - 1) {
                d = minOf(d, dist[i + tw] + 3f)
                if (x < tw - 1) d = minOf(d, dist[i + tw + 1] + 4f)
                if (x > 0) d = minOf(d, dist[i + tw - 1] + 4f)
            }
            dist[i] = d
            if (d > maxD) maxD = d
        }
    }

    // A shape too small for terraces reads better as one solid tone.
    if (maxD < 27f) return null

    val alphas = intArrayOf(246, 225, 204, 184)
    val cut1 = maxD * 0.15f
    val cut2 = maxD * 0.36f
    val cut3 = maxD * 0.62f
    val inkRgb = ink and 0x00FFFFFF
    val out = IntArray(gw * gh)
    for (y in 0 until gh) {
        for (x in 0 until gw) {
            val d = dist[(y + apron) * tw + (x + apron)]
            out[y * gw + x] = if (d <= 0f) 0 else {
                val band = when {
                    d < cut1 -> 0
                    d < cut2 -> 1
                    d < cut3 -> 2
                    else -> 3
                }
                (alphas[band] shl 24) or inkRgb
            }
        }
    }
    return Bitmap.createBitmap(out, gw, gh, Bitmap.Config.ARGB_8888)
}
