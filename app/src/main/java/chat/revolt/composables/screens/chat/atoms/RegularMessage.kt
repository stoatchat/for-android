package chat.revolt.composables.screens.chat.atoms

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.schemas.Channel
import chat.revolt.api.schemas.Message
import chat.revolt.api.settings.LoadedSettings
import chat.revolt.api.settings.MessageReplyStyle
import chat.revolt.callbacks.Action
import chat.revolt.callbacks.ActionChannel
import chat.revolt.composables.chat.Message
import chat.revolt.internals.extensions.supportSwipeReply
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

const val SWIPE_TO_REPLY_THRESHOLD = -450f

// Display a regular message in the LazyColumn of the chat screen.
@SuppressLint("UnusedBoxWithConstraintsScope") // we do use it, but the IDE is stupid
@Composable
fun RegularMessage(
    message: Message,
    channel: Channel?,
    drawerIsOpen: Boolean,
    setDrawerGestureEnabled: (Boolean) -> Unit,
    setDisableScroll: (Boolean) -> Unit,
    showMessageBottomSheet: (String) -> Unit,
    showReactBottomSheet: () -> Unit,
    putTextAtCursorPosition: (String) -> Unit,
    replyToMessage: suspend (String) -> Unit,
    jumpToMessage: (String) -> Unit = {},
    highlightedMessageId: String? = null,
    scope: CoroutineScope = rememberCoroutineScope()
) {
    val haptic = LocalHapticFeedback.current

    var offsetX by remember { mutableFloatStateOf(0f) }
    val animOffsetX by animateFloatAsState(
        when {
            drawerIsOpen -> 0f
            offsetX > -20f -> 0f
            else -> offsetX
        },
        label = "X offset of message for Swipe to Reply"
    )
    var markGestureInvalid by remember { mutableStateOf(false) }
    var hapticFeedbackPerformed by remember { mutableStateOf(false) }
    var messageHeight by remember { mutableIntStateOf(0) }

    val canReleaseToSend = remember(offsetX) { offsetX <= SWIPE_TO_REPLY_THRESHOLD }
    val indicatorBackground by animateColorAsState(
        when {
            canReleaseToSend -> MaterialTheme.colorScheme.inversePrimary
            else -> MaterialTheme.colorScheme.primaryContainer
        },
        label = "Swipe to Reply indicator background"
    )
    val indicatorForeground by animateColorAsState(
        when {
            canReleaseToSend -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onPrimaryContainer
        },
        label = "Swipe to Reply indicator foreground"
    )

    var onFingerMoveHandler: (List<PointerInputChange>) -> Unit =
        { changeList: List<PointerInputChange> ->
            changeList.firstOrNull()
                ?.let {
                    val deltaX = it.position.x - it.previousPosition.x
                    val deltaY = it.position.y - it.previousPosition.y

                    val couldBeTopDownScroll =
                        deltaX > -30f && abs(deltaY) > 30f && offsetX >= -100f

                    if (couldBeTopDownScroll) {
                        offsetX = 0f
                        markGestureInvalid = true
                        return@let
                    }

                    val goesTowardsLeft = it.position.x < it.previousPosition.x
                    if (goesTowardsLeft || offsetX <= -20f) {
                        if (markGestureInvalid) return@let

                        offsetX += deltaX
                        setDrawerGestureEnabled(false)
                    }

                    if (goesTowardsLeft && offsetX <= -30f) {
                        setDisableScroll(true)
                    }

                    if (goesTowardsLeft && offsetX <= SWIPE_TO_REPLY_THRESHOLD && !hapticFeedbackPerformed) {
                        hapticFeedbackPerformed = true
                        haptic.performHapticFeedback(
                            HapticFeedbackType.GestureThresholdActivate
                        )
                    } else if (hapticFeedbackPerformed && offsetX >= -100f) {
                        hapticFeedbackPerformed = false
                    }
                }
        }

    Box {
        Message(
            message = message,
            onMessageContextMenu = {
                message.id?.let { messageId ->
                    showMessageBottomSheet(messageId)
                }
            },
            onAvatarClick = {
                if (message.webhook != null) {
                    scope.launch {
                        ActionChannel.send(Action.OpenWebhookSheet)
                    }
                } else {
                    message.author?.let { author ->
                        scope.launch {
                            ActionChannel.send(Action.OpenUserSheet(author, channel?.server))
                        }
                    }
                }
            },
            onNameClick = {
                val author = message.author?.let { RevoltAPI.userCache[it] } ?: return@Message
                putTextAtCursorPosition("@${author.username}#${author.discriminator}")
            },
            canReply = true,
            onReply = {
                message.id?.let { messageId ->
                    scope.launch {
                        replyToMessage(messageId)
                    }
                }
            },
            onAddReaction = {
                message.id?.let { messageId ->
                    showReactBottomSheet()
                }
            },
            fromWebhook = message.webhook != null,
            webhookName = message.webhook?.name,
            jumpToMessage = jumpToMessage,
            highlightedMessageId = highlightedMessageId,
            modifier = Modifier
                .offset(
                    x = with(LocalDensity.current) { animOffsetX.toDp() }
                )
                .then(
                    if (LoadedSettings.messageReplyStyle == MessageReplyStyle.SwipeFromEnd)
                        Modifier.supportSwipeReply(
                            onDown = {},
                            onMove = onFingerMoveHandler,
                            onUp = {
                                if (offsetX <= SWIPE_TO_REPLY_THRESHOLD) {
                                    scope.launch {
                                        message.id?.let {
                                            replyToMessage(it)
                                        }
                                    }
                                }

                                setDrawerGestureEnabled(true)
                                markGestureInvalid = false
                                setDisableScroll(false)
                                hapticFeedbackPerformed = false
                                offsetX = 0f
                            }
                        )
                    else Modifier
                )
                .onSizeChanged {
                    // FIXME:
                    // This whole onSizeChanged pattern is, technically a workaround. LazyColumn
                    // doesn't support the usual idiomatic ways to make an item as tall as the
                    // tallest item in the row (intrinsic sizing; fill parent etc.)
                    // This workaround may bite us performance-wise!
                    if (messageHeight != it.height) messageHeight = it.height
                }
        )
        with(LocalDensity.current) {
            val msgHeightAsDp = messageHeight.toDp()
            BoxWithConstraints(Modifier.height(msgHeightAsDp)) {
                Row(
                    Modifier
                        .height(msgHeightAsDp)
                        .requiredHeightIn(max = msgHeightAsDp) // must not cause message to be taller
                        .offset(
                            x = with(LocalDensity.current) {
                                maxWidth - abs(
                                    animOffsetX
                                ).toDp()
                            }
                        )
                        .background(indicatorBackground)
                        .fillMaxWidth()
                        .padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        8.dp,
                        Alignment.Start
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.icn_reply_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(
                            min(
                                msgHeightAsDp - 4.dp,
                                24.dp
                            )
                        ),
                        tint = indicatorForeground
                    )
                    AnimatedContent(
                        targetState = canReleaseToSend,
                        transitionSpec = {
                            fadeIn(animationSpec = spring()) togetherWith
                                    fadeOut(animationSpec = spring())
                        },
                        label = "Swipe to Reply indicator label"
                    ) {
                        Text(
                            when (it) {
                                true -> stringResource(
                                    R.string.swipe_to_reply_release
                                )

                                else -> stringResource(
                                    R.string.swipe_to_reply_keep_swiping
                                )
                            },
                            color = indicatorForeground,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}