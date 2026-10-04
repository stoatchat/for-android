package chat.stoat.api.realtime

import android.os.SystemClock
import androidx.compose.runtime.mutableStateOf
import chat.stoat.api.StoatAPI
import chat.stoat.api.StoatAPIHost
import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatJson
import chat.stoat.api.internals.ActiveSlowmode
import chat.stoat.api.realtime.frames.receivable.AnyFrame
import chat.stoat.api.realtime.frames.receivable.BulkFrame
import chat.stoat.api.realtime.frames.receivable.ChannelAckFrame
import chat.stoat.api.realtime.frames.receivable.ChannelDeleteFrame
import chat.stoat.api.realtime.frames.receivable.ChannelStartTypingFrame
import chat.stoat.api.realtime.frames.receivable.ChannelStopTypingFrame
import chat.stoat.api.realtime.frames.receivable.ChannelUpdateFrame
import chat.stoat.api.realtime.frames.receivable.EmojiDeleteFrame
import chat.stoat.api.realtime.frames.receivable.MessageAppendFrame
import chat.stoat.api.realtime.frames.receivable.MessageDeleteFrame
import chat.stoat.api.realtime.frames.receivable.MessageFrame
import chat.stoat.api.realtime.frames.receivable.MessageReactFrame
import chat.stoat.api.realtime.frames.receivable.MessageUpdateFrame
import chat.stoat.api.realtime.frames.receivable.PongFrame
import chat.stoat.api.realtime.frames.receivable.ReadyFrame
import chat.stoat.api.realtime.frames.receivable.ServerCreateFrame
import chat.stoat.api.realtime.frames.receivable.ServerDeleteFrame
import chat.stoat.api.realtime.frames.receivable.ServerMemberJoinFrame
import chat.stoat.api.realtime.frames.receivable.ServerMemberLeaveFrame
import chat.stoat.api.realtime.frames.receivable.ServerMemberUpdateFrame
import chat.stoat.api.realtime.frames.receivable.ServerRoleDeleteFrame
import chat.stoat.api.realtime.frames.receivable.ServerRoleRanksUpdateFrame
import chat.stoat.api.realtime.frames.receivable.ServerRoleUpdateFrame
import chat.stoat.api.realtime.frames.receivable.ServerUpdateFrame
import chat.stoat.api.realtime.frames.receivable.UserMoveVoiceChannelFrame
import chat.stoat.api.realtime.frames.receivable.UserRelationshipFrame
import chat.stoat.api.realtime.frames.receivable.UserSlowmodesFrame
import chat.stoat.api.realtime.frames.receivable.UserUpdateFrame
import chat.stoat.api.realtime.frames.receivable.UserVoiceStateUpdateFrame
import chat.stoat.api.realtime.frames.receivable.VoiceChannelJoinFrame
import chat.stoat.api.realtime.frames.receivable.VoiceChannelLeaveFrame
import chat.stoat.api.realtime.frames.receivable.VoiceChannelMoveFrame
import chat.stoat.api.realtime.frames.sendable.AuthorizationFrame
import chat.stoat.api.realtime.frames.sendable.BeginTypingFrame
import chat.stoat.api.realtime.frames.sendable.EndTypingFrame
import chat.stoat.api.realtime.frames.sendable.PingFrame
import chat.stoat.api.routes.server.fetchMember
import chat.stoat.api.routes.sync.SyncedSetting
import chat.stoat.api.settings.LoadedSettings
import chat.stoat.api.settings.SyncedSettings
import chat.stoat.core.model.data.STOAT_WEBSOCKET
import chat.stoat.core.model.schemas.Channel
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.core.model.schemas.Emoji
import chat.stoat.core.model.schemas.Role
import chat.stoat.core.model.util.ChannelVoiceState
import chat.stoat.persistence.Database
import co.touchlab.kermit.Logger
import io.ktor.client.plugins.websocket.ws
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

enum class DisconnectionState {
    Disconnected,
    Reconnecting,
    Connected
}

sealed class RealtimeSocketFrames {
    data object Reconnected : RealtimeSocketFrames()
}

object RealtimeSocket {
    val database = Database(StoatAPIHost.storage.sqlDriver)
    var socket: WebSocketSession? = null

    @Volatile
    private var lastFrameAtElapsedRealtime: Long? = null

    private var _disconnectionState = mutableStateOf(DisconnectionState.Reconnecting)
    val disconnectionState: DisconnectionState
        get() = _disconnectionState.value

    fun updateDisconnectionState(state: DisconnectionState) {
        _disconnectionState.value = state
    }

    suspend fun connect(token: String, onReady: () -> Unit) {
        if (disconnectionState == DisconnectionState.Connected) {
            Logger.d { "Already connected to websocket. Refusing to connect again." }
            return
        }

        socket?.close(CloseReason(CloseReason.Codes.NORMAL, "Reconnecting to websocket."))

        var activeSocket: WebSocketSession? = null
        try {
            StoatHttp.ws(STOAT_WEBSOCKET) {
                activeSocket = this
                socket = this

                Logger.d { "Connected to websocket transport." }

                // Send authorization frame
                val authFrame = AuthorizationFrame("Authenticate", token)
                val authFrameString =
                    StoatJson.encodeToString(AuthorizationFrame.serializer(), authFrame)

                Logger.d {
                    "Sending authorization frame: ${
                        authFrameString.replace(
                            token,
                            "X".repeat(token.length)
                        )
                    }"
                }
                send(StoatJson.encodeToString(AuthorizationFrame.serializer(), authFrame))

                var connectionReady = false
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        lastFrameAtElapsedRealtime = SystemClock.elapsedRealtime()
                        val frameString = frame.readText()
                        try {
                            val frameType =
                                StoatJson.decodeFromString(AnyFrame.serializer(), frameString).type

                            if (!connectionReady && isConnectionReadyFrame(
                                    frameType,
                                    frameString
                                )
                            ) {
                                connectionReady = true
                                updateDisconnectionState(DisconnectionState.Connected)
                                pushReconnectEvent()
                                onReady()
                                Logger.d { "WebSocket authenticated." }
                            }

                            handleFrame(frameType, frameString)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Logger.e(e) { "Failed to handle frame: $frameString" }
                        }
                    }
                }
            }
        } finally {
            if (activeSocket == null || socket === activeSocket) {
                socket = null
                lastFrameAtElapsedRealtime = null
                updateDisconnectionState(DisconnectionState.Disconnected)
                Logger.d { "WebSocket disconnected." }
            }
        }
    }

    fun isConnectionStale(nowElapsedRealtime: Long = SystemClock.elapsedRealtime()): Boolean {
        if (disconnectionState != DisconnectionState.Connected) return false
        val lastFrameAt = lastFrameAtElapsedRealtime ?: return true
        return nowElapsedRealtime - lastFrameAt > STALE_CONNECTION_THRESHOLD.inWholeMilliseconds
    }

    private fun isConnectionReadyFrame(type: String, rawFrame: String): Boolean {
        if (type == "Authenticated" || type == "Ready") return true
        if (type != "Bulk") return false

        return StoatJson.decodeFromString(BulkFrame.serializer(), rawFrame).v.any { frame ->
            val frameType = StoatJson.decodeFromString(AnyFrame.serializer(), frame.toString()).type
            frameType == "Authenticated" || frameType == "Ready"
        }
    }

    suspend fun sendPing() {
        if (disconnectionState != DisconnectionState.Connected) return

        val pingPacket = PingFrame("Ping", System.currentTimeMillis())
        socket?.send(StoatJson.encodeToString(PingFrame.serializer(), pingPacket))
        Logger.d { "Sent ping frame with ${pingPacket.data}" }
    }

    private suspend fun handleFrame(type: String, rawFrame: String) {
        when (type) {
            "Pong" -> {
                val pongFrame = StoatJson.decodeFromString(PongFrame.serializer(), rawFrame)
                Logger.d { "Received pong frame for ${pongFrame.data}" }
            }

            "Bulk" -> {
                val bulkFrame = StoatJson.decodeFromString(BulkFrame.serializer(), rawFrame)
                Logger.d { "Received bulk frame with ${bulkFrame.v.size} sub-frames." }
                bulkFrame.v.forEach { subFrame ->
                    val subFrameType =
                        StoatJson.decodeFromString(AnyFrame.serializer(), subFrame.toString()).type
                    handleFrame(subFrameType, subFrame.toString())
                }
            }

            "Ready" -> {
                val readyFrame = StoatJson.decodeFromString(ReadyFrame.serializer(), rawFrame)
                StoatAPI.userSlowmodeCache.clear()

                Logger.d {
                    "Received ready frame with ${readyFrame.users.size} users, " +
                            "${readyFrame.servers.size} servers, " +
                            "${readyFrame.channels.size} channels, " +
                            "${readyFrame.emojis.size} emojis, " +
                            "and ${readyFrame.voiceStates.size} voice states."
                }

                Logger.d { "Adding users to cache." }
                val userMap = readyFrame.users.associateBy { it.id!! }
                StoatAPI.userCache.putAll(userMap)

                Logger.d { "Adding servers to cache." }
                val serverMap = readyFrame.servers.associateBy { it.id!! }
                StoatAPI.serverCache.putAll(serverMap)

                // Cache servers in persistent local database
                readyFrame.servers.map {
                    if (it.id == null || it.owner == null || it.name == null) {
                        return@map
                    }

                    database.serverQueries.upsert(
                        it.id!!,
                        it.owner!!,
                        it.name!!,
                        it.description,
                        it.icon?.id,
                        it.banner?.id,
                        it.flags
                    )
                }

                // Remove servers that are not in the ready frame
                val serversThatExist = readyFrame.servers.mapNotNull { it.id }
                val serversInDatabase = database.serverQueries.selectAllIds().executeAsList()
                val serversToDelete = serversInDatabase.filter { it !in serversThatExist }

                serversToDelete.forEach {
                    database.serverQueries.delete(it)
                    Logger.d {
                        "Deleted server $it from local database due to not being in ready frame."
                    }
                    // Conversely, remove the server from the API state
                    StoatAPI.serverCache.remove(it)
                }

                Logger.d { "Adding channels to cache." }
                val channelMap = readyFrame.channels.associateBy { it.id!! }
                StoatAPI.channelCache.putAll(channelMap)

                // Cache channels in persistent local database
                readyFrame.channels.map {
                    if (it.id == null || it.name == null) {
                        return@map
                    }

                    database.channelQueries.upsert(
                        it.id!!,
                        it.channelType?.value ?: ChannelType.TextChannel.value,
                        it.user,
                        it.name,
                        it.owner,
                        it.description,
                        if (it.channelType == ChannelType.DirectMessage) it.recipients?.firstOrNull { u -> u != StoatAPI.selfId } else null,
                        it.icon?.id,
                        it.lastMessageID,
                        if (it.active == true) 1L else 0L,
                        if (it.nsfw == true) 1L else 0L,
                        it.server
                    )
                }

                // Remove channels that are not in the ready frame
                val channelsThatExist = readyFrame.channels.mapNotNull { it.id }
                val channelsInDatabase = database.channelQueries.selectAllIds().executeAsList()
                val channelsToDelete = channelsInDatabase.filter { it !in channelsThatExist }

                channelsToDelete.forEach {
                    database.channelQueries.delete(it)
                    Logger.d {
                        "Deleted channel $it from local database due to not being in ready frame."
                    }
                    // Conversely, remove the channel from the API state
                    StoatAPI.channelCache.remove(it)
                }

                Logger.d { "Adding emojis to cache." }
                val emojiMap = readyFrame.emojis.associateBy { it.id!! }
                StoatAPI.emojiCache.putAll(emojiMap)

                Logger.d { "Adding voice states to cache." }
                val voiceStateMap = readyFrame.voiceStates.associateBy { it.id }
                StoatAPI.voiceStateCache.putAll(voiceStateMap)

                Logger.d { "New voice states: ${voiceStateMap}" }

                Logger.d { "Registering push notification channels." }
                StoatAPIHost.platform.onRealtimeHydrated()

                StoatAPI.closeHydration()
            }

            "Message" -> {
                val messageFrame = StoatJson.decodeFromString(MessageFrame.serializer(), rawFrame)
                Logger.d {
                    "Received message frame for ${messageFrame.id} in channel ${messageFrame.channel}."
                }

                if (messageFrame.id == null) {
                    Logger.d { "Message frame has no ID or channel. Ignoring." }
                    return
                }

                StoatAPI.messageCache[messageFrame.id!!] = messageFrame

                messageFrame.channel?.let {
                    if (StoatAPI.channelCache[it] == null) {
                        Logger.d { "Channel $it not found in cache. Ignoring." }
                        return
                    }

                    StoatAPI.channelCache[it] =
                        StoatAPI.channelCache[it]!!.copy(lastMessageID = messageFrame.id)

                    StoatAPI.wsFrameChannel.emit(messageFrame)
                }
            }

            "MessageAppend" -> {
                val messageAppendFrame =
                    StoatJson.decodeFromString(MessageAppendFrame.serializer(), rawFrame)
                Logger.d {
                    "Received message append frame for ${messageAppendFrame.id} in channel ${messageAppendFrame.channel}."
                }

                var message = StoatAPI.messageCache[messageAppendFrame.id]

                if (message == null) {
                    Logger.d {
                        "Message ${messageAppendFrame.id} not found in cache. Will not append."
                    }
                    return
                }

                messageAppendFrame.append.embeds?.let {
                    message = message!!.copy(embeds = message!!.embeds?.plus(it) ?: it)
                }

                StoatAPI.messageCache[messageAppendFrame.id] = message!!

                StoatAPI.wsFrameChannel.emit(messageAppendFrame)
            }

            "MessageUpdate" -> {
                val messageUpdateFrame =
                    StoatJson.decodeFromString(MessageUpdateFrame.serializer(), rawFrame)
                Logger.d {
                    "Received message update frame for ${messageUpdateFrame.id} in channel ${messageUpdateFrame.channel}."
                }

                val oldMessage = StoatAPI.messageCache[messageUpdateFrame.id]
                if (oldMessage == null) {
                    Logger.d {
                        "Message ${messageUpdateFrame.id} not found in cache. Will not update."
                    }
                    return
                }

                val rawMessage: MessageFrame
                try {
                    rawMessage =
                        StoatJson.decodeFromJsonElement(
                            MessageFrame.serializer(),
                            messageUpdateFrame.data
                        )
                } catch (e: SerializationException) {
                    Logger.d { "Message update frame has invalid data. Ignoring." }
                    return
                }

                Logger.d {
                    "Merging message ${messageUpdateFrame.id} with updated partial."
                }

                StoatAPI.messageCache[messageUpdateFrame.id] =
                    oldMessage.mergeWithPartial(rawMessage)

                messageUpdateFrame.channel.let {
                    if (StoatAPI.channelCache[it] == null) {
                        Logger.d { "Channel $it not found in cache. Ignoring." }
                        return
                    }
                }

                StoatAPI.wsFrameChannel.emit(messageUpdateFrame)
            }

            "MessageDelete" -> {
                val messageDeleteFrame =
                    StoatJson.decodeFromString(MessageDeleteFrame.serializer(), rawFrame)
                Logger.d {
                    "Received message react frame for ${messageDeleteFrame.id}."
                }

                val message = StoatAPI.messageCache[messageDeleteFrame.id]
                if (message == null) {
                    Logger.d {
                        "Message ${messageDeleteFrame.id} not found in cache. Will not delete."
                    }
                    return
                }

                StoatAPI.messageCache.remove(messageDeleteFrame.id)
                StoatAPI.wsFrameChannel.emit(messageDeleteFrame)
            }

            "MessageReact" -> {
                val messageReactFrame =
                    StoatJson.decodeFromString(MessageReactFrame.serializer(), rawFrame)
                Logger.d {
                    "Received message react frame for ${messageReactFrame.id}."
                }

                val oldMessage = StoatAPI.messageCache[messageReactFrame.id]
                if (oldMessage == null) {
                    Logger.d {
                        "Message ${messageReactFrame.id} not found in cache. Will not update."
                    }
                    return
                }

                val reactions = oldMessage.reactions?.toMutableMap() ?: mutableMapOf()
                val forEmoji =
                    reactions[messageReactFrame.emoji_id]?.toMutableList() ?: mutableListOf()
                forEmoji.add(messageReactFrame.user_id)
                reactions[messageReactFrame.emoji_id] = forEmoji

                StoatAPI.messageCache[messageReactFrame.id] =
                    oldMessage.copy(reactions = reactions)

                StoatAPI.wsFrameChannel.emit(messageReactFrame)
            }

            "MessageUnreact" -> {
                val messageUnreactFrame =
                    StoatJson.decodeFromString(MessageReactFrame.serializer(), rawFrame)
                Logger.d {
                    "Received message unreact frame for ${messageUnreactFrame.id}."
                }

                val oldMessage = StoatAPI.messageCache[messageUnreactFrame.id]
                if (oldMessage == null) {
                    Logger.d {
                        "Message ${messageUnreactFrame.id} not found in cache. Will not update."
                    }
                    return
                }

                val reactions = oldMessage.reactions?.toMutableMap() ?: mutableMapOf()
                val forEmoji =
                    reactions[messageUnreactFrame.emoji_id]?.toMutableList() ?: mutableListOf()
                forEmoji.remove(messageUnreactFrame.user_id)

                if (forEmoji.isEmpty()) {
                    reactions.remove(messageUnreactFrame.emoji_id)
                } else {
                    reactions[messageUnreactFrame.emoji_id] = forEmoji
                }

                StoatAPI.messageCache[messageUnreactFrame.id] =
                    oldMessage.copy(reactions = reactions)

                StoatAPI.wsFrameChannel.emit(messageUnreactFrame)
            }

            "UserUpdate" -> {
                val userUpdateFrame =
                    StoatJson.decodeFromString(UserUpdateFrame.serializer(), rawFrame)

                val existing = StoatAPI.userCache[userUpdateFrame.id]
                    ?: return // if we don't have the user no point in updating it

                var updated = existing.mergeWithPartial(userUpdateFrame.data)

                userUpdateFrame.clear?.forEach {
                    updated = when (it) {
                        "Avatar" -> updated.copy(avatar = null)
                        "DisplayName" -> updated.copy(displayName = null)
                        "Pronouns" -> updated.copy(pronouns = null)
                        else -> updated
                    }
                }

                StoatAPI.userCache[userUpdateFrame.id] = updated
            }

            "UserRelationship" -> {
                val userRelationshipFrame =
                    StoatJson.decodeFromString(UserRelationshipFrame.serializer(), rawFrame)

                val existing = StoatAPI.userCache[userRelationshipFrame.user.id]

                if (existing == null && userRelationshipFrame.user.id != null) {
                    StoatAPI.userCache[userRelationshipFrame.user.id!!] =
                        userRelationshipFrame.user.copy(
                            relationship = userRelationshipFrame.status ?: "None"
                        )
                } else if (existing != null && userRelationshipFrame.user.id != null) {
                    val merged = existing.mergeWithPartial(userRelationshipFrame.user).copy(
                        relationship = userRelationshipFrame.status ?: "None"
                    )
                    StoatAPI.userCache[userRelationshipFrame.user.id!!] = merged
                } else {
                    Logger.w { "Invalid UserRelationship frame: $rawFrame" }
                }
            }

            "ChannelUpdate" -> {
                val channelUpdateFrame =
                    StoatJson.decodeFromString(ChannelUpdateFrame.serializer(), rawFrame)

                val existing = StoatAPI.channelCache[channelUpdateFrame.id]
                    ?: return // if we don't have the channel no point in updating it

                var combined = existing.mergeWithPartial(channelUpdateFrame.data)
                if ("Slowmode" in channelUpdateFrame.clear.orEmpty()) {
                    combined = combined.copy(slowmode = null)
                }

                StoatAPI.channelCache[channelUpdateFrame.id] = combined
                if ((combined.slowmode ?: 0) <= 0) {
                    StoatAPI.userSlowmodeCache.remove(channelUpdateFrame.id)
                }

                database.channelQueries.upsert(
                    channelUpdateFrame.id,
                    combined.channelType?.value ?: ChannelType.TextChannel.value,
                    combined.user,
                    combined.name,
                    combined.owner,
                    combined.description,
                    if (combined.channelType == ChannelType.DirectMessage) combined.recipients?.firstOrNull { u -> u != StoatAPI.selfId } else null,
                    combined.icon?.id,
                    combined.lastMessageID,
                    if (combined.active == true) 1L else 0L,
                    if (combined.nsfw == true) 1L else 0L,
                    combined.server
                )
            }

            "ChannelCreate" -> {
                val channelCreateFrame =
                    StoatJson.decodeFromString(Channel.serializer(), rawFrame)

                Logger.d {
                    "Received channel create frame for ${channelCreateFrame.id}, with name ${channelCreateFrame.name}. Adding to cache."
                }

                StoatAPI.channelCache[channelCreateFrame.id!!] = channelCreateFrame
                channelCreateFrame.server?.let { serverId ->
                    StoatAPI.serverCache[serverId]?.let { server ->
                        if (channelCreateFrame.id !in server.channels.orEmpty()) {
                            StoatAPI.serverCache[serverId] = server.copy(
                                channels = server.channels.orEmpty() + channelCreateFrame.id!!,
                            )
                        }
                    }
                }
                database.channelQueries.upsert(
                    channelCreateFrame.id!!,
                    channelCreateFrame.channelType?.value ?: ChannelType.TextChannel.value,
                    channelCreateFrame.user,
                    channelCreateFrame.name,
                    channelCreateFrame.owner,
                    channelCreateFrame.description,
                    if (channelCreateFrame.channelType == ChannelType.DirectMessage) channelCreateFrame.recipients?.firstOrNull { u -> u != StoatAPI.selfId } else null,
                    channelCreateFrame.icon?.id,
                    channelCreateFrame.lastMessageID,
                    if (channelCreateFrame.active == true) 1L else 0L,
                    if (channelCreateFrame.nsfw == true) 1L else 0L,
                    channelCreateFrame.server
                )
            }

            "ChannelDelete" -> {
                val channelDeleteFrame =
                    StoatJson.decodeFromString(ChannelDeleteFrame.serializer(), rawFrame)
                Logger.d {
                    "Received channel delete frame for ${channelDeleteFrame.id}. Removing from cache."
                }

                val currentChannel = StoatAPI.channelCache[channelDeleteFrame.id]
                if (currentChannel == null) {
                    Logger.d {
                        "Channel ${channelDeleteFrame.id} not found in cache. Ignoring."
                    }
                    return
                }

                StoatAPI.channelCache.remove(channelDeleteFrame.id)
                StoatAPI.userSlowmodeCache.remove(channelDeleteFrame.id)
                database.channelQueries.delete(channelDeleteFrame.id)

                if (currentChannel.server != null) {
                    val existingServer = StoatAPI.serverCache[currentChannel.server]

                    if (existingServer == null) {
                        Logger.d {
                            "Server ${currentChannel.server} not found in cache. Ignoring."
                        }
                        return
                    }

                    StoatAPI.serverCache[currentChannel.server!!] = existingServer.copy(
                        channels = existingServer.channels?.filter { it != channelDeleteFrame.id }
                            ?: emptyList()
                    )
                }

                StoatAPI.wsFrameChannel.emit(channelDeleteFrame)
            }

            "ChannelAck" -> {
                val channelAckFrame =
                    StoatJson.decodeFromString(ChannelAckFrame.serializer(), rawFrame)
                Logger.d {
                    "Received channel ack frame for ${channelAckFrame.id} with new newest ${channelAckFrame.messageId}."
                }

                StoatAPI.unreads.processExternalAck(channelAckFrame.id, channelAckFrame.messageId)
            }

            "UserSlowmodes" -> {
                val userSlowmodesFrame =
                    StoatJson.decodeFromString(UserSlowmodesFrame.serializer(), rawFrame)
                val receivedAt = SystemClock.elapsedRealtime()

                userSlowmodesFrame.slowmodes.forEach { slowmode ->
                    StoatAPI.userSlowmodeCache[slowmode.channelId] =
                        ActiveSlowmode.from(slowmode, receivedAt)
                }
            }

            "ServerCreate" -> {
                val serverCreateFrame =
                    StoatJson.decodeFromString(ServerCreateFrame.serializer(), rawFrame)
                Logger.d {
                    "Received server create frame for ${serverCreateFrame.id}, with name ${serverCreateFrame.server.name}. Adding to cache."
                }

                StoatAPI.serverCache[serverCreateFrame.id] = serverCreateFrame.server

                val serverOwner = serverCreateFrame.server.owner
                val serverName = serverCreateFrame.server.name
                val canPersistServer = serverOwner != null && serverName != null

                if (canPersistServer) {
                    database.serverQueries.upsert(
                        serverCreateFrame.id,
                        serverOwner,
                        serverName,
                        serverCreateFrame.server.description,
                        serverCreateFrame.server.icon?.id,
                        serverCreateFrame.server.banner?.id,
                        serverCreateFrame.server.flags
                    )
                }

                serverCreateFrame.channels.forEach { channel ->
                    val channelId = channel.id ?: return@forEach
                    StoatAPI.channelCache[channelId] = channel

                    if (canPersistServer) {
                        database.channelQueries.upsert(
                            channelId,
                            channel.channelType?.value ?: ChannelType.TextChannel.value,
                            channel.user,
                            channel.name,
                            channel.owner,
                            channel.description,
                            if (channel.channelType == ChannelType.DirectMessage) {
                                channel.recipients?.firstOrNull { it != StoatAPI.selfId }
                            } else {
                                null
                            },
                            channel.icon?.id,
                            channel.lastMessageID,
                            if (channel.active == true) 1L else 0L,
                            if (channel.nsfw == true) 1L else 0L,
                            channel.server,
                        )
                    }
                }

                serverCreateFrame.emojis.forEach { emoji ->
                    emoji.id?.let { StoatAPI.emojiCache[it] = emoji }
                }
                serverCreateFrame.voiceStates.forEach { voiceState ->
                    StoatAPI.voiceStateCache[voiceState.id] = voiceState
                }

                if (!canPersistServer) {
                    Logger.e {
                        "Server ${serverCreateFrame.id} was missing required fields and could not be persisted."
                    }
                }
            }

            "EmojiCreate" -> {
                val emoji = StoatJson.decodeFromString(Emoji.serializer(), rawFrame)
                emoji.id?.let { StoatAPI.emojiCache[it] = emoji }
                StoatAPI.wsFrameChannel.emit(emoji)
            }

            "EmojiDelete" -> {
                val emojiDeleteFrame =
                    StoatJson.decodeFromString(EmojiDeleteFrame.serializer(), rawFrame)
                StoatAPI.emojiCache.remove(emojiDeleteFrame.id)
                StoatAPI.wsFrameChannel.emit(emojiDeleteFrame)
            }

            "ChannelStartTyping" -> {
                val channelStartTypingFrame =
                    StoatJson.decodeFromString(ChannelStartTypingFrame.serializer(), rawFrame)
                Logger.d {
                    "Received channel start typing frame for ${channelStartTypingFrame.id}."
                }

                StoatAPI.wsFrameChannel.emit(channelStartTypingFrame)
            }

            "ChannelStopTyping" -> {
                val channelStopTypingFrame =
                    StoatJson.decodeFromString(ChannelStopTypingFrame.serializer(), rawFrame)
                Logger.d {
                    "Received channel stop typing frame for ${channelStopTypingFrame.id}."
                }

                StoatAPI.wsFrameChannel.emit(channelStopTypingFrame)
            }

            "ServerUpdate" -> {
                val serverUpdateFrame =
                    StoatJson.decodeFromString(ServerUpdateFrame.serializer(), rawFrame)
                Logger.d {
                    "Received server update frame for ${serverUpdateFrame.id}."
                }

                val existing = StoatAPI.serverCache[serverUpdateFrame.id]
                    ?: return // if we don't have the server no point in updating it

                var updated =
                    existing.mergeWithPartial(serverUpdateFrame.data)

                serverUpdateFrame.clear?.forEach {
                    when (it) {
                        "Icon" -> updated = updated.copy(icon = null)
                        "Banner" -> updated = updated.copy(banner = null)
                        "Description" -> updated = updated.copy(description = null)
                        "Categories" -> updated = updated.copy(categories = null)
                        else -> Logger.e { "Unknown server clear field: $it" }
                    }
                }

                StoatAPI.serverCache[serverUpdateFrame.id] = updated

                if (updated.id != null && updated.owner != null && updated.name != null) {
                    try {
                        database.serverQueries.upsert(
                            updated.id!!,
                            updated.owner!!,
                            updated.name!!,
                            updated.description,
                            updated.icon?.id,
                            updated.banner?.id,
                            updated.flags
                        )
                    } catch (e: Exception) {
                        Logger.e { "Failed to update server in local database." }
                    }
                }
            }

            "ServerDelete" -> {
                val serverDeleteFrame =
                    StoatJson.decodeFromString(ServerDeleteFrame.serializer(), rawFrame)
                Logger.d {
                    "Received server delete frame for ${serverDeleteFrame.id}."
                }

                val deletedChannelIds = (
                        StoatAPI.serverCache[serverDeleteFrame.id]?.channels.orEmpty() +
                                StoatAPI.channelCache
                                    .filterValues { it.server == serverDeleteFrame.id }
                                    .keys
                        ).distinct()

                deletedChannelIds.forEach { channelId ->
                    StoatAPI.channelCache.remove(channelId)
                    StoatAPI.userSlowmodeCache.remove(channelId)
                    database.channelQueries.delete(channelId)
                }
                StoatAPI.unreads.removeChannels(deletedChannelIds)
                StoatAPI.members.removeServer(serverDeleteFrame.id)
                StoatAPI.emojiCache.keys
                    .filter { emojiId ->
                        StoatAPI.emojiCache[emojiId]?.parent?.id == serverDeleteFrame.id
                    }
                    .forEach(StoatAPI.emojiCache::remove)
                StoatAPI.serverCache.remove(serverDeleteFrame.id)
                database.serverQueries.delete(serverDeleteFrame.id)
                StoatAPI.wsFrameChannel.emit(serverDeleteFrame)
            }

            "ServerMemberUpdate" -> {
                val serverMemberUpdateFrame =
                    StoatJson.decodeFromString(ServerMemberUpdateFrame.serializer(), rawFrame)
                Logger.d {
                    "Received server member update frame for ${serverMemberUpdateFrame.id.user} in ${serverMemberUpdateFrame.id.server}."
                }

                val existing = StoatAPI.members.getMember(
                    serverMemberUpdateFrame.id.server,
                    serverMemberUpdateFrame.id.user
                )
                    ?: return // if we don't have the member no point in updating them

                var updated = existing.mergeWithPartial(serverMemberUpdateFrame.data)

                serverMemberUpdateFrame.clear?.forEach {
                    when (it) {
                        "Avatar" -> updated = updated.copy(avatar = null)
                        "Nickname" -> updated = updated.copy(nickname = null)
                        "Pronouns" -> updated = updated.copy(pronouns = null)
                        else -> Logger.e { "Unknown server member clear field: $it" }
                    }
                }

                Logger.d { "Updated member: $updated" }

                StoatAPI.members.setMember(serverMemberUpdateFrame.id.server, updated)
            }

            "ServerMemberJoin" -> {
                val serverMemberJoinFrame =
                    StoatJson.decodeFromString(ServerMemberJoinFrame.serializer(), rawFrame)
                Logger.d {
                    "Received server member join frame for ${serverMemberJoinFrame.user} in ${serverMemberJoinFrame.id}."
                }

                val member = fetchMember(serverMemberJoinFrame.id, serverMemberJoinFrame.user)

                StoatAPI.members.setMember(serverMemberJoinFrame.id, member)
            }

            "ServerMemberLeave" -> {
                val serverMemberLeaveFrame =
                    StoatJson.decodeFromString(ServerMemberLeaveFrame.serializer(), rawFrame)
                Logger.d {
                    "Received server member leave frame for ${serverMemberLeaveFrame.user} in ${serverMemberLeaveFrame.id}."
                }

                StoatAPI.members.removeMember(
                    serverMemberLeaveFrame.id,
                    serverMemberLeaveFrame.user
                )
            }

            "ServerRoleUpdate" -> {
                val serverRoleUpdateFrame =
                    StoatJson.decodeFromString(ServerRoleUpdateFrame.serializer(), rawFrame)
                Logger.d {
                    "Received server role update frame for ${serverRoleUpdateFrame.id}."
                }

                val server = StoatAPI.serverCache[serverRoleUpdateFrame.id]
                if (server == null) {
                    Logger.d {
                        "Server ${serverRoleUpdateFrame.id} not found in cache. Ignoring role update."
                    }
                    return
                }

                val existingRole = server.roles?.get(serverRoleUpdateFrame.roleId)
                if (existingRole == null) {
                    // New role.
                    Logger.d {
                        "New role ${serverRoleUpdateFrame.roleId} in server ${serverRoleUpdateFrame.id}. Adding to cache."
                    }
                    val newRole = Role().mergeWithPartial(serverRoleUpdateFrame.data)
                    val newServer = server.copy(
                        roles = server.roles?.plus(
                            Pair(serverRoleUpdateFrame.roleId, newRole)
                        ) ?: mapOf(serverRoleUpdateFrame.roleId to newRole)
                    )
                    StoatAPI.serverCache[serverRoleUpdateFrame.id] = newServer
                } else {
                    // True role update.
                    Logger.d {
                        "Updating existing role ${serverRoleUpdateFrame.roleId} in server ${serverRoleUpdateFrame.id}."
                    }
                    var updatedRole = existingRole.mergeWithPartial(serverRoleUpdateFrame.data)
                    serverRoleUpdateFrame.clear.orEmpty().forEach { field ->
                        updatedRole = when (field) {
                            "Colour" -> updatedRole.copy(colour = null)
                            "Icon" -> updatedRole.copy(icon = null)
                            else -> updatedRole
                        }
                    }
                    val newServer = server.copy(
                        roles = server.roles!!.plus(
                            Pair(serverRoleUpdateFrame.roleId, updatedRole)
                        )
                    )
                    StoatAPI.serverCache[serverRoleUpdateFrame.id] = newServer
                }
            }

            "ServerRoleRanksUpdate" -> {
                val serverRoleRanksUpdateFrame =
                    StoatJson.decodeFromString(ServerRoleRanksUpdateFrame.serializer(), rawFrame)
                Logger.d { "Received server role ranks update frame for ${serverRoleRanksUpdateFrame.id}." }

                val server = StoatAPI.serverCache[serverRoleRanksUpdateFrame.id]
                if (server == null) {
                    Logger.d { "Server ${serverRoleRanksUpdateFrame.id} not found in cache. Ignoring role ranks update." }
                    return
                }

                val ranksByRoleId = serverRoleRanksUpdateFrame.ranks
                    .withIndex()
                    .associate { (rank, roleId) -> roleId to rank.toDouble() }
                StoatAPI.serverCache[serverRoleRanksUpdateFrame.id] = server.copy(
                    roles = server.roles.orEmpty().mapValues { (roleId, role) ->
                        ranksByRoleId[roleId]?.let { role.copy(rank = it) } ?: role
                    }
                )
            }

            "ServerRoleDelete" -> {
                val serverRoleDeleteFrame =
                    StoatJson.decodeFromString(ServerRoleDeleteFrame.serializer(), rawFrame)
                Logger.d {
                    "Received server role delete frame for ${serverRoleDeleteFrame.id} and role ${serverRoleDeleteFrame.roleId}."
                }

                val server = StoatAPI.serverCache[serverRoleDeleteFrame.id]
                if (server == null) {
                    Logger.d {
                        "Server ${serverRoleDeleteFrame.id} not found in cache. Ignoring role delete."
                    }
                    return
                }

                val newRoles = server.roles?.toMutableMap() ?: mutableMapOf()
                newRoles.remove(serverRoleDeleteFrame.roleId)

                StoatAPI.serverCache[serverRoleDeleteFrame.id] =
                    server.copy(roles = newRoles)
            }

            "VoiceChannelJoin" -> {
                val voiceChannelJoinFrame =
                    StoatJson.decodeFromString(VoiceChannelJoinFrame.serializer(), rawFrame)

                Logger.d { "Received voice channel join frame for channel ${voiceChannelJoinFrame.id}." }

                val newParticipants =
                    StoatAPI.voiceStateCache[voiceChannelJoinFrame.id]?.participants?.filter {
                        it.id != voiceChannelJoinFrame.state.id
                    }?.toMutableList() ?: mutableListOf()
                newParticipants.add(voiceChannelJoinFrame.state)

                StoatAPI.voiceStateCache[voiceChannelJoinFrame.id] =
                    ChannelVoiceState(voiceChannelJoinFrame.id, newParticipants)
            }

            "VoiceChannelLeave" -> {
                val voiceChannelLeaveFrame =
                    StoatJson.decodeFromString(VoiceChannelLeaveFrame.serializer(), rawFrame)

                Logger.d { "Received voice channel leave frame for channel ${voiceChannelLeaveFrame.id}." }

                val existingChannelState =
                    StoatAPI.voiceStateCache[voiceChannelLeaveFrame.id] ?: return

                val newParticipants = existingChannelState.participants.filter {
                    it.id != voiceChannelLeaveFrame.user
                }

                StoatAPI.voiceStateCache[voiceChannelLeaveFrame.id] =
                    ChannelVoiceState(voiceChannelLeaveFrame.id, newParticipants)
            }

            "VoiceChannelMove" -> {
                val voiceChannelMoveFrame =
                    StoatJson.decodeFromString(VoiceChannelMoveFrame.serializer(), rawFrame)

                Logger.d { "Received voice channel move frame from ${voiceChannelMoveFrame.from} to ${voiceChannelMoveFrame.to}." }

                // Remove from old channel
                val existingFromChannelState =
                    StoatAPI.voiceStateCache[voiceChannelMoveFrame.from] ?: return

                val newFromParticipants = existingFromChannelState.participants.filter {
                    it.id != voiceChannelMoveFrame.state.id
                }

                StoatAPI.voiceStateCache[voiceChannelMoveFrame.from] =
                    ChannelVoiceState(voiceChannelMoveFrame.from, newFromParticipants)

                // Add to new channel
                val existingToChannelState =
                    StoatAPI.voiceStateCache[voiceChannelMoveFrame.to]

                val newToParticipants =
                    existingToChannelState?.participants?.toMutableList() ?: mutableListOf()
                newToParticipants.add(voiceChannelMoveFrame.state)

                StoatAPI.voiceStateCache[voiceChannelMoveFrame.to] =
                    ChannelVoiceState(voiceChannelMoveFrame.to, newToParticipants)
            }

            "UserVoiceStateUpdate" -> {
                val userVoiceStateUpdateFrame =
                    StoatJson.decodeFromString(UserVoiceStateUpdateFrame.serializer(), rawFrame)

                Logger.d { "Received user voice state update frame for user ${userVoiceStateUpdateFrame.id} in channel ${userVoiceStateUpdateFrame.channelId}." }

                val existingChannelState =
                    StoatAPI.voiceStateCache[userVoiceStateUpdateFrame.channelId] ?: return

                val newParticipants = existingChannelState.participants.map {
                    if (it.id == userVoiceStateUpdateFrame.id) {
                        userVoiceStateUpdateFrame.data.overrideInto(it)
                    } else {
                        it
                    }
                }

                StoatAPI.voiceStateCache[userVoiceStateUpdateFrame.channelId] =
                    ChannelVoiceState(userVoiceStateUpdateFrame.channelId, newParticipants)
            }

            "UserMoveVoiceChannel" -> {
                val userMoveVoiceChannelFrame =
                    StoatJson.decodeFromString(UserMoveVoiceChannelFrame.serializer(), rawFrame)

                Logger.d { "We got moved into a different channel on node ${userMoveVoiceChannelFrame.node}." }

                // Send message to UI to handle the move
                StoatAPI.wsFrameChannel.emit(userMoveVoiceChannelFrame)
            }

            "UserSettingsUpdate" -> {
                val update = StoatJson.parseToJsonElement(rawFrame).jsonObject["update"]
                    ?.jsonObject
                    ?: return
                SyncedSettings.applyRemoteUpdate(
                    update.mapNotNull { (key, value) ->
                        val (timestamp, data) = value.jsonArray.takeIf { it.size == 2 }
                            ?: return@mapNotNull null
                        key to SyncedSetting(
                            timestamp = timestamp.jsonPrimitive.long,
                            value = data.jsonPrimitive.content
                        )
                    }.toMap()
                )
            }

            "Authenticated" -> {
                SyncedSettings.fetch()
                LoadedSettings.hydrateWithSettings(SyncedSettings)
            }

            else -> {
                Logger.i { "Unknown frame: $rawFrame" }
            }
        }
    }

    private suspend fun pushReconnectEvent() {
        StoatAPI.wsFrameChannel.emit(RealtimeSocketFrames.Reconnected)
    }

    suspend fun beginTyping(channelId: String) {
        if (disconnectionState != DisconnectionState.Connected) return

        val beginTypingFrame = BeginTypingFrame("BeginTyping", channelId)
        socket?.send(
            StoatJson.encodeToString(
                BeginTypingFrame.serializer(),
                beginTypingFrame
            )
        )
    }

    suspend fun endTyping(channelId: String) {
        if (disconnectionState != DisconnectionState.Connected) return

        val endTypingFrame = EndTypingFrame("EndTyping", channelId)
        socket?.send(
            StoatJson.encodeToString(
                EndTypingFrame.serializer(),
                endTypingFrame
            )
        )
    }
}
