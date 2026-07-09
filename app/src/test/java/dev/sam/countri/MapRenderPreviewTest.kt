package dev.sam.countri

import dev.sam.countri.data.map.WorldMapAsset
import dev.sam.countri.data.map.WorldMapData
import dev.sam.countri.ui.map.MapProjection
import dev.sam.countri.ui.map.MapViewport
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.CRC32
import java.util.zip.Deflater
import kotlin.math.sqrt

/**
 * Renders the map through the exact production projection into PNGs under
 * build/map-previews so the geometry can be eyeballed without a device.
 * Uses a tiny scanline rasterizer + PNG encoder because Android unit tests
 * have no AWT. Doubles as a smoke test that projections stay finite.
 */
class MapRenderPreviewTest {

    private val data: WorldMapData by lazy {
        var dir: File? = File(System.getProperty("user.dir")!!)
        var file: File? = null
        while (dir != null && file == null) {
            listOf("src/main/assets/worldmap.bin", "app/src/main/assets/worldmap.bin")
                .map { File(dir, it) }
                .firstOrNull { it.exists() }
                ?.let { file = it }
            dir = dir.parentFile
        }
        WorldMapAsset.parse(file!!.readBytes())
    }

    private fun render(name: String, w: Int, h: Int, viewport: MapViewport, rotationDeg: Float, morph: Float) {
        val x = FloatArray(data.vertexCount)
        val y = FloatArray(data.vertexCount)
        val d = FloatArray(data.vertexCount)
        MapProjection.projectAll(data, viewport, w.toFloat(), h.toFloat(), rotationDeg, morph, x, y, d)
        for (i in 0 until data.vertexCount) {
            assertTrue("finite", x[i].isFinite() && y[i].isFinite())
        }

        val img = Raster(w, h)
        img.clear(0x0C110F)
        val cx = w / 2f
        val cy = h / 2f
        val r = MapProjection.globeRadius(w.toFloat(), h.toFloat())
        if (morph > 0.01f) img.fillCircle(cx, cy, r * 1.02f, 0x131B16)

        val clamp = morph > 0.5f
        val vx = FloatArray(1024)
        val vy = FloatArray(1024)
        for (ring in 0 until data.ringCount) {
            val start = data.ringStart[ring]
            val size = data.ringSize[ring]
            if (size > vx.size) continue
            var maxDepth = -2f
            for (i in start until start + size) if (d[i] > maxDepth) maxDepth = d[i]
            if (morph > 0.5f && maxDepth < -0.05f) continue
            for (k in 0 until size) {
                val i = start + k
                var px = x[i]
                var py = y[i]
                if (clamp && d[i] < 0f) {
                    val dx = px - cx; val dy = py - cy
                    val len = sqrt(dx * dx + dy * dy)
                    if (len > 0.0001f) { px = cx + dx / len * r; py = cy + dy / len * r }
                }
                vx[k] = px; vy[k] = py
            }
            val tagged = data.ringCountry[ring] != 0
            img.fillPolygon(vx, vy, size, if (tagged) 0x24322B else 0x1B241F)
        }

        val out = File("build/map-previews").apply { mkdirs() }
        img.writePng(File(out, "$name.png"))
    }

    @Test
    fun `render previews`() {
        render("flat_world", 1080, 720, MapViewport.World, 0f, 0f)
        render("globe_europe", 900, 900, MapViewport.World, -16f, 1f)
        render("globe_americas", 900, 900, MapViewport.World, 95f, 1f)
        render("globe_asia", 900, 900, MapViewport.World, -110f, 1f)
        render("morph_half", 1080, 900, MapViewport.World, -16f, 0.5f)
        render("locator_france", 800, 500, MapViewport(2.2f, 46.2f, 5f), 0f, 0f)
        render("locator_japan", 800, 500, MapViewport(138.3f, 36.2f, 5f), 0f, 0f)
    }
}

/** Just enough imaging for previews: RGB buffer, even-odd scanline fill, PNG out. */
private class Raster(val w: Int, val h: Int) {
    private val pix = IntArray(w * h)
    private val xs = FloatArray(256)

    fun clear(rgb: Int) = pix.fill(rgb)

    fun fillCircle(cx: Float, cy: Float, r: Float, rgb: Int) {
        val r2 = r * r
        for (yy in maxOf(0, (cy - r).toInt())..minOf(h - 1, (cy + r).toInt())) {
            for (xx in maxOf(0, (cx - r).toInt())..minOf(w - 1, (cx + r).toInt())) {
                val dx = xx + 0.5f - cx; val dy = yy + 0.5f - cy
                if (dx * dx + dy * dy <= r2) pix[yy * w + xx] = rgb
            }
        }
    }

    fun fillPolygon(vx: FloatArray, vy: FloatArray, n: Int, rgb: Int) {
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (i in 0 until n) { if (vy[i] < minY) minY = vy[i]; if (vy[i] > maxY) maxY = vy[i] }
        for (yy in maxOf(0, minY.toInt())..minOf(h - 1, maxY.toInt())) {
            val fy = yy + 0.5f
            var m = 0
            var j = n - 1
            for (i in 0 until n) {
                val y1 = vy[i]; val y2 = vy[j]
                if ((y1 <= fy && y2 > fy) || (y2 <= fy && y1 > fy)) {
                    if (m < xs.size) xs[m++] = vx[i] + (fy - y1) / (y2 - y1) * (vx[j] - vx[i])
                }
                j = i
            }
            xs.sort(0, m)
            var k = 0
            while (k + 1 < m) {
                val from = maxOf(0, xs[k].toInt())
                val to = minOf(w - 1, xs[k + 1].toInt())
                for (xx in from..to) pix[yy * w + xx] = rgb
                k += 2
            }
        }
    }

    fun writePng(file: File) {
        val raw = ByteArray(h * (1 + w * 3))
        var p = 0
        for (yy in 0 until h) {
            raw[p++] = 0
            for (xx in 0 until w) {
                val c = pix[yy * w + xx]
                raw[p++] = (c shr 16).toByte()
                raw[p++] = (c shr 8).toByte()
                raw[p++] = c.toByte()
            }
        }
        val deflater = Deflater(Deflater.BEST_SPEED)
        deflater.setInput(raw)
        deflater.finish()
        val buf = ByteArray(raw.size + 1024)
        var len = 0
        while (!deflater.finished()) len += deflater.deflate(buf, len, buf.size - len)
        deflater.end()

        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
        val ihdr = ByteArray(13)
        writeInt(ihdr, 0, w); writeInt(ihdr, 4, h)
        ihdr[8] = 8; ihdr[9] = 2 // 8-bit RGB
        chunk(out, "IHDR", ihdr)
        chunk(out, "IDAT", buf.copyOf(len))
        chunk(out, "IEND", ByteArray(0))
        file.writeBytes(out.toByteArray())
    }

    private fun writeInt(b: ByteArray, at: Int, v: Int) {
        b[at] = (v ushr 24).toByte(); b[at + 1] = (v ushr 16).toByte()
        b[at + 2] = (v ushr 8).toByte(); b[at + 3] = v.toByte()
    }

    private fun chunk(out: ByteArrayOutputStream, type: String, dataBytes: ByteArray) {
        val lenB = ByteArray(4); writeInt(lenB, 0, dataBytes.size)
        out.write(lenB)
        val typeB = type.toByteArray(Charsets.US_ASCII)
        out.write(typeB)
        out.write(dataBytes)
        val crc = CRC32()
        crc.update(typeB); crc.update(dataBytes)
        val crcB = ByteArray(4); writeInt(crcB, 0, crc.value.toInt())
        out.write(crcB)
    }
}
