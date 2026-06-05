package chat.stoat.internals

import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.internals.Roles
import chat.stoat.api.internals.has
import chat.stoat.composables.chat.AutocompleteSuggestion
import chat.stoat.core.model.schemas.ChannelType

object Autocomplete {
    private val emojiImpl = EmojiImpl()

    fun emoji(query: String): List<AutocompleteSuggestion.Emoji> {
        val unicodeResults = emojiImpl.shortcodeContains(query).map {
            AutocompleteSuggestion.Emoji(
                it.shortcodes.find { shortcode -> shortcode.contains(query, ignoreCase = true) }
                    ?: it.shortcodes.first(),
                it.base.joinToString("") { s -> String(Character.toChars(s.toInt())) },
                null,
                query
            )
        }.distinctBy { it.shortcode }

        val customResults =
            StoatAPI.emojiCache.values.filter {
                it.name?.contains(query, ignoreCase = true) ?: false
            }.mapNotNull {
                if (it.name != null) {
                    AutocompleteSuggestion.Emoji(
                        ":${it.id}:",
                        null,
                        it,
                        query
                    )
                } else {
                    null
                }
            }.distinctBy { it.custom?.id }

        return (unicodeResults + customResults)
    }

    fun userOrRole(
        channelId: String,
        serverId: String? = null,
        query: String
    ): List<AutocompleteSuggestion> {
        val channel = StoatAPI.channelCache[channelId] ?: return emptyList()

        val member = serverId?.let { StoatAPI.members.getMember(serverId, StoatAPI.selfId ?: "") }
        val massMentionSuggestions = listOf("everyone", "online")
            .filter { it.startsWith(query, ignoreCase = true) }
        val hereIsOnlineMnemonic = if (query == "here") listOf("online") else emptyList()

        val selfPermissions = StoatAPI.channelCache[channelId]?.let { ch ->
            Roles.permissionFor(
                ch,
                StoatAPI.userCache[StoatAPI.selfId],
                member
            )
        }

        return when (channel.channelType) {
            ChannelType.DirectMessage -> {
                val otherUser = channel.recipients?.find { it != StoatAPI.selfId }
                if (otherUser != null) {
                    val user = StoatAPI.userCache[otherUser]
                    if (user != null && user.username?.contains(query, ignoreCase = true) == true) {
                        listOf(
                            AutocompleteSuggestion.User(
                                user,
                                null,
                                query
                            )
                        )
                    } else {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            }

            ChannelType.Group -> {
                val users =
                    channel.recipients?.mapNotNull { StoatAPI.userCache[it] } ?: emptyList()
                users
                    .filter { it.username?.contains(query, ignoreCase = true) ?: false }
                    .map {
                        AutocompleteSuggestion.User(
                            it,
                            null,
                            query
                        )
                    }
            }

            ChannelType.SavedMessages -> {
                val user = StoatAPI.userCache[StoatAPI.selfId]
                return if (user != null && user.username?.contains(
                        query,
                        ignoreCase = true
                    ) == true
                ) {
                    listOf(
                        AutocompleteSuggestion.User(
                            user,
                            null,
                            query
                        )
                    )
                } else {
                    emptyList()
                }
            }

            ChannelType.TextChannel, ChannelType.VoiceChannel -> {
                if (serverId == null) return emptyList()
                if (query.length < 2) return emptyList()

                val roles =
                    if (selfPermissions has PermissionBit.MentionRoles) StoatAPI.serverCache[serverId]?.roles
                        ?: emptyMap() else emptyMap()
                val byNickname = StoatAPI.members.filterNamesFor(serverId, query)
                    .map { m -> m to StoatAPI.userCache[m.id?.user] }.filter { (_, u) ->
                        u != null
                    }.map { (m, u) ->
                        m to u!!
                    }
                val byUsername = StoatAPI.userCache.values.filter {
                    it.username?.contains(
                        query,
                        ignoreCase = true
                    ) == true
                }.mapNotNull {
                    it.id?.let { id ->
                        StoatAPI.members.getMember(
                            serverId,
                            id
                        ) to it
                    }
                }.filter { (member, _) ->
                    member != null
                }.map { (member, user) ->
                    member!! to user
                }

                val allUsers = (byNickname + byUsername).distinctBy { it.first.id }
                val rolesByName =
                    roles.filter { it.value.name?.contains(query, ignoreCase = true) == true }
                        .map { it.value to it.key }


                (allUsers.map {
                    AutocompleteSuggestion.User(
                        it.second,
                        it.first,
                        query
                    )
                } + rolesByName.map { (role, roleId) ->
                    AutocompleteSuggestion.Role(
                        role,
                        roleId,
                        query
                    )
                })
                    .sortedBy {
                        when (it) {
                            is AutocompleteSuggestion.User -> it.user.username
                            is AutocompleteSuggestion.Role -> it.role.name
                            else -> ""
                        }
                    }
            }

            null -> emptyList()
        } + if (selfPermissions has PermissionBit.MentionEveryone) (massMentionSuggestions + hereIsOnlineMnemonic).map { mention ->
            AutocompleteSuggestion.MassMention(mention)
        } else listOf()
    }

    fun channel(
        serverId: String,
        query: String
    ): List<AutocompleteSuggestion.Channel> {
        val server = StoatAPI.serverCache[serverId] ?: return emptyList()
        val channels = server.channels?.mapNotNull { StoatAPI.channelCache[it] } ?: emptyList()

        return channels.filter { it.name?.contains(query, ignoreCase = true) == true }.map {
            AutocompleteSuggestion.Channel(
                it,
                query
            )
        }
    }
}