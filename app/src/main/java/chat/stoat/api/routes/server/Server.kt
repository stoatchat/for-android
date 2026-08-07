package chat.stoat.api.routes.server

import chat.stoat.api.StoatAPI
import chat.stoat.api.StoatAPIError
import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatJson
import chat.stoat.api.api
import chat.stoat.core.model.schemas.Member
import chat.stoat.core.model.schemas.ServerWithChannelObjects
import chat.stoat.core.model.schemas.User
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

@Serializable
data class FetchMembersResponse(
    val members: List<Member>,
    val users: List<User>
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

suspend fun leaveOrDeleteServer(serverId: String, leaveSilently: Boolean = false) {
    val response = StoatHttp.delete("/servers/$serverId".api()) {
        parameter("leave_silently", leaveSilently)
    }

    if (!response.status.isSuccess()) {
        val responseContent = response.bodyAsText()
        val errorType = runCatching {
            StoatJson.decodeFromString(StoatAPIError.serializer(), responseContent).type
        }.getOrNull()

        throw Exception(errorType ?: "Request failed (${response.status.value})")
    }
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
