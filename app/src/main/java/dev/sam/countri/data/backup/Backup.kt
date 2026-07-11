package dev.sam.countri.data.backup

import dev.sam.countri.data.db.CountryStateEntity
import dev.sam.countri.data.db.VisitEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The whole atlas as one JSON document. Exported through the system file
 * picker, so "back up to Google Drive" is just choosing Drive as the
 * destination — no accounts, no APIs, restorable anywhere.
 */
@Serializable
data class BackupState(
    val iso2: String,
    val status: String,
    val firstVisitYear: Int? = null,
    val note: String? = null,
    val tags: List<String> = emptyList(),
    val trips: Int = 0,
)

@Serializable
data class BackupVisit(
    val iso2: String,
    val startDay: Long,
    val endDay: Long,
    val cities: List<String> = emptyList(),
)

@Serializable
data class BackupData(
    val version: Int = 1,
    val states: List<BackupState> = emptyList(),
    val visits: List<BackupVisit> = emptyList(),
)

object Backup {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun encode(states: List<CountryStateEntity>, visits: List<VisitEntity>): String =
        json.encodeToString(
            BackupData(
                states = states.map {
                    BackupState(it.iso2, it.status, it.firstVisitYear, it.note, it.tags, it.trips)
                },
                visits = visits.map {
                    BackupVisit(it.iso2, it.startDay, it.endDay, it.cities)
                },
            )
        )

    fun decode(text: String): BackupData = json.decodeFromString(text)
}
