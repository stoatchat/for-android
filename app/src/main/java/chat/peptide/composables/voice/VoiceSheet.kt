package chat.peptide.composables.voice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import chat.peptide.R
import chat.peptide.api.routes.misc.getRootRoute
import chat.peptide.api.routes.voice.joinCall
import logcat.LogPriority
import logcat.asLog
import logcat.logcat

class VoiceSheetViewModel(private val state: SavedStateHandle) : ViewModel() {
    private val _channelId = mutableStateOf(state.get<String>("channelId") ?: "")
    var channelId: String
        get() = _channelId.value
        set(value) {
            _channelId.value = value
            state["channelId"] = value
        }

    var voiceLkNode by mutableStateOf("")
    private val _voiceToken = mutableStateOf(state.get<String>("voiceToken") ?: "")
    var voiceToken: String
        get() = _voiceToken.value
        private set(value) {
            _voiceToken.value = value
            state["voiceToken"] = value
        }

    var errorResource by mutableStateOf<Int?>(null)
        private set

    suspend fun getVoiceToken() {
        val root = getRootRoute()
        val lk = root.features.livekit

        if (lk == null) {
            logcat(LogPriority.ERROR) {
                IllegalStateException("LiveKit is not supported by this API version!").asLog()
            }
            errorResource = R.string.voice_error_not_supported
            return
        }

        if (lk.nodes.isEmpty()) {
            logcat(LogPriority.ERROR) { IllegalStateException("No LiveKit nodes available!").asLog() }
            errorResource = R.string.voice_error_no_nodes
            return
        }

        val node = lk.nodes.random()
        try {
            val joined = joinCall(channelId, node.name)
            voiceLkNode = joined.url
            voiceToken = joined.token
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "Could not get LiveKit token\n" + e.asLog() }
            errorResource = R.string.voice_error_generic
            return
        }
    }
}

@Composable
fun VoiceSheet(
    channelId: String,
    onDisconnect: () -> Unit,
    viewModel: VoiceSheetViewModel = viewModel()
) {
    LaunchedEffect(channelId) {
        viewModel.channelId = channelId
        viewModel.getVoiceToken()
    }

    // TODO - Voice channels are not supported yet
    LaunchedEffect(Unit) { onDisconnect() }
    /*RoomScope(
        url = viewModel.voiceLkNode,
        token = viewModel.voiceToken,
        audio = true,
        video = false,
        connect = true,
    ) {
        val room = RoomLocal.current
        val trackRefs = rememberTracks()

        Column {
            LazyColumn(modifier = Modifier.animateContentSize()) {
                items(trackRefs.size) { index ->
                    VideoTrackView(
                        trackReference = trackRefs[index],
                        modifier = Modifier.fillParentMaxHeight(0.5f)
                    )
                }
                item(key = "stats") {
                    Text("status = ${room.state.name}")
                }
                item(key = "controls") {
                    Button(onClick = {
                        room.setMicrophoneMute(true)
                    }) {
                        Text("mute 🎤🫷😤")
                    }
                    Button(onClick = {
                        room.setMicrophoneMute(false)
                    }) {
                        Text("unmute 🗣️📢🔥")
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    colors = ButtonDefaults.buttonColors().copy(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        disabledContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        disabledContentColor = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.5f)
                    ),
                    onClick = {
                        room.disconnect()
                        onDisconnect()
                    },
                ) {
                    Text("disconnect")
                }
            }
        }
    }*/
}