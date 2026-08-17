package chat.stoat.api.routes.invites

import chat.stoat.api.StoatAPIError
import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatJson
import chat.stoat.api.api
import chat.stoat.api.apiError
import chat.stoat.core.model.schemas.Invite
import chat.stoat.core.model.schemas.InviteJoined
import chat.stoat.core.model.schemas.ServerInvite
import chat.stoat.core.model.util.RsResult
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer

suspend fun fetchServerInvites(serverId: String): List<ServerInvite> {
    val response = StoatHttp.get("/servers/$serverId/invites".api())
    val responseContent = response.bodyAsText()

    if (!response.status.isSuccess()) {
        throw Exception(apiError(responseContent, response.status.value))
    }

    return StoatJson.decodeFromString(
        ListSerializer(ServerInvite.serializer()),
        responseContent,
    )
}

suspend fun deleteInvite(code: String) {
    val response = StoatHttp.delete("/invites/$code".api())

    if (!response.status.isSuccess()) {
        throw Exception(apiError(response.bodyAsText(), response.status.value))
    }
}

suspend fun fetchInviteByCode(code: String): RsResult<Invite, StoatAPIError> {
    val response = StoatHttp.get("/invites/$code".api())
        .bodyAsText()

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), response)
        if (error.type != "Server") return RsResult.err(error)
    } catch (e: SerializationException) {
        // Not an error
    }

    val invite = StoatJson.decodeFromString(Invite.serializer(), response)
    return RsResult.ok(invite)
}

suspend fun joinInviteByCode(code: String): RsResult<InviteJoined, StoatAPIError> {
    val response = StoatHttp.post("/invites/$code".api())
        .bodyAsText()

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), response)
        if (error.type != "Server") return RsResult.err(error)
    } catch (e: SerializationException) {
        // Not an error
    }

    val invite = StoatJson.decodeFromString(InviteJoined.serializer(), response)
    return RsResult.ok(invite)
}
