package chat.stoat.api.routes.account

import android.os.Build
import android.util.Log
import chat.stoat.api.StoatAPIError
import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatJson
import chat.stoat.api.api
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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

/**
 * To add a new MFA method, add a variant here plus its branch in [MfaResponseSerializer]
 * then describe the UI in [chat.stoat.composables.mfa.MfaMethod].
 */
@Serializable(with = MfaResponseSerializer::class)
sealed interface MfaResponse

@Serializable
data class MfaResponsePassword(
    val password: String
) : MfaResponse

@Serializable
data class MfaResponseRecoveryCode(
    @SerialName("recovery_code")
    val recoveryCode: String
) : MfaResponse

@Serializable
data class MfaResponseTotpCode(
    @SerialName("totp_code")
    val totpCode: String
) : MfaResponse

object MfaResponseSerializer : KSerializer<MfaResponse> {
    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("MfaResponse", PolymorphicKind.SEALED)

    override fun serialize(encoder: Encoder, value: MfaResponse) = when (value) {
        is MfaResponsePassword ->
            encoder.encodeSerializableValue(MfaResponsePassword.serializer(), value)

        is MfaResponseRecoveryCode ->
            encoder.encodeSerializableValue(MfaResponseRecoveryCode.serializer(), value)

        is MfaResponseTotpCode ->
            encoder.encodeSerializableValue(MfaResponseTotpCode.serializer(), value)
    }

    override fun deserialize(decoder: Decoder): MfaResponse =
        throw UnsupportedOperationException("MfaResponse is only ever sent to the API")
}

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
    val error: StoatAPIError? = null
)

suspend fun negotiateAuthentication(email: String, password: String): EmailPasswordAssessment {
    val sessionName = friendlySessionName()

    val response: HttpResponse = StoatHttp.post("/auth/session/login".api()) {
        contentType(ContentType.Application.Json)
        setBody(LoginNegotiation(email, password, sessionName, null))
    }

    val responseContent = response.bodyAsText()
    Log.d("Stoat", "negotiateAuthentication: $responseContent")

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), responseContent)
        return EmailPasswordAssessment(error = error)
    } catch (e: SerializationException) {
        // Not an error
    }

    if (response.status == HttpStatusCode.InternalServerError) {
        return EmailPasswordAssessment(
            error = StoatAPIError(
                "InternalServerError"
            )
        )
    }

    val responseJson = StoatJson.decodeFromString(MfaCheck.serializer(), responseContent)

    return when (responseJson.result) {
        "Success" -> EmailPasswordAssessment(
            firstUserHints = StoatJson.decodeFromString(UserHints.serializer(), responseContent)
        )

        "MFA" -> EmailPasswordAssessment(
            proceedMfa = true,
            mfaSpec = StoatJson.decodeFromString(MfaLoginSpec.serializer(), responseContent)
        )

        else -> throw Exception("Unknown result: ${responseJson.result}")
    }
}

suspend fun authenticateWithMfaTotpCode(
    mfaTicket: String,
    mfaResponse: MfaResponseTotpCode
): EmailPasswordAssessment {
    val response: HttpResponse = StoatHttp.post("/auth/session/login".api()) {
        contentType(ContentType.Application.Json)
        setBody(LoginMfaAmendmentTotpCode(mfaTicket, mfaResponse, friendlySessionName()))
    }

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), response.bodyAsText())
        return EmailPasswordAssessment(error = error)
    } catch (e: SerializationException) {
        // Not an error
    }

    val responseContent = response.bodyAsText()
    Log.d("Stoat", "authenticateWithMfaTotpCode: $responseContent")

    return EmailPasswordAssessment(
        firstUserHints = StoatJson.decodeFromString(UserHints.serializer(), responseContent)
    )
}

suspend fun authenticateWithMfaRecoveryCode(
    mfaTicket: String,
    mfaResponse: MfaResponseRecoveryCode
): EmailPasswordAssessment {
    val response: HttpResponse = StoatHttp.post("/auth/session/login".api()) {
        contentType(ContentType.Application.Json)
        setBody(LoginMfaAmendmentRecoveryCode(mfaTicket, mfaResponse, friendlySessionName()))
    }

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), response.bodyAsText())
        return EmailPasswordAssessment(error = error)
    } catch (e: SerializationException) {
        // Not an error
    }

    val responseContent = response.bodyAsText()
    Log.d("Stoat", "authenticateWithMfaRecoveryCode: $responseContent")

    return EmailPasswordAssessment(
        firstUserHints = StoatJson.decodeFromString(UserHints.serializer(), responseContent)
    )
}

fun friendlySessionName(): String {
    return "Stoat for Android on ${Build.MANUFACTURER} ${Build.MODEL}"
}
