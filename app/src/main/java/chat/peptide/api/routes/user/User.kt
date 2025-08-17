package chat.peptide.api.routes.user

import chat.peptide.api.PeptideAPI
import chat.peptide.api.PeptideError
import chat.peptide.api.PeptideHttp
import chat.peptide.api.PeptideJson
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
    val response = PeptideHttp.get("/users/@me".api())
        .bodyAsText()

    try {
        val error = PeptideJson.decodeFromString(PeptideError.serializer(), response)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val user = PeptideJson.decodeFromString(User.serializer(), response)

    if (user.id == null) {
        throw Exception("Self user ID is null")
    }

    PeptideAPI.userCache[user.id] = user
    PeptideAPI.selfId = user.id

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
        body["status"] = PeptideJson.encodeToJsonElement(Status.serializer(), status)
    }

    if (avatar != null) {
        body["avatar"] = PeptideJson.encodeToJsonElement(String.serializer(), avatar)
    }

    if (background != null || bio != null) {
        val profileMap = mutableMapOf<String, String>()

        if (background != null) {
            profileMap["background"] = background
        }
        if (bio != null) {
            profileMap["content"] = bio
        }

        body["profile"] = PeptideJson.encodeToJsonElement(
            MapSerializer(
                String.serializer(),
                String.serializer()
            ),
            profileMap
        )
    }

    if (remove != null) {
        body["remove"] = PeptideJson.encodeToJsonElement(ListSerializer(String.serializer()), remove)
    }

    val response = PeptideHttp.patch("/users/@me".api()) {
        contentType(ContentType.Application.Json)
        setBody(
            PeptideJson.encodeToString(
                MapSerializer(
                    String.serializer(),
                    JsonElement.serializer()
                ),
                body
            )
        )
    }
        .bodyAsText()

    if (PeptideAPI.selfId == null) {
        throw Error("Self ID is null")
    }

    val currentUser = PeptideAPI.userCache[PeptideAPI.selfId] ?: fetchSelf()
    val newUserKeys = PeptideJson.decodeFromString(User.serializer(), response)
    val mergedUser = currentUser.mergeWithPartial(newUserKeys)

    if (!pure) {
        PeptideAPI.userCache[PeptideAPI.selfId!!] = mergedUser
    }
}

suspend fun fetchUser(id: String): User {
    val res = PeptideHttp.get("/users/$id".api())

    if (res.status.value == 404) {
        return User.getPlaceholder(id)
    }

    val response = res.bodyAsText()

    try {
        val error = PeptideJson.decodeFromString(PeptideError.serializer(), response)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val user = PeptideJson.decodeFromString(User.serializer(), response)

    user.id?.let {
        PeptideAPI.userCache[it] = user
    }

    return user
}

suspend fun getOrFetchUser(id: String): User {
    return PeptideAPI.userCache[id] ?: fetchUser(id)
}

suspend fun addUserIfUnknown(id: String) {
    if (PeptideAPI.userCache[id] == null) {
        PeptideAPI.userCache[id] = fetchUser(id)
    }
}

suspend fun fetchUserProfile(id: String): Profile {
    val res = PeptideHttp.get("/users/$id/profile".api())

    val response = res.bodyAsText()

    try {
        val error = PeptideJson.decodeFromString(PeptideError.serializer(), response)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    return PeptideJson.decodeFromString(Profile.serializer(), response)
}