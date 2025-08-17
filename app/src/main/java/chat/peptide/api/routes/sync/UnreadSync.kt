package chat.peptide.api.routes.sync

import chat.peptide.api.PeptideHttp
import chat.peptide.api.PeptideJson
import chat.peptide.api.api
import chat.peptide.api.schemas.ChannelUnreadResponse
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
