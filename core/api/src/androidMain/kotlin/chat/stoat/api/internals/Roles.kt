package chat.stoat.api.internals

import chat.stoat.api.StoatAPI
import chat.stoat.core.model.schemas.Channel
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.core.model.schemas.Member
import chat.stoat.core.model.schemas.PermissionDescription
import chat.stoat.core.model.schemas.Role
import chat.stoat.core.model.schemas.Server
import chat.stoat.core.model.schemas.User
import kotlinx.datetime.Clock

object Roles {
    // lowest rank = highest role
    private fun highestRoleWithPredicate(roles: List<Role?>, predicate: (Role) -> Boolean): Role? {
        return roles.filter { role ->
            if (role == null) return@filter false
            predicate(role)
        }.minByOrNull { role ->
            role?.rank ?: 0.0
        }
    }

    fun resolveHighestRole(
        serverId: String,
        userId: String,
        withColour: Boolean = false,
        hoisted: Boolean = false
    ): Role? {
        val server = StoatAPI.serverCache[serverId] ?: return null
        val member = StoatAPI.members.getMember(serverId, userId) ?: return null

        val roles = member.roles?.map { roleId ->
            server.roles?.get(roleId)
        } ?: return null

        return highestRoleWithPredicate(roles) { role ->
            val hoistPredicate = if (hoisted) (role.hoist == true) else true
            val colourPredicate = if (withColour) (role.colour != null) else true

            hoistPredicate && colourPredicate
        }
    }

    fun inOrder(serverId: String, predicate: (Role) -> Boolean): List<Role> {
        val server = StoatAPI.serverCache[serverId] ?: return emptyList()

        return server.roles?.values?.filter(predicate)?.sortedBy { it.rank } ?: emptyList()
    }

    fun permissionFor(server: Server, member: Member): Long {
        val user = StoatAPI.userCache[member.id?.user] ?: return 0L

        if (user.privileged == true) return PermissionBit.GrantAllSafe.value
        if (server.owner == member.id?.user) return PermissionBit.GrantAllSafe.value

        var calculated = server.defaultPermissions ?: BitDefaults.Server

        member.roles?.forEach { roleId ->
            val role = server.roles?.get(roleId) ?: return@forEach
            val permissions = role.permissions ?: PermissionDescription(0, 0)

            calculated = calculated or permissions.a and permissions.d.inv()
        }

        if (member.timeoutTimestamp()?.let { it > Clock.System.now() } == true) {
            calculated = calculated and BitDefaults.AllowedInTimeout
        }

        return calculated
    }

    // TODO may not be exactly accurate
    // See https://github.com/revoltchat/revolt.js/blob/2ba023c879b2a53f9a3cc7042e6721c28dd970ba/src/permissions/calculator.ts#L80-L158
    fun permissionFor(channel: Channel, user: User? = null, member: Member? = null): Long {
        return when (channel.channelType) {
            ChannelType.SavedMessages -> BitDefaults.SavedMessages

            ChannelType.DirectMessage -> BitDefaults.DirectMessages
            ChannelType.Group -> if (channel.owner == user?.id) PermissionBit.GrantAllSafe.value else BitDefaults.DirectMessages

            ChannelType.TextChannel, ChannelType.VoiceChannel -> {
                val server = StoatAPI.serverCache[channel.server]
                // FIXME this is a stupid patch to prevent it from showing "no permission" on a channel on launch
                    ?: return PermissionBit.GrantAllSafe.value

                if (server.owner == user?.id) return PermissionBit.GrantAllSafe.value

                val chMember = member ?: StoatAPI.members.getMember(
                    server.id ?: return 0L,
                    user?.id ?: return 0L
                ) ?: return 0L

                var calculated = permissionFor(server, chMember)

                if (channel.defaultPermissions != null) {
                    calculated =
                        calculated or channel.defaultPermissions!!.a and channel.defaultPermissions!!.d.inv()
                }

                if (chMember.roles?.isNotEmpty() == true) {
                    chMember.roles!!.forEach { roleId ->
                        val override = channel.rolePermissions?.get(roleId) ?: return@forEach
                        calculated = calculated or override.a and override.d.inv()
                    }
                }

                if (chMember.timeoutTimestamp()?.let { it > Clock.System.now() } == true) {
                    calculated = calculated and BitDefaults.AllowedInTimeout
                }

                return calculated
            }

            null -> 0L
        }
    }
}
