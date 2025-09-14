package chat.zekochat.api.routes.microservices.health

import chat.zekochat.api.PeptideHttp
import chat.zekochat.api.PeptideJson
import chat.zekochat.api.schemas.HealthNotice
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

suspend fun healthCheck(): HealthNotice {
    val response = PeptideHttp.get("https://health.peptide.chat/api/health").bodyAsText()
    return PeptideJson.decodeFromString(HealthNotice.serializer(), response)
}