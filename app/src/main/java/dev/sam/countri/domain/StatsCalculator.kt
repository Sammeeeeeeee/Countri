package dev.sam.countri.domain

import dev.sam.countri.data.catalog.Continent
import dev.sam.countri.data.catalog.WORLD_COUNTRY_COUNT
import kotlin.math.roundToInt

data class ContinentStat(
    val continent: Continent,
    val visited: Int,
) {
    val fraction: Float get() = visited.toFloat() / continent.total
}

data class YearGroup(val year: Int, val isoCodes: List<String>)

data class AtlasStats(
    val visitedCount: Int,
    val wishlistCount: Int,
    val percentOfWorld: Int,
    val continentsVisited: Int,
    val cityTotal: Int,
    val byContinent: List<ContinentStat>,
    val timeline: List<YearGroup>,
)

object StatsCalculator {

    fun calculate(countries: List<CountryWithState>, fallbackYear: Int): AtlasStats {
        val visited = countries.filter { it.isVisited }
        val byContinent = Continent.entries
            .map { cont -> ContinentStat(cont, visited.count { it.country.continent == cont }) }
            .sortedWith(compareByDescending<ContinentStat> { it.visited }.thenBy { it.continent.ordinal })
        val timeline = visited
            .groupBy { it.firstVisitYear ?: fallbackYear }
            .map { (year, group) -> YearGroup(year, group.map { it.country.iso2 }.sorted()) }
            .sortedBy { it.year }
        return AtlasStats(
            visitedCount = visited.size,
            wishlistCount = countries.count { it.isWishlist },
            percentOfWorld = (visited.size * 100f / WORLD_COUNTRY_COUNT).roundToInt(),
            continentsVisited = visited.map { it.country.continent }.toSet().size,
            cityTotal = visited.sumOf { it.cities.size },
            byContinent = byContinent,
            timeline = timeline,
        )
    }
}
