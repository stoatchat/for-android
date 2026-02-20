package chat.stoat.composables.screens.chat

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
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.ULID
import chat.stoat.api.routes.channel.SendMessageReply
import chat.stoat.composables.chat.authorAvatarUrl
import chat.stoat.composables.chat.authorColour
import chat.stoat.composables.chat.authorName
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.core.model.schemas.Message

@Composable
fun replyContentText(message: Message): String {
    return if (message.content.isNullOrBlank()) {
        stringResource(id = R.string.reply_message_empty_has_attachments)
    } else {
        message.content!!
    }
}

@Composable
fun ManageableReply(reply: SendMessageReply, onToggleMention: () -> Unit, onRemove: () -> Unit) {
    val replyMessage = StoatAPI.messageCache[reply.id] ?: return onRemove()
    val replyAuthor = StoatAPI.userCache[replyMessage.author] ?: return onRemove()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
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
    onRemove: (SendMessageReply) -> Unit
) {
    Column {
        replies.forEach { reply ->
            ManageableReply(
                reply = reply,
                onToggleMention = { onToggleMention(reply) },
                onRemove = { onRemove(reply) }
            )
        }
    }
}
