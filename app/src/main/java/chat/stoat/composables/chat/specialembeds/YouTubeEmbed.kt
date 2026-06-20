package chat.stoat.composables.chat.specialembeds

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import chat.stoat.R
import chat.stoat.composables.chat.VideoPlayButton
import chat.stoat.composables.generic.RemoteImage
import chat.stoat.core.model.data.STOAT_PROXY
import chat.stoat.core.model.schemas.Special
import org.intellij.lang.annotations.Language

@Language("HTML")
private const val YOUTUBE_EMBED_TEMPLATE = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        body {
            margin: 0;
            padding: 0;
            overflow: hidden;
            height: 100vh;
            width: 100vw;
            background-color: black;
        }
    </style>
</head>
<body>
    <script>
        const ytParams = new URLSearchParams()
        ytParams.set("autoplay", "1")
        ytParams.set("fs", "0")
        if ({{useTimestamp}}) {
            ytParams.set("start", "{{timestamp}}")
        }
        
        const frame = document.createElement("iframe")
        frame.setAttribute("src", `https://www.youtube.com/embed/{{videoId}}?${'$'}{ytParams.toString()}`)
        frame.setAttribute("width", window.innerWidth)
        frame.setAttribute("height", window.innerHeight)
        frame.setAttribute("frameborder", 0)
        frame.setAttribute("allow", "accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share")
        frame.setAttribute("referrerpolicy", "strict-origin-when-cross-origin")
        frame.setAttribute("allowfullscreen", "allowfullscreen")
        frame.setAttribute("title", "YouTube video player")
        document.body.appendChild(frame)
    </script>
</body>
"""

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeEmbed(special: Special, modifier: Modifier = Modifier) {
    if (special.id == null) return

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    javaScriptCanOpenWindowsAutomatically = true
                    builtInZoomControls = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    displayZoomControls = false
                    setSupportZoom(false)
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                }
            }
        },
        update = {
            it.loadDataWithBaseURL(
                STOAT_PROXY,
                YOUTUBE_EMBED_TEMPLATE
                    .replace("{{videoId}}", special.id!!)
                    .replace("{{useTimestamp}}", (special.timestamp != null).toString())
                    .replace("{{timestamp}}", special.timestamp ?: ""),
                "text/html",
                "UTF-8",
                null
            )
        },
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .width(with(LocalDensity.current) { 1280.toDp() })
            .aspectRatio(16f / 9f)
    )
}

/**
 * A switch that displays the thumbnail of a YouTube video and switches to the embedded player when clicked.
 * This ensures that the video is only loaded when the user wants to watch it for bandwidth reasons.
 */
@Composable
fun YoutubeEmbedSwitch(special: Special, modifier: Modifier = Modifier) {
    var embedEnabled by remember { mutableStateOf(false) }

    Crossfade(targetState = embedEnabled, label = "embed enabled") {
        if (it) {
            YouTubeEmbed(special, modifier)
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier
                    .clip(MaterialTheme.shapes.medium)
                    .clickable {
                        embedEnabled = true
                    }
                    .width(with(LocalDensity.current) { 1280.toDp() })
                    .aspectRatio(16f / 9f)
                    .wrapContentHeight()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                RemoteImage(
                    "https://i3.ytimg.com/vi/${special.id ?: "none"}/maxresdefault.jpg",
                    stringResource(R.string.message_embed_special_youtube_switch_alt),
                    width = 1280,
                    height = 720,
                )

                VideoPlayButton()
            }
        }
    }
}