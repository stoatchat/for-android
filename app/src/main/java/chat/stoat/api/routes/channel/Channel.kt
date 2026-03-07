package chat.stoat.api.routes.channel

import chat.stoat.api.StoatAPI
import chat.stoat.api.StoatAPIError
import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatJson
import chat.stoat.api.api
import chat.stoat.api.internals.ULID
import chat.stoat.core.model.schemas.Channel
import chat.stoat.core.model.schemas.Message
import chat.stoat.core.model.schemas.MessagesInChannel
import chat.stoat.core.model.schemas.User
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
    val response = StoatHttp.get("/channels/$channelId/messages".api()) {
        parameter("limit", limit)
        parameter("include_users", includeUsers)

        if (before != null) parameter("before", before)
        if (after != null) parameter("after", after)
        if (nearby != null) parameter("nearby", nearby)
        if (sort != null) parameter("sort", sort)
    }
        .bodyAsText()

    if (includeUsers) {
        return StoatJson.decodeFromString(
            MessagesInChannel.serializer(),
            response
        )
    } else {
        val messages = StoatJson.decodeFromString(
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
    val response = StoatHttp.post("/channels/$channelId/messages".api()) {
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
    val response = StoatHttp.patch("/channels/$channelId/messages/$messageId".api()) {
        contentType(ContentType.Application.Json)
        setBody(
            EditMessageBody(
                content = newContent
            )
        )
    }
        .bodyAsText()

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), response)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }
}

suspend fun deleteMessage(channelId: String, messageId: String) {
    StoatHttp.delete("/channels/$channelId/messages/$messageId".api())
}

suspend fun ackChannel(channelId: String, messageId: String = ULID.makeNext()) {
    StoatHttp.put("/channels/$channelId/ack/$messageId".api())
}

suspend fun fetchSingleChannel(channelId: String): Channel {
    val response = StoatHttp.get("/channels/$channelId".api())
        .bodyAsText()

    return StoatJson.decodeFromString(
        Channel.serializer(),
        response
    )
}

suspend fun fetchGroupParticipants(channelId: String): List<User> {
    val response = StoatHttp.get("/channels/$channelId/members".api())
        .bodyAsText()

    return StoatJson.decodeFromString(
        ListSerializer(User.serializer()),
        response
    )
}

suspend fun createInvite(channelId: String): CreateInviteResponse {
    val response = StoatHttp.post("/channels/$channelId/invites".api())
        .bodyAsText()

    val error = StoatJson.decodeFromString(StoatAPIError.serializer(), response)
    if (error.type != "Server") throw Error(error.type)

    return StoatJson.decodeFromString(CreateInviteResponse.serializer(), response)
}

suspend fun fetchSingleMessage(channelId: String, messageId: String): Message {
    val response = StoatHttp.get("/channels/$channelId/messages/$messageId".api())
        .bodyAsText()

    return StoatJson.decodeFromString(
        Message.serializer(),
        response
    )
}

suspend fun leaveDeleteOrCloseChannel(channelId: String, leaveSilently: Boolean = false) {
    StoatHttp.delete("/channels/$channelId".api()) {
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
        body["name"] = StoatJson.encodeToJsonElement(String.serializer(), name)
    }

    if (description != null) {
        body["description"] = StoatJson.encodeToJsonElement(String.serializer(), description)
    }

    if (icon != null) {
        body["icon"] = StoatJson.encodeToJsonElement(String.serializer(), icon)
    }

    if (banner != null) {
        body["banner"] = StoatJson.encodeToJsonElement(String.serializer(), banner)
    }

    if (remove != null) {
        body["remove"] = StoatJson.encodeToJsonElement(ListSerializer(String.serializer()), remove)
    }

    if (nsfw != null) {
        body["nsfw"] = StoatJson.encodeToJsonElement(Boolean.serializer(), nsfw)
    }

    val response = StoatHttp.patch("/channels/$channelId".api()) {
        contentType(ContentType.Application.Json)
        setBody(
            StoatJson.encodeToString(
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
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), response)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    if (!pure) {
        val channel = StoatJson.decodeFromString(Channel.serializer(), response)
        StoatAPI.channelCache[channelId] = channel
    }
}

suspend fun searchChannel(
    channelId: String,
    query: String? = null,
    pinned: Boolean? = null,
    includeUsers: Boolean? = null,
    after: String? = null,
    before: String? = null,
    limit: Int? = null,
    sort: String? = null
): MessagesInChannel {
    check(
        sort == null || sort in listOf(
            "Relevance",
            "Latest",
            "Oldest"
        )
    ) { "Sort must be one of Relevance, Latest, Oldest; failing that null" }
    check(limit == null || (limit in 1..100)) { "Limit must be between 1 and 100; failing that null" }
    check(query == null || pinned == null) { "One of query or pinned must be null" }
    check(
        pinned != null || (!query.isNullOrBlank() && query.length <= 64)
    ) { "Query must not be null when pinned is not null; Query must not be blank when pinned is not null; Query must be less than 65 characters when pinned is not null" }

    val body = mutableMapOf<String, JsonElement>()

    if (query != null) {
        body["query"] = StoatJson.encodeToJsonElement(String.serializer(), query)
    }
    if (pinned != null) {
        body["pinned"] = StoatJson.encodeToJsonElement(Boolean.serializer(), pinned)
    }
    if (includeUsers != null) {
        body["include_users"] = StoatJson.encodeToJsonElement(Boolean.serializer(), includeUsers)
    }
    if (after != null) {
        body["after"] = StoatJson.encodeToJsonElement(String.serializer(), after)
    }
    if (before != null) {
        body["before"] = StoatJson.encodeToJsonElement(String.serializer(), before)
    }
    if (limit != null) {
        body["limit"] = StoatJson.encodeToJsonElement(Int.serializer(), limit)
    }
    if (sort != null) {
        body["sort"] = StoatJson.encodeToJsonElement(String.serializer(), sort)
    }

    val response = StoatHttp.post("/channels/$channelId/search".api()) {
        contentType(ContentType.Application.Json)
        setBody(
            StoatJson.encodeToString(
                MapSerializer(
                    String.serializer(),
                    JsonElement.serializer()
                ),
                body
            )
        )
    }
        .bodyAsText()

    if (includeUsers == true) {
        return StoatJson.decodeFromString(
            MessagesInChannel.serializer(),
            response
        )
    } else {
        val messages = StoatJson.decodeFromString(
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