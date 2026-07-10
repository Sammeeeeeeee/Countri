package dev.sam.countri.data.db

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromTags(tags: List<String>): String = Json.encodeToString(tags)

    @TypeConverter
    fun toTags(json: String): List<String> =
        if (json.isBlank()) emptyList() else Json.decodeFromString(json)
}
