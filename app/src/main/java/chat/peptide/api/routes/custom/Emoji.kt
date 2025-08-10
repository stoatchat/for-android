package chat.peptide.api.routes.custom

import chat.peptide.api.RevoltHttp
import chat.peptide.api.RevoltJson
import chat.peptide.api.api
import chat.peptide.api.schemas.Emoji
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

suspend fun fetchEmoji(id: String): Emoji {
    val response = RevoltHttp.get("/custom/emoji/$id".api()).bodyAsText()
    return RevoltJson.decodeFromString(
        Emoji.serializer(),
        response
    )
}
