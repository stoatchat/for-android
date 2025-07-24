package chat.revolt.activities.media

import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.view.WindowCompat
import chat.revolt.R
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.RevoltHttp
import chat.revolt.api.schemas.AutumnResource
import chat.revolt.api.settings.LoadedSettings
import chat.revolt.api.settings.SyncedSettings
import chat.revolt.providers.getAttachmentContentUri
import chat.revolt.ui.theme.RevoltTheme
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.glide.ZoomableGlideImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState

class ImageViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // due to a bug in Android 13 we still use the deprecated method on Android 13, despite the new method being available
        val autumnResource = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("autumnResource", AutumnResource::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("autumnResource")
        }

        if (autumnResource?.id == null) {
            Log.e("ImageViewActivity", "No AutumnResource provided")
            finish()
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            ImageViewScreen(resource = autumnResource, onClose = { finish() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewScreen(resource: AutumnResource, onClose: () -> Unit = {}) {
    val resourceUrl = "$REVOLT_FILES/attachments/${resource.id}/${resource.filename}"

    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    val activityLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    val shareSubmenuIsOpen = remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun shareUrl() {
        shareSubmenuIsOpen.value = false

        val intent =
            Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(
            Intent.EXTRA_TEXT,
            resourceUrl
        )

        val shareIntent = Intent.createChooser(intent, null)
        activityLauncher.launch(shareIntent)
    }

    fun shareImage() {
        shareSubmenuIsOpen.value = false

        coroutineScope.launch {
            val contentUri = getAttachmentContentUri(
                context,
                resourceUrl,
                resource.id!!,
                resource.filename ?: "image"
            )

            val intent =
                Intent(Intent.ACTION_SEND)
            intent.type = resource.contentType ?: "image/*"
            intent.putExtra(
                Intent.EXTRA_STREAM,
                contentUri
            )

            val shareIntent = Intent.createChooser(intent, null)
            activityLauncher.launch(shareIntent)
        }
    }

    fun saveToGallery() {
        coroutineScope.launch {
            context.applicationContext.let {
                it.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, resource.filename)
                        put(MediaStore.Images.Media.MIME_TYPE, resource.contentType)
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Revolt")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                )
            }?.let { uri ->
                context.contentResolver.openOutputStream(uri).use { stream ->
                    val image = RevoltHttp.get(resourceUrl).readBytes()
                    stream?.write(image)

                    context.applicationContext.let {
                        it.contentResolver.update(
                            uri,
                            ContentValues().apply {
                                put(MediaStore.Images.Media.IS_PENDING, 0)
                            },
                            null,
                            null
                        )
                    }

                    val snackbar = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.media_viewer_saved),
                        actionLabel = context.getString(R.string.media_viewer_open),
                        duration = SnackbarDuration.Short
                    )

                    if (snackbar == SnackbarResult.ActionPerformed) {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setDataAndType(uri, resource.contentType)
                        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        activityLauncher.launch(intent)
                    }
                }
            }
        }
    }

    RevoltTheme(
        requestedTheme = LoadedSettings.theme,
        colourOverrides = SyncedSettings.android.colourOverrides
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(
                                id = R.string.media_viewer_title_image,
                                resource.filename ?: resource.id!!
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            onClose()
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.icn_arrow_back_24dp),
                                contentDescription = stringResource(id = R.string.back)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            shareSubmenuIsOpen.value = true
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.icn_ios_share_24dp),
                                contentDescription = stringResource(id = R.string.share)
                            )
                        }

                        DropdownMenu(
                            expanded = shareSubmenuIsOpen.value,
                            onDismissRequest = {
                                shareSubmenuIsOpen.value = false
                            }
                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    shareUrl()
                                },
                                text = {
                                    Text(
                                        stringResource(id = R.string.media_viewer_share_url)
                                    )
                                }
                            )
                            DropdownMenuItem(
                                onClick = {
                                    shareImage()
                                },
                                text = {
                                    Text(
                                        stringResource(
                                            id = R.string.media_viewer_share_image
                                        )
                                    )
                                }
                            )
                        }

                        IconButton(onClick = {
                            saveToGallery()
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.icn_download_24dp),
                                contentDescription = stringResource(
                                    id = R.string.media_viewer_save
                                )
                            )
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { pv ->
            Surface(
                modifier = Modifier
                    .padding(pv)
                    .background(MaterialTheme.colorScheme.background)
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .clip(RectangleShape)
                        .fillMaxSize()
                ) {
                    ZoomableGlideImage(
                        model = resourceUrl,
                        contentDescription = null,
                        state = rememberZoomableImageState(
                            rememberZoomableState(
                                zoomSpec = ZoomSpec(maxZoomFactor = 10f)
                            )
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }
            }
        }
    }
}
