package chat.stoat.api.internals

import chat.stoat.api.StoatAPI
import chat.stoat.core.model.schemas.Channel
import chat.stoat.core.model.schemas.Server
import chat.stoat.core.model.schemas.User

sealed class CategorisedChannelList {
    data class Channel(val channel: chat.stoat.core.model.schemas.Channel) :
        CategorisedChannelList()

    data class Category(val category: chat.stoat.core.model.schemas.Category) :
        CategorisedChannelList()
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
            ?: StoatAPI.userCache[channel.recipients?.first { u -> u != StoatAPI.selfId }]?.let {
                User.resolveDefaultName(
                    it
                )
            }
    }

    fun resolveDMPartner(channel: Channel): String? {
        return channel.recipients?.firstOrNull { u -> u != StoatAPI.selfId }
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
                    StoatAPI.channelCache[it]?.let { it1 ->
                        CategorisedChannelList.Channel(it1)
                    }
                } ?: emptyList()
        output.addAll(uncategorised)

        val categories =
            server.categories?.map { CategorisedChannelList.Category(it) } ?: emptyList()
        categories.forEach {
            output.add(it)
            val channels = it.category.channels?.mapNotNull { c ->
                StoatAPI.channelCache[c]?.let { it1 ->
                    CategorisedChannelList.Channel(it1)
                }
            } ?: emptyList()
            output.addAll(channels)
        }

        return output
    }
}
