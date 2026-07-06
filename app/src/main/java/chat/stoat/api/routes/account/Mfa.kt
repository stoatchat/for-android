package chat.stoat.api.routes.account

import chat.stoat.api.StoatAPIError
import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatJson
import chat.stoat.api.api
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val MFA_TICKET_HEADER_NAME = "x-mfa-ticket"

@Serializable
data class MfaTicket(
    @SerialName("_id") val id: String,
    @SerialName("account_id") val accountId: String,
    val token: String,
    val validated: Boolean = false,
    val authorised: Boolean = false,
    @SerialName("last_totp_code") val lastTotpCode: String? = null
)

@Serializable
private data class TotpSecretResponse(val secret: String)

private suspend fun HttpResponse.ensureSuccess(what: String) {
    if (status.isSuccess()) return

    val body = bodyAsText()
    runCatching { StoatJson.decodeFromString(StoatAPIError.serializer(), body) }
        .onSuccess { throw Exception(it.type) }

    throw Exception("Failed to $what: $body")
}

suspend fun fetchMfaMethods(): List<String> {
    val res = StoatHttp.get("/auth/mfa/methods".api())
    res.ensureSuccess("fetch MFA methods")
    return StoatJson.decodeFromString(res.bodyAsText())
}

suspend fun createMfaTicket(response: MfaResponse): MfaTicket {
    val res = StoatHttp.put("/auth/mfa/ticket".api()) {
        setBody(response)
        contentType(ContentType.Application.Json)
    }
    res.ensureSuccess("create MFA ticket")
    return StoatJson.decodeFromString(res.bodyAsText())
}

suspend fun disableTotp(mfaTicketToken: String) {
    StoatHttp.delete("/auth/mfa/totp".api()) {
        header(MFA_TICKET_HEADER_NAME, mfaTicketToken)
    }.ensureSuccess("disable TOTP")
}

suspend fun generateTotpSecret(mfaTicketToken: String): String {
    val res = StoatHttp.post("/auth/mfa/totp".api()) {
        header(MFA_TICKET_HEADER_NAME, mfaTicketToken)
    }
    res.ensureSuccess("generate TOTP secret")
    return StoatJson.decodeFromString(TotpSecretResponse.serializer(), res.bodyAsText()).secret
}

suspend fun enableTotp(totpCode: String) {
    StoatHttp.put("/auth/mfa/totp".api()) {
        setBody(MfaResponseTotpCode(totpCode))
        contentType(ContentType.Application.Json)
    }.ensureSuccess("enable TOTP")
}

suspend fun fetchRecoveryCodes(mfaTicketToken: String): List<String> {
    val res = StoatHttp.post("/auth/mfa/recovery".api()) {
        header(MFA_TICKET_HEADER_NAME, mfaTicketToken)
    }
    res.ensureSuccess("fetch recovery codes")
    return StoatJson.decodeFromString(res.bodyAsText())
}

suspend fun regenerateRecoveryCodes(mfaTicketToken: String): List<String> {
    val res = StoatHttp.patch("/auth/mfa/recovery".api()) {
        header(MFA_TICKET_HEADER_NAME, mfaTicketToken)
    }
    res.ensureSuccess("regenerate recovery codes")
    return StoatJson.decodeFromString(res.bodyAsText())
}
