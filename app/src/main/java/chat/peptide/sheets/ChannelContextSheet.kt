package chat.peptide.sheets

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import chat.peptide.R
import chat.peptide.api.PeptideAPI
import chat.peptide.api.internals.ChannelUtils
import chat.peptide.composables.generic.RemoteImage
import chat.peptide.composables.generic.SheetButton
import chat.peptide.internals.Platform
import chat.peptide.screens.chat.dialogs.InviteDialog
import kotlinx.coroutines.launch
import chat.peptide.api.settings.NotificationType
import chat.peptide.api.settings.SyncedSettings
import chat.peptide.api.schemas.NotificationSettings

@Composable
fun ChannelContextSheet(channelId: String, onHideSheet: suspend () -> Unit) {
    val channel = PeptideAPI.channelCache[channelId]
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

    val server = channel.server?.let { PeptideAPI.serverCache[it] }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()
    var inviteDialogShown by remember { mutableStateOf(false) }

    if (inviteDialogShown) {
        Dialog(onDismissRequest = { inviteDialogShown = false }) {
            InviteDialog(
                channelId = channelId,
                onDismissRequest = { inviteDialogShown = false }
            )
        }
    }

    // Animated content pages inside the sheet
    var currentPage by remember { mutableIntStateOf(0) } // 0 = main, 1 = notifications


    // Action buttons or notifications page
    AnimatedContent(
        targetState = currentPage,
        transitionSpec = {
            if (targetState > initialState) {
                // forward: slide in from right, out to left
                slideInHorizontally(animationSpec = tween(200)) { fullWidth -> fullWidth } togetherWith
                        slideOutHorizontally(animationSpec = tween(200)) { fullWidth -> -fullWidth }
            } else {
                // back: slide in from left, out to right
                slideInHorizontally(animationSpec = tween(200)) { fullWidth -> -fullWidth } togetherWith
                        slideOutHorizontally(animationSpec = tween(200)) { fullWidth -> fullWidth }
            }
        }, label = "Channel context sheet pager"
    ) { page ->
        if (page == 0) {
            Column(Modifier.fillMaxWidth()) {
                // Header with server image and channel info in a row layout
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        server?.icon?.id?.let { iconId ->
                            RemoteImage(
                                url = "${PeptideAPI.getCurrentFilesUrl()}/icons/$iconId",
                                allowAnimation = false,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                description = server.name ?: stringResource(R.string.unknown)
                            )
                        }

                        Text(
                            text = "# ${ChannelUtils.resolveName(channel) ?: stringResource(R.string.unknown)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    // Group 1: Mark as read + Notification options
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceBright)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SheetButton(
                                headlineContent = {
                                    Text(
                                        text = stringResource(id = R.string.channel_context_sheet_actions_mark_read),
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.icn_mark_chat_read_24dp),
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    coroutineScope.launch {
                                        channel.lastMessageID?.let {
                                            PeptideAPI.unreads.markAsRead(channelId, it, sync = true)
                                        }
                                        onHideSheet()
                                    }
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                            SheetButton(
                                headlineContent = {
                                    Text(
                                        text = stringResource(id = R.string.channel_info_sheet_options_notifications_manage),
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.icn_notification_settings_24dp),
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    currentPage = 1
                                }
                            )
                        }
                    }
                    // Group 2: Invite + Copy channel ID
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceBright)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SheetButton(
                                headlineContent = {
                                    Text(
                                        text = stringResource(id = R.string.channel_info_sheet_options_invite),
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.icn_add_24dp),
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    inviteDialogShown = true
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
    SheetButton(
        headlineContent = {
            Text(
                text = stringResource(id = R.string.channel_context_sheet_actions_copy_id),
                                        fontWeight = FontWeight.Medium
            )
        },
        leadingContent = {
            Icon(
                painter = painterResource(id = R.drawable.icn_identifier_copy_24dp),
                contentDescription = null
            )
        },
        onClick = {
            if (channel.id == null) return@SheetButton

            clipboardManager.setText(AnnotatedString(channel.id))

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
                        }
                    }
                }
            }
        }else{
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Top bar for notifications page
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
                            .clickable { currentPage = 0 }
                            .align(Alignment.CenterStart)
                    )
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = stringResource(id = R.string.channel_info_sheet_options_notifications_manage),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Notifications controls container (with radio options)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceBright)
                ) {
                    // Load current selection from SyncedSettings
                    var selected by remember {
                        mutableStateOf(
                            NotificationType.fromStorage(
                                SyncedSettings.notifications.channel[channelId]
                            ).storageValue
                        )
                    }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        NotificationRadioRow(
                            title = stringResource(id = R.string.notification_option_use_default),
                            type = NotificationType.DEFAULT,
                            channelId = channelId,
                            current = selected,
                            onChange = { selected = it }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                        NotificationRadioRow(
                            title = stringResource(id = R.string.notification_option_muted),
                            type = NotificationType.MUTED,
                            channelId = channelId,
                            current = selected,
                            onChange = { selected = it }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                        NotificationRadioRow(
                            title = stringResource(id = R.string.notification_option_all_messages),
                            type = NotificationType.ALL,
                            channelId = channelId,
                            current = selected,
                            onChange = { selected = it }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                        NotificationRadioRow(
                            title = stringResource(id = R.string.notification_option_mentions_only),
                            type = NotificationType.MENTIONS_ONLY,
                            channelId = channelId,
                            current = selected,
                            onChange = { selected = it }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                        NotificationRadioRow(
                            title = stringResource(id = R.string.notification_option_none),
                            type = NotificationType.NONE,
                            channelId = channelId,
                            current = selected,
                            onChange = { selected = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationOptionButton(
    title: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    SheetButton(
        headlineContent = { Text(text = title) },
        leadingContent = {
            Icon(
                painter = painterResource(id = R.drawable.icn_notification_settings_24dp),
                contentDescription = null
            )
        },
        trailingContent = {
            androidx.compose.material3.RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
        },
        onClick = onSelect
    )
}

@Composable
fun NotificationRadioRow(
    title: String,
    type: NotificationType,
    channelId: String,
    current: String,
    onChange: (String) -> Unit
) {
    val isSelected = current == type.storageValue
    SheetButton(
        headlineContent = { Text(text = title) },
        leadingContent = {
            Icon(
                painter = painterResource(id = R.drawable.icn_notification_settings_24dp),
                contentDescription = null
            )
        },
        trailingContent = {
            androidx.compose.material3.RadioButton(
                selected = isSelected,
                onClick = {
                    onChange(type.storageValue)
                    val currentSettings = SyncedSettings.notifications
                    val updated = NotificationSettings(
                        server = currentSettings.server,
                        channel = currentSettings.channel.toMutableMap().apply {
                            if (type == NotificationType.DEFAULT) {
                                remove(channelId)
                            } else {
                                put(channelId, type.storageValue)
                            }
                        }
                    )
                    kotlinx.coroutines.GlobalScope.launch {
                        SyncedSettings.updateNotifications(updated)
                    }
                }
            )
        },
        onClick = {
            onChange(type.storageValue)
            val currentSettings = SyncedSettings.notifications
            val updated = NotificationSettings(
                server = currentSettings.server,
                channel = currentSettings.channel.toMutableMap().apply {
                    if (type == NotificationType.DEFAULT) {
                        remove(channelId)
                    } else {
                        put(channelId, type.storageValue)
                    }
                }
            )
            kotlinx.coroutines.GlobalScope.launch {
                SyncedSettings.updateNotifications(updated)
            }
        }
    )
}
