package chat.stoat.api.routes.user

import chat.stoat.api.StoatAPI
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

private fun updateOutgoingRelationshipInCache(friendTag: String, userIdHint: String?) {
    if (userIdHint != null) {
        val byId = StoatAPI.userCache[userIdHint]
        if (byId != null) {
            StoatAPI.userCache[userIdHint] = byId.copy(relationship = "Outgoing")
            return
        }
    }

    val username = friendTag.substringBeforeLast("#", "")
    val discriminator = friendTag.substringAfterLast("#", "")
    if (username.isBlank() || discriminator.isBlank()) return

    val user = StoatAPI.userCache.values.firstOrNull {
        it.username == username && it.discriminator == discriminator
    } ?: return

    val userId = user.id ?: return
    StoatAPI.userCache[userId] = user.copy(relationship = "Outgoing")
}

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

suspend fun friendUser(username: String, userIdHint: String? = null) {
    val response = StoatHttp.post("/users/friend".api()) {
        contentType(ContentType.Application.Json)
        setBody(mapOf("username" to username))
    }
    val body = response.bodyAsText()

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), body)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error. Apply a local cache fallback so UI updates immediately.
        updateOutgoingRelationshipInCache(username, userIdHint)
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