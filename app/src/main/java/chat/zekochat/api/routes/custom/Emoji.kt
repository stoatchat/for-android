package chat.zekochat.api.routes.custom

import chat.zekochat.api.PeptideHttp
import chat.zekochat.api.PeptideJson
import chat.zekochat.api.api
import chat.zekochat.api.schemas.Emoji
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

suspend fun fetchEmoji(id: String): Emoji {
    val response = PeptideHttp.get("/custom/emoji/$id".api()).bodyAsText()
    return PeptideJson.decodeFromString(
        Emoji.serializer(),
        response
    )
}
