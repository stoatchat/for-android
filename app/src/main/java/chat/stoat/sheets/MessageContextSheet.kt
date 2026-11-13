package chat.stoat.sheets

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
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.internals.Roles
import chat.stoat.api.internals.has
import chat.stoat.api.routes.channel.deleteMessage
import chat.stoat.api.routes.channel.react
import chat.stoat.callbacks.UiCallbacks
import chat.stoat.composables.chat.Message
import chat.stoat.core.designsystem.components.SheetButton
import chat.stoat.core.designsystem.icons.StoatIcons
import chat.stoat.core.designsystem.icons.painter
import chat.stoat.core.model.data.STOAT_WEB_APP
import chat.stoat.internals.Platform
import chat.stoat.platform.LocalToaster
import chat.stoat.platform.show
import chat.stoat.shared.resources.Res
import chat.stoat.shared.resources.comingsoon_toast
import chat.stoat.shared.resources.copied
import chat.stoat.shared.resources.message_context_sheet_actions_copy
import chat.stoat.shared.resources.message_context_sheet_actions_copy_failed_empty
import chat.stoat.shared.resources.message_context_sheet_actions_copy_id
import chat.stoat.shared.resources.message_context_sheet_actions_copy_id_copied
import chat.stoat.shared.resources.message_context_sheet_actions_copy_link
import chat.stoat.shared.resources.message_context_sheet_actions_copy_link_copied
import chat.stoat.shared.resources.message_context_sheet_actions_delete
import chat.stoat.shared.resources.message_context_sheet_actions_delete_confirmation_body
import chat.stoat.shared.resources.message_context_sheet_actions_delete_confirmation_no
import chat.stoat.shared.resources.message_context_sheet_actions_delete_confirmation_title
import chat.stoat.shared.resources.message_context_sheet_actions_delete_confirmation_yes
import chat.stoat.shared.resources.message_context_sheet_actions_edit
import chat.stoat.shared.resources.message_context_sheet_actions_mark_unread
import chat.stoat.shared.resources.message_context_sheet_actions_react
import chat.stoat.shared.resources.message_context_sheet_actions_reply
import chat.stoat.shared.resources.message_context_sheet_actions_report
import chat.stoat.shared.resources.share
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageContextSheet(
    messageId: String,
    onHideSheet: suspend () -> Unit,
    onReportMessage: () -> Unit
) {
    val copiedText = stringResource(Res.string.copied)
    val messageContextSheetActionsCopyFailedEmptyText = stringResource(Res.string.message_context_sheet_actions_copy_failed_empty)
    val messageContextSheetActionsCopyLinkCopiedText = stringResource(Res.string.message_context_sheet_actions_copy_link_copied)
    val messageContextSheetActionsCopyIdCopiedText = stringResource(Res.string.message_context_sheet_actions_copy_id_copied)
    val comingsoonToastText = stringResource(Res.string.comingsoon_toast)
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

    val toaster = LocalToaster.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var showShareSheet by remember { mutableStateOf(false) }
    var showReactSheet by remember { mutableStateOf(false) }
    var showDeleteMessageConfirmation by remember { mutableStateOf(false) }

    if (showShareSheet) {
        val shareSheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        )

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
                            painter = StoatIcons.Copy.painter(),
                            contentDescription = null
                        )
                    },
                    headlineContent = {
                        Text(
                            text = stringResource(Res.string.message_context_sheet_actions_copy)
                        )
                    },
                    onClick = {
                        if (message.content.isNullOrEmpty()) {
                            coroutineScope.launch {
                                shareSheetState.hide()
                                onHideSheet()
                                toaster.show(getString(
                                        Res.string.message_context_sheet_actions_copy_failed_empty
                                    ))
                            }
                            return@SheetButton
                        }

                        if (Platform.needsShowClipboardNotification()) {
                            toaster.show(copiedText)
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
                            painter = StoatIcons.CopyLink.painter(),
                            contentDescription = null
                        )
                    },
                    headlineContent = {
                        Text(
                            text = stringResource(
                                Res.string.message_context_sheet_actions_copy_link
                            )
                        )
                    },
                    onClick = {
                        if (message.content.isNullOrEmpty()) {
                            toaster.show(messageContextSheetActionsCopyFailedEmptyText)

                            coroutineScope.launch {
                                shareSheetState.hide()
                            }
                            coroutineScope.launch {
                                onHideSheet()
                            }

                            return@SheetButton
                        }

                        val serverId = StoatAPI.channelCache[message.channel]?.server
                        val messagePath = if (serverId != null) {
                            "/server/$serverId/channel/${message.channel}/${message.id}"
                        } else {
                            "/channel/${message.channel}/${message.id}"
                        }
                        val messageLink = "$STOAT_WEB_APP$messagePath"

                        clipboardManager.setText(AnnotatedString(messageLink))
                        if (Platform.needsShowClipboardNotification()) {
                            toaster.show(messageContextSheetActionsCopyLinkCopiedText)
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
                            painter = StoatIcons.CopyId.painter(),
                            contentDescription = null
                        )
                    },
                    headlineContent = {
                        Text(
                            text = stringResource(
                                Res.string.message_context_sheet_actions_copy_id
                            )
                        )
                    },
                    onClick = {
                        if (message.id == null) return@SheetButton

                        clipboardManager.setText(AnnotatedString(message.id!!))

                        if (Platform.needsShowClipboardNotification()) {
                            toaster.show(messageContextSheetActionsCopyIdCopiedText)
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
        val reactSheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        )

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
                    text = stringResource(Res.string.message_context_sheet_actions_delete_confirmation_title)
                )
            },
            text = {
                Text(
                    text = stringResource(Res.string.message_context_sheet_actions_delete_confirmation_body)
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
                    Text(stringResource(Res.string.message_context_sheet_actions_delete_confirmation_yes))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteMessageConfirmation = false
                    }
                ) {
                    Text(stringResource(Res.string.message_context_sheet_actions_delete_confirmation_no))
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
                    painter = StoatIcons.Reply.painter(),
                    contentDescription = null
                )
            },
            headlineContent = {
                Text(
                    text = stringResource(Res.string.message_context_sheet_actions_reply),
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
                    painter = StoatIcons.AddReaction.painter(),
                    contentDescription = null
                )
            },
            headlineContent = {
                Text(
                    text = stringResource(Res.string.message_context_sheet_actions_react),
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
                        painter = StoatIcons.Edit.painter(),
                        contentDescription = null
                    )
                },
                headlineContent = {
                    Text(
                        text = stringResource(Res.string.message_context_sheet_actions_edit),
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
                    painter = StoatIcons.MarkUnread.painter(),
                    contentDescription = null
                )
            },
            headlineContent = {
                Text(
                    text = stringResource(Res.string.message_context_sheet_actions_mark_unread),
                )
            },
            onClick = {
                toaster.show(comingsoonToastText)

                coroutineScope.launch {
                    onHideSheet()
                }
            }
        )

        SheetButton(
            leadingContent = {
                Icon(
                    painter = StoatIcons.Share.painter(),
                    contentDescription = null,
                )
            },
            headlineContent = {
                Text(
                    text = stringResource(Res.string.share),
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
                        painter = StoatIcons.Delete.painter(),
                        contentDescription = null
                    )
                },
                headlineContent = {
                    Text(
                        text = stringResource(Res.string.message_context_sheet_actions_delete),
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
                        painter = StoatIcons.Report.painter(),
                        contentDescription = null
                    )
                },
                headlineContent = {
                    Text(
                        text = stringResource(Res.string.message_context_sheet_actions_report),
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
