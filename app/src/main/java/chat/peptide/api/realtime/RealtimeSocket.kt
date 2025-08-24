package chat.peptide.api.realtime

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import chat.peptide.PeptideApplication
import chat.peptide.api.PeptideAPI
import chat.peptide.api.PeptideHttp
import chat.peptide.api.PeptideJson
import chat.peptide.api.realtime.frames.receivable.AnyFrame
import chat.peptide.api.realtime.frames.receivable.BulkFrame
import chat.peptide.api.realtime.frames.receivable.ChannelAckFrame
import chat.peptide.api.realtime.frames.receivable.ChannelDeleteFrame
import chat.peptide.api.realtime.frames.receivable.ChannelStartTypingFrame
import chat.peptide.api.realtime.frames.receivable.ChannelStopTypingFrame
import chat.peptide.api.realtime.frames.receivable.ChannelUpdateFrame
import chat.peptide.api.realtime.frames.receivable.MessageAppendFrame
import chat.peptide.api.realtime.frames.receivable.MessageDeleteFrame
import chat.peptide.api.realtime.frames.receivable.MessageFrame
import chat.peptide.api.realtime.frames.receivable.MessageReactFrame
import chat.peptide.api.realtime.frames.receivable.MessageUpdateFrame
import chat.peptide.api.realtime.frames.receivable.PongFrame
import chat.peptide.api.realtime.frames.receivable.ReadyFrame
import chat.peptide.api.realtime.frames.receivable.ServerCreateFrame
import chat.peptide.api.realtime.frames.receivable.ServerDeleteFrame
import chat.peptide.api.realtime.frames.receivable.ServerMemberJoinFrame
import chat.peptide.api.realtime.frames.receivable.ServerMemberLeaveFrame
import chat.peptide.api.realtime.frames.receivable.ServerMemberUpdateFrame
import chat.peptide.api.realtime.frames.receivable.ServerRoleDeleteFrame
import chat.peptide.api.realtime.frames.receivable.ServerRoleUpdateFrame
import chat.peptide.api.realtime.frames.receivable.ServerUpdateFrame
import chat.peptide.api.realtime.frames.receivable.UserRelationshipFrame
import chat.peptide.api.realtime.frames.receivable.UserUpdateFrame
import chat.peptide.api.realtime.frames.sendable.AuthorizationFrame
import chat.peptide.api.realtime.frames.sendable.BeginTypingFrame
import chat.peptide.api.realtime.frames.sendable.EndTypingFrame
import chat.peptide.api.realtime.frames.sendable.PingFrame
import chat.peptide.api.routes.server.fetchMember
import chat.peptide.api.schemas.Channel
import chat.peptide.api.schemas.ChannelType
import chat.peptide.api.schemas.Role
import chat.peptide.api.settings.LoadedSettings
import chat.peptide.api.settings.SyncedSettings
import chat.peptide.c2dm.ChannelRegistrator
import chat.peptide.persistence.Database
import chat.peptide.persistence.SqlStorage
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
        get() = ChannelRegistrator(PeptideApplication.instance)

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

        PeptideHttp.ws(PeptideAPI.getCurrentWebSocketUrl()) {
            socket = this

            Log.d("RealtimeSocket", "Connected to websocket.")
            updateDisconnectionState(DisconnectionState.Connected)
            pushReconnectEvent()

            // Send authorization frame
            val authFrame = AuthorizationFrame("Authenticate", token)
            val authFrameString =
                PeptideJson.encodeToString(AuthorizationFrame.serializer(), authFrame)

            Log.d(
                "RealtimeSocket",
                "Sending authorization frame: ${
                    authFrameString.replace(
                        token,
                        "X".repeat(token.length)
                    )
                }"
            )
            send(PeptideJson.encodeToString(AuthorizationFrame.serializer(), authFrame))

            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    val frameString = frame.readText()
                    val frameType =
                        PeptideJson.decodeFromString(AnyFrame.serializer(), frameString).type

                    handleFrame(frameType, frameString)
                }
            }
        }
    }

    suspend fun sendPing() {
        if (disconnectionState != DisconnectionState.Connected) return

        val pingPacket = PingFrame("Ping", System.currentTimeMillis())
        socket?.send(PeptideJson.encodeToString(PingFrame.serializer(), pingPacket))
        Log.d("RealtimeSocket", "Sent ping frame with ${pingPacket.data}")
    }

    private suspend fun handleFrame(type: String, rawFrame: String) {
        when (type) {
            "Pong" -> {
                val pongFrame = PeptideJson.decodeFromString(PongFrame.serializer(), rawFrame)
                Log.d("RealtimeSocket", "Received pong frame for ${pongFrame.data}")
            }

            "Bulk" -> {
                val bulkFrame = PeptideJson.decodeFromString(BulkFrame.serializer(), rawFrame)
                Log.d("RealtimeSocket", "Received bulk frame with ${bulkFrame.v.size} sub-frames.")
                bulkFrame.v.forEach { subFrame ->
                    val subFrameType =
                        PeptideJson.decodeFromString(AnyFrame.serializer(), subFrame.toString()).type
                    handleFrame(subFrameType, subFrame.toString())
                }
            }

            "Ready" -> {
                val readyFrame = PeptideJson.decodeFromString(ReadyFrame.serializer(), rawFrame)

                logcat {
                    "Received ready frame with ${readyFrame.users.size} users, " +
                            "${readyFrame.servers.size} servers, " +
                            "${readyFrame.channels.size} channels, " +
                            "${readyFrame.emojis.size} emojis, " +
                            "and ${readyFrame.voiceStates.size} voice states."
                }

                Log.d("RealtimeSocket", "Adding users to cache.")
                val userMap = readyFrame.users.associateBy { it.id!! }
                PeptideAPI.userCache.putAll(userMap)

                Log.d("RealtimeSocket", "Adding servers to cache.")
                val serverMap = readyFrame.servers.associateBy { it.id!! }
                PeptideAPI.serverCache.putAll(serverMap)

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
                    PeptideAPI.serverCache.remove(it)
                }

                Log.d("RealtimeSocket", "Adding channels to cache.")
                val channelMap = readyFrame.channels.associateBy { it.id!! }
                PeptideAPI.channelCache.putAll(channelMap)

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
                        if (it.channelType == ChannelType.DirectMessage) it.recipients?.firstOrNull { u -> u != PeptideAPI.selfId } else null,
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
                    PeptideAPI.channelCache.remove(it)
                }

                Log.d("RealtimeSocket", "Adding emojis to cache.")
                val emojiMap = readyFrame.emojis.associateBy { it.id!! }
                PeptideAPI.emojiCache.putAll(emojiMap)

                Log.d("RealtimeSocket", "Registering push notification channels.")
                channelRegistrator.register()

                PeptideAPI.closeHydration()
            }

            "Message" -> {
                val messageFrame = PeptideJson.decodeFromString(MessageFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received message frame for ${messageFrame.id} in channel ${messageFrame.channel}."
                )

                if (messageFrame.id == null) {
                    Log.d("RealtimeSocket", "Message frame has no ID or channel. Ignoring.")
                    return
                }

                PeptideAPI.messageCache[messageFrame.id] = messageFrame

                messageFrame.channel?.let {
                    if (PeptideAPI.channelCache[it] == null) {
                        Log.d("RealtimeSocket", "Channel $it not found in cache. Ignoring.")
                        return
                    }

                    PeptideAPI.channelCache[it] =
                        PeptideAPI.channelCache[it]!!.copy(lastMessageID = messageFrame.id)

                    PeptideAPI.wsFrameChannel.send(messageFrame)
                }
            }

            "MessageAppend" -> {
                val messageAppendFrame =
                    PeptideJson.decodeFromString(MessageAppendFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received message append frame for ${messageAppendFrame.id} in channel ${messageAppendFrame.channel}."
                )

                var message = PeptideAPI.messageCache[messageAppendFrame.id]

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

                PeptideAPI.messageCache[messageAppendFrame.id] = message!!

                PeptideAPI.wsFrameChannel.send(messageAppendFrame)
            }

            "MessageUpdate" -> {
                val messageUpdateFrame =
                    PeptideJson.decodeFromString(MessageUpdateFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received message update frame for ${messageUpdateFrame.id} in channel ${messageUpdateFrame.channel}."
                )

                val oldMessage = PeptideAPI.messageCache[messageUpdateFrame.id]
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
                        PeptideJson.decodeFromJsonElement(
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

                PeptideAPI.messageCache[messageUpdateFrame.id] =
                    oldMessage.mergeWithPartial(rawMessage)

                messageUpdateFrame.channel.let {
                    if (PeptideAPI.channelCache[it] == null) {
                        Log.d("RealtimeSocket", "Channel $it not found in cache. Ignoring.")
                        return
                    }
                }

                PeptideAPI.wsFrameChannel.send(messageUpdateFrame)
            }

            "MessageDelete" -> {
                val messageDeleteFrame =
                    PeptideJson.decodeFromString(MessageDeleteFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received message react frame for ${messageDeleteFrame.id}."
                )

                val message = PeptideAPI.messageCache[messageDeleteFrame.id]
                if (message == null) {
                    Log.d(
                        "RealtimeSocket",
                        "Message ${messageDeleteFrame.id} not found in cache. Will not delete."
                    )
                    return
                }

                PeptideAPI.messageCache.remove(messageDeleteFrame.id)
                PeptideAPI.wsFrameChannel.send(messageDeleteFrame)
            }

            "MessageReact" -> {
                val messageReactFrame =
                    PeptideJson.decodeFromString(MessageReactFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received message react frame for ${messageReactFrame.id}."
                )

                val oldMessage = PeptideAPI.messageCache[messageReactFrame.id]
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

                PeptideAPI.messageCache[messageReactFrame.id] =
                    oldMessage.copy(reactions = reactions)

                PeptideAPI.wsFrameChannel.send(messageReactFrame)
            }

            "MessageUnreact" -> {
                val messageUnreactFrame =
                    PeptideJson.decodeFromString(MessageReactFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received message unreact frame for ${messageUnreactFrame.id}."
                )

                val oldMessage = PeptideAPI.messageCache[messageUnreactFrame.id]
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

                PeptideAPI.messageCache[messageUnreactFrame.id] =
                    oldMessage.copy(reactions = reactions)

                PeptideAPI.wsFrameChannel.send(messageUnreactFrame)
            }

            "UserUpdate" -> {
                val userUpdateFrame =
                    PeptideJson.decodeFromString(UserUpdateFrame.serializer(), rawFrame)

                val existing = PeptideAPI.userCache[userUpdateFrame.id]
                    ?: return // if we don't have the user no point in updating it

                if (userUpdateFrame.clear != null) {
                    if (userUpdateFrame.clear.contains("Avatar")) {
                        PeptideAPI.userCache[userUpdateFrame.id] =
                            existing.copy(avatar = null)
                    }
                }

                PeptideAPI.userCache[userUpdateFrame.id] =
                    existing.mergeWithPartial(userUpdateFrame.data)
            }

            "UserRelationship" -> {
                val userRelationshipFrame =
                    PeptideJson.decodeFromString(UserRelationshipFrame.serializer(), rawFrame)

                val existing = PeptideAPI.userCache[userRelationshipFrame.user.id]

                if (existing == null && userRelationshipFrame.user.id != null) {
                    PeptideAPI.userCache[userRelationshipFrame.user.id] =
                        userRelationshipFrame.user.copy(
                            relationship = userRelationshipFrame.status ?: "None"
                        )
                } else if (existing != null && userRelationshipFrame.user.id != null) {
                    val merged = existing.mergeWithPartial(userRelationshipFrame.user).copy(
                        relationship = userRelationshipFrame.status ?: "None"
                    )
                    PeptideAPI.userCache[userRelationshipFrame.user.id] = merged
                } else {
                    Log.w("RealtimeSocket", "Invalid UserRelationship frame: $rawFrame")
                }
            }

            "ChannelUpdate" -> {
                val channelUpdateFrame =
                    PeptideJson.decodeFromString(ChannelUpdateFrame.serializer(), rawFrame)

                val existing = PeptideAPI.channelCache[channelUpdateFrame.id]
                    ?: return // if we don't have the channel no point in updating it

                val combined = existing.mergeWithPartial(channelUpdateFrame.data)
                PeptideAPI.channelCache[channelUpdateFrame.id] = combined

                database.channelQueries.upsert(
                    channelUpdateFrame.id,
                    combined.channelType?.value ?: ChannelType.TextChannel.value,
                    combined.user,
                    combined.name,
                    combined.owner,
                    combined.description,
                    if (combined.channelType == ChannelType.DirectMessage) combined.recipients?.firstOrNull { u -> u != PeptideAPI.selfId } else null,
                    combined.icon?.id,
                    combined.lastMessageID,
                    if (combined.active == true) 1L else 0L,
                    if (combined.nsfw == true) 1L else 0L,
                    combined.server
                )
            }

            "ChannelCreate" -> {
                val channelCreateFrame =
                    PeptideJson.decodeFromString(Channel.serializer(), rawFrame)

                Log.d(
                    "RealtimeSocket",
                    "Received channel create frame for ${channelCreateFrame.id}, with name ${channelCreateFrame.name}. Adding to cache."
                )

                PeptideAPI.channelCache[channelCreateFrame.id!!] = channelCreateFrame
                database.channelQueries.upsert(
                    channelCreateFrame.id,
                    channelCreateFrame.channelType?.value ?: ChannelType.TextChannel.value,
                    channelCreateFrame.user,
                    channelCreateFrame.name,
                    channelCreateFrame.owner,
                    channelCreateFrame.description,
                    if (channelCreateFrame.channelType == ChannelType.DirectMessage) channelCreateFrame.recipients?.firstOrNull { u -> u != PeptideAPI.selfId } else null,
                    channelCreateFrame.icon?.id,
                    channelCreateFrame.lastMessageID,
                    if (channelCreateFrame.active == true) 1L else 0L,
                    if (channelCreateFrame.nsfw == true) 1L else 0L,
                    channelCreateFrame.server
                )
            }

            "ChannelDelete" -> {
                val channelDeleteFrame =
                    PeptideJson.decodeFromString(ChannelDeleteFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received channel delete frame for ${channelDeleteFrame.id}. Removing from cache."
                )

                val currentChannel = PeptideAPI.channelCache[channelDeleteFrame.id]
                if (currentChannel == null) {
                    Log.d(
                        "RealtimeSocket",
                        "Channel ${channelDeleteFrame.id} not found in cache. Ignoring."
                    )
                    return
                }

                PeptideAPI.channelCache.remove(channelDeleteFrame.id)
                database.channelQueries.delete(channelDeleteFrame.id)

                if (currentChannel.server != null) {
                    val existingServer = PeptideAPI.serverCache[currentChannel.server]

                    if (existingServer == null) {
                        Log.d(
                            "RealtimeSocket",
                            "Server ${currentChannel.server} not found in cache. Ignoring."
                        )
                        return
                    }

                    PeptideAPI.serverCache[currentChannel.server] = existingServer.copy(
                        channels = existingServer.channels?.filter { it != channelDeleteFrame.id }
                            ?: emptyList()
                    )
                }

                PeptideAPI.wsFrameChannel.send(channelDeleteFrame)
            }

            "ChannelAck" -> {
                val channelAckFrame =
                    PeptideJson.decodeFromString(ChannelAckFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received channel ack frame for ${channelAckFrame.id} with new newest ${channelAckFrame.messageId}."
                )

                PeptideAPI.unreads.processExternalAck(channelAckFrame.id, channelAckFrame.messageId)
            }

            "ServerCreate" -> {
                val serverCreateFrame =
                    PeptideJson.decodeFromString(ServerCreateFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received server create frame for ${serverCreateFrame.id}, with name ${serverCreateFrame.server.name}. Adding to cache."
                )

                PeptideAPI.serverCache[serverCreateFrame.id] = serverCreateFrame.server

                serverCreateFrame.channels.forEach { channel ->
                    if (channel.id == null) return@forEach
                    PeptideAPI.channelCache[channel.id] = channel
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
                    PeptideJson.decodeFromString(ChannelStartTypingFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received channel start typing frame for ${channelStartTypingFrame.id}."
                )

                PeptideAPI.wsFrameChannel.send(channelStartTypingFrame)
            }

            "ChannelStopTyping" -> {
                val channelStopTypingFrame =
                    PeptideJson.decodeFromString(ChannelStopTypingFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received channel stop typing frame for ${channelStopTypingFrame.id}."
                )

                PeptideAPI.wsFrameChannel.send(channelStopTypingFrame)
            }

            "ServerUpdate" -> {
                val serverUpdateFrame =
                    PeptideJson.decodeFromString(ServerUpdateFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received server update frame for ${serverUpdateFrame.id}."
                )

                val existing = PeptideAPI.serverCache[serverUpdateFrame.id]
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

                PeptideAPI.serverCache[serverUpdateFrame.id] = updated

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
                    PeptideJson.decodeFromString(ServerDeleteFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received server delete frame for ${serverDeleteFrame.id}."
                )

                PeptideAPI.serverCache.remove(serverDeleteFrame.id)
                database.serverQueries.delete(serverDeleteFrame.id)
            }

            "ServerMemberUpdate" -> {
                val serverMemberUpdateFrame =
                    PeptideJson.decodeFromString(ServerMemberUpdateFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received server member update frame for ${serverMemberUpdateFrame.id.user} in ${serverMemberUpdateFrame.id.server}."
                )

                val existing = PeptideAPI.members.getMember(
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

                PeptideAPI.members.setMember(serverMemberUpdateFrame.id.server, updated)
            }

            "ServerMemberJoin" -> {
                val serverMemberJoinFrame =
                    PeptideJson.decodeFromString(ServerMemberJoinFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received server member join frame for ${serverMemberJoinFrame.user} in ${serverMemberJoinFrame.id}."
                )

                val member = fetchMember(serverMemberJoinFrame.id, serverMemberJoinFrame.user)

                PeptideAPI.members.setMember(serverMemberJoinFrame.id, member)
            }

            "ServerMemberLeave" -> {
                val serverMemberLeaveFrame =
                    PeptideJson.decodeFromString(ServerMemberLeaveFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received server member leave frame for ${serverMemberLeaveFrame.user} in ${serverMemberLeaveFrame.id}."
                )

                PeptideAPI.members.removeMember(
                    serverMemberLeaveFrame.id,
                    serverMemberLeaveFrame.user
                )
            }

            "ServerRoleUpdate" -> {
                val serverRoleUpdateFrame =
                    PeptideJson.decodeFromString(ServerRoleUpdateFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received server role update frame for ${serverRoleUpdateFrame.id}."
                )

                val server = PeptideAPI.serverCache[serverRoleUpdateFrame.id]
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
                    PeptideAPI.serverCache[serverRoleUpdateFrame.id] = newServer
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
                    PeptideAPI.serverCache[serverRoleUpdateFrame.id] = newServer
                }
            }

            "ServerRoleDelete" -> {
                val serverRoleDeleteFrame =
                    PeptideJson.decodeFromString(ServerRoleDeleteFrame.serializer(), rawFrame)
                Log.d(
                    "RealtimeSocket",
                    "Received server role delete frame for ${serverRoleDeleteFrame.id} and role ${serverRoleDeleteFrame.roleId}."
                )

                val server = PeptideAPI.serverCache[serverRoleDeleteFrame.id]
                if (server == null) {
                    Log.d(
                        "RealtimeSocket",
                        "Server ${serverRoleDeleteFrame.id} not found in cache. Ignoring role delete."
                    )
                    return
                }

                val newRoles = server.roles?.toMutableMap() ?: mutableMapOf()
                newRoles.remove(serverRoleDeleteFrame.roleId)

                PeptideAPI.serverCache[serverRoleDeleteFrame.id] =
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
        PeptideAPI.wsFrameChannel.send(RealtimeSocketFrames.Reconnected)
    }

    suspend fun beginTyping(channelId: String) {
        if (disconnectionState != DisconnectionState.Connected) return

        val beginTypingFrame = BeginTypingFrame("BeginTyping", channelId)
        socket?.send(
            PeptideJson.encodeToString(
                BeginTypingFrame.serializer(),
                beginTypingFrame
            )
        )
    }

    suspend fun endTyping(channelId: String) {
        if (disconnectionState != DisconnectionState.Connected) return

        val endTypingFrame = EndTypingFrame("EndTyping", channelId)
        socket?.send(
            PeptideJson.encodeToString(
                EndTypingFrame.serializer(),
                endTypingFrame
            )
        )
    }
}
