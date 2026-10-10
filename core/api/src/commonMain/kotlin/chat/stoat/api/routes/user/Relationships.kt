package chat.stoat.api.routes.user

import chat.stoat.api.StoatAPIError
import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatJson
import chat.stoat.api.api
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerializationException

suspend fun blockUser(userId: String) {
    val response = StoatHttp.put("/users/$userId/block".api())
        .bodyAsText()

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), response)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }
}

suspend fun unblockUser(userId: String) {
    val response = StoatHttp.delete("/users/$userId/block".api())
        .bodyAsText()

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), response)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }
}

suspend fun friendUser(username: String) {
    val response = StoatHttp.post("/users/friend".api()) {
        contentType(ContentType.Application.Json)
        setBody(mapOf("username" to username))
    }
    val body = response.bodyAsText()

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), body)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }
}

suspend fun acceptFriendRequest(userId: String) {
    val response = StoatHttp.put("/users/$userId/friend".api())
        .bodyAsText()

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), response)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }
}

suspend fun unfriendUser(userId: String) {
    val response = StoatHttp.delete("/users/$userId/friend".api())
        .bodyAsText()

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), response)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }
}