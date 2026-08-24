package chat.stoat.composables.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.stoat.R
import chat.stoat.activities.StoatTweenFloat
import chat.stoat.activities.StoatTweenSize
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.ULID
import chat.stoat.api.routes.channel.SendMessageReply
import chat.stoat.composables.chat.authorAvatarUrl
import chat.stoat.composables.chat.authorColour
import chat.stoat.composables.chat.authorName
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.core.model.schemas.Message

private class AnimatedReply(
    reply: SendMessageReply,
    initiallyVisible: Boolean,
) {
    var reply by mutableStateOf(reply)
    val visibility = MutableTransitionState(initiallyVisible).apply {
        targetState = true
    }
}

@Composable
fun replyContentText(message: Message): String {
    return if (message.content.isNullOrBlank()) {
        stringResource(id = R.string.reply_message_empty_has_attachments)
    } else {
        message.content!!
    }
}

@Composable
fun ManageableReply(
    reply: SendMessageReply,
    onToggleMention: () -> Unit,
    onRemove: () -> Unit,
    embedded: Boolean = false,
) {
    val replyMessage = StoatAPI.messageCache[reply.id] ?: return onRemove()
    val replyAuthor = StoatAPI.userCache[replyMessage.author] ?: return onRemove()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (embedded) {
                    Modifier
                } else {
                    Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
                }
            )
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_close_24dp),
            contentDescription = stringResource(id = R.string.remove_reply_alt),
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable {
                    onRemove()
                }
                .padding(4.dp)
                .size(16.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))


        UserAvatar(
            username = authorName(message = replyMessage),
            userId = replyAuthor.id ?: ULID.makeSpecial(0),
            avatar = replyAuthor.avatar,
            rawUrl = authorAvatarUrl(message = replyMessage),
            size = 16.dp
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = authorName(message = replyMessage),
            modifier = Modifier
                .padding(4.dp),
            style = LocalTextStyle.current.copy(
                brush = authorColour(message = replyMessage),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        )

        Text(
            text = replyContentText(replyMessage),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(4.dp)
                .weight(1f)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = if (reply.mention) {
                stringResource(id = R.string.reply_mention_on)
            } else {
                stringResource(id = R.string.reply_mention_off)
            },
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable {
                    onToggleMention()
                }
                .padding(4.dp),
            color = if (reply.mention) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            },
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ReplyManager(
    replies: List<SendMessageReply>,
    onToggleMention: (SendMessageReply) -> Unit,
    onRemove: (SendMessageReply) -> Unit,
    embedded: Boolean = false,
) {
    val replySnapshot = replies.toList()
    val animatedReplies = remember {
        mutableStateListOf<AnimatedReply>().apply {
            replySnapshot.forEach { reply ->
                add(AnimatedReply(reply, initiallyVisible = true))
            }
        }
    }

    LaunchedEffect(replySnapshot) {
        val currentReplies = replySnapshot.associateBy { it.id }

        animatedReplies.forEach { animatedReply ->
            val currentReply = currentReplies[animatedReply.reply.id]
            if (currentReply == null) {
                animatedReply.visibility.targetState = false
            } else {
                animatedReply.reply = currentReply
                animatedReply.visibility.targetState = true
            }
        }

        replySnapshot.forEach { reply ->
            if (animatedReplies.none { it.reply.id == reply.id }) {
                animatedReplies.add(AnimatedReply(reply, initiallyVisible = false))
            }
        }
    }

    val currentReplyIds = replySnapshot.mapTo(mutableSetOf()) { it.id }

    Column {
        animatedReplies.forEach { animatedReply ->
            key(animatedReply.reply.id) {
                AnimatedVisibility(
                    visibleState = animatedReply.visibility,
                    enter = expandVertically(
                        animationSpec = StoatTweenSize,
                        expandFrom = Alignment.Bottom,
                    ) + fadeIn(animationSpec = StoatTweenFloat),
                    exit = shrinkVertically(
                        animationSpec = StoatTweenSize,
                        shrinkTowards = Alignment.Bottom,
                    ) + fadeOut(animationSpec = StoatTweenFloat),
                ) {
                    val reply = animatedReply.reply
                    ManageableReply(
                        reply = reply,
                        onToggleMention = { onToggleMention(reply) },
                        onRemove = { onRemove(reply) },
                        embedded = embedded,
                    )
                }

                LaunchedEffect(
                    animatedReply.visibility.isIdle,
                    animatedReply.visibility.currentState,
                    currentReplyIds,
                ) {
                    if (
                        animatedReply.visibility.isIdle &&
                        !animatedReply.visibility.currentState &&
                        animatedReply.reply.id !in currentReplyIds
                    ) {
                        animatedReplies.remove(animatedReply)
                    }
                }
            }
        }
    }
}
