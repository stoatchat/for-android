package chat.peptide.api.routes.auth

import chat.peptide.api.RevoltHttp
import chat.peptide.api.RevoltJson
import chat.peptide.api.api
import chat.peptide.api.schemas.Session
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.builtins.ListSerializer

suspend fun fetchAllSessions(): List<Session> {
    val response = RevoltHttp.get("/auth/session/all".api())
        .bodyAsText()

    return RevoltJson.decodeFromString(
        ListSerializer(Session.serializer()),
        response
    )
}

suspend fun logoutSessionById(id: String) {
    RevoltHttp.delete("/auth/session/$id".api())
}

suspend fun logoutAllSessions(includingSelf: Boolean = false) {
    RevoltHttp.delete("/auth/session/all".api()) {
        parameter("revoke_self", includingSelf)
    }
}
