package chat.stoat.fragments

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.res.ResourcesCompat
import androidx.webkit.WebViewAssetLoader
import chat.stoat.R
import chat.stoat.activities.InviteActivity
import chat.stoat.api.STOAT_WEB_APP
import chat.stoat.core.model.schemas.isInviteUri
import chat.stoat.databinding.SheetChangelogBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.color.MaterialColors


class ChangelogBottomSheetFragment(
    val onDismiss: () -> Unit
) : BottomSheetDialogFragment() {
    private lateinit var binding: SheetChangelogBinding

    private fun argbAsCssColour(argb: Int): String {
        val alpha = (argb shr 24 and 0xff) / 255.0f
        val red = argb shr 16 and 0xff
        val green = argb shr 8 and 0xff
        val blue = argb and 0xff
        return String.format("#%02x%02x%02x%02x", red, green, blue, (alpha * 255).toInt())
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SheetChangelogBinding.inflate(inflater, container, false)
        requireArguments().run {
            binding.tvTitle.apply {
                text = when {
                    getBoolean(ARG_HISTORICAL) -> requireContext().getString(
                        R.string.settings_changelogs_historical_version_header,
                        getString(ARG_VERSION_NAME)
                    )

                    else -> requireContext().getString(R.string.settings_changelogs_new_header)
                }
                typeface = ResourcesCompat.getFont(requireContext(), R.font.inter_display_semibold)
            }

            binding.wvChangelog.apply {
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
                                    .setToolbarColor(
                                        MaterialColors.getColor(
                                            binding.wvChangelog,
                                            com.google.android.material.R.attr.backgroundColor
                                        )
                                    )
                                    .build()
                            )
                            .build()
                        customTab.launchUrl(context, webResourceRequest.url)

                        // Prevent the WebView from navigating to the URL
                        return true
                    }
                }

                loadUrl(
                    "$STOAT_WEB_APP/_android_assets/changelogs/renderer.html"
                )

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
                        fun getMarkdown(): String {
                            return getString(ARG_RENDERED_CONTENTS) ?: ""
                        }

                        @JavascriptInterface
                        fun getContentColour(): String {
                            return argbAsCssColour(
                                MaterialColors.getColor(
                                    binding.wvChangelog,
                                    com.google.android.material.R.attr.colorOnSurface
                                )
                            )
                        }

                        @JavascriptInterface
                        fun getPrimaryColour(): String {
                            return argbAsCssColour(
                                MaterialColors.getColor(
                                    binding.wvChangelog,
                                    androidx.appcompat.R.attr.colorPrimary
                                )
                            )
                        }
                    },
                    "Bridge"
                )

                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        }
        return binding.root
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        onDismiss()
    }

    companion object {
        const val TAG = "ChangelogBottomSheetFragment"

        private const val ARG_VERSION_NAME = "version_name"
        private const val ARG_HISTORICAL = "historical"
        private const val ARG_RENDERED_CONTENTS = "rendered_contents"

        fun createArguments(
            versionName: String,
            historical: Boolean,
            renderedContents: String,
        ): Bundle {
            return Bundle().apply {
                putString(ARG_VERSION_NAME, versionName)
                putBoolean(ARG_HISTORICAL, historical)
                putString(ARG_RENDERED_CONTENTS, renderedContents)
            }
        }
    }
}