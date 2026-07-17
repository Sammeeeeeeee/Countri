package dev.sam.countri.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per country the user has touched; absence of a row means the
 * country is untracked. status is stored as text for schema readability.
 * tags = places to see / seen. wishlisted rides alongside status so a
 * country can be visited AND still on the wishlist.
 */
@Entity(tableName = "country_state")
data class CountryStateEntity(
    @PrimaryKey val iso2: String,
    val status: String, // "VISITED" | "WISHLIST"
    val firstVisitYear: Int?,
    val note: String?,
    val tags: List<String>,
    val trips: Int,
    @ColumnInfo(defaultValue = "0") val wishlisted: Boolean = false,
)
