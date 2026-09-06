package chat.stoat.screens.settings.server

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.internals.hasPermission
import chat.stoat.api.routes.server.fetchServerBans
import chat.stoat.api.routes.server.unbanMember
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.composables.settings.ServerSettingsEmptyState
import chat.stoat.core.model.schemas.BanListResult
import chat.stoat.core.model.schemas.BannedUser
import chat.stoat.core.model.schemas.ServerBan
import chat.stoat.internals.extensions.rememberServerPermissions
import chat.stoat.settings.dsl.SettingsPage
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

class ServerSettingsBansViewModel(
    private val context: Application,
) : ViewModel() {
    var banList by mutableStateOf<BanListResult?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var unbanningUserId by mutableStateOf<String?>(null)
        private set
    var unbanError by mutableStateOf<String?>(null)
        private set
    var unbanSucceeded by mutableIntStateOf(0)
        private set

    private var loadedServerId: String? = null

    fun load(serverId: String, force: Boolean = false) {
        if (!force && loadedServerId == serverId && (banList != null || loading)) return

        if (loadedServerId != serverId) banList = null
        loadedServerId = serverId
        loading = true
        loadError = null

        viewModelScope.launch {
            runCatching { fetchServerBans(serverId) }
                .onSuccess { result ->
                    if (loadedServerId == serverId) banList = result
                }
                .onFailure { error ->
                    if (loadedServerId == serverId) {
                        loadError = error.message
                            ?: context.getString(R.string.server_settings_bans_load_error)
                    }
                }

            if (loadedServerId == serverId) loading = false
        }
    }

    fun retryLoad() {
        loadedServerId?.let { load(it, force = true) }
    }

    fun unban(serverId: String, userId: String) {
        if (unbanningUserId != null) return

        viewModelScope.launch {
            unbanningUserId = userId
            unbanError = null

            runCatching { unbanMember(serverId, userId) }
                .onSuccess {
                    banList = banList?.let { current ->
                        current.copy(
                            users = current.users.filterNot { it.id == userId },
                            bans = current.bans.filterNot { it.id.user == userId },
                        )
                    }
                    unbanSucceeded++
                }
                .onFailure { error ->
                    unbanError = error.message
                        ?: context.getString(R.string.server_settings_bans_unban_error)
                }

            unbanningUserId = null
        }
    }

    fun clearUnbanError() {
        unbanError = null
    }
}

private data class BanListEntry(
    val ban: ServerBan,
    val user: BannedUser?,
    val bannedByName: String? = null,
)

private fun BanListResult.entries(): List<BanListEntry> {
    val usersById = users.associateBy(BannedUser::id)
    return bans
        .map { BanListEntry(it, usersById[it.id.user]) }
        .sortedWith(
            compareBy(String.CASE_INSENSITIVE_ORDER) { entry ->
                entry.user?.username ?: entry.ban.id.user
            }
        )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ServerSettingsBans(
    navController: NavController,
    serverId: String,
    viewModel: ServerSettingsBansViewModel = koinViewModel(),
) {
    val permissions by rememberServerPermissions(serverId)
    val server = StoatAPI.serverCache[serverId]
    val entries = remember(viewModel.banList) { viewModel.banList?.entries().orEmpty() }
    var detailsTarget by remember { mutableStateOf<BanListEntry?>(null) }
    var unbanTarget by remember { mutableStateOf<BanListEntry?>(null) }

    if (
        permissions != null &&
        permissions?.hasPermission(PermissionBit.BanMembers) == false
    ) {
        LaunchedEffect(serverId) { navController.popBackStack() }
        return
    }

    LaunchedEffect(serverId, permissions) {
        if (permissions?.hasPermission(PermissionBit.BanMembers) == true) {
            viewModel.load(serverId)
        }
    }

    BackHandler(enabled = viewModel.unbanningUserId != null) {}

    detailsTarget?.let { target ->
        val detailsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val detailsSheetScope = rememberCoroutineScope()
        ModalBottomSheet(
            sheetState = detailsSheetState,
            onDismissRequest = { detailsTarget = null },
        ) {
            BanDetailsSheet(
                entry = target,
                onDone = {
                    detailsSheetScope.launch {
                        detailsSheetState.hide()
                        detailsTarget = null
                    }
                },
            )
        }
    }

    unbanTarget?.let { target ->
        UnbanDialog(
            entry = target,
            unbanning = viewModel.unbanningUserId == target.ban.id.user,
            error = viewModel.unbanError,
            onConfirm = { viewModel.unban(serverId, target.ban.id.user) },
            onDismiss = {
                if (viewModel.unbanningUserId == null) {
                    viewModel.clearUnbanError()
                    unbanTarget = null
                }
            },
        )
    }

    SettingsPage(
        navController = navController,
        title = { Text(stringResource(R.string.server_settings_bans)) },
        scrollable = false,
        onNavigateBack = {
            if (viewModel.unbanningUserId == null) navController.popBackStack()
        },
    ) {
        val unbannedMessage = stringResource(R.string.server_settings_bans_unbanned)
        LaunchedEffect(viewModel.unbanSucceeded) {
            if (viewModel.unbanSucceeded > 0) {
                unbanTarget = null
                showSnackbar(unbannedMessage)
            }
        }

        if (permissions == null || server == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                LoadingIndicator()
            }
            return@SettingsPage
        }

        BanList(
            entries = entries,
            loading = viewModel.loading,
            loadError = viewModel.loadError,
            unbanningUserId = viewModel.unbanningUserId,
            onRetry = viewModel::retryLoad,
            onDetails = { detailsTarget = it },
            onUnban = {
                viewModel.clearUnbanError()
                unbanTarget = it
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BanList(
    entries: List<BanListEntry>,
    loading: Boolean,
    loadError: String?,
    unbanningUserId: String?,
    onRetry: () -> Unit,
    onDetails: (BanListEntry) -> Unit,
    onUnban: (BanListEntry) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "count") {
            Text(
                text = pluralStringResource(
                    R.plurals.server_settings_bans_count,
                    entries.size,
                    entries.size,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }

        if (loading && entries.isEmpty()) {
            item(key = "loading") {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(64.dp),
                ) {
                    LoadingIndicator()
                }
            }
        } else if (loadError != null && entries.isEmpty()) {
            item(key = "error") {
                BanLoadError(message = loadError, onRetry = onRetry)
            }
        } else if (entries.isEmpty()) {
            item(key = "empty") {
                ServerSettingsEmptyState(
                    icon = R.drawable.ic_gavel_24dp,
                    title = R.string.server_settings_bans_empty_title,
                    description = R.string.server_settings_bans_empty_description,
                )
            }
        } else {
            if (loading) {
                item(key = "refreshing") {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            if (loadError != null) {
                item(key = "refresh-error") {
                    BanLoadError(message = loadError, onRetry = onRetry)
                }
            }

            itemsIndexed(
                items = entries,
                key = { _, entry -> entry.ban.id.user },
            ) { index, entry ->
                BanListItem(
                    entry = entry,
                    first = index == 0,
                    last = index == entries.lastIndex,
                    enabled = unbanningUserId == null,
                    onDetails = { onDetails(entry) },
                    onUnban = { onUnban(entry) },
                )
                if (index != entries.lastIndex) Spacer(Modifier.height(2.dp))
            }
        }

        item(key = "bottom-space") { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun BanListItem(
    entry: BanListEntry,
    first: Boolean,
    last: Boolean,
    enabled: Boolean,
    onDetails: () -> Unit,
    onUnban: () -> Unit,
) {
    val userId = entry.ban.id.user
    val username = entry.user?.username ?: userId
    val displayName = entry.user?.let { "${it.username}#${it.discriminator}" } ?: userId
    val reason = entry.ban.reason?.takeIf(String::isNotBlank)
        ?: stringResource(R.string.server_settings_bans_no_reason)
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(enabled = enabled, onClick = onDetails)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        UserAvatar(
            username = username,
            userId = userId,
            avatar = entry.user?.avatar,
            size = 44.dp,
        )
        Spacer(Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = reason,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TextButton(onClick = onUnban, enabled = enabled) {
            Text(stringResource(R.string.server_settings_bans_unban))
        }
    }
}

@Composable
private fun BanDetailsSheet(
    entry: BanListEntry,
    onDone: () -> Unit,
) {
    val userId = entry.ban.id.user
    val username = entry.user?.username ?: userId
    val displayName = entry.user?.let { "${it.username}#${it.discriminator}" } ?: userId
    val reason = entry.ban.reason?.takeIf(String::isNotBlank)
        ?: stringResource(R.string.server_settings_bans_no_reason)

    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.server_settings_bans_details_title),
            style = MaterialTheme.typography.headlineSmall,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            UserAvatar(
                username = username,
                userId = userId,
                avatar = entry.user?.avatar,
                size = 52.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                entry.bannedByName?.let { bannedBy ->
                    Text(
                        text = stringResource(
                            R.string.server_settings_bans_banned_by,
                            bannedBy,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.server_settings_bans_reason),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
            SelectionContainer {
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(16.dp),
                )
            }
        }

        TextButton(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.ok))
        }
    }
}

@Composable
private fun BanLoadError(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 32.dp),
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetry) {
            Text(stringResource(R.string.server_settings_bans_retry))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UnbanDialog(
    entry: BanListEntry,
    unbanning: Boolean,
    error: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val username = entry.user?.let { "${it.username}#${it.discriminator}" }
        ?: entry.ban.id.user

    AlertDialog(
        onDismissRequest = { if (!unbanning) onDismiss() },
        title = {
            Text(stringResource(R.string.server_settings_bans_unban_title, username))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.server_settings_bans_unban_description, username))
                AnimatedVisibility(error != null) {
                    Text(
                        text = error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !unbanning) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !unbanning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                if (unbanning) {
                    LoadingIndicator(
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Text(stringResource(R.string.server_settings_bans_unban))
                }
            }
        },
    )
}
