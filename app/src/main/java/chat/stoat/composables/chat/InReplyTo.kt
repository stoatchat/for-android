package chat.stoat.composables.chat

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.SpecialUsers
import chat.stoat.api.internals.solidColor
import chat.stoat.api.routes.channel.fetchSingleMessage
import chat.stoat.composables.generic.RemoteImage
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.schemas.User
import java.util.concurrent.CancellationException

@Composable
fun InReplyTo(
    channelId: String,
    messageId: String,
    modifier: Modifier = Modifier,
    withMention: Boolean = false,
    onMessageClick: (String) -> Unit = { _ -> }
) {
    val message = StoatAPI.messageCache[messageId]
    val author = StoatAPI.userCache[message?.author ?: ""]

    val username = message?.let { authorName(it) }
        ?: author?.let { User.resolveDefaultName(it) }
        ?: stringResource(id = R.string.unknown)

    val contentColor = LocalContentColor.current
    val usernameColor =
        message?.let { authorColour(it) } ?: Brush.solidColor(contentColor)
    val roleIcon = message?.let { authorRoleIcon(it) }

    val serverId = remember(channelId) { StoatAPI.channelCache[channelId]?.server }

    LaunchedEffect(messageId) {
        if (messageId !in StoatAPI.messageCache) {
            try {
                StoatAPI.messageCache[messageId] = fetchSingleMessage(channelId, messageId)
            } catch (e: CancellationException) {
                // It's fine
            } catch (e: Exception) {
                Log.e("InReplyTo", "Failed to fetch message $messageId", e)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onMessageClick(messageId) }
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(40.dp))

            if (message != null) {
                UserAvatar(
                    username = username,
                    userId = author?.id ?: "",
                    avatar = author?.avatar,
                    rawUrl = authorAvatarUrl(message),
                    size = 16.dp
                )

                Text(
                    text = if (author != null) {
                        if (withMention) {
                            "@$username"
                        } else {
                            username
                        }
                    } else {
                        stringResource(id = R.string.unknown)
                    },
                    style = LocalTextStyle.current.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        brush = usernameColor
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                if (roleIcon != null) {
                    RemoteImage(
                        url = "$STOAT_FILES/icons/${roleIcon.id}",
                        contentScale = ContentScale.Fit,
                        description = null,
                        modifier = Modifier
                            .size(12.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))
                }

                InlineBadges(
                    bot = message.masquerade == null && author?.bot != null,
                    bridge = message.masquerade != null && author?.bot != null,
                    platformModeration = author?.id == SpecialUsers.PLATFORM_MODERATION_USER,
                    teamMember = author?.id in SpecialUsers.TEAM_MEMBER_FLAIRS.keys,
                    webhook = message.webhook != null,
                    colour = contentColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(12.dp),
                    followingIfAny = {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                )

                if (message.content.isNullOrBlank()) {
                    Text(
                        text = stringResource(id = R.string.reply_message_empty_has_attachments),
                        fontSize = 12.sp,
                        color = contentColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = message.content!!,
                        fontSize = 12.sp,
                        color = contentColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = stringResource(id = R.string.reply_message_not_cached),
                    fontStyle = FontStyle.Italic, // inter _still_ doesn't have italics...
                    color = contentColor.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Default, // ...so we use the default font
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
