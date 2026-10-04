package chat.stoat.api.internals

enum class PermissionBit(val value: Long) {
    // * Generic permissions
    ManageChannel(1L shl 0),
    ManageServer(1L shl 1),
    ManagePermissions(1L shl 2),
    ManageRole(1L shl 3),
    ManageCustomisation(1L shl 4),

    // % 1 bit reserved

    // * Member permissions
    KickMembers(1L shl 6),
    BanMembers(1L shl 7),
    TimeoutMembers(1L shl 8),
    AssignRoles(1L shl 9),
    ChangeNickname(1L shl 10),
    ManageNicknames(1L shl 11),
    ChangeAvatar(1L shl 12),
    RemoveAvatars(1L shl 13),

    // % 7 bits reserved

    // * Channel permissions
    ViewChannel(1L shl 20),
    ReadMessageHistory(1L shl 21),
    SendMessage(1L shl 22),
    ManageMessages(1L shl 23),
    ManageWebhooks(1L shl 24),
    InviteOthers(1L shl 25),
    SendEmbeds(1L shl 26),
    UploadFiles(1L shl 27),
    Masquerade(1L shl 28),
    React(1L shl 29),

    // * Voice permissions
    Connect(1L shl 30),
    Speak(1L shl 31),
    Video(1L shl 32),
    MuteMembers(1L shl 33),
    DeafenMembers(1L shl 34),
    MoveMembers(1L shl 35),
    Listen(1L shl 36),

    // % 1 bit reserved

    // * Channel permissions cont.
    MentionEveryone(1L shl 37),
    MentionRoles(1L shl 38),
    BypassSlowmode(1L shl 39),

    // * Misc. permissions
    // % Bits 40 to 52: free area
    // % Bits 53 to 64: do not use

    // * Grant all permissions
    GrantAllSafe(0x000FFFFFFFFFFFFFL),
    GrantAll(Long.MAX_VALUE);

    operator fun plus(other: PermissionBit): Long {
        return this.value or other.value
    }

    operator fun plus(other: Long): Long {
        return this.value or other
    }
}

operator fun Long.plus(other: PermissionBit): Long {
    return this or other.value
}

fun Long.hasPermission(permission: PermissionBit): Boolean {
    return this and permission.value == permission.value
}

infix fun Long?.has(permission: PermissionBit): Boolean {
    return this != null && this.hasPermission(permission)
}

object BitDefaults {
    val AllowedInTimeout =
        PermissionBit.ViewChannel + PermissionBit.ReadMessageHistory

    val ViewOnly =
        PermissionBit.ViewChannel + PermissionBit.ReadMessageHistory

    val Default =
        ViewOnly +
                PermissionBit.SendMessage +
                PermissionBit.InviteOthers +
                PermissionBit.SendEmbeds +
                PermissionBit.UploadFiles +
                PermissionBit.Connect +
                PermissionBit.Speak +
                PermissionBit.Listen

    val SavedMessages =
        PermissionBit.GrantAllSafe.value

    val DirectMessages =
        Default +
                PermissionBit.ManageChannel +
                PermissionBit.React

    val Server =
        Default +
                PermissionBit.React +
                PermissionBit.ChangeNickname +
                PermissionBit.ChangeAvatar

    val Webhook =
        PermissionBit.SendMessage +
                PermissionBit.SendEmbeds +
                PermissionBit.Masquerade +
                PermissionBit.React
}
