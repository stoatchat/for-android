package chat.peptide.api.routes.voice

import chat.peptide.api.RevoltError
import chat.peptide.api.RevoltHttp
import chat.peptide.api.RevoltJson
import chat.peptide.api.api
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

@Serializable
data class JoinCallResponse(
    val token: String,
    val url: String,
)

suspend fun joinCall(channelId: String, nodeName: String): JoinCallResponse {
    val response = RevoltHttp.post("/channels/$channelId/join_call".api()) {
        contentType(ContentType.Application.Json)
        setBody(mapOf("node" to nodeName))
    }.bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    return RevoltJson.decodeFromString(JoinCallResponse.serializer(), response)
}