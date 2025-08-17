package chat.peptide.api.routes.user

import chat.peptide.api.PeptideError
import chat.peptide.api.PeptideHttp
import chat.peptide.api.PeptideJson
import chat.peptide.api.api
import chat.peptide.api.schemas.Channel
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerializationException

suspend fun openDM(userId: String): Channel {
    val response = PeptideHttp.get("/users/$userId/dm".api())
        .bodyAsText()

    try {
        val error = PeptideJson.decodeFromString(PeptideError.serializer(), response)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    return PeptideJson.decodeFromString(Channel.serializer(), response)
}