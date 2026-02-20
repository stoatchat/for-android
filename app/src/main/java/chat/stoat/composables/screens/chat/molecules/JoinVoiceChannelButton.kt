package chat.stoat.composables.screens.chat.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.core.model.schemas.User
import chat.stoat.callbacks.Action
import chat.stoat.callbacks.ActionChannel
import chat.stoat.composables.screens.chat.StackedUserAvatars
import kotlinx.coroutines.launch

@Composable
fun JoinVoiceChannelButton(channelId: String, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val voiceStatesForChannel = StoatAPI.voiceStateCache[channelId]

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
        if (voiceStatesForChannel?.participants?.isNotEmpty() == true) {
            StackedUserAvatars(
                users = voiceStatesForChannel.participants.map { it.id },
                size = 24.dp,
                offset = 12.dp,
                amount = voiceStatesForChannel.participants.size,
                serverId = null
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_voice_chat_24dp),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            stringResource(R.string.voice_join_offering),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Start
        )
        Text(
            when {
                voiceStatesForChannel == null || voiceStatesForChannel.participants.isEmpty() ->
                    stringResource(R.string.voice_join_offering_description_zero)

                voiceStatesForChannel.participants.size == 1 ->
                    stringResource(
                        R.string.voice_join_offering_description_one,
                        voiceStatesForChannel.participants[0].id.let {
                            StoatAPI.userCache[it]?.let { u -> User.resolveDefaultName(u) }
                                ?: R.string.unknown
                        })

                else ->
                    stringResource(
                        R.string.voice_join_offering_description_other,
                        voiceStatesForChannel.participants.size
                    )
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Start
        )
    }
}