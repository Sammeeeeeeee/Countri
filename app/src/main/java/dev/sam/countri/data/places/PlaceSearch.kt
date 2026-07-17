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
 * Search-as-you-type against Photon (komoot's OpenStreetMap search) —
 * free, keyless, and actually built for autocomplete, so half-typed
 * names match. The country being edited ranks first (with a location
 * bias toward it), but the world stays reachable: a match abroad shows
 * up labelled with its country instead of vanishing into an empty list.
 * Failures degrade to no suggestions; typing is never blocked.
 */
object PlaceSearch {

    // Geography, sights and nature — not shops and offices.
    private val junkKeys = setOf("shop", "office", "craft", "highway", "emergency")

    suspend fun search(
        query: String,
        iso2: String,
        biasLat: Float,
        biasLon: Float,
    ): List<FoundPlace> =
        withContext(Dispatchers.IO) {
            val q = query.trim()
            if (q.length < 2) return@withContext emptyList()
            runCatching {
                val url = URL(
                    "https://photon.komoot.io/api/?q=" + URLEncoder.encode(q, "UTF-8") +
                        "&limit=18&lang=en&lat=$biasLat&lon=$biasLon"
                )
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4_000
                    readTimeout = 4_000
                    setRequestProperty(
                        "User-Agent",
                        "Countri/0.4 (https://github.com/Sammeeeeeeee/Countri)",
                    )
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    val body = connection.inputStream.bufferedReader().readText()
                    val features = Json.parseToJsonElement(body)
                        .jsonObject["features"]?.jsonArray ?: return@runCatching emptyList()
                    val parsed = features.mapNotNull { feature ->
                        val props = feature.jsonObject["properties"]?.jsonObject
                            ?: return@mapNotNull null
                        val key = props["osm_key"]?.jsonPrimitive?.content
                        if (key in junkKeys) return@mapNotNull null
                        val name = props["name"]?.jsonPrimitive?.content
                            ?.takeUnless { it.isBlank() }
                            ?: return@mapNotNull null
                        val inCountry = iso2.equals(
                            props["countrycode"]?.jsonPrimitive?.content,
                            ignoreCase = true,
                        )
                        val base = (props["osm_value"]?.jsonPrimitive?.content
                            ?.takeUnless { it == "yes" } ?: key)
                            ?.replace('_', ' ')
                            ?.replaceFirstChar { it.uppercaseChar() }
                        val kind = if (inCountry) base else {
                            listOfNotNull(base, props["country"]?.jsonPrimitive?.content)
                                .joinToString("  ·  ")
                                .ifBlank { null }
                        }
                        FoundPlace(name, kind) to inCountry
                    }
                    val (home, away) = parsed.partition { it.second }
                    (home + away)
                        .map { it.first }
                        .distinctBy { it.name.lowercase() }
                        .take(6)
                } finally {
                    connection.disconnect()
                }
            }.onFailure {
                android.util.Log.w("Countri", "place search failed", it)
            }.getOrDefault(emptyList())
        }
}
