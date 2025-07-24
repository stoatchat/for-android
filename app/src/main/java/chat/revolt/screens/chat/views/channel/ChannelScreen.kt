package chat.revolt.screens.chat.views.channel

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.res.Configuration
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import chat.revolt.R
import chat.revolt.RevoltApplication
import chat.revolt.activities.RevoltTweenDp
import chat.revolt.activities.RevoltTweenFloat
import chat.revolt.activities.RevoltTweenInt
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.ChannelUtils
import chat.revolt.api.internals.PermissionBit
import chat.revolt.api.internals.has
import chat.revolt.api.routes.channel.react
import chat.revolt.api.routes.microservices.autumn.FileArgs
import chat.revolt.api.schemas.ChannelType
import chat.revolt.api.schemas.Message
import chat.revolt.api.settings.FeatureFlags
import chat.revolt.callbacks.Action
import chat.revolt.callbacks.ActionChannel
import chat.revolt.composables.chat.DateDivider
import chat.revolt.composables.chat.Message
import chat.revolt.composables.chat.MessageField
import chat.revolt.composables.chat.SystemMessage
import chat.revolt.composables.emoji.EmojiPicker
import chat.revolt.composables.generic.GroupIcon
import chat.revolt.composables.generic.PresenceBadge
import chat.revolt.composables.generic.UserAvatar
import chat.revolt.composables.generic.UserAvatarWidthPlaceholder
import chat.revolt.composables.generic.presenceFromStatus
import chat.revolt.composables.media.MediaPickerGateway
import chat.revolt.composables.screens.chat.AttachmentManager
import chat.revolt.composables.screens.chat.ChannelIcon
import chat.revolt.composables.screens.chat.ReplyManager
import chat.revolt.composables.screens.chat.TypingIndicator
import chat.revolt.composables.screens.chat.atoms.RegularMessage
import chat.revolt.composables.screens.chat.molecules.JoinVoiceChannelButton
import chat.revolt.composables.skeletons.MessageSkeleton
import chat.revolt.composables.skeletons.MessageSkeletonVariant
import chat.revolt.internals.extensions.rememberChannelPermissions
import chat.revolt.internals.extensions.zero
import chat.revolt.screens.chat.LocalIsConnected
import chat.revolt.sheets.ChannelInfoSheet
import chat.revolt.sheets.MessageContextSheet
import chat.revolt.sheets.ReactSheet
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import java.io.File
import kotlin.math.max

sealed class ChannelScreenItem {
    data class RegularMessage(val message: Message) : ChannelScreenItem()
    data class ProspectiveMessage(val message: Message) : ChannelScreenItem()
    data class FailedMessage(val message: Message) : ChannelScreenItem()
    data class SystemMessage(val message: Message) : ChannelScreenItem()
    data class DateDivider(val instant: Instant) : ChannelScreenItem()
    data class LoadTrigger(val after: String?, val before: String?) :
        ChannelScreenItem()

    data object Loading : ChannelScreenItem()
}

sealed class ChannelScreenActivePane {
    data object None : ChannelScreenActivePane()
    data object EmojiPicker : ChannelScreenActivePane()
    data object AttachmentPicker : ChannelScreenActivePane()
}

private fun pxAsDp(px: Int): Dp {
    return (
            px / (
                    RevoltApplication.instance.resources
                        .displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT
                    )
            ).dp
}

private const val NOT_ENOUGH_SPACE_FOR_PANES_THRESHOLD = 500

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChannelScreen(
    channelId: String,
    onToggleDrawer: () -> Unit,
    useDrawer: Boolean,
    useBackButton: Boolean = false,
    drawerGestureEnabled: Boolean = true,
    setDrawerGestureEnabled: (Boolean) -> Unit = {},
    drawerIsOpen: Boolean = false,
    backButtonAction: (() -> Unit)? = null,
    useChatUI: Boolean = false,
    viewModel: ChannelScreenViewModel = hiltViewModel()
) {
    // <editor-fold desc="State and effects">
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val config = LocalConfiguration.current

    LaunchedEffect(Unit) {
        viewModel.listenToWsEvents()
    }

    DisposableEffect(Unit) {
        val job = scope.launch { viewModel.listenToUiCallbacks() }

        onDispose {
            job.cancel()
        }
    }
    // </editor-fold>
    // <editor-fold desc="Load/switch channel">
    val channelPermissions by rememberChannelPermissions(channelId, viewModel.ensuredSelfMember)

    LaunchedEffect(channelId) {
        viewModel.switchChannel(channelId)
    }
    // </editor-fold>
    // <editor-fold desc="Keyboard height handling">
    val imeTarget = WindowInsets.imeAnimationTarget.getBottom(LocalDensity.current)
    val navigationBarsInset = WindowInsets.navigationBars.getBottom(LocalDensity.current)
    val imeCurrentInset = WindowInsets.ime.getBottom(LocalDensity.current)
    var imeInTransition by remember { mutableStateOf(false) }

    var emojiSearchFocused by remember { mutableStateOf(false) }

    val fallbackKeyboardHeight by animateIntAsState(
        targetValue = if (viewModel.activePane == ChannelScreenActivePane.None && !imeInTransition) navigationBarsInset else viewModel.keyboardHeight,
        label = "keyboardHeight"
    )

    val notEnoughSpaceForPanes by remember {
        derivedStateOf {
            viewModel.keyboardHeight < NOT_ENOUGH_SPACE_FOR_PANES_THRESHOLD
        }
    }

    LaunchedEffect(imeTarget) {
        if (imeTarget > 0) {
            viewModel.updateSaveKeyboardHeight(imeTarget)
        } else {
            imeInTransition = false
        }
    }

    LaunchedEffect(Unit) {
        if (config.keyboard and Configuration.KEYBOARD_QWERTY != 0) {
            viewModel.usesPhysicalKeyboard()
        }
    }
    // </editor-fold>
    // <editor-fold desc="Attachment handling">
    val processFileUri: (Uri, String?) -> Unit = remember {
        { uri, pickerIdentifier ->
            DocumentFile.fromSingleUri(context, uri)?.let { file ->
                val mFile = File(context.cacheDir, file.name ?: "attachment")

                mFile.outputStream().use { output ->
                    @Suppress("Recycle")
                    context.contentResolver.openInputStream(uri)?.copyTo(output)
                }

                // If the file is already pending and was picked from the inbuilt picker, remove it.
                // This is so you can "toggle" the file in the picker.
                // If the file was picked via DocumentsUI we don't want toggling functionality as
                // if you specifically opened it from DocumentsUI you probably want to send it anyway.
                if (
                    pickerIdentifier != null &&
                    viewModel.draftAttachments.any { it.pickerIdentifier == pickerIdentifier }
                ) {
                    viewModel.draftAttachments.removeIf { it.pickerIdentifier == pickerIdentifier }
                    return@let
                }

                viewModel.draftAttachments.add(
                    FileArgs(
                        file = mFile,
                        contentType = file.type ?: "application/octet-stream",
                        filename = file.name ?: "attachment",
                        pickerIdentifier = pickerIdentifier
                    )
                )
            }
        }
    }

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uriList ->
        uriList.let { list ->
            list.forEach { uri ->
                processFileUri(uri, null)
            }
        }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) {
        it.let { list ->
            list.forEach { uri ->
                processFileUri(uri, null)
            }
        }
    }

    val capturedPhotoUri = rememberSaveable { mutableStateOf<Uri?>(null) }
    val pickCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { uriUpdated ->
        if (uriUpdated) {
            capturedPhotoUri.value?.let { uri ->
                processFileUri(uri, null)
            }
        }
    }

    val openCameraCallback = cb@{
        // Create a new content URI to store the captured image.
        val contentResolver =
            context.contentResolver
        val contentValues = ContentValues().apply {
            put(
                MediaStore.MediaColumns.DISPLAY_NAME,
                "RVL_${System.currentTimeMillis()}.jpg"
            )
            put(
                MediaStore.MediaColumns.MIME_TYPE,
                "image/jpeg"
            )
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES
            )
        }

        try {
            capturedPhotoUri.value =
                contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(
                    R.string.file_picker_chip_camera_failed
                ),
                Toast.LENGTH_SHORT
            ).show()

            return@cb
        }

        try {
            capturedPhotoUri.value?.let { uri ->
                pickCameraLauncher.launch(uri)
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(
                    R.string.file_picker_chip_camera_none_installed
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val openDocumentPickerCallback = {
        pickFileLauncher.launch(arrayOf("*/*"))
    }

    val openPhotoPickerCallback = {
        pickMediaLauncher.launch(
            PickVisualMediaRequest(
                mediaType = ActivityResultContracts.PickVisualMedia.ImageAndVideo
            )
        )
    }
    // </editor-fold>
    // <editor-fold desc="UI elements">
    val lazyListState = rememberLazyListState()
    var disableScroll by remember { mutableStateOf(false) }

    val isScrolledToBottom = remember(lazyListState) {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex <= 6
        }
    }

    val isNearTop = remember(lazyListState) {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex =
                (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
            val buffer = 6

            lastVisibleItemIndex > (totalItemsNumber - buffer)
        }
    }

    val scrollDownFABPadding by animateDpAsState(
        if (viewModel.typingUsers.isNotEmpty()) 25.dp else 0.dp,
        animationSpec = RevoltTweenDp,
        label = "ScrollDownFABPadding"
    )

    // Load more messages when we reach the top of the list
    // TODO: Temp - use LoadTrigger instead

    LaunchedEffect(isNearTop) {
        snapshotFlow { isNearTop.value }
            .distinctUntilChanged()
            .collect { isNearTop ->
                if (isNearTop) {
                    Log.d("ChannelScreen", "Loading more messages")
                    viewModel.loadMessages(before = viewModel.items.lastOrNull {
                        it is ChannelScreenItem.RegularMessage || it is ChannelScreenItem.SystemMessage
                    }?.let {
                        when (it) {
                            is ChannelScreenItem.RegularMessage -> it.message.id
                            is ChannelScreenItem.SystemMessage -> it.message.id
                            else -> null
                        }
                    }, amount = 50)
                }
            }
    }
    // </editor-fold>
    // <editor-fold desc="Sheets">
    var channelInfoSheetShown by remember { mutableStateOf(false) }
    if (channelInfoSheetShown) {
        val channelInfoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            sheetState = channelInfoSheetState,
            onDismissRequest = {
                channelInfoSheetShown = false
            }
        ) {
            ChannelInfoSheet(
                channelId = channelId,
                onHideSheet = {
                    channelInfoSheetState.hide()
                    channelInfoSheetShown = false
                }
            )
        }
    }

    var messageContextSheetShown by remember { mutableStateOf(false) }
    var messageContextSheetTarget by remember { mutableStateOf("") }
    if (messageContextSheetShown) {
        val messageContextSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            sheetState = messageContextSheetState,
            onDismissRequest = {
                messageContextSheetShown = false
            }
        ) {
            MessageContextSheet(
                messageId = messageContextSheetTarget,
                onHideSheet = {
                    messageContextSheetState.hide()
                    messageContextSheetShown = false
                },
                onReportMessage = {
                    scope.launch {
                        ActionChannel.send(Action.ReportMessage(messageContextSheetTarget))
                    }
                }
            )
        }
    }

    var reactSheetShown by remember { mutableStateOf(false) }
    var reactSheetTarget by remember { mutableStateOf("") }
    if (reactSheetShown) {
        val reactSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            sheetState = reactSheetState,
            onDismissRequest = {
                reactSheetShown = false
            }
        ) {
            ReactSheet(reactSheetTarget) {
                if (it == null) return@ReactSheet

                scope.launch {
                    react(channelId, reactSheetTarget, it)
                    reactSheetState.hide()
                    reactSheetShown = false
                }
            }
        }
    }
    // </editor-fold>
    // <editor-fold desc="Begin UI composition">
    Scaffold(
        contentWindowInsets = WindowInsets.zero,
        topBar = {
            Column {
                AnimatedVisibility(LocalIsConnected.current) {
                    Spacer(
                        Modifier
                            .height(
                                WindowInsets.statusBars.asPaddingValues()
                                    .calculateTopPadding()
                            )
                    )
                }
                TopAppBar(
                    modifier = Modifier.clickable {
                        channelInfoSheetShown = true
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            viewModel.channel?.let {
                                when (it.channelType) {
                                    ChannelType.DirectMessage -> {
                                        val partner =
                                            RevoltAPI.userCache[ChannelUtils.resolveDMPartner(it)]
                                        UserAvatar(
                                            username = it.name ?: stringResource(R.string.unknown),
                                            userId = ChannelUtils.resolveDMPartner(it) ?: "",
                                            size = 24.dp,
                                            presenceSize = 12.dp,
                                            avatar = partner?.avatar
                                        )
                                    }

                                    ChannelType.Group -> {
                                        GroupIcon(
                                            name = it.name ?: stringResource(R.string.unknown),
                                            size = 24.dp,
                                            icon = it.icon
                                        )
                                    }

                                    else -> {
                                        ChannelIcon(
                                            channelType = it.channelType ?: ChannelType.TextChannel,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .alpha(0.8f)
                                        )
                                    }
                                }

                                CompositionLocalProvider(
                                    LocalTextStyle provides LocalTextStyle.current.copy(
                                        fontSize = 20.sp,
                                        lineHeightStyle = LineHeightStyle(
                                            alignment = LineHeightStyle.Alignment.Bottom,
                                            trim = LineHeightStyle.Trim.LastLineBottom
                                        )
                                    )
                                ) {
                                    when (it.channelType) {
                                        ChannelType.TextChannel, ChannelType.VoiceChannel, ChannelType.Group -> Text(
                                            it.name ?: stringResource(R.string.unknown),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        ChannelType.SavedMessages -> Text(
                                            stringResource(R.string.channel_notes),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        ChannelType.DirectMessage -> Text(
                                            ChannelUtils.resolveName(it)
                                                ?: stringResource(R.string.unknown),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        else -> Text(
                                            stringResource(R.string.unknown),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (it.channelType == ChannelType.DirectMessage) {
                                    val partner =
                                        RevoltAPI.userCache[ChannelUtils.resolveDMPartner(it)]
                                    PresenceBadge(
                                        presence = presenceFromStatus(
                                            partner?.status?.presence,
                                            online = partner?.online == true
                                        ),
                                        size = 12.dp
                                    )
                                }

                                Icon(
                                    painter = painterResource(R.drawable.icn_keyboard_arrow_right_24dp),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .alpha(0.5f)
                                )
                            }
                        }
                    },
                    windowInsets = if (useChatUI) WindowInsets.statusBars else WindowInsets.zero,
                    navigationIcon = {
                        if (useDrawer) {
                            IconButton(onClick = onToggleDrawer) {
                                Icon(
                                    painter = painterResource(R.drawable.icn_menu_24dp),
                                    contentDescription = stringResource(id = R.string.menu)
                                )
                            }
                        }
                        if (useBackButton) {
                            IconButton(onClick = backButtonAction ?: {}) {
                                Icon(
                                    painter = painterResource(R.drawable.icn_arrow_back_24dp),
                                    contentDescription = stringResource(id = R.string.back)
                                )
                            }
                        }
                    }
                )
            }
        }
    ) { pv ->
        Crossfade(
            targetState = viewModel.ageGateUnlocked,
            label = "ageGateUnlocked"
        ) { ageGateUnlocked ->
            if (ageGateUnlocked == false) {
                ChannelScreenAgeGate(
                    onAccept = {
                        scope.launch {
                            viewModel.unlockAgeGate()
                        }
                    },
                    onDeny = {
                        onToggleDrawer()
                    }
                )
            } else if (ageGateUnlocked == null) {
                Box(Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                }
            } else if (ageGateUnlocked == true) {
                Column(
                    modifier = Modifier
                        .padding(pv)
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        LazyColumn(
                            state = lazyListState,
                            userScrollEnabled = !disableScroll,
                            reverseLayout = true,
                            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
                        ) {

                            // If we don't have a guaranteed first item, the message list will not scroll
                            // to the bottom when new messages are added. Evil hack to make our other evil
                            // hack (clear/addAll) work. Too bad!
                            item(key = "guaranteed_first") {
                                Box {}
                            }

                            items(
                                viewModel.items.size,
                                key = { index ->
                                    when (val item = viewModel.items[index]) {
                                        is ChannelScreenItem.RegularMessage -> item.message.id!!
                                        is ChannelScreenItem.ProspectiveMessage -> item.message.id!!
                                        is ChannelScreenItem.FailedMessage -> item.message.id!!
                                        is ChannelScreenItem.SystemMessage -> item.message.id!!
                                        is ChannelScreenItem.DateDivider -> item.instant.toEpochMilliseconds()
                                        is ChannelScreenItem.LoadTrigger -> index
                                        is ChannelScreenItem.Loading -> index
                                    }
                                },
                                contentType = { index ->
                                    when (viewModel.items.getOrNull(index)) {
                                        null -> null
                                        is ChannelScreenItem.RegularMessage -> "RegularMessage"
                                        is ChannelScreenItem.ProspectiveMessage -> "ProspectiveMessage"
                                        is ChannelScreenItem.FailedMessage -> "FailedMessage"
                                        is ChannelScreenItem.SystemMessage -> "SystemMessage"
                                        is ChannelScreenItem.DateDivider -> "DateDivider"
                                        is ChannelScreenItem.LoadTrigger -> "LoadTrigger"
                                        is ChannelScreenItem.Loading -> "Loading"
                                    }
                                }
                            ) { index ->
                                when (val item = viewModel.items[index]) {
                                    is ChannelScreenItem.RegularMessage -> {
                                        RegularMessage(
                                            item.message,
                                            viewModel.channel,
                                            drawerIsOpen = drawerIsOpen,
                                            setDrawerGestureEnabled = {
                                                setDrawerGestureEnabled(it)
                                            },
                                            setDisableScroll = {
                                                disableScroll = it
                                            },
                                            showMessageBottomSheet = {
                                                messageContextSheetTarget = it
                                                messageContextSheetShown = true
                                            },
                                            showReactBottomSheet = {
                                                item.message.id?.let {
                                                    reactSheetTarget = it
                                                    reactSheetShown = true
                                                }
                                            },
                                            putTextAtCursorPosition = viewModel::putAtCursorPosition,
                                            replyToMessage = viewModel::addReplyTo,
                                            scope = scope
                                        )
                                    }

                                    is ChannelScreenItem.ProspectiveMessage -> {
                                        Box(Modifier.alpha(0.5f)) {
                                            Message(
                                                message = item.message,
                                                onMessageContextMenu = {
                                                    // TODO Context menu that allows you to cancel send
                                                },
                                                onAvatarClick = {},
                                                onNameClick = {},
                                                canReply = false,
                                                onReply = {},
                                                onAddReaction = {}
                                            )
                                        }
                                    }

                                    is ChannelScreenItem.FailedMessage -> {
                                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
                                            Column {
                                                Message(
                                                    message = item.message,
                                                    onMessageContextMenu = {},
                                                    onAvatarClick = {},
                                                    onNameClick = {},
                                                    canReply = false,
                                                    onReply = {},
                                                    onAddReaction = {}
                                                )
                                                Row {
                                                    UserAvatarWidthPlaceholder()
                                                    Text(
                                                        stringResource(R.string.message_failed_to_send),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.error.copy(
                                                            alpha = 0.8f
                                                        ),
                                                        modifier = Modifier.padding(
                                                            top = 4.dp,
                                                            bottom = 4.dp,
                                                            start = 20.dp
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    is ChannelScreenItem.SystemMessage -> {
                                        SystemMessage(message = item.message)
                                    }

                                    is ChannelScreenItem.DateDivider -> {
                                        DateDivider(instant = item.instant)
                                    }

                                    is ChannelScreenItem.LoadTrigger -> {
                                        LaunchedEffect(Unit) {
                                            Log.d(
                                                "ChannelScreen",
                                                "LoadTrigger: After ${item.after} Before ${item.before}"
                                            )
                                        }
                                    }

                                    is ChannelScreenItem.Loading -> {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .shimmer(rememberShimmer(ShimmerBounds.Window)),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            MessageSkeleton(MessageSkeletonVariant.One)
                                            MessageSkeleton(MessageSkeletonVariant.Two)
                                            MessageSkeleton(MessageSkeletonVariant.Three)
                                        }
                                    }
                                }
                            }
                        }

                        TypingIndicator(
                            users = viewModel.typingUsers,
                            serverId = viewModel.channel?.server
                        )

                        androidx.compose.animation.AnimatedVisibility(
                            !isScrolledToBottom.value,
                            enter = slideInVertically(
                                animationSpec = RevoltTweenInt,
                                initialOffsetY = { it }
                            ) + fadeIn(animationSpec = RevoltTweenFloat),
                            exit = slideOutVertically(
                                animationSpec = RevoltTweenInt,
                                targetOffsetY = { it }
                            ) + fadeOut(animationSpec = RevoltTweenFloat)
                        ) {
                            SmallFloatingActionButton(
                                modifier = Modifier
                                    .padding(bottom = scrollDownFABPadding)
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp),
                                onClick = {
                                    scope.launch {
                                        lazyListState.animateScrollToItem(0)
                                    }
                                },
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.icn_south_24dp),
                                    contentDescription = stringResource(R.string.scroll_to_bottom)
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(8.dp)
                        ) {
                            if (viewModel.showPhysicalKeyboardSpark) {
                                Card {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            stringResource(R.string.spark_keyboard_shortcuts),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            buildAnnotatedString {
                                                val raw =
                                                    stringResource(R.string.spark_keyboard_shortcuts_description)
                                                val before = raw.substringBefore("%1\$s")
                                                val after = raw.substringAfter("%1\$s")

                                                append(before)
                                                appendInlineContent("metaKey", "Meta")
                                                append(" + /")
                                                append(after)
                                            },
                                            inlineContent = mapOf(
                                                "metaKey" to InlineTextContent(
                                                    placeholder = Placeholder(
                                                        width = 1.em,
                                                        height = 1.em,
                                                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                                                    )
                                                ) {
                                                    with(LocalDensity.current) {
                                                        Image(
                                                            painterResource(R.drawable.ic_meta_key_24dp),
                                                            contentDescription = null,
                                                            colorFilter = ColorFilter.tint(
                                                                LocalContentColor.current
                                                            )
                                                        )
                                                    }
                                                }
                                            ),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Button(
                                                onClick = {
                                                    viewModel.dismissPhysicalKeyboardSpark()
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(stringResource(R.string.spark_keyboard_shortcuts_dismiss))
                                            }
                                            TextButton(
                                                onClick = {
                                                    (context as Activity).requestShowKeyboardShortcuts()
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(stringResource(R.string.spark_keyboard_shortcuts_cta))
                                            }
                                        }
                                    }
                                }
                            }

                            if (viewModel.channel?.channelType == ChannelType.VoiceChannel
                                && FeatureFlags.voiceChannels2_0Granted
                            ) {
                                JoinVoiceChannelButton(channelId)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedContent(
                            targetState = viewModel.denyMessageField,
                            label = "denyMessageField"
                        ) { deny ->
                            if (!deny) {
                                Column {
                                    AnimatedVisibility(
                                        visible = viewModel.draftReplyTo.isNotEmpty() && !viewModel.denyMessageField
                                    ) {
                                        ReplyManager(
                                            replies = viewModel.draftReplyTo,
                                            onToggleMention = {
                                                scope.launch { viewModel.toggleMentionOnReply(it.id) }
                                            },
                                            onRemove = {
                                                viewModel.draftReplyTo.remove(it)
                                            }
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = viewModel.draftAttachments.isNotEmpty() && !viewModel.denyMessageField
                                    ) {
                                        AttachmentManager(
                                            attachments = viewModel.draftAttachments,
                                            uploading = viewModel.attachmentUploadProgress > 0,
                                            uploadProgress = viewModel.attachmentUploadProgress,
                                            canRemove = true,
                                            canPreview = true,
                                            onRemove = {
                                                viewModel.draftAttachments.remove(it)
                                            },
                                            onToggleSpoiler = {
                                                val index = viewModel.draftAttachments
                                                    .indexOfFirst { a -> a.pickerIdentifier == it.pickerIdentifier }

                                                if (index != -1) {
                                                    val attachment =
                                                        viewModel.draftAttachments[index]
                                                    viewModel.draftAttachments[index] =
                                                        attachment.copy(
                                                            spoiler = !attachment.spoiler
                                                        )
                                                }
                                            }
                                        )
                                    }

                                    AnimatedVisibility(visible = viewModel.editingMessage != null) {
                                        Row(Modifier.padding(start = 24.dp, top = 8.dp)) {
                                            AssistChip(
                                                onClick = {
                                                    viewModel.editingMessage = null
                                                    viewModel.putDraftContent("", true)
                                                },
                                                label = {
                                                    Text(stringResource(R.string.message_field_editing_message))
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        painter = painterResource(R.drawable.icn_edit_24dp),
                                                        contentDescription = null
                                                    )
                                                },
                                                trailingIcon = {
                                                    Icon(
                                                        painter = painterResource(R.drawable.icn_close_24dp),
                                                        contentDescription = stringResource(R.string.message_field_editing_message_cancel_alt),
                                                        tint = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.alpha(0.8f)
                                                    )
                                                }
                                            )
                                        }
                                    }

                                    MessageField(
                                        initialValue = viewModel.initialTextFieldValue,
                                        initialValueDirtyMarker = viewModel.initialTextFieldValueDirtyMarker,
                                        onValueChange = viewModel::putDraftContent,
                                        onAddAttachment = {
                                            if (viewModel.activePane == ChannelScreenActivePane.AttachmentPicker) {
                                                viewModel.activePane = ChannelScreenActivePane.None
                                            } else {
                                                viewModel.activePane =
                                                    ChannelScreenActivePane.AttachmentPicker
                                            }
                                        },
                                        onCommitAttachment = {
                                            processFileUri(it, null)
                                        },
                                        onPickEmoji = {
                                            if (viewModel.activePane == ChannelScreenActivePane.EmojiPicker) {
                                                viewModel.activePane = ChannelScreenActivePane.None
                                            } else {
                                                viewModel.activePane =
                                                    ChannelScreenActivePane.EmojiPicker
                                            }
                                        },
                                        onSendMessage = viewModel::sendPendingMessage,
                                        channelType = viewModel.channel?.channelType
                                            ?: ChannelType.TextChannel,
                                        channelName = viewModel.channel?.let { channel ->
                                            ChannelUtils.resolveName(channel)
                                        }
                                            ?: stringResource(R.string.unknown),
                                        onFocusChange = { isFocused ->
                                            if (isFocused && viewModel.activePane != ChannelScreenActivePane.None) {
                                                viewModel.activePane = ChannelScreenActivePane.None
                                                imeInTransition = true
                                            }
                                        },
                                        forceSendButton = viewModel.draftAttachments.isNotEmpty(),
                                        canAttach = (channelPermissions has PermissionBit.UploadFiles) && viewModel.editingMessage == null,
                                        serverId = viewModel.channel?.server,
                                        channelId = channelId,
                                        failedValidation = viewModel.draftContent.length > 2000,
                                        valueIsBlank = viewModel.draftContent.isBlank(),
                                        cancelEdit = {
                                            viewModel.editingMessage = null
                                            viewModel.putDraftContent("", true)
                                        }
                                    )

                                    DropdownMenu(
                                        expanded = viewModel.activePane == ChannelScreenActivePane.AttachmentPicker && notEnoughSpaceForPanes,
                                        onDismissRequest = {
                                            viewModel.activePane = ChannelScreenActivePane.None
                                        }
                                    ) {
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Icon(
                                                    painter = painterResource(R.drawable.icn_attach_file_24dp),
                                                    contentDescription = null // Provided by text below
                                                )
                                            },
                                            text = { Text(stringResource(R.string.file_picker_chip_documents)) },
                                            onClick = {
                                                openDocumentPickerCallback()
                                                viewModel.activePane = ChannelScreenActivePane.None
                                            }
                                        )
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Icon(
                                                    painter = painterResource(R.drawable.icn_camera_24dp),
                                                    contentDescription = null // Provided by text below
                                                )
                                            },
                                            text = { Text(stringResource(R.string.file_picker_chip_camera)) },
                                            onClick = {
                                                openCameraCallback()
                                                viewModel.activePane = ChannelScreenActivePane.None
                                            }
                                        )
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Icon(
                                                    painter = painterResource(R.drawable.icn_photo_library_24dp),
                                                    contentDescription = null // Provided by text below
                                                )
                                            },
                                            text = { Text(stringResource(R.string.file_picker_chip_photo_picker)) },
                                            onClick = {
                                                openPhotoPickerCallback()
                                                viewModel.activePane = ChannelScreenActivePane.None
                                            }
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 32.dp, vertical = 16.dp)
                                ) {
                                    Text(
                                        stringResource(viewModel.denyMessageFieldReasonResource),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        if (viewModel.activePane == ChannelScreenActivePane.None && !imeInTransition) {
                            Spacer(
                                Modifier
                                    .imePadding()
                                    .navigationBarsPadding()
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                            )
                        } else {
                            if (!notEnoughSpaceForPanes) {
                                Box(
                                    Modifier
                                        .heightIn(min = pxAsDp(fallbackKeyboardHeight))
                                ) {
                                    Box(
                                        Modifier.then(
                                            if (emojiSearchFocused) {
                                                Modifier.requiredHeight(
                                                    pxAsDp(
                                                        max(
                                                            imeCurrentInset * 2,
                                                            fallbackKeyboardHeight
                                                        )
                                                    )
                                                )
                                            } else {
                                                Modifier.requiredHeight(
                                                    pxAsDp(
                                                        fallbackKeyboardHeight
                                                    )
                                                )
                                            }
                                        )
                                    ) {
                                        when (viewModel.activePane) {
                                            ChannelScreenActivePane.EmojiPicker -> {
                                                BackHandler(enabled = viewModel.activePane == ChannelScreenActivePane.EmojiPicker) {
                                                    viewModel.activePane =
                                                        ChannelScreenActivePane.None
                                                }

                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(MaterialTheme.colorScheme.surfaceContainer)
                                                        .padding(4.dp)
                                                        .navigationBarsPadding()
                                                ) {
                                                    EmojiPicker(
                                                        onEmojiSelected = viewModel::putAtCursorPosition,
                                                        bottomInset = pxAsDp(
                                                            max(
                                                                imeCurrentInset - navigationBarsInset,
                                                                0
                                                            )
                                                        ),
                                                        onSearchFocus = {
                                                            emojiSearchFocused = it
                                                        }
                                                    )
                                                }
                                            }

                                            ChannelScreenActivePane.AttachmentPicker -> {
                                                BackHandler(enabled = viewModel.activePane == ChannelScreenActivePane.AttachmentPicker) {
                                                    viewModel.activePane =
                                                        ChannelScreenActivePane.None
                                                }

                                                MediaPickerGateway(
                                                    onOpenPhotoPicker = {
                                                        openPhotoPickerCallback()
                                                        viewModel.activePane =
                                                            ChannelScreenActivePane.None
                                                    },
                                                    onOpenDocumentPicker = {
                                                        openDocumentPickerCallback()
                                                        viewModel.activePane =
                                                            ChannelScreenActivePane.None
                                                    },
                                                    onOpenCamera = {
                                                        openCameraCallback()
                                                        viewModel.activePane =
                                                            ChannelScreenActivePane.None
                                                    },
                                                )
                                            }

                                            else -> {
                                                // Do nothing
                                            }
                                        }
                                    }
                                    Box(Modifier.imePadding())
                                }
                            } else {
                                if (viewModel.activePane == ChannelScreenActivePane.EmojiPicker) {
                                    BackHandler(enabled = viewModel.activePane == ChannelScreenActivePane.EmojiPicker) {
                                        viewModel.activePane =
                                            ChannelScreenActivePane.None
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(600.dp)
                                            .background(MaterialTheme.colorScheme.surfaceContainer)
                                            .padding(4.dp)
                                            .navigationBarsPadding()
                                    ) {
                                        EmojiPicker(
                                            onEmojiSelected = viewModel::putAtCursorPosition,
                                            bottomInset = pxAsDp(
                                                max(
                                                    imeCurrentInset - navigationBarsInset,
                                                    0
                                                )
                                            ),
                                            onSearchFocus = {
                                                emojiSearchFocused = it
                                            }
                                        )
                                    }
                                }
                                Box(
                                    Modifier
                                        .imePadding()
                                        .navigationBarsPadding()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    // </editor-fold>
}