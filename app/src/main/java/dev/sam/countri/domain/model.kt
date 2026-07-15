package dev.sam.countri.domain

import dev.sam.countri.data.catalog.Country
import java.time.LocalDate

enum class CountryStatus { VISITED, WISHLIST }

/** One trip: a date range plus the cities it touched. */
data class Visit(
    val id: Long,
    val start: LocalDate,
    val end: LocalDate,
    val cities: List<String>,
) {
    /**
     * Inclusive length in days. Floored at 1: a visit always spans at least
     * the day it happened, and this keeps a corrupt or hand-edited range
     * (end before start) from ever contributing a zero or negative day count
     * to the timeline totals.
     */
    val days: Int get() = (end.toEpochDay() - start.toEpochDay() + 1).toInt().coerceAtLeast(1)
}

data class CountryWithState(
    val country: Country,
    val status: CountryStatus? = null,
    /** Legacy year for entries created before visits existed. */
    val firstVisitYear: Int? = null,
    val note: String? = null,
    /** Places to see / seen — Wikipedia titles, linkable. */
    val places: List<String> = emptyList(),
    /** Legacy trip count for entries without visit records. */
    val trips: Int = 0,
    val visits: List<Visit> = emptyList(),
) {
    val isVisited get() = status == CountryStatus.VISITED
    val isWishlist get() = status == CountryStatus.WISHLIST

    /** Year of the earliest visit; falls back to the legacy year. */
    val firstYear: Int? get() = visits.minOfOrNull { it.start.year } ?: firstVisitYear

    val tripCount: Int get() = if (visits.isNotEmpty()) visits.size else trips

    val allCities: List<String>
        get() = visits.flatMap { it.cities }.distinctBy { it.lowercase() }
}
