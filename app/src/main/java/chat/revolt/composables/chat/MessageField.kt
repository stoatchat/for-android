package chat.revolt.composables.chat

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.content.ReceiveContentListener
import androidx.compose.foundation.content.consume
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
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
import chat.revolt.R
import chat.revolt.activities.RevoltTweenFloat
import chat.revolt.activities.RevoltTweenInt
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.internals.BrushCompat
import chat.revolt.api.schemas.ChannelType
import chat.revolt.api.schemas.Member
import chat.revolt.composables.generic.RemoteImage
import chat.revolt.composables.generic.UserAvatar
import chat.revolt.composables.screens.chat.ChannelIcon
import chat.revolt.internals.Autocomplete
import kotlinx.coroutines.launch

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

sealed class AutocompleteSuggestion {
    data class User(
        val user: chat.revolt.api.schemas.User,
        val member: Member?,
        val query: String
    ) : AutocompleteSuggestion()

    data class Channel(
        val channel: chat.revolt.api.schemas.Channel,
        val query: String
    ) : AutocompleteSuggestion()

    data class Emoji(
        val shortcode: String,
        val unicode: String?,
        val custom: chat.revolt.api.schemas.Emoji?,
        val query: String
    ) : AutocompleteSuggestion()

    data class Role(
        val role: chat.revolt.api.schemas.Role,
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
    forceSendButton: Boolean = false,
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
) {
    val placeholderResource = when (channelType) {
        ChannelType.DirectMessage -> R.string.message_field_placeholder_dm
        ChannelType.Group -> R.string.message_field_placeholder_group
        ChannelType.TextChannel -> R.string.message_field_placeholder_text
        ChannelType.VoiceChannel -> R.string.message_field_placeholder_voice
        ChannelType.SavedMessages -> R.string.message_field_placeholder_notes
    }

    val sendButtonVisible =
        (!valueIsBlank || forceSendButton) && !disabled && !failedValidation

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

    var textFieldState = rememberTextFieldState(
        initialText = initialValue,
        initialSelection = selection.asTextRange()
    )

    LaunchedEffect(initialValue, initialValueDirtyMarker) {
        textFieldState.setTextAndPlaceCursorAtEnd(initialValue)
    }

    val scope = rememberCoroutineScope()

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

    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
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
                                            "$REVOLT_FILES/avatars/$it"
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
                                    item.channel.channelType?.let { type ->
                                        ChannelIcon(
                                            channelType = type,
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
                                            url = "$REVOLT_FILES/emojis/${item.custom?.id}",
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
                                        painter = painterResource(R.drawable.icn_campaign_24dp),
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
        Row(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surfaceContainer),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(8.dp))

            // Note: There is an assumption that editing a message implies canAttach = false and editMode = true
            AnimatedVisibility(canAttach) {
                Icon(
                    Icons.Default.Add,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    contentDescription = stringResource(id = R.string.add_attachment_alt),
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(32.dp)
                        .clickable {
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
                            when {
                                it.key == Key.Enter &&
                                        !it.isShiftPressed &&
                                        !it.isAltPressed &&
                                        it.isCtrlPressed &&
                                        !it.isMetaPressed -> {
                                    onSendMessage()
                                    return@onKeyEvent true
                                }

                                it.key == Key.Escape &&
                                        !it.isShiftPressed &&
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
                        if (textFieldState.text.isEmptyOrOnlyNewlines()) {
                            Text(
                                stringResource(placeholderResource, channelName),
                                style = LocalTextStyle.current.copy(
                                    color = LocalContentColor.current.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.align(Alignment.CenterStart)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Icon(
                painter = painterResource(R.drawable.icn_mood_24dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                contentDescription = stringResource(id = R.string.pick_emoji_alt),
                modifier = Modifier
                    .clip(CircleShape)
                    .size(32.dp)
                    .clickable {
                        focusManager.clearFocus()
                        onPickEmoji()
                    }
                    .padding(4.dp)
                    .testTag("pick_emoji")
            )

            Spacer(modifier = Modifier.width(8.dp))

            AnimatedVisibility(
                sendButtonVisible,
                enter = expandIn(initialSize = { full ->
                    IntSize(
                        0,
                        full.height
                    )
                }) + slideInHorizontally(
                    animationSpec = RevoltTweenInt,
                    initialOffsetX = { -it }
                ) + fadeIn(animationSpec = RevoltTweenFloat),
                exit = shrinkOut(targetSize = { full ->
                    IntSize(
                        0,
                        full.height
                    )
                }) + slideOutHorizontally(
                    animationSpec = RevoltTweenInt,
                    targetOffsetX = { it }
                ) + fadeOut(animationSpec = RevoltTweenFloat)
            ) {
                Icon(
                    painter = when {
                        editMode -> painterResource(R.drawable.icn_edit_24dp)
                        else -> painterResource(R.drawable.icn_send_24dp)
                    },
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = stringResource(id = R.string.send_alt),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(CircleShape)
                        .clickable { onSendMessage() }
                        .size(32.dp)
                        .padding(4.dp)
                        .testTag("send_message")
                )
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
