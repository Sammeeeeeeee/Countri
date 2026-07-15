package dev.sam.countri.data.repo

import androidx.room.withTransaction
import dev.sam.countri.data.backup.Backup
import dev.sam.countri.data.catalog.CountryCatalog
import dev.sam.countri.data.db.AppDatabase
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
    private val db: AppDatabase,
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
                    // One source of truth for status: the stored row wins
                    // whenever it exists, so an explicit Wishlist is honoured
                    // even while dormant visit records sit behind it. Visits
                    // only imply Visited for a country with no row at all
                    // (untracked), never overriding a stored value.
                    status = row?.status?.let { runCatching { CountryStatus.valueOf(it) }.getOrNull() }
                        ?: if (hasVisits) CountryStatus.VISITED else null,
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
        // Guard the range at the boundary so no invalid visit ever reaches the
        // table: a visit spans at least its start day.
        val safeEnd = if (end.isBefore(start)) start else end
        visitDao.insert(
            VisitEntity(
                iso2 = iso2,
                startDay = start.toEpochDay(),
                endDay = safeEnd.toEpochDay(),
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
        val safeEnd = if (end.isBefore(start)) start else end
        visitDao.update(
            VisitEntity(
                id = id,
                iso2 = iso2,
                startDay = start.toEpochDay(),
                endDay = safeEnd.toEpochDay(),
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

    /** Removes a country's row and every visit under it, atomically. */
    suspend fun clear(iso2: String) = db.withTransaction {
        visitDao.deleteAllFor(iso2)
        dao.delete(iso2)
    }

    suspend fun exportBackup(): String =
        Backup.encode(dao.allOnce(), visitDao.allOnce())

    /**
     * Replaces everything with the backup's contents — atomically, and only
     * after the document has been validated. The whole swap runs in one
     * transaction, so a crash mid-import can never leave a half-restored or
     * empty atlas: it either fully applies or the old data survives untouched.
     */
    suspend fun importBackup(text: String) {
        // Parse and reject unsupported versions BEFORE deleting anything.
        val data = Backup.decode(text)

        val validIso = CountryCatalog.all.mapTo(HashSet()) { it.iso2 }
        val validStatus = CountryStatus.entries.mapTo(HashSet()) { it.name }

        // Drop rows the app can't represent rather than storing invisible junk
        // that would only bloat the DB and re-export forever.
        val states = data.states.filter { it.iso2 in validIso && it.status in validStatus }
        val visits = data.visits
            .filter { it.iso2 in validIso }
            .map { if (it.endDay < it.startDay) it.copy(endDay = it.startDay) else it }

        db.withTransaction {
            dao.deleteAll()
            visitDao.deleteAll()
            states.forEach { s ->
                dao.upsert(
                    CountryStateEntity(
                        iso2 = s.iso2,
                        status = s.status,
                        firstVisitYear = s.firstVisitYear,
                        note = s.note,
                        tags = s.tags,
                        trips = s.trips,
                    )
                )
            }
            visitDao.insertAll(
                visits.map { v ->
                    VisitEntity(iso2 = v.iso2, startDay = v.startDay, endDay = v.endDay, cities = v.cities)
                }
            )
            // Keep the DB self-consistent: a country that has visits but no
            // stored row is marked Visited, matching how the app writes its
            // own data. A country that carries an explicit row (e.g. Wishlist)
            // is left exactly as the backup recorded it.
            val stated = states.mapTo(HashSet()) { it.iso2 }
            visits.mapTo(HashSet()) { it.iso2 }.forEach { iso ->
                if (iso !in stated) {
                    dao.upsert(
                        CountryStateEntity(
                            iso2 = iso,
                            status = CountryStatus.VISITED.name,
                            firstVisitYear = null,
                            note = null,
                            tags = emptyList(),
                            trips = 0,
                        )
                    )
                }
            }
        }
    }
}
