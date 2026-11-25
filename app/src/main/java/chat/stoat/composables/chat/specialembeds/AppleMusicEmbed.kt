package chat.stoat.composables.chat.specialembeds

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebView
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import chat.stoat.core.designsystem.theme.LocalThemeIsDark
import chat.stoat.core.model.schemas.Special

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AppleMusicEmbed(special: Special, modifier: Modifier = Modifier) {
    val useDarkTheme = LocalThemeIsDark.current

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
            val embedUrl = Uri.Builder().apply {
                scheme("https")
                authority("embed.music.apple.com")
                appendPath("album")
                appendPath("")
                appendPath(special.albumID)
                appendQueryParameter("theme", if (useDarkTheme) "dark" else "light")
            }
            it.loadUrl(embedUrl.toString())
        },
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .width(400.dp)
            .requiredHeight(450.dp)
    )
}