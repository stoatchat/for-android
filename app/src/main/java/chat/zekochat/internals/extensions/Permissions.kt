package chat.zekochat.internals.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import chat.zekochat.api.PeptideAPI
import chat.zekochat.api.internals.Roles

@Composable
fun rememberChannelPermissions(channelId: String, key1: Any = Unit): MutableLongState {
    val permissions = rememberSaveable { mutableLongStateOf(0L) }

    LaunchedEffect(channelId, key1) {
        if (PeptideAPI.selfId == null) return@LaunchedEffect
        if (PeptideAPI.userCache[PeptideAPI.selfId] == null) return@LaunchedEffect
        if (PeptideAPI.channelCache[channelId] == null) return@LaunchedEffect

        val channel = PeptideAPI.channelCache[channelId]
        val selfUser = PeptideAPI.userCache[PeptideAPI.selfId]
        val member = channel?.let {
            it.server?.let { server ->
                PeptideAPI.selfId?.let { selfId ->
                    PeptideAPI.members.getMember(server, selfId)
                }
            }
        }
        channel?.let { permissions.longValue = Roles.permissionFor(it, selfUser, member) }
    }

    return permissions
}