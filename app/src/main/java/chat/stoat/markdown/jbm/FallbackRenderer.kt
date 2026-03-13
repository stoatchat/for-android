package chat.stoat.markdown.jbm

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import chat.stoat.activities.InviteActivity
import chat.stoat.api.STOAT_FILES
import chat.stoat.api.STOAT_WEB_APP
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.ResourceLocations
import chat.stoat.core.model.schemas.isInviteUri

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FallbackRenderer(content: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val mdState = LocalJBMarkdownTreeState.current

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                val assetLoader = WebViewAssetLoader.Builder()
                    .setDomain(Uri.parse(STOAT_WEB_APP).host!!)
                    .addPathHandler(
                        "/_android_assets/",
                        WebViewAssetLoader.AssetsPathHandler(context)
                    )
                    .addPathHandler(
                        "/_android_res/",
                        WebViewAssetLoader.ResourcesPathHandler(context)
                    )
                    .build()

                webChromeClient = object : WebChromeClient() {}
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        return request?.let { assetLoader.shouldInterceptRequest(it.url) }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        webResourceRequest: WebResourceRequest
                    ): Boolean {
                        // Capture clicks on invite links
                        if (webResourceRequest.url.isInviteUri()) {
                            val intent = Intent(
                                context,
                                InviteActivity::class.java
                            ).setAction(Intent.ACTION_VIEW)

                            intent.data = webResourceRequest.url
                            context.startActivity(intent)

                            return true
                        }

                        // Otherwise, open the link in the browser using androidx.browser
                        val customTab = CustomTabsIntent.Builder()
                            .setShowTitle(true)
                            .setDefaultColorSchemeParams(
                                CustomTabColorSchemeParams.Builder()
                                    .setToolbarColor(colors.background.toArgb())
                                    .build()
                            )
                            .build()
                        customTab.launchUrl(context, webResourceRequest.url)

                        // Prevent the WebView from navigating to the URL
                        return true
                    }
                }

                settings.apply {
                    javaScriptEnabled = true
                    setSupportZoom(false)
                    setSupportMultipleWindows(false)
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    cacheMode = WebSettings.LOAD_NO_CACHE
                }

                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun getSource(): String {
                            return content
                        }

                        @JavascriptInterface
                        fun getForegroundColour(): String {
                            return colors.onBackground.asHexString()
                        }

                        @JavascriptInterface
                        fun getPrimaryColour(): String {
                            return colors.primary.asHexString()
                        }

                        @JavascriptInterface
                        fun getMentionBackgroundColour(): String {
                            return colors.primary.copy(alpha = 0.2f).asHexString()
                        }

                        @JavascriptInterface
                        fun getCustomEmoteUrl(emoteId: String): String {
                            return "$STOAT_FILES/emojis/$emoteId/original"
                        }

                        @JavascriptInterface
                        fun resolveUserMention(userId: String): String {
                            return MentionResolver.resolveUser(userId, mdState.currentServer)
                        }

                        @JavascriptInterface
                        fun userAvatar(userId: String): String {
                            return ResourceLocations.userAvatarUrl(StoatAPI.userCache[userId])
                        }

                        @JavascriptInterface
                        fun resolveChannelMention(channelId: String): String {
                            return MentionResolver.resolveChannel(channelId)
                        }
                    },
                    "Bridge"
                )
                setBackgroundColor(Color.TRANSPARENT)

                loadUrl(
                    "$STOAT_WEB_APP/_android_assets/markdown/markdown.html"
                )
            }
        }
    )
}