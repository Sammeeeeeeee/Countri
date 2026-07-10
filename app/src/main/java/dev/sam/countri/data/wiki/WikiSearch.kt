package dev.sam.countri.data.wiki

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class WikiPlace(val title: String, val description: String?)

/**
 * Search-as-you-type against Wikipedia's public REST API — free, keyless,
 * and every saved place gets a page to open. Failures degrade to no
 * suggestions; typing is never blocked on the network.
 */
object WikiSearch {

    fun pageUrl(title: String): String =
        "https://en.wikipedia.org/wiki/" + URLEncoder.encode(title.replace(' ', '_'), "UTF-8")

    suspend fun search(query: String): List<WikiPlace> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.length < 2) return@withContext emptyList()
        runCatching {
            val url = URL(
                "https://en.wikipedia.org/w/rest.php/v1/search/title?q=" +
                    URLEncoder.encode(q, "UTF-8") + "&limit=6"
            )
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4_000
                readTimeout = 4_000
                setRequestProperty("User-Agent", "Countri/1.0 (Android)")
                setRequestProperty("Accept", "application/json")
            }
            try {
                val body = connection.inputStream.bufferedReader().readText()
                Json.parseToJsonElement(body).jsonObject["pages"]?.jsonArray
                    ?.mapNotNull { page ->
                        val obj = page.jsonObject
                        val title = obj["title"]?.jsonPrimitive?.content ?: return@mapNotNull null
                        WikiPlace(
                            title = title,
                            description = obj["description"]?.jsonPrimitive?.contentOrNullSafe(),
                        )
                    }
                    ?: emptyList()
            } finally {
                connection.disconnect()
            }
        }.getOrDefault(emptyList())
    }

    private fun kotlinx.serialization.json.JsonPrimitive.contentOrNullSafe(): String? =
        if (this.toString() == "null") null else content
}
