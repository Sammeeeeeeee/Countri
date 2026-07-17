package dev.sam.countri.data.map

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.tan

/**
 * Parsed contents of assets/worldmap.bin (see tools/generate_map.py for the
 * format). Vertices are flattened into parallel arrays; rings index into them
 * via [ringStart]/[ringSize]. Unit-sphere vectors are precomputed here so the
 * globe projection is a pure rotate-and-scale per frame.
 */
class WorldMapData(
    val ringCount: Int,
    /** Per ring: 0 = land without a catalog country, else 1-based catalog index. */
    val ringCountry: IntArray,
    val ringStart: IntArray,
    val ringSize: IntArray,
    val lon: FloatArray,
    val lat: FloatArray,
) {
    val vertexCount = lon.size

    // Unit sphere vectors for the orthographic projection.
    val ux = FloatArray(vertexCount)
    val uy = FloatArray(vertexCount)
    val uz = FloatArray(vertexCount)

    // Mercator y per vertex (pseudo-degrees), so the flat projection keeps
    // country shapes true without per-frame trig.
    val mercY = FloatArray(vertexCount)

    // Per-ring bounding boxes for hit-test candidate filtering.
    private val bboxMinLon = FloatArray(ringCount)
    private val bboxMaxLon = FloatArray(ringCount)
    private val bboxMinLat = FloatArray(ringCount)
    private val bboxMaxLat = FloatArray(ringCount)

    init {
        for (i in 0 until vertexCount) {
            val la = Math.toRadians(lat[i].toDouble().coerceIn(-89.0, 89.0))
            val lo = Math.toRadians(lon[i].toDouble())
            ux[i] = (cos(la) * sin(lo)).toFloat()
            uy[i] = sin(la).toFloat()
            uz[i] = (cos(la) * cos(lo)).toFloat()
            mercY[i] = Math.toDegrees(ln(tan(PI / 4.0 + la / 2.0))).toFloat()
        }
        for (r in 0 until ringCount) {
            var minLon = Float.MAX_VALUE; var maxLon = -Float.MAX_VALUE
            var minLat = Float.MAX_VALUE; var maxLat = -Float.MAX_VALUE
            for (i in ringStart[r] until ringStart[r] + ringSize[r]) {
                if (lon[i] < minLon) minLon = lon[i]
                if (lon[i] > maxLon) maxLon = lon[i]
                if (lat[i] < minLat) minLat = lat[i]
                if (lat[i] > maxLat) maxLat = lat[i]
            }
            bboxMinLon[r] = minLon; bboxMaxLon[r] = maxLon
            bboxMinLat[r] = minLat; bboxMaxLat[r] = maxLat
        }
    }

    /**
     * Catalog index (1-based) of the country containing the point, or 0.
     * Even-odd across each country's rings, so holes (Lesotho in South
     * Africa) resolve to the inner country.
     */
    fun countryAt(lonQ: Float, latQ: Float): Int {
        val counts = HashMap<Int, Int>(4)
        val smallestRing = HashMap<Int, Float>(4)
        for (r in 0 until ringCount) {
            if (lonQ < bboxMinLon[r] || lonQ > bboxMaxLon[r] ||
                latQ < bboxMinLat[r] || latQ > bboxMaxLat[r]
            ) continue
            if (ringContains(r, lonQ, latQ)) {
                val c = ringCountry[r]
                if (c != 0) {
                    counts[c] = (counts[c] ?: 0) + 1
                    val area = (bboxMaxLon[r] - bboxMinLon[r]) * (bboxMaxLat[r] - bboxMinLat[r])
                    val prev = smallestRing[c]
                    if (prev == null || area < prev) smallestRing[c] = area
                }
            }
        }
        // Odd parity = inside (holes cancel). When several countries claim the
        // point (a microstate chip over a coarse neighbor), the smallest
        // claiming ring wins.
        var best = 0
        var bestArea = Float.MAX_VALUE
        for ((country, n) in counts) {
            if (n % 2 == 1) {
                val area = smallestRing[country] ?: Float.MAX_VALUE
                if (area < bestArea) {
                    bestArea = area
                    best = country
                }
            }
        }
        return best
    }

    /**
     * Like [countryAt], but coastal-tap friendly: if the point is in the sea,
     * falls back to the country of the nearest coastline vertex within
     * [maxDeg] degrees (110m coastlines cut fine coastal detail, so exact
     * point-in-polygon misses cities like New York).
     */
    fun countryNear(lonQ: Float, latQ: Float, maxDeg: Float): Int {
        val exact = countryAt(lonQ, latQ)
        if (exact != 0) return exact
        var best = 0
        var bestD2 = maxDeg * maxDeg
        for (r in 0 until ringCount) {
            val c = ringCountry[r]
            if (c == 0) continue
            if (lonQ < bboxMinLon[r] - maxDeg || lonQ > bboxMaxLon[r] + maxDeg ||
                latQ < bboxMinLat[r] - maxDeg || latQ > bboxMaxLat[r] + maxDeg
            ) continue
            for (i in ringStart[r] until ringStart[r] + ringSize[r]) {
                val dLon = lon[i] - lonQ
                val dLat = lat[i] - latQ
                val d2 = dLon * dLon + dLat * dLat
                if (d2 < bestD2) {
                    bestD2 = d2
                    best = c
                }
            }
        }
        return best
    }

    private fun ringContains(r: Int, lonQ: Float, latQ: Float): Boolean {
        val start = ringStart[r]
        val n = ringSize[r]
        var inside = false
        var j = n - 1
        for (i in 0 until n) {
            val yi = lat[start + i]; val yj = lat[start + j]
            val xi = lon[start + i]; val xj = lon[start + j]
            if ((yi > latQ) != (yj > latQ) &&
                lonQ < (xj - xi) * (latQ - yi) / (yj - yi) + xi
            ) inside = !inside
            j = i
        }
        return inside
    }
}

object WorldMapAsset {

    fun parse(bytes: ByteArray): WorldMapData {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(buf.get() == 'C'.code.toByte() && buf.get() == 'M'.code.toByte() &&
                buf.get() == 'A'.code.toByte() && buf.get() == 'P'.code.toByte()) {
            "worldmap.bin: bad magic"
        }
        val version = buf.get().toInt()
        require(version == 1) { "worldmap.bin: unsupported version $version" }
        val ringCount = buf.short.toInt() and 0xFFFF

        val ringCountry = IntArray(ringCount)
        val ringStart = IntArray(ringCount)
        val ringSize = IntArray(ringCount)

        // First pass over the buffer to size the vertex arrays.
        val mark = buf.position()
        var total = 0
        for (r in 0 until ringCount) {
            buf.get() // country
            val n = buf.short.toInt() and 0xFFFF
            total += n
            buf.position(buf.position() + n * 4)
        }
        buf.position(mark)

        val lon = FloatArray(total)
        val lat = FloatArray(total)
        var v = 0
        for (r in 0 until ringCount) {
            ringCountry[r] = buf.get().toInt() and 0xFF
            val n = buf.short.toInt() and 0xFFFF
            ringStart[r] = v
            ringSize[r] = n
            for (i in 0 until n) {
                lon[v] = buf.short / 100f
                lat[v] = buf.short / 100f
                v++
            }
        }
        return WorldMapData(ringCount, ringCountry, ringStart, ringSize, lon, lat)
    }
}
