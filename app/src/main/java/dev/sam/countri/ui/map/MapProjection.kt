package dev.sam.countri.ui.map

import dev.sam.countri.data.map.WorldMapData
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Where the flat map looks. zoom 1 covers the world (crop-to-fill); larger
 * zooms center on a country for the detail locator.
 */
data class MapViewport(
    val centerLon: Float,
    val centerLat: Float,
    val zoom: Float,
) {
    companion object {
        // Center of the rendered lat band (-56..83.5).
        val World = MapViewport(11f, 16f, 1f)
    }
}

/**
 * Pure projection math shared by the renderer, hit-testing, and tests.
 * Flat = equirectangular; globe = orthographic with a fixed aesthetic tilt;
 * morph linearly interpolates the two screen positions per vertex.
 */
object MapProjection {

    const val MIN_LAT = -56f
    const val MAX_LAT = 83.5f
    private const val LAT_SPAN = MAX_LAT - MIN_LAT // 139.5
    private const val TILT_DEG = 12f

    /** Degrees→pixels scale for the flat map (crop-to-fill the canvas). */
    fun flatScale(w: Float, h: Float, zoom: Float): Float =
        max(w / 360f, h / LAT_SPAN) * zoom

    fun globeRadius(w: Float, h: Float): Float = min(w, h) * 0.42f

    /**
     * Projects every vertex into [outX]/[outY]; [outDepth] receives the
     * orthographic z (relevant when morph > 0; 1 = facing the viewer,
     * negative = back hemisphere). Zero allocation.
     */
    fun projectAll(
        data: WorldMapData,
        viewport: MapViewport,
        w: Float,
        h: Float,
        rotationDeg: Float,
        morph: Float,
        outX: FloatArray,
        outY: FloatArray,
        outDepth: FloatArray,
    ) {
        val cx = w / 2f
        val cy = h / 2f
        val s = flatScale(w, h, viewport.zoom)
        val r = globeRadius(w, h)
        val phi = Math.toRadians(rotationDeg.toDouble())
        val cosP = cos(phi).toFloat()
        val sinP = sin(phi).toFloat()
        val tau = Math.toRadians(TILT_DEG.toDouble())
        val cosT = cos(tau).toFloat()
        val sinT = sin(tau).toFloat()
        val n = data.vertexCount
        val lon = data.lon
        val lat = data.lat
        val ux = data.ux
        val uy = data.uy
        val uz = data.uz
        val flatOnly = morph <= 0f

        for (i in 0 until n) {
            val fx = cx + (lon[i] - viewport.centerLon) * s
            val fy = cy - (lat[i] - viewport.centerLat) * s
            if (flatOnly) {
                outX[i] = fx
                outY[i] = fy
                outDepth[i] = 1f
            } else {
                val rx = ux[i] * cosP + uz[i] * sinP
                val rz = uz[i] * cosP - ux[i] * sinP
                val ry = uy[i] * cosT - rz * sinT
                val depth = rz * cosT + uy[i] * sinT
                val gx = cx + rx * r
                val gy = cy - ry * r
                outX[i] = fx + (gx - fx) * morph
                outY[i] = fy + (gy - fy) * morph
                outDepth[i] = depth
            }
        }
    }

    /** Inverse of the flat projection: screen point → (lon, lat), or null off-map. */
    fun inverseFlat(
        x: Float,
        y: Float,
        viewport: MapViewport,
        w: Float,
        h: Float,
    ): Pair<Float, Float>? {
        val s = flatScale(w, h, viewport.zoom)
        val lon = viewport.centerLon + (x - w / 2f) / s
        val lat = viewport.centerLat - (y - h / 2f) / s
        if (lon < -180f || lon > 180f || lat < -90f || lat > 90f) return null
        return lon to lat
    }

    /** Inverse orthographic on the front hemisphere, or null off-sphere. */
    fun inverseGlobe(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        rotationDeg: Float,
    ): Pair<Float, Float>? {
        val r = globeRadius(w, h)
        val px = (x - w / 2f) / r
        val py = -(y - h / 2f) / r
        val d2 = px * px + py * py
        if (d2 > 1f) return null
        val pz = sqrt(1f - d2)
        val tau = Math.toRadians(TILT_DEG.toDouble())
        val cosT = cos(tau).toFloat()
        val sinT = sin(tau).toFloat()
        // un-tilt (inverse rotation about the x-axis)
        val ry = py * cosT + pz * sinT
        val rz = -py * sinT + pz * cosT
        val phi = Math.toRadians(rotationDeg.toDouble())
        val cosP = cos(phi).toFloat()
        val sinP = sin(phi).toFloat()
        // un-rotate (inverse rotation about the y-axis)
        val ux = px * cosP - rz * sinP
        val uz = px * sinP + rz * cosP
        val lat = Math.toDegrees(asin(ry.coerceIn(-1f, 1f).toDouble())).toFloat()
        val lon = Math.toDegrees(atan2(ux.toDouble(), uz.toDouble())).toFloat()
        return lon to lat
    }
}
