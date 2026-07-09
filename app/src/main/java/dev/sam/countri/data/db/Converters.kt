package dev.sam.countri.data.db

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromCities(cities: List<String>): String = Json.encodeToString(cities)

    @TypeConverter
    fun toCities(json: String): List<String> =
        if (json.isBlank()) emptyList() else Json.decodeFromString(json)
}
