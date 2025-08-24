package chat.peptide.api.routes.onboard

import chat.peptide.api.RateLimitResponse
import chat.peptide.api.PeptideAPI
import chat.peptide.api.PeptideError
import chat.peptide.api.PeptideHttp
import chat.peptide.api.PeptideJson
import chat.peptide.api.api
import chat.peptide.api.schemas.RsResult
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

@Serializable
data class OnboardingResponse(
    val onboarding: Boolean
)

suspend fun needsOnboarding(sessionToken: String = PeptideAPI.sessionToken): Boolean {
    val response = PeptideHttp.get("/onboard/hello".api()) {
        header(PeptideAPI.TOKEN_HEADER_NAME, sessionToken)
    }

    val responseContent = response.bodyAsText()

    try {
        val rateLimitResponse =
            PeptideJson.decodeFromString(RateLimitResponse.serializer(), responseContent)
        throw rateLimitResponse.toException()
    } catch (e: SerializationException) {
        // good path
    }

    return PeptideJson.decodeFromString(OnboardingResponse.serializer(), responseContent).onboarding
}

@Serializable
data class OnboardingCompletionBody(
    val username: String
)

suspend fun completeOnboarding(
    body: OnboardingCompletionBody,
    sessionToken: String = PeptideAPI.sessionToken
): RsResult<Unit, PeptideError> {
    val response = PeptideHttp.post("/onboard/complete".api()) {
        setBody(body)
        contentType(ContentType.Application.Json)
        header(PeptideAPI.TOKEN_HEADER_NAME, sessionToken)
    }

    if (response.status == HttpStatusCode.Conflict) {
        return RsResult.err(PeptideError("UsernameTaken"))
    }

    if (response.status == HttpStatusCode.BadRequest) {
        return RsResult.err(PeptideError("InvalidUsername"))
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
