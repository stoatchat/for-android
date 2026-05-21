package chat.stoat.sheets

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.routes.custom.fetchEmoji
import chat.stoat.core.model.schemas.Emoji
import chat.stoat.core.model.schemas.Server
import chat.stoat.composables.generic.RemoteImage
import chat.stoat.composables.generic.SheetButton
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.internals.Platform
import kotlinx.coroutines.launch

@Composable
fun EmoteInfoSheet(id: String, onDismiss: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var emoteInfo by remember { mutableStateOf<Emoji?>(null) }
    var parentServer by remember { mutableStateOf<Server?>(null) }

    LaunchedEffect(id) {
        emoteInfo = StoatAPI.emojiCache[id] ?: fetchEmoji(id)
        when (emoteInfo?.parent?.type) {
            "Server" -> parentServer = StoatAPI.serverCache[emoteInfo?.parent?.id]
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            RemoteImage(
                url = "$STOAT_FILES/emojis/$id",
                description = emoteInfo?.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = ":${emoteInfo?.name ?: id}:",
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.15.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (parentServer != null) {
                        stringResource(
                            id = R.string.emote_info_from_server,
                            parentServer?.name ?: ""
                        )
                    } else {
                        stringResource(id = R.string.emote_info_from_server_unknown)
                    }
                )
            }
        }

        HorizontalDivider()
    }

    SheetButton(
        headlineContent = {
            Text(
                text = stringResource(id = R.string.copy)
            )
        },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_content_copy_24dp),
                contentDescription = null
            )
        },
        onClick = {
            coroutineScope.launch {
                clipboardManager.setText(AnnotatedString(":$id:"))
                if (Platform.needsShowClipboardNotification()) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.copied),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            onDismiss()
        }
    )


}
