package chat.revolt.sheets

import android.widget.Toast
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.routes.channel.removeMember
import chat.revolt.composables.generic.SheetButton
import chat.revolt.internals.Platform
import kotlinx.coroutines.launch

@Composable
fun ColumnScope.GroupDMMemberContextSheet(
    userId: String,
    channelId: String,
    dismissSheet: suspend () -> Unit,
    onRequestUpdateMembers: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    val channel = RevoltAPI.channelCache[channelId]
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(channel) {
        if (channel == null) {
            dismissSheet()
        }
    }

    if (channel == null) return

    if (channel.owner == RevoltAPI.selfId && userId != RevoltAPI.selfId) {
        SheetButton(
            headlineContent = {
                CompositionLocalProvider(value = LocalContentColor provides MaterialTheme.colorScheme.error) {
                    Text(
                        stringResource(
                            R.string.member_context_sheet_remove_from_channel,
                            channel.name ?: stringResource(R.string.unknown)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            leadingContent = {
                CompositionLocalProvider(value = LocalContentColor provides MaterialTheme.colorScheme.error) {
                    Icon(
                        painter = painterResource(R.drawable.icn_person_off_24dp),
                        contentDescription = null
                    )
                }
            },
            onClick = {
                scope.launch {
                    removeMember(channelId, userId)
                    onRequestUpdateMembers()
                    dismissSheet()
                }
            }
        )
    }

    // TODO replace with something useful (currently so that your sheet is not empty if you don't have permissions)
    SheetButton(
        headlineContent = {
            Text(stringResource(R.string.user_info_sheet_copy_id))
        },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.icn_identifier_copy_24dp),
                contentDescription = null
            )
        },
        onClick = {
            clipboardManager.setText(AnnotatedString(userId))

            if (Platform.needsShowClipboardNotification()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.copied),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    )


}

@Composable
fun ColumnScope.ServerMemberContextSheet(
    userId: String,
    serverId: String,
    channelId: String,
    dismissSheet: suspend () -> Unit,
    onRequestUpdateMembers: suspend () -> Unit
) {
    val server = RevoltAPI.serverCache[serverId]
    val channel = RevoltAPI.channelCache[channelId]
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(server) {
        if (server == null || channel == null) {
            dismissSheet()
        }
    }

    if (server == null || channel == null) return

    // TODO add something useful (moderation actions)

    // TODO replace with something useful (currently so that your sheet is not empty if you don't have permissions)
    SheetButton(
        headlineContent = {
            Text(stringResource(R.string.user_info_sheet_copy_id))
        },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.icn_identifier_copy_24dp),
                contentDescription = null
            )
        },
        onClick = {
            clipboardManager.setText(AnnotatedString(userId))

            if (Platform.needsShowClipboardNotification()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.copied),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    )


}