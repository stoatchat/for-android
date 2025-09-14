package chat.zekochat.api.routes.sync

import chat.zekochat.api.PeptideAPI
import chat.zekochat.api.PeptideHttp
import chat.zekochat.api.PeptideJson
import chat.zekochat.api.api
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonArray

@Serializable
data class SyncedSetting(val timestamp: Long, val value: String)

suspend fun getKeys(vararg keys: String, peptideToken: String): Map<String, SyncedSetting> {
    val response = PeptideHttp.post("/sync/settings/fetch".api()) {
        headers.append(PeptideAPI.TOKEN_HEADER_NAME, peptideToken)

        // format: {"keys": ["key1", "key2"]}
        setBody(
            PeptideJson.encodeToString(
                MapSerializer(
                    String.serializer(),
                    ListSerializer(String.serializer())
                ),
                mapOf("keys" to keys.toList())
            )
        )
    }.bodyAsText()

    return PeptideJson.decodeFromString(
        MapSerializer(
            String.serializer(),
            JsonArray.serializer()
        ),
        response
    ).mapValues { (_, value) ->
        SyncedSetting(
            timestamp = value[0].toString().toLong(),
            value = value[1]
                .toString()
                .removeSurrounding("\"")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\") // the peptide API is so scuffed i can't even make this up
        )
    }
}

suspend fun getKeys(vararg keys: String): Map<String, SyncedSetting> {
    return getKeys(*keys, peptideToken = PeptideAPI.sessionToken)
}

suspend fun setKey(key: String, value: String) {
    PeptideHttp.post("/sync/settings/set".api()) {
        parameter("timestamp", System.currentTimeMillis())

        // format: {"key": "value"}
        setBody(
            PeptideJson.encodeToString(
                MapSerializer(
                    String.serializer(),
                    String.serializer()
                ),
                mapOf(key to value)
            )
        )
    }
}
