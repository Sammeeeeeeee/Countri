package dev.sam.countri

import dev.sam.countri.data.catalog.CountryCatalog
import dev.sam.countri.data.map.DetailMapAsset
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DetailMapAssetTest {

    private val data by lazy {
        DetailMapAsset.parse(assetFile().readBytes())
    }

    private fun assetFile(): File {
        var dir: File? = File(System.getProperty("user.dir")!!)
        while (dir != null) {
            for (candidate in listOf("src/main/assets/detailmap.bin", "app/src/main/assets/detailmap.bin")) {
                val f = File(dir, candidate)
                if (f.exists()) return f
            }
            dir = dir.parentFile
        }
        error("detailmap.bin not found upwards of ${System.getProperty("user.dir")}")
    }

    @Test
    fun `every catalog country has rings`() {
        CountryCatalog.all.forEach { country ->
            val rings = data.ringsFor(country.iso2)
            assertNotNull("missing rings for ${country.iso2}", rings)
            assertTrue("empty rings for ${country.iso2}", rings!!.isNotEmpty())
        }
    }

    @Test
    fun `coordinates decode to sane degrees`() {
        CountryCatalog.all.forEach { country ->
            data.ringsFor(country.iso2)!!.forEach { ring ->
                assertTrue("odd-length ring for ${country.iso2}", ring.size % 2 == 0)
                assertTrue("degenerate ring for ${country.iso2}", ring.size >= 6)
                var i = 0
                while (i < ring.size) {
                    val lon = ring[i]
                    val lat = ring[i + 1]
                    assertTrue("lon out of range for ${country.iso2}: $lon", lon in -180f..180f)
                    assertTrue("lat out of range for ${country.iso2}: $lat", lat in -90f..90f)
                    i += 2
                }
            }
        }
    }

    @Test
    fun `detail is meaningfully denser than the world map for big countries`() {
        // Norway's 10m coastline should dwarf its 110m one.
        val norway = data.ringsFor("NO")!!.sumOf { it.size / 2 }
        assertTrue("expected dense Norway, got $norway vertices", norway > 400)
    }

    @Test
    fun `unknown country returns null`() {
        assertTrue(data.ringsFor("ZZ") == null)
    }
}
