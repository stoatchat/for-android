package chat.stoat.screens.settings.server

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.routes.server.leaveOrDeleteServer
import chat.stoat.callbacks.Action
import chat.stoat.callbacks.ActionChannel
import chat.stoat.composables.generic.ListHeader
import chat.stoat.internals.extensions.rememberServerPermissions
import chat.stoat.screens.chat.ChatRouterDestination
import chat.stoat.screens.settings.SettingsIcon
import chat.stoat.screens.settings.SettingsListItem
import chat.stoat.settings.dsl.SettingsPage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ServerSettingsHome(
    navController: NavController,
    serverId: String,
    deleteServer: suspend (String) -> Unit = { leaveOrDeleteServer(it) },
) {
    val server = StoatAPI.serverCache[serverId]
    val permissions by rememberServerPermissions(serverId)
    val options = remember(permissions, server?.owner, StoatAPI.selfId) {
        permissions?.let {
            availableServerSettingsOptions(
                permissions = it,
                isOwner = server?.owner == StoatAPI.selfId,
            )
        }.orEmpty()
    }
    val scope = rememberCoroutineScope()

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    val deleteErrorFallback = stringResource(R.string.server_settings_delete_error)

    if (showDeleteConfirmation && server != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeleting) {
                    showDeleteConfirmation = false
                    deleteError = null
                }
            },
            title = {
                Text(
                    stringResource(
                        R.string.server_settings_delete_confirm,
                        server.name ?: serverId,
                    )
                )
            },
            text = {
                Column {
                    Text(stringResource(R.string.server_settings_delete_confirm_description))
                    deleteError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = {
                        showDeleteConfirmation = false
                        deleteError = null
                    },
                ) {
                    Text(stringResource(R.string.server_settings_delete_cancel))
                }
            },
            confirmButton = {
                Button(
                    enabled = !isDeleting,
                    onClick = {
                        if (isDeleting) return@Button

                        isDeleting = true
                        deleteError = null
                        scope.launch {
                            runCatching { deleteServer(serverId) }
                                .onSuccess {
                                    showDeleteConfirmation = false
                                    ActionChannel.send(
                                        Action.ChatNavigate(ChatRouterDestination.Overview)
                                    )
                                    navController.popBackStack()
                                }
                                .onFailure {
                                    deleteError = it.message ?: deleteErrorFallback
                                }
                            isDeleting = false
                        }
                    },
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(R.string.server_settings_delete_confirm_yes))
                    }
                }
            },
        )
    }

    SettingsPage(
        navController = navController,
        title = { Text(stringResource(R.string.server_settings)) },
    ) {
        if (server == null || permissions == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                LoadingIndicator()
            }
            return@SettingsPage
        }

        val overviewOptions = options.filter { it == ServerSettingsOption.Overview }
        val customisationOptions = options.filter { it == ServerSettingsOption.Emojis }
        val userManagementOptions = options.filter {
            it in listOf(
                ServerSettingsOption.Members,
                ServerSettingsOption.Roles,
                ServerSettingsOption.Invites,
                ServerSettingsOption.Bans,
            )
        }

        if (overviewOptions.isNotEmpty()) {
            ServerSettingsSection(
                title = server.name ?: stringResource(R.string.server_settings),
                options = overviewOptions,
                onOptionSelected = { option ->
                    if (option == ServerSettingsOption.Overview) {
                        navController.navigate("settings/server/$serverId/overview")
                    }
                },
            )
        }

        if (customisationOptions.isNotEmpty()) {
            ServerSettingsSection(
                title = stringResource(R.string.server_settings_category_customisation),
                options = customisationOptions,
                onOptionSelected = { option ->
                    if (option == ServerSettingsOption.Emojis) {
                        navController.navigate("settings/server/$serverId/emojis")
                    }
                },
            )
        }

        if (userManagementOptions.isNotEmpty()) {
            ServerSettingsSection(
                title = stringResource(R.string.server_settings_category_user_management),
                options = userManagementOptions,
                onOptionSelected = { option ->
                    if (option == ServerSettingsOption.Invites) {
                        navController.navigate("settings/server/$serverId/invites")
                    }
                },
            )
        }

        if (ServerSettingsOption.DeleteServer in options) {
            Spacer(Modifier.height(16.dp))
            ServerSettingsOptionRow(
                option = ServerSettingsOption.DeleteServer,
                first = true,
                last = true,
                onClick = { showDeleteConfirmation = true },
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ServerSettingsSection(
    title: String,
    options: List<ServerSettingsOption>,
    onOptionSelected: (ServerSettingsOption) -> Unit,
) {
    ListHeader { Text(title) }
    options.forEachIndexed { index, option ->
        ServerSettingsOptionRow(
            option = option,
            first = index == 0,
            last = index == options.lastIndex,
            onClick = { onOptionSelected(option) },
        )
        if (index != options.lastIndex) {
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun ServerSettingsOptionRow(
    option: ServerSettingsOption,
    first: Boolean,
    last: Boolean,
    onClick: () -> Unit,
) {
    val dangerous = option == ServerSettingsOption.DeleteServer

    SettingsListItem(
        first = first,
        last = last,
        headlineContent = {
            Text(
                text = stringResource(option.stringResource),
                color = if (dangerous) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        },
        leadingContent = {
            SettingsIcon(danger = dangerous) {
                Icon(
                    painter = painterResource(option.iconResource),
                    contentDescription = null,
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

private val ServerSettingsOption.stringResource: Int
    @StringRes get() = when (this) {
        ServerSettingsOption.Overview -> R.string.server_settings_overview
        ServerSettingsOption.Emojis -> R.string.server_settings_emojis
        ServerSettingsOption.Members -> R.string.server_settings_members
        ServerSettingsOption.Roles -> R.string.server_settings_roles
        ServerSettingsOption.Invites -> R.string.server_settings_invites
        ServerSettingsOption.Bans -> R.string.server_settings_bans
        ServerSettingsOption.DeleteServer -> R.string.server_settings_delete
    }

private val ServerSettingsOption.iconResource: Int
    @DrawableRes get() = when (this) {
        ServerSettingsOption.Overview -> R.drawable.ic_info_24dp
        ServerSettingsOption.Emojis -> R.drawable.ic_mood_24dp
        ServerSettingsOption.Members -> R.drawable.ic_group_24dp
        ServerSettingsOption.Roles -> R.drawable.ic_flag_24dp
        ServerSettingsOption.Invites -> R.drawable.ic_link_24dp
        ServerSettingsOption.Bans -> R.drawable.ic_gavel_24dp
        ServerSettingsOption.DeleteServer -> R.drawable.ic_delete_24dp
    }
