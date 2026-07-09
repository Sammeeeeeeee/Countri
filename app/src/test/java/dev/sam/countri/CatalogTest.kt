package dev.sam.countri

import dev.sam.countri.data.catalog.Continent
import dev.sam.countri.data.catalog.CountryCatalog
import dev.sam.countri.data.catalog.WORLD_COUNTRY_COUNT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogTest {

    @Test
    fun `catalog has exactly 195 countries with unique codes`() {
        assertEquals(WORLD_COUNTRY_COUNT, CountryCatalog.all.size)
        assertEquals(WORLD_COUNTRY_COUNT, CountryCatalog.all.map { it.iso2 }.toSet().size)
        CountryCatalog.all.forEach { assertEquals(2, it.iso2.length) }
    }

    @Test
    fun `continent bucketing matches the fixed totals`() {
        val counts = CountryCatalog.all.groupingBy { it.continent }.eachCount()
        Continent.entries.forEach { cont ->
            assertEquals("continent $cont", cont.total, counts[cont])
        }
        assertEquals(WORLD_COUNTRY_COUNT, Continent.entries.sumOf { it.total })
    }

    @Test
    fun `centroids are in range`() {
        CountryCatalog.all.forEach {
            assertTrue(it.iso2, it.lat in -90f..90f)
            assertTrue(it.iso2, it.lon in -180f..180f)
        }
    }

    @Test
    fun `key assignments follow UN buckets`() {
        assertEquals(Continent.ASIA, CountryCatalog.byIso2.getValue("TR").continent)
        assertEquals(Continent.EUROPE, CountryCatalog.byIso2.getValue("RU").continent)
        assertEquals(Continent.AFRICA, CountryCatalog.byIso2.getValue("EG").continent)
        assertEquals(Continent.N_AMERICA, CountryCatalog.byIso2.getValue("MX").continent)
        assertEquals(Continent.OCEANIA, CountryCatalog.byIso2.getValue("PG").continent)
    }

    @Test
    fun `indexOf is 1-based and consistent`() {
        assertTrue(CountryCatalog.indexOf("FR") > 0)
        assertEquals(0, CountryCatalog.indexOf("ZZ"))
        val idx = CountryCatalog.indexOf("JP")
        assertEquals("JP", CountryCatalog.all[idx - 1].iso2)
    }
}
