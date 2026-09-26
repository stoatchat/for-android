package chat.stoat.sheets

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.routes.server.leaveOrDeleteServer
import chat.stoat.api.settings.ServerFolders
import chat.stoat.callbacks.Action
import chat.stoat.callbacks.ActionChannel
import chat.stoat.composables.generic.SheetButton
import chat.stoat.composables.markdown.prose.ChatMarkdown
import chat.stoat.composables.screens.chat.drawer.parseFolderColour
import chat.stoat.composables.screens.settings.ServerOverview
import chat.stoat.internals.Platform
import chat.stoat.internals.extensions.rememberServerPermissions
import chat.stoat.internals.server.availableServerSettingsOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ServerContextSheet(
    serverId: String,
    onReportServer: () -> Unit,
    onHideSheet: suspend () -> Unit
) {
    val server = StoatAPI.serverCache[serverId]

    if (server == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val permissions by rememberServerPermissions(serverId)
    val serverSettingsOptions = permissions?.let {
        availableServerSettingsOptions(
            permissions = it,
            isOwner = server.owner == StoatAPI.selfId,
        )
    }.orEmpty()

    var showLeaveConfirmation by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    val currentFolder = ServerFolders.folderOf(serverId)
    val newFolderName = stringResource(R.string.server_folder_default_name)
    var leaveSilently by remember { mutableStateOf(false) }

    if (showLeaveConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showLeaveConfirmation = false
            },
            title = {
                Text(
                    text = stringResource(
                        id = R.string.server_context_sheet_actions_leave_confirm,
                        server.name ?: stringResource(R.string.unknown)
                    )
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(
                            id = R.string.server_context_sheet_actions_leave_confirm_eyebrow
                        )
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 0.dp, end = 0.dp, top = 16.dp, bottom = 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = leaveSilently,
                            onCheckedChange = { leaveSilently = it }
                        )
                        Text(
                            text = stringResource(
                                id = R.string.server_context_sheet_actions_leave_silently
                            ),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            onHideSheet()
                        }
                        coroutineScope.launch {
                            leaveOrDeleteServer(serverId, leaveSilently)
                        }
                    }
                ) {
                    Text(
                        text = stringResource(
                            id = R.string.server_context_sheet_actions_leave_confirm_yes
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLeaveConfirmation = false
                    }
                ) {
                    Text(
                        text = stringResource(
                            id = R.string.server_context_sheet_actions_leave_confirm_no
                        )
                    )
                }
            }
        )
    }

    if (showFolderPicker) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            ServerFolders.folders.forEach { folder ->
                SheetButton(
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_folder_24dp),
                            contentDescription = null,
                            tint = folder.colour?.let(::parseFolderColour)
                                ?: LocalContentColor.current
                        )
                    },
                    headlineContent = {
                        Text(
                            folder.name.ifEmpty {
                                stringResource(R.string.server_folder_unnamed)
                            }
                        )
                    },
                    onClick = {
                        coroutineScope.launch {
                            onHideSheet()
                            ServerFolders.addServer(folder.id, serverId)
                        }
                    }
                )
            }

            SheetButton(
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_create_new_folder_24dp),
                        contentDescription = null
                    )
                },
                headlineContent = {
                    Text(stringResource(R.string.server_context_sheet_actions_new_folder))
                },
                onClick = {
                    coroutineScope.launch {
                        onHideSheet()
                        ServerFolders.create(newFolderName, listOf(serverId))
                    }
                }
            )
        }
        return
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 4.dp),
        ) {
            ServerOverview(server)

            SelectionContainer {
                ChatMarkdown(
                    content = if (server.description?.isBlank() == false) {
                        server.description!!
                    } else {
                        stringResource(R.string.server_context_sheet_description_empty)
                    },
                    serverId = serverId,
                )
            }

            HorizontalDivider()
        }

        SheetButton(
            leadingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_identifier_copy_24dp),
                    contentDescription = null
                )
            },
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.server_context_sheet_actions_copy_id)
                )
            },
            onClick = {
                if (server.id == null) return@SheetButton

                clipboardManager.setText(AnnotatedString(server.id!!))

                if (Platform.needsShowClipboardNotification()) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.server_context_sheet_actions_copy_id_copied),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                coroutineScope.launch {
                    onHideSheet()
                }
            }
        )

        SheetButton(
            leadingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_mark_chat_read_24dp),
                    contentDescription = null
                )
            },
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.server_context_sheet_actions_mark_read)
                )
            },
            onClick = {
                coroutineScope.launch {
                    server.id?.let {
                        StoatAPI.unreads.markServerAsRead(it, sync = true)
                    }
                    onHideSheet()
                }
            }
        )

        SheetButton(
            leadingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_folder_24dp),
                    contentDescription = null
                )
            },
            headlineContent = {
                Text(stringResource(R.string.server_context_sheet_actions_add_to_folder))
            },
            onClick = {
                if (ServerFolders.folders.isEmpty()) {
                    coroutineScope.launch {
                        onHideSheet()
                        ServerFolders.create(newFolderName, listOf(serverId))
                    }
                } else {
                    showFolderPicker = true
                }
            }
        )

        if (currentFolder != null) {
            SheetButton(
                leadingContent = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_folder_off_24dp),
                        contentDescription = null
                    )
                },
                headlineContent = {
                    Text(stringResource(R.string.server_context_sheet_actions_remove_from_folder))
                },
                onClick = {
                    coroutineScope.launch {
                        onHideSheet()
                        ServerFolders.removeServer(serverId)
                    }
                }
            )
        }

        SheetButton(
            leadingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_id_card_24dp),
                    contentDescription = null,
                )
            },
            headlineContent = {
                Text(stringResource(R.string.server_identity))
            },
            onClick = {
                coroutineScope.launch {
                    onHideSheet()
                }
                coroutineScope.launch {
                    delay(100.milliseconds)
                    ActionChannel.send(
                        Action.TopNavigate("settings/server/$serverId/identity")
                    )
                }
            },
        )

        if (serverSettingsOptions.isNotEmpty()) {
            SheetButton(
                leadingContent = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_settings_24dp),
                        contentDescription = null,
                    )
                },
                headlineContent = {
                    Text(stringResource(R.string.server_settings))
                },
                onClick = {
                    coroutineScope.launch {
                        onHideSheet()
                    }
                    coroutineScope.launch {
                        delay(100.milliseconds)
                        ActionChannel.send(Action.TopNavigate("settings/server/$serverId"))
                    }
                },
            )
        }

        if (server.owner != StoatAPI.selfId) {
            SheetButton(
                leadingContent = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_report_24dp),
                        contentDescription = null
                    )
                },
                headlineContent = {
                    Text(
                        text = stringResource(id = R.string.server_context_sheet_actions_report),
                    )
                },
                dangerous = true,
                onClick = {
                    onReportServer()
                }
            )

            SheetButton(
                leadingContent = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_door_open_24dp),
                        contentDescription = null,
                    )
                },
                headlineContent = {
                    Text(
                        text = stringResource(id = R.string.server_context_sheet_actions_leave)
                    )
                },
                dangerous = true,
                onClick = {
                    showLeaveConfirmation = true
                }
            )
        }
    }
}
