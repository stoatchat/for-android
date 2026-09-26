package chat.stoat.screens.settings.server

import android.app.Application
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
import chat.stoat.api.internals.BrushCompat
import chat.stoat.api.internals.hasPermission
import chat.stoat.api.routes.microservices.autumn.uploadToAutumn
import chat.stoat.api.routes.server.deleteServerRole
import chat.stoat.api.routes.server.editServerRole
import chat.stoat.api.routes.server.fetchServerRole
import chat.stoat.api.routes.server.setDefaultServerPermissions
import chat.stoat.api.routes.server.setServerRolePermissions
import chat.stoat.composables.generic.InlineMediaPicker
import chat.stoat.composables.generic.PermissionOverridePicker
import chat.stoat.composables.server.RoleColourIndicator
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.schemas.PermissionDescription
import chat.stoat.core.model.schemas.Role
import chat.stoat.internals.extensions.rememberServerPermissions
import chat.stoat.internals.server.PermissionOverrideValue
import chat.stoat.internals.server.ServerPermissionGroups
import chat.stoat.internals.server.ServerPermissionOption
import chat.stoat.internals.server.ServerRoleCapabilities
import chat.stoat.internals.server.canManageServerRole
import chat.stoat.internals.server.overrideFor
import chat.stoat.internals.server.serverRoleCapabilities
import chat.stoat.internals.server.withOverride
import chat.stoat.internals.server.withPermission
import chat.stoat.settings.dsl.SettingsPage
import chat.stoat.sheets.ColourPickerSheet
import chat.stoat.sheets.colourPickerString
import chat.stoat.sheets.colourPickerValue
import chat.stoat.ui.theme.DarkColorScheme
import chat.stoat.ui.theme.LightColorScheme
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import java.io.File
import androidx.compose.ui.semantics.Role as SemanticsRole

private const val DefaultRoleId = "default"

class ServerSettingsRoleEditorViewModel(
    private val context: Application,
) : ViewModel() {
    var loaded by mutableStateOf(false)
        private set
    var roleName by mutableStateOf("")
        private set
    var colour by mutableStateOf<String?>(null)
        private set
    var hoist by mutableStateOf(false)
        private set
    var iconModel by mutableStateOf<Any?>(null)
        private set
    var rolePermissions by mutableStateOf(PermissionDescription(0, 0))
        private set
    var defaultPermissions by mutableLongStateOf(0L)
        private set
    var saving by mutableStateOf(false)
        private set
    var deleting by mutableStateOf(false)
        private set
    var uploadProgress by mutableFloatStateOf(0f)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var saveSucceeded by mutableIntStateOf(0)
        private set
    var deletedRoleId by mutableStateOf<String?>(null)
        private set

    private var loadedKey: Pair<String, String>? = null
    private var initialRole: Role? = null
    private var initialDefaultPermissions = 0L
    private var initialIconModel: Any? = null

    val metadataChanged: Boolean
        get() = initialRole?.let { role ->
            roleName != role.name.orEmpty() ||
                    colour != role.colour ||
                    hoist != (role.hoist == true) ||
                    iconModel != initialIconModel
        } ?: false

    val permissionsChanged: Boolean
        get() = if (loadedKey?.second == DefaultRoleId) {
            defaultPermissions != initialDefaultPermissions
        } else {
            initialRole?.let { (it.permissions ?: PermissionDescription(0, 0)) != rolePermissions }
                ?: false
        }

    val hasChanges: Boolean
        get() = metadataChanged || permissionsChanged

    val canSave: Boolean
        get() = hasChanges && roleName.trim().length in 1..32 && !saving && !deleting

    fun load(serverId: String, roleId: String) {
        val key = serverId to roleId
        if (loadedKey == key && loaded) return
        loadedKey = key
        loaded = false
        error = null
        val server = StoatAPI.serverCache[serverId] ?: return

        if (roleId == DefaultRoleId) {
            initialRole = null
            initialDefaultPermissions = server.defaultPermissions ?: BitDefaults.Server
            defaultPermissions = initialDefaultPermissions
            roleName = context.getString(R.string.server_settings_roles_everyone)
            colour = null
            hoist = false
            iconModel = null
            initialIconModel = null
        } else {
            val role = server.roles?.get(roleId)
            if (role == null) {
                viewModelScope.launch {
                    runCatching { fetchServerRole(serverId, roleId) }
                        .onSuccess {
                            if (loadedKey == key) {
                                populateFromRole(it)
                                error = null
                                loaded = true
                            }
                        }
                        .onFailure {
                            if (loadedKey == key) {
                                error = it.message
                                    ?: context.getString(R.string.server_settings_role_load_error)
                            }
                        }
                }
                return
            }
            populateFromRole(role)
        }
        error = null
        loaded = true
    }

    fun retryLoad() {
        val key = loadedKey ?: return
        loadedKey = null
        load(key.first, key.second)
    }

    fun updateName(value: String) {
        if (value.length <= 32) roleName = value
    }

    fun updateColour(value: String?) {
        colour = value?.take(128)?.takeIf(String::isNotBlank)
    }

    fun updateHoist(value: Boolean) {
        hoist = value
    }

    fun updateIcon(uri: Uri?) {
        iconModel = uri
        error = null
    }

    fun updateRolePermission(option: ServerPermissionOption, value: PermissionOverrideValue) {
        rolePermissions = rolePermissions.withOverride(option.bit, value)
    }

    fun updateDefaultPermission(option: ServerPermissionOption, enabled: Boolean) {
        defaultPermissions = defaultPermissions.withPermission(option.bit, enabled)
    }

    fun reset() {
        if (loadedKey?.second == DefaultRoleId) {
            defaultPermissions = initialDefaultPermissions
        } else {
            initialRole?.let(::populateFromRole)
        }
        error = null
    }

    fun save(
        serverId: String,
        roleId: String,
        capabilities: ServerRoleCapabilities,
    ) {
        if (!canSave) return
        viewModelScope.launch {
            saving = true
            error = null
            uploadProgress = 0f
            runCatching {
                if (roleId == DefaultRoleId) {
                    if (capabilities.canManagePermissions && permissionsChanged) {
                        setDefaultServerPermissions(serverId, defaultPermissions)
                    }
                } else {
                    val initial = initialRole ?: error("Role is no longer available")
                    if (capabilities.canManageRoles && metadataChanged) {
                        val remove = mutableListOf<String>()
                        val iconId = changedIconId(remove)
                        if (colour == null && initial.colour != null) remove += "Colour"

                        editServerRole(
                            serverId = serverId,
                            roleId = roleId,
                            name = roleName.trim().takeIf { it != initial.name },
                            colour = colour?.takeIf { it != initial.colour },
                            hoist = hoist.takeIf { it != (initial.hoist == true) },
                            icon = iconId,
                            remove = remove,
                        )
                    }
                    if (capabilities.canManagePermissions && permissionsChanged) {
                        setServerRolePermissions(serverId, roleId, rolePermissions)
                    }
                }
            }.onSuccess {
                val server = StoatAPI.serverCache[serverId]
                if (roleId == DefaultRoleId) {
                    initialDefaultPermissions = server?.defaultPermissions ?: defaultPermissions
                    defaultPermissions = initialDefaultPermissions
                } else {
                    server?.roles?.get(roleId)?.let(::populateFromRole)
                }
                saveSucceeded++
            }.onFailure {
                error = it.message ?: context.getString(R.string.server_settings_role_update_error)
            }
            saving = false
            uploadProgress = 0f
        }
    }

    fun delete(serverId: String, roleId: String) {
        if (saving || deleting || roleId == DefaultRoleId) return
        viewModelScope.launch {
            deleting = true
            error = null
            runCatching { deleteServerRole(serverId, roleId) }
                .onSuccess { deletedRoleId = roleId }
                .onFailure {
                    error =
                        it.message ?: context.getString(R.string.server_settings_role_delete_error)
                }
            deleting = false
        }
    }

    fun consumeDeletedRole() {
        deletedRoleId = null
    }

    private fun populateFromRole(role: Role) {
        initialRole = role
        roleName = role.name.orEmpty()
        colour = role.colour
        hoist = role.hoist == true
        rolePermissions = role.permissions ?: PermissionDescription(0, 0)
        initialIconModel = role.icon?.let { "$STOAT_FILES/icons/${it.id}/original" }
        iconModel = initialIconModel
    }

    private suspend fun changedIconId(remove: MutableList<String>): String? {
        if (iconModel == initialIconModel) return null
        if (iconModel == null) {
            remove += "Icon"
            return null
        }

        val uri = iconModel as? Uri ?: return null
        val mime = context.contentResolver.getType(uri) ?: "image/*"
        val file = withContext(Dispatchers.IO) {
            File.createTempFile("stoat-role-icon-", null, context.cacheDir).also { target ->
                context.contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use(input::copyTo)
                } ?: throw IllegalArgumentException(
                    context.getString(R.string.server_settings_role_file_error)
                )
            }
        }

        return try {
            uploadToAutumn(
                file = file,
                name = uri.lastPathSegment ?: "role-icon",
                tag = "icons",
                contentType = ContentType.parse(mime),
                onProgress = { sent, total ->
                    uploadProgress = if (total > 0) sent.toFloat() / total else 0f
                },
            )
        } finally {
            withContext(Dispatchers.IO) { file.delete() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSettingsRoleEditor(
    navController: NavController,
    serverId: String,
    roleId: String,
    viewModel: ServerSettingsRoleEditorViewModel = koinViewModel(),
) {
    val permissions by rememberServerPermissions(serverId)
    val server = StoatAPI.serverCache[serverId]
    val capabilities = remember(server, permissions) {
        if (server != null && permissions != null) serverRoleCapabilities(server, permissions!!)
        else null
    }
    val role = server?.roles?.get(roleId)
    val elevated = roleId == DefaultRoleId || (server != null && role != null &&
            canManageServerRole(server, role))
    val editorCapabilities = capabilities?.let {
        if (roleId == DefaultRoleId || elevated) it
        else it.copy(canManageRoles = false, canManagePermissions = false)
    }
    var showDiscardConfirmation by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(serverId, roleId, server) {
        if (server != null) viewModel.load(serverId, roleId)
    }

    fun navigateBack() {
        if (viewModel.saving || viewModel.deleting) return
        if (viewModel.hasChanges) showDiscardConfirmation = true
        else navController.popBackStack()
    }

    BackHandler { navigateBack() }

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text(stringResource(R.string.server_settings_role_discard_title)) },
            text = { Text(stringResource(R.string.server_settings_role_discard_description)) },
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
                }) { Text(stringResource(R.string.server_settings_role_discard)) }
            },
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = {
                if (!viewModel.deleting) showDeleteConfirmation = false
            },
            title = {
                Text(
                    stringResource(
                        R.string.server_settings_role_delete_title,
                        viewModel.roleName,
                    )
                )
            },
            text = { Text(stringResource(R.string.server_settings_role_delete_description)) },
            dismissButton = {
                TextButton(
                    enabled = !viewModel.deleting,
                    onClick = { showDeleteConfirmation = false },
                ) { Text(stringResource(R.string.cancel)) }
            },
            confirmButton = {
                Button(
                    enabled = !viewModel.deleting,
                    onClick = { viewModel.delete(serverId, roleId) },
                ) {
                    if (viewModel.deleting) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.server_settings_role_delete))
                    }
                }
            },
        )
    }

    LaunchedEffect(viewModel.deletedRoleId) {
        if (viewModel.deletedRoleId == roleId) {
            viewModel.consumeDeletedRole()
            navController.popBackStack()
        }
    }

    SettingsPage(
        navController = navController,
        title = {
            Text(
                if (roleId == DefaultRoleId) stringResource(R.string.server_settings_roles_everyone)
                else viewModel.roleName.ifBlank { stringResource(R.string.server_settings_role) }
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
                val canSave = viewModel.canSave
                FloatingActionButton(
                    onClick = {
                        if (canSave && editorCapabilities != null) {
                            viewModel.save(serverId, roleId, editorCapabilities)
                        }
                    },
                    containerColor = if (canSave) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (canSave) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    if (viewModel.saving) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            painterResource(R.drawable.ic_check_24dp),
                            contentDescription = stringResource(R.string.server_settings_role_save),
                        )
                    }
                }
            }
        },
    ) {
        val savedMessage = stringResource(R.string.server_settings_role_saved)
        LaunchedEffect(viewModel.saveSucceeded) {
            if (viewModel.saveSucceeded > 0) showSnackbar(savedMessage)
        }

        if (
            permissions == null || server == null || capabilities == null ||
            editorCapabilities == null || !viewModel.loaded
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (viewModel.error == null) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        text = viewModel.error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Button(onClick = viewModel::retryLoad) {
                        Text(stringResource(R.string.server_identity_retry))
                    }
                }
            }
            return@SettingsPage
        }

        if (roleId != DefaultRoleId) {
            RolePreview(viewModel.roleName, viewModel.colour, viewModel.iconModel)

            if (editorCapabilities.canManageRoles) {
                RoleAppearanceEditor(viewModel)
            } else {
                ReadOnlyNotice(R.string.server_settings_role_appearance_permission)
            }
        } else {
            Text(
                text = stringResource(R.string.server_settings_roles_everyone_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        Text(
            text = stringResource(R.string.server_settings_role_permissions),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
        )

        if (roleId != DefaultRoleId) {
            Text(
                text = stringResource(R.string.server_settings_role_permissions_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        if (!editorCapabilities.canManagePermissions) {
            ReadOnlyNotice(R.string.server_settings_role_permissions_permission)
        }

        ServerPermissionGroups.forEach { group ->
            Text(
                text = stringResource(group.title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
            )
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                group.permissions.forEachIndexed { index, option ->
                    PermissionRow(
                        option = option,
                        defaultRole = roleId == DefaultRoleId,
                        overrideValue = viewModel.rolePermissions.overrideFor(option.bit),
                        defaultValue = viewModel.defaultPermissions.hasPermission(option.bit),
                        enabled = editorCapabilities.canManagePermissions &&
                                permissions!!.hasPermission(option.bit) && !viewModel.saving,
                        first = index == 0,
                        last = index == group.permissions.lastIndex,
                        onOverrideChange = { viewModel.updateRolePermission(option, it) },
                        onDefaultChange = { viewModel.updateDefaultPermission(option, it) },
                    )
                }
            }
        }

        if (roleId != DefaultRoleId && editorCapabilities.canManageRoles) {
            Spacer(Modifier.height(24.dp))
            TextButton(
                enabled = !viewModel.saving && !viewModel.deleting,
                onClick = { showDeleteConfirmation = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Icon(painterResource(R.drawable.ic_delete_24dp), contentDescription = null)
                Text(
                    stringResource(R.string.server_settings_role_delete),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        AnimatedVisibility(viewModel.saving && viewModel.uploadProgress > 0f) {
            LinearProgressIndicator(
                progress = { viewModel.uploadProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
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
private fun RolePreview(name: String, colour: String?, iconModel: Any?) {
    val brush = colour?.let { BrushCompat.parseColour(it) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        RolePreviewSurface(
            name = name,
            roleBrush = brush,
            iconModel = iconModel,
            colourScheme = LightColorScheme,
            modifier = Modifier.weight(1f),
        )
        RolePreviewSurface(
            name = name,
            roleBrush = brush,
            iconModel = iconModel,
            colourScheme = DarkColorScheme,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun RolePreviewSurface(
    name: String,
    roleBrush: Brush?,
    iconModel: Any?,
    colourScheme: ColorScheme,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = colourScheme.surfaceContainerLowest,
        contentColor = colourScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, colourScheme.outlineVariant),
        modifier = modifier,
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(colourScheme.surfaceContainerHighest),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = name.trim().ifBlank {
                                stringResource(R.string.server_settings_roles_name)
                            },
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = MaterialTheme.typography.labelLarge.fontSize,
                                lineHeight = MaterialTheme.typography.labelLarge.lineHeight,
                                fontWeight = FontWeight.Bold,
                                brush = roleBrush ?: Brush.linearGradient(
                                    listOf(colourScheme.onSurface, colourScheme.onSurface)
                                ),
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (iconModel != null) {
                            Spacer(Modifier.width(4.dp))
                            GlideImage(
                                model = iconModel,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                transition = CrossFade,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.server_settings_role_preview_message),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = MaterialTheme.typography.bodySmall.fontSize,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                        ),
                        color = colourScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleAppearanceEditor(viewModel: ServerSettingsRoleEditorViewModel) {
    var showColourPicker by remember { mutableStateOf(false) }
    val pickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pickerScope = rememberCoroutineScope()

    if (showColourPicker) {
        ModalBottomSheet(
            sheetState = pickerSheetState,
            onDismissRequest = { showColourPicker = false },
        ) {
            ColourPickerSheet(
                initialValue = colourPickerValue(
                    viewModel.colour,
                    MaterialTheme.colorScheme.primary.toArgb(),
                ),
                onColourSelected = {
                    viewModel.updateColour(colourPickerString(it))
                    pickerScope.launch {
                        pickerSheetState.hide()
                        showColourPicker = false
                    }
                },
                onUseDefaultColour = {
                    viewModel.updateColour(null)
                    pickerScope.launch {
                        pickerSheetState.hide()
                        showColourPicker = false
                    }
                },
                onDismiss = {
                    pickerScope.launch {
                        pickerSheetState.hide()
                        showColourPicker = false
                    }
                },
            )
        }
    }

    Text(
        text = stringResource(R.string.server_settings_role_appearance),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
    )
    OutlinedTextField(
        value = viewModel.roleName,
        onValueChange = viewModel::updateName,
        label = { Text(stringResource(R.string.server_settings_roles_name)) },
        supportingText = { Text("${viewModel.roleName.length}/32") },
        singleLine = true,
        enabled = !viewModel.saving,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )

    Text(
        text = stringResource(R.string.server_settings_role_colour),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
    OutlinedButton(
        onClick = { showColourPicker = true },
        enabled = !viewModel.saving,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        RoleColourIndicator(
            colour = viewModel.colour,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = if (viewModel.colour == null) {
                stringResource(R.string.server_settings_role_default_colour)
            } else {
                stringResource(R.string.server_settings_role_change_colour)
            },
            modifier = Modifier.padding(start = 12.dp),
        )
    }

    Text(
        text = stringResource(R.string.server_settings_role_icon),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
    InlineMediaPicker(
        currentModel = viewModel.iconModel,
        circular = true,
        onPick = viewModel::updateIcon,
        onRemove = { viewModel.updateIcon(null) },
        enabled = !viewModel.saving,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(MaterialTheme.shapes.large)
            .semantics { role = SemanticsRole.Switch }
            .clickable(enabled = !viewModel.saving) {
                viewModel.updateHoist(!viewModel.hoist)
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.server_settings_role_hoist),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.server_settings_role_hoist_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = viewModel.hoist,
                enabled = !viewModel.saving,
                onCheckedChange = null,
            )
        }
    }
}

@Composable
private fun ReadOnlyNotice(message: Int) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
private fun PermissionRow(
    option: ServerPermissionOption,
    defaultRole: Boolean,
    overrideValue: PermissionOverrideValue,
    defaultValue: Boolean,
    enabled: Boolean,
    first: Boolean,
    last: Boolean,
    onOverrideChange: (PermissionOverrideValue) -> Unit,
    onDefaultChange: (Boolean) -> Unit,
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
        modifier = if (defaultRole) {
            Modifier
                .clip(shape)
                .semantics { role = SemanticsRole.Switch }
                .clickable(enabled = enabled) { onDefaultChange(!defaultValue) }
        } else {
            Modifier
        },
    ) {
        Column(
            Modifier.padding(
                horizontal = 16.dp,
                vertical = if (defaultRole) 14.dp else 12.dp,
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(option.title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(option.description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (defaultRole) {
                    Switch(
                        checked = defaultValue,
                        enabled = enabled,
                        onCheckedChange = null,
                    )
                } else {
                    PermissionOverridePicker(
                        value = overrideValue,
                        enabled = enabled,
                        onValueChange = onOverrideChange,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}
