package dev.sam.countri.data.map

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Parsed index over assets/detailmap.bin (see tools/generate_detail.py):
 * high-resolution Natural Earth 10m outlines, one country at a time. Only
 * the directory is walked up front; ring data is decoded on demand, so the
 * 700KB asset never sits in memory twice.
 */
class DetailMapData(private val bytes: ByteArray) {

    /** iso2 → byte offset of that country's bbox block. */
    private val offsets = HashMap<String, Int>(256)

    init {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(
            buf.get() == 'C'.code.toByte() && buf.get() == 'D'.code.toByte() &&
                buf.get() == 'T'.code.toByte() && buf.get() == 'L'.code.toByte()
        ) { "detailmap.bin: bad magic" }
        val version = buf.get().toInt()
        require(version == 1) { "detailmap.bin: unsupported version $version" }
        val count = buf.short.toInt() and 0xFFFF
        repeat(count) {
            val iso2 = "${buf.get().toInt().toChar()}${buf.get().toInt().toChar()}"
            offsets[iso2] = buf.position()
            buf.position(buf.position() + 16) // bbox
            val rings = buf.short.toInt() and 0xFFFF
            repeat(rings) {
                val n = buf.short.toInt() and 0xFFFF
                buf.position(buf.position() + n * 4)
            }
        }
    }

    /**
     * The country's rings as flat [lon0, lat0, lon1, lat1, …] arrays,
     * or null when the asset doesn't know the country.
     */
    fun ringsFor(iso2: String): List<FloatArray>? {
        val at = offsets[iso2] ?: return null
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        buf.position(at)
        val minLon = buf.float
        val minLat = buf.float
        val maxLon = buf.float
        val maxLat = buf.float
        val sx = (maxLon - minLon) / 65535f
        val sy = (maxLat - minLat) / 65535f
        val ringCount = buf.short.toInt() and 0xFFFF
        return List(ringCount) {
            val n = buf.short.toInt() and 0xFFFF
            val out = FloatArray(n * 2)
            for (i in 0 until n) {
                out[i * 2] = minLon + (buf.short.toInt() and 0xFFFF) * sx
                out[i * 2 + 1] = minLat + (buf.short.toInt() and 0xFFFF) * sy
            }
            out
        }
    }
}

object DetailMapAsset {
    fun parse(bytes: ByteArray): DetailMapData = DetailMapData(bytes)
}
