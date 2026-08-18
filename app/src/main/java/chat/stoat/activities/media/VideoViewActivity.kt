package chat.stoat.activities.media

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.state.rememberErrorState
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import androidx.media3.ui.compose.state.rememberSeekBackButtonState
import androidx.media3.ui.compose.state.rememberSeekForwardButtonState
import chat.stoat.R
import chat.stoat.api.StoatHttp
import chat.stoat.api.settings.LoadedSettings
import chat.stoat.api.settings.SyncedSettings
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.schemas.AutumnResource
import chat.stoat.internals.extensions.zero
import chat.stoat.providers.getAttachmentContentUri
import chat.stoat.ui.theme.StoatTheme
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class VideoViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val autumnResource =
            // due to a bug in Android 13 we still use the deprecated method on Android 13, despite the new method being available
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("autumnResource", AutumnResource::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra("autumnResource")
            }

        if (autumnResource?.id == null) {
            Log.e("VideoViewActivity", "No AutumnResource provided")
            finish()
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            VideoViewScreen(resource = autumnResource, onClose = ::finish)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoViewScreen(resource: AutumnResource, onClose: () -> Unit = {}) {
    val resourceId = requireNotNull(resource.id)
    val filename = resource.filename ?: "video"
    val resourceUrl = "$STOAT_FILES/attachments/$resourceId/$filename"
    val context = LocalContext.current
    val resources = LocalResources.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val activityLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    var shareSubmenuIsOpen by remember { mutableStateOf(false) }
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    val player = remember(context, resourceUrl) {
        ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(5_000)
            .setSeekForwardIncrementMs(5_000)
            .build().apply {
                setMediaItem(MediaItem.fromUri(resourceUrl))
                prepare()
                playWhenReady = true
            }
    }
    var isPlaying by remember(player) { mutableStateOf(player.isPlaying) }

    DisposableEffect(player, lifecycleOwner) {
        val playerListener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) player.pause()
        }

        player.addListener(playerListener)
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            player.removeListener(playerListener)
            player.release()
        }
    }

    LaunchedEffect(isFullscreen, view) {
        val window = view.context.findActivity()?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)

        if (isFullscreen) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
    }

    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }

        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            controller?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
    }

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
        controlsVisible = true
    }

    fun shareUrl() {
        shareSubmenuIsOpen = false
        activityLauncher.launch(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, resourceUrl)
                },
                null
            )
        )
    }

    fun shareVideo() {
        shareSubmenuIsOpen = false
        coroutineScope.launch {
            val contentUri = getAttachmentContentUri(
                context,
                resourceUrl,
                resourceId,
                filename
            )

            activityLauncher.launch(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = resource.contentType ?: "video/*"
                        putExtra(Intent.EXTRA_TITLE, resource.filename)
                        putExtra(Intent.EXTRA_SUBJECT, resource.filename)
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    null
                )
            )
        }
    }

    fun saveToGallery() {
        coroutineScope.launch {
            context.contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, resource.filename)
                    put(MediaStore.Video.Media.MIME_TYPE, resource.contentType)
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Revolt")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            )?.let { uri ->
                context.contentResolver.openOutputStream(uri).use { stream ->
                    val video = StoatHttp.get(resourceUrl).readRawBytes()
                    stream?.write(video)
                }

                context.contentResolver.update(
                    uri,
                    ContentValues().apply {
                        put(MediaStore.Video.Media.IS_PENDING, 0)
                    },
                    null,
                    null
                )

                val result = snackbarHostState.showSnackbar(
                    message = resources.getString(R.string.media_viewer_saved),
                    actionLabel = resources.getString(R.string.media_viewer_open),
                    duration = SnackbarDuration.Short
                )

                if (result == SnackbarResult.ActionPerformed) {
                    activityLauncher.launch(
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, resource.contentType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    )
                }
            }
        }
    }

    StoatTheme(
        requestedTheme = LoadedSettings.theme,
        requestedUserInterfaceFont = LoadedSettings.font,
        colourOverrides = SyncedSettings.android.colourOverrides
    ) {
        Scaffold(
            containerColor = if (isFullscreen) {
                Color.Black
            } else {
                MaterialTheme.colorScheme.background
            },
            contentWindowInsets = if (isFullscreen) {
                WindowInsets.zero
            } else {
                ScaffoldDefaults.contentWindowInsets
            },
            topBar = {
                if (!isFullscreen) {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.media_viewer_title_video, filename),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onClose) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_back_24dp),
                                    contentDescription = stringResource(R.string.back)
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { shareSubmenuIsOpen = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_ios_share_24dp),
                                    contentDescription = stringResource(R.string.share)
                                )
                            }

                            DropdownMenu(
                                expanded = shareSubmenuIsOpen,
                                onDismissRequest = { shareSubmenuIsOpen = false }
                            ) {
                                DropdownMenuItem(
                                    onClick = ::shareUrl,
                                    text = { Text(stringResource(R.string.media_viewer_share_url)) }
                                )
                                DropdownMenuItem(
                                    onClick = ::shareVideo,
                                    text = { Text(stringResource(R.string.media_viewer_share_video)) }
                                )
                            }

                            IconButton(onClick = ::saveToGallery) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_download_24dp),
                                    contentDescription = stringResource(R.string.media_viewer_save)
                                )
                            }
                        }
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            StoatVideoPlayer(
                player = player,
                isPlaying = isPlaying,
                controlsVisible = controlsVisible,
                onControlsVisibleChange = { controlsVisible = it },
                isFullscreen = isFullscreen,
                onFullscreenChange = { isFullscreen = it },
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            )
        }
    }
}

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
@Composable
private fun StoatVideoPlayer(
    player: Player,
    isPlaying: Boolean,
    controlsVisible: Boolean,
    onControlsVisibleChange: (Boolean) -> Unit,
    isFullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val playPauseState = rememberPlayPauseButtonState(player)
    val seekBackState = rememberSeekBackButtonState(player)
    val seekForwardState = rememberSeekForwardButtonState(player)
    val progressState = rememberProgressStateWithTickInterval(player, tickIntervalMs = 250)
    val errorState = rememberErrorState(player)
    val interactionSource = remember { MutableInteractionSource() }
    var scrubPositionMs by remember { mutableStateOf<Long?>(null) }
    var controlsInteraction by remember { mutableIntStateOf(0) }
    var isBuffering by remember(player) {
        mutableStateOf(player.playbackState == Player.STATE_BUFFERING)
    }
    val keepControlsVisible = {
        controlsInteraction++
        onControlsVisibleChange(true)
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(
        controlsVisible,
        isPlaying,
        scrubPositionMs,
        isFullscreen,
        controlsInteraction
    ) {
        if (controlsVisible && isPlaying && scrubPositionMs == null) {
            delay(3.seconds)
            onControlsVisibleChange(false)
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onControlsVisibleChange(!controlsVisible) }
    ) {
        ContentFrame(
            player = player,
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.38f),
                            0.35f to Color.Transparent,
                            0.6f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.82f)
                        )
                    )
            ) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VideoControlButton(
                        icon = R.drawable.ic_replay_5_24dp,
                        contentDescription = stringResource(R.string.media_viewer_seek_back),
                        enabled = seekBackState.isEnabled,
                        onClick = {
                            seekBackState.onClick()
                            keepControlsVisible()
                        }
                    )

                    Spacer(Modifier.width(24.dp))

                    IconButton(
                        onClick = {
                            playPauseState.onClick()
                            keepControlsVisible()
                        },
                        enabled = playPauseState.isEnabled,
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            painter = painterResource(
                                if (playPauseState.showPlay) {
                                    R.drawable.ic_play_arrow_24dp__fill
                                } else {
                                    R.drawable.ic_pause_24dp__fill
                                }
                            ),
                            contentDescription = stringResource(
                                if (playPauseState.showPlay) {
                                    R.string.media_viewer_play
                                } else {
                                    R.string.media_viewer_pause
                                }
                            ),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(Modifier.width(24.dp))

                    VideoControlButton(
                        icon = R.drawable.ic_forward_5_24dp,
                        contentDescription = stringResource(R.string.media_viewer_seek_forward),
                        enabled = seekForwardState.isEnabled,
                        onClick = {
                            seekForwardState.onClick()
                            keepControlsVisible()
                        }
                    )
                }

                VideoTimeline(
                    currentPositionMs = scrubPositionMs ?: progressState.currentPositionMs,
                    durationMs = progressState.durationMs,
                    onScrub = {
                        scrubPositionMs = it
                        keepControlsVisible()
                    },
                    onScrubFinished = {
                        scrubPositionMs?.let(player::seekTo)
                        scrubPositionMs = null
                        keepControlsVisible()
                    },
                    isFullscreen = isFullscreen,
                    onFullscreenClick = {
                        onFullscreenChange(!isFullscreen)
                        keepControlsVisible()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .then(
                            if (isFullscreen) {
                                Modifier.windowInsetsPadding(
                                    WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)
                                )
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        if (isBuffering && errorState.error == null) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp),
                color = MaterialTheme.colorScheme.onSurface,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
            )
        }

        errorState.error?.let { error ->
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = error.localizedMessage
                        ?: stringResource(R.string.media_viewer_playback_error),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    player.prepare()
                    player.play()
                }) {
                    Text(stringResource(R.string.retry))
                }
            }
        }
    }
}

@Composable
private fun VideoControlButton(
    icon: Int,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(52.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), CircleShape)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.5f
            ),
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun VideoTimeline(
    currentPositionMs: Long,
    durationMs: Long,
    onScrub: (Long) -> Unit,
    onScrubFinished: () -> Unit,
    isFullscreen: Boolean,
    onFullscreenClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val usableDurationMs = durationMs.coerceAtLeast(0L)
    val sliderValue = if (usableDurationMs > 0L) {
        currentPositionMs.coerceIn(0L, usableDurationMs).toFloat() / usableDurationMs
    } else {
        0f
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatPlaybackTime(currentPositionMs),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFeatureSettings = "tnum"
            )
        )

        Slider(
            value = sliderValue,
            onValueChange = { onScrub((it * usableDurationMs).toLong()) },
            onValueChangeFinished = onScrubFinished,
            enabled = usableDurationMs > 0L,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                disabledThumbColor = Color.White.copy(alpha = 0.6f),
                disabledActiveTrackColor = Color.White.copy(alpha = 0.3f),
                disabledInactiveTrackColor = Color.White.copy(alpha = 0.18f)
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
        )

        Text(
            text = formatPlaybackTime(usableDurationMs),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFeatureSettings = "tnum"
            ),
        )

        Spacer(Modifier.width(4.dp))

        IconButton(onClick = onFullscreenClick) {
            Icon(
                painter = painterResource(
                    if (isFullscreen) {
                        R.drawable.ic_fullscreen_exit_24dp
                    } else {
                        R.drawable.ic_fullscreen_24dp
                    }
                ),
                contentDescription = stringResource(
                    if (isFullscreen) {
                        R.string.media_viewer_exit_fullscreen
                    } else {
                        R.string.media_viewer_fullscreen
                    }
                ),
                tint = Color.White
            )
        }
    }
}

private fun formatPlaybackTime(timeMs: Long): String {
    val totalSeconds = timeMs.coerceAtLeast(0L) / 1_000
    val seconds = totalSeconds % 60
    val minutes = totalSeconds / 60 % 60
    val hours = totalSeconds / 3_600

    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
