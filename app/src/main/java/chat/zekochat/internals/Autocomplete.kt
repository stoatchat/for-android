package chat.zekochat.internals

import chat.zekochat.api.PeptideAPI
import chat.zekochat.api.internals.PermissionBit
import chat.zekochat.api.internals.Roles
import chat.zekochat.api.internals.has
import chat.zekochat.api.schemas.ChannelType
import chat.zekochat.api.settings.FeatureFlags
import chat.zekochat.composables.chat.AutocompleteSuggestion

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
            PeptideAPI.emojiCache.values.filter {
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
        val channel = PeptideAPI.channelCache[channelId] ?: return emptyList()

        val member = serverId?.let { PeptideAPI.members.getMember(serverId, PeptideAPI.selfId ?: "") }
        val massMentionSuggestions = listOf("everyone", "online")
            .filter { it.startsWith(query, ignoreCase = true) }

        val selfPermissions = PeptideAPI.channelCache[channelId]?.let { ch ->
            Roles.permissionFor(
                ch,
                PeptideAPI.userCache[PeptideAPI.selfId],
                member
            )
        }

        return when (channel.channelType) {
            ChannelType.DirectMessage -> {
                val otherUser = channel.recipients?.find { it != PeptideAPI.selfId }
                if (otherUser != null) {
                    val user = PeptideAPI.userCache[otherUser]
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
                    channel.recipients?.mapNotNull { PeptideAPI.userCache[it] } ?: emptyList()
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
                val user = PeptideAPI.userCache[PeptideAPI.selfId]
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
                    if (selfPermissions has PermissionBit.MentionRoles && FeatureFlags.massMentionsGranted) PeptideAPI.serverCache[serverId]?.roles
                        ?: emptyMap() else emptyMap()
                val byNickname = PeptideAPI.members.filterNamesFor(serverId, query)
                    .map { m -> m to PeptideAPI.userCache[m.id?.user] }.filter { (_, u) ->
                        u != null
                    }.map { (m, u) ->
                        m to u!!
                    }
                val byUsername = PeptideAPI.userCache.values.filter {
                    it.username?.contains(
                        query,
                        ignoreCase = true
                    ) == true
                }.mapNotNull {
                    it.id?.let { _ ->
                        PeptideAPI.members.getMember(
                            serverId,
                            it.id
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
        } + if (selfPermissions has PermissionBit.MentionEveryone && FeatureFlags.massMentionsGranted) massMentionSuggestions.map { mention ->
            AutocompleteSuggestion.MassMention(mention)
        } else listOf()
    }

    fun channel(
        serverId: String,
        query: String
    ): List<AutocompleteSuggestion.Channel> {
        val server = PeptideAPI.serverCache[serverId] ?: return emptyList()
        val channels = server.channels?.mapNotNull { PeptideAPI.channelCache[it] } ?: emptyList()

        return channels.filter { it.name?.contains(query, ignoreCase = true) == true }.map {
            AutocompleteSuggestion.Channel(
                it,
                query
            )
        }
    }
}