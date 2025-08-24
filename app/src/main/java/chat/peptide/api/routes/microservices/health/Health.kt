package chat.peptide.api.routes.microservices.health

import chat.peptide.api.PeptideHttp
import chat.peptide.api.PeptideJson
import chat.peptide.api.schemas.HealthNotice
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

suspend fun healthCheck(): HealthNotice {
    val response = PeptideHttp.get("https://health.peptide.chat/api/health").bodyAsText()
    return PeptideJson.decodeFromString(HealthNotice.serializer(), response)
}