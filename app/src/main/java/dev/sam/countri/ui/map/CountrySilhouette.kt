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
import dev.sam.countri.data.map.WorldMapData
import dev.sam.countri.ui.theme.LocalCountriPalette
import dev.sam.countri.ui.theme.Springs
import kotlinx.coroutines.launch

data class CityMarker(val name: String, val lat: Float, val lon: Float)

/**
 * One country, drawn huge: the detail page hero. The silhouette lands with
 * a soft spring and sits on a faint halo of its continent's hue. Visited
 * cities appear as labeled dots inside the shape.
 */
@Composable
fun CountrySilhouette(
    data: WorldMapData,
    iso2: String,
    modifier: Modifier = Modifier,
    cityMarkers: List<CityMarker> = emptyList(),
) {
    val palette = LocalCountriPalette.current
    val country = CountryCatalog.byIso2[iso2] ?: return
    val hue = palette.continentColor(country.continent)
    val index = CountryCatalog.indexOf(iso2)

    // Geometry in lon/lat space, normalized across the dateline once.
    val geometry = remember(data, iso2) { buildGeometry(data, index) }

    val entrance = remember(iso2) { Animatable(0f) }
    LaunchedEffect(iso2) {
        launch { entrance.animateTo(1f, Springs.Gentle) }
    }

    val density = LocalDensity.current.density
    val fillPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    }
    val strokePaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
        }
    }
    val haloPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
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

        // Halo behind the shape.
        haloPaint.color = hue.toArgb()
        haloPaint.alpha = (26 * alpha).toInt()
        canvas.drawCircle(w / 2f, h / 2f, minOf(w, h) * 0.46f, haloPaint)

        fillPaint.color = hue.toArgb()
        fillPaint.alpha = ((if (palette.isDark) 116 else 92) * alpha).toInt()
        canvas.drawPath(drawPath, fillPaint)

        if (palette.isDark) {
            strokePaint.color = hue.toArgb()
            strokePaint.strokeWidth = 5f * density
            strokePaint.alpha = (46 * alpha).toInt()
            canvas.drawPath(drawPath, strokePaint)
        }
        strokePaint.color = hue.toArgb()
        strokePaint.strokeWidth = 1.6f * density
        strokePaint.alpha = (235 * alpha).toInt()
        canvas.drawPath(drawPath, strokePaint)

        // ---- visited cities as labeled pins ----
        if (cityMarkers.isNotEmpty()) {
            labelPaint.textSize = 10.5f * density
            labelPaint.color = palette.textPrimary.toArgb()
            labelPaint.alpha = (230 * alpha).toInt()
            cityMarkers.forEach { marker ->
                point[0] = marker.lon * geometry.lonScale
                point[1] = -marker.lat
                matrix.mapPoints(point)
                val px = point[0]
                val py = point[1]
                dotPaint.color = palette.canvas.toArgb()
                dotPaint.alpha = (255 * alpha).toInt()
                canvas.drawCircle(px, py, 4.6f * density, dotPaint)
                dotPaint.color = palette.textPrimary.toArgb()
                dotPaint.alpha = (255 * alpha).toInt()
                canvas.drawCircle(px, py, 3f * density, dotPaint)
                canvas.drawText(marker.name, px + 7f * density, py + 3.5f * density, labelPaint)
            }
        }
    }
}

private class CountryGeometry(val path: Path, val bounds: RectF, val lonScale: Float)

private fun buildGeometry(data: WorldMapData, index: Int): CountryGeometry? {
    if (index <= 0) return null
    // Dateline check: if the raw bbox spans most of the world, shift
    // negative longitudes east so Russia/Fiji/NZ don't smear across.
    var minLon = Float.MAX_VALUE
    var maxLon = -Float.MAX_VALUE
    var minLat = Float.MAX_VALUE
    var maxLat = -Float.MAX_VALUE
    for (r in 0 until data.ringCount) {
        if (data.ringCountry[r] != index) continue
        for (i in data.ringStart[r] until data.ringStart[r] + data.ringSize[r]) {
            if (data.lon[i] < minLon) minLon = data.lon[i]
            if (data.lon[i] > maxLon) maxLon = data.lon[i]
            if (data.lat[i] < minLat) minLat = data.lat[i]
            if (data.lat[i] > maxLat) maxLat = data.lat[i]
        }
    }
    if (minLon > maxLon) return null
    val wrap = maxLon - minLon > 180f

    // Equirectangular squashes east-west away from the equator; scale x by
    // cos(mid-latitude) so shapes keep their familiar proportions.
    val lonScale = kotlin.math.cos(
        Math.toRadians(((minLat + maxLat) / 2f).toDouble())
    ).toFloat().coerceAtLeast(0.2f)

    val path = Path().apply { fillType = Path.FillType.EVEN_ODD }
    for (r in 0 until data.ringCount) {
        if (data.ringCountry[r] != index) continue
        val start = data.ringStart[r]
        val size = data.ringSize[r]
        for (k in 0 until size) {
            val i = start + k
            var lon = data.lon[i]
            if (wrap && lon < 0f) lon += 360f
            val x = lon * lonScale
            val y = -data.lat[i]
            if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    }
    val bounds = RectF()
    @Suppress("DEPRECATION")
    path.computeBounds(bounds, true)
    if (bounds.width() <= 0f || bounds.height() <= 0f) return null
    return CountryGeometry(path, bounds, lonScale)
}
