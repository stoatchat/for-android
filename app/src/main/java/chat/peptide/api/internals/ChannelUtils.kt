package chat.peptide.api.internals

import chat.peptide.api.RevoltAPI
import chat.peptide.api.schemas.Channel
import chat.peptide.api.schemas.Server
import chat.peptide.api.schemas.User

sealed class CategorisedChannelList {
    data class Channel(val channel: chat.peptide.api.schemas.Channel) : CategorisedChannelList()
    data class Category(val category: chat.peptide.api.schemas.Category) : CategorisedChannelList()
}

object ChannelUtils {
    /**
     * Resolves the name of a channel, preferring the name of the channel itself, then the name of the first recipient.
     * @param channel The channel to resolve the name of.
     * @return The name of the channel, or the name of the first recipient if the channel is a DM.
     * @see User.resolveDefaultName
     */
    fun resolveName(channel: Channel): String? {
        return channel.name
            ?: RevoltAPI.userCache[channel.recipients?.first { u -> u != RevoltAPI.selfId }]?.let {
                User.resolveDefaultName(
                    it
                )
            }
    }

    fun resolveDMPartner(channel: Channel): String? {
        return channel.recipients?.firstOrNull { u -> u != RevoltAPI.selfId }
    }

    fun categoriseServerFlat(server: Server): List<CategorisedChannelList> {
        val output = mutableListOf<CategorisedChannelList>()

        val uncategorised =
            server.channels?.filter { c ->
                server.categories?.none { cat ->
                    cat.channels?.contains(
                        c
                    ) == true
                } ?: true
            }
                ?.mapNotNull {
                    RevoltAPI.channelCache[it]?.let { it1 ->
                        CategorisedChannelList.Channel(it1)
                    }
                } ?: emptyList()
        output.addAll(uncategorised)

        val categories =
            server.categories?.map { CategorisedChannelList.Category(it) } ?: emptyList()
        categories.forEach {
            output.add(it)
            val channels = it.category.channels?.mapNotNull { c ->
                RevoltAPI.channelCache[c]?.let { it1 ->
                    CategorisedChannelList.Channel(it1)
                }
            } ?: emptyList()
            output.addAll(channels)
        }

        return output
    }
}
