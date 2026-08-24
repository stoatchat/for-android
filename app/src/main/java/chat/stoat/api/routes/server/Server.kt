package chat.stoat.api.routes.server

import chat.stoat.api.StoatAPI
import chat.stoat.api.StoatAPIError
import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatJson
import chat.stoat.api.api
import chat.stoat.api.apiError
import chat.stoat.core.model.schemas.BanListResult
import chat.stoat.core.model.schemas.Member
import chat.stoat.core.model.schemas.Server
import chat.stoat.core.model.schemas.ServerWithChannelObjects
import chat.stoat.core.model.schemas.User
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class FetchMembersResponse(
    val members: List<Member>,
    val users: List<User>
)

@Serializable
private data class BanMemberBody(
    val reason: String? = null,
    @SerialName("delete_message_seconds")
    val deleteMessageSeconds: Long = 0,
)

@Serializable
private data class EditMemberBody(
    val nickname: String? = null,
    val pronouns: String? = null,
    val avatar: String? = null,
    val timeout: String? = null,
    val remove: List<String> = emptyList(),
)

suspend fun ackServer(serverId: String) {
    StoatHttp.put("/servers/$serverId/ack".api())
}

suspend fun fetchMembers(
    serverId: String,
    includeOffline: Boolean = false,
    pure: Boolean = false
): FetchMembersResponse {
    val response = StoatHttp.get("/servers/$serverId/members".api()) {
        parameter("exclude_offline", !includeOffline)
    }

    val responseContent = response.bodyAsText()

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), responseContent)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val membersResponse =
        StoatJson.decodeFromString(FetchMembersResponse.serializer(), responseContent)

    if (pure) {
        return membersResponse
    }

    membersResponse.members.forEach { member ->
        if (!StoatAPI.members.hasMember(serverId, member.id!!.user)) {
            StoatAPI.members.setMember(serverId, member)
        }
    }

    membersResponse.users.forEach { user ->
        user.id?.let { StoatAPI.userCache.putIfAbsent(it, user) }
    }

    return membersResponse
}

suspend fun fetchMember(serverId: String, userId: String, pure: Boolean = false): Member {
    val response = StoatHttp.get("/servers/$serverId/members/$userId".api())

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), response.bodyAsText())
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val member = StoatJson.decodeFromString(Member.serializer(), response.bodyAsText())

    if (!pure) {
        member.id?.let {
            if (!StoatAPI.members.hasMember(serverId, it.user)) {
                StoatAPI.members.setMember(serverId, member)
            }
        }
    }

    return member
}

suspend fun kickMember(serverId: String, userId: String) {
    val response = StoatHttp.delete("/servers/$serverId/members/$userId".api())
    val responseContent = response.bodyAsText()

    if (!response.status.isSuccess()) {
        throw Exception(apiError(responseContent, response.status.value))
    }

    StoatAPI.members.removeMember(serverId, userId)
}

suspend fun banMember(
    serverId: String,
    userId: String,
    reason: String?,
    deleteMessageSeconds: Long,
) {
    val body = BanMemberBody(
        reason = reason?.trim()?.takeIf(String::isNotEmpty),
        deleteMessageSeconds = deleteMessageSeconds,
    )
    val response = StoatHttp.put("/servers/$serverId/bans/$userId".api()) {
        contentType(ContentType.Application.Json)
        setBody(StoatJson.encodeToString(BanMemberBody.serializer(), body))
    }
    val responseContent = response.bodyAsText()

    if (!response.status.isSuccess()) {
        throw Exception(apiError(responseContent, response.status.value))
    }

    StoatAPI.members.removeMember(serverId, userId)
}

suspend fun fetchServerBans(serverId: String): BanListResult {
    val response = StoatHttp.get("/servers/$serverId/bans".api())
    val responseContent = response.bodyAsText()

    if (!response.status.isSuccess()) {
        throw Exception(apiError(responseContent, response.status.value))
    }

    return StoatJson.decodeFromString(BanListResult.serializer(), responseContent)
}

suspend fun unbanMember(serverId: String, userId: String) {
    val response = StoatHttp.delete("/servers/$serverId/bans/$userId".api())

    if (!response.status.isSuccess()) {
        throw Exception(apiError(response.bodyAsText(), response.status.value))
    }
}

suspend fun setMemberTimeout(serverId: String, userId: String, timeout: String?): Member {
    return patchMember(
        serverId = serverId,
        userId = userId,
        timeout = timeout,
        remove = if (timeout == null) listOf("Timeout") else emptyList(),
    )
}

suspend fun patchMemberIdentity(
    serverId: String,
    userId: String,
    nickname: String? = null,
    pronouns: String? = null,
    avatar: String? = null,
    remove: List<String> = emptyList(),
): Member {
    return patchMember(
        serverId = serverId,
        userId = userId,
        nickname = nickname,
        pronouns = pronouns,
        avatar = avatar,
        remove = remove,
    )
}

private suspend fun patchMember(
    serverId: String,
    userId: String,
    nickname: String? = null,
    pronouns: String? = null,
    avatar: String? = null,
    timeout: String? = null,
    remove: List<String> = emptyList(),
): Member {
    val body = EditMemberBody(
        nickname = nickname,
        pronouns = pronouns,
        avatar = avatar,
        timeout = timeout,
        remove = remove,
    )
    val response = StoatHttp.patch("/servers/$serverId/members/$userId".api()) {
        contentType(ContentType.Application.Json)
        setBody(StoatJson.encodeToString(EditMemberBody.serializer(), body))
    }
    val responseContent = response.bodyAsText()

    if (!response.status.isSuccess()) {
        throw Exception(apiError(responseContent, response.status.value))
    }

    return StoatJson.decodeFromString(Member.serializer(), responseContent).also {
        StoatAPI.members.setMember(serverId, it)
    }
}

suspend fun leaveOrDeleteServer(serverId: String, leaveSilently: Boolean = false) {
    val response = StoatHttp.delete("/servers/$serverId".api()) {
        parameter("leave_silently", leaveSilently)
    }

    if (!response.status.isSuccess()) {
        val responseContent = response.bodyAsText()
        throw Exception(apiError(responseContent, response.status.value))
    }
}

suspend fun patchServer(
    serverId: String,
    name: String? = null,
    description: String? = null,
    icon: String? = null,
    banner: String? = null,
    systemMessages: Map<String, String>? = null,
    remove: List<String> = emptyList(),
): Server {
    val body = mutableMapOf<String, JsonElement>()

    name?.let { body["name"] = JsonPrimitive(it) }
    description?.let { body["description"] = JsonPrimitive(it) }
    icon?.let { body["icon"] = JsonPrimitive(it) }
    banner?.let { body["banner"] = JsonPrimitive(it) }
    systemMessages?.let { messages ->
        body["system_messages"] = JsonObject(
            messages.mapValues { (_, channelId) -> JsonPrimitive(channelId) }
        )
    }
    if (remove.isNotEmpty()) {
        body["remove"] = StoatJson.encodeToJsonElement(
            ListSerializer(String.serializer()),
            remove,
        )
    }

    val response = StoatHttp.patch("/servers/$serverId".api()) {
        contentType(ContentType.Application.Json)
        setBody(
            StoatJson.encodeToString(
                MapSerializer(String.serializer(), JsonElement.serializer()),
                body,
            )
        )
    }
    val responseContent = response.bodyAsText()

    if (!response.status.isSuccess()) {
        throw Exception(apiError(responseContent, response.status.value))
    }

    val server = StoatJson.decodeFromString(Server.serializer(), responseContent)
    StoatAPI.serverCache[serverId] = server
    return server
}

@Serializable
data class ServerCreationBody(
    val name: String,
    val description: String? = null,
    val nsfw: Boolean = false
)

suspend fun createServer(
    name: String,
    description: String = "",
    nsfw: Boolean = false
): ServerWithChannelObjects {
    val body = ServerCreationBody(name, description, nsfw)

    val response = StoatHttp.post("/servers/create".api()) {
        setBody(StoatJson.encodeToString(ServerCreationBody.serializer(), body))
    }

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), response.bodyAsText())
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    return StoatJson.decodeFromString(ServerWithChannelObjects.serializer(), response.bodyAsText())
}
