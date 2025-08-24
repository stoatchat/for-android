package chat.peptide.api.internals

import chat.peptide.api.PeptideAPI
import chat.peptide.api.internals.SpecialUsers.PLATFORM_MODERATION_USER
import chat.peptide.api.schemas.Channel
import chat.peptide.api.schemas.ChannelType

object DirectMessages {
    fun unreadDMs(): List<Channel> {
        return PeptideAPI.channelCache.values
            .filter {
                it.channelType in listOf(
                    ChannelType.DirectMessage, ChannelType.Group
                ) && it.active == true && it.lastMessageID != null
            }
            .filter {
                it.id?.let { id ->
                    PeptideAPI.unreads.hasUnread(
                        id,
                        it.lastMessageID!!,
                        serverId = null
                    )
                } ?: false
            }
    }

    fun hasPlatformModerationDM(): Boolean {
        return unreadDMs().any {
            it.channelType == ChannelType.DirectMessage &&
                    it.recipients?.contains(PLATFORM_MODERATION_USER) ?: false
        }
    }

    fun getPlatformModerationDM(): Channel? {
        return unreadDMs().firstOrNull {
            it.channelType == ChannelType.DirectMessage &&
                    it.recipients?.contains(PLATFORM_MODERATION_USER) ?: false
        }
    }
}
