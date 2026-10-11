package chat.stoat.internals.server

import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.internals.hasPermission
import chat.stoat.core.model.schemas.PermissionDescription
import chat.stoat.core.model.schemas.Role
import chat.stoat.core.model.schemas.Server
import chat.stoat.shared.resources.Res
import chat.stoat.shared.resources.permission_assign_roles
import chat.stoat.shared.resources.permission_assign_roles_description
import chat.stoat.shared.resources.permission_ban_members
import chat.stoat.shared.resources.permission_ban_members_description
import chat.stoat.shared.resources.permission_bypass_slowmode
import chat.stoat.shared.resources.permission_bypass_slowmode_description
import chat.stoat.shared.resources.permission_change_avatar
import chat.stoat.shared.resources.permission_change_avatar_description
import chat.stoat.shared.resources.permission_change_nickname
import chat.stoat.shared.resources.permission_change_nickname_description
import chat.stoat.shared.resources.permission_connect
import chat.stoat.shared.resources.permission_connect_description
import chat.stoat.shared.resources.permission_deafen_members
import chat.stoat.shared.resources.permission_deafen_members_description
import chat.stoat.shared.resources.permission_invite_others
import chat.stoat.shared.resources.permission_invite_others_description
import chat.stoat.shared.resources.permission_kick_members
import chat.stoat.shared.resources.permission_kick_members_description
import chat.stoat.shared.resources.permission_listen
import chat.stoat.shared.resources.permission_listen_description
import chat.stoat.shared.resources.permission_manage_channels
import chat.stoat.shared.resources.permission_manage_channels_description
import chat.stoat.shared.resources.permission_manage_customisation
import chat.stoat.shared.resources.permission_manage_customisation_description
import chat.stoat.shared.resources.permission_manage_messages
import chat.stoat.shared.resources.permission_manage_messages_description
import chat.stoat.shared.resources.permission_manage_nicknames
import chat.stoat.shared.resources.permission_manage_nicknames_description
import chat.stoat.shared.resources.permission_manage_permissions
import chat.stoat.shared.resources.permission_manage_permissions_description
import chat.stoat.shared.resources.permission_manage_roles
import chat.stoat.shared.resources.permission_manage_roles_description
import chat.stoat.shared.resources.permission_manage_server
import chat.stoat.shared.resources.permission_manage_server_description
import chat.stoat.shared.resources.permission_manage_webhooks
import chat.stoat.shared.resources.permission_manage_webhooks_description
import chat.stoat.shared.resources.permission_masquerade
import chat.stoat.shared.resources.permission_masquerade_description
import chat.stoat.shared.resources.permission_mention_everyone
import chat.stoat.shared.resources.permission_mention_everyone_description
import chat.stoat.shared.resources.permission_mention_roles
import chat.stoat.shared.resources.permission_mention_roles_description
import chat.stoat.shared.resources.permission_move_members
import chat.stoat.shared.resources.permission_move_members_description
import chat.stoat.shared.resources.permission_mute_members
import chat.stoat.shared.resources.permission_mute_members_description
import chat.stoat.shared.resources.permission_react
import chat.stoat.shared.resources.permission_react_description
import chat.stoat.shared.resources.permission_read_history
import chat.stoat.shared.resources.permission_read_history_description
import chat.stoat.shared.resources.permission_remove_avatars
import chat.stoat.shared.resources.permission_remove_avatars_description
import chat.stoat.shared.resources.permission_send_embeds
import chat.stoat.shared.resources.permission_send_embeds_description
import chat.stoat.shared.resources.permission_send_messages
import chat.stoat.shared.resources.permission_send_messages_description
import chat.stoat.shared.resources.permission_speak
import chat.stoat.shared.resources.permission_speak_description
import chat.stoat.shared.resources.permission_timeout_members
import chat.stoat.shared.resources.permission_timeout_members_description
import chat.stoat.shared.resources.permission_upload_files
import chat.stoat.shared.resources.permission_upload_files_description
import chat.stoat.shared.resources.permission_video
import chat.stoat.shared.resources.permission_video_description
import chat.stoat.shared.resources.permission_view_channels
import chat.stoat.shared.resources.permission_view_channels_description
import chat.stoat.shared.resources.server_role_permissions_admin
import chat.stoat.shared.resources.server_role_permissions_channels
import chat.stoat.shared.resources.server_role_permissions_members
import chat.stoat.shared.resources.server_role_permissions_mentions
import chat.stoat.shared.resources.server_role_permissions_messaging
import chat.stoat.shared.resources.server_role_permissions_voice
import org.jetbrains.compose.resources.StringResource

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
    val title: StringResource,
    val description: StringResource,
)

data class ServerPermissionGroup(
    val title: StringResource,
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

    val member = StoatAPI.selfId
        ?.let { StoatAPI.members.getMember(server.id.orEmpty(), it) }
        ?: return true
    val ownTopRank = member
        .roles
        .orEmpty()
        .mapNotNull { server.roles?.get(it)?.rank }
        .minOrNull()
        ?: Double.MAX_VALUE

    return (role.rank ?: Double.MAX_VALUE) > ownTopRank
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
        Res.string.server_role_permissions_admin,
        listOf(
            ServerPermissionOption(
                PermissionBit.ManageChannel,
                Res.string.permission_manage_channels,
                Res.string.permission_manage_channels_description
            ),
            ServerPermissionOption(
                PermissionBit.ManageServer,
                Res.string.permission_manage_server,
                Res.string.permission_manage_server_description
            ),
            ServerPermissionOption(
                PermissionBit.ManagePermissions,
                Res.string.permission_manage_permissions,
                Res.string.permission_manage_permissions_description
            ),
            ServerPermissionOption(
                PermissionBit.ManageRole,
                Res.string.permission_manage_roles,
                Res.string.permission_manage_roles_description
            ),
            ServerPermissionOption(
                PermissionBit.ManageCustomisation,
                Res.string.permission_manage_customisation,
                Res.string.permission_manage_customisation_description
            ),
        ),
    ),
    ServerPermissionGroup(
        Res.string.server_role_permissions_members,
        listOf(
            ServerPermissionOption(
                PermissionBit.KickMembers,
                Res.string.permission_kick_members,
                Res.string.permission_kick_members_description
            ),
            ServerPermissionOption(
                PermissionBit.BanMembers,
                Res.string.permission_ban_members,
                Res.string.permission_ban_members_description
            ),
            ServerPermissionOption(
                PermissionBit.TimeoutMembers,
                Res.string.permission_timeout_members,
                Res.string.permission_timeout_members_description
            ),
            ServerPermissionOption(
                PermissionBit.AssignRoles,
                Res.string.permission_assign_roles,
                Res.string.permission_assign_roles_description
            ),
            ServerPermissionOption(
                PermissionBit.ChangeNickname,
                Res.string.permission_change_nickname,
                Res.string.permission_change_nickname_description
            ),
            ServerPermissionOption(
                PermissionBit.ManageNicknames,
                Res.string.permission_manage_nicknames,
                Res.string.permission_manage_nicknames_description
            ),
            ServerPermissionOption(
                PermissionBit.ChangeAvatar,
                Res.string.permission_change_avatar,
                Res.string.permission_change_avatar_description
            ),
            ServerPermissionOption(
                PermissionBit.RemoveAvatars,
                Res.string.permission_remove_avatars,
                Res.string.permission_remove_avatars_description
            ),
        ),
    ),
    ServerPermissionGroup(
        Res.string.server_role_permissions_channels,
        listOf(
            ServerPermissionOption(
                PermissionBit.ViewChannel,
                Res.string.permission_view_channels,
                Res.string.permission_view_channels_description
            ),
            ServerPermissionOption(
                PermissionBit.ReadMessageHistory,
                Res.string.permission_read_history,
                Res.string.permission_read_history_description
            ),
            ServerPermissionOption(
                PermissionBit.SendMessage,
                Res.string.permission_send_messages,
                Res.string.permission_send_messages_description
            ),
            ServerPermissionOption(
                PermissionBit.ManageMessages,
                Res.string.permission_manage_messages,
                Res.string.permission_manage_messages_description
            ),
            ServerPermissionOption(
                PermissionBit.ManageWebhooks,
                Res.string.permission_manage_webhooks,
                Res.string.permission_manage_webhooks_description
            ),
            ServerPermissionOption(
                PermissionBit.InviteOthers,
                Res.string.permission_invite_others,
                Res.string.permission_invite_others_description
            ),
        ),
    ),
    ServerPermissionGroup(
        Res.string.server_role_permissions_messaging,
        listOf(
            ServerPermissionOption(
                PermissionBit.SendEmbeds,
                Res.string.permission_send_embeds,
                Res.string.permission_send_embeds_description
            ),
            ServerPermissionOption(
                PermissionBit.UploadFiles,
                Res.string.permission_upload_files,
                Res.string.permission_upload_files_description
            ),
            ServerPermissionOption(
                PermissionBit.Masquerade,
                Res.string.permission_masquerade,
                Res.string.permission_masquerade_description
            ),
            ServerPermissionOption(
                PermissionBit.React,
                Res.string.permission_react,
                Res.string.permission_react_description
            ),
            ServerPermissionOption(
                PermissionBit.BypassSlowmode,
                Res.string.permission_bypass_slowmode,
                Res.string.permission_bypass_slowmode_description
            ),
        ),
    ),
    ServerPermissionGroup(
        Res.string.server_role_permissions_voice,
        listOf(
            ServerPermissionOption(
                PermissionBit.Connect,
                Res.string.permission_connect,
                Res.string.permission_connect_description
            ),
            ServerPermissionOption(
                PermissionBit.Speak,
                Res.string.permission_speak,
                Res.string.permission_speak_description
            ),
            ServerPermissionOption(
                PermissionBit.Video,
                Res.string.permission_video,
                Res.string.permission_video_description
            ),
            ServerPermissionOption(
                PermissionBit.MuteMembers,
                Res.string.permission_mute_members,
                Res.string.permission_mute_members_description
            ),
            ServerPermissionOption(
                PermissionBit.DeafenMembers,
                Res.string.permission_deafen_members,
                Res.string.permission_deafen_members_description
            ),
            ServerPermissionOption(
                PermissionBit.MoveMembers,
                Res.string.permission_move_members,
                Res.string.permission_move_members_description
            ),
            ServerPermissionOption(
                PermissionBit.Listen,
                Res.string.permission_listen,
                Res.string.permission_listen_description
            ),
        ),
    ),
    ServerPermissionGroup(
        Res.string.server_role_permissions_mentions,
        listOf(
            ServerPermissionOption(
                PermissionBit.MentionEveryone,
                Res.string.permission_mention_everyone,
                Res.string.permission_mention_everyone_description
            ),
            ServerPermissionOption(
                PermissionBit.MentionRoles,
                Res.string.permission_mention_roles,
                Res.string.permission_mention_roles_description
            ),
        ),
    ),
)

private val ChannelPermissionBits = setOf(
    PermissionBit.ManageChannel,
    PermissionBit.ManagePermissions,
    PermissionBit.ViewChannel,
    PermissionBit.ReadMessageHistory,
    PermissionBit.SendMessage,
    PermissionBit.ManageMessages,
    PermissionBit.ManageWebhooks,
    PermissionBit.InviteOthers,
    PermissionBit.SendEmbeds,
    PermissionBit.UploadFiles,
    PermissionBit.Masquerade,
    PermissionBit.React,
    PermissionBit.BypassSlowmode,
    PermissionBit.Connect,
    PermissionBit.Speak,
    PermissionBit.Video,
    PermissionBit.MuteMembers,
    PermissionBit.DeafenMembers,
    PermissionBit.MoveMembers,
    PermissionBit.Listen,
    PermissionBit.MentionEveryone,
    PermissionBit.MentionRoles,
)

val ChannelPermissionGroups = ServerPermissionGroups.mapNotNull { group ->
    group.copy(permissions = group.permissions.filter { it.bit in ChannelPermissionBits })
        .takeIf { it.permissions.isNotEmpty() }
}
