package chat.revolt.api.realtime

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import chat.revolt.RevoltApplication
import chat.revolt.api.RevoltAPI
import chat.revolt.api.RevoltHttp
import chat.revolt.api.RevoltJson
import chat.revolt.api.realtime.frames.receivable.AnyFrame
import chat.revolt.api.realtime.frames.receivable.BulkFrame
import chat.revolt.api.realtime.frames.receivable.ChannelAckFrame
import chat.revolt.api.realtime.frames.receivable.ChannelDeleteFrame
import chat.revolt.api.realtime.frames.receivable.ChannelStartTypingFrame
import chat.revolt.api.realtime.frames.receivable.ChannelStopTypingFrame
import chat.revolt.api.realtime.frames.receivable.ChannelUpdateFrame
import chat.revolt.api.realtime.frames.receivable.MessageAppendFrame
import chat.revolt.api.realtime.frames.receivable.MessageDeleteFrame
import chat.revolt.api.realtime.frames.receivable.MessageFrame
import chat.revolt.api.realtime.frames.receivable.MessageReactFrame
import chat.revolt.api.realtime.frames.receivable.MessageUpdateFrame
import chat.revolt.api.realtime.frames.receivable.PongFrame
import chat.revolt.api.realtime.frames.receivable.ReadyFrame
import chat.revolt.api.realtime.frames.receivable.ServerCreateFrame
import chat.revolt.api.realtime.frames.receivable.ServerDeleteFrame
import chat.revolt.api.realtime.frames.receivable.ServerMemberJoinFrame
import chat.revolt.api.realtime.frames.receivable.ServerMemberLeaveFrame
import chat.revolt.api.realtime.frames.receivable.ServerMemberUpdateFrame
import chat.revolt.api.realtime.frames.receivable.ServerRoleDeleteFrame
import chat.revolt.api.realtime.frames.receivable.ServerRoleUpdateFrame
import chat.revolt.api.realtime.frames.receivable.ServerUpdateFrame
import chat.revolt.api.realtime.frames.receivable.UserRelationshipFrame
import chat.revolt.api.realtime.frames.receivable.UserUpdateFrame
import chat.revolt.api.realtime.frames.sendable.AuthorizationFrame
import chat.revolt.api.realtime.frames.sendable.BeginTypingFrame
import chat.revolt.api.realtime.frames.sendable.EndTypingFrame
import chat.revolt.api.realtime.frames.sendable.PingFrame
import chat.revolt.api.routes.server.fetchMember
import chat.revolt.api.schemas.Channel
import chat.revolt.api.schemas.ChannelType
import chat.revolt.api.schemas.Role
import chat.revolt.api.settings.LoadedSettings
import chat.revolt.api.settings.SyncedSettings
import chat.revolt.c2dm.ChannelRegistrator
import chat.revolt.persistence.Database
import chat.revolt.persistence.SqlStorage
import io.ktor.client.plugins.websocket.ws
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.SerializationException
import logcat.logcat

enum class DisconnectionState {
    Disconnected,
    Reconnecting,
    Connected
}

sealed class RealtimeSocketFrames {
    data object Reconnected : RealtimeSocketFrames()
}

object RealtimeSocket {
    val database = Database(SqlStorage.driver)
    var socket: WebSocketSession? = null

    private val channelRegistrator: ChannelRegistrator
        get() = ChannelRegistrator(RevoltApplication.instance)

    private var _disconnectionState = mutableStateOf(DisconnectionState.Reconnecting)
    val disconnectionState: DisconnectionState
        get() = _disconnectionState.value

    fun updateDisconnectionState(state: DisconnectionState) {
        _disconnectionState.value = state
    }

    suspend fun connect(token: String) {
        if (disconnectionState == DisconnectionState.Connected) {
            Log.d("RealtimeSocket", "Already connected to websocket. Refusing to connect again.")
            return
        }

        socket?.close(CloseReason(CloseReason.Codes.NORMAL, "Reconnecting to websocket."))

        RevoltHttp.ws(RevoltAPI.getCurrentWebSocketUrl()) {
            socket = this

            Log.d("RealtimeSocket", "Connected to websocket.")
            updateDisconnectionState(DisconnectionState.Connected)
            pushReconnectEvent()

            // Send authorization frame
            val authFrame = AuthorizationFrame("Authenticate", token)
            val authFrameString =
                RevoltJson.encodeToString(AuthorizationFrame.serializer(), authFrame)

            Log.d(
                "RealtimeSocket",
                "Sending authorization frame: ${
                    authFrameString.replace(
                        token,
                        "X".repeat(token.length)
                    )
                }"
            )
            send(RevoltJson.encodeToString(AuthorizationFrame.serializer(), authFrame))

            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    val frameString = frame.readText()
                    val frameType =
                        RevoltJson.decodeFromString(AnyFrame.serializer(), frameString).type

                    handleFrame(frameType, frameString)
                }
            }
        }
    }

    suspend fun sendPing() {
        if (disconnectionState != DisconnectionState.Connected) return

        val pingPacket = PingFrame("Ping", System.currentTimeMillis())
        socket?.send(RevoltJson.encodeToString(PingFrame.serializer(), pingPacket))
        Log.d("RealtimeSocket", "Sent ping frame with ${pingPacket.data}")
    }

    private suspend fun handleFrame(type: String, rawFrame: String) {
        when (type) {
            "Pong" -> {
                val pongFrame = RevoltJson.decodeFromString(PongFrame.serializer(), rawFrame)
                Log.d("RealtimeSocket", "Received pong frame for ${pongFrame.data}")
            }

            "Bulk" -> {
                val bulkFrame = RevoltJson.decodeFromString(BulkFrame.serializer(), rawFrame)
                Log.d("RealtimeSocket", "Received bulk frame with ${bulkFrame.v.size} sub-frames.")
                bulkFrame.v.forEach { subFrame ->
                    val subFrameType =
                        RevoltJson.decodeFromString(AnyFrame.serializer(), subFrame.toString()).type
                    handleFrame(subFrameType, subFrame.toString())
                }
            }

            "Ready" -> {
                val readyFrame = RevoltJson.decodeFromString(ReadyFrame.serializer(), rawFrame)

                logcat {
                    "Received ready frame with ${readyFrame.users.size} users, " +
                            "${readyFrame.servers.size} servers, " +
                            "${readyFrame.channels.size} channels, " +
                            "${readyFrame.emojis.size} emojis, " +
                            "and ${readyFrame.voiceStates.size} voice states."
                }

                Log.d("RealtimeSocket", "Adding users to cache.")
                val userMap = readyFrame.users.associateBy { it.id!! }
                RevoltAPI.userCache.putAll(userMap)

                Log.d("RealtimeSocket", "Adding servers to cache.")
                val serverMap = readyFrame.servers.associateBy { it.id!! }
                RevoltAPI.serverCache.putAll(serverMap)

                // Cache servers in persistent local database
                readyFrame.servers.map {
                    if (it.id == null || it.owner == null || it.name == null) {
                        return@map
                    }

                    database.serverQueries.upsert(
                        it.id,
                        it.owner,
                        it.name,
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
                    Log.d(
                        "RealtimeSocket",
                        "Deleted server $it from local database due to not being in ready frame."
                    )
                    // Conversely, remove the server from the API state
                    RevoltAPI.serverCache.remove(it)
                }

                Log.d("RealtimeSocket", "Adding channels to cache.")
                val channelMap = readyFrame.channels.associateBy { it.id!! }
                RevoltAPI.channelCache.putAll(channelMap)

                // Cache channels in persistent local database
                readyFrame.channels.map {
                    if (it.id == null || it.name == null) {
                        return@map
                    }

                    database.channelQueries.upsert(
                        it.id,
                        it.channelType?.value ?: ChannelType.TextChannel.value,
                        it.user,
                        it.name,
                        it.owner,
                        it.description,
                        if (it.channelType == ChannelType.DirectMessage) it.recipients?.firstOrNull { u -> u != RevoltAPI.selfId } else null,
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
                    Log.d(
                        "RealtimeSocket",
                        "Deleted channel $it from local database due to not being in ready frame."
                    )
                    // Conversely, remove the channel from the API state
                    RevoltAPI.channelCache.remove(it)
                }

                Log.d("RealtimeSocket", "Adding emojis to cache.")
                val emojiMap = readyFrame.emojis.associateBy { it.id!! }
                RevoltAPI.emojiCache.putAll(emojiMap)

                Log.d("RealtimeSocket", "Registering push notification channels.")
                channelRegistrator.register()

                RevoltAPI.closeHydration()
            }

            "Message" -> {
                val messageFrame = RevoltJson.decodeFromString(MessageFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received message frame for ${messageFrame.id} in channel ${messageFrame.channel}."
                )

                if (messageFrame.id == null) {
                    Log.d("RealtimeSocket", "Message frame has no ID or channel. Ignoring.")
                    return
                }

                RevoltAPI.messageCache[messageFrame.id] = messageFrame

                messageFrame.channel?.let {
                    if (RevoltAPI.channelCache[it] == null) {
                        Log.d("RealtimeSocket", "Channel $it not found in cache. Ignoring.")
                        return
                    }

                    RevoltAPI.channelCache[it] =
                        RevoltAPI.channelCache[it]!!.copy(lastMessageID = messageFrame.id)

                    RevoltAPI.wsFrameChannel.send(messageFrame)
                }
            }

            "MessageAppend" -> {
                val messageAppendFrame =
                    RevoltJson.decodeFromString(MessageAppendFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received message append frame for ${messageAppendFrame.id} in channel ${messageAppendFrame.channel}."
                )

                var message = RevoltAPI.messageCache[messageAppendFrame.id]

                if (message == null) {
                    Log.d(
                        "RealtimeSocket",
                        "Message ${messageAppendFrame.id} not found in cache. Will not append."
                    )
                    return
                }

                messageAppendFrame.append.embeds?.let {
                    message = message!!.copy(embeds = message!!.embeds?.plus(it) ?: it)
                }

                RevoltAPI.messageCache[messageAppendFrame.id] = message!!

                RevoltAPI.wsFrameChannel.send(messageAppendFrame)
            }

            "MessageUpdate" -> {
                val messageUpdateFrame =
                    RevoltJson.decodeFromString(MessageUpdateFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received message update frame for ${messageUpdateFrame.id} in channel ${messageUpdateFrame.channel}."
                )

                val oldMessage = RevoltAPI.messageCache[messageUpdateFrame.id]
                if (oldMessage == null) {
                    Log.d(
                        "RealtimeSocket",
                        "Message ${messageUpdateFrame.id} not found in cache. Will not update."
                    )
                    return
                }

                val rawMessage: MessageFrame
                try {
                    rawMessage =
                        RevoltJson.decodeFromJsonElement(
                            MessageFrame.serializer(),
                            messageUpdateFrame.data
                        )
                } catch (e: SerializationException) {
                    Log.d("RealtimeSocket", "Message update frame has invalid data. Ignoring.")
                    return
                }

                Log.d(
                    "RealtimeSocket",
                    "Merging message ${messageUpdateFrame.id} with updated partial."
                )

                RevoltAPI.messageCache[messageUpdateFrame.id] =
                    oldMessage.mergeWithPartial(rawMessage)

                messageUpdateFrame.channel.let {
                    if (RevoltAPI.channelCache[it] == null) {
                        Log.d("RealtimeSocket", "Channel $it not found in cache. Ignoring.")
                        return
                    }
                }

                RevoltAPI.wsFrameChannel.send(messageUpdateFrame)
            }

            "MessageDelete" -> {
                val messageDeleteFrame =
                    RevoltJson.decodeFromString(MessageDeleteFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received message react frame for ${messageDeleteFrame.id}."
                )

                val message = RevoltAPI.messageCache[messageDeleteFrame.id]
                if (message == null) {
                    Log.d(
                        "RealtimeSocket",
                        "Message ${messageDeleteFrame.id} not found in cache. Will not delete."
                    )
                    return
                }

                RevoltAPI.messageCache.remove(messageDeleteFrame.id)
                RevoltAPI.wsFrameChannel.send(messageDeleteFrame)
            }

            "MessageReact" -> {
                val messageReactFrame =
                    RevoltJson.decodeFromString(MessageReactFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received message react frame for ${messageReactFrame.id}."
                )

                val oldMessage = RevoltAPI.messageCache[messageReactFrame.id]
                if (oldMessage == null) {
                    Log.d(
                        "RealtimeSocket",
                        "Message ${messageReactFrame.id} not found in cache. Will not update."
                    )
                    return
                }

                val reactions = oldMessage.reactions?.toMutableMap() ?: mutableMapOf()
                val forEmoji =
                    reactions[messageReactFrame.emoji_id]?.toMutableList() ?: mutableListOf()
                forEmoji.add(messageReactFrame.user_id)
                reactions[messageReactFrame.emoji_id] = forEmoji

                RevoltAPI.messageCache[messageReactFrame.id] =
                    oldMessage.copy(reactions = reactions)

                RevoltAPI.wsFrameChannel.send(messageReactFrame)
            }

            "MessageUnreact" -> {
                val messageUnreactFrame =
                    RevoltJson.decodeFromString(MessageReactFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received message unreact frame for ${messageUnreactFrame.id}."
                )

                val oldMessage = RevoltAPI.messageCache[messageUnreactFrame.id]
                if (oldMessage == null) {
                    Log.d(
                        "RealtimeSocket",
                        "Message ${messageUnreactFrame.id} not found in cache. Will not update."
                    )
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

                RevoltAPI.messageCache[messageUnreactFrame.id] =
                    oldMessage.copy(reactions = reactions)

                RevoltAPI.wsFrameChannel.send(messageUnreactFrame)
            }

            "UserUpdate" -> {
                val userUpdateFrame =
                    RevoltJson.decodeFromString(UserUpdateFrame.serializer(), rawFrame)

                val existing = RevoltAPI.userCache[userUpdateFrame.id]
                    ?: return // if we don't have the user no point in updating it

                if (userUpdateFrame.clear != null) {
                    if (userUpdateFrame.clear.contains("Avatar")) {
                        RevoltAPI.userCache[userUpdateFrame.id] =
                            existing.copy(avatar = null)
                    }
                }

                RevoltAPI.userCache[userUpdateFrame.id] =
                    existing.mergeWithPartial(userUpdateFrame.data)
            }

            "UserRelationship" -> {
                val userRelationshipFrame =
                    RevoltJson.decodeFromString(UserRelationshipFrame.serializer(), rawFrame)

                val existing = RevoltAPI.userCache[userRelationshipFrame.user.id]

                if (existing == null && userRelationshipFrame.user.id != null) {
                    RevoltAPI.userCache[userRelationshipFrame.user.id] =
                        userRelationshipFrame.user.copy(
                            relationship = userRelationshipFrame.status ?: "None"
                        )
                } else if (existing != null && userRelationshipFrame.user.id != null) {
                    val merged = existing.mergeWithPartial(userRelationshipFrame.user).copy(
                        relationship = userRelationshipFrame.status ?: "None"
                    )
                    RevoltAPI.userCache[userRelationshipFrame.user.id] = merged
                } else {
                    Log.w("RealtimeSocket", "Invalid UserRelationship frame: $rawFrame")
                }
            }

            "ChannelUpdate" -> {
                val channelUpdateFrame =
                    RevoltJson.decodeFromString(ChannelUpdateFrame.serializer(), rawFrame)

                val existing = RevoltAPI.channelCache[channelUpdateFrame.id]
                    ?: return // if we don't have the channel no point in updating it

                val combined = existing.mergeWithPartial(channelUpdateFrame.data)
                RevoltAPI.channelCache[channelUpdateFrame.id] = combined

                database.channelQueries.upsert(
                    channelUpdateFrame.id,
                    combined.channelType?.value ?: ChannelType.TextChannel.value,
                    combined.user,
                    combined.name,
                    combined.owner,
                    combined.description,
                    if (combined.channelType == ChannelType.DirectMessage) combined.recipients?.firstOrNull { u -> u != RevoltAPI.selfId } else null,
                    combined.icon?.id,
                    combined.lastMessageID,
                    if (combined.active == true) 1L else 0L,
                    if (combined.nsfw == true) 1L else 0L,
                    combined.server
                )
            }

            "ChannelCreate" -> {
                val channelCreateFrame =
                    RevoltJson.decodeFromString(Channel.serializer(), rawFrame)

                Log.d(
                    "RealtimeSocket",
                    "Received channel create frame for ${channelCreateFrame.id}, with name ${channelCreateFrame.name}. Adding to cache."
                )

                RevoltAPI.channelCache[channelCreateFrame.id!!] = channelCreateFrame
                database.channelQueries.upsert(
                    channelCreateFrame.id,
                    channelCreateFrame.channelType?.value ?: ChannelType.TextChannel.value,
                    channelCreateFrame.user,
                    channelCreateFrame.name,
                    channelCreateFrame.owner,
                    channelCreateFrame.description,
                    if (channelCreateFrame.channelType == ChannelType.DirectMessage) channelCreateFrame.recipients?.firstOrNull { u -> u != RevoltAPI.selfId } else null,
                    channelCreateFrame.icon?.id,
                    channelCreateFrame.lastMessageID,
                    if (channelCreateFrame.active == true) 1L else 0L,
                    if (channelCreateFrame.nsfw == true) 1L else 0L,
                    channelCreateFrame.server
                )
            }

            "ChannelDelete" -> {
                val channelDeleteFrame =
                    RevoltJson.decodeFromString(ChannelDeleteFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received channel delete frame for ${channelDeleteFrame.id}. Removing from cache."
                )

                val currentChannel = RevoltAPI.channelCache[channelDeleteFrame.id]
                if (currentChannel == null) {
                    Log.d(
                        "RealtimeSocket",
                        "Channel ${channelDeleteFrame.id} not found in cache. Ignoring."
                    )
                    return
                }

                RevoltAPI.channelCache.remove(channelDeleteFrame.id)
                database.channelQueries.delete(channelDeleteFrame.id)

                if (currentChannel.server != null) {
                    val existingServer = RevoltAPI.serverCache[currentChannel.server]

                    if (existingServer == null) {
                        Log.d(
                            "RealtimeSocket",
                            "Server ${currentChannel.server} not found in cache. Ignoring."
                        )
                        return
                    }

                    RevoltAPI.serverCache[currentChannel.server] = existingServer.copy(
                        channels = existingServer.channels?.filter { it != channelDeleteFrame.id }
                            ?: emptyList()
                    )
                }

                RevoltAPI.wsFrameChannel.send(channelDeleteFrame)
            }

            "ChannelAck" -> {
                val channelAckFrame =
                    RevoltJson.decodeFromString(ChannelAckFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received channel ack frame for ${channelAckFrame.id} with new newest ${channelAckFrame.messageId}."
                )

                RevoltAPI.unreads.processExternalAck(channelAckFrame.id, channelAckFrame.messageId)
            }

            "ServerCreate" -> {
                val serverCreateFrame =
                    RevoltJson.decodeFromString(ServerCreateFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received server create frame for ${serverCreateFrame.id}, with name ${serverCreateFrame.server.name}. Adding to cache."
                )

                RevoltAPI.serverCache[serverCreateFrame.id] = serverCreateFrame.server

                serverCreateFrame.channels.forEach { channel ->
                    if (channel.id == null) return@forEach
                    RevoltAPI.channelCache[channel.id] = channel
                }

                if (serverCreateFrame.server.owner != null && serverCreateFrame.server.name != null) {
                    database.serverQueries.upsert(
                        serverCreateFrame.id,
                        serverCreateFrame.server.owner,
                        serverCreateFrame.server.name,
                        serverCreateFrame.server.description,
                        serverCreateFrame.server.icon?.id,
                        serverCreateFrame.server.banner?.id,
                        serverCreateFrame.server.flags
                    )
                }
            }

            "ChannelStartTyping" -> {
                val channelStartTypingFrame =
                    RevoltJson.decodeFromString(ChannelStartTypingFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received channel start typing frame for ${channelStartTypingFrame.id}."
                )

                RevoltAPI.wsFrameChannel.send(channelStartTypingFrame)
            }

            "ChannelStopTyping" -> {
                val channelStopTypingFrame =
                    RevoltJson.decodeFromString(ChannelStopTypingFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received channel stop typing frame for ${channelStopTypingFrame.id}."
                )

                RevoltAPI.wsFrameChannel.send(channelStopTypingFrame)
            }

            "ServerUpdate" -> {
                val serverUpdateFrame =
                    RevoltJson.decodeFromString(ServerUpdateFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received server update frame for ${serverUpdateFrame.id}."
                )

                val existing = RevoltAPI.serverCache[serverUpdateFrame.id]
                    ?: return // if we don't have the server no point in updating it

                var updated =
                    existing.mergeWithPartial(serverUpdateFrame.data)

                serverUpdateFrame.clear?.forEach {
                    when (it) {
                        "Icon" -> updated = updated.copy(icon = null)
                        "Banner" -> updated = updated.copy(banner = null)
                        "Description" -> updated = updated.copy(description = null)
                        else -> Log.e("RealtimeSocket", "Unknown server clear field: $it")
                    }
                }

                RevoltAPI.serverCache[serverUpdateFrame.id] = updated

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
                        Log.e("RealtimeSocket", "Failed to update server in local database.")
                    }
                }
            }

            "ServerDelete" -> {
                val serverDeleteFrame =
                    RevoltJson.decodeFromString(ServerDeleteFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received server delete frame for ${serverDeleteFrame.id}."
                )

                RevoltAPI.serverCache.remove(serverDeleteFrame.id)
                database.serverQueries.delete(serverDeleteFrame.id)
            }

            "ServerMemberUpdate" -> {
                val serverMemberUpdateFrame =
                    RevoltJson.decodeFromString(ServerMemberUpdateFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received server member update frame for ${serverMemberUpdateFrame.id.user} in ${serverMemberUpdateFrame.id.server}."
                )

                val existing = RevoltAPI.members.getMember(
                    serverMemberUpdateFrame.id.server,
                    serverMemberUpdateFrame.id.user
                )
                    ?: return // if we don't have the member no point in updating them

                var updated = existing.mergeWithPartial(serverMemberUpdateFrame.data)

                serverMemberUpdateFrame.clear?.forEach {
                    when (it) {
                        "Avatar" -> updated = updated.copy(avatar = null)
                        "Nickname" -> updated = updated.copy(nickname = null)
                        else -> Log.e("RealtimeSocket", "Unknown server member clear field: $it")
                    }
                }

                Log.d("RealtimeSocket", "Updated member: $updated")

                RevoltAPI.members.setMember(serverMemberUpdateFrame.id.server, updated)
            }

            "ServerMemberJoin" -> {
                val serverMemberJoinFrame =
                    RevoltJson.decodeFromString(ServerMemberJoinFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received server member join frame for ${serverMemberJoinFrame.user} in ${serverMemberJoinFrame.id}."
                )

                val member = fetchMember(serverMemberJoinFrame.id, serverMemberJoinFrame.user)

                RevoltAPI.members.setMember(serverMemberJoinFrame.id, member)
            }

            "ServerMemberLeave" -> {
                val serverMemberLeaveFrame =
                    RevoltJson.decodeFromString(ServerMemberLeaveFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received server member leave frame for ${serverMemberLeaveFrame.user} in ${serverMemberLeaveFrame.id}."
                )

                RevoltAPI.members.removeMember(
                    serverMemberLeaveFrame.id,
                    serverMemberLeaveFrame.user
                )
            }

            "ServerRoleUpdate" -> {
                val serverRoleUpdateFrame =
                    RevoltJson.decodeFromString(ServerRoleUpdateFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received server role update frame for ${serverRoleUpdateFrame.id}."
                )

                val server = RevoltAPI.serverCache[serverRoleUpdateFrame.id]
                if (server == null) {
                    Log.d(
                        "RealtimeSocket",
                        "Server ${serverRoleUpdateFrame.id} not found in cache. Ignoring role update."
                    )
                    return
                }

                val existingRole = server.roles?.get(serverRoleUpdateFrame.roleId)
                if (existingRole == null) {
                    // New role.
                    Log.d(
                        "RealtimeSocket",
                        "New role ${serverRoleUpdateFrame.roleId} in server ${serverRoleUpdateFrame.id}. Adding to cache."
                    )
                    val newRole = Role().mergeWithPartial(serverRoleUpdateFrame.data)
                    val newServer = server.copy(
                        roles = server.roles?.plus(
                            Pair(serverRoleUpdateFrame.roleId, newRole)
                        ) ?: mapOf(serverRoleUpdateFrame.roleId to newRole)
                    )
                    RevoltAPI.serverCache[serverRoleUpdateFrame.id] = newServer
                } else {
                    // True role update.
                    Log.d(
                        "RealtimeSocket",
                        "Updating existing role ${serverRoleUpdateFrame.roleId} in server ${serverRoleUpdateFrame.id}."
                    )
                    val updatedRole = existingRole.mergeWithPartial(serverRoleUpdateFrame.data)
                    val newServer = server.copy(
                        roles = server.roles.plus(
                            Pair(serverRoleUpdateFrame.roleId, updatedRole)
                        )
                    )
                    RevoltAPI.serverCache[serverRoleUpdateFrame.id] = newServer
                }
            }

            "ServerRoleDelete" -> {
                val serverRoleDeleteFrame =
                    RevoltJson.decodeFromString(ServerRoleDeleteFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received server role delete frame for ${serverRoleDeleteFrame.id} and role ${serverRoleDeleteFrame.roleId}."
                )

                val server = RevoltAPI.serverCache[serverRoleDeleteFrame.id]
                if (server == null) {
                    Log.d(
                        "RealtimeSocket",
                        "Server ${serverRoleDeleteFrame.id} not found in cache. Ignoring role delete."
                    )
                    return
                }

                val newRoles = server.roles?.toMutableMap() ?: mutableMapOf()
                newRoles.remove(serverRoleDeleteFrame.roleId)

                RevoltAPI.serverCache[serverRoleDeleteFrame.id] =
                    server.copy(roles = newRoles)
            }

            "Authenticated" -> {
                SyncedSettings.fetch()
                LoadedSettings.hydrateWithSettings(SyncedSettings)
            }

            else -> {
                Log.i("RealtimeSocket", "Unknown frame: $rawFrame")
            }
        }
    }

    private suspend fun pushReconnectEvent() {
        RevoltAPI.wsFrameChannel.send(RealtimeSocketFrames.Reconnected)
    }

    suspend fun beginTyping(channelId: String) {
        if (disconnectionState != DisconnectionState.Connected) return

        val beginTypingFrame = BeginTypingFrame("BeginTyping", channelId)
        socket?.send(
            RevoltJson.encodeToString(
                BeginTypingFrame.serializer(),
                beginTypingFrame
            )
        )
    }

    suspend fun endTyping(channelId: String) {
        if (disconnectionState != DisconnectionState.Connected) return

        val endTypingFrame = EndTypingFrame("EndTyping", channelId)
        socket?.send(
            RevoltJson.encodeToString(
                EndTypingFrame.serializer(),
                endTypingFrame
            )
        )
    }
}
