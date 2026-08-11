package chat.stoat.screens.settings.server

import android.app.Application
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.activities.StoatTweenFloat
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.internals.hasPermission
import chat.stoat.api.routes.microservices.autumn.uploadToAutumn
import chat.stoat.api.routes.server.patchServer
import chat.stoat.composables.generic.InlineMediaPicker
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.schemas.Channel
import chat.stoat.core.model.schemas.Server
import chat.stoat.internals.extensions.rememberServerPermissions
import chat.stoat.settings.dsl.SettingsPage
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import java.io.File

class ServerSettingsOverviewViewModel(
    private val context: Application,
) : ViewModel() {
    var initialServer by mutableStateOf<Server?>(null)
        private set
    var loaded by mutableStateOf(false)
        private set

    var serverName by mutableStateOf("")
        private set
    var serverDescription by mutableStateOf("")
        private set
    var iconModel by mutableStateOf<Any?>(null)
        private set
    var bannerModel by mutableStateOf<Any?>(null)
        private set
    var userJoinedChannel by mutableStateOf<String?>(null)
        private set
    var userLeftChannel by mutableStateOf<String?>(null)
        private set
    var userKickedChannel by mutableStateOf<String?>(null)
        private set
    var userBannedChannel by mutableStateOf<String?>(null)
        private set

    var saving by mutableStateOf(false)
        private set
    var uploadProgress by mutableFloatStateOf(0f)
        private set
    var updateError by mutableStateOf<String?>(null)
        private set
    var saveSucceeded by mutableIntStateOf(0)
        private set

    private var loadedServerId: String? = null
    private var initialIconModel: Any? = null
    private var initialBannerModel: Any? = null

    val hasChanges: Boolean
        get() = initialServer?.let { server ->
            serverName != server.name.orEmpty() ||
                    serverDescription != server.description.orEmpty() ||
                    iconModel != initialIconModel ||
                    bannerModel != initialBannerModel ||
                    userJoinedChannel != server.systemMessages?.userJoined ||
                    userLeftChannel != server.systemMessages?.userLeft ||
                    userKickedChannel != server.systemMessages?.userKicked ||
                    userBannedChannel != server.systemMessages?.userBanned
        } ?: false

    val canSave: Boolean
        get() = hasChanges &&
                serverName.trim().length in 1..32 &&
                serverDescription.trim().length <= 1024 &&
                !saving

    fun load(serverId: String) {
        if (loadedServerId == serverId && loaded) return

        loadedServerId = serverId
        StoatAPI.serverCache[serverId]?.let(::populateFromServer)
        loaded = true
    }

    fun updateServerName(value: String) {
        if (value.length <= 32) serverName = value
    }

    fun updateServerDescription(value: String) {
        if (value.length <= 1024) serverDescription = value
    }

    fun setIcon(uri: Uri?) {
        iconModel = uri
        updateError = null
    }

    fun setBanner(uri: Uri?) {
        bannerModel = uri
        updateError = null
    }

    fun updateUserJoinedChannel(channelId: String?) {
        userJoinedChannel = channelId
    }

    fun updateUserLeftChannel(channelId: String?) {
        userLeftChannel = channelId
    }

    fun updateUserKickedChannel(channelId: String?) {
        userKickedChannel = channelId
    }

    fun updateUserBannedChannel(channelId: String?) {
        userBannedChannel = channelId
    }

    fun reset() {
        initialServer?.let(::populateFromServer)
        updateError = null
    }

    fun requestSave() {
        val server = initialServer ?: return
        val serverId = server.id ?: return
        if (!canSave) return

        viewModelScope.launch {
            saving = true
            updateError = null
            uploadProgress = 0f

            runCatching {
                val remove = mutableListOf<String>()
                val trimmedName = serverName.trim()
                val trimmedDescription = serverDescription.trim()

                val iconId = changedMediaId(
                    current = iconModel,
                    initial = initialIconModel,
                    tag = "icons",
                    fallbackName = "server-icon",
                    removeField = "Icon",
                    remove = remove,
                )
                val bannerId = changedMediaId(
                    current = bannerModel,
                    initial = initialBannerModel,
                    tag = "banners",
                    fallbackName = "server-banner",
                    removeField = "Banner",
                    remove = remove,
                )

                val descriptionChanged = serverDescription != server.description.orEmpty()
                if (descriptionChanged && trimmedDescription.isEmpty()) {
                    remove += "Description"
                }

                val systemMessagesChanged =
                    userJoinedChannel != server.systemMessages?.userJoined ||
                            userLeftChannel != server.systemMessages?.userLeft ||
                            userKickedChannel != server.systemMessages?.userKicked ||
                            userBannedChannel != server.systemMessages?.userBanned

                patchServer(
                    serverId = serverId,
                    name = trimmedName.takeIf { it != server.name },
                    description = trimmedDescription.takeIf {
                        descriptionChanged && it.isNotEmpty()
                    },
                    icon = iconId,
                    banner = bannerId,
                    systemMessages = if (systemMessagesChanged) {
                        buildMap {
                            userJoinedChannel?.let { put("user_joined", it) }
                            userLeftChannel?.let { put("user_left", it) }
                            userKickedChannel?.let { put("user_kicked", it) }
                            userBannedChannel?.let { put("user_banned", it) }
                        }
                    } else {
                        null
                    },
                    remove = remove,
                )
            }.onSuccess { updatedServer ->
                populateFromServer(updatedServer)
                saveSucceeded++
            }.onFailure { error ->
                updateError = error.message
                    ?: context.getString(R.string.server_settings_overview_update_error)
            }

            saving = false
            uploadProgress = 0f
        }
    }

    private fun populateFromServer(server: Server) {
        initialServer = server
        serverName = server.name.orEmpty()
        serverDescription = server.description.orEmpty()
        initialIconModel = server.icon?.let { "$STOAT_FILES/icons/${it.id}" }
        initialBannerModel = server.banner?.let { "$STOAT_FILES/banners/${it.id}/${it.filename}" }
        iconModel = initialIconModel
        bannerModel = initialBannerModel
        userJoinedChannel = server.systemMessages?.userJoined
        userLeftChannel = server.systemMessages?.userLeft
        userKickedChannel = server.systemMessages?.userKicked
        userBannedChannel = server.systemMessages?.userBanned
    }

    private suspend fun changedMediaId(
        current: Any?,
        initial: Any?,
        tag: String,
        fallbackName: String,
        removeField: String,
        remove: MutableList<String>,
    ): String? {
        if (current == initial) return null
        if (current == null) {
            remove += removeField
            return null
        }

        val uri = current as? Uri ?: return null
        val mime = context.contentResolver.getType(uri) ?: "image/*"
        if (mime.endsWith("webp", ignoreCase = true)) {
            throw IllegalArgumentException(
                context.getString(R.string.server_settings_overview_webp_unsupported)
            )
        }

        val file = withContext(Dispatchers.IO) {
            File.createTempFile("stoat-$tag-", null, context.cacheDir).also { temporaryFile ->
                context.contentResolver.openInputStream(uri)?.use { input ->
                    temporaryFile.outputStream().use(input::copyTo)
                } ?: throw IllegalArgumentException(
                    context.getString(R.string.server_settings_overview_file_error)
                )
            }
        }

        return try {
            uploadToAutumn(
                file = file,
                name = uri.lastPathSegment ?: fallbackName,
                tag = tag,
                contentType = ContentType.parse(mime),
                onProgress = { sent, total ->
                    uploadProgress = if (total > 0) sent.toFloat() / total.toFloat() else 0f
                },
            )
        } finally {
            withContext(Dispatchers.IO) { file.delete() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSettingsOverview(
    navController: NavController,
    serverId: String,
    viewModel: ServerSettingsOverviewViewModel = koinViewModel(),
) {
    val permissions by rememberServerPermissions(serverId)
    val server = StoatAPI.serverCache[serverId]
    var showDiscardConfirmation by remember { mutableStateOf(false) }

    if (permissions != null && permissions?.hasPermission(PermissionBit.ManageServer) == false) {
        LaunchedEffect(serverId) { navController.popBackStack() }
        return
    }

    LaunchedEffect(serverId, permissions) {
        if (permissions?.hasPermission(PermissionBit.ManageServer) == true) {
            viewModel.load(serverId)
        }
    }

    val channels = remember(server?.channels, StoatAPI.channelCache.size) {
        server?.channels.orEmpty().mapNotNull(StoatAPI.channelCache::get)
    }

    fun navigateBack() {
        if (viewModel.saving) return
        if (viewModel.hasChanges) {
            showDiscardConfirmation = true
        } else {
            navController.popBackStack()
        }
    }

    BackHandler { navigateBack() }

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = {
                Text(stringResource(R.string.server_settings_overview_discard_title))
            },
            text = {
                Text(stringResource(R.string.server_settings_overview_discard_description))
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reset()
                        showDiscardConfirmation = false
                        navController.popBackStack()
                    },
                ) {
                    Text(stringResource(R.string.server_settings_overview_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmation = false }) {
                    Text(stringResource(R.string.server_settings_overview_keep_editing))
                }
            },
        )
    }

    SettingsPage(
        navController = navController,
        title = { Text(stringResource(R.string.server_settings_overview)) },
        onNavigateBack = ::navigateBack,
        floatingActionButton = {
            AnimatedVisibility(
                visible = viewModel.hasChanges,
                enter = scaleIn(animationSpec = StoatTweenFloat),
                exit = scaleOut(animationSpec = StoatTweenFloat),
                modifier = Modifier.imePadding(),
            ) {
                val canSave = viewModel.canSave
                FloatingActionButton(
                    onClick = {
                        if (canSave) viewModel.requestSave()
                    },
                    containerColor = if (canSave) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (canSave) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ) {
                    if (viewModel.saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_check_24dp),
                            contentDescription = stringResource(
                                R.string.server_settings_overview_save
                            ),
                        )
                    }
                }
            }
        },
    ) {
        val saveSuccessMessage = stringResource(R.string.server_settings_overview_saved)
        LaunchedEffect(viewModel.saveSucceeded) {
            if (viewModel.saveSucceeded > 0) showSnackbar(saveSuccessMessage)
        }

        if (permissions == null || !viewModel.loaded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@SettingsPage
        }

        ServerInfoFields(viewModel)
        SystemMessageFields(viewModel, channels)

        AnimatedVisibility(viewModel.updateError != null) {
            Text(
                text = viewModel.updateError
                    ?: stringResource(R.string.server_settings_overview_update_error),
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        Spacer(Modifier.height(88.dp))
    }
}

@Composable
private fun ServerInfoFields(viewModel: ServerSettingsOverviewViewModel) {
    Text(
        text = stringResource(R.string.server_settings_overview_server_icon),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        InlineMediaPicker(
            currentModel = viewModel.iconModel,
            circular = true,
            onPick = viewModel::setIcon,
            onRemove = { viewModel.setIcon(null) },
            enabled = !viewModel.saving,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }

    Text(
        text = stringResource(R.string.server_settings_overview_server_banner),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        InlineMediaPicker(
            currentModel = viewModel.bannerModel,
            onPick = viewModel::setBanner,
            onRemove = { viewModel.setBanner(null) },
            enabled = !viewModel.saving,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }

    AnimatedVisibility(viewModel.saving && viewModel.uploadProgress > 0f) {
        LinearProgressIndicator(
            progress = { viewModel.uploadProgress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }

    TextField(
        value = viewModel.serverName,
        onValueChange = viewModel::updateServerName,
        label = { Text(stringResource(R.string.server_settings_overview_server_name)) },
        supportingText = { Text("${viewModel.serverName.length}/32") },
        singleLine = true,
        enabled = !viewModel.saving,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )

    TextField(
        value = viewModel.serverDescription,
        onValueChange = viewModel::updateServerDescription,
        label = { Text(stringResource(R.string.server_settings_overview_server_description)) },
        placeholder = { Text(stringResource(R.string.server_settings_overview_description_hint)) },
        supportingText = { Text("${viewModel.serverDescription.length}/1024") },
        minLines = 3,
        enabled = !viewModel.saving,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun SystemMessageFields(
    viewModel: ServerSettingsOverviewViewModel,
    channels: List<Channel>,
) {
    Text(
        text = stringResource(R.string.server_settings_overview_system_messages),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )

    SystemMessageChannelPicker(
        label = stringResource(R.string.server_settings_overview_user_joined),
        selectedChannelId = viewModel.userJoinedChannel,
        channels = channels,
        enabled = !viewModel.saving,
        onSelected = viewModel::updateUserJoinedChannel,
    )
    SystemMessageChannelPicker(
        label = stringResource(R.string.server_settings_overview_user_left),
        selectedChannelId = viewModel.userLeftChannel,
        channels = channels,
        enabled = !viewModel.saving,
        onSelected = viewModel::updateUserLeftChannel,
    )
    SystemMessageChannelPicker(
        label = stringResource(R.string.server_settings_overview_user_kicked),
        selectedChannelId = viewModel.userKickedChannel,
        channels = channels,
        enabled = !viewModel.saving,
        onSelected = viewModel::updateUserKickedChannel,
    )
    SystemMessageChannelPicker(
        label = stringResource(R.string.server_settings_overview_user_banned),
        selectedChannelId = viewModel.userBannedChannel,
        channels = channels,
        enabled = !viewModel.saving,
        onSelected = viewModel::updateUserBannedChannel,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SystemMessageChannelPicker(
    label: String,
    selectedChannelId: String?,
    channels: List<Channel>,
    enabled: Boolean,
    onSelected: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val disabled = stringResource(R.string.server_settings_overview_disabled)
    val selectedName = channels.firstOrNull { it.id == selectedChannelId }?.name ?: disabled

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        TextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(disabled) },
                onClick = {
                    onSelected(null)
                    expanded = false
                },
            )
            channels.forEach { channel ->
                val channelId = channel.id ?: return@forEach
                DropdownMenuItem(
                    text = { Text(channel.name.orEmpty()) },
                    onClick = {
                        onSelected(channelId)
                        expanded = false
                    },
                )
            }
        }
    }
}
