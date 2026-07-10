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

/** One country's contribution to a year: which cities, how many days. */
data class TimelineEntry(val iso2: String, val cities: List<String>, val days: Int)

data class YearGroup(val year: Int, val entries: List<TimelineEntry>) {
    val isoCodes: List<String> get() = entries.map { it.iso2 }.distinct().sorted()
    val cities: List<String> get() = entries.flatMap { it.cities }.distinct()
    val totalDays: Int get() = entries.sumOf { it.days }
}

data class AtlasStats(
    val visitedCount: Int,
    val wishlistCount: Int,
    val percentOfWorld: Int,
    val continentsVisited: Int,
    val placeTotal: Int,
    val byContinent: List<ContinentStat>,
    val timeline: List<YearGroup>,
)

object StatsCalculator {

    fun calculate(countries: List<CountryWithState>, fallbackYear: Int): AtlasStats {
        val visited = countries.filter { it.isVisited }
        val byContinent = Continent.entries
            .map { cont -> ContinentStat(cont, visited.count { it.country.continent == cont }) }
            .sortedWith(compareByDescending<ContinentStat> { it.visited }.thenBy { it.continent.ordinal })
        // Visits carry the real story (cities, days); countries marked
        // visited before visits existed fall back to their legacy year.
        val timeline = visited
            .flatMap { entry ->
                if (entry.visits.isNotEmpty()) {
                    entry.visits.map { visit ->
                        visit.start.year to TimelineEntry(
                            iso2 = entry.country.iso2,
                            cities = visit.cities,
                            days = visit.days,
                        )
                    }
                } else {
                    listOf(
                        (entry.firstVisitYear ?: fallbackYear) to TimelineEntry(
                            iso2 = entry.country.iso2,
                            cities = emptyList(),
                            days = 0,
                        )
                    )
                }
            }
            .groupBy({ it.first }, { it.second })
            .map { (year, entries) -> YearGroup(year, entries.sortedBy { it.iso2 }) }
            .sortedBy { it.year }
        return AtlasStats(
            visitedCount = visited.size,
            wishlistCount = countries.count { it.isWishlist },
            percentOfWorld = (visited.size * 100f / WORLD_COUNTRY_COUNT).roundToInt(),
            continentsVisited = visited.map { it.country.continent }.toSet().size,
            placeTotal = countries.sumOf { it.places.size },
            byContinent = byContinent,
            timeline = timeline,
        )
    }
}
