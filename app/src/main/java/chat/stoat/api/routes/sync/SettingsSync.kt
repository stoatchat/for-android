package chat.stoat.api.routes.sync

import chat.stoat.api.StoatAPI
import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatJson
import chat.stoat.api.api
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

suspend fun getKeys(vararg keys: String, token: String): Map<String, SyncedSetting> {
    val response = StoatHttp.post("/sync/settings/fetch".api()) {
        headers.append(StoatAPI.TOKEN_HEADER_NAME, token)

        // format: {"keys": ["key1", "key2"]}
        setBody(
            StoatJson.encodeToString(
                MapSerializer(
                    String.serializer(),
                    ListSerializer(String.serializer())
                ),
                mapOf("keys" to keys.toList())
            )
        )
    }.bodyAsText()

    return StoatJson.decodeFromString(
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
                .replace("\\\\", "\\") // the revolt API is so scuffed i can't even make this up
        )
    }
}

suspend fun getKeys(vararg keys: String): Map<String, SyncedSetting> {
    return getKeys(*keys, token = StoatAPI.sessionToken)
}

suspend fun setKey(key: String, value: String, timestamp: Long = System.currentTimeMillis()) {
    StoatHttp.post("/sync/settings/set".api()) {
        parameter("timestamp", timestamp)

        // format: {"key": "value"}
        setBody(
            StoatJson.encodeToString(
                MapSerializer(
                    String.serializer(),
                    String.serializer()
                ),
                mapOf(key to value)
            )
        )
    }
}
