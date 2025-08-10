package chat.peptide.api.routes.channel

import chat.peptide.api.RevoltHttp
import chat.peptide.api.api
import io.ktor.client.request.delete
import io.ktor.client.request.put

suspend fun react(channelId: String, messageId: String, emoji: String) {
    RevoltHttp.put("/channels/$channelId/messages/$messageId/reactions/$emoji".api())
}

suspend fun unreact(channelId: String, messageId: String, emoji: String) {
    RevoltHttp.delete("/channels/$channelId/messages/$messageId/reactions/$emoji".api())
}