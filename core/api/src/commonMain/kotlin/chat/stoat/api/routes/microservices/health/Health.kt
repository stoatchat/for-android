package chat.stoat.api.routes.microservices.health

import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatJson
import chat.stoat.core.model.schemas.HealthNotice
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

suspend fun healthCheck(): HealthNotice {
    val response = StoatHttp.get("https://health.revolt.chat/api/health").bodyAsText()
    return StoatJson.decodeFromString(HealthNotice.serializer(), response)
}