package chat.revolt.composables.screens.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.api.internals.ChannelUtils
import chat.revolt.api.schemas.Channel
import chat.revolt.api.schemas.ChannelType

@Composable
fun ChannelHeader(
    channel: Channel,
    onChannelClick: (String) -> Unit,
    onToggleDrawer: () -> Unit,
    useDrawer: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                channel.id?.let { onChannelClick(it) }
            }
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (useDrawer) {
            IconButton(onClick = {
                onToggleDrawer()
            }) {
                Icon(
                    painter = painterResource(R.drawable.icn_menu_24dp),
                    contentDescription = stringResource(R.string.menu)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))
        } else {
            // Compensate for the IconButton not increasing our height
            Spacer(
                modifier = Modifier
                    .height(48.dp)
                    .width(12.dp)
            )
        }

        channel.channelType?.let {
            when (it) {
                ChannelType.TextChannel, ChannelType.VoiceChannel -> {
                    ChannelIcon(
                        channel = channel,
                        modifier = Modifier.alpha(0.6f)
                    )
                }
                else -> {
                    ChannelIcon(
                        channelType = it,
                        modifier = Modifier.alpha(0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = channel.name
                    ?: ChannelUtils.resolveName(channel)
                    ?: if (channel.channelType == ChannelType.SavedMessages) {
                        stringResource(R.string.channel_notes)
                    } else {
                        stringResource(R.string.unknown)
                    },
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                painter = painterResource(R.drawable.icn_keyboard_arrow_right_24dp),
                contentDescription = stringResource(R.string.menu),
                modifier = Modifier
                    .size(18.dp)
                    .alpha(0.4f)
            )
        }
    }
}
