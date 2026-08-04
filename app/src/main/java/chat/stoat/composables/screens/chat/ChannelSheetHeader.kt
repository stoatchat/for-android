package chat.stoat.composables.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.stoat.R
import chat.stoat.api.internals.ChannelUtils
import chat.stoat.composables.generic.RemoteImage
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.composables.markdown.prose.ChatMarkdown
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.schemas.Channel
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.core.model.schemas.User

@Composable
fun ChannelSheetHeader(
    channel: Channel,
    dmPartner: User? = null
) {
    val channelType = channel.channelType ?: ChannelType.TextChannel
    val channelIcon = channel.icon
    val channelDescription = channel.description
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(if (channelType == ChannelType.DirectMessage) CircleShape else MaterialTheme.shapes.medium)
                .background(
                    MaterialTheme.colorScheme.primaryContainer
                ),
            contentAlignment = Alignment.Center
        ) {
            if (channelIcon != null) {
                RemoteImage(
                    url = "$STOAT_FILES/icons/${channelIcon.id ?: ""}",
                    description = null, // decorative
                    contentScale = ContentScale.Crop,
                    height = 48,
                    width = 48,
                    allowAnimation = false,
                    modifier = Modifier
                        .size(48.dp)
                )
            } else if (dmPartner != null) {
                UserAvatar(
                    username = User.resolveDefaultName(dmPartner),
                    userId = dmPartner.id ?: "",
                    avatar = dmPartner.avatar,
                    presence = null,
                    size = 48.dp,
                    modifier = Modifier
                        .size(48.dp)
                )
            } else {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimaryContainer) {
                    ChannelIcon(channel = channel)
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = channel.name
                    ?: ChannelUtils.resolveName(channel)
                    ?: stringResource(R.string.unknown),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!channelDescription.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                ChatMarkdown(channelDescription, serverId = channel.server)
            }
        }
    }
}
