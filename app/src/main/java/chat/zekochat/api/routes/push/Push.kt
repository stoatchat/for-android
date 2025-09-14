package chat.zekochat.api.routes.push

import chat.zekochat.api.PeptideHttp
import chat.zekochat.api.routes.account.WebPushData
import chat.zekochat.api.api
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