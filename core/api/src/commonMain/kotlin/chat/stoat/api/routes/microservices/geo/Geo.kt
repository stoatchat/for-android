package chat.stoat.api.routes.microservices.geo

import chat.stoat.api.HitRateLimitException
import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatJson
import chat.stoat.api.buildUserAgent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

@Serializable
data class GeoResponse(
    val countryCode: String,
    val isAgeRestrictedGeo: Boolean,
)

suspend fun queryGeo(): GeoResponse {
    try {
        val response = StoatHttp.get("https://geo.revolt.chat/?client=android") {
            header("User-Agent", buildUserAgent("Ktor queryGeo"))
        }

        if (response.status == HttpStatusCode.OK) {
            return StoatJson.decodeFromString(response.bodyAsText())
        } else throw Exception("Failed to query geo: ${response.status.value} ${response.status.description}")
    } catch (e: Exception) {
        throw Exception("Failed to query geo: ${e.message}", e).also {
            if (e is HitRateLimitException) {
                throw e
            }
        }
    }
}