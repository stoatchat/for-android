package chat.stoat.api.routes.channel

import chat.stoat.api.StoatAPIError
import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatJson
import chat.stoat.api.api
import chat.stoat.core.model.schemas.Channel
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

const val MAX_PEOPLE_IN_GROUP = 50
const val MAX_ADDABLE_PEOPLE_IN_GROUP = MAX_PEOPLE_IN_GROUP - 1

@Serializable
data class CreateGroupDMBody(
    val name: String,
    val users: List<String>
)

suspend fun createGroupDM(name: String, members: List<String>): Channel {
    if (members.size > MAX_ADDABLE_PEOPLE_IN_GROUP) {
        throw Exception("Too many members, maximum is $MAX_ADDABLE_PEOPLE_IN_GROUP")
    }

    val response = StoatHttp.post("/channels/create".api()) {
        contentType(ContentType.Application.Json)
        setBody(CreateGroupDMBody(name, members))
    }.bodyAsText()

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), response)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    return StoatJson.decodeFromString(Channel.serializer(), response)
}

suspend fun removeMember(channelId: String, userId: String) {
    val response = StoatHttp.delete("/channels/$channelId/recipients/$userId".api())

    if (!response.status.isSuccess()) {
        throw Error(response.status.toString())
    }
}

suspend fun addMember(channelId: String, userId: String) {
    val response = StoatHttp.put("/channels/$channelId/recipients/$userId".api())

    if (!response.status.isSuccess()) {
        throw Error(response.status.toString())
    }
}