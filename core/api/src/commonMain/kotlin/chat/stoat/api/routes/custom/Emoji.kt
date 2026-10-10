package chat.stoat.api.routes.custom

import chat.stoat.api.StoatAPI
import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatJson
import chat.stoat.api.api
import chat.stoat.api.apiError
import chat.stoat.core.model.schemas.Emoji
import chat.stoat.core.model.schemas.EmojiParent
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

suspend fun fetchEmoji(id: String): Emoji {
    val response = StoatHttp.get("/custom/emoji/$id".api()).bodyAsText()
    return StoatJson.decodeFromString(
        Emoji.serializer(),
        response
    )
}

@Serializable
private data class CreateEmojiBody(
    val name: String,
    val parent: EmojiParent,
)

suspend fun createEmoji(
    autumnId: String,
    serverId: String,
    name: String,
): Emoji {
    val response = StoatHttp.put("/custom/emoji/$autumnId".api()) {
        contentType(ContentType.Application.Json)
        setBody(
            CreateEmojiBody(
                name = name,
                parent = EmojiParent(type = "Server", id = serverId),
            )
        )
    }
    val responseContent = response.bodyAsText()

    if (!response.status.isSuccess()) {
        throw Exception(apiError(responseContent, response.status.value))
    }

    return StoatJson.decodeFromString(Emoji.serializer(), responseContent).also { emoji ->
        emoji.id?.let { StoatAPI.emojiCache[it] = emoji }
    }
}

suspend fun deleteEmoji(id: String) {
    val response = StoatHttp.delete("/custom/emoji/$id".api())
    val responseContent = response.bodyAsText()

    if (!response.status.isSuccess()) {
        throw Exception(apiError(responseContent, response.status.value))
    }

    StoatAPI.emojiCache.remove(id)
}
