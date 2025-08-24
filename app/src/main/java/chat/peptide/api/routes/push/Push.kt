package chat.peptide.api.routes.push

import chat.peptide.api.PeptideHttp
import chat.peptide.api.routes.account.WebPushData
import chat.peptide.api.api
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

suspend fun subscribePush(
    endpoint: String = "fcm",
    auth: String,
    p256diffieHellman: String? = null,
) {
    val data = WebPushData(
        endpoint = endpoint,
        p256diffieHellman = p256diffieHellman ?: "",
        auth = auth
    )

    PeptideHttp.post("/push/subscribe".api()) {
        setBody(data)
        contentType(ContentType.Application.Json)
    }
}