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
 * names match. Results are filtered to the country being edited and to
 * things worth travelling for; failures degrade to no suggestions and
 * typing is never blocked on the network.
 */
object PlaceSearch {

    // Geography, sights and nature — not shops and offices.
    private val junkKeys = setOf("shop", "office", "craft", "highway", "emergency")

    suspend fun search(query: String, iso2: String): List<FoundPlace> =
        withContext(Dispatchers.IO) {
            val q = query.trim()
            if (q.length < 2) return@withContext emptyList()
            runCatching {
                val url = URL(
                    "https://photon.komoot.io/api/?q=" + URLEncoder.encode(q, "UTF-8") +
                        "&limit=18&lang=en"
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
                    features.mapNotNull { feature ->
                        val props = feature.jsonObject["properties"]?.jsonObject
                            ?: return@mapNotNull null
                        val cc = props["countrycode"]?.jsonPrimitive?.content
                        if (!iso2.equals(cc, ignoreCase = true)) return@mapNotNull null
                        val key = props["osm_key"]?.jsonPrimitive?.content
                        if (key in junkKeys) return@mapNotNull null
                        val name = props["name"]?.jsonPrimitive?.content
                            ?.takeUnless { it.isBlank() }
                            ?: return@mapNotNull null
                        val value = props["osm_value"]?.jsonPrimitive?.content
                        FoundPlace(
                            name = name,
                            kind = (value?.takeUnless { it == "yes" } ?: key)
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
