package chat.stoat.screens.settings.server

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.internals.hasPermission
import chat.stoat.api.routes.custom.createEmoji
import chat.stoat.api.routes.custom.deleteEmoji
import chat.stoat.api.routes.microservices.autumn.uploadToAutumn
import chat.stoat.api.routes.misc.getRootRoute
import chat.stoat.composables.generic.RemoteImage
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.composables.settings.ServerSettingsEmptyState
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.schemas.Emoji
import chat.stoat.core.model.schemas.User
import chat.stoat.internals.extensions.rememberServerPermissions
import chat.stoat.screens.settings.SettingsListItem
import chat.stoat.settings.dsl.SettingsPage
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestOptions
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.nio.charset.StandardCharsets

private const val EmojiPreviewPixels = 128
private const val DefaultEmojiUploadLimit = 500_000L
private val InvalidEmojiNameCharacters = Regex("[^a-z0-9_]+")

internal fun normalizeEmojiName(value: String): String = value
    .lowercase()
    .replace(InvalidEmojiNameCharacters, "_")
    .take(32)

private data class PreparedEmojiFile(
    val file: File,
    val filename: String,
    val contentType: ContentType,
)

class ServerSettingsEmojisViewModel(
    private val context: Application,
) : ViewModel() {
    var emojiName by mutableStateOf("")
        private set
    var emojiModel by mutableStateOf<Uri?>(null)
        private set
    var creating by mutableStateOf(false)
        private set
    var deletingEmojiId by mutableStateOf<String?>(null)
        private set
    var uploadProgress by mutableFloatStateOf(0f)
        private set
    var createError by mutableStateOf<String?>(null)
        private set
    var deleteError by mutableStateOf<String?>(null)
        private set
    var emojiLimit by mutableStateOf<Int?>(null)
        private set
    var emojiUploadLimit by mutableLongStateOf(DefaultEmojiUploadLimit)
        private set
    var createSucceeded by mutableIntStateOf(0)
        private set
    var deleteSucceeded by mutableIntStateOf(0)
        private set

    private var serverId: String? = null

    val savedEmojiName: String
        get() = normalizeEmojiName(emojiName)

    val formattedEmojiUploadLimit: String
        get() = Formatter.formatShortFileSize(context, emojiUploadLimit)

    fun load(serverId: String) {
        if (this.serverId == serverId) return
        this.serverId = serverId

        viewModelScope.launch {
            runCatching { getRootRoute().features.limits }
                .onSuccess { limits ->
                    emojiLimit = limits?.global?.serverEmoji
                    emojiUploadLimit = limits?.defaultUser
                        ?.fileUploadSizeLimits
                        ?.get("emojis")
                        ?: limits?.newUser
                            ?.fileUploadSizeLimits
                            ?.get("emojis")
                                ?: DefaultEmojiUploadLimit
                }
        }
    }

    fun updateEmojiName(value: String) {
        emojiName = value
        createError = null
    }

    fun selectEmoji(uri: Uri) {
        emojiModel = uri
        createError = null

        if (emojiName.isBlank()) {
            suggestedName(uri)?.let { emojiName = it }
        }
    }

    fun resetCreateForm() {
        if (creating) return
        emojiName = ""
        emojiModel = null
        createError = null
        uploadProgress = 0f
    }

    fun create(serverEmojiCount: Int) {
        val targetServerId = serverId ?: return
        val uri = emojiModel ?: return
        val limit = emojiLimit
        val name = savedEmojiName
        if (name.isEmpty() || creating || deletingEmojiId != null) return
        if (limit != null && serverEmojiCount >= limit) return

        viewModelScope.launch {
            creating = true
            createError = null
            uploadProgress = 0f

            runCatching {
                val prepared = prepareEmoji(uri)
                try {
                    val autumnId = uploadToAutumn(
                        file = prepared.file,
                        name = prepared.filename,
                        tag = "emojis",
                        contentType = prepared.contentType,
                        onProgress = { sent, total ->
                            uploadProgress = if (total > 0) {
                                sent.toFloat() / total.toFloat()
                            } else {
                                0f
                            }
                        },
                    )
                    createEmoji(
                        autumnId = autumnId,
                        serverId = targetServerId,
                        name = name,
                    )
                } finally {
                    withContext(Dispatchers.IO) { prepared.file.delete() }
                }
            }.onSuccess {
                emojiName = ""
                emojiModel = null
                createSucceeded++
            }.onFailure { error ->
                createError = when (error.message) {
                    "TooManyEmoji" -> context.getString(R.string.server_settings_emojis_limit_error)
                    else -> error.message
                        ?: context.getString(R.string.server_settings_emojis_create_error)
                }
            }

            creating = false
            uploadProgress = 0f
        }
    }

    fun delete(emojiId: String) {
        if (creating || deletingEmojiId != null) return

        viewModelScope.launch {
            deletingEmojiId = emojiId
            deleteError = null

            runCatching { deleteEmoji(emojiId) }
                .onSuccess { deleteSucceeded++ }
                .onFailure { error ->
                    deleteError = error.message
                        ?: context.getString(R.string.server_settings_emojis_delete_error)
                }

            deletingEmojiId = null
        }
    }

    fun clearDeleteError() {
        deleteError = null
    }

    private fun suggestedName(uri: Uri): String? {
        val displayName = context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: uri.lastPathSegment

        return displayName
            ?.substringBeforeLast('.')
            ?.let(::normalizeEmojiName)
            ?.takeIf(String::isNotBlank)
    }

    private suspend fun prepareEmoji(uri: Uri): PreparedEmojiFile =
        withContext(Dispatchers.IO) {
            val mime = context.contentResolver.getType(uri).orEmpty()
            val isGif = isGif(uri, mime)
            val file = File.createTempFile(
                "stoat-emoji-",
                if (isGif) ".gif" else ".png",
                context.cacheDir,
            )

            try {
                if (isGif) {
                    copyAnimatedEmoji(uri, file)
                    PreparedEmojiFile(
                        file = file,
                        filename = "emoji.gif",
                        contentType = ContentType.Image.GIF,
                    )
                } else {
                    resizeStaticEmoji(uri, file)
                    if (file.length() > emojiUploadLimit) {
                        throw IllegalArgumentException(
                            context.getString(R.string.server_settings_emojis_processing_error)
                        )
                    }
                    PreparedEmojiFile(
                        file = file,
                        filename = "emoji.png",
                        contentType = ContentType.Image.PNG,
                    )
                }
            } catch (error: Throwable) {
                file.delete()
                throw error
            }
        }

    private fun isGif(uri: Uri, mime: String): Boolean {
        if (mime.equals("image/gif", ignoreCase = true)) return true

        return context.contentResolver.openInputStream(uri)?.use { input ->
            val signature = ByteArray(6)
            input.read(signature) == signature.size &&
                    String(signature, StandardCharsets.US_ASCII) in setOf("GIF87a", "GIF89a")
        } == true
    }

    private fun copyAnimatedEmoji(uri: Uri, outputFile: File) {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException(
                context.getString(R.string.server_settings_emojis_file_error)
            )

        input.use { source ->
            outputFile.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val read = source.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > emojiUploadLimit) {
                        throw IllegalArgumentException(
                            context.getString(
                                R.string.server_settings_emojis_animated_too_large,
                                formattedEmojiUploadLimit,
                            )
                        )
                    }
                    output.write(buffer, 0, read)
                }
            }
        }
    }

    private fun resizeStaticEmoji(uri: Uri, outputFile: File) {
        val request = Glide.with(context)
            .asBitmap()
            .load(uri)
            .apply(
                RequestOptions()
                    .override(EmojiPreviewPixels, EmojiPreviewPixels)
                    .downsample(DownsampleStrategy.CENTER_INSIDE)
                    .fitCenter()
                    .disallowHardwareConfig()
            )
            .submit()

        try {
            val bitmap = request.get()
            val compressed = outputFile.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            if (!compressed) {
                throw IllegalArgumentException(
                    context.getString(R.string.server_settings_emojis_processing_error)
                )
            }
        } catch (error: Throwable) {
            if (error is IllegalArgumentException) throw error
            throw IllegalArgumentException(
                context.getString(R.string.server_settings_emojis_processing_error),
                error,
            )
        } finally {
            Glide.with(context).clear(request)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSettingsEmojis(
    navController: NavController,
    serverId: String,
    viewModel: ServerSettingsEmojisViewModel = koinViewModel(),
) {
    val permissions by rememberServerPermissions(serverId)
    val server = StoatAPI.serverCache[serverId]
    val emojis = StoatAPI.emojiCache.values
        .filter { emoji ->
            emoji.parent?.let { parent ->
                parent.type == "Server" && parent.id == serverId
            } == true
        }
        .sortedByDescending(Emoji::id)
    val listState = rememberLazyListState()
    val atLimit = viewModel.emojiLimit?.let { emojis.size >= it } == true
    var showCreateSheet by remember { mutableStateOf(false) }
    var selectedEmoji by remember { mutableStateOf<Emoji?>(null) }
    var deleteTarget by remember { mutableStateOf<Emoji?>(null) }
    var limitNotice by remember { mutableIntStateOf(0) }

    if (
        permissions != null &&
        permissions?.hasPermission(PermissionBit.ManageCustomisation) == false
    ) {
        LaunchedEffect(serverId) { navController.popBackStack() }
        return
    }

    LaunchedEffect(serverId, permissions) {
        if (permissions?.hasPermission(PermissionBit.ManageCustomisation) == true) {
            viewModel.load(serverId)
        }
    }

    BackHandler(enabled = viewModel.creating || viewModel.deletingEmojiId != null) {}

    if (showCreateSheet) {
        ModalBottomSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onDismissRequest = {
                if (!viewModel.creating) {
                    viewModel.resetCreateForm()
                    showCreateSheet = false
                }
            },
        ) {
            CreateEmojiSheet(
                viewModel = viewModel,
                emojiCount = emojis.size,
            )
        }
    }

    selectedEmoji?.let { emoji ->
        ModalBottomSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onDismissRequest = { selectedEmoji = null },
        ) {
            EmojiManagementSheet(
                emoji = emoji,
                onDelete = {
                    selectedEmoji = null
                    viewModel.clearDeleteError()
                    deleteTarget = emoji
                },
            )
        }
    }

    deleteTarget?.let { emoji ->
        EmojiDeleteDialog(
            emoji = emoji,
            deleting = viewModel.deletingEmojiId == emoji.id,
            error = viewModel.deleteError,
            onDelete = { emoji.id?.let(viewModel::delete) },
            onDismiss = {
                if (viewModel.deletingEmojiId == null) {
                    viewModel.clearDeleteError()
                    deleteTarget = null
                }
            },
        )
    }

    SettingsPage(
        navController = navController,
        title = { Text(stringResource(R.string.server_settings_emojis)) },
        scrollable = false,
        onNavigateBack = {
            if (!viewModel.creating && viewModel.deletingEmojiId == null) {
                navController.popBackStack()
            }
        },
        floatingActionButton = {
            if (
                server != null &&
                permissions?.hasPermission(PermissionBit.ManageCustomisation) == true
            ) {
                FloatingActionButton(
                    onClick = {
                        if (atLimit) {
                            limitNotice++
                        } else {
                            showCreateSheet = true
                        }
                    },
                    containerColor = if (atLimit) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    contentColor = if (atLimit) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_24dp),
                        contentDescription = stringResource(R.string.server_settings_emojis_add),
                    )
                }
            }
        },
    ) {
        val createdMessage = stringResource(R.string.server_settings_emojis_created)
        val deletedMessage = stringResource(R.string.server_settings_emojis_deleted)
        val limitMessage = stringResource(R.string.server_settings_emojis_limit_error)

        LaunchedEffect(viewModel.createSucceeded) {
            if (viewModel.createSucceeded > 0) {
                showCreateSheet = false
                showSnackbar(createdMessage)
                listState.animateScrollToItem(0)
            }
        }
        LaunchedEffect(viewModel.deleteSucceeded) {
            if (viewModel.deleteSucceeded > 0) {
                deleteTarget = null
                showSnackbar(deletedMessage)
            }
        }
        LaunchedEffect(limitNotice) {
            if (limitNotice > 0) showSnackbar(limitMessage)
        }

        if (permissions == null || server == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@SettingsPage
        }

        EmojiList(
            emojis = emojis,
            emojiLimit = viewModel.emojiLimit,
            listState = listState,
            enabled = viewModel.deletingEmojiId == null && !viewModel.creating,
            onEmojiClick = { selectedEmoji = it },
        )
    }
}

@Composable
private fun EmojiList(
    emojis: List<Emoji>,
    emojiLimit: Int?,
    listState: LazyListState,
    enabled: Boolean,
    onEmojiClick: (Emoji) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "usage") {
            Text(
                text = emojiLimit?.let { limit ->
                    stringResource(R.string.server_settings_emojis_usage, emojis.size, limit)
                } ?: stringResource(R.string.server_settings_emojis_count, emojis.size),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }

        if (emojis.isEmpty()) {
            item(key = "empty") {
                ServerSettingsEmptyState(
                    icon = R.drawable.ic_mood_24dp,
                    title = R.string.server_settings_emojis_empty_title,
                    description = R.string.server_settings_emojis_empty,
                )
            }
        } else {
            itemsIndexed(
                items = emojis,
                key = { _, emoji -> emoji.id.orEmpty() },
            ) { index, emoji ->
                EmojiListItem(
                    emoji = emoji,
                    first = index == 0,
                    last = index == emojis.lastIndex,
                    enabled = enabled,
                    onClick = { onEmojiClick(emoji) },
                )
                if (index != emojis.lastIndex) Spacer(Modifier.height(2.dp))
            }
        }

        item(key = "bottom-space") { Spacer(Modifier.height(88.dp)) }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun CreateEmojiSheet(
    viewModel: ServerSettingsEmojisViewModel,
    emojiCount: Int,
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(viewModel::selectEmoji)
    }
    val atLimit = viewModel.emojiLimit?.let { emojiCount >= it } == true
    val savedEmojiName = viewModel.savedEmojiName
    val canCreate = !atLimit &&
            !viewModel.creating &&
            savedEmojiName.isNotEmpty() &&
            viewModel.emojiModel != null

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.server_settings_emojis_create_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(120.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = !viewModel.creating) { launcher.launch("image/*") },
        ) {
            if (viewModel.emojiModel != null) {
                GlideImage(
                    model = viewModel.emojiModel,
                    contentDescription = stringResource(
                        R.string.server_settings_emojis_selected_image
                    ),
                    contentScale = ContentScale.Fit,
                    transition = CrossFade,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_24dp),
                        contentDescription = null,
                    )
                    Text(
                        text = stringResource(R.string.server_settings_emojis_choose_image),
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        TextField(
            value = viewModel.emojiName,
            onValueChange = viewModel::updateEmojiName,
            label = { Text(stringResource(R.string.server_settings_emojis_name)) },
            supportingText = {
                Text(
                    stringResource(
                        R.string.server_settings_emojis_saved_as,
                        savedEmojiName.ifEmpty { "—" },
                    )
                )
            },
            singleLine = true,
            enabled = !viewModel.creating,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = stringResource(
                R.string.server_settings_emojis_image_help,
                viewModel.formattedEmojiUploadLimit,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )

        Text(
            text = viewModel.emojiLimit?.let { limit ->
                stringResource(
                    R.string.server_settings_emojis_slots_remaining,
                    (limit - emojiCount).coerceAtLeast(0),
                )
            } ?: stringResource(R.string.server_settings_emojis_count, emojiCount),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )

        AnimatedVisibility(viewModel.creating && viewModel.uploadProgress > 0f) {
            LinearProgressIndicator(
                progress = { viewModel.uploadProgress },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        AnimatedVisibility(viewModel.createError != null) {
            Text(
                text = viewModel.createError
                    ?: stringResource(R.string.server_settings_emojis_create_error),
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Button(
            enabled = canCreate,
            onClick = { viewModel.create(emojiCount) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (viewModel.creating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(stringResource(R.string.server_settings_emojis_create))
            }
        }
    }
}

@Composable
private fun EmojiListItem(
    emoji: Emoji,
    first: Boolean,
    last: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val creator = emoji.creatorID?.let(StoatAPI.userCache::get)

    SettingsListItem(
        first = first,
        last = last,
        headlineContent = { Text(":${emoji.name.orEmpty()}:") },
        supportingContent = creator?.let { user ->
            {
                Text(User.resolveDefaultName(user))
            }
        },
        leadingContent = {
            EmojiImage(
                emoji = emoji,
                size = 44,
            )
        },
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
    )
}

@Composable
private fun EmojiManagementSheet(
    emoji: Emoji,
    onDelete: () -> Unit,
) {
    val creator = emoji.creatorID?.let(StoatAPI.userCache::get)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
    ) {
        EmojiImage(emoji = emoji, size = 96)
        Text(
            text = ":${emoji.name.orEmpty()}:",
            style = MaterialTheme.typography.headlineSmall,
        )

        creator?.let { user ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UserAvatar(
                    username = User.resolveDefaultName(user),
                    userId = user.id.orEmpty(),
                    avatar = user.avatar,
                    size = 24.dp,
                )
                Text(
                    stringResource(
                        R.string.server_settings_emojis_created_by,
                        User.resolveDefaultName(user),
                    )
                )
            }
        }

        Button(
            onClick = onDelete,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.server_settings_emojis_delete))
        }
    }
}

@Composable
private fun EmojiDeleteDialog(
    emoji: Emoji,
    deleting: Boolean,
    error: String?,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.server_settings_emojis_delete_title)) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                EmojiImage(emoji = emoji, size = 64)
                Text(
                    text = stringResource(
                        R.string.server_settings_emojis_delete_description,
                        emoji.name.orEmpty(),
                    ),
                    textAlign = TextAlign.Center,
                )
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !deleting,
                onClick = onDelete,
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
                    Text(stringResource(R.string.server_settings_emojis_delete))
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled = !deleting,
                onClick = onDismiss,
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun EmojiImage(
    emoji: Emoji,
    size: Int,
) {
    RemoteImage(
        url = "$STOAT_FILES/emojis/${emoji.id}",
        description = stringResource(
            R.string.server_settings_emojis_image_description,
            emoji.name.orEmpty(),
        ),
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(size.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
    )
}
