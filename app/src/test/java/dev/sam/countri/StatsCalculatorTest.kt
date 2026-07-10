package dev.sam.countri

import dev.sam.countri.data.catalog.Continent
import dev.sam.countri.data.catalog.CountryCatalog
import dev.sam.countri.domain.CountryStatus
import dev.sam.countri.domain.CountryWithState
import dev.sam.countri.domain.StatsCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsCalculatorTest {

    private fun visited(iso: String, year: Int?, places: List<String> = emptyList()) =
        CountryWithState(
            country = CountryCatalog.byIso2.getValue(iso),
            status = CountryStatus.VISITED,
            firstVisitYear = year,
            places = places,
            trips = 1,
        )

    private fun wishlist(iso: String) =
        CountryWithState(CountryCatalog.byIso2.getValue(iso), CountryStatus.WISHLIST)

    @Test
    fun `aggregates counts, percent, continents and places`() {
        val stats = StatsCalculator.calculate(
            listOf(
                visited("FR", 2018, listOf("Paris", "Lyon")),
                visited("ES", 2019, listOf("Madrid")),
                visited("JP", 2019),
                wishlist("NO"),
                CountryWithState(CountryCatalog.byIso2.getValue("DE")),
            ),
            fallbackYear = 2026,
        )
        assertEquals(3, stats.visitedCount)
        assertEquals(1, stats.wishlistCount)
        assertEquals(2, stats.percentOfWorld) // round(3/195*100) = 2
        assertEquals(2, stats.continentsVisited)
        assertEquals(3, stats.placeTotal)
    }

    @Test
    fun `continent breakdown is sorted by visited count`() {
        val stats = StatsCalculator.calculate(
            listOf(visited("FR", 2018), visited("ES", 2019), visited("JP", 2020)),
            fallbackYear = 2026,
        )
        assertEquals(Continent.EUROPE, stats.byContinent.first().continent)
        assertEquals(2, stats.byContinent.first().visited)
        assertEquals(Continent.entries.size, stats.byContinent.size)
    }

    @Test
    fun `timeline groups by year ascending with fallback for missing years`() {
        val stats = StatsCalculator.calculate(
            listOf(visited("FR", 2019), visited("ES", 2015), visited("JP", 2019), visited("BR", null)),
            fallbackYear = 2026,
        )
        assertEquals(listOf(2015, 2019, 2026), stats.timeline.map { it.year })
        assertEquals(listOf("FR", "JP"), stats.timeline[1].isoCodes)
        assertEquals(listOf("BR"), stats.timeline[2].isoCodes)
    }

    @Test
    fun `empty atlas is all zeros`() {
        val stats = StatsCalculator.calculate(emptyList(), 2026)
        assertEquals(0, stats.visitedCount)
        assertEquals(0, stats.percentOfWorld)
        assertEquals(0, stats.continentsVisited)
        assertEquals(0, stats.timeline.size)
    }
}
