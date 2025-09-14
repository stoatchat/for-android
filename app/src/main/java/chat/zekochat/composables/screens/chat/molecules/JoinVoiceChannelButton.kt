package chat.zekochat.composables.screens.chat.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chat.zekochat.R
import chat.zekochat.callbacks.Action
import chat.zekochat.callbacks.ActionChannel
import chat.zekochat.composables.screens.chat.StackedUserAvatars
import kotlinx.coroutines.launch

@Composable
fun JoinVoiceChannelButton(channelId: String, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable {
                scope.launch { ActionChannel.send(Action.OpenVoiceChannelOverlay(channelId)) }
            }
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        StackedUserAvatars(
            listOf(
                "01FHGJ3NPP7XANQQH8C2BE44ZY",
                "01F1WKM5TK2V6KCZWR6DGBJDTZ",
                "01EX2NCWQ0CHS3QJF0FEQS1GR4"
            ),
            size = 24.dp,
            offset = 12.dp,
            amount = 3,
            serverId = null
        )
        Text(
            stringResource(R.string.voice_join_offering),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Text(
            stringResource(R.string.voice_join_offering_description_other, Integer.MAX_VALUE),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}