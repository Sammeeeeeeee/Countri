package dev.sam.countri.data

import kotlinx.serialization.json.Json

/**
 * One JSON configuration for the whole app — Room converters and the backup
 * codec share it, so a value written by one is always readable by the other.
 * Lenient on unknown keys so an older build can still open a newer file's
 * common fields instead of throwing while merely reading a row.
 */
val CountriJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}
