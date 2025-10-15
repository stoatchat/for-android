package chat.zekochat.screens.chat

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.zekochat.R
import chat.zekochat.api.PeptideAPI
import chat.zekochat.api.internals.DirectMessages
import chat.zekochat.api.realtime.DisconnectionState
import chat.zekochat.api.realtime.RealtimeSocket
import chat.zekochat.api.routes.push.subscribePush
import chat.zekochat.callbacks.Action
import chat.zekochat.callbacks.ActionChannel
import chat.zekochat.composables.chat.DisconnectedNotice
import chat.zekochat.composables.generic.PepTextButton
import chat.zekochat.composables.screens.chat.drawer.ChannelSideDrawer
import chat.zekochat.dialogs.NotificationRationaleDialog
import chat.zekochat.internals.Changelogs
import chat.zekochat.persistence.KVStorage
import chat.zekochat.screens.chat.dialogs.safety.ReportMessageDialog
import chat.zekochat.screens.chat.dialogs.safety.ReportServerDialog
import chat.zekochat.screens.chat.dialogs.safety.ReportUserDialog
import chat.zekochat.screens.chat.views.FriendsScreen
import chat.zekochat.screens.chat.views.NoCurrentChannelScreen
import chat.zekochat.screens.chat.views.channel.ChannelScreen
import chat.zekochat.screens.settings.SettingsScreen
import chat.zekochat.sheets.AddServerSheet
import chat.zekochat.sheets.ChangelogSheet
import chat.zekochat.sheets.EmoteInfoSheet
import chat.zekochat.sheets.LinkInfoSheet
import chat.zekochat.sheets.ReactionInfoSheet
import chat.zekochat.sheets.ServerContextSheet
import chat.zekochat.sheets.StatusSheet
import chat.zekochat.sheets.UserInfoSheet
import chat.zekochat.sheets.WebHookUserSheet
import chat.zekochat.sheets.spark.SwipeToReplySparkSheet
import chat.zekochat.utils.DeepLinkHandler
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ChatRouterDestination {
    data object Settings : ChatRouterDestination()
    data object Friends : ChatRouterDestination()
    data object Home : ChatRouterDestination()
    data object Discover : ChatRouterDestination()
    data class Channel(
        val channelId: String,
        val messageId: String? = null,
    ) : ChatRouterDestination()

    data class ServersChannels(val serverID: String) : ChatRouterDestination()
    data class NoCurrentChannel(val serverId: String?) : ChatRouterDestination()

    companion object {
        val default = Home // Changed default from Settings to Home

        fun fromString(destination: String): ChatRouterDestination {
            return when {
                destination == "home" -> Home // Changed from Settings to Home
                destination == "overview" -> Home // Changed from Settings to Home
                destination == "friends" -> Friends
                destination.startsWith("no_current_channel/") -> NoCurrentChannel(
                    destination.removePrefix(
                        "no_current_channel/"
                    )
                )

                destination.startsWith("channel/") -> {
                    val parts = destination.removePrefix("channel/").split("/")
                    val channelId = parts.getOrNull(0)
                    val messageId = parts.getOrNull(1)
                    if (channelId != null) {
                        Channel(channelId, messageId)
                    } else {
                        default
                    }
                }

                else -> default
            }
        }
    }
}

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class ChatRouterViewModel @Inject constructor(
    private val kvStorage: KVStorage,
    @ApplicationContext val context: Context
) : ViewModel() {
    var currentDestination by mutableStateOf<ChatRouterDestination>(ChatRouterDestination.default)
    var previousDestination by mutableStateOf<ChatRouterDestination?>(null)
    var lastNonChannelDestination by mutableStateOf<ChatRouterDestination?>(null)
    var lastSelectedChannelId by mutableStateOf<String?>(null)
    var latestChangelogRead by mutableStateOf(true)
    var latestChangelog by mutableStateOf("")
    var latestChangelogBody by mutableStateOf("")
    var showNotificationRationale by mutableStateOf(false)
    var showSwipeToReplySpark by mutableStateOf(false)

    private val changelogs = Changelogs(context, kvStorage)

    init {
        viewModelScope.launch {
            // Check if there's a deep link to handle first
            if (DeepLinkHandler.hasDeepLink) {
                Log.d(
                    "ChatRouterViewModel",
                    "Deep link detected in init, skipping saved destination"
                )
                // Don't set any destination here, let the LaunchedEffect in ChatRouterScreen handle it
            } else {
                // No deep link, load the saved destination
                val current = kvStorage.get("currentDestination")
                setSaveDestination(ChatRouterDestination.fromString(current ?: ""))
            }

            setRegisterForNotifications()

            latestChangelogRead = changelogs.hasSeenCurrent()
            latestChangelog = changelogs.getLatestChangelogCode()
//            latestChangelogBody =
//                changelogs.fetchChangelogByVersionCode(latestChangelog.toLong()).rendered
            if (!latestChangelogRead) {
                changelogs.markAsSeen()
            }

            val seenEarlyAccess = kvStorage.getBoolean("spark/earlyAccess/dismissed")
            val seenSwipeToReply = kvStorage.getBoolean("spark/swipeToReply/dismissed")

            if (seenEarlyAccess == true && seenSwipeToReply != true) {
                showSwipeToReplySpark = true
            }

            val hasNotificationPermission =
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            // right now we only show this in debug builds so Chucker can show its notification
            if (!hasNotificationPermission) {
                showNotificationRationale = true
            }
        }
    }

    fun setSaveDestination(destination: ChatRouterDestination) {
        if (destination != currentDestination) {
            previousDestination = currentDestination
            currentDestination = destination
            if (destination !is ChatRouterDestination.Channel) {
                lastNonChannelDestination = destination
            } else {
                lastSelectedChannelId = destination.channelId
            }
        }
    }

    fun setRegisterForNotifications() {
        showNotificationRationale = false
        FirebaseMessaging.getInstance().token.addOnCompleteListener(
            OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FCM", "Fetching FCM registration token failed", task.exception)
//                    task.exception?.let { Sentry.captureException(it) }
                    return@OnCompleteListener
                }

                val token = task.result
                viewModelScope.launch {
                    kvStorage.set("fcmToken", token)
                    subscribePush(auth = token)
                }
            }
        )
    }

    fun markNotificationsRejected() {
        showNotificationRationale = false
        viewModelScope.launch {
            kvStorage.set("pushNotificationsRejected", true)
        }
    }

    fun dismissSwipeToReplySpark() {
        showSwipeToReplySpark = false
        viewModelScope.launch {
            kvStorage.set("spark/swipeToReply/dismissed", true)
        }
    }

    fun navigateToServer(serverId: String) {
        viewModelScope.launch {
            setSaveDestination(ChatRouterDestination.ServersChannels(serverId))
        }
    }
}

val LocalIsConnected = compositionLocalOf(structuralEqualityPolicy()) { false }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRouterScreen(
    topNav: NavController,
    windowSizeClass: WindowSizeClass,
    onNullifiedUser: () -> Unit,
    onEnterVoiceUI: (String) -> Unit,
    viewModel: ChatRouterViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current

    var showPlatformModDMHint by remember { mutableStateOf(false) }

    var showStatusSheet by remember { mutableStateOf(false) }
    var showAddServerSheet by remember { mutableStateOf(false) }

    var showServerContextSheet by remember { mutableStateOf(false) }
    var serverContextSheetTarget by remember { mutableStateOf("") }

    var showUserContextSheet by remember { mutableStateOf(false) }
    var userContextSheetTarget by remember { mutableStateOf("") }
    var userContextSheetServer by remember { mutableStateOf<String?>(null) }

    var showWebhookInfoSheet by remember { mutableStateOf(false) }

    var showChannelUnavailableAlert by remember { mutableStateOf(false) }

    var showLinkInfoSheet by remember { mutableStateOf(false) }
    var linkInfoSheetUrl by remember { mutableStateOf("") }

    var showEmoteInfoSheet by remember { mutableStateOf(false) }
    var emoteInfoSheetTarget by remember { mutableStateOf("") }

    var showReactionInfoSheet by remember { mutableStateOf(false) }
    var reactionInfoSheetMessageId by remember { mutableStateOf("") }
    var reactionInfoSheetEmoji by remember { mutableStateOf("") }

    var useTabletAwareUI by remember { mutableStateOf(false) }

    var showReportUser by remember { mutableStateOf(false) }
    var reportUserTarget by remember { mutableStateOf("") }

    var showReportMessage by remember { mutableStateOf(false) }
    var reportMessageTarget by remember { mutableStateOf("") }

    var showReportServer by remember { mutableStateOf(false) }
    var reportServerTarget by remember { mutableStateOf("") }

    val toggleDrawerLambda = remember {
        {
            scope.launch {
                if (drawerState.isOpen) {
                    drawerState.close()
                } else {
                    drawerState.open()
                }
            }
        }
    }

    val currentServer = remember(viewModel.currentDestination) {
        when (viewModel.currentDestination) {
            is ChatRouterDestination.Channel -> {
                PeptideAPI.channelCache[(viewModel.currentDestination as ChatRouterDestination.Channel).channelId]?.server
            }

            is ChatRouterDestination.NoCurrentChannel -> {
                (viewModel.currentDestination as ChatRouterDestination.NoCurrentChannel).serverId
            }

            else -> null
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (RealtimeSocket.disconnectionState == DisconnectionState.Disconnected) {
            RealtimeSocket.updateDisconnectionState(DisconnectionState.Reconnecting)
            scope.launch { PeptideAPI.connectWS() }
        }
    }

    LaunchedEffect(drawerState) {
        snapshotFlow { drawerState.currentValue }
            .distinctUntilChanged()
            .collect { state ->
                if (state == DrawerValue.Open) {
                    val keyboard =
                        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    keyboard.hideSoftInputFromWindow(view.windowToken, 0)
                }
            }
    }

    LaunchedEffect(PeptideAPI.selfId) {
        snapshotFlow { PeptideAPI.selfId }
            .distinctUntilChanged()
            .collect { selfId ->
                if (selfId == null) {
                    onNullifiedUser()
                }
            }
    }

    LaunchedEffect(DirectMessages.unreadDMs()) {
        snapshotFlow { DirectMessages.unreadDMs() }
            .distinctUntilChanged()
            .collect { _ ->
                if (DirectMessages.hasPlatformModerationDM()) {
                    showPlatformModDMHint = true
                }
            }
    }

    LaunchedEffect(windowSizeClass) {
        snapshotFlow { windowSizeClass }
            .distinctUntilChanged()
            .collect { sizeClass ->
                useTabletAwareUI = sizeClass.widthSizeClass == WindowWidthSizeClass.Expanded &&
                        sizeClass.heightSizeClass != WindowHeightSizeClass.Compact
            }
    }

    // Handle deep links when the ChatRouterScreen is first composed
    LaunchedEffect(Unit) {
        // Check if there's a deep link to handle
        if (DeepLinkHandler.hasDeepLink) {
            Log.d("ChatRouterScreen", "Processing deep link data")

            // Process the deep link based on priority
            when {
                // Priority 1: Handle server/channel/message format (most specific)
                DeepLinkHandler.messageId != null && DeepLinkHandler.channelId != null -> {
                    val messageId = DeepLinkHandler.messageId!!
                    val channelId = DeepLinkHandler.channelId!!
                    Log.d(
                        "ChatRouterScreen",
                        "Setting destination to message: $messageId in channel: $channelId"
                    )

                    val resolvedChannel = PeptideAPI.channelCache[channelId]
                    if (resolvedChannel == null) {
                        showChannelUnavailableAlert = true
                    } else {
                        // Navigate to the channel first, then the message will be handled by the channel screen
                        viewModel.setSaveDestination(ChatRouterDestination.Channel(channelId))
                        // TODO: Add logic to scroll to the specific message in the channel
                    }
                }

                // Priority 2: Handle channel deep link
                DeepLinkHandler.channelId != null -> {
                    val channelId = DeepLinkHandler.channelId!!
                    Log.d("ChatRouterScreen", "Setting destination to channel: $channelId")
                    val resolvedChannel = PeptideAPI.channelCache[channelId]

                    if (resolvedChannel == null) {
                        showChannelUnavailableAlert = true
                    } else {
                        viewModel.setSaveDestination(ChatRouterDestination.Channel(channelId))
                    }
                }

                // Priority 3: Handle server deep link
                DeepLinkHandler.serverId != null -> {
                    val serverId = DeepLinkHandler.serverId!!
                    Log.d("ChatRouterScreen", "Setting destination to server: $serverId")
                    viewModel.navigateToServer(serverId)
                }

                // Priority 4: Handle user deep link
                DeepLinkHandler.userId != null -> {
                    val userId = DeepLinkHandler.userId!!
                    Log.d("ChatRouterScreen", "Setting destination to user: $userId")
                    // TODO: Implement user navigation if needed
                }
            }

            // Clear the deep link data after processing
            DeepLinkHandler.clear()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            ActionChannel.receive().let { action ->
                when (action) {
                    is Action.OpenUserSheet -> {
                        userContextSheetTarget = action.userId
                        userContextSheetServer = action.serverId
                        showUserContextSheet = true
                    }

                    is Action.SwitchChannel -> {
                        val resolvedChannel = PeptideAPI.channelCache[action.channelId]

                        if (resolvedChannel == null) {
                            showChannelUnavailableAlert = true
                            return@let
                        }

                        viewModel.setSaveDestination(
                            ChatRouterDestination.Channel(
                                channelId = action.channelId,
                                messageId = action.messageId,
                            )
                        )
                    }

                    is Action.LinkInfo -> {
                        linkInfoSheetUrl = action.url
                        showLinkInfoSheet = true
                    }

                    is Action.EmoteInfo -> {
                        emoteInfoSheetTarget = action.emoteId
                        showEmoteInfoSheet = true
                    }

                    is Action.MessageReactionInfo -> {
                        reactionInfoSheetMessageId = action.messageId
                        reactionInfoSheetEmoji = action.emoji
                        showReactionInfoSheet = true
                    }

                    is Action.TopNavigate -> {
                        topNav.navigate(action.route)
                    }

                    is Action.ChatNavigate -> {
                        viewModel.setSaveDestination(action.destination)
                    }

                    is Action.ReportUser -> {
                        reportUserTarget = action.userId
                        showReportUser = true
                    }

                    is Action.ReportMessage -> {
                        reportMessageTarget = action.messageId
                        showReportMessage = true
                    }

                    is Action.OpenVoiceChannelOverlay -> {
                        onEnterVoiceUI(action.channelId)
                    }

                    is Action.OpenWebhookSheet -> {
                        showWebhookInfoSheet = true
                    }
                }
            }
        }
    }

    var isTouchExplorationEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val accessibilityManager =
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

        isTouchExplorationEnabled = accessibilityManager.isTouchExplorationEnabled
        accessibilityManager.addTouchExplorationStateChangeListener { enabled ->
            isTouchExplorationEnabled = enabled
        }
    }

    if (!viewModel.latestChangelogRead) {
        ChangelogSheet(
            versionName = viewModel.latestChangelog,
            versionIsHistorical = false,
            renderedContents = viewModel.latestChangelogBody,
            onDismiss = {
                viewModel.latestChangelogRead = true
            }
        )
    }

    if (showPlatformModDMHint) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(stringResource(id = R.string.notice_platform_mod_dm_title))
            },
            text = {
                Text(stringResource(id = R.string.notice_platform_mod_dm_description))
            },
            confirmButton = {
                PepTextButton(onClick = {
                    showPlatformModDMHint = false
                    DirectMessages.getPlatformModerationDM()?.id?.let {
                        viewModel.setSaveDestination(ChatRouterDestination.Channel(it))
                    }
                }) {
                    Text(stringResource(id = R.string.notice_platform_mod_dm_acknowledge))
                }
            }
        )
    }

    if (showStatusSheet) {
        val statusSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            sheetState = statusSheetState,
            onDismissRequest = {
                showStatusSheet = false
            }
        ) {
            StatusSheet(
                onBeforeNavigation = {
                    scope.launch {
                        statusSheetState.hide()
                        showStatusSheet = false
                    }
                },
                onGoSettings = {
                    topNav.navigate("settings")
                }
            )
        }
    }

    if (showAddServerSheet) {
        val addServerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            sheetState = addServerSheetState,
            sheetGesturesEnabled = false,
            dragHandle = {},
            onDismissRequest = {
                showAddServerSheet = false
            }
        ) {
            AddServerSheet(
                onDismiss = {
                    showAddServerSheet = false
                }
            )
        }
    }

    if (showServerContextSheet) {
        val serverContextSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            sheetState = serverContextSheetState,
            onDismissRequest = {
                showServerContextSheet = false
            }
        ) {
            ServerContextSheet(
                serverId = serverContextSheetTarget,
                onHideSheet = {
                    serverContextSheetState.hide()
                    showServerContextSheet = false
                },
                onReportServer = {
                    reportServerTarget = serverContextSheetTarget
                    showReportServer = true
                }
            )
        }
    }

    if (showUserContextSheet) {
        val userContextSheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )

        ModalBottomSheet(
            sheetState = userContextSheetState,
            onDismissRequest = {
                showUserContextSheet = false
            }
        ) {
            UserInfoSheet(
                userId = userContextSheetTarget,
                dismissSheet = {
                    userContextSheetState.hide()
                    showUserContextSheet = false
                }
            )
        }
    }

    if (showWebhookInfoSheet) {
        val webhookInfoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            sheetState = webhookInfoSheetState,
            sheetGesturesEnabled = false,
            dragHandle = {},
            onDismissRequest = {
                showWebhookInfoSheet = false
            }
        ) {
            WebHookUserSheet()
        }
    }

    if (showReportUser) {
        ReportUserDialog(
            onDismiss = { showReportUser = false },
            userId = reportUserTarget
        )
    }

    if (showReportMessage) {
        ReportMessageDialog(
            onDismiss = { showReportMessage = false },
            messageId = reportMessageTarget
        )
    }

    if (showReportServer) {
        ReportServerDialog(
            onDismiss = { showReportServer = false },
            serverId = reportServerTarget
        )
    }

    if (showChannelUnavailableAlert) {
        AlertDialog(
            onDismissRequest = {
                showChannelUnavailableAlert = false
                viewModel.setSaveDestination(ChatRouterDestination.Discover)
            },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.icn_lock_24dp),
                    contentDescription = null, // decorative
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = stringResource(id = R.string.channel_link_invalid),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.channel_link_invalid_description),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                PepTextButton(onClick = {
                    showChannelUnavailableAlert = false
                    viewModel.setSaveDestination(ChatRouterDestination.Discover)
                }) {
                    Text(text = stringResource(id = R.string.ok))
                }
            }
        )
    }

    if (showLinkInfoSheet) {
        val linkInfoSheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            sheetState = linkInfoSheetState,
            onDismissRequest = {
                showLinkInfoSheet = false
            }
        ) {
            LinkInfoSheet(
                url = linkInfoSheetUrl,
                onDismiss = {
                    showLinkInfoSheet = false
                }
            )
        }
    }

    if (showEmoteInfoSheet) {
        val emoteInfoSheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            sheetState = emoteInfoSheetState,
            onDismissRequest = {
                showEmoteInfoSheet = false
            }
        ) {
            EmoteInfoSheet(
                id = emoteInfoSheetTarget,
                onDismiss = {
                    showEmoteInfoSheet = false
                }
            )
        }
    }

    if (showReactionInfoSheet) {
        val reactionInfoSheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            sheetState = reactionInfoSheetState,
            onDismissRequest = {
                showReactionInfoSheet = false
            }
        ) {
            ReactionInfoSheet(
                messageId = reactionInfoSheetMessageId,
                emoji = reactionInfoSheetEmoji,
                onDismiss = {
                    showReactionInfoSheet = false
                }
            )
        }
    }

    val askNotificationsPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                viewModel.setRegisterForNotifications()
            } else {
                viewModel.showNotificationRationale = false
            }
        }
    if (viewModel.showNotificationRationale) {
        NotificationRationaleDialog(
            onDismiss = {
                viewModel.showNotificationRationale = false
            },
            onSelected = { accepted ->
                if (accepted) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        askNotificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.setRegisterForNotifications()
                    }
                } else {
                    viewModel.markNotificationsRejected()
                }
            }
        )
    }

    if (viewModel.showSwipeToReplySpark) {
        val swipeToReplySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            sheetState = swipeToReplySheetState,
            sheetGesturesEnabled = false,
            dragHandle = {},
            onDismissRequest = {
                // Only dismiss using button in sheet
            }
        ) {
            SwipeToReplySparkSheet(
                onDismissSheet = {
                    scope.launch {
                        swipeToReplySheetState.hide()
                        viewModel.dismissSwipeToReplySpark()
                    }
                },
                onOpenOptions = {
                    topNav.navigate("settings/chat")
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
            )
    ) {
        AnimatedVisibility(
            visible = RealtimeSocket.disconnectionState != DisconnectionState.Connected
        ) {
            DisconnectedNotice(
                state = RealtimeSocket.disconnectionState,
                onReconnect = {
                    RealtimeSocket.updateDisconnectionState(DisconnectionState.Reconnecting)
                    scope.launch { PeptideAPI.connectWS() }
                }
            )
        }

        CompositionLocalProvider(
            LocalIsConnected provides (RealtimeSocket.disconnectionState == DisconnectionState.Connected)
        ) {
            ChannelNavigator(
                dest = viewModel.currentDestination,
                topNav = topNav,
                toggleDrawer = {
                    toggleDrawerLambda()
                },
                onShowStatusSheet = {
                    showStatusSheet = true
                },
                onShowServerContextSheet = {
                    serverContextSheetTarget = it
                    showServerContextSheet = true
                },
                onShowAddServerSheet = {
                    showAddServerSheet = true
                },
                isTouchExplorationEnabled = isTouchExplorationEnabled,
                viewModel = viewModel,
                currentServer = currentServer,
            )
        }
    }
}

@Composable
fun ChannelNavigator(
    dest: ChatRouterDestination,
    topNav: NavController,
    viewModel: ChatRouterViewModel,
    onShowStatusSheet: () -> Unit = {},
    onShowAddServerSheet: () -> Unit = {},
    onShowServerContextSheet: (String) -> Unit = {},
    currentServer: String?,
    toggleDrawer: () -> Unit,
    isTouchExplorationEnabled: Boolean,
    setDrawerGestureEnabled: (Boolean) -> Unit = {},
) {
    // Keep track of the last opened channel to allow edge-swipe reopen
    var lastChannel by remember { mutableStateOf<ChatRouterDestination.Channel?>(null) }
    if (dest is ChatRouterDestination.Channel) {
        lastChannel = dest
    }

    var containerWidth by remember { mutableStateOf(0) }
    var isReopenDragging by remember { mutableStateOf(false) }
    var reopenDragDelta by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(dest, lastChannel) {
                if (dest !is ChatRouterDestination.Channel && lastChannel != null) {
                    detectHorizontalDragGestures(
                        onDragStart = { _ ->
                            // Allow reopen swipe to start anywhere on the screen
                            isReopenDragging = true
                            reopenDragDelta = 0f
                        },
                        onDragEnd = {
                            if (isReopenDragging) {
                                // If dragged sufficiently left, reopen the last channel
                                if (reopenDragDelta < -containerWidth / 6f) {
                                    lastChannel?.let { viewModel.setSaveDestination(it) }
                                }
                            }
                            isReopenDragging = false
                            reopenDragDelta = 0f
                        },
                        onDragCancel = {
                            isReopenDragging = false
                            reopenDragDelta = 0f
                        }
                    ) { change, dragAmount ->
                        if (isReopenDragging) {
                            reopenDragDelta += dragAmount
                        }
                    }
                }
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                containerWidth = placeable.width
                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            }
    ) {
        // Background scaffold is always rendered to act as the underlying layer
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                BottomAppBar {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_bottom_navbar_home),
                                contentDescription = "Home",
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        label = {
                            Text(text = "you")
                        },
                        selected = when (dest) {
                            is ChatRouterDestination.ServersChannels,
                            is ChatRouterDestination.NoCurrentChannel,
                            ChatRouterDestination.Home,
                            ChatRouterDestination.Discover -> true
                            else -> false
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent,
                            selectedIconColor = MaterialTheme.colorScheme.onBackground,
                            selectedTextColor = MaterialTheme.colorScheme.onBackground,
                            unselectedTextColor = MaterialTheme.colorScheme.onBackground.copy(
                                alpha = 0.4f
                            ),
                            unselectedIconColor = MaterialTheme.colorScheme.onBackground.copy(
                                alpha = 0.4f
                            ),
                        ),
                        enabled = true,
                        onClick = {
                            viewModel.setSaveDestination(ChatRouterDestination.Home)
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_bottom_navbar_friends),
                                contentDescription = "Friends",
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        label = {
                            Text(text = "Friends")
                        },
                        selected = dest is ChatRouterDestination.Friends,
                        enabled = true,
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent,
                            selectedIconColor = MaterialTheme.colorScheme.onBackground,
                            selectedTextColor = MaterialTheme.colorScheme.onBackground,
                            unselectedTextColor = MaterialTheme.colorScheme.onBackground.copy(
                                alpha = 0.4f
                            ),
                            unselectedIconColor = MaterialTheme.colorScheme.onBackground.copy(
                                alpha = 0.4f
                            ),
                        ),
                        onClick = {
                            viewModel.setSaveDestination(ChatRouterDestination.Friends)
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_bottom_navbar_you),
                                contentDescription = "You",
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        label = {
                            Text(text = "you")
                        },
                        selected = dest is ChatRouterDestination.Settings,
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent,
                            selectedIconColor = MaterialTheme.colorScheme.onBackground,
                            selectedTextColor = MaterialTheme.colorScheme.onBackground,
                            unselectedTextColor = MaterialTheme.colorScheme.onBackground.copy(
                                alpha = 0.4f
                            ),
                            unselectedIconColor = MaterialTheme.colorScheme.onBackground.copy(
                                alpha = 0.4f
                            ),
                        ),
                        enabled = true,
                        onClick = {
                            viewModel.setSaveDestination(ChatRouterDestination.Settings)
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (dest) {
                    is ChatRouterDestination.Settings -> {
                        SettingsScreen(
                            navController = topNav,
                        )
                    }

                    is ChatRouterDestination.Friends -> {
                        FriendsScreen(
                            topNav = topNav,
                            onDrawerClicked = toggleDrawer,
                        )
                    }

                    is ChatRouterDestination.Home,
                    is ChatRouterDestination.ServersChannels,
                    is ChatRouterDestination.Discover,
                    is ChatRouterDestination.Channel -> { // When Channel is active, show drawer as background
                        ChannelSideDrawer(
                            onDestinationChanged = viewModel::setSaveDestination,
                            currentDestination = viewModel.currentDestination,
                            currentServer = when (dest) {
                                is ChatRouterDestination.Home -> currentServer
                                is ChatRouterDestination.ServersChannels -> dest.serverID
                                is ChatRouterDestination.Channel -> currentServer
                                else -> null
                            },
                            selectedChannelId = viewModel.lastSelectedChannelId,
                            navigateToServer = viewModel::navigateToServer,
                            onShowServerContextSheet = onShowServerContextSheet,
                            showSettingsIcon = isTouchExplorationEnabled,
                            onOpenSettings = {
                                topNav.navigate("settings")
                            },
                            onShowAddServerSheet = onShowAddServerSheet
                        )
                    }

                    is ChatRouterDestination.NoCurrentChannel -> {
                        NoCurrentChannelScreen(onDrawerClicked = toggleDrawer)
                    }
                }
            }
        }

        // Foreground overlay: Channel screen when destination is a Channel
        if (dest is ChatRouterDestination.Channel) {
            ChannelScreen(
                channelId = dest.channelId,
                messageId = dest.messageId,
                backToChannelsScreen = {
                    // Prefer going back to the current server's channels screen explicitly
                    val target = currentServer?.let { ChatRouterDestination.ServersChannels(serverID = it) }
                        ?: viewModel.lastNonChannelDestination
                        ?: viewModel.previousDestination
                        ?: ChatRouterDestination.Home
                    viewModel.setSaveDestination(target)
                },
                setDrawerGestureEnabled = setDrawerGestureEnabled,
            )
        }
    }
}
