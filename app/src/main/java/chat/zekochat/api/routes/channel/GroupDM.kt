package chat.zekochat.api.routes.channel

import chat.zekochat.api.PeptideError
import chat.zekochat.api.PeptideHttp
import chat.zekochat.api.PeptideJson
import chat.zekochat.api.api
import chat.zekochat.api.schemas.Channel
import chat.zekochat.screens.create.MAX_ADDABLE_PEOPLE_IN_GROUP
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

@Serializable
data class CreateGroupDMBody(
    val name: String,
    val users: List<String>
)

suspend fun createGroupDM(name: String, members: List<String>): Channel {
    if (members.size > MAX_ADDABLE_PEOPLE_IN_GROUP) {
        throw Exception("Too many members, maximum is $MAX_ADDABLE_PEOPLE_IN_GROUP")
    }

    val response = PeptideHttp.post("/channels/create".api()) {
        contentType(ContentType.Application.Json)
        setBody(CreateGroupDMBody(name, members))
    }.bodyAsText()

    try {
        val error = PeptideJson.decodeFromString(PeptideError.serializer(), response)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    return PeptideJson.decodeFromString(Channel.serializer(), response)
}

suspend fun removeMember(channelId: String, userId: String) {
    val response = PeptideHttp.delete("/channels/$channelId/recipients/$userId".api())

    if (!response.status.isSuccess()) {
        throw Error(response.status.toString())
    }
}

suspend fun addMember(channelId: String, userId: String) {
    val response = PeptideHttp.put("/channels/$channelId/recipients/$userId".api())

    if (!response.status.isSuccess()) {
        throw Error(response.status.toString())
    }
}