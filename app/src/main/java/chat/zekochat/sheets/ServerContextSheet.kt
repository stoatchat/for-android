package chat.zekochat.sheets

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
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
import chat.zekochat.R
import chat.zekochat.api.PeptideAPI
import chat.zekochat.api.routes.server.leaveOrDeleteServer
import chat.zekochat.api.schemas.NotificationSettings
import chat.zekochat.api.settings.NotificationType
import chat.zekochat.api.settings.SyncedSettings
import chat.zekochat.composables.generic.PepTextButton
import chat.zekochat.composables.generic.SheetButton
import chat.zekochat.composables.generic.SquareButton
import chat.zekochat.composables.markdown.RichMarkdown
import chat.zekochat.composables.screens.settings.ServerOverview
import chat.zekochat.composables.sheets.SheetSelection
import chat.zekochat.callbacks.Action
import chat.zekochat.callbacks.ActionChannel
import chat.zekochat.internals.Platform
import chat.zekochat.screens.chat.ChatRouterDestination
import kotlinx.coroutines.GlobalScope
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

    var page by remember { mutableIntStateOf(0) }

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

    AnimatedContent(
        targetState = page,
        transitionSpec = {
            if (targetState > initialState) {
                slideInHorizontally(animationSpec = tween(200)) { it } togetherWith
                        slideOutHorizontally(animationSpec = tween(200)) { -it }
            } else {
                slideInHorizontally(animationSpec = tween(200)) { -it } togetherWith
                        slideOutHorizontally(animationSpec = tween(200)) { it }
            }
        }, label = "Server context pager"
    ) { current ->
        if (current == 0) {
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
                                painter = painterResource(R.drawable.icn_notification_settings_24dp),
                                contentDescription = null
                            )
                        },
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.channel_info_sheet_options_notifications_manage)
                            )
                        },
                        onClick = { page = 1 }
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
        } else {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_left_24dp),
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { page = 0 }
                            .align(Alignment.CenterStart)
                    )
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = stringResource(id = R.string.channel_info_sheet_options_notifications_manage),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Column(
                    modifier = Modifier
                        .padding(top = 24.dp, start = 12.dp, end = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceBright)
                ) {
                    var selected by remember {
                        mutableStateOf(
                            NotificationType.fromStorage(
                                SyncedSettings.notifications.server[server.id ?: ""]
                            ).storageValue
                        )
                    }

                    fun updateSelection(newValue: NotificationType) {
                        selected = newValue.storageValue
                        val currentSettings = SyncedSettings.notifications
                        val updated = NotificationSettings(
                            server = currentSettings.server.toMutableMap().apply {
                                if (server.id.isNullOrBlank() || newValue == NotificationType.DEFAULT) {
                                    if (!server.id.isNullOrBlank()) remove(server.id)
                                } else {
                                    put(server.id!!, newValue.storageValue)
                                }
                            },
                            channel = currentSettings.channel
                        )
                        GlobalScope.launch {
                            SyncedSettings.updateNotifications(updated)
                        }
                    }

                    @Composable
                    fun ServerNotificationRow(title: String, type: NotificationType) {
                        val isSelected = selected == type.storageValue
                        SheetButton(
                            headlineContent = { Text(text = title) },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(
                                        id = when (type) {
                                            NotificationType.DEFAULT -> R.drawable.icn_notif_use_default
                                            NotificationType.MUTED -> R.drawable.icn_notif_mute
                                            NotificationType.ALL -> R.drawable.icn_notif_all_messages
                                            NotificationType.MENTIONS_ONLY -> R.drawable.icn_notif_mention_only
                                            NotificationType.NONE -> R.drawable.icn_notif_none
                                        }
                                    ),
                                    contentDescription = null
                                )
                            },
                            trailingContent = {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { updateSelection(type) }
                                )
                            },
                            onClick = { updateSelection(type) }
                        )
                    }

                    ServerNotificationRow(
                        title = stringResource(id = R.string.notification_option_use_default),
                        type = NotificationType.DEFAULT
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    ServerNotificationRow(
                        title = stringResource(id = R.string.notification_option_muted),
                        type = NotificationType.MUTED
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    ServerNotificationRow(
                        title = stringResource(id = R.string.notification_option_all_messages),
                        type = NotificationType.ALL
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    ServerNotificationRow(
                        title = stringResource(id = R.string.notification_option_mentions_only),
                        type = NotificationType.MENTIONS_ONLY
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    ServerNotificationRow(
                        title = stringResource(id = R.string.notification_option_none),
                        type = NotificationType.NONE
                    )
                }
            }
        }
    }
}
