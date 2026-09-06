package chat.stoat.screens.settings.channel

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.activities.StoatTweenFloat
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.BitDefaults
import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.internals.hasPermission
import chat.stoat.api.routes.channel.setChannelRolePermissions
import chat.stoat.api.routes.channel.setDefaultChannelPermissions
import chat.stoat.composables.generic.PermissionOverridePicker
import chat.stoat.composables.server.RoleColourIndicator
import chat.stoat.core.model.schemas.Channel
import chat.stoat.core.model.schemas.PermissionDescription
import chat.stoat.core.model.schemas.Role
import chat.stoat.internals.extensions.rememberChannelPermissions
import chat.stoat.internals.server.ChannelPermissionGroups
import chat.stoat.internals.server.PermissionOverrideValue
import chat.stoat.internals.server.ServerPermissionOption
import chat.stoat.internals.server.canManageServerRole
import chat.stoat.internals.server.overrideFor
import chat.stoat.internals.server.withOverride
import chat.stoat.screens.settings.SettingsIcon
import chat.stoat.screens.settings.SettingsListItem
import chat.stoat.settings.dsl.SettingsPage
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.koin.androidx.compose.koinViewModel

private const val DefaultChannelRoleId = "default"

private data class ChannelPermissionRoleEntry(
    val id: String,
    val role: Role,
)

class ChannelSettingsPermissionsViewModel(
    private val context: Application,
) : ViewModel() {
    var permissions by mutableStateOf(PermissionDescription(0, 0))
        private set
    var loaded by mutableStateOf(false)
        private set
    var saving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var saveSucceeded by mutableIntStateOf(0)
        private set

    private var loadedKey: Pair<String, String>? = null
    private var initialPermissions = PermissionDescription(0, 0)
    private var consumedSaveSucceeded = 0

    val hasChanges: Boolean
        get() = loaded && permissions != initialPermissions

    fun load(channelId: String, roleId: String) {
        val key = channelId to roleId
        if (loadedKey == key && loaded) return
        val channel = StoatAPI.channelCache[channelId] ?: return

        loadedKey = key
        initialPermissions = if (roleId == DefaultChannelRoleId) {
            channel.defaultPermissions
        } else {
            channel.rolePermissions?.get(roleId)
        } ?: PermissionDescription(0, 0)
        permissions = initialPermissions
        error = null
        loaded = true
    }

    fun updatePermission(option: ServerPermissionOption, value: PermissionOverrideValue) {
        permissions = permissions.withOverride(option.bit, value)
    }

    fun reset() {
        permissions = initialPermissions
        error = null
    }

    fun save(channelId: String, roleId: String) {
        if (!hasChanges || saving) return
        viewModelScope.launch {
            saving = true
            error = null
            runCatching {
                if (roleId == DefaultChannelRoleId) {
                    setDefaultChannelPermissions(channelId, permissions)
                } else {
                    setChannelRolePermissions(channelId, roleId, permissions)
                }
            }.onSuccess { channel ->
                initialPermissions = if (roleId == DefaultChannelRoleId) {
                    channel.defaultPermissions
                } else {
                    channel.rolePermissions?.get(roleId)
                } ?: permissions
                permissions = initialPermissions
                saveSucceeded++
            }.onFailure {
                error = it.message
                    ?: context.getString(R.string.channel_settings_permissions_update_error)
            }
            saving = false
        }
    }

    fun permissionsLostBySaving(
        channelId: String,
        roleId: String,
    ): List<ServerPermissionOption> {
        val channel = StoatAPI.channelCache[channelId] ?: return emptyList()
        val currentPermissions = effectivePermissionsFor(channel)
        val proposedChannel = if (roleId == DefaultChannelRoleId) {
            channel.copy(defaultPermissions = permissions)
        } else {
            channel.copy(
                rolePermissions = channel.rolePermissions.orEmpty() + (roleId to permissions)
            )
        }
        val proposedPermissions = effectivePermissionsFor(proposedChannel)

        return ChannelPermissionGroups
            .flatMap { it.permissions }
            .filter { option ->
                currentPermissions.hasPermission(option.bit) &&
                        !proposedPermissions.hasPermission(option.bit)
            }
    }

    private fun effectivePermissionsFor(channel: Channel): Long {
        val selfId = StoatAPI.selfId ?: return 0L
        val selfUser = StoatAPI.userCache[selfId] ?: return 0L
        val server = channel.server?.let(StoatAPI.serverCache::get) ?: return 0L
        if (selfUser.privileged == true || server.owner == selfId) {
            return PermissionBit.GrantAllSafe.value
        }

        val member = StoatAPI.members.getMember(server.id.orEmpty(), selfId) ?: return 0L
        var calculated = server.defaultPermissions ?: BitDefaults.Server

        fun apply(override: PermissionDescription?) {
            if (override != null) {
                calculated = (calculated or override.a) and override.d.inv()
            }
        }

        apply(channel.defaultPermissions)
        val assignedRoles = server.roles.orEmpty()
            .filterKeys { it in member.roles.orEmpty() }
            .toList()
            .sortedByDescending { (_, role) -> role.rank ?: Double.MAX_VALUE }
        assignedRoles.forEach { (_, role) ->
            apply(role.permissions)
        }
        assignedRoles.forEach { (roleId, _) ->
            apply(channel.rolePermissions?.get(roleId))
        }

        if (member.canPublish == false) {
            calculated = calculated and PermissionBit.Speak.value.inv()
            calculated = calculated and PermissionBit.Video.value.inv()
        }
        if (member.canReceive == false) {
            calculated = calculated and PermissionBit.Listen.value.inv()
        }
        if (member.timeoutTimestamp()?.let { it > Clock.System.now() } == true) {
            calculated = calculated and BitDefaults.AllowedInTimeout
        }
        return calculated.takeIf { it.hasPermission(PermissionBit.ViewChannel) } ?: 0L
    }

    fun consumeSaveSucceeded(): Boolean {
        if (consumedSaveSucceeded == saveSucceeded) return false
        consumedSaveSucceeded = saveSucceeded
        return true
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChannelSettingsPermissions(navController: NavController, channelId: String) {
    val channel = StoatAPI.channelCache[channelId]
    val server = channel?.server?.let(StoatAPI.serverCache::get)
    val roles = remember(server?.roles) {
        server?.roles.orEmpty()
            .map { ChannelPermissionRoleEntry(it.key, it.value) }
            .sortedBy { it.role.rank }
    }

    SettingsPage(
        navController = navController,
        title = { Text(stringResource(R.string.channel_settings_permissions)) },
    ) {
        if (channel == null || server == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
            return@SettingsPage
        }

        Text(
            text = stringResource(R.string.channel_settings_permissions_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Box {
            SettingsListItem(
                first = true,
                last = roles.isEmpty(),
                headlineContent = {
                    Text(
                        text = stringResource(R.string.server_settings_roles_everyone),
                        modifier = Modifier.offset(y = 4.dp),
                    )
                },
                supportingContent = {
                    Text(
                        text = stringResource(
                            R.string.channel_settings_permissions_everyone_description
                        ),
                        modifier = Modifier.offset(y = 4.dp),
                    )
                },
                leadingContent = { Spacer(Modifier.size(24.dp)) },
                modifier = Modifier.clickable {
                    navController.navigate(
                        "settings/channel/$channelId/permissions/$DefaultChannelRoleId"
                    )
                },
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 32.dp)
                    .size(24.dp),
            ) {
                SettingsIcon {
                    Icon(
                        painter = painterResource(R.drawable.ic_group_24dp),
                        contentDescription = null,
                    )
                }
            }
        }

        roles.forEachIndexed { index, entry ->
            Spacer(Modifier.height(2.dp))
            val elevated = canManageServerRole(server, entry.role)
            val override = channel.rolePermissions?.get(entry.id)
            val hasActiveOverrides = override != null && (override.a != 0L || override.d != 0L)
            SettingsListItem(
                last = index == roles.lastIndex,
                headlineContent = {
                    Text(
                        text = entry.role.name ?: entry.id,
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (hasActiveOverrides) 1f else 0.7f
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                supportingContent = if (elevated) null else {
                    { Text(stringResource(R.string.server_settings_roles_above_you)) }
                },
                leadingContent = {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(24.dp),
                    ) {
                        RoleColourIndicator(
                            colour = entry.role.colour,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                },
                modifier = Modifier.clickable {
                    navController.navigate("settings/channel/$channelId/permissions/${entry.id}")
                },
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChannelSettingsPermissionEditor(
    navController: NavController,
    channelId: String,
    roleId: String,
    viewModel: ChannelSettingsPermissionsViewModel = koinViewModel(),
) {
    val channel = StoatAPI.channelCache[channelId]
    val server = channel?.server?.let(StoatAPI.serverCache::get)
    val role = server?.roles?.get(roleId)
    val effectivePermissions by rememberChannelPermissions(channelId, channel ?: Unit)
    val elevated = roleId == DefaultChannelRoleId ||
            (server != null && role != null && canManageServerRole(server, role))
    val hasManagePermissions =
        effectivePermissions.hasPermission(PermissionBit.ManagePermissions)
    val canManage = hasManagePermissions && elevated
    var showDiscardConfirmation by remember { mutableStateOf(false) }
    var blockedPermissions by remember {
        mutableStateOf<List<ServerPermissionOption>?>(null)
    }

    LaunchedEffect(channelId, roleId, channel) {
        if (channel != null) viewModel.load(channelId, roleId)
    }

    fun navigateBack() {
        if (viewModel.saving) return
        if (viewModel.hasChanges) showDiscardConfirmation = true
        else navController.popBackStack()
    }

    BackHandler(onBack = ::navigateBack)

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = {
                Text(stringResource(R.string.channel_settings_permissions_discard_title))
            },
            text = {
                Text(stringResource(R.string.channel_settings_permissions_discard_description))
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmation = false }) {
                    Text(stringResource(R.string.server_settings_role_keep_editing))
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.reset()
                    showDiscardConfirmation = false
                    navController.popBackStack()
                }) {
                    Text(stringResource(R.string.server_settings_role_discard))
                }
            },
        )
    }

    blockedPermissions?.let { lostPermissions ->
        AlertDialog(
            onDismissRequest = { blockedPermissions = null },
            title = {
                Text(stringResource(R.string.channel_settings_permissions_not_applied))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(
                            R.string.channel_settings_permissions_not_applied_description
                        )
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        lostPermissions.forEach { permission ->
                            Text("• ${stringResource(permission.title)}")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { blockedPermissions = null }) {
                    Text(stringResource(R.string.ok))
                }
            },
        )
    }

    SettingsPage(
        navController = navController,
        title = {
            Text(
                if (roleId == DefaultChannelRoleId) {
                    stringResource(R.string.server_settings_roles_everyone)
                } else {
                    role?.name ?: stringResource(R.string.server_settings_role)
                }
            )
        },
        onNavigateBack = ::navigateBack,
        floatingActionButton = {
            AnimatedVisibility(
                visible = viewModel.hasChanges,
                enter = scaleIn(animationSpec = StoatTweenFloat),
                exit = scaleOut(animationSpec = StoatTweenFloat),
                modifier = Modifier.imePadding(),
            ) {
                val canSave = canManage && !viewModel.saving
                FloatingActionButton(
                    onClick = {
                        if (canSave) {
                            val lostPermissions = viewModel.permissionsLostBySaving(
                                channelId,
                                roleId,
                            )
                            if (lostPermissions.isEmpty()) {
                                viewModel.save(channelId, roleId)
                            } else {
                                blockedPermissions = lostPermissions
                            }
                        }
                    },
                    containerColor = if (canSave) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (canSave) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    if (viewModel.saving) {
                        LoadingIndicator(Modifier.size(24.dp))
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_check_24dp),
                            contentDescription = stringResource(
                                R.string.channel_settings_permissions_save
                            ),
                        )
                    }
                }
            }
        },
    ) {
        val savedMessage = stringResource(R.string.channel_settings_permissions_saved)
        LaunchedEffect(viewModel.saveSucceeded) {
            if (viewModel.consumeSaveSucceeded()) showSnackbar(savedMessage)
        }

        if (channel == null || server == null || !viewModel.loaded) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
            return@SettingsPage
        }

        Text(
            text = stringResource(R.string.channel_settings_permissions_role_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        if (!canManage) {
            ChannelPermissionReadOnlyNotice(
                message = if (!elevated) {
                    R.string.server_settings_roles_above_you
                } else {
                    R.string.channel_settings_permissions_permission
                }
            )
        }

        ChannelPermissionGroups.forEach { group ->
            Text(
                text = stringResource(group.title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 20.dp,
                    bottom = 8.dp,
                ),
            )
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                group.permissions.forEachIndexed { index, option ->
                    ChannelPermissionRow(
                        option = option,
                        value = viewModel.permissions.overrideFor(option.bit),
                        enabled = canManage &&
                                effectivePermissions.hasPermission(option.bit) &&
                                !viewModel.saving,
                        first = index == 0,
                        last = index == group.permissions.lastIndex,
                        onValueChange = { viewModel.updatePermission(option, it) },
                    )
                }
            }
        }

        AnimatedVisibility(viewModel.error != null) {
            Text(
                text = viewModel.error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun ChannelPermissionReadOnlyNotice(message: Int) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
    ) {
        Text(
            text = stringResource(message),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun ChannelPermissionRow(
    option: ServerPermissionOption,
    value: PermissionOverrideValue,
    enabled: Boolean,
    first: Boolean,
    last: Boolean,
    onValueChange: (PermissionOverrideValue) -> Unit,
) {
    val shape = when {
        first && last -> MaterialTheme.shapes.large
        first -> MaterialTheme.shapes.extraSmall.copy(
            topStart = MaterialTheme.shapes.large.topStart,
            topEnd = MaterialTheme.shapes.large.topEnd,
        )

        last -> MaterialTheme.shapes.extraSmall.copy(
            bottomStart = MaterialTheme.shapes.large.bottomStart,
            bottomEnd = MaterialTheme.shapes.large.bottomEnd,
        )

        else -> MaterialTheme.shapes.extraSmall
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = shape,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(option.title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(option.description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PermissionOverridePicker(
                value = value,
                enabled = enabled,
                onValueChange = onValueChange,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}
