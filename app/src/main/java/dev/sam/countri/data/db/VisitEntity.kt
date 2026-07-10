package dev.sam.countri.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One trip to one country: a date range and the cities it touched. */
@Entity(
    tableName = "visit",
    indices = [Index("iso2")],
)
data class VisitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val iso2: String,
    val startDay: Long, // LocalDate.toEpochDay()
    val endDay: Long,
    val cities: List<String>,
)
