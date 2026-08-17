package chat.stoat.screens.settings.server

import android.app.Application
import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.internals.hasPermission
import chat.stoat.api.routes.channel.createInvite
import chat.stoat.api.routes.invites.deleteInvite
import chat.stoat.api.routes.invites.fetchServerInvites
import chat.stoat.api.routes.user.fetchUser
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.core.model.data.STOAT_INVITES
import chat.stoat.core.model.schemas.Channel
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.core.model.schemas.Server
import chat.stoat.core.model.schemas.ServerInvite
import chat.stoat.core.model.schemas.User
import chat.stoat.internals.extensions.rememberServerPermissions
import chat.stoat.screens.settings.SettingsListItem
import chat.stoat.settings.dsl.SettingsPage
import chat.stoat.ui.theme.FragmentMono
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.koin.androidx.compose.koinViewModel

internal fun inviteLink(code: String, compact: Boolean = false): String =
    if (compact) "${STOAT_INVITES.toUri().host}/$code"
    else "$STOAT_INVITES/$code"


internal fun availableInviteChannels(
    server: Server?,
    channels: Map<String, Channel>,
): List<Channel> = server
    ?.channels
    .orEmpty()
    .mapNotNull(channels::get)
    .filter { it.channelType == ChannelType.TextChannel }

class ServerSettingsInvitesViewModel(
    private val context: Application,
) : ViewModel() {
    var invites by mutableStateOf<List<ServerInvite>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var creating by mutableStateOf(false)
        private set
    var createdInvite by mutableStateOf<ServerInvite?>(null)
        private set
    var createError by mutableStateOf<String?>(null)
        private set
    var deletingInviteCode by mutableStateOf<String?>(null)
        private set
    var deleteError by mutableStateOf<String?>(null)
        private set
    var createSucceeded by mutableIntStateOf(0)
        private set
    var deleteSucceeded by mutableIntStateOf(0)
        private set

    private var loadedServerId: String? = null
    private var loaded = false

    fun load(serverId: String, force: Boolean = false) {
        if (!force && loadedServerId == serverId && (loaded || loading)) return

        if (loadedServerId != serverId) {
            invites = emptyList()
            loaded = false
        }
        loadedServerId = serverId
        loading = true
        loadError = null

        viewModelScope.launch {
            val result = runCatching { fetchServerInvites(serverId) }
            result
                .onSuccess { fetchedInvites ->
                    invites = fetchedInvites.sortedByDescending(ServerInvite::id)
                    loaded = true
                }
                .onFailure { error ->
                    loadError = error.message
                        ?: context.getString(R.string.server_settings_invites_load_error)
                }

            result.getOrNull()?.let { hydrateMissingCreators(it) }
            loading = false
        }
    }

    fun retryLoad() {
        loadedServerId?.let { load(it, force = true) }
    }

    fun beginCreate() {
        if (creating) return
        createdInvite = null
        createError = null
    }

    fun create(channelId: String) {
        if (creating || deletingInviteCode != null) return

        viewModelScope.launch {
            creating = true
            createdInvite = null
            createError = null

            runCatching { createInvite(channelId) }
                .onSuccess { invite ->
                    invites = listOf(invite) + invites.filterNot { it.id == invite.id }
                    createdInvite = invite
                    createSucceeded++
                }
                .onFailure { error ->
                    createError = error.message
                        ?: context.getString(R.string.server_settings_invites_create_error)
                }

            creating = false
        }
    }

    fun resetCreate() {
        if (creating) return
        createdInvite = null
        createError = null
    }

    fun delete(code: String) {
        if (creating || deletingInviteCode != null) return

        viewModelScope.launch {
            deletingInviteCode = code
            deleteError = null

            runCatching { deleteInvite(code) }
                .onSuccess {
                    invites = invites.filterNot { it.id == code }
                    deleteSucceeded++
                }
                .onFailure { error ->
                    deleteError = error.message
                        ?: context.getString(R.string.server_settings_invites_delete_error)
                }

            deletingInviteCode = null
        }
    }

    fun clearDeleteError() {
        deleteError = null
    }

    private suspend fun hydrateMissingCreators(invites: List<ServerInvite>) {
        val missingCreatorIds = invites
            .map(ServerInvite::creator)
            .distinct()
            .filterNot(StoatAPI.userCache::containsKey)
        val requests = Semaphore(4)

        coroutineScope {
            missingCreatorIds.map { creatorId ->
                async {
                    requests.withPermit {
                        runCatching { fetchUser(creatorId) }
                    }
                }
            }.awaitAll()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ServerSettingsInvites(
    navController: NavController,
    serverId: String,
    viewModel: ServerSettingsInvitesViewModel = koinViewModel(),
) {
    val permissions by rememberServerPermissions(serverId)
    val server = StoatAPI.serverCache[serverId]
    val channels = availableInviteChannels(server, StoatAPI.channelCache)
    val listState = rememberLazyListState()
    var showCreateSheet by remember { mutableStateOf(false) }
    var selectedInvite by remember { mutableStateOf<ServerInvite?>(null) }
    var deleteTarget by remember { mutableStateOf<ServerInvite?>(null) }
    var noChannelsNotice by remember { mutableIntStateOf(0) }

    if (
        permissions != null &&
        permissions?.hasPermission(PermissionBit.ManageServer) == false
    ) {
        LaunchedEffect(serverId) { navController.popBackStack() }
        return
    }

    LaunchedEffect(serverId, permissions) {
        if (permissions?.hasPermission(PermissionBit.ManageServer) == true) {
            viewModel.load(serverId)
        }
    }

    BackHandler(enabled = viewModel.creating || viewModel.deletingInviteCode != null) {}

    if (showCreateSheet) {
        val createSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val createSheetScope = rememberCoroutineScope()

        ModalBottomSheet(
            sheetState = createSheetState,
            onDismissRequest = {
                if (!viewModel.creating) {
                    viewModel.resetCreate()
                    showCreateSheet = false
                }
            },
        ) {
            CreateInviteSheet(
                channels = channels,
                creating = viewModel.creating,
                createdInvite = viewModel.createdInvite,
                error = viewModel.createError,
                onChannelSelected = viewModel::create,
                onDone = {
                    createSheetScope.launch {
                        createSheetState.hide()
                        viewModel.resetCreate()
                        showCreateSheet = false
                    }
                },
            )
        }
    }

    selectedInvite?.let { invite ->
        val managementSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val managementSheetScope = rememberCoroutineScope()

        ModalBottomSheet(
            sheetState = managementSheetState,
            onDismissRequest = { selectedInvite = null },
        ) {
            InviteManagementSheet(
                invite = invite,
                creator = StoatAPI.userCache[invite.creator],
                channel = StoatAPI.channelCache[invite.channel],
                onDelete = {
                    viewModel.clearDeleteError()
                    deleteTarget = invite
                    managementSheetScope.launch {
                        managementSheetState.hide()
                        selectedInvite = null
                    }
                },
            )
        }
    }

    deleteTarget?.let { invite ->
        InviteDeleteDialog(
            invite = invite,
            deleting = viewModel.deletingInviteCode == invite.id,
            error = viewModel.deleteError,
            onDelete = { viewModel.delete(invite.id) },
            onDismiss = {
                if (viewModel.deletingInviteCode == null) {
                    viewModel.clearDeleteError()
                    deleteTarget = null
                }
            },
        )
    }

    SettingsPage(
        navController = navController,
        title = { Text(stringResource(R.string.server_settings_invites)) },
        scrollable = false,
        onNavigateBack = {
            if (!viewModel.creating && viewModel.deletingInviteCode == null) {
                navController.popBackStack()
            }
        },
        floatingActionButton = {
            if (
                server != null &&
                permissions?.hasPermission(PermissionBit.ManageServer) == true
            ) {
                FloatingActionButton(
                    onClick = {
                        if (channels.isEmpty()) {
                            noChannelsNotice++
                        } else {
                            viewModel.beginCreate()
                            showCreateSheet = true
                        }
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_24dp),
                        contentDescription = stringResource(R.string.server_settings_invites_add),
                    )
                }
            }
        },
    ) {
        val deletedMessage = stringResource(R.string.server_settings_invites_deleted)
        val noChannelsMessage = stringResource(R.string.server_settings_invites_no_channels)

        LaunchedEffect(viewModel.createSucceeded) {
            if (viewModel.createSucceeded > 0) listState.animateScrollToItem(0)
        }
        LaunchedEffect(viewModel.deleteSucceeded) {
            if (viewModel.deleteSucceeded > 0) {
                deleteTarget = null
                showSnackbar(deletedMessage)
            }
        }
        LaunchedEffect(noChannelsNotice) {
            if (noChannelsNotice > 0) showSnackbar(noChannelsMessage)
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

        InviteList(
            invites = viewModel.invites,
            listState = listState,
            loading = viewModel.loading,
            loadError = viewModel.loadError,
            enabled = !viewModel.creating && viewModel.deletingInviteCode == null,
            onRetry = viewModel::retryLoad,
            onInviteClick = { selectedInvite = it },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InviteList(
    invites: List<ServerInvite>,
    listState: LazyListState,
    loading: Boolean,
    loadError: String?,
    enabled: Boolean,
    onRetry: () -> Unit,
    onInviteClick: (ServerInvite) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "count") {
            Text(
                text = pluralStringResource(
                    R.plurals.server_settings_invites_count,
                    invites.size,
                    invites.size,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }

        if (loading && invites.isEmpty()) {
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
        } else if (loadError != null && invites.isEmpty()) {
            item(key = "error") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 64.dp),
                ) {
                    Text(
                        text = loadError,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Button(onClick = onRetry) {
                        Text(stringResource(R.string.server_settings_invites_retry))
                    }
                }
            }
        } else if (invites.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.server_settings_invites_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 64.dp),
                )
            }
        } else {
            itemsIndexed(
                items = invites,
                key = { _, invite -> invite.id },
            ) { index, invite ->
                InviteListItem(
                    invite = invite,
                    creator = StoatAPI.userCache[invite.creator],
                    channel = StoatAPI.channelCache[invite.channel],
                    first = index == 0,
                    last = index == invites.lastIndex,
                    enabled = enabled,
                    onClick = { onInviteClick(invite) },
                )
                if (index != invites.lastIndex) Spacer(Modifier.height(2.dp))
            }
        }

        item(key = "bottom-space") { Spacer(Modifier.height(88.dp)) }
    }
}

@Composable
private fun InviteListItem(
    invite: ServerInvite,
    creator: User?,
    channel: Channel?,
    first: Boolean,
    last: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val creatorName = creator?.let(User::resolveDefaultName)
        ?: stringResource(R.string.server_settings_invites_unknown_user)
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
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        UserAvatar(
            username = creatorName,
            userId = invite.creator,
            avatar = creator?.avatar,
            size = 40.dp,
        )
        Spacer(Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = creatorName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = channel?.name?.let { "#$it" }
                    ?: stringResource(R.string.server_settings_invites_unknown_channel),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = invite.id,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .7f),
                fontFamily = FragmentMono,
                fontWeight = FontWeight.Normal,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CreateInviteSheet(
    channels: List<Channel>,
    creating: Boolean,
    createdInvite: ServerInvite?,
    error: String?,
    onChannelSelected: (String) -> Unit,
    onDone: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.server_settings_invites_create_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        )

        AnimatedContent(createdInvite) { invite ->
            if (invite != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                ) {
                    Text(
                        text = stringResource(R.string.server_settings_invites_created_description),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    InviteLink(
                        link = inviteLink(invite.id),
                        copyableLink = inviteLink(invite.id, true)
                    )
                    TextButton(
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.ok))
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                ) {
                    Text(
                        text = stringResource(R.string.server_settings_invites_choose_channel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    AnimatedVisibility(creating) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ContainedLoadingIndicator()
                        }
                    }

                    error?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    itemsIndexed(
                        items = channels,
                        key = { _, channel -> channel.id.orEmpty() },
                    ) { index, channel ->
                        SettingsListItem(
                            first = index == 0,
                            last = index == channels.lastIndex,
                            headlineContent = {
                                Text(
                                    text = channel.name
                                        ?: stringResource(R.string.server_settings_invites_unknown_channel),
                                )
                            },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_tag_24dp),
                                    contentDescription = null,
                                )
                            },
                            modifier = Modifier.clickable(enabled = !creating) {
                                channel.id?.let(onChannelSelected)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InviteManagementSheet(
    invite: ServerInvite,
    creator: User?,
    channel: Channel?,
    onDelete: () -> Unit,
) {
    val creatorName = creator?.let(User::resolveDefaultName)
        ?: stringResource(R.string.server_settings_invites_unknown_user)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
    ) {
        Text(
            text = invite.id,
            fontFamily = FragmentMono,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(
                R.string.server_settings_invites_created_by_in_channel,
                creatorName,
                channel?.name?.let { "#$it" }
                    ?: stringResource(R.string.server_settings_invites_unknown_channel),
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        InviteLink(
            link = inviteLink(invite.id, true),
            copyableLink = inviteLink(invite.id)
        )
        Button(
            onClick = onDelete,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.server_settings_invites_delete))
        }
    }
}

@Composable
private fun InviteLink(link: String, copyableLink: String) {
    val clip = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable {
                coroutineScope.launch {
                    clip.setClipEntry(
                        ClipData.newPlainText(
                            "Stoat invite link",
                            copyableLink
                        ).toClipEntry()
                    )
                }
            }
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(24.dp)
    ) {
        Text(
            text = link,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            painter = painterResource(R.drawable.ic_content_copy_24dp),
            contentDescription = stringResource(R.string.server_settings_invites_copy),
        )
    }
}

@Composable
private fun InviteDeleteDialog(
    invite: ServerInvite,
    deleting: Boolean,
    error: String?,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!deleting) onDismiss() },
        title = { Text(stringResource(R.string.server_settings_invites_delete_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(
                        R.string.server_settings_invites_delete_description,
                        invite.id,
                    )
                )
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !deleting,
            ) {
                Text(stringResource(R.string.server_settings_invites_delete_no_keep))
            }
        },
        confirmButton = {
            Button(
                onClick = onDelete,
                enabled = !deleting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                if (deleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.server_settings_invites_delete_yes_delete))
                }
            }
        },
    )
}
