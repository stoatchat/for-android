package chat.peptide.api.routes.user

import chat.peptide.api.RevoltAPI
import chat.peptide.api.RevoltError
import chat.peptide.api.RevoltHttp
import chat.peptide.api.RevoltJson
import chat.peptide.api.api
import chat.peptide.api.schemas.Profile
import chat.peptide.api.schemas.Status
import chat.peptide.api.schemas.User
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement

suspend fun fetchSelf(): User {
    val response = RevoltHttp.get("/users/@me".api())
        .bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val user = RevoltJson.decodeFromString(User.serializer(), response)

    if (user.id == null) {
        throw Exception("Self user ID is null")
    }

    RevoltAPI.userCache[user.id] = user
    RevoltAPI.selfId = user.id

    return user
}

suspend fun patchSelf(
    status: Status? = null,
    avatar: String? = null,
    background: String? = null,
    bio: String? = null,
    remove: List<String>? = null,
    pure: Boolean = false
) {
    val body = mutableMapOf<String, JsonElement>()

    if (status != null) {
        body["status"] = RevoltJson.encodeToJsonElement(Status.serializer(), status)
    }

    if (avatar != null) {
        body["avatar"] = RevoltJson.encodeToJsonElement(String.serializer(), avatar)
    }

    if (background != null || bio != null) {
        val profileMap = mutableMapOf<String, String>()

        if (background != null) {
            profileMap["background"] = background
        }
        if (bio != null) {
            profileMap["content"] = bio
        }

        body["profile"] = RevoltJson.encodeToJsonElement(
            MapSerializer(
                String.serializer(),
                String.serializer()
            ),
            profileMap
        )
    }

    if (remove != null) {
        body["remove"] = RevoltJson.encodeToJsonElement(ListSerializer(String.serializer()), remove)
    }

    val response = RevoltHttp.patch("/users/@me".api()) {
        contentType(ContentType.Application.Json)
        setBody(
            RevoltJson.encodeToString(
                MapSerializer(
                    String.serializer(),
                    JsonElement.serializer()
                ),
                body
            )
        )
    }
        .bodyAsText()

    if (RevoltAPI.selfId == null) {
        throw Error("Self ID is null")
    }

    val currentUser = RevoltAPI.userCache[RevoltAPI.selfId] ?: fetchSelf()
    val newUserKeys = RevoltJson.decodeFromString(User.serializer(), response)
    val mergedUser = currentUser.mergeWithPartial(newUserKeys)

    if (!pure) {
        RevoltAPI.userCache[RevoltAPI.selfId!!] = mergedUser
    }
}

suspend fun fetchUser(id: String): User {
    val res = RevoltHttp.get("/users/$id".api())

    if (res.status.value == 404) {
        return User.getPlaceholder(id)
    }

    val response = res.bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val user = RevoltJson.decodeFromString(User.serializer(), response)

    user.id?.let {
        RevoltAPI.userCache[it] = user
    }

    return user
}

suspend fun getOrFetchUser(id: String): User {
    return RevoltAPI.userCache[id] ?: fetchUser(id)
}

suspend fun addUserIfUnknown(id: String) {
    if (RevoltAPI.userCache[id] == null) {
        RevoltAPI.userCache[id] = fetchUser(id)
    }
}

suspend fun fetchUserProfile(id: String): Profile {
    val res = RevoltHttp.get("/users/$id/profile".api())

    val response = res.bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    return RevoltJson.decodeFromString(Profile.serializer(), response)
}