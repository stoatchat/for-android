package chat.stoat.internals.server

import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.internals.hasPermission

data class ServerIdentityCapabilities(
    val canChangeNickname: Boolean,
    val canChangePronouns: Boolean,
    val canSetAvatar: Boolean,
    val canRemoveAvatar: Boolean,
)

fun serverIdentityCapabilities(
    targetUserId: String,
    selfUserId: String?,
    permissions: Long?,
): ServerIdentityCapabilities {
    val isSelf = targetUserId == selfUserId

    return if (isSelf) {
        val canChangeAvatar =
            permissions?.hasPermission(PermissionBit.ChangeAvatar) == true
        ServerIdentityCapabilities(
            canChangeNickname =
                permissions?.hasPermission(PermissionBit.ChangeNickname) == true,
            canChangePronouns = true,
            canSetAvatar = canChangeAvatar,
            canRemoveAvatar = canChangeAvatar,
        )
    } else {
        ServerIdentityCapabilities(
            canChangeNickname =
                permissions?.hasPermission(PermissionBit.ManageNicknames) == true,
            canChangePronouns = false,
            canSetAvatar = false,
            canRemoveAvatar =
                permissions?.hasPermission(PermissionBit.RemoveAvatars) == true,
        )
    }
}
