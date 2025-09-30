package chat.revolt.composables.screens.chat

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.schemas.Channel
import chat.revolt.api.schemas.ChannelType
import chat.revolt.composables.generic.RemoteImage

@Composable
fun ChannelIcon(channelType: ChannelType, modifier: Modifier = Modifier) {
    when (channelType) {
        ChannelType.TextChannel -> {
            Icon(
                painter = painterResource(R.drawable.icn_grid_3x3_24dp),
                contentDescription = stringResource(R.string.channel_text),
                modifier = modifier
            )
        }

        ChannelType.VoiceChannel -> {
            Icon(
                painter = painterResource(R.drawable.icn_volume_up_24dp),
                contentDescription = stringResource(R.string.channel_voice),
                modifier = modifier
            )
        }

        ChannelType.SavedMessages -> {
            Icon(
                painter = painterResource(R.drawable.icn_note_stack_24dp),
                contentDescription = stringResource(R.string.channel_notes),
                modifier = modifier
            )
        }

        ChannelType.DirectMessage -> {
            Icon(
                painter = painterResource(R.drawable.icn_account_circle_24dp),
                contentDescription = stringResource(R.string.channel_dm),
                modifier = modifier
            )
        }

        ChannelType.Group -> {
            Icon(
                painter = painterResource(R.drawable.icn_account_box_24dp),
                contentDescription = stringResource(R.string.channel_group),
                modifier = modifier
            )
        }
    }
}

@Composable
fun ChannelIcon(
    channel: Channel,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 24.dp
) {
    val channelType = channel.channelType ?: ChannelType.TextChannel
    
    if (channel.icon?.id != null) {
        RemoteImage(
            url = "$REVOLT_FILES/icons/${channel.icon.id}",
            description = channel.name ?: stringResource(R.string.unknown),
            contentScale = ContentScale.Crop,
            height = size.value.toInt(),
            width = size.value.toInt(),
            allowAnimation = false,
            modifier = modifier.size(size)
        )
    } else {
        ChannelIcon(channelType = channelType, modifier = modifier)
    }
}

class ChannelTypeProvider : PreviewParameterProvider<ChannelType> {
    override val values: Sequence<ChannelType>
        get() = sequenceOf(
            ChannelType.TextChannel,
            ChannelType.VoiceChannel,
            ChannelType.SavedMessages,
            ChannelType.DirectMessage,
            ChannelType.Group
        )

    override val count: Int
        get() = values.count()
}

@Preview
@Composable
fun ChannelIconPreview(@PreviewParameter(ChannelTypeProvider::class) channelType: ChannelType) {
    ChannelIcon(channelType = channelType)
}
