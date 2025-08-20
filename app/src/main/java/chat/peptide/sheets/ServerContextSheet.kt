package chat.peptide.sheets

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import chat.peptide.R
import chat.peptide.api.PeptideAPI
import chat.peptide.api.routes.server.leaveOrDeleteServer
import chat.peptide.composables.generic.PepTextButton
import chat.peptide.composables.generic.SheetButton
import chat.peptide.composables.generic.SquareButton
import chat.peptide.composables.markdown.RichMarkdown
import chat.peptide.composables.screens.settings.ServerOverview
import chat.peptide.composables.sheets.SheetSelection
import chat.peptide.callbacks.Action
import chat.peptide.callbacks.ActionChannel
import chat.peptide.internals.Platform
import chat.peptide.screens.chat.ChatRouterDestination
import kotlinx.coroutines.launch

@Composable
fun ServerContextSheet(
    serverId: String,
    onReportServer: () -> Unit,
    onHideSheet: suspend () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showLeaveConfirmation by remember { mutableStateOf(false) }
    var leaveSilently by remember { mutableStateOf(false) }
    val server = PeptideAPI.serverCache[serverId]

    LaunchedEffect(server) {
        if (server == null) {
            coroutineScope.launch {
                onHideSheet()
            }
        }
    }

    if (server == null) {
        return
    }


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
                SquareButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                leaveOrDeleteServer(serverId, leaveSilently)
                                PeptideAPI.serverCache.remove(serverId)
                                ActionChannel.send(Action.ChatNavigate(ChatRouterDestination.Home))
                            } catch (_: Exception) {
                                // ignore
                            } finally {
                                showLeaveConfirmation = false
                            }
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
                PepTextButton(
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

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 4.dp),
        ) {
            ServerOverview(server)

            SelectionContainer {
                RichMarkdown(
                    input = if (server.description?.isBlank() == false) {
                        server.description
                    } else {
                        stringResource(
                            R.string.server_context_sheet_description_empty
                        )
                    }
                )
            }

            if (server.owner == PeptideAPI.selfId) {
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimary) {
                        SheetSelection(
                            icon = {},
                            title = {
                                Text(
                                    text = stringResource(id = R.string.server_context_sheet_moderators_early_disclaimer_title)
                                )
                            },
                            description = {
                                Text(
                                    text = stringResource(id = R.string.server_context_sheet_moderators_early_disclaimer_body)
                                )
                            },
                            arrowTint = LocalContentColor.current.copy(alpha = 0.5f),
                        ) {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    "${PeptideAPI.getCurrentAppUrl()}/server/${server.id}/settings".toUri()
                                )
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Primary actions group
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceBright)
        ) {
            SheetButton(
                leadingContent = {
                    Icon(
                        painter = painterResource(id = R.drawable.icn_identifier_copy_24dp),
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

                    clipboardManager.setText(AnnotatedString(server.id))

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
            HorizontalDivider()
            SheetButton(
                leadingContent = {
                    Icon(
                        painter = painterResource(id = R.drawable.icn_mark_chat_read_24dp),
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
                            PeptideAPI.unreads.markServerAsRead(it, sync = true)
                        }
                        onHideSheet()
                    }
                }
            )
        }

        // Danger actions group
        if (server.owner != PeptideAPI.selfId) {
            Column(
                modifier = Modifier
                    .padding(start = 12.dp, end = 12.dp, top = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceBright)
            ) {
                SheetButton(
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.icn_report_24dp),
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

                HorizontalDivider()

                SheetButton(
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.icn_door_open_24dp),
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
}
