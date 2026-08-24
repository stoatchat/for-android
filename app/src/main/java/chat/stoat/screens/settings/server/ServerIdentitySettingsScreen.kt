package chat.stoat.screens.settings.server

import android.app.Application
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.activities.StoatTweenFloat
import chat.stoat.activities.StoatTweenSize
import chat.stoat.api.StoatAPI
import chat.stoat.api.routes.microservices.autumn.uploadToAutumn
import chat.stoat.api.routes.server.fetchMember
import chat.stoat.api.routes.server.patchMemberIdentity
import chat.stoat.composables.generic.InlineMediaPicker
import chat.stoat.composables.screens.settings.UserOverview
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.schemas.Member
import chat.stoat.core.model.schemas.User
import chat.stoat.internals.extensions.rememberServerPermissions
import chat.stoat.internals.server.ServerIdentityCapabilities
import chat.stoat.internals.server.serverIdentityCapabilities
import chat.stoat.settings.dsl.SettingsPage
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import java.io.File

class ServerIdentitySettingsViewModel(
    private val context: Application,
) : ViewModel() {
    var initialMember by mutableStateOf<Member?>(null)
        private set
    var loaded by mutableStateOf(false)
        private set
    var loading by mutableStateOf(false)
        private set

    var nickname by mutableStateOf("")
        private set
    var pronouns by mutableStateOf("")
        private set
    var avatarModel by mutableStateOf<Any?>(null)
        private set

    var saving by mutableStateOf(false)
        private set
    var uploadProgress by mutableFloatStateOf(0f)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var updateError by mutableStateOf<String?>(null)
        private set
    var saveSucceeded by mutableIntStateOf(0)
        private set

    private var loadedTarget: Pair<String, String>? = null
    private var initialAvatarModel: Any? = null

    fun load(serverId: String, userId: String, force: Boolean = false) {
        val target = serverId to userId
        if (!force && loadedTarget == target && (loaded || loading)) return

        loadedTarget = target
        loaded = false
        loading = true
        loadError = null

        viewModelScope.launch {
            runCatching { fetchMember(serverId, userId, pure = true) }
                .onSuccess { member ->
                    if (loadedTarget != target) return@onSuccess
                    StoatAPI.members.setMember(serverId, member)
                    populateFromMember(member)
                    loaded = true
                }
                .onFailure { error ->
                    if (loadedTarget != target) return@onFailure
                    val cached = StoatAPI.members.getMember(serverId, userId)
                    if (cached != null) {
                        populateFromMember(cached)
                        loaded = true
                    } else {
                        loadError = error.message
                            ?: context.getString(R.string.server_identity_load_error)
                    }
                }

            if (loadedTarget == target) loading = false
        }
    }

    fun updateNickname(value: String) {
        if (value.length <= 32) nickname = value
        updateError = null
    }

    fun updatePronouns(value: String) {
        if (value.length <= 24) pronouns = value
        updateError = null
    }

    fun setAvatar(uri: Uri?) {
        avatarModel = uri
        updateError = null
    }

    fun reset() {
        initialMember?.let(::populateFromMember)
        updateError = null
    }

    fun hasChanges(): Boolean {
        val member = initialMember ?: return false
        return nickname != member.nickname.orEmpty() ||
                pronouns != member.pronouns.orEmpty() ||
                avatarModel != initialAvatarModel
    }

    fun hasSavableChanges(capabilities: ServerIdentityCapabilities): Boolean {
        val member = initialMember ?: return false
        return (capabilities.canChangeNickname &&
                nickname.trim().ifEmpty { null } != member.nickname) ||
                (capabilities.canChangePronouns &&
                        pronouns.trim().ifEmpty { null } != member.pronouns) ||
                canSaveAvatarChange(capabilities)
    }

    fun canSave(capabilities: ServerIdentityCapabilities): Boolean {
        return hasSavableChanges(capabilities) &&
                nickname.trim().length <= 32 &&
                pronouns.trim().length <= 24 &&
                !saving
    }

    fun save(
        serverId: String,
        userId: String,
        capabilities: ServerIdentityCapabilities,
    ) {
        val member = initialMember ?: return
        if (!canSave(capabilities)) return

        viewModelScope.launch {
            saving = true
            updateError = null
            uploadProgress = 0f

            runCatching {
                val remove = mutableListOf<String>()
                val normalizedNickname = nickname.trim().ifEmpty { null }
                val normalizedPronouns = pronouns.trim().ifEmpty { null }
                val nicknameChanged = capabilities.canChangeNickname &&
                        normalizedNickname != member.nickname
                val pronounsChanged = capabilities.canChangePronouns &&
                        normalizedPronouns != member.pronouns
                val avatarChanged = canSaveAvatarChange(capabilities)

                if (nicknameChanged && normalizedNickname == null) remove += "Nickname"
                if (pronounsChanged && normalizedPronouns == null) remove += "Pronouns"

                val avatarId = if (avatarChanged) {
                    when (val selectedAvatar = avatarModel) {
                        null -> {
                            remove += "Avatar"
                            null
                        }

                        is Uri -> uploadAvatar(selectedAvatar)
                        else -> null
                    }
                } else {
                    null
                }

                patchMemberIdentity(
                    serverId = serverId,
                    userId = userId,
                    nickname = normalizedNickname.takeIf { nicknameChanged },
                    pronouns = normalizedPronouns.takeIf { pronounsChanged },
                    avatar = avatarId,
                    remove = remove,
                )
            }.onSuccess(::populateFromMember)
                .onSuccess {
                    saveSucceeded++
                }
                .onFailure { error ->
                    updateError = error.message
                        ?: context.getString(R.string.server_identity_update_error)
                }

            saving = false
            uploadProgress = 0f
        }
    }

    private fun canSaveAvatarChange(capabilities: ServerIdentityCapabilities): Boolean {
        if (avatarModel == initialAvatarModel) return false
        return when (avatarModel) {
            null -> capabilities.canRemoveAvatar
            is Uri -> capabilities.canSetAvatar
            else -> false
        }
    }

    private fun populateFromMember(member: Member) {
        initialMember = member
        nickname = member.nickname.orEmpty()
        pronouns = member.pronouns.orEmpty()
        initialAvatarModel = member.avatar?.id?.let { "$STOAT_FILES/avatars/$it/original" }
        avatarModel = initialAvatarModel
        member.id?.let { StoatAPI.members.setMember(it.server, member) }
    }

    private suspend fun uploadAvatar(uri: Uri): String {
        val mime = context.contentResolver.getType(uri) ?: "image/*"

        val file = withContext(Dispatchers.IO) {
            File.createTempFile("stoat-server-avatar-", null, context.cacheDir)
                .also { temporaryFile ->
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        temporaryFile.outputStream().use(input::copyTo)
                    } ?: throw IllegalArgumentException(
                        context.getString(R.string.server_identity_file_error)
                    )
                }
        }

        return try {
            uploadToAutumn(
                file = file,
                name = uri.lastPathSegment ?: "server-avatar",
                tag = "avatars",
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

@Composable
fun ServerIdentitySettingsScreen(
    navController: NavController,
    serverId: String,
    targetUserId: String,
    viewModel: ServerIdentitySettingsViewModel = koinViewModel(),
) {
    val permissions by rememberServerPermissions(serverId)
    val server = StoatAPI.serverCache[serverId]
    val user = StoatAPI.userCache[targetUserId]
    val editingOwnIdentity = targetUserId == StoatAPI.selfId
    val capabilities = serverIdentityCapabilities(
        targetUserId = targetUserId,
        selfUserId = StoatAPI.selfId,
        permissions = permissions,
    )
    var showDiscardConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(serverId, targetUserId) { viewModel.load(serverId, targetUserId) }

    fun navigateBack() {
        if (viewModel.saving) return
        if (viewModel.hasChanges()) {
            showDiscardConfirmation = true
        } else {
            navController.popBackStack()
        }
    }

    BackHandler { navigateBack() }

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text(stringResource(R.string.server_identity_discard_title)) },
            text = { Text(stringResource(R.string.server_identity_discard_description)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reset()
                        showDiscardConfirmation = false
                        navController.popBackStack()
                    },
                ) {
                    Text(stringResource(R.string.server_identity_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmation = false }) {
                    Text(stringResource(R.string.server_identity_keep_editing))
                }
            },
        )
    }

    SettingsPage(
        navController = navController,
        title = {
            Text(
                if (editingOwnIdentity) {
                    stringResource(
                        R.string.server_identity_title,
                        server?.name ?: stringResource(R.string.unknown),
                    )
                } else {
                    stringResource(
                        R.string.server_identity_member_title,
                        user?.let { User.resolveDefaultName(it) }
                            ?: stringResource(R.string.unknown),
                        server?.name ?: stringResource(R.string.unknown),
                    )
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        onNavigateBack = ::navigateBack,
        floatingActionButton = {
            AnimatedVisibility(
                visible = viewModel.hasSavableChanges(capabilities),
                enter = scaleIn(animationSpec = StoatTweenFloat),
                exit = scaleOut(animationSpec = StoatTweenFloat),
                modifier = Modifier.imePadding(),
            ) {
                val canSave = viewModel.canSave(capabilities)
                FloatingActionButton(
                    onClick = {
                        if (canSave) {
                            viewModel.save(serverId, targetUserId, capabilities)
                        }
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
                            contentDescription = stringResource(R.string.server_identity_save),
                        )
                    }
                }
            }
        },
    ) {
        val savedMessage = stringResource(R.string.server_identity_saved)
        LaunchedEffect(viewModel.saveSucceeded) {
            if (viewModel.saveSucceeded > 0) showSnackbar(savedMessage)
        }

        if (permissions == null || viewModel.loading) {
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

        if (!viewModel.loaded) {
            LoadError(
                message = viewModel.loadError
                    ?: stringResource(R.string.server_identity_load_error),
                onRetry = { viewModel.load(serverId, targetUserId, force = true) },
            )
            return@SettingsPage
        }

        if (user != null) {
            Text(
                text = stringResource(
                    if (editingOwnIdentity) {
                        R.string.server_identity_preview_description
                    } else {
                        R.string.server_identity_preview_description_member
                    }
                ),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 8.dp),
            )

            UserOverview(
                user = user.copy(
                    displayName = viewModel.nickname.trim().takeIf(String::isNotEmpty)
                        ?: user.displayName,
                    pronouns = viewModel.pronouns.trim().takeIf(String::isNotEmpty)
                        ?: user.pronouns,
                ),
                pfpUrl = viewModel.avatarModel?.toString() ?: accountAvatarUrl(user),
            )
        }

        Text(
            text = stringResource(R.string.server_identity_avatar),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
        )
        AnimatedContent(
            targetState = when {
                !capabilities.canSetAvatar && !capabilities.canRemoveAvatar ->
                    ServerAvatarDescription.PermissionDenied

                viewModel.avatarModel == null -> ServerAvatarDescription.Inherited
                else -> ServerAvatarDescription.Custom
            },
            label = "server avatar description",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        ) { description ->
            Text(
                text = stringResource(
                    when (description) {
                        ServerAvatarDescription.PermissionDenied ->
                            if (editingOwnIdentity) {
                                R.string.server_identity_permission_avatar
                            } else {
                                R.string.server_identity_permission_avatar_member
                            }

                        ServerAvatarDescription.Inherited ->
                            if (editingOwnIdentity) {
                                R.string.server_identity_avatar_inherited
                            } else {
                                R.string.server_identity_avatar_inherited_member
                            }

                        ServerAvatarDescription.Custom ->
                            if (editingOwnIdentity) {
                                R.string.server_identity_avatar_custom
                            } else {
                                R.string.server_identity_avatar_custom_member
                            }
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (description == ServerAvatarDescription.PermissionDenied) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            InlineMediaPicker(
                currentModel = viewModel.avatarModel ?: accountAvatarUrl(user),
                circular = true,
                useAvatarCircularity = true,
                onPick = viewModel::setAvatar,
                canRemove = viewModel.avatarModel != null,
                onRemove = { viewModel.setAvatar(null) },
                enabled = capabilities.canSetAvatar && !viewModel.saving,
                removeEnabled = capabilities.canRemoveAvatar && !viewModel.saving,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .animateContentSize(StoatTweenSize),
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

        OutlinedTextField(
            value = viewModel.nickname,
            onValueChange = viewModel::updateNickname,
            label = { Text(stringResource(R.string.server_identity_nickname)) },
            placeholder = {
                Text(
                    stringResource(
                        if (editingOwnIdentity) {
                            R.string.server_identity_nickname_placeholder
                        } else {
                            R.string.server_identity_nickname_placeholder_member
                        }
                    )
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_id_card_24dp),
                    contentDescription = null,
                )
            },
            supportingText = {
                Text(
                    if (capabilities.canChangeNickname) {
                        "${viewModel.nickname.length}/32"
                    } else {
                        stringResource(
                            if (editingOwnIdentity) {
                                R.string.server_identity_permission_nickname
                            } else {
                                R.string.server_identity_permission_nickname_member
                            }
                        )
                    }
                )
            },
            isError = !capabilities.canChangeNickname,
            singleLine = true,
            enabled = capabilities.canChangeNickname && !viewModel.saving,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        OutlinedTextField(
            value = viewModel.pronouns,
            onValueChange = viewModel::updatePronouns,
            label = { Text(stringResource(R.string.server_identity_pronouns)) },
            placeholder = { Text(stringResource(R.string.server_identity_pronouns_placeholder)) },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_person_celebrate_24dp),
                    contentDescription = null,
                )
            },
            supportingText = {
                Text(
                    if (capabilities.canChangePronouns) {
                        "${viewModel.pronouns.length}/24"
                    } else {
                        stringResource(R.string.server_identity_permission_pronouns)
                    }
                )
            },
            isError = !capabilities.canChangePronouns,
            singleLine = true,
            enabled = capabilities.canChangePronouns && !viewModel.saving,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        AnimatedVisibility(viewModel.updateError != null) {
            Text(
                text = viewModel.updateError
                    ?: stringResource(R.string.server_identity_update_error),
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
private fun LoadError(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_error_24dp),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.server_identity_retry))
        }
    }
}

private fun accountAvatarUrl(user: User?): String? {
    return user?.avatar?.id?.let { "$STOAT_FILES/avatars/$it/original" }
}

private enum class ServerAvatarDescription {
    PermissionDenied,
    Inherited,
    Custom,
}
