package chat.stoat.api.routes.microservices.gifbox

import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatAPI
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val GIFBOX_BASE = "https://gifbox.stoat.chat"

private val gifboxJson = Json { ignoreUnknownKeys = true }

@Serializable
data class GifCategory(
    val title: String,
    val image: String
)

@Serializable
data class GifMediaFormat(
    val url: String
)

@Serializable
data class GifResult(
    val url: String,
    @SerialName("media_formats")
    val mediaFormats: Map<String, GifMediaFormat> = emptyMap()
)

@Serializable
data class GifSearchResponse(
    val results: List<GifResult> = emptyList(),
    val next: String? = null
)

object Gifbox {
    suspend fun fetchCategories(): List<GifCategory> {
        val response = StoatHttp.get("$GIFBOX_BASE/categories") {
            parameter("locale", "en_US")
            header(StoatAPI.TOKEN_HEADER_NAME, StoatAPI.sessionToken)
        }
        return gifboxJson.decodeFromString(response.bodyAsText())
    }

    suspend fun fetchTrending(limit: Int = 50): GifSearchResponse {
        val response = StoatHttp.get("$GIFBOX_BASE/trending") {
            parameter("locale", "en_US")
            parameter("limit", limit.toString())
            header(StoatAPI.TOKEN_HEADER_NAME, StoatAPI.sessionToken)
        }
        return gifboxJson.decodeFromString(response.bodyAsText())
    }

    suspend fun search(query: String, limit: Int = 50): GifSearchResponse {
        val response = StoatHttp.get("$GIFBOX_BASE/search") {
            parameter("locale", "en_US")
            parameter("query", query)
            parameter("limit", limit.toString())
            header(StoatAPI.TOKEN_HEADER_NAME, StoatAPI.sessionToken)
        }
        return gifboxJson.decodeFromString(response.bodyAsText())
    }
}
