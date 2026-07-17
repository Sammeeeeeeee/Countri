package dev.sam.countri.data.backup

import dev.sam.countri.data.CountriJson
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
    // Null in pre-dual-state backups; restore derives it from status then.
    val wishlisted: Boolean? = null,
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
    // Required, deliberately: a foreign JSON document that merely happens to
    // parse (an unrelated file, a fragment) has no version field and is
    // rejected here, instead of decoding to an empty atlas that would wipe the
    // user's data on restore.
    val version: Int,
    val states: List<BackupState> = emptyList(),
    val visits: List<BackupVisit> = emptyList(),
)

object Backup {
    /** Bump when the on-disk shape changes in a way older builds can't read. */
    const val CURRENT_VERSION = 1

    private val json = Json(from = CountriJson) { prettyPrint = true }

    fun encode(states: List<CountryStateEntity>, visits: List<VisitEntity>): String =
        json.encodeToString(
            BackupData(
                version = CURRENT_VERSION,
                states = states.map {
                    BackupState(
                        it.iso2, it.status, it.firstVisitYear, it.note, it.tags, it.trips,
                        it.wishlisted,
                    )
                },
                visits = visits.map {
                    BackupVisit(it.iso2, it.startDay, it.endDay, it.cities)
                },
            )
        )

    /**
     * Parse a backup document, refusing anything this build can't safely
     * restore. A file from a newer, incompatible format is rejected here —
     * before the caller touches the existing data — so a bad import can never
     * wipe the atlas and leave nothing in its place.
     */
    fun decode(text: String): BackupData {
        val data = json.decodeFromString<BackupData>(text)
        require(data.version in 1..CURRENT_VERSION) {
            "Unsupported backup version ${data.version}"
        }
        return data
    }
}
