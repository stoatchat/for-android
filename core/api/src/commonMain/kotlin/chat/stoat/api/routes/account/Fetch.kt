package chat.stoat.api.routes.account

import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatJson
import chat.stoat.api.api
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountResponse(
    @SerialName("_id") val id: String,
    val email: String,
)

suspend fun fetchAccount(): AccountResponse {
    val response = StoatHttp.get("/auth/account".api())
        .bodyAsText()

    return StoatJson.decodeFromString(response)
}

@Serializable
data class MfaSettings(
    @SerialName("email_otp") val emailOtp: Boolean? = null,
    @SerialName("trusted_handover") val trustedHandover: Boolean? = null,
    @SerialName("email_mfa") val emailMfa: Boolean? = null,
    @SerialName("totp_mfa") val totpMfa: Boolean? = null,
    @SerialName("security_key_mfa") val securityKeyMfa: Boolean? = null,
    @SerialName("recovery_active") val recoveryActive: Boolean? = null,
)

suspend fun fetchMfaSettings(): MfaSettings {
    val response = StoatHttp.get("/auth/mfa".api())
        .bodyAsText()

    return StoatJson.decodeFromString(response)
}