package chat.stoat.composables.voice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.ChannelUtils
import chat.stoat.voice.VoiceCallManager

@Composable
fun VoiceCallBanner(modifier: Modifier = Modifier) {
    val channelId = VoiceCallManager.activeChannelId

    AnimatedVisibility(
        visible = channelId != null && !VoiceCallManager.isSheetVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { VoiceCallManager.isSheetVisible = true }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(start = 16.dp, end = 4.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_voice_chat_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = channelId
                            ?.let { StoatAPI.channelCache[it] }
                            ?.let(ChannelUtils::resolveName)
                            ?: stringResource(R.string.voice_notification_fallback_channel_name),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.voice_banner_tap_to_return),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { VoiceCallManager.leave() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_call_end_24dp__fill),
                        contentDescription = stringResource(R.string.voice_action_disconnect),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
