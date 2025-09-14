package chat.zekochat.api.routes.invites

import chat.zekochat.api.PeptideError
import chat.zekochat.api.PeptideHttp
import chat.zekochat.api.PeptideJson
import chat.zekochat.api.api
import chat.zekochat.api.schemas.Invite
import chat.zekochat.api.schemas.InviteJoined
import chat.zekochat.api.schemas.RsResult
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerializationException

suspend fun fetchInviteByCode(code: String): RsResult<Invite, PeptideError> {
    val response = PeptideHttp.get("/invites/$code".api())
        .bodyAsText()

    try {
        val error = PeptideJson.decodeFromString(PeptideError.serializer(), response)
        if (error.type != "Server") return RsResult.err(error)
    } catch (e: SerializationException) {
        // Not an error
    }

    val invite = PeptideJson.decodeFromString(Invite.serializer(), response)
    return RsResult.ok(invite)
}

suspend fun joinInviteByCode(code: String): RsResult<InviteJoined, PeptideError> {
    val response = PeptideHttp.post("/invites/$code".api())
        .bodyAsText()

    try {
        val error = PeptideJson.decodeFromString(PeptideError.serializer(), response)
        if (error.type != "Server") return RsResult.err(error)
    } catch (e: SerializationException) {
        // Not an error
    }

    val invite = PeptideJson.decodeFromString(InviteJoined.serializer(), response)
    return RsResult.ok(invite)
}
