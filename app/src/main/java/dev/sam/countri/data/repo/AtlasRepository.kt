package dev.sam.countri.data.repo

import dev.sam.countri.data.catalog.CountryCatalog
import dev.sam.countri.data.db.CountryStateDao
import dev.sam.countri.data.db.CountryStateEntity
import dev.sam.countri.domain.CountryStatus
import dev.sam.countri.domain.CountryWithState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Year

/**
 * The single source every screen derives from: the static 195-country
 * catalog joined with the user's Room rows.
 */
class AtlasRepository(private val dao: CountryStateDao) {

    val countries: Flow<List<CountryWithState>> = dao.observeAll().map { rows ->
        val byIso = rows.associateBy { it.iso2 }
        CountryCatalog.all.map { country ->
            val row = byIso[country.iso2]
            CountryWithState(
                country = country,
                status = row?.status?.let { runCatching { CountryStatus.valueOf(it) }.getOrNull() },
                firstVisitYear = row?.firstVisitYear,
                note = row?.note,
                cities = row?.cities ?: emptyList(),
                trips = row?.trips ?: 0,
            )
        }
    }

    /** Sets or changes status, preserving details when a row already exists. */
    suspend fun setStatus(iso2: String, status: CountryStatus, existing: CountryWithState?) {
        dao.upsert(
            CountryStateEntity(
                iso2 = iso2,
                status = status.name,
                firstVisitYear = when (status) {
                    CountryStatus.VISITED -> existing?.firstVisitYear ?: Year.now().value
                    CountryStatus.WISHLIST -> existing?.firstVisitYear
                },
                note = existing?.note,
                cities = existing?.cities ?: emptyList(),
                trips = when (status) {
                    CountryStatus.VISITED -> maxOf(existing?.trips ?: 0, 1)
                    CountryStatus.WISHLIST -> existing?.trips ?: 0
                },
            )
        )
    }

    suspend fun updateDetails(
        current: CountryWithState,
        firstVisitYear: Int? = current.firstVisitYear,
        note: String? = current.note,
        cities: List<String> = current.cities,
        trips: Int = current.trips,
    ) {
        val status = current.status ?: return
        dao.upsert(
            CountryStateEntity(
                iso2 = current.country.iso2,
                status = status.name,
                firstVisitYear = firstVisitYear,
                note = note?.takeUnless { it.isBlank() },
                cities = cities.map { it.trim() }.filter { it.isNotEmpty() },
                trips = trips.coerceIn(0, 999),
            )
        )
    }

    suspend fun clear(iso2: String) = dao.delete(iso2)
}
