package chat.peptide.api.routes.onboard

import chat.peptide.api.RateLimitResponse
import chat.peptide.api.RevoltAPI
import chat.peptide.api.RevoltError
import chat.peptide.api.RevoltHttp
import chat.peptide.api.RevoltJson
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

suspend fun needsOnboarding(sessionToken: String = RevoltAPI.sessionToken): Boolean {
    val response = RevoltHttp.get("/onboard/hello".api()) {
        header(RevoltAPI.TOKEN_HEADER_NAME, sessionToken)
    }

    val responseContent = response.bodyAsText()

    try {
        val rateLimitResponse =
            RevoltJson.decodeFromString(RateLimitResponse.serializer(), responseContent)
        throw rateLimitResponse.toException()
    } catch (e: SerializationException) {
        // good path
    }

    return RevoltJson.decodeFromString(OnboardingResponse.serializer(), responseContent).onboarding
}

@Serializable
data class OnboardingCompletionBody(
    val username: String
)

suspend fun completeOnboarding(
    body: OnboardingCompletionBody,
    sessionToken: String = RevoltAPI.sessionToken
): RsResult<Unit, RevoltError> {
    val response = RevoltHttp.post("/onboard/complete".api()) {
        setBody(body)
        contentType(ContentType.Application.Json)
        header(RevoltAPI.TOKEN_HEADER_NAME, sessionToken)
    }

    if (response.status == HttpStatusCode.Conflict) {
        return RsResult.err(RevoltError("UsernameTaken"))
    }

    if (response.status == HttpStatusCode.BadRequest) {
        return RsResult.err(RevoltError("InvalidUsername"))
    }

    val responseContent = response.bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), responseContent)
        return RsResult.err(error)
    } catch (e: SerializationException) {
        // Not an error
    }

    return RsResult.ok(Unit)
}
