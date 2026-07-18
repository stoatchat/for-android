package chat.stoat.composables.voice

import android.app.Activity
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.composables.chat.displayNameInChannel
import chat.stoat.core.model.util.UserVoiceState
import chat.stoat.voice.VoiceCallManager
import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioDeviceChangeListener
import io.livekit.android.compose.local.RoomLocal
import io.livekit.android.compose.state.rememberParticipants
import io.livekit.android.compose.state.rememberTracks
import io.livekit.android.compose.ui.ScaleType
import io.livekit.android.compose.ui.VideoTrackView
import io.livekit.android.room.Room
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.screencapture.ScreenCaptureParams
import io.livekit.android.util.flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VoiceSheet(onDisconnect: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val room = VoiceCallManager.room
    val channelId = VoiceCallManager.activeChannelId
    if (room == null || channelId == null) return

    CompositionLocalProvider(RoomLocal provides room) {
        val roomState by room::state.flow.collectAsState()
        val isMicOn by room.localParticipant::isMicrophoneEnabled.flow.collectAsState()
        val isCameraOn by room.localParticipant::isCameraEnabled.flow.collectAsState()
        val isScreenShared by room.localParticipant::isScreenShareEnabled.flow.collectAsState()
        val activeSpeakers by room::activeSpeakers.flow.collectAsState()
        val participants by rememberParticipants(room)
        val trackRefs by rememberTracks(passedRoom = room)
        val isDeafened = VoiceCallManager.isDeafened

        val audioHandler = room.audioSwitchHandler
        var audioDevices by remember {
            mutableStateOf(audioHandler?.availableAudioDevices ?: emptyList())
        }
        var selectedAudioDevice by remember { mutableStateOf(audioHandler?.selectedAudioDevice) }
        DisposableEffect(audioHandler) {
            val listener: AudioDeviceChangeListener = { devices, selected ->
                audioDevices = devices
                selectedAudioDevice = selected
            }
            audioHandler?.registerAudioDeviceChangeListener(listener)
            onDispose {
                audioHandler?.unregisterAudioDeviceChangeListener(listener)
            }
        }

        Column {
            LazyColumn(
                modifier = Modifier
                    // don't push the toolbar off-screen
                    .weight(1f, fill = false)
                    .animateContentSize(
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = LinearOutSlowInEasing
                        )
                    )
            ) {
                val voiceStates = StoatAPI.voiceStateCache[channelId]
                items(participants.size) { index ->
                    val participant = participants[index]
                    val userId = participant.identity?.value
                    if (userId != null) {
                        val micEnabled by participant::isMicrophoneEnabled.flow.collectAsState()
                        val cameraEnabled by participant::isCameraEnabled.flow.collectAsState()
                        val screenShareEnabled by participant::isScreenShareEnabled.flow.collectAsState()
                        val cachedState = voiceStates?.participants?.find { it.id == userId }
                        VoiceParticipant(
                            state = UserVoiceState(
                                id = userId,
                                isReceiving = cachedState?.isReceiving ?: true,
                                isPublishing = micEnabled,
                                screensharing = screenShareEnabled,
                                camera = cameraEnabled,
                                joinedAt = cachedState?.joinedAt
                            ),
                            channelId = channelId,
                            speaking = activeSpeakers.any { it.identity == participant.identity }
                        )
                    }
                }
                items(trackRefs.size) { index ->
                    val trackRef = trackRefs[index]
                    val publication = trackRef.publication
                    if (publication != null) {
                        val isTrackMuted by publication::muted.flow.collectAsState()
                        if (!isTrackMuted) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .aspectRatio(16f / 9f)
                                    .clip(MaterialTheme.shapes.large)
                                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                            ) {
                                VideoTrackView(
                                    trackReference = trackRef,
                                    room = room,
                                    // Letterbox vertical feeds
                                    scaleType = ScaleType.FitInside,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainer.copy(
                                        alpha = 0.85f
                                    ),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 4.dp
                                        )
                                    ) {
                                        if (trackRef.source == Track.Source.SCREEN_SHARE) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_screen_share_24dp),
                                                contentDescription = stringResource(R.string.voice_screen_sharing),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = trackRef.participant.identity?.value
                                                ?.let { displayNameInChannel(it, channelId) }
                                                ?: stringResource(R.string.unknown),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item(key = "status") {
                    var showStatus by remember { mutableStateOf(true) }
                    LaunchedEffect(roomState) {
                        if (roomState == Room.State.CONNECTED) {
                            delay(1.seconds)
                            showStatus = false
                        } else {
                            showStatus = true
                        }
                    }

                    AnimatedVisibility(
                        visible = showStatus,
                        enter = fadeIn(
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = LinearOutSlowInEasing
                            )
                        ) +
                                expandVertically(
                                    expandFrom = Alignment.Top,
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        easing = LinearOutSlowInEasing
                                    )
                                ),
                        exit = fadeOut(
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = LinearOutSlowInEasing
                            )
                        ) +
                                slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        easing = LinearOutSlowInEasing
                                    )
                                ) +
                                shrinkVertically(
                                    shrinkTowards = Alignment.Top,
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        easing = LinearOutSlowInEasing
                                    )
                                )
                    ) {
                        AnimatedContent(
                            roomState
                        ) { roomState ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(
                                    8.dp,
                                    Alignment.CenterHorizontally
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                CompositionLocalProvider(
                                    LocalContentColor provides when (roomState) {
                                        Room.State.CONNECTING, Room.State.RECONNECTING -> MaterialTheme.colorScheme.onSurfaceVariant
                                        Room.State.CONNECTED -> MaterialTheme.colorScheme.primary
                                        Room.State.DISCONNECTED -> MaterialTheme.colorScheme.error
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            when (roomState) {
                                                Room.State.CONNECTING -> R.drawable.ic_sprint_24dp
                                                Room.State.CONNECTED -> R.drawable.ic_wifi_tethering_24dp
                                                Room.State.DISCONNECTED -> R.drawable.ic_wifi_tethering_error_24dp
                                                Room.State.RECONNECTING -> R.drawable.ic_sprint_24dp
                                            }
                                        ),
                                        contentDescription = null
                                    )
                                    Text(
                                        text = when (roomState) {
                                            Room.State.CONNECTING -> stringResource(R.string.voice_status_connecting)
                                            Room.State.CONNECTED -> stringResource(R.string.voice_status_connected)
                                            Room.State.DISCONNECTED -> stringResource(R.string.voice_status_disconnected)
                                            Room.State.RECONNECTING -> stringResource(R.string.voice_status_reconnecting)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(VoiceCallManager.errorResource != null) {
                VoiceCallManager.errorResource?.let { resId ->
                    Text(
                        text = stringResource(resId),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // the prompt where you select whether you want to share a single app or the whole screen
            val screenCaptureLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val data = result.data
                if (result.resultCode == Activity.RESULT_OK && data != null) {
                    scope.launch {
                        try {
                            val published = room.localParticipant.setScreenShareEnabled(
                                true,
                                ScreenCaptureParams(data)
                            )
                            logcat { "Screen share enable result: $published" }
                        } catch (e: Exception) {
                            logcat(LogPriority.ERROR) {
                                "Could not start screen share\n" + e.asLog()
                            }
                        }
                    }
                } else {
                    logcat(LogPriority.WARN) {
                        "Screen capture consent denied or empty " +
                                "(resultCode ${result.resultCode}, data $data)"
                    }
                }
            }

            var toolbarExpanded by remember { mutableStateOf(false) }
            var outputMenuOpen by remember { mutableStateOf(false) }
            val chevronRotation by animateFloatAsState(
                targetValue = if (toolbarExpanded) 90f else 270f,
                label = "voice toolbar chevron"
            )
            val firstOptionShape = MaterialTheme.shapes.extraSmall.copy(
                topStart = MaterialTheme.shapes.large.topStart,
                topEnd = MaterialTheme.shapes.large.topEnd
            )
            val toolbarTopCornerRadius by animateDpAsState(
                targetValue = if (toolbarExpanded) 4.dp else 48.dp,
                label = "voice toolbar corners"
            )
            AnimatedVisibility(
                visible = toolbarExpanded,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 8.dp,
                            end = 8.dp,
                            bottom = 2.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            headlineColor = if (isCameraOn) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                ListItemDefaults.colors().contentColor
                            },
                            leadingIconColor = if (isCameraOn) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                ListItemDefaults.colors().leadingContentColor
                            }
                        ),
                        headlineContent = { Text(stringResource(R.string.voice_action_video)) },
                        leadingContent = {
                            Icon(
                                painter = if (isCameraOn) painterResource(R.drawable.ic_videocam_24dp) else painterResource(
                                    R.drawable.ic_videocam_off_24dp
                                ),
                                contentDescription = null
                            )
                        },
                        modifier = Modifier
                            .clip(firstOptionShape)
                            .clickable {
                                scope.launch {
                                    room.localParticipant.setCameraEnabled(!isCameraOn)
                                }
                            }
                    )
                    AnimatedVisibility(visible = isCameraOn) {
                        ListItem(
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            headlineContent = {
                                Text(stringResource(R.string.voice_action_flip_camera))
                            },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_cameraswitch_24dp),
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.extraSmall)
                                .clickable {
                                    (room.localParticipant
                                        .getTrackPublication(Track.Source.CAMERA)
                                        ?.track as? LocalVideoTrack)
                                        ?.switchCamera()
                                }
                        )
                    }
                    ListItem(
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            headlineColor = if (isScreenShared) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                ListItemDefaults.colors().contentColor
                            },
                            leadingIconColor = if (isScreenShared) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                ListItemDefaults.colors().leadingContentColor
                            }
                        ),
                        headlineContent = { Text(stringResource(R.string.voice_action_screen_share)) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_mobile_share_24px),
                                contentDescription = null
                            )
                        },
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraSmall)
                            .clickable {
                                if (isScreenShared) {
                                    scope.launch {
                                        room.localParticipant.setScreenShareEnabled(false)
                                    }
                                } else {
                                    context.getSystemService(MediaProjectionManager::class.java)
                                        ?.let { manager ->
                                            screenCaptureLauncher.launch(
                                                manager.createScreenCaptureIntent()
                                            )
                                        }
                                }
                            }
                    )
                    if (audioHandler != null) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            ListItem(
                                colors = ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                ),
                                headlineContent = {
                                    Text(stringResource(R.string.voice_audio_output))
                                },
                                supportingContent = {
                                    selectedAudioDevice?.let { Text(audioDeviceLabel(it)) }
                                },
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_volume_up_24dp),
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .clickable { outputMenuOpen = true }
                            )
                            DropdownMenu(
                                expanded = outputMenuOpen,
                                onDismissRequest = { outputMenuOpen = false }
                            ) {
                                audioDevices.forEach { device ->
                                    DropdownMenuItem(
                                        text = { Text(audioDeviceLabel(device)) },
                                        trailingIcon = {
                                            if (device == selectedAudioDevice) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_check_24dp),
                                                    contentDescription = null
                                                )
                                            }
                                        },
                                        onClick = {
                                            audioHandler.selectDevice(device)
                                            outputMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            HorizontalFloatingToolbar(
                expanded = true,
                shape = RoundedCornerShape(
                    topStart = CornerSize(toolbarTopCornerRadius),
                    topEnd = CornerSize(toolbarTopCornerRadius),
                    bottomStart = CornerSize(50),
                    bottomEnd = CornerSize(50)
                ),
                modifier = Modifier.padding(
                    start = 8.dp,
                    end = 8.dp,
                    bottom = 16.dp,
                ),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 16.dp,
                )
            ) {
                Button(
                    onClick = { VoiceCallManager.toggleMicrophone() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMicOn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (isMicOn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                ) {
                    Icon(
                        painter = if (isMicOn) painterResource(R.drawable.ic_mic_24dp) else painterResource(
                            R.drawable.ic_mic_off_24dp
                        ),
                        contentDescription = stringResource(
                            if (isMicOn) R.string.voice_action_mute else R.string.voice_action_unmute
                        )
                    )
                }
                Spacer(Modifier.width(4.dp))
                Button(
                    onClick = { VoiceCallManager.toggleDeafen() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isDeafened) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (!isDeafened) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                ) {
                    Icon(
                        painter = if (isDeafened) painterResource(R.drawable.ic_headset_off_24dp) else painterResource(
                            R.drawable.ic_headset_24dp
                        ),
                        contentDescription = stringResource(
                            if (isDeafened) R.string.voice_action_undeafen else R.string.voice_action_deafen
                        )
                    )
                }
                Spacer(Modifier.width(4.dp))
                Button(
                    onClick = { toolbarExpanded = !toolbarExpanded },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_forward_24dp),
                        contentDescription = null,
                        modifier = Modifier.graphicsLayer { rotationZ = chevronRotation }
                    )
                }
                Spacer(Modifier.width(4.dp))
                Button(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier
                        .weight(2f)
                        .height(64.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_call_end_24dp__fill),
                        contentDescription = stringResource(R.string.voice_action_disconnect)
                    )
                }
            }
        }
    }
}

@Composable
private fun audioDeviceLabel(device: AudioDevice): String = when (device) {
    is AudioDevice.BluetoothHeadset -> device.name
    is AudioDevice.WiredHeadset -> stringResource(R.string.voice_audio_output_wired_headset)
    is AudioDevice.Earpiece -> stringResource(R.string.voice_audio_output_earpiece)
    is AudioDevice.Speakerphone -> stringResource(R.string.voice_audio_output_speaker)
}
