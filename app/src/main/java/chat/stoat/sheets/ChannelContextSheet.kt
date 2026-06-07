package chat.stoat.sheets

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.internals.has
import chat.stoat.callbacks.Action
import chat.stoat.callbacks.ActionChannel
import chat.stoat.composables.generic.SheetButton
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.internals.Platform
import chat.stoat.internals.extensions.rememberChannelPermissions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChannelContextSheet(channelId: String, onHideSheet: suspend () -> Unit) {
    val channel = StoatAPI.channelCache[channelId]
    if (channel == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val permissions by rememberChannelPermissions(channelId)

    val coroutineScope = rememberCoroutineScope()

    SheetButton(
        headlineContent = {
            Text(
                text = stringResource(id = R.string.channel_context_sheet_actions_copy_id),
            )
        },
        leadingContent = {
            Icon(
                painter = painterResource(id = R.drawable.ic_identifier_copy_24dp),
                contentDescription = null
            )
        },
        onClick = {
            if (channel.id == null) return@SheetButton

            clipboardManager.setText(AnnotatedString(channel.id!!))

            if (Platform.needsShowClipboardNotification()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.channel_context_sheet_actions_copy_id_copied),
                    Toast.LENGTH_SHORT
                ).show()
            }

            coroutineScope.launch {
                onHideSheet()
            }
        }
    )

    SheetButton(
        headlineContent = {
            Text(
                text = stringResource(id = R.string.channel_context_sheet_actions_mark_read),
            )
        },
        leadingContent = {
            Icon(
                painter = painterResource(id = R.drawable.ic_mark_chat_read_24dp),
                contentDescription = null
            )
        },
        onClick = {
            coroutineScope.launch {
                channel.lastMessageID?.let {
                    StoatAPI.unreads.markAsRead(channelId, it, sync = true)
                }
                onHideSheet()
            }
        }
    )

    if (
        (permissions has PermissionBit.ManageChannel)
        && (channel.channelType != ChannelType.SavedMessages && channel.channelType != ChannelType.DirectMessage)
    ) {
        SheetButton(
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.channel_context_sheet_actions_edit_channel),
                )
            },
            leadingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_edit_24dp),
                    contentDescription = null
                )
            },
            onClick = {
                coroutineScope.launch {
                    onHideSheet()
                }
                coroutineScope.launch {
                    delay(100) // wait for the sheet to close or at least start closing
                    ActionChannel.send(Action.TopNavigate("settings/channel/$channelId"))
                }
            }
        )
    }


}
