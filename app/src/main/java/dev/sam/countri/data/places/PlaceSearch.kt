package dev.sam.countri.data.places

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class FoundPlace(val name: String, val kind: String?)

/**
 * Search-as-you-type against OpenStreetMap's Nominatim — free, keyless,
 * and it only knows about actual places, scoped to the country being
 * edited, so no stray band pages or surnames sneak into the suggestions.
 * Failures degrade to no suggestions; typing is never blocked on the net.
 */
object PlaceSearch {

    // Things that are geography, sights, or nature — not shops and offices.
    private val junkClasses = setOf(
        "shop", "office", "craft", "building", "highway", "landuse", "emergency",
    )

    suspend fun search(query: String, iso2: String): List<FoundPlace> =
        withContext(Dispatchers.IO) {
            val q = query.trim()
            if (q.length < 2) return@withContext emptyList()
            runCatching {
                val url = URL(
                    "https://nominatim.openstreetmap.org/search?format=jsonv2" +
                        "&q=" + URLEncoder.encode(q, "UTF-8") +
                        "&countrycodes=" + iso2.lowercase() +
                        "&limit=10&accept-language=en"
                )
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4_000
                    readTimeout = 4_000
                    // Nominatim's usage policy asks for an identifiable agent.
                    setRequestProperty(
                        "User-Agent",
                        "Countri/0.4 (https://github.com/Sammeeeeeeee/Countri)",
                    )
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    val body = connection.inputStream.bufferedReader().readText()
                    Json.parseToJsonElement(body).jsonArray
                        .mapNotNull { item ->
                            val obj = item.jsonObject
                            val cls = obj["class"]?.jsonPrimitive?.content
                            if (cls in junkClasses) return@mapNotNull null
                            val name = obj["name"]?.jsonPrimitive?.content
                                ?.takeUnless { it.isBlank() }
                                ?: return@mapNotNull null
                            FoundPlace(
                                name = name,
                                kind = obj["type"]?.jsonPrimitive?.content
                                    ?.replace('_', ' '),
                            )
                        }
                        .distinctBy { it.name.lowercase() }
                        .take(6)
                } finally {
                    connection.disconnect()
                }
            }.getOrDefault(emptyList())
        }
}
