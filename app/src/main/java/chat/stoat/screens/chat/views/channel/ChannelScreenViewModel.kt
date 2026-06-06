package chat.stoat.screens.chat.views.channel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.util.fastDistinctBy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.StoatJson
import chat.stoat.api.internals.ChannelUtils
import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.internals.Roles
import chat.stoat.api.internals.SpecialUsers
import chat.stoat.api.internals.ULID
import chat.stoat.api.internals.has
import chat.stoat.api.realtime.RealtimeSocket
import chat.stoat.api.realtime.RealtimeSocketFrames
import chat.stoat.api.realtime.frames.receivable.ChannelDeleteFrame
import chat.stoat.api.realtime.frames.receivable.ChannelStartTypingFrame
import chat.stoat.api.realtime.frames.receivable.ChannelStopTypingFrame
import chat.stoat.api.realtime.frames.receivable.MessageAppendFrame
import chat.stoat.api.realtime.frames.receivable.MessageDeleteFrame
import chat.stoat.api.realtime.frames.receivable.MessageFrame
import chat.stoat.api.realtime.frames.receivable.MessageReactFrame
import chat.stoat.api.realtime.frames.receivable.MessageUnreactFrame
import chat.stoat.api.realtime.frames.receivable.MessageUpdateFrame
import chat.stoat.api.routes.channel.SendMessageReply
import chat.stoat.api.routes.channel.ackChannel
import chat.stoat.api.routes.channel.editMessage
import chat.stoat.api.routes.channel.fetchMessagesFromChannel
import chat.stoat.api.routes.channel.sendMessage
import chat.stoat.api.routes.microservices.autumn.FileArgs
import chat.stoat.api.routes.microservices.autumn.MAX_ATTACHMENTS_PER_MESSAGE
import chat.stoat.api.routes.microservices.autumn.uploadToAutumn
import chat.stoat.api.routes.server.fetchMember
import chat.stoat.api.routes.user.addUserIfUnknown
import chat.stoat.api.routes.user.fetchUser
import chat.stoat.api.settings.GeoStateProvider
import chat.stoat.callbacks.Action
import chat.stoat.callbacks.ActionChannel
import chat.stoat.callbacks.UiCallback
import chat.stoat.callbacks.UiCallbacks
import chat.stoat.core.model.schemas.Channel
import chat.stoat.core.model.schemas.Message
import chat.stoat.internals.text.MessageProcessor
import chat.stoat.internals.text.stripPUAChars
import chat.stoat.composables.markdown.prose.easyLineBreaks
import chat.stoat.markdown.StoatMarkdownFlavour
import chat.stoat.persistence.KVStorage
import chat.stoat.screens.chat.ChatRouterDestination
import chat.stoat.settings.providers.AgeGateUnlockedStorageProvider
import com.mikepenz.markdown.model.State
import com.mikepenz.markdown.model.parseMarkdownFlow
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.time.ZoneId

class ChannelScreenViewModel(
    private val kvStorage: KVStorage,
) : ViewModel() {
    var items = mutableStateListOf<ChannelScreenItem>()
    var typingUsers = mutableStateListOf<String>()

    var channelId by mutableStateOf<String?>(null)
    val channel: Channel?
        get() = channelId?.let { StoatAPI.channelCache[it] }

    var activePane by mutableStateOf<ChannelScreenActivePane>(ChannelScreenActivePane.None)
    var keyboardHeight by mutableIntStateOf(0)

    // Setting [initialTextFieldValue] causes the text field to switch value.
    // However [initialTextFieldValue] gets de-synced with the actual text field value when the user types.
    // For a field that keeps track of the actual text field value, see [draftContent].
    // Setting [draftContent] does not cause the text field to switch value.
    // Note that if [initialTextFieldValue] is set to the same value it has now, the text field will not switch value.
    // Set [initialTextFieldValueDirtyMarker] to a new value to force the text field to switch value.
    var initialTextFieldValue by mutableStateOf("")
    var initialTextFieldValueDirtyMarker by mutableStateOf("")
    var draftContent by mutableStateOf("")
    var draftAttachments = mutableStateListOf<FileArgs>()
    var draftReplyTo = mutableStateListOf<SendMessageReply>()
    var attachmentUploadProgress by mutableStateOf(0f)

    var endOfChannel by mutableStateOf(false)
    var didInitialChannelFetch by mutableStateOf(false)

    var ensuredSelfMember by mutableStateOf(false)

    var denyMessageField by mutableStateOf(false)
    var denyMessageFieldReasonResource by mutableIntStateOf(R.string.typing_blank)

    var editingMessage by mutableStateOf<String?>(null)
    var isSending by mutableStateOf(false)

    var ageGateUnlocked by mutableStateOf<Boolean?>(null)
    var showGeoGate by mutableStateOf(false)

    init {
        viewModelScope.launch {
            keyboardHeight = kvStorage.getInt("keyboardHeight") ?: 900 // reasonable default for now
        }
    }

    private var loadMessagesJob: Job? = null

    fun switchChannel(id: String) {
        // Reset state
        this.loadMessagesJob?.cancel()
        this.channelId = id
        this.items = mutableStateListOf(ChannelScreenItem.Loading)
        this.activePane = ChannelScreenActivePane.None
        this.typingUsers = mutableStateListOf()
        this.endOfChannel = false
        this.didInitialChannelFetch = false
        this.ensuredSelfMember = false
        this.denyMessageField = false
        this.denyMessageFieldReasonResource = R.string.typing_blank
        this.editingMessage = null
        this.isSending = false
        this.ageGateUnlocked = channel?.nsfw != true
        this.showGeoGate = when {
            channel?.nsfw == true && GeoStateProvider.geoState?.isAgeRestrictedGeo == true -> true
            else -> false
        }
        viewModelScope.launch {
            if (ageGateUnlocked != true) {
                ageGateUnlocked = AgeGateUnlockedStorageProvider.getAgeGateUnlocked()
            }
        }

        viewModelScope.launch {
            putDraftContent(kvStorage.get("draftContent/$id") ?: "", true)
        }
        this.draftAttachments = mutableStateListOf()
        this.draftReplyTo = mutableStateListOf()
        this.attachmentUploadProgress = 0f

        viewModelScope.launch {
            ensureSelfHasMember()
            denyMessageFieldIfNeeded()
        }

        this.loadMessages(50, markLastAsRead = true)
    }

    suspend fun unlockAgeGate() {
        AgeGateUnlockedStorageProvider.setAgeGateUnlocked(true)
        ageGateUnlocked = true
    }

    private suspend fun ensureSelfHasMember() {
        channel?.server?.let { serverId ->
            StoatAPI.selfId?.let { selfId ->
                if (!StoatAPI.members.hasMember(serverId, selfId)) {
                    try {
                        fetchMember(serverId, selfId)
                    } catch (e: Exception) {
                        Log.e("ChannelScreenViewModel", "Failed to fetch member", e)
                    }
                }

                ensuredSelfMember = true
            }
        }
    }

    private suspend fun denyMessageFieldIfNeeded() {
        if (channel == null) return

        val selfUser = StoatAPI.userCache[StoatAPI.selfId] ?: return
        val selfMember = if (channel!!.server == null) {
            null
        } else {
            channel?.server?.let { serverId ->
                try {
                    StoatAPI.members.getMember(serverId, selfUser.id!!) ?: fetchMember(
                        serverId,
                        selfUser.id!!
                    )
                } catch (e: Exception) {
                    Log.e("ChannelScreenViewModel", "Failed to fetch member", e)
                    null
                }
            }
        }

        val permission = Roles.permissionFor(channel!!, selfUser, selfMember)
        val canSend = permission has PermissionBit.SendMessage

        val partnerId = ChannelUtils.resolveDMPartner(channel!!)

        denyMessageField = when {
            partnerId == SpecialUsers.PLATFORM_MODERATION_USER -> true
            !canSend -> true
            else -> false
        }

        denyMessageFieldReasonResource = when {
            partnerId == SpecialUsers.PLATFORM_MODERATION_USER -> R.string.message_field_denied_platform_moderation
            !canSend -> R.string.message_field_denied_no_permission
            else -> R.string.message_field_denied_generic
        }
    }

    fun putAtCursorPosition(text: String) {
        putDraftContent(draftContent + text, true)
    }

    private var lastSentBeginTyping: Instant? = null

    private fun startTyping() {
        if (editingMessage != null) return
        if (lastSentBeginTyping != null) {
            val diff = Clock.System.now() - lastSentBeginTyping!!
            if (diff.inWholeSeconds < 1) return
        }

        viewModelScope.launch {
            withContext(StoatAPI.realtimeContext) {
                channel?.id?.let {
                    RealtimeSocket.beginTyping(it)
                }
            }
        }

        lastSentBeginTyping = Clock.System.now()
    }

    private var stopTypingJob: Job? = null

    private fun queueStopTyping() {
        stopTypingJob = viewModelScope.launch {
            delay(5000)
            stopTyping()
        }
    }

    private fun stopTyping() {
        if (editingMessage != null) return
        viewModelScope.launch {
            withContext(StoatAPI.realtimeContext) {
                channel?.id?.let {
                    RealtimeSocket.endTyping(it)
                }
            }
        }
    }

    private suspend fun parseAst(content: String?): State? =
        content?.takeIf { it.isNotBlank() }?.let { c ->
            val prepared = injectMassMentionMarkers(easyLineBreaks(c.stripPUAChars()))
            parseMarkdownFlow(prepared, flavour = StoatMarkdownFlavour(prepared))
                .first { it !is State.Loading }
        }

    // Replaces @everyone/@online with <@EVERYONE>/<@ONLINE> outside code blocks and inline code spans.
    private fun injectMassMentionMarkers(content: String): String {
        val sb = StringBuilder(content.length)
        var i = 0
        var inFence = false
        var inCodeSpan = false

        while (i < content.length) {
            val ch = content[i]

            if (!inCodeSpan && (ch == '`' || ch == '~')) {
                val lineStart = sb.lastIndexOf('\n') + 1
                val atLineStart = sb.length == lineStart || sb.substring(lineStart).isBlank()
                if (atLineStart) {
                    var run = 0
                    while (i + run < content.length && content[i + run] == ch) run++
                    if (run >= 3) {
                        inFence = !inFence
                        sb.append(content, i, i + run)
                        i += run
                        continue
                    }
                }
            }

            if (!inFence && ch == '`') {
                inCodeSpan = !inCodeSpan
                sb.append(ch)
                i++
                continue
            }

            if (!inFence && !inCodeSpan && ch == '@') {
                when {
                    content.startsWith("everyone", i + 1) &&
                        (i + 9 >= content.length || !content[i + 9].isLetterOrDigit()) -> {
                        sb.append("<@EVERYONE>")
                        i += 9
                        continue
                    }
                    content.startsWith("online", i + 1) &&
                        (i + 7 >= content.length || !content[i + 7].isLetterOrDigit()) -> {
                        sb.append("<@ONLINE>")
                        i += 7
                        continue
                    }
                }
            }

            sb.append(ch)
            i++
        }

        return sb.toString()
    }

    /**
     * Puts the draft content in the KV storage, the in-memory state of the message content,
     * and, if [setInitial] is true, updates the text field to say the new [content].
     */
    fun putDraftContent(content: String, setInitial: Boolean = false) {
        viewModelScope.launch {
            kvStorage.set("draftContent/${channel?.id}", content)
        }

        if (editingMessage == null) {
            if (content.isNotBlank()) {
                startTyping()
                stopTypingJob?.cancel()
                queueStopTyping()
            } else {
                stopTyping()
            }
        }

        draftContent = content
        if (setInitial) {
            initialTextFieldValue = content
            initialTextFieldValueDirtyMarker = ULID.makeNext()
        }
    }

    suspend fun addReplyTo(messageId: String) {
        if (draftReplyTo.size >= 5) return
        if (draftReplyTo.any { it.id == messageId }) return

        val shouldMention = kvStorage.getBoolean("mentionOnReply") ?: false
        draftReplyTo.add(SendMessageReply(messageId, shouldMention))
    }

    suspend fun toggleMentionOnReply(messageId: String) {
        val shouldMention = draftReplyTo.find { it.id == messageId }?.mention ?: false
        val newItems = draftReplyTo.map {
            if (it.id == messageId) {
                it.copy(mention = !shouldMention)
            } else {
                it
            }
        }
        draftReplyTo.clear()
        draftReplyTo.addAll(newItems)
        kvStorage.set("mentionOnReply", !shouldMention)
    }

    fun updateSaveKeyboardHeight(height: Int) {
        viewModelScope.launch {
            kvStorage.set("keyboardHeight", height)
        }
        keyboardHeight = height
    }

    private suspend fun applyMessageEdit() {
        try {
            editMessage(
                channelId = channel?.id ?: return,
                messageId = editingMessage ?: return,
                newContent = draftContent,
            )
            putDraftContent("", true)
        } catch (e: Exception) {
            Log.e("ChannelScreenViewModel", "Failed to edit message", e)
        }
    }

    fun sendPendingMessage() {
        if (isSending) return

        if (editingMessage != null) {
            viewModelScope.launch {
                isSending = true
                applyMessageEdit()
                editingMessage = null
                isSending = false
            }
            return
        }

        // Immediately, make copies of the draft content and replyTo list, as
        // 1. they will be cleared
        // 2. if the user changes the content while the message is being sent we want to persist
        //    the original content
        val content = MessageProcessor.processOutgoing(draftContent, channel?.server)
        val replyTo = draftReplyTo.toList()

        // First we upload (the next 5) attachments...
        viewModelScope.launch {
            isSending = true
            val attachmentIds = arrayListOf<String>()
            val takenAttachments =
                this@ChannelScreenViewModel.draftAttachments.take(MAX_ATTACHMENTS_PER_MESSAGE)
            val totalTaken = takenAttachments.size

            takenAttachments.forEachIndexed { index, it ->
                try {
                    val id = uploadToAutumn(
                        it.file,
                        if (it.spoiler) "SPOILER_${it.filename}" else it.filename,
                        "attachments",
                        ContentType.parse(it.contentType),
                        onProgress = { current, total ->
                            attachmentUploadProgress =
                                ((current.toFloat() / total.toFloat()) / totalTaken.toFloat()) + (index.toFloat() / totalTaken.toFloat())
                        }
                    )
                    attachmentIds.add(id)
                } catch (e: Exception) {
                    Log.e("ChannelScreenViewModel", "Failed to upload attachment", e)
                    attachmentUploadProgress = 0f
                    isSending = false
                    // TODO show error message
                    return@launch
                }
            }

            val nonce = ULID.makeNext()
            val prospectiveMessage = Message(
                id = nonce,
                channel = channel?.id,
                author = StoatAPI.selfId,
                content = content,
                nonce = nonce,
                attachments = listOf(),
                replies = listOf(),
                tail = items.firstOrNull()?.let {
                    if (it is ChannelScreenItem.RegularMessage) {
                        it.message.author == StoatAPI.selfId
                    } else if (it is ChannelScreenItem.ProspectiveMessage) {
                        it.message.author == StoatAPI.selfId
                    } else {
                        false
                    }
                } ?: false
            )

            val ast = parseAst(content)
            updateItems(
                listOf(ChannelScreenItem.ProspectiveMessage(prospectiveMessage, ast)) + items
            )

            kvStorage.remove("draftContent/${channel?.id}")
            putDraftContent("", true)
            draftReplyTo.clear()
            attachmentUploadProgress = 0f

            this@ChannelScreenViewModel.draftAttachments.removeAll(takenAttachments)

            try {
                sendMessage(
                    channelId = channel?.id ?: return@launch,
                    content = content,
                    nonce = nonce,
                    replies = replyTo,
                    attachments = attachmentIds,
                    idempotencyKey = ULID.makeNext()
                )
            } catch (e: Exception) {
                Log.e("ChannelScreenViewModel", "Failed to send message", e)
                updateItems(
                    listOf(
                        ChannelScreenItem.FailedMessage(
                            prospectiveMessage,
                            ast
                        )
                    ) + items.filter { it !is ChannelScreenItem.ProspectiveMessage })
            } finally {
                isSending = false
            }
        }
    }

    /**
     * Load messages from the channel. If the channel is switched, the job will be cancelled.
     *
     * @param amount The amount of messages to load.
     * @param before Load [amount] messages before this message ID. Do not use with [around] or [after].
     * @param after Load [amount] messages after this message ID. Do not use with [around] or [before].
     * @param around Load [amount] messages around this message ID. Do not use with [before] or [after].
     * @param ignoreExisting If true, messages that are already in the list will not be added again. Possible performance degradation.
     */
    fun loadMessages(
        amount: Int,
        before: String? = null,
        after: String? = null,
        around: String? = null,
        ignoreExisting: Boolean = false,
        markLastAsRead: Boolean = false
    ) {
        val currentChannelId = channelId ?: return
        loadMessagesJob = viewModelScope.launch {
            try {
                val messages = arrayListOf<Message>()

                fetchMessagesFromChannel(
                    currentChannelId,
                    amount,
                    true,
                    before,
                    after,
                    around
                ).let {
                    if (it.messages.isNullOrEmpty() || it.messages!!.size < 50) {
                        endOfChannel = true
                    }

                    it.users?.forEach { user ->
                        if (!StoatAPI.userCache.containsKey(user.id)) {
                            StoatAPI.userCache[user.id!!] = user
                        }
                    }

                    it.messages?.forEach { message ->
                        addUserIfUnknown(message.author ?: return@forEach)
                        if (!StoatAPI.messageCache.containsKey(message.id)) {
                            StoatAPI.messageCache[message.id!!] = message
                        }
                        messages.add(message)
                    }

                    it.members?.forEach { member ->
                        if (!StoatAPI.members.hasMember(member.id!!.server, member.id!!.user)) {
                            StoatAPI.members.setMember(member.id!!.server, member)
                        }
                    }

                    if (markLastAsRead) {
                        ackMessage(messages.firstOrNull()?.id ?: return@launch)
                    }
                }

                val newItems = messages.filter {
                    if (ignoreExisting) {
                        items.none { m ->
                            when (m) {
                                is ChannelScreenItem.RegularMessage -> m.message.id == it.id
                                is ChannelScreenItem.ProspectiveMessage -> m.message.id == it.id
                                is ChannelScreenItem.SystemMessage -> m.message.id == it.id
                                is ChannelScreenItem.FailedMessage -> m.message.id == it.id
                                else -> false
                            }
                        }
                    } else {
                        true
                    }
                }.let { filtered ->
                    val result = mutableListOf<ChannelScreenItem>()
                    for (msg in filtered) {
                        result.add(
                            when {
                                msg.system != null -> ChannelScreenItem.SystemMessage(msg)
                                else -> ChannelScreenItem.RegularMessage(msg, parseAst(msg.content))
                            }
                        )
                    }
                    result
                }

                // Place items according to whether above/below/around was specified.
                // TODO: Aditionally, place LoadTriggers at the beginning and end of the list.
                val newItemsWithPosition = when {
                    before != null -> items + newItems
                    after != null -> newItems + items
                    // TODO around, which should place the new items in the middle of the list
                    else -> newItems
                }

                updateItems(newItemsWithPosition)

                if (!didInitialChannelFetch) {
                    didInitialChannelFetch = true
                }
            } catch (e: Exception) {
                Log.e("ChannelScreenViewModel", "Failed to fetch messages", e)
            }
        }
    }

    suspend fun ackMessage(messageId: String) {
        ackChannel(channel?.id ?: return, messageId)
    }

    suspend fun listenToWsEvents() {
        withContext(StoatAPI.realtimeContext) {
            StoatAPI.wsFrameChannel.onEach {
                when (it) {
                    is MessageFrame -> {
                        if (it.channel != channel?.id) return@onEach
                        // If we already have the message we are just catching up on the WebSocket connection. Skip
                        if (items.any { m -> (m is ChannelScreenItem.RegularMessage && m.message.id == it.id) || (m is ChannelScreenItem.SystemMessage && m.message.id == it.id) }) return@onEach

                        it.author?.let { userId ->
                            if (StoatAPI.userCache[userId] == null) {
                                StoatAPI.userCache[userId] = fetchUser(userId)
                            }
                        }
                        channel?.server?.let { serverId ->
                            try {
                                it.author?.let { userId ->
                                    fetchMember(serverId, userId)
                                }
                            } catch (e: Exception) {
                                Log.e("ChannelScreenViewModel", "Failed to fetch member", e)
                            }
                        }

                        if (didInitialChannelFetch) { // this check is so that we don't end up with a message that arrives at the same time as the initial fetch in front of the loading indicator
                            val newItem = when {
                                it.system != null -> ChannelScreenItem.SystemMessage(it)
                                else -> ChannelScreenItem.RegularMessage(it, parseAst(it.content))
                            }
                            updateItems(listOf(newItem) + items.filter { m ->
                                if (m is ChannelScreenItem.ProspectiveMessage) {
                                    m.message.id != it.nonce
                                } else {
                                    true
                                }
                            })
                        }

                        it.id?.let { mid -> ackMessage(mid) }
                    }

                    is MessageDeleteFrame -> {
                        if (it.channel != channel?.id) return@onEach

                        val newRenderableMessages =
                            items.filter { m ->
                                if (m is ChannelScreenItem.RegularMessage) {
                                    m.message.id != it.id
                                } else {
                                    true
                                }
                            }

                        updateItems(newRenderableMessages)
                    }


                    is MessageUpdateFrame -> {
                        if (it.channel != channel?.id) return@onEach

                        val messageFrame =
                            StoatJson.decodeFromJsonElement(MessageFrame.serializer(), it.data)

                        val currentMessage = items.find { m ->
                            m is ChannelScreenItem.RegularMessage && m.message.id == it.id
                        }
                        if (currentMessage == null) return@onEach

                        if (messageFrame.author != null) {
                            addUserIfUnknown(messageFrame.author!!)
                        }

                        updateItems(
                            items.map { m ->
                                if (m is ChannelScreenItem.RegularMessage && m.message.id == it.id) {
                                    val merged = m.message.mergeWithPartial(messageFrame)
                                    val ast =
                                        if (messageFrame.content != null) parseAst(merged.content) else m.mdAst
                                    ChannelScreenItem.RegularMessage(merged, ast)
                                } else {
                                    m
                                }
                            }
                        )
                    }

                    is MessageAppendFrame -> {
                        if (it.channel != channel?.id) return@onEach

                        val hasMessage = items.any { currentMsg ->
                            currentMsg is ChannelScreenItem.RegularMessage && currentMsg.message.id == it.id
                        }

                        if (!hasMessage) return@onEach

                        updateItems(
                            items.map { currentMsg ->
                                if (currentMsg is ChannelScreenItem.RegularMessage && currentMsg.message.id == it.id) {
                                    StoatAPI.messageCache[it.id]?.let { m ->
                                        ChannelScreenItem.RegularMessage(m, currentMsg.mdAst)
                                    } ?: return@map currentMsg
                                } else {
                                    currentMsg
                                }
                            }
                        )
                    }

                    is MessageReactFrame -> {
                        if (it.channel_id != channel?.id) return@onEach

                        val hasMessage = items
                            .filterIsInstance<ChannelScreenItem.RegularMessage>()
                            .any { msg ->
                                msg.message.id == it.id
                            }

                        if (!hasMessage) return@onEach

                        updateItems(
                            items.map { currentMsg ->
                                if (currentMsg is ChannelScreenItem.RegularMessage && currentMsg.message.id == it.id) {
                                    StoatAPI.messageCache[it.id]?.let { m ->
                                        ChannelScreenItem.RegularMessage(m, currentMsg.mdAst)
                                    } ?: return@map currentMsg
                                } else {
                                    currentMsg
                                }
                            }
                        )
                    }

                    is MessageUnreactFrame -> {
                        if (it.channel_id != channel?.id) return@onEach

                        val hasMessage = items
                            .filterIsInstance<ChannelScreenItem.RegularMessage>()
                            .any { msg ->
                                msg.message.id == it.id
                            }

                        if (!hasMessage) return@onEach

                        updateItems(
                            items.map { currentMsg ->
                                if (currentMsg is ChannelScreenItem.RegularMessage && currentMsg.message.id == it.id) {
                                    StoatAPI.messageCache[it.id]?.let { m ->
                                        ChannelScreenItem.RegularMessage(m, currentMsg.mdAst)
                                    } ?: return@map currentMsg
                                } else {
                                    currentMsg
                                }
                            }
                        )
                    }

                    is ChannelStartTypingFrame -> {
                        if (it.id != channel?.id) return@onEach
                        if (typingUsers.contains(it.user)) return@onEach
                        if (it.user == StoatAPI.selfId) return@onEach

                        addUserIfUnknown(it.user)
                        typingUsers.add(it.user)
                    }

                    is ChannelStopTypingFrame -> {
                        if (it.id != channel?.id) return@onEach
                        if (!typingUsers.contains(it.user)) return@onEach

                        typingUsers.remove(it.user)
                    }

                    is ChannelDeleteFrame -> {
                        if (it.id != channel?.id) return@onEach
                        // FIXME This is UI logic from the view model. Too bad!
                        ActionChannel.send(
                            Action.ChatNavigate(
                                ChatRouterDestination.NoCurrentChannel(
                                    channel?.server ?: return@onEach
                                )
                            )
                        )
                    }

                    is RealtimeSocketFrames.Reconnected -> {
                        Log.d("ChannelScreen", "Reconnected to WS.")
                        loadMessages(50, ignoreExisting = true)
                        typingUsers.clear()
                        listenToWsEvents()
                    }
                }
            }.catch {
                Log.e("ChannelScreen", "Failed to receive WS frame", it)
            }.launchIn(this)
        }
    }

    suspend fun listenToUiCallbacks() {
        withContext(Dispatchers.Main) {
            UiCallbacks.uiCallbackFlow.onEach {
                Log.d("ChannelScreen", "Received UI callback: $it")

                when (it) {
                    is UiCallback.ReplyToMessage -> {
                        val message = items.find { m ->
                            m is ChannelScreenItem.RegularMessage && m.message.id == it.messageId
                        } as? ChannelScreenItem.RegularMessage ?: return@onEach

                        val shouldMention = kvStorage.getBoolean("mentionOnReply") ?: false
                        draftReplyTo.add(
                            SendMessageReply(
                                message.message.id ?: return@onEach,
                                shouldMention
                            )
                        )
                    }

                    is UiCallback.EditMessage -> {
                        editingMessage = it.messageId
                        val message = items.find { m ->
                            m is ChannelScreenItem.RegularMessage && m.message.id == it.messageId
                        } as? ChannelScreenItem.RegularMessage ?: return@onEach

                        putDraftContent(message.message.content ?: "", true)
                        this@ChannelScreenViewModel.draftAttachments.clear()
                        draftReplyTo.clear()
                    }

                    is UiCallback.ReplyToMessageWithContent -> {
                        val message = items.find { m ->
                            m is ChannelScreenItem.RegularMessage && m.message.id == it.messageId
                        } as? ChannelScreenItem.RegularMessage ?: return@onEach

                        val shouldMention = kvStorage.getBoolean("mentionOnReply") ?: false
                        draftReplyTo.add(
                            SendMessageReply(
                                message.message.id ?: return@onEach,
                                shouldMention
                            )
                        )
                        putDraftContent(it.content, true)
                    }
                }
            }.catch {
                Log.e("ChannelScreen", "Failed to receive UI callback", it)
            }.launchIn(this)
        }
    }

    private suspend fun updateItems(newItems: List<ChannelScreenItem>) {
        // Spec https://wiki.rvlt.gg/index.php/Text_Channel_(UI)#Message_Grouping_Algorithm
        val innerItems = newItems.toMutableStateList()
        // Let L be the list of messages ordered from newest to oldest
        val allItemsThatAreMessages =
            innerItems.filterIsInstance<ChannelScreenItem.RegularMessage>()
        // Let E be the list of elements to be rendered
        val allItems = innerItems

        val groupedItems = mutableListOf<ChannelScreenItem>()

        // For each message M in L:
        allItems.forEachIndexed { index, m ->
            // [Deviation from spec: if M is not a [Regular/System]Message we just put it in the list...]
            if (m !is ChannelScreenItem.RegularMessage && m !is ChannelScreenItem.SystemMessage) {
                groupedItems.add(m)
                Log.d("ChannelScreenViewModel", "Non-regular message: $m. Skipping grouping.")
                return@forEachIndexed
            }

            val message = when (m) {
                is ChannelScreenItem.RegularMessage -> m.message
                is ChannelScreenItem.SystemMessage -> m.message
                else -> null
            }

            // Let tail be true
            var tail = true
            // Let date be null
            var date: Instant? = null
            // Let next be the next item in list L
            val next = allItems.getOrNull(index + 1)
            // If next is not null:
            if (next != null) {
                // Let adate and bdate be the times the message M and the next message were created respectively
                val adate = message?.id?.let { ULID.asTimestamp(it) }?.let {
                    Instant.fromEpochMilliseconds(it)
                }
                val bdate = (next as? ChannelScreenItem.RegularMessage)?.message?.id?.let {
                    ULID.asTimestamp(it)
                }?.let {
                    Instant.fromEpochMilliseconds(it)
                }

                // [Deviation from spec: if either adate or bdate is null but next is a RegularMessage we skip this message]
                if ((adate == null || bdate == null) && next is ChannelScreenItem.RegularMessage) {
                    return@forEachIndexed
                }

                if (adate != null && bdate != null) {
                    // If adate and bdate are not the same day:
                    val adateLocal =
                        adate.toJavaInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    val bdateLocal =
                        bdate.toJavaInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    if (!adateLocal.isEqual(bdateLocal)) {
                        // Let date be adate
                        date = adate
                    }
                }

                val minuteDifference = adate?.let {
                    bdate?.let { bdate -> it.minus(bdate) }
                }?.inWholeMinutes

                // [Conditions, which we have extracted into variables for readability]
                // Message M and last [spec should say next] have the same author
                val authorsMatch =
                    message?.author == (next as? ChannelScreenItem.RegularMessage)?.message?.author
                // The difference between bdate and adate is equal to or over 7 minutes
                val closeEnough = minuteDifference != null && minuteDifference >= 7
                // The masquerades for message M and last [spec should say next] do not match
                val masqueradesMatch =
                    message?.masquerade == (next as? ChannelScreenItem.RegularMessage)?.message?.masquerade
                // [Possible optimisation: in theory we should not need to check for system messages here as they are a separate type of renderable item]
                // The message M or last [spec should say next] is a system message
                val eitherIsSystem =
                    message?.system != null || (next as? ChannelScreenItem.RegularMessage)?.message?.system != null
                // Message M replies to one or more messages
                val messageHasReplies = message?.replies?.isNotEmpty() == true

                // Let tail be false if one of the following conditions is satisfied:
                if (!authorsMatch || closeEnough || !masqueradesMatch || eitherIsSystem || messageHasReplies) {
                    tail = false
                }
            }
            // Else if next is null:
            else {
                // Let tail be false
                tail = false
            }

            // Push the message
            groupedItems.add(
                when (m) {
                    is ChannelScreenItem.RegularMessage -> ChannelScreenItem.RegularMessage(
                        m.message.copy(tail = tail), m.mdAst
                    )

                    is ChannelScreenItem.ProspectiveMessage -> ChannelScreenItem.ProspectiveMessage(
                        m.message.copy(tail = tail), m.mdAst
                    )

                    is ChannelScreenItem.SystemMessage -> ChannelScreenItem.SystemMessage(
                        m.message.copy(tail = tail)
                    )

                    else -> m
                }
            )
            // [Deviation from spec: we first push the message, then the date, to preserve UI order]
            // If date is not null:
            if (date != null) {
                // Push the date
                groupedItems.add(ChannelScreenItem.DateDivider(date))
            }
        }

        withContext(Dispatchers.Main) {
            items.clear()
            items.addAll(groupedItems.fastDistinctBy {
                when (it) {
                    is ChannelScreenItem.RegularMessage -> it.message.id
                    is ChannelScreenItem.SystemMessage -> it.message.id
                    is ChannelScreenItem.DateDivider -> it.instant.toString()
                    else -> it.toString() // Fallback for other item types
                }
            })
        }
    }

    var showPhysicalKeyboardSpark by mutableStateOf(false)
    fun usesPhysicalKeyboard() {
        viewModelScope.launch {
            if (kvStorage.getBoolean("spark/physicalKeyboard/dismissed") != true) {
                showPhysicalKeyboardSpark = true
            }
        }
    }

    fun dismissPhysicalKeyboardSpark() {
        viewModelScope.launch {
            kvStorage.set("spark/physicalKeyboard/dismissed", true)
            showPhysicalKeyboardSpark = false
        }
    }
}