package chat.peptide.api.routes.account

import android.os.Build
import android.util.Log
import chat.peptide.api.PeptideError
import chat.peptide.api.PeptideHttp
import chat.peptide.api.PeptideJson
import chat.peptide.api.api
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

@Serializable
data class LoginNegotiation(
    val email: String,
    val password: String,

    @SerialName("friendly_name")
    val friendlyName: String,
    val captcha: String? = null
)

@Serializable
data class LoginMfaAmendmentTotpCode(
    @SerialName("mfa_ticket")
    val mfaTicket: String,

    @SerialName("mfa_response")
    val mfaResponse: MfaResponseTotpCode,

    @SerialName("friendly_name")
    val friendlyName: String
)

@Serializable
data class LoginMfaAmendmentRecoveryCode(
    @SerialName("mfa_ticket")
    val mfaTicket: String,

    @SerialName("mfa_response")
    val mfaResponse: MfaResponseRecoveryCode,

    @SerialName("friendly_name")
    val friendlyName: String
)

@Serializable
data class MfaResponseRecoveryCode(
    @SerialName("recovery_code")
    val recoveryCode: String
)

@Serializable
data class MfaResponseTotpCode(
    @SerialName("totp_code")
    val totpCode: String
)

@Serializable
data class MfaLoginSpec(
    val result: String,
    val ticket: String,

    @SerialName("allowed_methods")
    val allowedMethods: List<String>
)

@Serializable
data class MfaCheck(
    val result: String
)

@Serializable
data class WebPushData(
    val endpoint: String,

    @SerialName("p256dh")
    val p256diffieHellman: String,
    val auth: String
)

@Serializable
data class UserHints(
    val result: String,

    @SerialName("_id")
    val id: String,

    @SerialName("user_id")
    val userId: String,
    val token: String,
    val name: String,
    val subscription: WebPushData? = null
)

data class EmailPasswordAssessment(
    val proceedMfa: Boolean = false,
    val mfaSpec: MfaLoginSpec? = null,
    val firstUserHints: UserHints? = null,
    val error: PeptideError? = null
)

suspend fun negotiateAuthentication(email: String, password: String): EmailPasswordAssessment {
    val sessionName = friendlySessionName()

    val response: HttpResponse = PeptideHttp.post("/auth/session/login".api()) {
        contentType(ContentType.Application.Json)
        headers.append("x-no-retry", "true")
        setBody(LoginNegotiation(email, password, sessionName, null))
    }

    val responseContent = response.bodyAsText()
    Log.d("Peptide", "negotiateAuthentication: $responseContent")

    try {
        val error = PeptideJson.decodeFromString(PeptideError.serializer(), responseContent)
        return EmailPasswordAssessment(error = error)
    } catch (e: SerializationException) {
        // Not an error
    }

    if (response.status == HttpStatusCode.InternalServerError) {
        return EmailPasswordAssessment(
            error = PeptideError(
                "InternalServerError"
            )
        )
    }

    val responseJson = PeptideJson.decodeFromString(MfaCheck.serializer(), responseContent)

    return when (responseJson.result) {
        "Success" -> EmailPasswordAssessment(
            firstUserHints = PeptideJson.decodeFromString(UserHints.serializer(), responseContent)
        )

        "MFA" -> EmailPasswordAssessment(
            proceedMfa = true,
            mfaSpec = PeptideJson.decodeFromString(MfaLoginSpec.serializer(), responseContent)
        )

        else -> throw Exception("Unknown result: ${responseJson.result}")
    }
}

suspend fun authenticateWithMfaTotpCode(
    mfaTicket: String,
    mfaResponse: MfaResponseTotpCode
): EmailPasswordAssessment {
    val response: HttpResponse = PeptideHttp.post("/auth/session/login".api()) {
        contentType(ContentType.Application.Json)
        setBody(LoginMfaAmendmentTotpCode(mfaTicket, mfaResponse, friendlySessionName()))
    }

    try {
        val error = PeptideJson.decodeFromString(PeptideError.serializer(), response.bodyAsText())
        return EmailPasswordAssessment(error = error)
    } catch (e: SerializationException) {
        // Not an error
    }

    val responseContent = response.bodyAsText()
    Log.d("Peptide", "authenticateWithMfaTotpCode: $responseContent")

    return EmailPasswordAssessment(
        firstUserHints = PeptideJson.decodeFromString(UserHints.serializer(), responseContent)
    )
}

suspend fun authenticateWithMfaRecoveryCode(
    mfaTicket: String,
    mfaResponse: MfaResponseRecoveryCode
): EmailPasswordAssessment {
    val response: HttpResponse = PeptideHttp.post("/auth/session/login".api()) {
        contentType(ContentType.Application.Json)
        setBody(LoginMfaAmendmentRecoveryCode(mfaTicket, mfaResponse, friendlySessionName()))
    }

    try {
        val error = PeptideJson.decodeFromString(PeptideError.serializer(), response.bodyAsText())
        return EmailPasswordAssessment(error = error)
    } catch (e: SerializationException) {
        // Not an error
    }

    val responseContent = response.bodyAsText()
    Log.d("Peptide", "authenticateWithMfaRecoveryCode: $responseContent")

    return EmailPasswordAssessment(
        firstUserHints = PeptideJson.decodeFromString(UserHints.serializer(), responseContent)
    )
}

fun friendlySessionName(): String {
    return "Peptide Android on ${Build.MANUFACTURER} ${Build.MODEL}"
}
