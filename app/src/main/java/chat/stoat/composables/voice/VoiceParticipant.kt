package chat.stoat.composables.voice

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.composables.chat.displayNameInChannel
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.core.model.util.UserVoiceState
import chat.stoat.internals.extensions.TransparentListItemColours

@Composable
fun VoiceParticipant(
    state: UserVoiceState,
    channelId: String,
    speaking: Boolean,
    modifier: Modifier = Modifier
) {
    val speakingBorderColour by animateColorAsState(
        targetValue = if (speaking) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = spring()
    )
    val speakingBorderWidth by animateDpAsState(
        targetValue = if (speaking) 2.dp else 0.dp,
        animationSpec = spring()
    )

    val user = StoatAPI.userCache[state.id]
    ListItem(
        modifier = modifier,
        colors = TransparentListItemColours,
        headlineContent = {
            Text(displayNameInChannel(state.id, channelId))
        },
        leadingContent = {
            UserAvatar(
                username = displayNameInChannel(state.id, channelId),
                userId = state.id,
                allowAnimation = speaking,
                avatar = user?.avatar,
                modifier = Modifier.border(
                    speakingBorderWidth,
                    speakingBorderColour,
                    CircleShape
                )
            )
        },
        trailingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
            ) {
                if (!state.isPublishing) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mic_off_24dp),
                        contentDescription = stringResource(R.string.voice_muted),
                    )
                }
                if (!state.isReceiving) {
                    Icon(
                        painter = painterResource(R.drawable.ic_headset_off_24dp),
                        contentDescription = stringResource(R.string.voice_deafened),
                    )
                }
                if (state.camera) {
                    Icon(
                        painter = painterResource(R.drawable.ic_videocam_24dp),
                        contentDescription = stringResource(R.string.voice_camera_on),
                    )
                }
                if (state.screensharing) {
                    Icon(
                        painter = painterResource(R.drawable.ic_screen_share_24dp),
                        contentDescription = stringResource(R.string.voice_screen_sharing),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    )
}