package chat.stoat.internals.server

import androidx.annotation.StringRes
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.internals.hasPermission
import chat.stoat.core.model.schemas.PermissionDescription
import chat.stoat.core.model.schemas.Role
import chat.stoat.core.model.schemas.Server

enum class PermissionOverrideValue {
    Allow,
    Neutral,
    Deny,
}

data class ServerRoleCapabilities(
    val canManageRoles: Boolean,
    val canManagePermissions: Boolean,
) {
    val canOpenRoles: Boolean
        get() = canManageRoles || canManagePermissions
}

data class ServerPermissionOption(
    val bit: PermissionBit,
    @param:StringRes val title: Int,
    @param:StringRes val description: Int,
)

data class ServerPermissionGroup(
    @param:StringRes val title: Int,
    val permissions: List<ServerPermissionOption>,
)

fun serverRoleCapabilities(server: Server, permissions: Long): ServerRoleCapabilities {
    val isOwner = server.owner == StoatAPI.selfId
    return ServerRoleCapabilities(
        canManageRoles = isOwner || permissions.hasPermission(PermissionBit.ManageRole),
        canManagePermissions = isOwner || permissions.hasPermission(PermissionBit.ManagePermissions),
    )
}

// as per backend
fun canManageServerRole(server: Server, role: Role): Boolean {
    if (server.owner == StoatAPI.selfId) return true

    val ownTopRank = StoatAPI.selfId
        ?.let { StoatAPI.members.getMember(server.id.orEmpty(), it) }
        ?.roles
        .orEmpty()
        .mapNotNull { server.roles?.get(it)?.rank }
        .minOrNull()

    return ownTopRank == null || (role.rank ?: Double.MAX_VALUE) > ownTopRank
}

fun PermissionDescription.overrideFor(bit: PermissionBit): PermissionOverrideValue = when {
    a.hasPermission(bit) -> PermissionOverrideValue.Allow
    d.hasPermission(bit) -> PermissionOverrideValue.Deny
    else -> PermissionOverrideValue.Neutral
}

fun PermissionDescription.withOverride(
    bit: PermissionBit,
    value: PermissionOverrideValue,
): PermissionDescription {
    val clearedAllow = a and bit.value.inv()
    val clearedDeny = d and bit.value.inv()
    return when (value) {
        PermissionOverrideValue.Allow -> copy(a = clearedAllow or bit.value, d = clearedDeny)
        PermissionOverrideValue.Neutral -> copy(a = clearedAllow, d = clearedDeny)
        PermissionOverrideValue.Deny -> copy(a = clearedAllow, d = clearedDeny or bit.value)
    }
}

fun Long.withPermission(bit: PermissionBit, enabled: Boolean): Long =
    if (enabled) this or bit.value else this and bit.value.inv()

val ServerPermissionGroups = listOf(
    ServerPermissionGroup(
        R.string.server_role_permissions_admin,
        listOf(
            ServerPermissionOption(
                PermissionBit.ManageChannel,
                R.string.permission_manage_channels,
                R.string.permission_manage_channels_description
            ),
            ServerPermissionOption(
                PermissionBit.ManageServer,
                R.string.permission_manage_server,
                R.string.permission_manage_server_description
            ),
            ServerPermissionOption(
                PermissionBit.ManagePermissions,
                R.string.permission_manage_permissions,
                R.string.permission_manage_permissions_description
            ),
            ServerPermissionOption(
                PermissionBit.ManageRole,
                R.string.permission_manage_roles,
                R.string.permission_manage_roles_description
            ),
            ServerPermissionOption(
                PermissionBit.ManageCustomisation,
                R.string.permission_manage_customisation,
                R.string.permission_manage_customisation_description
            ),
        ),
    ),
    ServerPermissionGroup(
        R.string.server_role_permissions_members,
        listOf(
            ServerPermissionOption(
                PermissionBit.KickMembers,
                R.string.permission_kick_members,
                R.string.permission_kick_members_description
            ),
            ServerPermissionOption(
                PermissionBit.BanMembers,
                R.string.permission_ban_members,
                R.string.permission_ban_members_description
            ),
            ServerPermissionOption(
                PermissionBit.TimeoutMembers,
                R.string.permission_timeout_members,
                R.string.permission_timeout_members_description
            ),
            ServerPermissionOption(
                PermissionBit.AssignRoles,
                R.string.permission_assign_roles,
                R.string.permission_assign_roles_description
            ),
            ServerPermissionOption(
                PermissionBit.ChangeNickname,
                R.string.permission_change_nickname,
                R.string.permission_change_nickname_description
            ),
            ServerPermissionOption(
                PermissionBit.ManageNicknames,
                R.string.permission_manage_nicknames,
                R.string.permission_manage_nicknames_description
            ),
            ServerPermissionOption(
                PermissionBit.ChangeAvatar,
                R.string.permission_change_avatar,
                R.string.permission_change_avatar_description
            ),
            ServerPermissionOption(
                PermissionBit.RemoveAvatars,
                R.string.permission_remove_avatars,
                R.string.permission_remove_avatars_description
            ),
        ),
    ),
    ServerPermissionGroup(
        R.string.server_role_permissions_channels,
        listOf(
            ServerPermissionOption(
                PermissionBit.ViewChannel,
                R.string.permission_view_channels,
                R.string.permission_view_channels_description
            ),
            ServerPermissionOption(
                PermissionBit.ReadMessageHistory,
                R.string.permission_read_history,
                R.string.permission_read_history_description
            ),
            ServerPermissionOption(
                PermissionBit.SendMessage,
                R.string.permission_send_messages,
                R.string.permission_send_messages_description
            ),
            ServerPermissionOption(
                PermissionBit.ManageMessages,
                R.string.permission_manage_messages,
                R.string.permission_manage_messages_description
            ),
            ServerPermissionOption(
                PermissionBit.ManageWebhooks,
                R.string.permission_manage_webhooks,
                R.string.permission_manage_webhooks_description
            ),
            ServerPermissionOption(
                PermissionBit.InviteOthers,
                R.string.permission_invite_others,
                R.string.permission_invite_others_description
            ),
        ),
    ),
    ServerPermissionGroup(
        R.string.server_role_permissions_messaging,
        listOf(
            ServerPermissionOption(
                PermissionBit.SendEmbeds,
                R.string.permission_send_embeds,
                R.string.permission_send_embeds_description
            ),
            ServerPermissionOption(
                PermissionBit.UploadFiles,
                R.string.permission_upload_files,
                R.string.permission_upload_files_description
            ),
            ServerPermissionOption(
                PermissionBit.Masquerade,
                R.string.permission_masquerade,
                R.string.permission_masquerade_description
            ),
            ServerPermissionOption(
                PermissionBit.React,
                R.string.permission_react,
                R.string.permission_react_description
            ),
            ServerPermissionOption(
                PermissionBit.BypassSlowmode,
                R.string.permission_bypass_slowmode,
                R.string.permission_bypass_slowmode_description
            ),
        ),
    ),
    ServerPermissionGroup(
        R.string.server_role_permissions_voice,
        listOf(
            ServerPermissionOption(
                PermissionBit.Connect,
                R.string.permission_connect,
                R.string.permission_connect_description
            ),
            ServerPermissionOption(
                PermissionBit.Speak,
                R.string.permission_speak,
                R.string.permission_speak_description
            ),
            ServerPermissionOption(
                PermissionBit.Video,
                R.string.permission_video,
                R.string.permission_video_description
            ),
            ServerPermissionOption(
                PermissionBit.MuteMembers,
                R.string.permission_mute_members,
                R.string.permission_mute_members_description
            ),
            ServerPermissionOption(
                PermissionBit.DeafenMembers,
                R.string.permission_deafen_members,
                R.string.permission_deafen_members_description
            ),
            ServerPermissionOption(
                PermissionBit.MoveMembers,
                R.string.permission_move_members,
                R.string.permission_move_members_description
            ),
            ServerPermissionOption(
                PermissionBit.Listen,
                R.string.permission_listen,
                R.string.permission_listen_description
            ),
        ),
    ),
    ServerPermissionGroup(
        R.string.server_role_permissions_mentions,
        listOf(
            ServerPermissionOption(
                PermissionBit.MentionEveryone,
                R.string.permission_mention_everyone,
                R.string.permission_mention_everyone_description
            ),
            ServerPermissionOption(
                PermissionBit.MentionRoles,
                R.string.permission_mention_roles,
                R.string.permission_mention_roles_description
            ),
        ),
    ),
)
