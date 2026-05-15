package chat.stoat.api.routes.microservices.gazette

import chat.stoat.api.HitRateLimitException
import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatJson
import chat.stoat.api.buildUserAgent
import chat.stoat.core.model.data.STOAT_CHANGELOG
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GazetteChangelog(
    val id: String,
    val title: String,
    @SerialName("markdown_content") val markdownContent: String,
    @SerialName("ios_version") val iosVersion: String,
    @SerialName("android_version") val androidVersion: String,
    @SerialName("web_version") val webVersion: String,
    @SerialName("published_at") val publishedAt: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

suspend fun getLatestChangelog(): GazetteChangelog {
    try {
        val response = StoatHttp.get("$STOAT_CHANGELOG/v1/changelogs/latest") {
            header("User-Agent", buildUserAgent())
        }

        if (response.status == HttpStatusCode.OK) {
            return StoatJson.decodeFromString(response.bodyAsText())
        } else throw Exception("Failed to query changelog: ${response.status.value} ${response.status.description}")
    } catch (e: Exception) {
        throw Exception("Failed to query changelog: ${e.message}", e).also {
            if (e is HitRateLimitException) {
                throw e
            }
        }
    }
}

suspend fun getChangelogById(id: String): GazetteChangelog {
    try {
        val response = StoatHttp.get("$STOAT_CHANGELOG/v1/changelogs/$id") {
            header("User-Agent", buildUserAgent())
        }

        if (response.status == HttpStatusCode.OK) {
            return StoatJson.decodeFromString(response.bodyAsText())
        } else throw Exception("Failed to query changelog: ${response.status.value} ${response.status.description}")
    } catch (e: Exception) {
        throw Exception("Failed to query changelog: ${e.message}", e).also {
            if (e is HitRateLimitException) {
                throw e
            }
        }
    }
}