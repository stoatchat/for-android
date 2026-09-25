package chat.stoat.composables.media

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

internal object AudioPlaybackCoordinator : DefaultLifecycleObserver {
    private var activePlayer: Player? = null

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun play(player: Player) {
        if (activePlayer !== player) {
            activePlayer?.pause()
            activePlayer = player
        }
        if (
            player.playbackState == Player.STATE_ENDED ||
            (player.duration > 0 && player.currentPosition >= player.duration)
        ) {
            player.seekTo(0)
        }
        player.play()
    }

    fun forget(player: Player) {
        if (activePlayer === player) {
            activePlayer = null
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        activePlayer?.pause()
    }
}

internal class AudioPlaybackState(val player: ExoPlayer) {
    val currentTime = mutableLongStateOf(0L)
    val isPlaying = mutableStateOf(false)
    val isLoading = mutableStateOf(false)
    private var anchorPositionMillis = 0L
    private var anchorRealtimeMillis = SystemClock.elapsedRealtime()

    fun seekTo(position: Long) {
        player.seekTo(position)
        currentTime.longValue = position
        anchor(position)
    }

    fun togglePlayback() {
        if (isPlaying.value) {
            player.pause()
        } else {
            AudioPlaybackCoordinator.play(player)
        }
    }

    fun onIsPlayingChanged(playing: Boolean) {
        isPlaying.value = playing
        val position = player.currentPosition.coerceAtLeast(0)
        currentTime.longValue = position
        anchor(position)
    }

    fun estimatedPosition(): Long {
        if (!player.isPlaying) return player.currentPosition.coerceAtLeast(0)

        val elapsedMillis = SystemClock.elapsedRealtime() - anchorRealtimeMillis
        val estimatedPosition = anchorPositionMillis +
            (elapsedMillis * player.playbackParameters.speed).toLong()
        return if (player.duration > 0) {
            estimatedPosition.coerceAtMost(player.duration)
        } else {
            estimatedPosition
        }
    }

    private fun anchor(position: Long) {
        anchorPositionMillis = position
        anchorRealtimeMillis = SystemClock.elapsedRealtime()
    }
}

@Composable
internal fun rememberAudioPlaybackState(url: String): AudioPlaybackState {
    val context = LocalContext.current
    val state = remember(context, url) {
        val player = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
        }
        AudioPlaybackState(player).also { state ->
            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    state.onIsPlayingChanged(playing)
                }

                override fun onIsLoadingChanged(loading: Boolean) {
                    state.isLoading.value = loading
                }
            })
        }
    }

    LaunchedEffect(state) {
        while (true) {
            if (state.player.playbackState == Player.STATE_ENDED) {
                state.player.pause()
                state.currentTime.longValue = 0
                state.player.seekTo(0)
            } else {
                state.currentTime.longValue = state.estimatedPosition()
            }
            if (state.player.isPlaying) {
                withFrameMillis { }
            } else {
                delay(100.milliseconds)
            }
        }
    }

    DisposableEffect(state) {
        onDispose {
            AudioPlaybackCoordinator.forget(state.player)
            state.player.release()
        }
    }

    return state
}
