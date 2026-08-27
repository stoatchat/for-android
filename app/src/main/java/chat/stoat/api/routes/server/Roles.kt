package chat.stoat.api.routes.server

import chat.stoat.api.StoatAPI
import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatJson
import chat.stoat.api.api
import chat.stoat.api.apiError
import chat.stoat.core.model.schemas.PermissionDescription
import chat.stoat.core.model.schemas.Role
import chat.stoat.core.model.schemas.Server
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

@Serializable
private data class CreateRoleBody(val name: String)

@Serializable
private data class CreateRoleResponse(
    val id: String,
    val role: Role,
)

@Serializable
private data class EditRoleBody(
    val name: String? = null,
    val colour: String? = null,
    val hoist: Boolean? = null,
    val icon: String? = null,
    val remove: List<String> = emptyList(),
)

@Serializable
private data class PermissionOverrideBody(
    val allow: Long,
    val deny: Long,
)

@Serializable
private data class SetRolePermissionsBody(
    val permissions: PermissionOverrideBody,
)

@Serializable
private data class SetDefaultPermissionsBody(val permissions: Long)

@Serializable
private data class EditRoleRanksBody(val ranks: List<String>)

data class CreatedServerRole(
    val id: String,
    val role: Role,
)

suspend fun fetchServerRole(serverId: String, roleId: String): Role {
    val response = StoatHttp.get("/servers/$serverId/roles/$roleId".api())
    val content = response.bodyAsText()
    if (!response.status.isSuccess()) throw Exception(apiError(content, response.status.value))

    return StoatJson.decodeFromString(Role.serializer(), content).also {
        updateCachedRole(serverId, roleId, it)
    }
}

suspend fun createServerRole(serverId: String, name: String): CreatedServerRole {
    val response = StoatHttp.post("/servers/$serverId/roles".api()) {
        contentType(ContentType.Application.Json)
        setBody(StoatJson.encodeToString(CreateRoleBody.serializer(), CreateRoleBody(name)))
    }
    val content = response.bodyAsText()
    if (!response.status.isSuccess()) throw Exception(apiError(content, response.status.value))

    val created = StoatJson.decodeFromString(CreateRoleResponse.serializer(), content)
    updateCachedRole(serverId, created.id, created.role)
    return CreatedServerRole(created.id, created.role)
}

suspend fun editServerRole(
    serverId: String,
    roleId: String,
    name: String? = null,
    colour: String? = null,
    hoist: Boolean? = null,
    icon: String? = null,
    remove: List<String> = emptyList(),
): Role {
    val body = EditRoleBody(name, colour, hoist, icon, remove)
    val response = StoatHttp.patch("/servers/$serverId/roles/$roleId".api()) {
        contentType(ContentType.Application.Json)
        setBody(StoatJson.encodeToString(EditRoleBody.serializer(), body))
    }
    val content = response.bodyAsText()
    if (!response.status.isSuccess()) throw Exception(apiError(content, response.status.value))

    return StoatJson.decodeFromString(Role.serializer(), content).also {
        updateCachedRole(serverId, roleId, it)
    }
}

suspend fun deleteServerRole(serverId: String, roleId: String) {
    val response = StoatHttp.delete("/servers/$serverId/roles/$roleId".api())
    if (!response.status.isSuccess()) {
        throw Exception(apiError(response.bodyAsText(), response.status.value))
    }

    StoatAPI.serverCache[serverId]?.let { server ->
        StoatAPI.serverCache[serverId] = server.copy(
            roles = server.roles.orEmpty() - roleId,
        )
    }
}

suspend fun setServerRolePermissions(
    serverId: String,
    roleId: String,
    permissions: PermissionDescription,
): Server {
    val body = SetRolePermissionsBody(
        PermissionOverrideBody(permissions.a, permissions.d)
    )
    return updateServerFromResponse(
        serverId = serverId,
        responseContent = StoatHttp.put("/servers/$serverId/permissions/$roleId".api()) {
            contentType(ContentType.Application.Json)
            setBody(StoatJson.encodeToString(SetRolePermissionsBody.serializer(), body))
        }.let { response ->
            val content = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw Exception(apiError(content, response.status.value))
            }
            content
        },
    )
}

suspend fun setDefaultServerPermissions(serverId: String, permissions: Long): Server {
    val response = StoatHttp.put("/servers/$serverId/permissions/default".api()) {
        contentType(ContentType.Application.Json)
        setBody(
            StoatJson.encodeToString(
                SetDefaultPermissionsBody.serializer(),
                SetDefaultPermissionsBody(permissions),
            )
        )
    }
    val content = response.bodyAsText()
    if (!response.status.isSuccess()) throw Exception(apiError(content, response.status.value))
    return updateServerFromResponse(serverId, content)
}

suspend fun reorderServerRoles(serverId: String, roleIds: List<String>): Server {
    val response = StoatHttp.patch("/servers/$serverId/roles/ranks".api()) {
        contentType(ContentType.Application.Json)
        setBody(
            StoatJson.encodeToString(
                EditRoleRanksBody.serializer(),
                EditRoleRanksBody(roleIds),
            )
        )
    }
    val content = response.bodyAsText()
    if (!response.status.isSuccess()) throw Exception(apiError(content, response.status.value))
    return updateServerFromResponse(serverId, content)
}

private fun updateCachedRole(serverId: String, roleId: String, role: Role) {
    StoatAPI.serverCache[serverId]?.let { server ->
        StoatAPI.serverCache[serverId] = server.copy(
            roles = server.roles.orEmpty() + (roleId to role),
        )
    }
}

private fun updateServerFromResponse(serverId: String, responseContent: String): Server =
    StoatJson.decodeFromString(Server.serializer(), responseContent).also {
        StoatAPI.serverCache[serverId] = it
    }
