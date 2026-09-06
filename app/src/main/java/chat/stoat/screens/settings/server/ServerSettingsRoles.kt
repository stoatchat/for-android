package chat.stoat.screens.settings.server

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.routes.server.createServerRole
import chat.stoat.api.routes.server.reorderServerRoles
import chat.stoat.composables.server.RoleColourIndicator
import chat.stoat.composables.settings.ServerSettingsEmptyState
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.schemas.Role
import chat.stoat.internals.extensions.rememberServerPermissions
import chat.stoat.internals.server.canManageServerRole
import chat.stoat.internals.server.serverRoleCapabilities
import chat.stoat.settings.dsl.SettingsPage
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

class ServerSettingsRolesViewModel(
    private val context: Application,
) : ViewModel() {
    var creating by mutableStateOf(false)
        private set
    var reorderingRoleId by mutableStateOf<String?>(null)
        private set
    var createError by mutableStateOf<String?>(null)
        private set
    var reorderError by mutableStateOf<String?>(null)
        private set
    var createdRoleId by mutableStateOf<String?>(null)
        private set
    var reordered by mutableIntStateOf(0)
        private set
    private var pendingUndoOrder: List<String>? = null

    fun beginCreate() {
        createError = null
        createdRoleId = null
    }

    fun create(serverId: String, name: String) {
        if (creating || reorderingRoleId != null || name.trim().length !in 1..32) return
        viewModelScope.launch {
            creating = true
            createError = null
            runCatching { createServerRole(serverId, name.trim()) }
                .onSuccess { createdRoleId = it.id }
                .onFailure {
                    createError = it.message
                        ?: context.getString(R.string.server_settings_roles_create_error)
                }
            creating = false
        }
    }

    fun consumeCreatedRole() {
        createdRoleId = null
    }

    fun reorder(
        serverId: String,
        movedRoleId: String,
        roleIds: List<String>,
        previousRoleIds: List<String>,
    ) {
        if (creating || reorderingRoleId != null) return
        viewModelScope.launch {
            reorderingRoleId = movedRoleId
            reorderError = null
            pendingUndoOrder = null
            runCatching { reorderServerRoles(serverId, roleIds) }
                .onSuccess {
                    pendingUndoOrder = previousRoleIds
                    reordered++
                }
                .onFailure {
                    reorderError = it.message
                        ?: context.getString(R.string.server_settings_roles_reorder_error)
                }
            reorderingRoleId = null
        }
    }

    fun undoLastReorder(serverId: String) {
        val roleIds = pendingUndoOrder ?: return
        if (creating || reorderingRoleId != null) return
        pendingUndoOrder = null

        viewModelScope.launch {
            reorderingRoleId = UNDO_ROLE_ORDER
            reorderError = null
            runCatching { reorderServerRoles(serverId, roleIds) }
                .onFailure {
                    reorderError = it.message
                        ?: context.getString(R.string.server_settings_roles_reorder_error)
                }
            reorderingRoleId = null
        }
    }

    private companion object {
        const val UNDO_ROLE_ORDER =
            "chat.stoat.screens.settings.server.ServerSettingsRolesViewModel.UNDO_ROLE_ORDER"
    }
}

private data class RoleEntry(val id: String, val role: Role)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ServerSettingsRoles(
    navController: NavController,
    serverId: String,
    viewModel: ServerSettingsRolesViewModel = koinViewModel(),
) {
    val permissions by rememberServerPermissions(serverId)
    val server = StoatAPI.serverCache[serverId]
    val capabilities = remember(server, permissions) {
        if (server != null && permissions != null) {
            serverRoleCapabilities(server, permissions!!)
        } else null
    }
    val entries = remember(server?.roles) {
        server?.roles.orEmpty()
            .map { RoleEntry(it.key, it.value) }
            .sortedBy { it.role.rank }
    }
    var displayedEntries by remember(serverId) { mutableStateOf(entries) }
    val lazyListState = rememberLazyListState()
    val hapticFeedback = LocalHapticFeedback.current
    var dragStartOrder by remember(serverId) { mutableStateOf<List<String>>(emptyList()) }
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val currentServer = server ?: return@rememberReorderableLazyListState
        if (capabilities?.canManageRoles != true || viewModel.reorderingRoleId != null) {
            return@rememberReorderableLazyListState
        }

        // The first LazyColumn item is the count header
        val fromIndex = from.index - 1
        val toIndex = (to.index - 1).coerceIn(0, displayedEntries.lastIndex)
        val fromEntry = displayedEntries.getOrNull(fromIndex)
            ?: return@rememberReorderableLazyListState
        val toEntry = displayedEntries.getOrNull(toIndex)
            ?: return@rememberReorderableLazyListState

        if (
            fromIndex == toIndex ||
            !canManageServerRole(currentServer, fromEntry.role) ||
            !canManageServerRole(currentServer, toEntry.role)
        ) return@rememberReorderableLazyListState

        displayedEntries = displayedEntries.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }
    var showCreateDialog by remember { mutableStateOf(false) }
    var roleName by remember { mutableStateOf("") }

    LaunchedEffect(entries, viewModel.reorderingRoleId) {
        if (viewModel.reorderingRoleId == null) displayedEntries = entries
    }

    if (capabilities?.canOpenRoles == false) {
        LaunchedEffect(serverId) { navController.popBackStack() }
        return
    }

    BackHandler(enabled = viewModel.creating || viewModel.reorderingRoleId != null) {}

    LaunchedEffect(viewModel.createdRoleId) {
        viewModel.createdRoleId?.let { roleId ->
            showCreateDialog = false
            viewModel.consumeCreatedRole()
            navController.navigate("settings/server/$serverId/roles/$roleId")
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!viewModel.creating) showCreateDialog = false
            },
            title = { Text(stringResource(R.string.server_settings_roles_create)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = roleName,
                        onValueChange = { if (it.length <= 32) roleName = it },
                        label = { Text(stringResource(R.string.server_settings_roles_name)) },
                        supportingText = { Text("${roleName.length}/32") },
                        singleLine = true,
                        enabled = !viewModel.creating,
                    )
                    AnimatedVisibility(viewModel.createError != null) {
                        Text(
                            text = viewModel.createError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !viewModel.creating,
                    onClick = { showCreateDialog = false },
                ) { Text(stringResource(R.string.cancel)) }
            },
            confirmButton = {
                Button(
                    enabled = roleName.trim().length in 1..32 && !viewModel.creating,
                    onClick = { viewModel.create(serverId, roleName) },
                ) {
                    if (viewModel.creating) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.server_settings_roles_create_action))
                    }
                }
            },
        )
    }

    SettingsPage(
        navController = navController,
        title = { Text(stringResource(R.string.server_settings_roles)) },
        scrollable = false,
        onNavigateBack = {
            if (!viewModel.creating && viewModel.reorderingRoleId == null) navController.popBackStack()
        },
        floatingActionButton = {
            if (capabilities?.canManageRoles == true) {
                FloatingActionButton(
                    onClick = {
                        roleName = ""
                        viewModel.beginCreate()
                        showCreateDialog = true
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_24dp),
                        contentDescription = stringResource(R.string.server_settings_roles_create),
                    )
                }
            }
        },
    ) {
        val reorderedMessage = stringResource(R.string.server_settings_roles_reordered)
        val undoLabel = stringResource(R.string.server_settings_roles_undo)
        LaunchedEffect(viewModel.reordered) {
            if (viewModel.reordered > 0) {
                showSnackbar(
                    message = reorderedMessage,
                    actionLabel = undoLabel,
                    onAction = { viewModel.undoLastReorder(serverId) },
                )
            }
        }

        if (server == null || permissions == null || capabilities == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
            return@SettingsPage
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
        ) {
            item(key = "role-count") {
                Text(
                    text = pluralStringResource(
                        R.plurals.server_settings_roles_count,
                        displayedEntries.size,
                        displayedEntries.size,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            if (displayedEntries.isEmpty()) {
                item(key = "empty") {
                    ServerSettingsEmptyState(
                        icon = R.drawable.ic_flag_24dp,
                        title = R.string.server_settings_roles_empty_title,
                        description = R.string.server_settings_roles_empty_description,
                    )
                }
            }

            itemsIndexed(displayedEntries, key = { _, entry -> entry.id }) { index, entry ->
                val elevated = canManageServerRole(server, entry.role)
                ReorderableItem(
                    state = reorderableState,
                    key = entry.id,
                    enabled = capabilities.canManageRoles && elevated,
                ) { isDragging ->
                    val canDrag = capabilities.canManageRoles && elevated &&
                            viewModel.reorderingRoleId == null
                    val canMoveUp = canDrag && index > 0 &&
                            canManageServerRole(server, displayedEntries[index - 1].role)
                    val canMoveDown = canDrag && index < displayedEntries.lastIndex &&
                            canManageServerRole(server, displayedEntries[index + 1].role)
                    val dragHandleModifier = if (canDrag) {
                        Modifier.draggableHandle(
                            onDragStarted = {
                                dragStartOrder = displayedEntries.map(RoleEntry::id)
                                hapticFeedback.performHapticFeedback(
                                    HapticFeedbackType.GestureThresholdActivate
                                )
                            },
                            onDragStopped = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                val settledOrder = displayedEntries.map(RoleEntry::id)
                                if (settledOrder != dragStartOrder) {
                                    viewModel.reorder(
                                        serverId = serverId,
                                        movedRoleId = entry.id,
                                        roleIds = settledOrder,
                                        previousRoleIds = dragStartOrder,
                                    )
                                }
                                dragStartOrder = emptyList()
                            },
                        )
                    } else {
                        Modifier
                    }

                    RoleListRow(
                        entry = entry,
                        first = index == 0,
                        last = false,
                        enabled = capabilities.canOpenRoles &&
                                viewModel.reorderingRoleId == null,
                        lockedByHierarchy = !elevated,
                        dragHandle = if (capabilities.canManageRoles) {
                            {
                                IconButton(
                                    enabled = canDrag,
                                    modifier = dragHandleModifier.clearAndSetSemantics {},
                                    onClick = {},
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            R.drawable.ic_drag_handle_24dp
                                        ),
                                        contentDescription = stringResource(
                                            R.string.server_settings_roles_reorder
                                        ),
                                    )
                                }
                            }
                        } else null,
                        moving = viewModel.reorderingRoleId == entry.id,
                        isDragging = isDragging,
                        canMoveUp = canMoveUp,
                        canMoveDown = canMoveDown,
                        onClick = {
                            navController.navigate("settings/server/$serverId/roles/${entry.id}")
                        },
                        onMoveUp = {
                            val previousOrder = displayedEntries.map(RoleEntry::id)
                            val reordered = displayedEntries.toMutableList().apply {
                                add(index - 1, removeAt(index))
                            }
                            displayedEntries = reordered
                            viewModel.reorder(
                                serverId,
                                entry.id,
                                reordered.map(RoleEntry::id),
                                previousOrder,
                            )
                        },
                        onMoveDown = {
                            val previousOrder = displayedEntries.map(RoleEntry::id)
                            val reordered = displayedEntries.toMutableList().apply {
                                add(index + 1, removeAt(index))
                            }
                            displayedEntries = reordered
                            viewModel.reorder(
                                serverId,
                                entry.id,
                                reordered.map(RoleEntry::id),
                                previousOrder,
                            )
                        },
                    )
                }
            }

            item(key = "everyone") {
                val shape = if (displayedEntries.isEmpty()) {
                    MaterialTheme.shapes.large
                } else {
                    MaterialTheme.shapes.extraSmall.copy(
                        bottomStart = MaterialTheme.shapes.large.bottomStart,
                        bottomEnd = MaterialTheme.shapes.large.bottomEnd,
                    )
                }
                Surface(
                    onClick = {
                        navController.navigate("settings/server/$serverId/roles/default")
                    },
                    enabled = capabilities.canManagePermissions &&
                            viewModel.reorderingRoleId == null,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = shape,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Box {
                        ListItem(
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                            headlineContent = {
                                Text(stringResource(R.string.server_settings_roles_everyone))
                            },
                            supportingContent = {
                                Text(
                                    stringResource(
                                        R.string.server_settings_roles_everyone_description
                                    )
                                )
                            },
                            leadingContent = { Spacer(Modifier.size(24.dp)) },
                            trailingContent = { Spacer(Modifier.size(48.dp)) },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 16.dp)
                                .size(24.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_group_24dp),
                                contentDescription = null,
                            )
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 16.dp)
                                .size(48.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_chevron_forward_24dp),
                                contentDescription = null,
                            )
                        }
                    }
                }
            }

            viewModel.reorderError?.let { error ->
                item(key = "reorder-error") {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    )
                }
            }

            item(key = "bottom-space") { Spacer(Modifier.height(88.dp)) }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun RoleListRow(
    entry: RoleEntry,
    first: Boolean,
    last: Boolean,
    enabled: Boolean,
    lockedByHierarchy: Boolean,
    dragHandle: (@Composable () -> Unit)?,
    moving: Boolean,
    isDragging: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val roleIcon = entry.role.icon
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
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        label = "role drag elevation",
    )
    val moveUpLabel = stringResource(R.string.server_settings_roles_move_up)
    val moveDownLabel = stringResource(R.string.server_settings_roles_move_down)
    ListItem(
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.role.name ?: entry.id,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (roleIcon != null) {
                    GlideImage(
                        model = "$STOAT_FILES/icons/${roleIcon.id}/original",
                        contentDescription = null,
                        transition = CrossFade,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(16.dp),
                    )
                }
            }
        },
        supportingContent = if (lockedByHierarchy) {
            { Text(stringResource(R.string.server_settings_roles_above_you)) }
        } else null,
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
        trailingContent = {
            if (moving) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            } else if (dragHandle != null) {
                dragHandle()
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_forward_24dp),
                    contentDescription = null,
                )
            }
        },
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 2.dp)
            .zIndex(if (isDragging) 1f else 0f)
            .shadow(elevation, shape)
            .clip(shape)
            .semantics {
                customActions = buildList {
                    if (canMoveUp) {
                        add(CustomAccessibilityAction(moveUpLabel) {
                            onMoveUp()
                            true
                        })
                    }
                    if (canMoveDown) {
                        add(CustomAccessibilityAction(moveDownLabel) {
                            onMoveDown()
                            true
                        })
                    }
                }
            }
            .clickable(enabled = enabled, onClick = onClick),
    )
}
