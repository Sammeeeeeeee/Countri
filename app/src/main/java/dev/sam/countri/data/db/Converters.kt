package dev.sam.countri.data.db

import androidx.room.TypeConverter
import dev.sam.countri.data.CountriJson

class Converters {
    @TypeConverter
    fun fromTags(tags: List<String>): String = CountriJson.encodeToString(tags)

    @TypeConverter
    fun toTags(json: String): List<String> =
        if (json.isBlank()) emptyList() else CountriJson.decodeFromString(json)
}
