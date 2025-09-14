package chat.zekochat.markdown.jbm

import chat.zekochat.api.PeptideAPI

object MentionResolver {
    /**
     * Resolves a user mention to its fancy representation.
     * Note that this uses the new format without a leading @ unless the user is not found.
     *
     * @param userId The user ID to resolve.
     * @param serverId The server ID to resolve the user in.
     * @return The resolved user mention.
     */
    fun resolveUser(userId: String, serverId: String? = null): String {
        val maybeMember = serverId?.let { PeptideAPI.members.getMember(serverId, userId) }
        return maybeMember?.nickname
            ?: PeptideAPI.userCache[userId]?.username
            ?: "<@$userId>"
    }

    /**
     * Resolves a channel mention to its fancy representation.
     *
     * @param channelId The channel ID to resolve.
     * @param serverId The server ID to resolve the channel in.
     * @return The resolved channel mention.
     */
    fun resolveChannel(channelId: String): String {
        val channel = PeptideAPI.channelCache[channelId]
        return channel?.name?.let { name -> "#$name" } ?: "<#$channelId>"
    }
}