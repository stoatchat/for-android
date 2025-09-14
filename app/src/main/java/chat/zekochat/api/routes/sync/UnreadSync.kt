package chat.zekochat.api.routes.sync

import chat.zekochat.api.PeptideHttp
import chat.zekochat.api.PeptideJson
import chat.zekochat.api.api
import chat.zekochat.api.schemas.ChannelUnreadResponse
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.builtins.ListSerializer

suspend fun syncUnreads(): List<ChannelUnreadResponse> {
    val response = PeptideHttp.get("/sync/unreads".api())
        .bodyAsText()

    return PeptideJson.decodeFromString(
        ListSerializer(ChannelUnreadResponse.serializer()),
        response
    )
}
