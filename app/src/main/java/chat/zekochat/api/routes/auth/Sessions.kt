package chat.zekochat.api.routes.auth

import chat.zekochat.api.PeptideHttp
import chat.zekochat.api.PeptideJson
import chat.zekochat.api.api
import chat.zekochat.api.schemas.Session
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.builtins.ListSerializer

suspend fun fetchAllSessions(): List<Session> {
    val response = PeptideHttp.get("/auth/session/all".api())
        .bodyAsText()

    return PeptideJson.decodeFromString(
        ListSerializer(Session.serializer()),
        response
    )
}

suspend fun logoutSessionById(id: String) {
    PeptideHttp.delete("/auth/session/$id".api())
}

suspend fun logoutAllSessions(includingSelf: Boolean = false) {
    PeptideHttp.delete("/auth/session/all".api()) {
        parameter("revoke_self", includingSelf)
    }
}
