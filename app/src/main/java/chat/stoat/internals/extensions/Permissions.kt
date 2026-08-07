package chat.stoat.internals.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.internals.Roles
import chat.stoat.api.routes.server.fetchMember

@Composable
fun rememberChannelPermissions(channelId: String, key1: Any = Unit): MutableLongState {
    val permissions = rememberSaveable { mutableLongStateOf(0L) }
    val selfId = StoatAPI.selfId
    val channel = StoatAPI.channelCache[channelId]

    LaunchedEffect(channelId, key1, selfId, channel) {
        if (StoatAPI.selfId == null) return@LaunchedEffect
        if (StoatAPI.userCache[StoatAPI.selfId] == null) return@LaunchedEffect
        if (StoatAPI.channelCache[channelId] == null) return@LaunchedEffect

        val channel = StoatAPI.channelCache[channelId]
        val selfUser = StoatAPI.userCache[StoatAPI.selfId]
        val member = channel?.let {
            it.server?.let { server ->
                StoatAPI.selfId?.let { selfId ->
                    StoatAPI.members.getMember(server, selfId)
                }
            }
        }
        channel?.let { permissions.longValue = Roles.permissionFor(it, selfUser, member) }
    }

    return permissions
}

@Composable
fun rememberServerPermissions(serverId: String, key1: Any = Unit): State<Long?> {
    val permissions = remember(serverId) { mutableStateOf<Long?>(null) }
    val selfId = StoatAPI.selfId
    val server = StoatAPI.serverCache[serverId]
    val selfUser = selfId?.let(StoatAPI.userCache::get)

    LaunchedEffect(serverId, key1, selfId, selfUser, server) {
        permissions.value = null

        if (server == null || selfId == null) {
            return@LaunchedEffect
        }

        if (server.owner == selfId) {
            permissions.value = PermissionBit.GrantAllSafe.value
            return@LaunchedEffect
        }

        if (selfUser == null) return@LaunchedEffect

        if (selfUser.privileged == true) {
            permissions.value = PermissionBit.GrantAllSafe.value
            return@LaunchedEffect
        }

        val member = StoatAPI.members.getMember(serverId, selfId)
            ?: runCatching { fetchMember(serverId, selfId) }.getOrNull()

        permissions.value = member?.let { Roles.permissionFor(server, it) } ?: 0L
    }

    return permissions
}
