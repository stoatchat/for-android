package chat.peptide.api.routes.account

import chat.peptide.api.PeptideError
import chat.peptide.api.PeptideHttp
import chat.peptide.api.PeptideJson
import chat.peptide.api.schemas.RsResult
import chat.peptide.api.api
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

@Serializable
data class RegistrationBody(
    val email: String,
    val password: String,
    val invite: String? = null,
    val captcha: String
)

suspend fun register(body: RegistrationBody): RsResult<Unit, PeptideError> {
    val response = PeptideHttp.post("/auth/account/create".api()) {
        setBody(body)
        contentType(ContentType.Application.Json)
    }

    val responseContent = response.bodyAsText()

    try {
        val error = PeptideJson.decodeFromString(PeptideError.serializer(), responseContent)
        return RsResult.err(error)
    } catch (e: SerializationException) {
        // Not an error
    }

    return RsResult.ok(Unit)
}
