package chat.revolt.composables.screens.chat

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import chat.revolt.R
import chat.revolt.api.schemas.ChannelType

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
