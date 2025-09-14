package chat.zekochat.api.routes.channel

import chat.zekochat.api.PeptideAPI
import chat.zekochat.api.PeptideError
import chat.zekochat.api.PeptideHttp
import chat.zekochat.api.PeptideJson
import chat.zekochat.api.api
import chat.zekochat.api.internals.ULID
import chat.zekochat.api.schemas.Channel
import chat.zekochat.api.schemas.Message
import chat.zekochat.api.schemas.MessagesInChannel
import chat.zekochat.api.schemas.User
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement

suspend fun fetchMessagesFromChannel(
    channelId: String,
    limit: Int = 50,
    includeUsers: Boolean = false,
    before: String? = null,
    after: String? = null,
    nearby: String? = null,
    sort: String? = null
): MessagesInChannel {
    val response = PeptideHttp.get("/channels/$channelId/messages".api()) {
        parameter("limit", limit)
        parameter("include_users", includeUsers)

        if (before != null) parameter("before", before)
        if (after != null) parameter("after", after)
        if (nearby != null) parameter("nearby", nearby)
        if (sort != null) parameter("sort", sort)
    }
        .bodyAsText()

    if (includeUsers) {
        return PeptideJson.decodeFromString(
            MessagesInChannel.serializer(),
            response
        )
    } else {
        val messages = PeptideJson.decodeFromString(
            ListSerializer(Message.serializer()),
            response
        )

        return MessagesInChannel(
            messages = messages,
            users = emptyList(),
            members = emptyList()
        )
    }
}

@kotlinx.serialization.Serializable
data class SendMessageReply(
    val id: String,
    val mention: Boolean
)

@kotlinx.serialization.Serializable
data class SendMessageBody(
    val content: String,
    val nonce: String = ULID.makeNext(),
    val replies: List<SendMessageReply> = emptyList(),
    val attachments: List<String>?
)

@kotlinx.serialization.Serializable
data class EditMessageBody(
    val content: String?
)

@kotlinx.serialization.Serializable
data class CreateInviteResponse(
    val type: String,
    @SerialName("_id")
    val id: String,
    val server: String,
    val creator: String,
    val channel: String,
)

suspend fun sendMessage(
    channelId: String,
    content: String,
    nonce: String = ULID.makeNext(),
    replies: List<SendMessageReply>? = null,
    attachments: List<String>? = null,
    idempotencyKey: String = ULID.makeNext()
): String {
    val response = PeptideHttp.post("/channels/$channelId/messages".api()) {
        contentType(ContentType.Application.Json)
        setBody(
            SendMessageBody(
                content = content,
                nonce = nonce,
                replies = replies ?: emptyList(),
                attachments = attachments
            )
        )
        header("Idempotency-Key", idempotencyKey)
    }
        .bodyAsText()

    return response
}

suspend fun editMessage(channelId: String, messageId: String, newContent: String? = null) {
    val response = PeptideHttp.patch("/channels/$channelId/messages/$messageId".api()) {
        contentType(ContentType.Application.Json)
        setBody(
            EditMessageBody(
                content = newContent
            )
        )
    }
        .bodyAsText()

    try {
        val error = PeptideJson.decodeFromString(PeptideError.serializer(), response)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }
}

suspend fun deleteMessage(channelId: String, messageId: String) {
    PeptideHttp.delete("/channels/$channelId/messages/$messageId".api())
}

suspend fun ackChannel(channelId: String, messageId: String = ULID.makeNext()) {
    PeptideHttp.put("/channels/$channelId/ack/$messageId".api())
}

suspend fun fetchSingleChannel(channelId: String): Channel {
    val response = PeptideHttp.get("/channels/$channelId".api())
        .bodyAsText()

    return PeptideJson.decodeFromString(
        Channel.serializer(),
        response
    )
}

suspend fun fetchGroupParticipants(channelId: String): List<User> {
    val response = PeptideHttp.get("/channels/$channelId/members".api())
        .bodyAsText()

    return PeptideJson.decodeFromString(
        ListSerializer(User.serializer()),
        response
    )
}

suspend fun createInvite(channelId: String): CreateInviteResponse {
    val response = PeptideHttp.post("/channels/$channelId/invites".api())
        .bodyAsText()

    val error = PeptideJson.decodeFromString(PeptideError.serializer(), response)
    if (error.type != "Server") throw Error(error.type)

    return PeptideJson.decodeFromString(CreateInviteResponse.serializer(), response)
}

suspend fun fetchSingleMessage(channelId: String, messageId: String): Message {
    val response = PeptideHttp.get("/channels/$channelId/messages/$messageId".api())
        .bodyAsText()

    return PeptideJson.decodeFromString(
        Message.serializer(),
        response
    )
}

suspend fun leaveDeleteOrCloseChannel(channelId: String, leaveSilently: Boolean = false) {
    PeptideHttp.delete("/channels/$channelId".api()) {
        parameter("leave_silently", leaveSilently)
    }
}

suspend fun patchChannel(
    channelId: String,
    name: String? = null,
    description: String? = null,
    icon: String? = null,
    banner: String? = null,
    remove: List<String>? = null,
    nsfw: Boolean? = null,
    pure: Boolean = false
) {
    val body = mutableMapOf<String, JsonElement>()

    if (name != null) {
        body["name"] = PeptideJson.encodeToJsonElement(String.serializer(), name)
    }

    if (description != null) {
        body["description"] = PeptideJson.encodeToJsonElement(String.serializer(), description)
    }

    if (icon != null) {
        body["icon"] = PeptideJson.encodeToJsonElement(String.serializer(), icon)
    }

    if (banner != null) {
        body["banner"] = PeptideJson.encodeToJsonElement(String.serializer(), banner)
    }

    if (remove != null) {
        body["remove"] = PeptideJson.encodeToJsonElement(ListSerializer(String.serializer()), remove)
    }

    if (nsfw != null) {
        body["nsfw"] = PeptideJson.encodeToJsonElement(Boolean.serializer(), nsfw)
    }

    val response = PeptideHttp.patch("/channels/$channelId".api()) {
        contentType(ContentType.Application.Json)
        setBody(
            PeptideJson.encodeToString(
                MapSerializer(
                    String.serializer(),
                    JsonElement.serializer()
                ),
                body
            )
        )
    }
        .bodyAsText()

    try {
        val error = PeptideJson.decodeFromString(PeptideError.serializer(), response)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    if (!pure) {
        val channel = PeptideJson.decodeFromString(Channel.serializer(), response)
        PeptideAPI.channelCache[channelId] = channel
    }
}