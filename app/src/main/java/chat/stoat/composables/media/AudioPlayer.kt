package chat.stoat.composables.media

import android.content.ContentValues
import android.content.Intent
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.stoat.R
import chat.stoat.api.StoatHttp
import chat.stoat.composables.LocalSnackbarHostState
import chat.stoat.internals.extensions.blockSwipeReplyOnHorizontalDrag
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AudioPlayer(
    url: String,
    filename: String,
    contentType: String,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbarHostState = LocalSnackbarHostState.current

    val showMenu = remember { mutableStateOf(false) }
    val playback = rememberAudioPlaybackState(url)
    val player = playback.player

    val coroutineScope = rememberCoroutineScope()

    val activityLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    fun formatTime(time: Long): String {
        val seconds = time / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            hours > 0 -> {
                val remainingMinutes = minutes % 60
                val remainingSeconds = seconds % 60

                "%02d:%02d:%02d".format(hours, remainingMinutes, remainingSeconds)
            }

            else -> {
                val remainingSeconds = seconds % 60

                "%02d:%02d".format(minutes, remainingSeconds)
            }
        }
    }

    fun saveToStorage() {
        showMenu.value = false

        coroutineScope.launch {
            context.applicationContext.let {
                it.contentResolver.insert(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
                        put(MediaStore.Audio.Media.MIME_TYPE, contentType)
                        put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Stoat")
                        put(MediaStore.Audio.Media.IS_PENDING, 1)
                    }
                )
            }?.let { uri ->
                context.contentResolver.openOutputStream(uri).use { stream ->
                    val audio = StoatHttp.get(url).readRawBytes()
                    stream?.write(audio)

                    context.applicationContext.let {
                        it.contentResolver.update(
                            uri,
                            ContentValues().apply {
                                put(MediaStore.Audio.Media.IS_PENDING, 0)
                            },
                            null,
                            null
                        )
                    }

                    val message = resources.getString(R.string.media_viewer_saved)
                    if (snackbarHostState != null) {
                        snackbarHostState.showSnackbar(message)
                    } else {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun shareUrl() {
        showMenu.value = false

        coroutineScope.launch {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, url)
            }

            val shareIntent = Intent.createChooser(intent, null)
            activityLauncher.launch(shareIntent)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .blockSwipeReplyOnHorizontalDrag()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Text(
                text = filename,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatTime(playback.currentTime.longValue),
                fontWeight = FontWeight.Medium,
                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
            )
            if (player.duration >= 0) {
                Text(
                    text = " / ${formatTime(player.duration)}",
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = playback::togglePlayback) {
                if (playback.isLoading.value && player.playWhenReady) {
                    LoadingIndicator()
                } else {
                    if (playback.isPlaying.value) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pause_24dp__fill),
                            contentDescription = stringResource(R.string.media_viewer_pause)
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_play_arrow_24dp__fill),
                            contentDescription = stringResource(R.string.media_viewer_play)
                        )
                    }
                }
            }

            if (player.duration >= 0) {
                Slider(
                    value = player.currentPosition.toFloat(),
                    onValueChange = { playback.seekTo(it.toLong()) },
                    valueRange = 0f..player.duration.toFloat(),
                    modifier = Modifier.weight(1f)
                )
            } else {
                Slider(
                    value = 0f,
                    onValueChange = {},
                    valueRange = 0f..1f,
                    enabled = false,
                    modifier = Modifier.weight(1f)
                )
            }

            IconButton(onClick = {
                showMenu.value = !showMenu.value
            }) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert_24dp),
                    contentDescription = stringResource(R.string.media_viewer_more)
                )
                DropdownMenu(
                    expanded = showMenu.value,
                    onDismissRequest = {
                        showMenu.value = false
                    }
                ) {
                    DropdownMenuItem(
                        onClick = {
                            saveToStorage()
                        },
                        text = {
                            Text(text = stringResource(R.string.media_viewer_save))
                        }
                    )
                    DropdownMenuItem(
                        onClick = {
                            shareUrl()
                        },
                        text = {
                            Text(text = stringResource(R.string.media_viewer_share_url))
                        }
                    )
                }
            }
        }
    }
}
