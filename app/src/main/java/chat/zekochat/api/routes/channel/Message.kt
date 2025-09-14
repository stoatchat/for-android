package chat.zekochat.api.routes.channel

import chat.zekochat.api.PeptideHttp
import chat.zekochat.api.api
import io.ktor.client.request.delete
import io.ktor.client.request.put

suspend fun react(channelId: String, messageId: String, emoji: String) {
    PeptideHttp.put("/channels/$channelId/messages/$messageId/reactions/$emoji".api())
}

suspend fun unreact(channelId: String, messageId: String, emoji: String) {
    PeptideHttp.delete("/channels/$channelId/messages/$messageId/reactions/$emoji".api())
}