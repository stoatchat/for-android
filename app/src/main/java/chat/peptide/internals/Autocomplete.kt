package chat.peptide.internals

import chat.peptide.api.RevoltAPI
import chat.peptide.api.internals.PermissionBit
import chat.peptide.api.internals.Roles
import chat.peptide.api.internals.has
import chat.peptide.api.schemas.ChannelType
import chat.peptide.api.settings.FeatureFlags
import chat.peptide.composables.chat.AutocompleteSuggestion

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
            RevoltAPI.emojiCache.values.filter {
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
        val channel = RevoltAPI.channelCache[channelId] ?: return emptyList()

        val member = serverId?.let { RevoltAPI.members.getMember(serverId, RevoltAPI.selfId ?: "") }
        val massMentionSuggestions = listOf("everyone", "online")
            .filter { it.startsWith(query, ignoreCase = true) }

        val selfPermissions = RevoltAPI.channelCache[channelId]?.let { ch ->
            Roles.permissionFor(
                ch,
                RevoltAPI.userCache[RevoltAPI.selfId],
                member
            )
        }

        return when (channel.channelType) {
            ChannelType.DirectMessage -> {
                val otherUser = channel.recipients?.find { it != RevoltAPI.selfId }
                if (otherUser != null) {
                    val user = RevoltAPI.userCache[otherUser]
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
                    channel.recipients?.mapNotNull { RevoltAPI.userCache[it] } ?: emptyList()
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
                val user = RevoltAPI.userCache[RevoltAPI.selfId]
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
                    if (selfPermissions has PermissionBit.MentionRoles && FeatureFlags.massMentionsGranted) RevoltAPI.serverCache[serverId]?.roles
                        ?: emptyMap() else emptyMap()
                val byNickname = RevoltAPI.members.filterNamesFor(serverId, query)
                    .map { m -> m to RevoltAPI.userCache[m.id?.user] }.filter { (_, u) ->
                        u != null
                    }.map { (m, u) ->
                        m to u!!
                    }
                val byUsername = RevoltAPI.userCache.values.filter {
                    it.username?.contains(
                        query,
                        ignoreCase = true
                    ) == true
                }.mapNotNull {
                    it.id?.let { _ ->
                        RevoltAPI.members.getMember(
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
        val server = RevoltAPI.serverCache[serverId] ?: return emptyList()
        val channels = server.channels?.mapNotNull { RevoltAPI.channelCache[it] } ?: emptyList()

        return channels.filter { it.name?.contains(query, ignoreCase = true) == true }.map {
            AutocompleteSuggestion.Channel(
                it,
                query
            )
        }
    }
}