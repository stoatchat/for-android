package chat.peptide.api.routes.server

import chat.peptide.api.RevoltAPI
import chat.peptide.api.RevoltError
import chat.peptide.api.RevoltHttp
import chat.peptide.api.RevoltJson
import chat.peptide.api.api
import chat.peptide.api.schemas.Member
import chat.peptide.api.schemas.ServerWithChannelObjects
import chat.peptide.api.schemas.User
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
    RevoltHttp.put("/servers/$serverId/ack".api())
}

suspend fun fetchMembers(
    serverId: String,
    includeOffline: Boolean = false,
    pure: Boolean = false
): FetchMembersResponse {
    val response = RevoltHttp.get("/servers/$serverId/members".api()) {
        parameter("exclude_offline", !includeOffline)
    }

    val responseContent = response.bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), responseContent)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val membersResponse =
        RevoltJson.decodeFromString(FetchMembersResponse.serializer(), responseContent)

    if (pure) {
        return membersResponse
    }

    membersResponse.members.forEach { member ->
        if (!RevoltAPI.members.hasMember(serverId, member.id!!.user)) {
            RevoltAPI.members.setMember(serverId, member)
        }
    }

    membersResponse.users.forEach { user ->
        user.id?.let { RevoltAPI.userCache.putIfAbsent(it, user) }
    }

    return membersResponse
}

suspend fun fetchMember(serverId: String, userId: String, pure: Boolean = false): Member {
    val response = RevoltHttp.get("/servers/$serverId/members/$userId".api())

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response.bodyAsText())
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val member = RevoltJson.decodeFromString(Member.serializer(), response.bodyAsText())

    if (!pure) {
        member.id?.let {
            if (!RevoltAPI.members.hasMember(serverId, it.user)) {
                RevoltAPI.members.setMember(serverId, member)
            }
        }
    }

    return member
}

suspend fun leaveOrDeleteServer(serverId: String, leaveSilently: Boolean = false) {
    RevoltHttp.delete("/servers/$serverId".api()) {
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

    val response = RevoltHttp.post("/servers/create".api()) {
        setBody(RevoltJson.encodeToString(ServerCreationBody.serializer(), body))
    }

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response.bodyAsText())
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    return RevoltJson.decodeFromString(ServerWithChannelObjects.serializer(), response.bodyAsText())
}