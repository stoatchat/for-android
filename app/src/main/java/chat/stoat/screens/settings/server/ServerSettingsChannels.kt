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
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
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
import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.internals.ULID
import chat.stoat.api.internals.hasPermission
import chat.stoat.api.routes.server.createServerChannel
import chat.stoat.api.routes.server.patchServer
import chat.stoat.composables.screens.chat.ChannelIcon
import chat.stoat.composables.settings.ServerSettingsEmptyState
import chat.stoat.core.model.schemas.Channel
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.internals.extensions.rememberServerPermissions
import chat.stoat.internals.server.ServerChannelListEntry
import chat.stoat.internals.server.ServerChannelSection
import chat.stoat.internals.server.UncategorisedChannelSectionId
import chat.stoat.internals.server.flattenChannelSections
import chat.stoat.internals.server.moveServerChannelEntry
import chat.stoat.internals.server.serverChannelSections
import chat.stoat.internals.server.toServerCategories
import chat.stoat.screens.settings.SettingsIcon
import chat.stoat.screens.settings.SettingsListItem
import chat.stoat.settings.dsl.SettingsPage
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private data class PendingLayoutRequest(
    val sections: List<ServerChannelSection>,
    val undoSections: List<ServerChannelSection>?
)

class ServerSettingsChannelsViewModel(private val context: Application) : ViewModel() {
    var mutating by mutableStateOf(false)
        private set
    var savingLayout by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var createdChannelId by mutableStateOf<String?>(null)
        private set
    var mutationSucceeded by mutableIntStateOf(0)
        private set
    var layoutSaved by mutableIntStateOf(0)
        private set
    var layoutSaveFailed by mutableIntStateOf(0)
        private set
    var layoutError by mutableStateOf<String?>(null)
        private set
    private var pendingUndoLayout: List<ServerChannelSection>? = null
    private var pendingRetryRequest: PendingLayoutRequest? = null
    private var consumedLayoutSaved = 0

    val busy: Boolean
        get() = mutating || savingLayout

    fun beginMutation() {
        error = null
    }

    fun createChannel(serverId: String, name: String, channelType: ChannelType, sectionId: String) {
        if (busy || name.trim().length !in 1..32) return
        viewModelScope.launch {
            mutating = true
            error = null
            runCatching {
                val channel = createServerChannel(serverId, name.trim(), channelType)
                val channelId = requireNotNull(channel.id)
                val server = requireNotNull(StoatAPI.serverCache[serverId])
                val sections = serverChannelSections(server).map { section ->
                    section.copy(
                        channelIds = section.channelIds.filterNot { it == channelId } +
                                if (section.id == sectionId) listOf(channelId) else emptyList()
                    )
                }
                patchServer(serverId, categories = sections.toServerCategories())
                channelId
            }.onSuccess { channelId ->
                createdChannelId = channelId
                mutationSucceeded++
            }.onFailure {
                error = it.message
                    ?: context.getString(R.string.server_settings_channels_create_error)
            }
            mutating = false
        }
    }

    fun createCategory(serverId: String, name: String) {
        if (busy || name.trim().length !in 1..32) return
        mutate {
            val server = requireNotNull(StoatAPI.serverCache[serverId])
            val sections = serverChannelSections(server) + ServerChannelSection(
                id = ULID.makeNext(),
                title = name.trim(),
                channelIds = emptyList()
            )
            patchServer(serverId, categories = sections.toServerCategories())
        }
    }

    fun renameCategory(serverId: String, categoryId: String, name: String) {
        if (busy || name.trim().length !in 1..32) return
        mutate {
            val server = requireNotNull(StoatAPI.serverCache[serverId])
            val sections = serverChannelSections(server).map { section ->
                if (section.id == categoryId) section.copy(title = name.trim()) else section
            }
            patchServer(serverId, categories = sections.toServerCategories())
        }
    }

    fun deleteCategory(serverId: String, categoryId: String) {
        if (busy || categoryId == UncategorisedChannelSectionId) return
        mutate {
            val server = requireNotNull(StoatAPI.serverCache[serverId])
            val sections = serverChannelSections(server)
            val removed = sections.first { it.id == categoryId }
            val updated = sections
                .filterNot { it.id == categoryId }
                .map { section ->
                    if (section.id == UncategorisedChannelSectionId) {
                        section.copy(channelIds = section.channelIds + removed.channelIds)
                    } else {
                        section
                    }
                }
            patchServer(serverId, categories = updated.toServerCategories())
        }
    }

    fun saveLayout(
        serverId: String,
        sections: List<ServerChannelSection>,
        previousSections: List<ServerChannelSection>
    ) {
        submitLayoutRequest(
            serverId = serverId,
            request = PendingLayoutRequest(sections, previousSections)
        )
    }

    fun undoLastLayoutChange(serverId: String) {
        val sections = pendingUndoLayout ?: return
        if (busy) return
        pendingUndoLayout = null

        submitLayoutRequest(
            serverId = serverId,
            request = PendingLayoutRequest(sections, undoSections = null)
        )
    }

    fun retryLastLayoutRequest(serverId: String) {
        val request = pendingRetryRequest ?: return
        if (busy) return
        pendingRetryRequest = null
        submitLayoutRequest(serverId, request)
    }

    private fun submitLayoutRequest(serverId: String, request: PendingLayoutRequest) {
        if (busy) return
        viewModelScope.launch {
            savingLayout = true
            error = null
            layoutError = null
            pendingUndoLayout = null
            pendingRetryRequest = null
            runCatching {
                patchServer(serverId, categories = request.sections.toServerCategories())
            }
                .onSuccess {
                    request.undoSections?.let { previousSections ->
                        pendingUndoLayout = previousSections
                        layoutSaved++
                    }
                }
                .onFailure {
                    layoutError = it.message
                        ?: context.getString(R.string.server_settings_channels_reorder_error)
                    pendingRetryRequest = request
                    layoutSaveFailed++
                }
            savingLayout = false
        }
    }

    fun consumeCreatedChannel() {
        createdChannelId = null
    }

    fun consumeLayoutSaved(): Boolean {
        if (consumedLayoutSaved == layoutSaved) return false
        consumedLayoutSaved = layoutSaved
        return true
    }

    private fun mutate(block: suspend () -> Unit) {
        viewModelScope.launch {
            mutating = true
            error = null
            runCatching { block() }
                .onSuccess { mutationSucceeded++ }
                .onFailure {
                    error = it.message
                        ?: context.getString(R.string.server_settings_channels_update_error)
                }
            mutating = false
        }
    }
}

private enum class ChannelDialogType {
    Text,
    Voice
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ServerSettingsChannels(
    navController: NavController,
    serverId: String,
    viewModel: ServerSettingsChannelsViewModel = koinViewModel()
) {
    val server = StoatAPI.serverCache[serverId]
    val permissions by rememberServerPermissions(serverId)
    val canManage = permissions?.hasPermission(PermissionBit.ManageChannel) == true
    val sourceSections = remember(server?.channels, server?.categories) {
        server?.let(::serverChannelSections).orEmpty()
    }
    var displayedSections by remember(serverId) { mutableStateOf(sourceSections) }
    val entries = remember(displayedSections) { displayedSections.flattenChannelSections() }
    val lazyListState = rememberLazyListState()
    val hapticFeedback = LocalHapticFeedback.current
    var draggingSection by remember(serverId) { mutableStateOf(false) }
    var dragStartSections by remember(serverId) {
        mutableStateOf<List<ServerChannelSection>?>(null)
    }
    val channelReorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (!canManage || viewModel.busy) return@rememberReorderableLazyListState
        val fromIndex = entries.indexOfFirst { it.key == from.key }
        val toIndex = entries.indexOfFirst { it.key == to.key }
        val targetEntry = entries.getOrNull(toIndex)
        if (
            fromIndex < 0 ||
            toIndex < 0 ||
            entries[fromIndex] !is ServerChannelListEntry.Channel ||
            targetEntry is ServerChannelListEntry.Section &&
            targetEntry.sectionId == UncategorisedChannelSectionId
        ) {
            return@rememberReorderableLazyListState
        }

        val moved = moveServerChannelEntry(displayedSections, fromIndex, toIndex)
        if (moved != displayedSections) {
            displayedSections = moved
            hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }
    }
    val sectionReorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (!canManage || viewModel.busy) return@rememberReorderableLazyListState
        val fromEntry = entries.firstOrNull { it.key == from.key }
                as? ServerChannelListEntry.Section
            ?: return@rememberReorderableLazyListState
        val toEntry = entries.firstOrNull { it.key == to.key }
                as? ServerChannelListEntry.Section
            ?: return@rememberReorderableLazyListState
        val fromIndex = displayedSections.indexOfFirst { it.id == fromEntry.sectionId }
        val toIndex = displayedSections.indexOfFirst { it.id == toEntry.sectionId }
        if (
            fromIndex < 1 ||
            toIndex < 1 ||
            fromIndex == toIndex
        ) {
            return@rememberReorderableLazyListState
        }

        displayedSections = displayedSections.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    var showAddSheet by remember { mutableStateOf(false) }
    var showCreateChannel by remember { mutableStateOf(false) }
    var showCreateCategory by remember { mutableStateOf(false) }
    var channelName by remember { mutableStateOf("") }
    var categoryName by remember { mutableStateOf("") }
    var channelType by remember { mutableStateOf(ChannelDialogType.Text) }
    var selectedSectionId by remember { mutableStateOf(UncategorisedChannelSectionId) }
    var categoryToRename by remember { mutableStateOf<ServerChannelSection?>(null) }
    var categoryToDelete by remember { mutableStateOf<ServerChannelSection?>(null) }

    LaunchedEffect(sourceSections, viewModel.busy) {
        if (!viewModel.busy) displayedSections = sourceSections
    }

    if (permissions != null && !canManage) {
        LaunchedEffect(serverId) { navController.popBackStack() }
        return
    }

    BackHandler(enabled = viewModel.busy) {}

    LaunchedEffect(viewModel.createdChannelId) {
        viewModel.createdChannelId?.let { channelId ->
            showCreateChannel = false
            showAddSheet = false
            viewModel.consumeCreatedChannel()
            navController.navigate("settings/channel/$channelId")
        }
    }

    LaunchedEffect(viewModel.mutationSucceeded) {
        if (viewModel.mutationSucceeded > 0 && viewModel.createdChannelId == null) {
            showCreateCategory = false
            categoryToRename = null
            categoryToDelete = null
        }
    }

    fun applyAccessibleMove(movedSections: List<ServerChannelSection>) {
        if (viewModel.busy || movedSections == displayedSections) return
        val previousSections = displayedSections
        displayedSections = movedSections
        viewModel.saveLayout(serverId, movedSections, previousSections)
    }

    fun startDrag(section: Boolean) {
        dragStartSections = displayedSections
        draggingSection = section
        hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
    }

    fun stopDrag() {
        val previousSections = dragStartSections ?: return
        hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
        if (displayedSections != previousSections) {
            viewModel.saveLayout(serverId, displayedSections, previousSections)
        }
        dragStartSections = null
        draggingSection = false
    }

    if (showAddSheet) {
        val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val addSheetScope = rememberCoroutineScope()

        fun dismissAddSheet(onDismissed: () -> Unit) {
            addSheetScope.launch {
                addSheetState.hide()
                showAddSheet = false
                onDismissed()
            }
        }

        ModalBottomSheet(
            sheetState = addSheetState,
            onDismissRequest = { showAddSheet = false }
        ) {
            Text(
                text = stringResource(R.string.server_settings_channels_add_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            SettingsListItem(
                first = true,
                headlineContent = {
                    Text(stringResource(R.string.server_settings_channels_text_channel))
                },
                supportingContent = {
                    Text(stringResource(R.string.server_settings_channels_text_description))
                },
                leadingContent = {
                    SettingsIcon {
                        Icon(painterResource(R.drawable.ic_tag_24dp), contentDescription = null)
                    }
                },
                modifier = Modifier.clickable {
                    dismissAddSheet {
                        channelName = ""
                        channelType = ChannelDialogType.Text
                        viewModel.beginMutation()
                        showCreateChannel = true
                    }
                }
            )
            Spacer(Modifier.height(2.dp))
            SettingsListItem(
                headlineContent = {
                    Text(stringResource(R.string.server_settings_channels_voice_channel))
                },
                supportingContent = {
                    Text(stringResource(R.string.server_settings_channels_voice_description))
                },
                last = selectedSectionId != UncategorisedChannelSectionId,
                leadingContent = {
                    SettingsIcon {
                        Icon(
                            painterResource(R.drawable.ic_volume_up_24dp),
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier.clickable {
                    dismissAddSheet {
                        channelName = ""
                        channelType = ChannelDialogType.Voice
                        viewModel.beginMutation()
                        showCreateChannel = true
                    }
                }
            )

            if (selectedSectionId == UncategorisedChannelSectionId) {
                Spacer(Modifier.height(2.dp))
                SettingsListItem(
                    last = true,
                    headlineContent = {
                        Text(stringResource(R.string.server_settings_channels_category))
                    },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.server_settings_channels_create_category_description
                            )
                        )
                    },
                    leadingContent = {
                        SettingsIcon {
                            Icon(
                                painterResource(R.drawable.ic_list_24dp),
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.clickable {
                        dismissAddSheet {
                            categoryName = ""
                            viewModel.beginMutation()
                            showCreateCategory = true
                        }
                    }
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showCreateChannel) {
        CreateChannelDialog(
            name = channelName,
            onNameChange = { if (it.length <= 32) channelName = it },
            type = channelType,
            sections = displayedSections,
            selectedSectionId = selectedSectionId,
            onSectionSelected = { selectedSectionId = it },
            busy = viewModel.mutating,
            error = viewModel.error,
            onDismiss = { showCreateChannel = false },
            onCreate = {
                viewModel.createChannel(
                    serverId = serverId,
                    name = channelName,
                    channelType = if (channelType == ChannelDialogType.Voice) {
                        ChannelType.VoiceChannel
                    } else {
                        ChannelType.TextChannel
                    },
                    sectionId = selectedSectionId
                )
            }
        )
    }

    if (showCreateCategory) {
        CategoryNameDialog(
            title = stringResource(R.string.server_settings_channels_create_category),
            action = stringResource(R.string.server_settings_channels_create),
            name = categoryName,
            onNameChange = { if (it.length <= 32) categoryName = it },
            busy = viewModel.mutating,
            error = viewModel.error,
            onDismiss = { showCreateCategory = false },
            onConfirm = { viewModel.createCategory(serverId, categoryName) }
        )
    }

    categoryToRename?.let { category ->
        CategoryNameDialog(
            title = stringResource(R.string.server_settings_channels_rename_category),
            action = stringResource(R.string.server_settings_channels_rename),
            name = categoryName,
            onNameChange = { if (it.length <= 32) categoryName = it },
            busy = viewModel.mutating,
            error = viewModel.error,
            onDismiss = { categoryToRename = null },
            onConfirm = { viewModel.renameCategory(serverId, category.id, categoryName) }
        )
    }

    categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { if (!viewModel.mutating) categoryToDelete = null },
            title = {
                Text(
                    stringResource(
                        R.string.server_settings_channels_delete_category_title,
                        category.title.orEmpty()
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(
                            R.string.server_settings_channels_delete_category_description
                        )
                    )
                    DialogError(viewModel.error)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !viewModel.mutating,
                    onClick = { categoryToDelete = null }
                ) { Text(stringResource(R.string.cancel)) }
            },
            confirmButton = {
                Button(
                    enabled = !viewModel.mutating,
                    onClick = { viewModel.deleteCategory(serverId, category.id) },
                    colors = ButtonDefaults.buttonColors().copy(
                        contentColor = MaterialTheme.colorScheme.onError,
                        containerColor = MaterialTheme.colorScheme.error,
                        disabledContentColor = MaterialTheme.colorScheme.onError.copy(
                            alpha = 0.38f
                        ),
                        disabledContainerColor = MaterialTheme.colorScheme.error.copy(
                            alpha = 0.12f
                        )
                    )
                ) {
                    if (viewModel.mutating) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.server_settings_channels_delete))
                    }
                }
            }
        )
    }

    SettingsPage(
        navController = navController,
        title = { Text(stringResource(R.string.server_settings_channels)) },
        scrollable = false,
        onNavigateBack = { if (!viewModel.busy) navController.popBackStack() },
        floatingActionButton = {
            if (canManage) {
                FloatingActionButton(
                    onClick = {
                        selectedSectionId = UncategorisedChannelSectionId
                        channelName = ""
                        channelType = ChannelDialogType.Text
                        viewModel.beginMutation()
                        showAddSheet = true
                    }
                ) {
                    Icon(
                        painterResource(R.drawable.ic_add_24dp),
                        contentDescription = stringResource(
                            R.string.server_settings_channels_add_title
                        )
                    )
                }
            }
        }
    ) {
        val savedMessage = stringResource(R.string.server_settings_channels_reordered)
        val undoLabel = stringResource(R.string.server_settings_channels_undo)
        val reorderErrorMessage = stringResource(R.string.server_settings_channels_reorder_error)
        val retryLabel = stringResource(R.string.retry)
        LaunchedEffect(viewModel.layoutSaved) {
            if (viewModel.consumeLayoutSaved()) {
                showSnackbar(
                    message = savedMessage,
                    actionLabel = undoLabel,
                    onAction = { viewModel.undoLastLayoutChange(serverId) },
                    dismissCurrent = true
                )
            }
        }
        LaunchedEffect(viewModel.layoutSaveFailed) {
            if (viewModel.layoutSaveFailed > 0) {
                showSnackbar(
                    message = viewModel.layoutError ?: reorderErrorMessage,
                    actionLabel = retryLabel,
                    onAction = { viewModel.retryLastLayoutRequest(serverId) },
                    dismissCurrent = true
                )
            }
        }

        if (server == null || permissions == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
            return@SettingsPage
        }

        val hasChannelLayoutItems = displayedSections.any { section ->
            section.id != UncategorisedChannelSectionId || section.channelIds.isNotEmpty()
        }
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "channel-summary") {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.server_settings_channels_count,
                            server.channels.orEmpty().size,
                            server.channels.orEmpty().size
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    AnimatedVisibility(viewModel.savingLayout) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        )
                    }
                }
            }

            if (!hasChannelLayoutItems) {
                item(key = "empty") {
                    ServerSettingsEmptyState(
                        icon = R.drawable.ic_grid_3x3_24dp,
                        title = R.string.server_settings_channels_empty_title,
                        description = R.string.server_settings_channels_empty_description,
                    )
                }
            }
            itemsIndexed(
                items = if (hasChannelLayoutItems) entries else emptyList(),
                key = { _, entry -> entry.key },
            ) { index, entry ->
                val lazyItemScope = this
                ReorderableItem(
                    state = when (entry) {
                        is ServerChannelListEntry.Section -> sectionReorderableState
                        is ServerChannelListEntry.Channel -> channelReorderableState
                    },
                    key = entry.key,
                    enabled = !viewModel.busy &&
                            (entry !is ServerChannelListEntry.Section ||
                                    entry.sectionId != UncategorisedChannelSectionId)
                ) { isDragging ->
                    val dragHandleModifier = Modifier.draggableHandle(
                        onDragStarted = {
                            startDrag(section = entry is ServerChannelListEntry.Section)
                        },
                        onDragStopped = { stopDrag() }
                    )

                    when (entry) {
                        is ServerChannelListEntry.Section -> {
                            val section = displayedSections.first { it.id == entry.sectionId }
                            val sectionIndex = displayedSections.indexOf(section)
                            val moveUp = displayedSections.getOrNull(sectionIndex - 1)
                                ?.takeIf { sectionIndex > 1 }
                                ?.let { target ->
                                    val targetIndex = entries.indexOfFirst {
                                        it is ServerChannelListEntry.Section &&
                                                it.sectionId == target.id
                                    }
                                    moveServerChannelEntry(
                                        displayedSections,
                                        index,
                                        targetIndex
                                    )
                                }
                            val moveDown = displayedSections.getOrNull(sectionIndex + 1)
                                ?.let { target ->
                                    val targetIndex = entries.indexOfFirst {
                                        it is ServerChannelListEntry.Section &&
                                                it.sectionId == target.id
                                    }
                                    moveServerChannelEntry(
                                        displayedSections,
                                        index,
                                        targetIndex
                                    )
                                }
                            with(lazyItemScope) {
                                ReorderableItem(
                                    state = channelReorderableState,
                                    key = entry.key,
                                    enabled = !viewModel.busy && entry.sectionId != UncategorisedChannelSectionId,
                                    animateItemModifier = Modifier
                                ) {
                                    Column {
                                        ChannelSectionRow(
                                            section = section,
                                            isDragging = isDragging,
                                            canDrag = section.id !=
                                                    UncategorisedChannelSectionId,
                                            enabled = !viewModel.busy,
                                            dragHandle = {
                                                ReorderDragHandle(
                                                    enabled = !viewModel.busy,
                                                    contentDescription = stringResource(
                                                        R.string
                                                            .server_settings_channels_reorder_category
                                                    ),
                                                    modifier = dragHandleModifier
                                                )
                                            },
                                            onCreateChannel = {
                                                selectedSectionId = section.id
                                                channelName = ""
                                                channelType = ChannelDialogType.Text
                                                viewModel.beginMutation()
                                                showAddSheet = true
                                            },
                                            onRename = if (section.id ==
                                                UncategorisedChannelSectionId
                                            ) {
                                                null
                                            } else {
                                                {
                                                    categoryName = section.title.orEmpty()
                                                    viewModel.beginMutation()
                                                    categoryToRename = section
                                                }
                                            },
                                            onDelete = if (section.id ==
                                                UncategorisedChannelSectionId
                                            ) {
                                                null
                                            } else {
                                                {
                                                    viewModel.beginMutation()
                                                    categoryToDelete = section
                                                }
                                            },
                                            onMoveUp = moveUp
                                                ?.takeIf { it != displayedSections }
                                                ?.let { { applyAccessibleMove(it) } },
                                            onMoveDown = moveDown
                                                ?.takeIf { it != displayedSections }
                                                ?.let { { applyAccessibleMove(it) } }
                                        )

                                        if (draggingSection) {
                                            section.channelIds.forEachIndexed { channelIndex,
                                                                                channelId ->
                                                ChannelSettingsRow(
                                                    channel = StoatAPI.channelCache[channelId],
                                                    channelId = channelId,
                                                    isDragging = false,
                                                    enabled = false,
                                                    dragHandle = {
                                                        ReorderDragHandle(
                                                            enabled = false,
                                                            contentDescription = stringResource(
                                                                R.string
                                                                    .server_settings_channels_reorder_channel
                                                            )
                                                        )
                                                    },
                                                    last = channelIndex ==
                                                            section.channelIds.lastIndex,
                                                    onClick = {},
                                                    onMoveUp = null,
                                                    onMoveDown = null
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        is ServerChannelListEntry.Channel -> {
                            if (draggingSection) {
                                // the same row is rendered inside its section item above
                                Box {}
                                return@ReorderableItem
                            }
                            val channel = StoatAPI.channelCache[entry.channelId]
                            val moveUp = if (index > 0) {
                                moveServerChannelEntry(displayedSections, index, index - 1)
                            } else {
                                displayedSections
                            }
                            val moveDown = if (index < entries.lastIndex) {
                                moveServerChannelEntry(displayedSections, index, index + 1)
                            } else {
                                displayedSections
                            }
                            ChannelSettingsRow(
                                channel = channel,
                                channelId = entry.channelId,
                                isDragging = isDragging,
                                enabled = !viewModel.busy,
                                dragHandle = {
                                    ReorderDragHandle(
                                        enabled = !viewModel.busy,
                                        contentDescription = stringResource(
                                            R.string.server_settings_channels_reorder_channel
                                        ),
                                        modifier = dragHandleModifier
                                    )
                                },
                                last = displayedSections
                                    .first { it.id == entry.sectionId }
                                    .channelIds.lastOrNull() == entry.channelId,
                                onClick = {
                                    navController.navigate(
                                        "settings/channel/${entry.channelId}"
                                    )
                                },
                                onMoveUp = moveUp
                                    .takeIf { it != displayedSections }
                                    ?.let { { applyAccessibleMove(it) } },
                                onMoveDown = moveDown
                                    .takeIf { it != displayedSections }
                                    ?.let { { applyAccessibleMove(it) } }
                            )
                        }
                    }
                }
            }

            viewModel.error?.takeIf {
                !showCreateChannel && !showCreateCategory &&
                        categoryToRename == null && categoryToDelete == null
            }?.let { error ->
                item(key = "channel-error") {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }

            item(key = "channel-bottom-space") { Spacer(Modifier.height(96.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CreateChannelDialog(
    name: String,
    onNameChange: (String) -> Unit,
    type: ChannelDialogType,
    sections: List<ServerChannelSection>,
    selectedSectionId: String,
    onSectionSelected: (String) -> Unit,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onCreate: () -> Unit
) {
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    val selectedSection = sections.firstOrNull { it.id == selectedSectionId } ?: sections.first()
    val isText = type == ChannelDialogType.Text
    BasicAlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
    ) {
        Surface(
            shape = AlertDialogDefaults.shape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = stringResource(
                        if (isText) {
                            R.string.server_settings_channels_new_text_channel
                        } else {
                            R.string.server_settings_channels_new_voice_channel
                        }
                    ),
                    style = MaterialTheme.typography.headlineSmall
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = {
                        Text(stringResource(R.string.server_settings_channels_channel_name))
                    },
                    supportingText = { Text("${name.length}/32") },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = categoryMenuExpanded,
                    onExpandedChange = { if (!busy) categoryMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = sectionTitle(selectedSection),
                        onValueChange = {},
                        readOnly = true,
                        enabled = !busy,
                        label = {
                            Text(stringResource(R.string.server_settings_channels_category))
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(categoryMenuExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, !busy)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false }
                    ) {
                        sections.forEach { section ->
                            DropdownMenuItem(
                                text = { Text(sectionTitle(section)) },
                                onClick = {
                                    onSectionSelected(section.id)
                                    categoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                DialogError(error)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(enabled = !busy, onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        enabled = !busy && name.trim().length in 1..32,
                        onClick = onCreate
                    ) {
                        if (busy) {
                            LoadingIndicator(Modifier.size(22.dp))
                        } else {
                            Text(stringResource(R.string.server_settings_channels_create))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryNameDialog(
    title: String,
    action: String,
    name: String,
    onNameChange: (String) -> Unit,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = {
                        Text(stringResource(R.string.server_settings_channels_category_name))
                    },
                    supportingText = { Text("${name.length}/32") },
                    singleLine = true,
                    enabled = !busy
                )
                DialogError(error)
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            Button(
                enabled = !busy && name.trim().length in 1..32,
                onClick = onConfirm
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(action)
                }
            }
        }
    )
}

@Composable
private fun DialogError(error: String?) {
    AnimatedVisibility(error != null) {
        Text(
            text = error.orEmpty(),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ChannelSectionRow(
    section: ServerChannelSection,
    isDragging: Boolean,
    canDrag: Boolean,
    enabled: Boolean,
    dragHandle: @Composable () -> Unit,
    onCreateChannel: () -> Unit,
    onRename: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        label = "channel category drag elevation"
    )
    val bottomCornerRadius by animateDpAsState(
        targetValue = if (section.channelIds.isEmpty()) 16.dp else 4.dp,
        label = "channel category bottom corners"
    )
    val shape = MaterialTheme.shapes.large.copy(
        bottomEnd = CornerSize(bottomCornerRadius),
        bottomStart = CornerSize(bottomCornerRadius)
    )
    val moveUpLabel = stringResource(R.string.server_settings_channels_move_category_up)
    val moveDownLabel = stringResource(R.string.server_settings_channels_move_category_down)
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = shape,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 2.dp)
            .zIndex(if (isDragging) 1f else 0f)
            .shadow(elevation, shape)
            .semantics {
                customActions = buildList {
                    onMoveUp?.let { action ->
                        add(
                            CustomAccessibilityAction(moveUpLabel) {
                                action()
                                true
                            }
                        )
                    }
                    onMoveDown?.let { action ->
                        add(
                            CustomAccessibilityAction(moveDownLabel) {
                                action()
                                true
                            }
                        )
                    }
                }
            }
    ) {
        ListItem(
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                headlineColor = MaterialTheme.colorScheme.onSecondaryContainer,
                supportingColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                    alpha = 0.72f
                ),
                leadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                trailingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            headlineContent = {
                Text(
                    text = sectionTitle(section),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Text(
                    pluralStringResource(
                        R.plurals.server_settings_channels_category_count,
                        section.channelIds.size,
                        section.channelIds.size
                    )
                )
            },
            leadingContent = {
                Icon(
                    painterResource(R.drawable.ic_list_24dp),
                    contentDescription = null
                )
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        IconButton(enabled = enabled, onClick = { menuExpanded = true }) {
                            Icon(
                                painterResource(R.drawable.ic_more_vert_24dp),
                                contentDescription = stringResource(
                                    R.string.server_settings_channels_category_actions
                                )
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            R.string.server_settings_channels_create_channel
                                        )
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painterResource(R.drawable.ic_add_24dp),
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onCreateChannel()
                                }
                            )
                            onRename?.let {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(
                                                R.string.server_settings_channels_rename_category
                                            )
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painterResource(R.drawable.ic_edit_24dp),
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        it()
                                    }
                                )
                            }
                            onDelete?.let {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(
                                                R.string.server_settings_channels_delete_category
                                            ),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painterResource(R.drawable.ic_delete_24dp),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        it()
                                    }
                                )
                            }
                        }
                    }
                    if (canDrag) {
                        dragHandle()
                    }
                }
            }
        )
    }
}

@Composable
private fun ChannelSettingsRow(
    channel: Channel?,
    channelId: String,
    isDragging: Boolean,
    enabled: Boolean,
    dragHandle: @Composable () -> Unit,
    last: Boolean,
    onClick: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    val bottomCornerRadius by animateDpAsState(
        targetValue = if (last) 16.dp else 4.dp,
        label = "channel bottom corners"
    )
    val shape = RoundedCornerShape(
        topStart = 4.dp, // channels are never first because they must be in a category (or uncategorised)
        topEnd = 4.dp,
        bottomStart = bottomCornerRadius,
        bottomEnd = bottomCornerRadius
    )
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        label = "channel drag elevation"
    )
    val moveUpLabel = stringResource(R.string.server_settings_channels_move_channel_up)
    val moveDownLabel = stringResource(R.string.server_settings_channels_move_channel_down)
    ListItem(
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        headlineContent = {
            Text(
                text = channel?.name ?: channelId,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                stringResource(
                    if (channel?.voice != null) {
                        R.string.server_settings_channels_voice_channel
                    } else {
                        R.string.server_settings_channels_text_channel
                    }
                )
            )
        },
        leadingContent = {
            channel?.let {
                ChannelIcon(channel = it, modifier = Modifier.size(24.dp))
            }
        },
        trailingContent = {
            dragHandle()
        },
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 2.dp)
            .zIndex(if (isDragging) 1f else 0f)
            .shadow(elevation, shape)
            .clip(shape)
            .semantics {
                customActions = buildList {
                    onMoveUp?.let { action ->
                        add(
                            CustomAccessibilityAction(moveUpLabel) {
                                action()
                                true
                            }
                        )
                    }
                    onMoveDown?.let { action ->
                        add(
                            CustomAccessibilityAction(moveDownLabel) {
                                action()
                                true
                            }
                        )
                    }
                }
            }
            .clickable(enabled = enabled, onClick = onClick)
    )
}

@Composable
private fun ReorderDragHandle(
    enabled: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    IconButton(
        enabled = enabled,
        modifier = modifier,
        onClick = {}
    ) {
        Icon(
            painterResource(R.drawable.ic_drag_handle_24dp),
            contentDescription = contentDescription
        )
    }
}

@Composable
private fun sectionTitle(section: ServerChannelSection): String =
    if (section.id == UncategorisedChannelSectionId) {
        stringResource(R.string.server_settings_channels_uncategorised)
    } else {
        section.title.orEmpty()
    }
