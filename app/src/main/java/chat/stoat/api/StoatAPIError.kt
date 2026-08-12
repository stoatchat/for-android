package chat.stoat.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class StoatAPIError(val type: String)

internal fun apiError(responseContent: String, statusCode: Int): String =
    runCatching {
        Json.parseToJsonElement(responseContent)
            .jsonObject["type"]
            ?.jsonPrimitive
            ?.contentOrNull
    }.getOrNull() ?: "Request failed ($statusCode)"
