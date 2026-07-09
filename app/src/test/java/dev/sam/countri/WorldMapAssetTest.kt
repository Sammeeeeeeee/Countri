package dev.sam.countri

import dev.sam.countri.data.catalog.CountryCatalog
import dev.sam.countri.data.map.WorldMapAsset
import dev.sam.countri.data.map.WorldMapData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WorldMapAssetTest {

    private val data: WorldMapData by lazy {
        WorldMapAsset.parse(assetFile().readBytes())
    }

    private fun assetFile(): File {
        var dir: File? = File(System.getProperty("user.dir")!!)
        while (dir != null) {
            for (candidate in listOf("src/main/assets/worldmap.bin", "app/src/main/assets/worldmap.bin")) {
                val f = File(dir, candidate)
                if (f.exists()) return f
            }
            dir = dir.parentFile
        }
        error("worldmap.bin not found upwards of ${System.getProperty("user.dir")}")
    }

    @Test
    fun `asset parses with a sane shape budget`() {
        assertTrue("rings: ${data.ringCount}", data.ringCount in 200..600)
        assertTrue("vertices: ${data.vertexCount}", data.vertexCount in 6000..12000)
    }

    @Test
    fun `all vertices are inside the rendered band`() {
        for (i in 0 until data.vertexCount) {
            assertTrue("lat ${data.lat[i]}", data.lat[i] in -57f..84f)
            assertTrue("lon ${data.lon[i]}", data.lon[i] in -180f..180f)
        }
    }

    @Test
    fun `every catalog country has at least one ring`() {
        val present = data.ringCountry.toSet()
        val missing = CountryCatalog.all.filterIndexed { i, _ -> (i + 1) !in present }
        assertTrue("missing rings for: ${missing.map { it.iso2 }}", missing.isEmpty())
    }

    @Test
    fun `hit-testing resolves known locations`() {
        fun isoAt(lon: Float, lat: Float): String? {
            val idx = data.countryAt(lon, lat)
            return if (idx == 0) null else CountryCatalog.all[idx - 1].iso2
        }
        assertEquals("FR", isoAt(2.35f, 48.85f))      // Paris
        assertEquals("BR", isoAt(-47.9f, -15.8f))     // Brasília
        assertEquals("AU", isoAt(133.8f, -25.3f))     // outback
        assertEquals("JP", isoAt(138.3f, 36.2f))      // Honshu
        assertEquals("SG", isoAt(103.8f, 1.35f))      // Singapore chip
        assertEquals(null, isoAt(-30f, 30f))          // mid-Atlantic
    }

    @Test
    fun `coastal points resolve via nearest-coastline fallback`() {
        fun isoNear(lon: Float, lat: Float): String? {
            val idx = data.countryNear(lon, lat, maxDeg = 1.5f)
            return if (idx == 0) null else CountryCatalog.all[idx - 1].iso2
        }
        // New York sits in the sea at 110m resolution; the fallback must catch it.
        assertEquals("US", isoNear(-74.0f, 40.7f))
        assertEquals(null, isoNear(-30f, 30f)) // mid-Atlantic stays a miss
    }

    @Test
    fun `enclave holes resolve to the inner country`() {
        val lesotho = data.countryAt(28.2f, -29.6f)
        assertEquals("LS", CountryCatalog.all[lesotho - 1].iso2)
        val southAfrica = data.countryAt(24f, -29f)
        assertEquals("ZA", CountryCatalog.all[southAfrica - 1].iso2)
    }
}
