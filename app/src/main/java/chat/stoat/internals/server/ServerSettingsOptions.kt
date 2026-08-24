package chat.stoat.internals.server

import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.internals.hasPermission

enum class ServerSettingsOption {
    Overview,
    Emojis,
    Members,
    Roles,
    Invites,
    Bans,
    DeleteServer,
}

fun availableServerSettingsOptions(
    permissions: Long,
    isOwner: Boolean,
): List<ServerSettingsOption> = buildList {
    fun has(permission: PermissionBit) = isOwner || permissions.hasPermission(permission)

    if (has(PermissionBit.ManageServer)) {
        add(ServerSettingsOption.Overview)
    }

    if (has(PermissionBit.ManageCustomisation)) {
        add(ServerSettingsOption.Emojis)
    }

    if (
        isOwner ||
        permissions.hasPermission(PermissionBit.AssignRoles) ||
        permissions.hasPermission(PermissionBit.BanMembers) ||
        permissions.hasPermission(PermissionBit.KickMembers) ||
        permissions.hasPermission(PermissionBit.ManageNicknames) ||
        permissions.hasPermission(PermissionBit.RemoveAvatars)
    ) {
        add(ServerSettingsOption.Members)
    }

    if (has(PermissionBit.ManageRole) || has(PermissionBit.ManagePermissions)) {
        add(ServerSettingsOption.Roles)
    }

    if (has(PermissionBit.ManageServer)) {
        add(ServerSettingsOption.Invites)
    }

    if (has(PermissionBit.BanMembers)) {
        add(ServerSettingsOption.Bans)
    }

    if (isOwner) {
        add(ServerSettingsOption.DeleteServer)
    }
}
