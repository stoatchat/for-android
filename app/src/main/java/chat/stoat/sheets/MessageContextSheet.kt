package chat.stoat.sheets

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.internals.Roles
import chat.stoat.api.internals.has
import chat.stoat.api.routes.channel.deleteMessage
import chat.stoat.api.routes.channel.react
import chat.stoat.callbacks.UiCallbacks
import chat.stoat.composables.chat.Message
import chat.stoat.composables.generic.SheetButton
import chat.stoat.core.model.data.STOAT_WEB_APP
import chat.stoat.internals.Platform
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageContextSheet(
    messageId: String,
    onHideSheet: suspend () -> Unit,
    onReportMessage: () -> Unit
) {
    val message = StoatAPI.messageCache[messageId]
    if (message == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    val context = LocalContext.current
    val resources = LocalResources.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var showShareSheet by remember { mutableStateOf(false) }
    var showReactSheet by remember { mutableStateOf(false) }
    var showDeleteMessageConfirmation by remember { mutableStateOf(false) }

    if (showShareSheet) {
        val shareSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            sheetState = shareSheetState,
            onDismissRequest = {
                showShareSheet = false
            }
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
            ) {
                SheetButton(
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_content_copy_24dp),
                            contentDescription = null
                        )
                    },
                    headlineContent = {
                        Text(
                            text = stringResource(id = R.string.message_context_sheet_actions_copy)
                        )
                    },
                    onClick = {
                        if (message.content.isNullOrEmpty()) {
                            coroutineScope.launch {
                                shareSheetState.hide()
                                onHideSheet()
                                Toast.makeText(
                                    context,
                                    resources.getString(
                                        R.string.message_context_sheet_actions_copy_failed_empty
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            return@SheetButton
                        }

                        if (Platform.needsShowClipboardNotification()) {
                            Toast.makeText(
                                context,
                                resources.getString(R.string.copied),
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        coroutineScope.launch {
                            shareSheetState.hide()
                        }
                        coroutineScope.launch {
                            clipboardManager.setText(AnnotatedString(message.content!!))
                            onHideSheet()
                        }
                    }
                )

                SheetButton(
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_link_24dp),
                            contentDescription = null
                        )
                    },
                    headlineContent = {
                        Text(
                            text = stringResource(
                                id = R.string.message_context_sheet_actions_copy_link
                            )
                        )
                    },
                    onClick = {
                        if (message.content.isNullOrEmpty()) {
                            Toast.makeText(
                                context,
                                resources.getString(
                                    R.string.message_context_sheet_actions_copy_failed_empty
                                ),
                                Toast.LENGTH_SHORT
                            ).show()

                            coroutineScope.launch {
                                shareSheetState.hide()
                            }
                            coroutineScope.launch {
                                onHideSheet()
                            }

                            return@SheetButton
                        }

                        val server = StoatAPI.serverCache.values.find { server ->
                            server.channels?.contains(message.channel) ?: false
                        }
                        val messageLink =
                            "$STOAT_WEB_APP/server/${server?.id}/channel/${message.channel}/${message.id}"

                        clipboardManager.setText(AnnotatedString(messageLink))
                        if (Platform.needsShowClipboardNotification()) {
                            Toast.makeText(
                                context,
                                resources.getString(
                                    R.string.message_context_sheet_actions_copy_link_copied
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        coroutineScope.launch {
                            shareSheetState.hide()
                        }
                        coroutineScope.launch {
                            onHideSheet()
                        }
                    }
                )

                SheetButton(
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_identifier_copy_24dp),
                            contentDescription = null
                        )
                    },
                    headlineContent = {
                        Text(
                            text = stringResource(
                                id = R.string.message_context_sheet_actions_copy_id
                            )
                        )
                    },
                    onClick = {
                        if (message.id == null) return@SheetButton

                        clipboardManager.setText(AnnotatedString(message.id!!))

                        if (Platform.needsShowClipboardNotification()) {
                            Toast.makeText(
                                context,
                                resources.getString(
                                    R.string.message_context_sheet_actions_copy_id_copied
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        coroutineScope.launch {
                            shareSheetState.hide()
                        }
                        coroutineScope.launch {
                            onHideSheet()
                        }
                    }
                )
            }


        }
    }

    if (showReactSheet) {
        val reactSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            sheetState = reactSheetState,
            onDismissRequest = {
                showReactSheet = false
            }
        ) {
            ReactSheet(messageId) {
                if (it == null) return@ReactSheet

                coroutineScope.launch {
                    message.channel?.let { channelId ->
                        react(channelId, messageId, it)
                    }

                    reactSheetState.hide()
                    onHideSheet()
                }
            }
        }
    }

    if (showDeleteMessageConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showDeleteMessageConfirmation = false
            },
            title = {
                Text(
                    text = stringResource(R.string.message_context_sheet_actions_delete_confirmation_title)
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.message_context_sheet_actions_delete_confirmation_body)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteMessageConfirmation = false
                        coroutineScope.launch {
                            onHideSheet()
                            message.channel?.let { channelId ->
                                deleteMessage(channelId, messageId)
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.message_context_sheet_actions_delete_confirmation_yes))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteMessageConfirmation = false
                    }
                ) {
                    Text(stringResource(R.string.message_context_sheet_actions_delete_confirmation_no))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 4.dp),
        ) {
            Message(
                message = message.copy(
                    tail = false,
                    masquerade = null
                )
            )

            HorizontalDivider()
        }

        SheetButton(
            leadingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_reply_24dp),
                    contentDescription = null
                )
            },
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.message_context_sheet_actions_reply),
                )
            },
            onClick = {
                coroutineScope.launch {
                    UiCallbacks.replyToMessage(messageId)
                    onHideSheet()
                }
            }
        )

        SheetButton(
            leadingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add_reaction_24dp),
                    contentDescription = null
                )
            },
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.message_context_sheet_actions_react),
                )
            },
            onClick = {
                showReactSheet = true
            }
        )

        if (message.author == StoatAPI.selfId) {
            SheetButton(
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit_24dp),
                        contentDescription = null
                    )
                },
                headlineContent = {
                    Text(
                        text = stringResource(id = R.string.message_context_sheet_actions_edit),
                    )
                },
                onClick = {
                    coroutineScope.launch {
                        UiCallbacks.editMessage(messageId)
                        onHideSheet()
                    }
                }
            )
        }

        SheetButton(
            leadingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_visibility_off_24dp),
                    contentDescription = null
                )
            },
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.message_context_sheet_actions_mark_unread),
                )
            },
            onClick = {
                Toast.makeText(
                    context,
                    resources.getString(R.string.comingsoon_toast),
                    Toast.LENGTH_SHORT
                ).show()

                coroutineScope.launch {
                    onHideSheet()
                }
            }
        )

        SheetButton(
            leadingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ios_share_24dp),
                    contentDescription = null,
                )
            },
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.share),
                )
            },
            onClick = {
                showShareSheet = true
            }
        )

        if (
            (message.channel?.let {
                val channel = StoatAPI.channelCache[it] ?: return@let null
                Roles.permissionFor(
                    channel,
                    StoatAPI.userCache[StoatAPI.selfId]
                )
            } ?: 0) has PermissionBit.ManageMessages || message.author == StoatAPI.selfId
        ) {
            SheetButton(
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete_24dp),
                        contentDescription = null
                    )
                },
                headlineContent = {
                    Text(
                        text = stringResource(id = R.string.message_context_sheet_actions_delete),
                    )
                },
                dangerous = true,
                onClick = {
                    showDeleteMessageConfirmation = true
                }
            )
        }

        if (message.author != StoatAPI.selfId) {
            SheetButton(
                leadingContent = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_report_24dp),
                        contentDescription = null
                    )
                },
                headlineContent = {
                    Text(
                        text = stringResource(id = R.string.message_context_sheet_actions_report),
                    )
                },
                dangerous = true,
                onClick = {
                    coroutineScope.launch {
                        onReportMessage()
                    }
                },
            )
        }


    }
}
