package chat.zekochat.api.routes.server

import chat.zekochat.api.PeptideAPI
import chat.zekochat.api.PeptideError
import chat.zekochat.api.PeptideHttp
import chat.zekochat.api.PeptideJson
import chat.zekochat.api.api
import chat.zekochat.api.schemas.Member
import chat.zekochat.api.schemas.ServerWithChannelObjects
import chat.zekochat.api.schemas.User
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

@Serializable
data class FetchMembersResponse(
    val members: List<Member>,
    val users: List<User>
)

suspend fun ackServer(serverId: String) {
    PeptideHttp.put("/servers/$serverId/ack".api())
}

suspend fun fetchMembers(
    serverId: String,
    includeOffline: Boolean = false,
    pure: Boolean = false
): FetchMembersResponse {
    val response = PeptideHttp.get("/servers/$serverId/members".api()) {
        parameter("exclude_offline", !includeOffline)
    }

    val responseContent = response.bodyAsText()

    try {
        val error = PeptideJson.decodeFromString(PeptideError.serializer(), responseContent)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val membersResponse =
        PeptideJson.decodeFromString(FetchMembersResponse.serializer(), responseContent)

    if (pure) {
        return membersResponse
    }

    membersResponse.members.forEach { member ->
        if (!PeptideAPI.members.hasMember(serverId, member.id!!.user)) {
            PeptideAPI.members.setMember(serverId, member)
        }
    }

    membersResponse.users.forEach { user ->
        user.id?.let { PeptideAPI.userCache.putIfAbsent(it, user) }
    }

    return membersResponse
}

suspend fun fetchMember(serverId: String, userId: String, pure: Boolean = false): Member {
    val response = PeptideHttp.get("/servers/$serverId/members/$userId".api())

    try {
        val error = PeptideJson.decodeFromString(PeptideError.serializer(), response.bodyAsText())
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val member = PeptideJson.decodeFromString(Member.serializer(), response.bodyAsText())

    if (!pure) {
        member.id?.let {
            if (!PeptideAPI.members.hasMember(serverId, it.user)) {
                PeptideAPI.members.setMember(serverId, member)
            }
        }
    }

    return member
}

suspend fun leaveOrDeleteServer(serverId: String, leaveSilently: Boolean = false) {
    PeptideHttp.delete("/servers/$serverId".api()) {
        parameter("leave_silently", leaveSilently)
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

    val response = PeptideHttp.post("/servers/create".api()) {
        setBody(PeptideJson.encodeToString(ServerCreationBody.serializer(), body))
    }

    try {
        val error = PeptideJson.decodeFromString(PeptideError.serializer(), response.bodyAsText())
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    return PeptideJson.decodeFromString(ServerWithChannelObjects.serializer(), response.bodyAsText())
}