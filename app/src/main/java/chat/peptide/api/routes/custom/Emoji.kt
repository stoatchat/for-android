package chat.peptide.api.routes.custom

import chat.peptide.api.PeptideHttp
import chat.peptide.api.PeptideJson
import chat.peptide.api.api
import chat.peptide.api.schemas.Emoji
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

suspend fun fetchEmoji(id: String): Emoji {
    val response = PeptideHttp.get("/custom/emoji/$id".api()).bodyAsText()
    return PeptideJson.decodeFromString(
        Emoji.serializer(),
        response
    )
}
