package chat.stoat.api.unreads

import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.ULID
import chat.stoat.api.routes.channel.ackChannel
import chat.stoat.api.routes.server.ackServer
import chat.stoat.api.routes.sync.syncUnreads
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.core.model.schemas.ChannelUnread
import chat.stoat.api.settings.NotificationSettingsProvider

class Unreads {
    private val hasLoaded = mutableStateOf(false)
    private val channels = mutableStateMapOf<String, ChannelUnread>()

    suspend fun sync() {
        channels.clear()
        channels.putAll(
            try {
                syncUnreads().associate {
                    it.id.channel to ChannelUnread(
                        id = it.id.channel,
                        last_id = it.last_id,
                        mentions = it.mentions
                    )
                }
            } catch (e: Exception) {
                Log.e("Unreads", "Failed to sync unreads", e)
                emptyMap()
            }
        )
        hasLoaded.value = true
    }

    fun getForChannel(channelId: String, serverId: String?): ChannelUnread? {
        if (!hasLoaded.value) return null
        if (NotificationSettingsProvider.isChannelMuted(channelId, serverId)) return null
        return channels[channelId]
    }

    fun hasUnread(channelId: String, lastMessageId: String, serverId: String?): Boolean {
        if (!hasLoaded.value) return false
        if (NotificationSettingsProvider.isChannelMuted(channelId, serverId)) return false
        return (channels[channelId]?.last_id?.compareTo(lastMessageId) ?: 0) < 0
    }

    fun serverHasUnread(serverId: String): Boolean {
        if (!hasLoaded.value) return false

        return StoatAPI.serverCache[serverId]?.channels?.any {
            val channel = StoatAPI.channelCache[it] ?: return@any false // Channel not found
            if (channel.channelType == ChannelType.VoiceChannel) return@any false // Channel is voice
            if (NotificationSettingsProvider.isChannelMuted(
                    it,
                    serverId
                )
            ) return@any false // Channel is muted
            hasUnread(it, channel.lastMessageID ?: "", serverId) // Channel has unread
        } == true // Null guard
    }

    suspend fun markAsRead(channelId: String, messageId: String, sync: Boolean = true) {
        if (!hasLoaded.value) return
        channels[channelId]?.let {
            channels[channelId] = it.copy(last_id = messageId)
        }
        if (sync) {
            ackChannel(channelId, messageId)
        }
    }

    fun processExternalAck(channelId: String, messageId: String) {
        channels[channelId]?.let {
            channels[channelId] = it.copy(last_id = messageId)
        }
    }

    suspend fun markServerAsRead(serverId: String, sync: Boolean = true) {
        if (!hasLoaded.value) return

        val server = StoatAPI.serverCache[serverId] ?: return
        server.channels?.forEach { channel ->
            channels[channel] = channels[channel]?.copy(last_id = ULID.makeNext()) ?: ChannelUnread(
                channel,
                ULID.makeNext()
            )
        }

        if (sync) {
            ackServer(serverId)
        }
    }

    fun getAllUnreads(): List<ChannelUnread> {
        if (!hasLoaded.value) return emptyList()
        return channels.values.toList()
    }

    fun removeChannels(channelIds: Collection<String>) {
        channelIds.forEach(channels::remove)
    }

    /**
     * Returns true if there are any unreads in any of the channels that are not muted.
     * **SLOW:** Run in a background coroutine.
     */
    fun hasAnyUnreads(): Boolean {
        if (!hasLoaded.value) return false

        for ((channelId, unread) in StoatAPI.channelCache) {
            if (channelId !in channels) continue
            if (NotificationSettingsProvider.isChannelMuted(channelId, unread.server)) continue
            if (hasUnread(channelId, unread.lastMessageID ?: "", unread.server)) {
                return true
            }
        }
        return false
    }

    /**
     * Returns the count of channels with unreads that are not muted.
     * **SLOW:** Run in a background coroutine.
     */
    fun countChannelsWithUnreads(): Int? {
        if (!hasLoaded.value) return null

        var count = 0
        for ((channelId, unread) in StoatAPI.channelCache) {
            if (channelId !in channels) continue
            if (NotificationSettingsProvider.isChannelMuted(channelId, unread.server)) continue
            if (hasUnread(channelId, unread.lastMessageID ?: "", unread.server)) {
                count++
            }
        }
        return count
    }

    fun clear() {
        channels.clear()
        hasLoaded.value = false
    }
}
