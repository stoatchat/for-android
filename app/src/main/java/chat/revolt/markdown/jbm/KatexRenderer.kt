package chat.revolt.markdown.jbm

import android.annotation.SuppressLint
import android.content.Intent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import chat.revolt.activities.InviteActivity
import chat.revolt.api.RevoltAPI

internal fun Color.asHexString(includeAlphaComponent: Boolean = true): String {
    val argb = toArgb()
    val red = argb shr 16 and 0xff
    val green = argb shr 8 and 0xff
    val blue = argb and 0xff

    if (!includeAlphaComponent) {
        return String.format("#%02x%02x%02x", red, green, blue)
    }

    val alpha = (argb shr 24 and 0xff) / 255.0f
    return String.format("#%02x%02x%02x%02x", red, green, blue, (alpha * 255).toInt())
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KatexRenderer(content: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                val assetLoader = WebViewAssetLoader.Builder()
                    .setDomain(Uri.parse(RevoltAPI.getCurrentAppUrl()).host!!)
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
                        if (webResourceRequest.url.host == "rvlt.gg" ||
                            (
                                    webResourceRequest.url.host?.endsWith("revolt.chat") == true && webResourceRequest.url.path?.startsWith(
                                        "/invite"
                                    ) == true
                                    )
                        ) {
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
                    },
                    "Bridge"
                )
                setBackgroundColor(android.graphics.Color.TRANSPARENT)

                loadUrl(
                    "${RevoltAPI.getCurrentAppUrl()}/_android_assets/katex/katex.html"
                )
            }
        }
    )
}