package chat.peptide.api.routes.microservices.health

import chat.peptide.api.RevoltHttp
import chat.peptide.api.RevoltJson
import chat.peptide.api.schemas.HealthNotice
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

suspend fun healthCheck(): HealthNotice {
    val response = RevoltHttp.get("https://health.revolt.chat/api/health").bodyAsText()
    return RevoltJson.decodeFromString(HealthNotice.serializer(), response)
}