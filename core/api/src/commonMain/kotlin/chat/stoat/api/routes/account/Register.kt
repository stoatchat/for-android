package chat.stoat.api.routes.account

import chat.stoat.api.StoatAPIError
import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatJson
import chat.stoat.core.model.util.RsResult
import chat.stoat.api.api
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

suspend fun register(body: RegistrationBody): RsResult<Unit, StoatAPIError> {
    val response = StoatHttp.post("/auth/account/create".api()) {
        setBody(body)
        contentType(ContentType.Application.Json)
    }

    val responseContent = response.bodyAsText()

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), responseContent)
        return RsResult.err(error)
    } catch (e: SerializationException) {
        // Not an error
    }

    return RsResult.ok(Unit)
}
