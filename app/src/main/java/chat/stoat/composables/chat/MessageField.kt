package chat.stoat.composables.chat

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.content.ReceiveContentListener
import androidx.compose.foundation.content.consume
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import chat.stoat.R
import chat.stoat.activities.StoatTweenDp
import chat.stoat.api.internals.BrushCompat
import chat.stoat.api.settings.Experiments
import chat.stoat.composables.generic.RemoteImage
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.composables.screens.chat.ChannelIcon
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.core.model.schemas.Member
import chat.stoat.internals.Autocomplete
import chat.stoat.media.AndroidVoiceRecorder
import chat.stoat.media.Recording
import chat.stoat.media.VoiceRecorder
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

fun Pair<Int, Int>.asTextRange(): TextRange {
    return TextRange(this.first, this.second)
}

private fun CharSequence.isEmptyOrOnlyNewlines(): Boolean {
    return this.lines().all { it.isEmpty() || it.all { c -> c == '\n' } }
}

private fun TextFieldState.lastWord(): String? {
    return this.text.substring(0, this.selection.min)
        .split(" ").lastOrNull()
}

private fun CharSequence.lastWordStartsAt(): Int {
    return this.lastIndexOf(" ")
}

@Composable
private fun VoiceRecordingStatus(
    elapsedMillis: Long,
    isFinishing: Boolean,
    cancelProgress: Float,
    isCancelArmed: Boolean,
    modifier: Modifier = Modifier
) {
    val pulseTransition = rememberInfiniteTransition(label = "VoiceRecordingPulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "VoiceRecordingPulseAlpha"
    )
    val totalSeconds = elapsedMillis / 1_000
    val duration = "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(pulseAlpha)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = duration,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.weight(1f))
        val instruction = when {
            isFinishing -> R.string.voice_message_finishing
            isCancelArmed -> R.string.voice_message_release_to_cancel
            else -> R.string.voice_message_slide_to_cancel
        }
        Box(
            contentAlignment = Alignment.CenterEnd,
            modifier = Modifier.graphicsLayer {
                translationX = -24.dp.toPx() * cancelProgress
                alpha = 1f - (cancelProgress * 0.25f)
            }
        ) {
            listOf(
                R.string.voice_message_slide_to_cancel,
                R.string.voice_message_release_to_cancel,
                R.string.voice_message_finishing,
            ).forEach { label ->
                val labelAlpha by animateFloatAsState(
                    targetValue = if (label == instruction) 1f else 0f,
                    animationSpec = tween(if (label == instruction) 150 else 100),
                    label = "VoiceRecordingInstructionAlpha"
                )
                Text(
                    text = stringResource(label),
                    color = if (label == R.string.voice_message_release_to_cancel) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    modifier = Modifier.graphicsLayer { alpha = labelAlpha }
                )
            }
        }
    }
}

sealed class AutocompleteSuggestion {
    data class User(
        val user: chat.stoat.core.model.schemas.User,
        val member: Member?,
        val query: String
    ) : AutocompleteSuggestion()

    data class Channel(
        val channel: chat.stoat.core.model.schemas.Channel,
        val query: String
    ) : AutocompleteSuggestion()

    data class Emoji(
        val shortcode: String,
        val unicode: String?,
        val custom: chat.stoat.core.model.schemas.Emoji?,
        val query: String
    ) : AutocompleteSuggestion()

    data class Role(
        val role: chat.stoat.core.model.schemas.Role,
        val id: String,
        val query: String
    ) : AutocompleteSuggestion()

    data class MassMention(
        val content: String
    ) : AutocompleteSuggestion()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageField(
    initialValue: String,
    onValueChange: (String) -> Unit,
    onAddAttachment: () -> Unit,
    onCommitAttachment: (Uri) -> Unit,
    onPickEmoji: () -> Unit,
    onSendMessage: () -> Unit,
    channelType: ChannelType,
    channelName: String,
    modifier: Modifier = Modifier,
    containerModifier: Modifier = Modifier,
    forceSendButton: Boolean = false,
    sendEnabled: Boolean = true,
    canAttach: Boolean = true,
    disabled: Boolean = false,
    failedValidation: Boolean = false,
    serverId: String? = null,
    channelId: String? = null,
    valueIsBlank: Boolean = false,
    editMode: Boolean = false,
    initialValueDirtyMarker: Any = Unit,
    cancelEdit: () -> Unit = {},
    onFocusChange: (Boolean) -> Unit = {},
    onSendVoiceMessage: ((Recording) -> Boolean)? = null,
    contentBeforeInput: @Composable ColumnScope.() -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val voiceRecorder = remember(context) {
        AndroidVoiceRecorder(context.applicationContext)
    }
    var isRecordingVoiceMessage by remember { mutableStateOf(false) }
    var isFinishingVoiceMessage by remember { mutableStateOf(false) }
    var pendingVoiceMessage by remember { mutableStateOf<Recording?>(null) }
    var voiceRecordingStartedAtMillis by remember { mutableLongStateOf(0L) }
    var voiceRecordingElapsedMillis by remember { mutableLongStateOf(0L) }
    var voiceRecordingCancelProgress by remember { mutableFloatStateOf(0f) }
    var isVoiceRecordingCancelArmed by remember { mutableStateOf(false) }
    val currentSendEnabled by rememberUpdatedState(sendEnabled)
    val currentDisabled by rememberUpdatedState(disabled)

    val startVoiceRecording: () -> Boolean = {
        try {
            voiceRecorder.start()
            voiceRecordingStartedAtMillis = SystemClock.elapsedRealtime()
            voiceRecordingElapsedMillis = 0L
            voiceRecordingCancelProgress = 0f
            isVoiceRecordingCancelArmed = false
            isRecordingVoiceMessage = true
            true
        } catch (_: Exception) {
            Toast.makeText(
                context,
                R.string.voice_message_recording_failed,
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }
    val requestMicrophonePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                context,
                R.string.voice_message_microphone_permission_denied,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    suspend fun finishVoiceRecording(cancelled: Boolean, heldDurationMillis: Long) {
        val recording = try {
            withContext(NonCancellable) {
                if (
                    !cancelled &&
                    heldDurationMillis >= VoiceRecorder.MIN_DURATION_MILLIS
                ) {
                    delay(VoiceRecorder.POST_ROLL_DURATION_MILLIS.milliseconds)
                }
                voiceRecorder.finish()
            }
        } finally {
            isRecordingVoiceMessage = false
            isFinishingVoiceMessage = false
            voiceRecordingCancelProgress = 0f
            isVoiceRecordingCancelArmed = false
        }

        if (cancelled) {
            return
        }

        if (
            heldDurationMillis >= VoiceRecorder.MIN_DURATION_MILLIS &&
            recording == null
        ) {
            Toast.makeText(
                context,
                R.string.voice_message_recording_failed,
                Toast.LENGTH_SHORT
            ).show()
        } else if (
            recording != null &&
            recording.durationMillis >= VoiceRecorder.MIN_DURATION_MILLIS
        ) {
            if (recording.isSilent) {
                Toast.makeText(
                    context,
                    R.string.voice_message_silent,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                pendingVoiceMessage = recording
            }
        }
    }

    val placeholderResource = when (channelType) {
        ChannelType.DirectMessage -> R.string.message_field_placeholder_dm
        ChannelType.Group -> R.string.message_field_placeholder_group
        ChannelType.TextChannel -> R.string.message_field_placeholder_text
        ChannelType.VoiceChannel -> R.string.message_field_placeholder_voice
        ChannelType.SavedMessages -> R.string.message_field_placeholder_notes
    }

    val sendButtonVisible =
        (!valueIsBlank || forceSendButton) && !disabled && !failedValidation
    val sendButtonTransition = updateTransition(
        targetState = sendButtonVisible,
        label = "SendButton",
    )
    val sendButtonSlotWidth by sendButtonTransition.animateDp(
        transitionSpec = { StoatTweenDp },
        label = "SendButtonSlotWidth",
    ) { visible ->
        if (visible) 48.dp else 0.dp
    }
    val sendButtonOffsetX by sendButtonTransition.animateDp(
        transitionSpec = { StoatTweenDp },
        label = "SendButtonOffsetX",
    ) { visible ->
        if (visible) 0.dp else 48.dp
    }
    val voiceMicContainerColor by animateColorAsState(
        targetValue = if (isRecordingVoiceMessage) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            Color.Transparent
        },
        animationSpec = tween(140),
        label = "VoiceMicContainerColor"
    )
    val voiceMicContentColor by animateColorAsState(
        targetValue = if (isRecordingVoiceMessage) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        },
        animationSpec = tween(140),
        label = "VoiceMicContentColor"
    )
    val voiceAuxiliaryContentColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.onSurface.copy(
            alpha = if (isRecordingVoiceMessage) 0.24f else 0.5f
        ),
        animationSpec = tween(140),
        label = "VoiceAuxiliaryContentColor"
    )
    val voiceMicScale by animateFloatAsState(
        targetValue = if (isRecordingVoiceMessage) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "VoiceMicScale"
    )

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    var selection by remember { mutableStateOf(0 to 0) }
    val autocompleteSuggestions = remember { mutableStateListOf<AutocompleteSuggestion>() }
    val autocompleteSuggestionState = rememberLazyListState()

    val receiveContentListener = remember {
        ReceiveContentListener { transferableContent ->
            transferableContent.consume { item ->
                val uri = item.uri
                if (uri != null) {
                    onCommitAttachment(uri)
                }
                uri != null
            }
        }
    }

    val textFieldState = rememberTextFieldState(
        initialText = initialValue,
        initialSelection = selection.asTextRange()
    )

    LaunchedEffect(initialValue, initialValueDirtyMarker) {
        textFieldState.setTextAndPlaceCursorAtEnd(initialValue)
    }

    LaunchedEffect(textFieldState.text) {
        onValueChange(textFieldState.text.toString())

        scope.launch {
            autocompleteSuggestionState.animateScrollToItem(0)
        }
        autocompleteSuggestions.clear()

        if (textFieldState.text.isNotBlank() &&
            (textFieldState.selection.min == textFieldState.selection.max)
        ) {
            val lastWord = textFieldState.lastWord()
            if (lastWord != null) {
                when {
                    lastWord.startsWith(':') && !lastWord.endsWith(':') -> {
                        autocompleteSuggestions.addAll(
                            Autocomplete.emoji(lastWord.substring(1))
                        )
                    }

                    lastWord.startsWith('@') -> {
                        if (channelId != null && serverId != null) {
                            autocompleteSuggestions.addAll(
                                Autocomplete.userOrRole(
                                    channelId,
                                    serverId,
                                    lastWord.substring(1)
                                )
                            )
                        }
                    }

                    lastWord.startsWith('#') -> {
                        if (serverId != null) {
                            autocompleteSuggestions.addAll(
                                Autocomplete.channel(
                                    serverId,
                                    lastWord.substring(1)
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(editMode) {
        if (editMode) {
            focusRequester.requestFocus()
        } else {
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(isRecordingVoiceMessage) {
        while (isRecordingVoiceMessage) {
            voiceRecordingElapsedMillis =
                SystemClock.elapsedRealtime() - voiceRecordingStartedAtMillis
            delay(100.milliseconds)
        }
    }

    pendingVoiceMessage?.let { recording ->
        AlertDialog(
            onDismissRequest = {
                pendingVoiceMessage = null
            },
            title = {
                Text(stringResource(R.string.voice_message_temp_review_title))
            },
            text = {
                val totalSeconds = recording.durationMillis / 1_000
                val duration = "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
                Text(stringResource(R.string.voice_message_temp_review_description, duration))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (onSendVoiceMessage?.invoke(recording) != false) {
                            pendingVoiceMessage = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.send_alt))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingVoiceMessage = null
                    }
                ) {
                    Text(stringResource(R.string.voice_message_discard))
                }
            }
        )
    }

    val messageFieldShape = RoundedCornerShape(28.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .shadow(4.dp, messageFieldShape, clip = false)
            .clip(messageFieldShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(containerModifier),
    ) {
        contentBeforeInput()

        AnimatedVisibility(
            visible = autocompleteSuggestions.isNotEmpty(),
            enter = expandIn(initialSize = { full ->
                IntSize(
                    full.width,
                    0
                )
            }),
            exit = shrinkOut(targetSize = { full ->
                IntSize(
                    full.width,
                    0
                )
            })
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                state = autocompleteSuggestionState
            ) {
                items(autocompleteSuggestions.size, key = {
                    when (val item = autocompleteSuggestions[it]) {
                        is AutocompleteSuggestion.User -> item.user.id!!
                        is AutocompleteSuggestion.Channel -> item.channel.id!!
                        is AutocompleteSuggestion.Emoji -> item.shortcode
                        is AutocompleteSuggestion.Role -> item.id
                        is AutocompleteSuggestion.MassMention -> item.content
                    }
                }) {
                    when (val item = autocompleteSuggestions[it]) {
                        is AutocompleteSuggestion.User -> {
                            SuggestionChip(
                                onClick = {
                                    textFieldState.edit {
                                        val lastWordStartsAt =
                                            textFieldState.text
                                                .substring(0, textFieldState.selection.max)
                                                .lastWordStartsAt()
                                        replace(
                                            if (lastWordStartsAt == -1) 0 else (lastWordStartsAt + 1),
                                            textFieldState.selection.max,
                                            "@${item.user.username}#${item.user.discriminator} "
                                        )
                                    }
                                },
                                label = { Text("@${item.user.username}#${item.user.discriminator}") },
                                icon = {
                                    UserAvatar(
                                        username = item.user.username
                                            ?: stringResource(R.string.unknown),
                                        userId = item.user.id ?: "",
                                        avatar = item.user.avatar,
                                        rawUrl = item.member?.avatar?.id?.let {
                                            "$STOAT_FILES/avatars/$it"
                                        },
                                        size = SuggestionChipDefaults.IconSize,
                                    )
                                },
                                modifier = Modifier
                                    .animateItem()
                            )
                        }

                        is AutocompleteSuggestion.Role -> {
                            SuggestionChip(
                                onClick = {
                                    textFieldState.edit {
                                        val lastWordStartsAt =
                                            textFieldState.text
                                                .substring(0, textFieldState.selection.max)
                                                .lastWordStartsAt()
                                        replace(
                                            if (lastWordStartsAt == -1) 0 else (lastWordStartsAt + 1),
                                            textFieldState.selection.max,
                                            "<%${item.id}> "
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = "@${item.role.name}",
                                        style = item.role.colour?.let {
                                            LocalTextStyle.current.copy(
                                                brush = BrushCompat.parseColour(it)
                                            )
                                        } ?: LocalTextStyle.current
                                    )
                                },
                                icon = {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                item.role.colour?.let { BrushCompat.parseColour(it) }
                                                    ?: SolidColor(MaterialTheme.colorScheme.primaryContainer)
                                            )
                                            .size(SuggestionChipDefaults.IconSize)
                                            .align(Alignment.CenterHorizontally),
                                    )
                                },
                                modifier = Modifier
                                    .animateItem()
                            )
                        }

                        is AutocompleteSuggestion.Channel -> {
                            SuggestionChip(
                                onClick = {
                                    textFieldState.edit {
                                        val lastWordStartsAt =
                                            textFieldState.text
                                                .substring(0, textFieldState.selection.max)
                                                .lastWordStartsAt()

                                        val replacement =
                                            if (item.channel.name?.contains(
                                                    " ",
                                                    ignoreCase = true
                                                ) == true
                                            ) {
                                                "<#${item.channel.id}> "
                                            } else {
                                                "#${item.channel.name} "
                                            }

                                        replace(
                                            if (lastWordStartsAt == -1) 0 else (lastWordStartsAt + 1),
                                            textFieldState.selection.max,
                                            replacement
                                        )
                                    }
                                },
                                label = { Text("#${item.channel.name}") },
                                icon = {
                                    if (item.channel.channelType != null) {
                                        ChannelIcon(
                                            channel = item.channel,
                                            modifier = Modifier.size(SuggestionChipDefaults.IconSize)
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .animateItem()
                            )
                        }

                        is AutocompleteSuggestion.Emoji -> {
                            SuggestionChip(
                                onClick = {
                                    textFieldState.edit {
                                        val lastWordStartsAt =
                                            textFieldState.text
                                                .substring(0, textFieldState.selection.max)
                                                .lastWordStartsAt()
                                        replace(
                                            if (lastWordStartsAt == -1) 0 else (lastWordStartsAt + 1),
                                            textFieldState.selection.max,
                                            item.shortcode + " "
                                        )
                                    }
                                },
                                label = {
                                    if (item.custom != null) {
                                        Text(":${item.custom.name}:")
                                    } else {
                                        Text(item.shortcode)
                                    }
                                },
                                icon = {
                                    if (item.unicode != null) {
                                        Text(
                                            item.unicode,
                                            modifier = Modifier
                                                .size(SuggestionChipDefaults.IconSize)
                                                .align(Alignment.CenterHorizontally),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    } else {
                                        RemoteImage(
                                            url = "$STOAT_FILES/emojis/${item.custom?.id}",
                                            description = null,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .size(SuggestionChipDefaults.IconSize)
                                                .align(Alignment.CenterHorizontally)
                                        )
                                    }
                                },
                                modifier = Modifier.animateItem()
                            )
                        }

                        is AutocompleteSuggestion.MassMention -> {
                            SuggestionChip(
                                onClick = {
                                    textFieldState.edit {
                                        val lastWordStartsAt =
                                            textFieldState.text
                                                .substring(0, textFieldState.selection.max)
                                                .lastWordStartsAt()
                                        replace(
                                            if (lastWordStartsAt == -1) 0 else (lastWordStartsAt + 1),
                                            textFieldState.selection.max,
                                            "@${item.content} "
                                        )
                                    }
                                },
                                label = { Text("@${item.content}") },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_campaign_24dp),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(SuggestionChipDefaults.IconSize)
                                            .align(Alignment.CenterHorizontally)
                                    )
                                },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(8.dp))

                    // Note: There is an assumption that editing a message implies canAttach = false and editMode = true
                    AnimatedVisibility(canAttach) {
                        Icon(
                            Icons.Default.Add,
                            tint = voiceAuxiliaryContentColor,
                            contentDescription = stringResource(id = R.string.add_attachment_alt),
                            modifier = Modifier
                                .clip(CircleShape)
                                .size(32.dp)
                                .clickable(enabled = !isRecordingVoiceMessage) {
                                    if (!editMode) {
                                        // hide keyboard because it's annoying
                                        focusManager.clearFocus()
                                        onAddAttachment()
                                    }
                                }
                                .padding(4.dp)
                                .testTag("add_attachment")
                        )
                    }

                    BasicTextField(
                        state = textFieldState,
                        textStyle = LocalTextStyle.current.copy(
                            color = if (failedValidation) {
                                MaterialTheme.colorScheme.error
                            } else LocalContentColor.current
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            capitalization = KeyboardCapitalization.Sentences,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.None,
                            showKeyboardOnFocus = false
                        ),
                        readOnly = isRecordingVoiceMessage,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(max = 128.dp)
                            .verticalScroll(rememberScrollState())
                            .onFocusChanged {
                                onFocusChange(it.isFocused)
                            }
                            .focusRequester(focusRequester)
                            .contentReceiver(receiveContentListener)
                            .onKeyEvent {
                                if (it.type == KeyEventType.KeyUp) {
                                    when (it.key) {
                                        Key.Enter if !it.isShiftPressed &&
                                                !it.isAltPressed &&
                                                it.isCtrlPressed &&
                                                !it.isMetaPressed -> {
                                            if (sendEnabled) {
                                                onSendMessage()
                                            }
                                            return@onKeyEvent true
                                        }

                                        Key.Escape if !it.isShiftPressed &&
                                                !it.isAltPressed &&
                                                !it.isCtrlPressed &&
                                                !it.isMetaPressed -> {
                                            cancelEdit()
                                            return@onKeyEvent true
                                        }
                                    }
                                }

                                return@onKeyEvent false
                            },
                        decorator = { innerTextField ->
                            Box(Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
                                innerTextField()
                                AnimatedContent(
                                    targetState = isRecordingVoiceMessage,
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart,
                                    transitionSpec = {
                                        (fadeIn(tween(160)) + scaleIn(
                                            initialScale = 0.97f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )).togetherWith(
                                            fadeOut(tween(100)) + scaleOut(
                                                targetScale = 0.97f,
                                                animationSpec = tween(100)
                                            )
                                        )
                                    },
                                    label = "VoiceRecordingContent"
                                ) { recording ->
                                    if (recording) {
                                        VoiceRecordingStatus(
                                            elapsedMillis = voiceRecordingElapsedMillis,
                                            isFinishing = isFinishingVoiceMessage,
                                            cancelProgress = voiceRecordingCancelProgress,
                                            isCancelArmed = isVoiceRecordingCancelArmed,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else if (textFieldState.text.isEmptyOrOnlyNewlines()) {
                                        Text(
                                            stringResource(placeholderResource, channelName),
                                            style = LocalTextStyle.current.copy(
                                                color = LocalContentColor.current.copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    )

                    Icon(
                        painter = painterResource(R.drawable.ic_mood_24dp),
                        tint = voiceAuxiliaryContentColor,
                        contentDescription = stringResource(id = R.string.pick_emoji_alt),
                        modifier = Modifier
                            .clip(CircleShape)
                            .size(32.dp)
                            .clickable(enabled = !isRecordingVoiceMessage) {
                                focusManager.clearFocus()
                                onPickEmoji()
                            }
                            .padding(4.dp)
                            .testTag("pick_emoji")
                    )

                    AnimatedVisibility(
                        visible = Experiments.voiceMessages.isEnabled &&
                                onSendVoiceMessage != null &&
                                canAttach &&
                                !editMode &&
                                valueIsBlank &&
                                !forceSendButton
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .scale(voiceMicScale)
                                .clip(CircleShape)
                                .background(voiceMicContainerColor)
                                .pointerInput(Unit) {
                                    coroutineScope {
                                        val gestureScope = this
                                        val cancelDistancePx = 96.dp.toPx()

                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)

                                            if (
                                                !currentSendEnabled ||
                                                currentDisabled ||
                                                isFinishingVoiceMessage
                                            ) {
                                                return@awaitEachGesture
                                            }

                                            if (
                                                ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.RECORD_AUDIO
                                                ) != PackageManager.PERMISSION_GRANTED
                                            ) {
                                                requestMicrophonePermission.launch(
                                                    Manifest.permission.RECORD_AUDIO
                                                )
                                                return@awaitEachGesture
                                            }

                                            if (!startVoiceRecording()) {
                                                return@awaitEachGesture
                                            }
                                            down.consume()
                                            focusManager.clearFocus()
                                            val recordingStartedAtMillis =
                                                voiceRecordingStartedAtMillis

                                            var endedNormally = false
                                            var cancelArmed = false
                                            try {
                                                while (true) {
                                                    val event = awaitPointerEvent()
                                                    val change = event.changes.firstOrNull {
                                                        it.id == down.id
                                                    } ?: break
                                                    val cancelProgress = (
                                                            (down.position.x - change.position.x) /
                                                                    cancelDistancePx
                                                            ).coerceIn(0f, 1f)
                                                    cancelArmed = cancelProgress >= 1f
                                                    voiceRecordingCancelProgress = cancelProgress
                                                    isVoiceRecordingCancelArmed = cancelArmed
                                                    change.consume()

                                                    if (!change.pressed) {
                                                        endedNormally = true
                                                        break
                                                    }
                                                }
                                            } finally {
                                                val heldDurationMillis =
                                                    SystemClock.elapsedRealtime() -
                                                            recordingStartedAtMillis
                                                isFinishingVoiceMessage = true
                                                gestureScope.launch(NonCancellable) {
                                                    finishVoiceRecording(
                                                        cancelled = !endedNormally || cancelArmed,
                                                        heldDurationMillis = heldDurationMillis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                .testTag("voice_message")
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_mic_24dp),
                                tint = voiceMicContentColor,
                                contentDescription = stringResource(R.string.voice_message_start_recording),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Spacer(modifier = Modifier.width(sendButtonSlotWidth))
                }

                if (
                    sendButtonTransition.currentState ||
                    sendButtonTransition.targetState
                ) {
                    Icon(
                        painter = when {
                            editMode -> painterResource(R.drawable.ic_edit_24dp)
                            else -> painterResource(R.drawable.ic_send_24dp)
                        },
                        tint = if (sendEnabled) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
                        },
                        contentDescription = stringResource(id = R.string.send_alt),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset(x = sendButtonOffsetX)
                            .padding(end = 8.dp)
                            .size(width = 40.dp, height = 32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable(
                                enabled = sendEnabled && sendButtonTransition.targetState
                            ) {
                                onSendMessage()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("send_message")
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun NativeMessageFieldPreview() {
    MessageField(
        initialValue = "Hello world!",
        onValueChange = {},
        onAddAttachment = {},
        onCommitAttachment = {},
        onPickEmoji = {},
        onSendMessage = {},
        channelType = ChannelType.DirectMessage,
        channelName = "Test",
        modifier = Modifier,
        forceSendButton = false,
        canAttach = true,
        disabled = false,
        editMode = false,
        cancelEdit = {},
        onFocusChange = {},
    )
}
