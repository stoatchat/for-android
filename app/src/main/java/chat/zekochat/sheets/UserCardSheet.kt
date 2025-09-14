package chat.zekochat.sheets

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import chat.zekochat.BuildConfig
import chat.zekochat.R
import chat.zekochat.api.PeptideAPI
import chat.zekochat.api.schemas.User
import chat.zekochat.composables.generic.PepTextButton
import chat.zekochat.composables.generic.SquareButton
import chat.zekochat.composables.profile.UserCard
import chat.zekochat.internals.Platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun UserCardSheet(user: User?) {
    val scope = rememberCoroutineScope()
    val cardGraphics = rememberGraphicsLayer()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var hasError by remember { mutableStateOf(false) }

    suspend fun shareCard() {
        val folder = File(
            context.cacheDir,
            "usercards"
        ).let { File(it, PeptideAPI.selfId.toString()) }

        try {
            folder.mkdirs()
            val bitmap = cardGraphics.toImageBitmap()
            val file = File(folder, "usercard.png")
            val outputStream = withContext(Dispatchers.IO) {
                FileOutputStream(file)
            }

            bitmap
                .asAndroidBitmap()
                .compress(
                    Bitmap.CompressFormat.PNG,
                    90,
                    outputStream
                )

            withContext(Dispatchers.IO) {
                outputStream.flush()
                outputStream.close()
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/png"
            intent.putExtra(
                Intent.EXTRA_TITLE,
                "User Card"
            )
            intent.putExtra(
                Intent.EXTRA_SUBJECT,
                "User Card"
            )
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(Intent.createChooser(intent, null))
        } catch (e: Exception) {
            hasError = true
            e.printStackTrace()
        }
    }

    suspend fun copyCard() {
        val folder = File(
            context.cacheDir,
            "usercards"
        ).let { File(it, PeptideAPI.selfId.toString()) }

        folder.mkdirs()
        val bitmap = cardGraphics.toImageBitmap()
        val file = File(folder, "usercard.png")
        val outputStream = withContext(Dispatchers.IO) {
            FileOutputStream(file)
        }

        bitmap
            .asAndroidBitmap()
            .compress(
                Bitmap.CompressFormat.PNG,
                90,
                outputStream
            )

        withContext(Dispatchers.IO) {
            outputStream.flush()
            outputStream.close()
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            file
        )

        clipboardManager.nativeClipboard.setPrimaryClip(
            ClipData.newUri(
                context.contentResolver,
                "User Card",
                uri
            )
        )

        if (Platform.needsShowClipboardNotification()) {
            Toast.makeText(
                context,
                context.getString(R.string.copied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.user_card_tap_to_copy),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        )

        UserCard(
            user = user,
            graphicsLayer = cardGraphics,
            modifier = Modifier
                .clickable {
                    scope.launch {
                        copyCard()
                    }
                }
        )

        AnimatedVisibility(visible = hasError) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.user_card_error_sharing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (Platform.needsShowClipboardNotification()) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SquareButton(
                    onClick = {
                        scope.launch {
                            shareCard()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.icn_ios_share_24dp),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.share),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                PepTextButton(
                    onClick = {
                        scope.launch {
                            copyCard()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.icn_content_copy_24dp),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.copy),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}