package chat.stoat.composables.media

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import chat.stoat.R
import chat.stoat.internals.extensions.blockSwipeReplyOnHorizontalDrag

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VoiceMessagePlayer(
    url: String,
    waveform: List<Int>,
    durationMillis: Long,
) {
    val playback = rememberAudioPlaybackState(url)
    val player = playback.player
    val playbackDurationMillis = if (player.duration > 0) player.duration else durationMillis
    val progress = (
            playback.currentTime.longValue.toFloat() / playbackDurationMillis.toFloat()
            ).coerceIn(0f, 1f)
    val playedColour = MaterialTheme.colorScheme.primary
    val remainingColour = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val currentProgress = rememberUpdatedState(progress)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .blockSwipeReplyOnHorizontalDrag()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = playback::togglePlayback) {
                if (playback.isLoading.value && player.playWhenReady) {
                    LoadingIndicator()
                } else {
                    Icon(
                        painter = painterResource(
                            if (playback.isPlaying.value) {
                                R.drawable.ic_pause_24dp__fill
                            } else {
                                R.drawable.ic_play_arrow_24dp__fill
                            }
                        ),
                        contentDescription = stringResource(
                            if (playback.isPlaying.value) {
                                R.string.media_viewer_pause
                            } else {
                                R.string.media_viewer_play
                            }
                        )
                    )
                }
            }

            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .pointerInput(playbackDurationMillis) {
                        detectTapGestures { position ->
                            playback.seekTo(
                                (position.x / size.width * playbackDurationMillis.toFloat())
                                    .toLong()
                                    .coerceIn(0, playbackDurationMillis)
                            )
                        }
                    }
                    .drawWithCache {
                        val slotWidth = size.width / waveform.size
                        val barWidth = (slotWidth * 0.6f).coerceAtLeast(1f)
                        val minimumBarHeight = maxOf(2.dp.toPx(), barWidth)
                        val canvasHeight = size.height
                        val centreY = canvasHeight / 2f
                        val points = buildList(waveform.size * 2) {
                            waveform.forEachIndexed { index, sample ->
                                val barHeight = (
                                        canvasHeight * sample / UByte.MAX_VALUE.toFloat()
                                        ).coerceAtLeast(minimumBarHeight)
                                val halfLineHeight = (barHeight - barWidth) / 2f
                                val centreX = index * slotWidth + slotWidth / 2f
                                add(Offset(centreX, centreY - halfLineHeight))
                                add(Offset(centreX, centreY + halfLineHeight))
                            }
                        }

                        onDrawBehind {
                            drawPoints(
                                points = points,
                                pointMode = PointMode.Lines,
                                color = remainingColour,
                                strokeWidth = barWidth,
                                cap = StrokeCap.Round,
                            )

                            val progress = currentProgress.value.coerceIn(0f, 1f)
                            val playedWidth = if (progress >= 1f) {
                                size.width
                            } else {
                                val scaledProgress = progress * waveform.size
                                val activeBar = scaledProgress.toInt()
                                val activeBarProgress = scaledProgress - activeBar
                                activeBar * slotWidth +
                                        (slotWidth - barWidth) / 2f +
                                        barWidth * activeBarProgress
                            }
                            if (playedWidth > 0f) {
                                clipRect(
                                    right = playedWidth,
                                ) {
                                    drawPoints(
                                        points = points,
                                        pointMode = PointMode.Lines,
                                        color = playedColour,
                                        strokeWidth = barWidth,
                                        cap = StrokeCap.Round,
                                    )
                                }
                            }
                        }
                    }
            )
        }
    }
}
