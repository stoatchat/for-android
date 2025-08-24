package chat.peptide.api.routes.voice

import chat.peptide.api.PeptideError
import chat.peptide.api.PeptideHttp
import chat.peptide.api.PeptideJson
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
    val response = PeptideHttp.post("/channels/$channelId/join_call".api()) {
        contentType(ContentType.Application.Json)
        setBody(mapOf("node" to nodeName))
    }.bodyAsText()

    try {
        val error = PeptideJson.decodeFromString(PeptideError.serializer(), response)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    return PeptideJson.decodeFromString(JoinCallResponse.serializer(), response)
}