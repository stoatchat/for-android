package chat.peptide.api.routes.user

import chat.peptide.api.PeptideError
import chat.peptide.api.PeptideHttp
import chat.peptide.api.PeptideJson
import chat.peptide.api.api
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerializationException

suspend fun blockUser(userId: String) {
    val response = PeptideHttp.put("/users/$userId/block".api())
        .bodyAsText()

    try {
        val error = PeptideJson.decodeFromString(PeptideError.serializer(), response)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }
}

suspend fun unblockUser(userId: String) {
    val response = PeptideHttp.delete("/users/$userId/block".api())
        .bodyAsText()

    try {
        val error = PeptideJson.decodeFromString(PeptideError.serializer(), response)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }
}

suspend fun friendUser(username: String) {
    val response = PeptideHttp.post("/users/friend".api()) {
        contentType(ContentType.Application.Json)
        setBody(mapOf("username" to username))
    }
    val body = response.bodyAsText()

    try {
        val error = PeptideJson.decodeFromString(PeptideError.serializer(), body)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }
}

suspend fun acceptFriendRequest(userId: String) {
    val response = PeptideHttp.put("/users/$userId/friend".api())
        .bodyAsText()

    try {
        val error = PeptideJson.decodeFromString(PeptideError.serializer(), response)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }
}

suspend fun unfriendUser(userId: String) {
    val response = PeptideHttp.delete("/users/$userId/friend".api())
        .bodyAsText()

    try {
        val error = PeptideJson.decodeFromString(PeptideError.serializer(), response)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }
}