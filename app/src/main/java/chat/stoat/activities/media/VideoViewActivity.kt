package chat.stoat.activities.media

import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import chat.stoat.R
import chat.stoat.api.StoatHttp
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.schemas.AutumnResource
import chat.stoat.databinding.ActivityVideoplayerBinding
import chat.stoat.providers.getAttachmentContentUri
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.launch

class VideoViewActivity : FragmentActivity() {
    private lateinit var binding: ActivityVideoplayerBinding

    private var player: ExoPlayer? = null

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

        val resourceUrl =
            "$STOAT_FILES/attachments/${autumnResource.id}/${autumnResource.filename}"

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityVideoplayerBinding.inflate(layoutInflater)

        binding.tbTop.title = autumnResource.filename
        binding.tbTop.setNavigationOnClickListener { finish() }
        binding.tbTop.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.mi_save -> {
                    downloadFile(autumnResource, resourceUrl)
                    true
                }

                R.id.mi_share_file -> {
                    shareFile(autumnResource, resourceUrl)
                    true
                }

                R.id.mi_share_link -> {
                    shareUrl(resourceUrl)
                    true
                }

                else -> false
            }
        }

        player = ExoPlayer.Builder(this).build().apply {
            setMediaItem(MediaItem.fromUri(resourceUrl))
            prepare()
            play()
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.xpPlayer) { v, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<MarginLayoutParams> {
                leftMargin = systemBarInsets.left
                bottomMargin = systemBarInsets.bottom
                rightMargin = systemBarInsets.right
            }
            WindowInsetsCompat.CONSUMED
        }

        binding.xpPlayer.player = player
        binding.xpPlayer.setFullscreenButtonClickListener {
            when (binding.alTop.visibility) {
                android.view.View.VISIBLE -> {
                    binding.alTop.visibility = android.view.View.GONE
                    WindowInsetsControllerCompat(window, binding.root).let { controller ->
                        controller.hide(WindowInsetsCompat.Type.systemBars())
                        controller.systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                    binding.xpPlayer.background = ColorDrawable(Color.BLACK)
                }

                else -> {
                    binding.alTop.visibility = android.view.View.VISIBLE
                    WindowInsetsControllerCompat(window, binding.root).let { controller ->
                        controller.show(WindowInsetsCompat.Type.systemBars())
                        controller.systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                    }
                    binding.xpPlayer.background = null
                }
            }
        }


        setContentView(binding.root)
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    private fun shareUrl(resourceUrl: String) {
        val intent =
            Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(
            Intent.EXTRA_TEXT,
            resourceUrl
        )

        val shareIntent = Intent.createChooser(intent, null)
        startActivity(shareIntent)
    }

    private fun shareFile(resource: AutumnResource, resourceUrl: String) {
        lifecycleScope.launch {
            val contentUri = getAttachmentContentUri(
                this@VideoViewActivity,
                resourceUrl,
                resource.id!!,
                resource.filename ?: "video"
            )

            val intent =
                Intent(Intent.ACTION_SEND)
            intent.type = resource.contentType ?: "video/*"
            intent.putExtra(
                Intent.EXTRA_TITLE,
                resource.filename
            )
            intent.putExtra(
                Intent.EXTRA_SUBJECT,
                resource.filename
            )
            intent.putExtra(
                Intent.EXTRA_STREAM,
                contentUri
            )

            val shareIntent = Intent.createChooser(intent, null)
            startActivity(shareIntent)
        }
    }

    private fun downloadFile(resource: AutumnResource, resourceUrl: String) {
        lifecycleScope.launch {
            this@VideoViewActivity.applicationContext.let {
                it.contentResolver.insert(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, resource.filename)
                        put(MediaStore.Video.Media.MIME_TYPE, resource.contentType)
                        put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Revolt")
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    }
                )
            }?.let { uri ->
                this@VideoViewActivity.contentResolver.openOutputStream(uri).use { stream ->
                    val video = StoatHttp.get(resourceUrl).readBytes()
                    stream?.write(video)

                    this@VideoViewActivity.applicationContext.let {
                        it.contentResolver.update(
                            uri,
                            ContentValues().apply {
                                put(MediaStore.Video.Media.IS_PENDING, 0)
                            },
                            null,
                            null
                        )
                    }

                    Snackbar.make(
                        binding.xpPlayer,
                        R.string.media_viewer_saved,
                        Snackbar.LENGTH_SHORT
                    ).setAction(
                        R.string.media_viewer_open
                    ) {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setDataAndType(uri, resource.contentType)
                        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        startActivity(intent)
                    }
                        .setActionTextColor(
                            MaterialColors.getColor(
                                binding.xpPlayer,
                                androidx.appcompat.R.attr.colorPrimary
                            )
                        )
                        .show()
                }
            }
        }
    }
}