package dev.sam.countri.data.repo

import dev.sam.countri.data.catalog.CountryCatalog
import dev.sam.countri.data.db.CountryStateDao
import dev.sam.countri.data.db.CountryStateEntity
import dev.sam.countri.data.db.VisitDao
import dev.sam.countri.data.db.VisitEntity
import dev.sam.countri.domain.CountryStatus
import dev.sam.countri.domain.CountryWithState
import dev.sam.countri.domain.Visit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

/**
 * The single source every screen derives from: the static 195-country
 * catalog joined with the user's country rows and visit records.
 */
class AtlasRepository(
    private val dao: CountryStateDao,
    private val visitDao: VisitDao,
) {

    val countries: Flow<List<CountryWithState>> =
        combine(dao.observeAll(), visitDao.observeAll()) { rows, visitRows ->
            val byIso = rows.associateBy { it.iso2 }
            val visitsByIso = visitRows.groupBy { it.iso2 }
            CountryCatalog.all.map { country ->
                val row = byIso[country.iso2]
                val hasVisits = visitsByIso.containsKey(country.iso2)
                CountryWithState(
                    country = country,
                    // Recorded trips outrank any stored status: a country
                    // with visits can never demote to wishlist.
                    status = if (hasVisits) CountryStatus.VISITED
                    else row?.status?.let {
                        runCatching { CountryStatus.valueOf(it) }.getOrNull()
                    },
                    firstVisitYear = row?.firstVisitYear,
                    note = row?.note,
                    places = row?.tags ?: emptyList(),
                    trips = row?.trips ?: 0,
                    visits = (visitsByIso[country.iso2] ?: emptyList()).map { v ->
                        Visit(
                            id = v.id,
                            start = LocalDate.ofEpochDay(v.startDay),
                            end = LocalDate.ofEpochDay(v.endDay),
                            cities = v.cities,
                        )
                    },
                )
            }
        }

    /** Sets or changes status, preserving details when a row already exists. */
    suspend fun setStatus(iso2: String, status: CountryStatus, existing: CountryWithState?) {
        dao.upsert(
            CountryStateEntity(
                iso2 = iso2,
                status = status.name,
                firstVisitYear = existing?.firstVisitYear,
                note = existing?.note,
                tags = existing?.places ?: emptyList(),
                trips = existing?.trips ?: 0,
            )
        )
    }

    /** Records a trip; the country becomes visited if it wasn't already. */
    suspend fun addVisit(
        iso2: String,
        start: LocalDate,
        end: LocalDate,
        cities: List<String>,
        existing: CountryWithState?,
    ) {
        visitDao.insert(
            VisitEntity(
                iso2 = iso2,
                startDay = start.toEpochDay(),
                endDay = end.toEpochDay(),
                cities = cities.map { it.trim() }.filter { it.isNotEmpty() },
            )
        )
        if (existing?.status != CountryStatus.VISITED) {
            setStatus(iso2, CountryStatus.VISITED, existing)
        }
    }

    suspend fun updateVisit(
        id: Long,
        iso2: String,
        start: LocalDate,
        end: LocalDate,
        cities: List<String>,
    ) {
        visitDao.update(
            VisitEntity(
                id = id,
                iso2 = iso2,
                startDay = start.toEpochDay(),
                endDay = end.toEpochDay(),
                cities = cities.map { it.trim() }.filter { it.isNotEmpty() },
            )
        )
    }

    suspend fun deleteVisit(id: Long) = visitDao.delete(id)

    suspend fun updateDetails(
        current: CountryWithState,
        firstVisitYear: Int? = current.firstVisitYear,
        note: String? = current.note,
        places: List<String> = current.places,
        trips: Int = current.trips,
    ) {
        val status = current.status ?: return
        dao.upsert(
            CountryStateEntity(
                iso2 = current.country.iso2,
                status = status.name,
                firstVisitYear = firstVisitYear,
                note = note?.takeUnless { it.isBlank() },
                tags = places.map { it.trim() }.filter { it.isNotEmpty() },
                trips = trips.coerceIn(0, 999),
            )
        )
    }

    suspend fun clear(iso2: String) {
        visitDao.deleteAllFor(iso2)
        dao.delete(iso2)
    }
}
