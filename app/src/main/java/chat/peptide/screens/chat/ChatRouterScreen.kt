package chat.peptide.screens.chat

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import chat.peptide.BuildConfig
import chat.peptide.R
import chat.peptide.api.RevoltAPI
import chat.peptide.api.internals.DirectMessages
import chat.peptide.api.realtime.DisconnectionState
import chat.peptide.api.realtime.RealtimeSocket
import chat.peptide.api.routes.push.subscribePush
import chat.peptide.callbacks.Action
import chat.peptide.callbacks.ActionChannel
import chat.peptide.composables.chat.DisconnectedNotice
import chat.peptide.composables.screens.chat.drawer.ChannelSideDrawer
import chat.peptide.dialogs.NotificationRationaleDialog
import chat.peptide.internals.Changelogs
import chat.peptide.utils.DeepLinkHandler
import chat.peptide.persistence.KVStorage
import chat.peptide.screens.chat.dialogs.safety.ReportMessageDialog
import chat.peptide.screens.chat.dialogs.safety.ReportServerDialog
import chat.peptide.screens.chat.dialogs.safety.ReportUserDialog
import chat.peptide.screens.chat.views.FriendsScreen
import chat.peptide.screens.chat.views.NoCurrentChannelScreen
import chat.peptide.screens.chat.views.channel.ChannelScreen
import chat.peptide.screens.settings.SettingsScreen
import chat.peptide.sheets.AddServerSheet
import chat.peptide.sheets.ChangelogSheet
import chat.peptide.sheets.EarlyAccessSheet
import chat.peptide.sheets.EmoteInfoSheet
import chat.peptide.sheets.LinkInfoSheet
import chat.peptide.sheets.ReactionInfoSheet
import chat.peptide.sheets.ServerContextSheet
import chat.peptide.sheets.StatusSheet
import chat.peptide.sheets.UserInfoSheet
import chat.peptide.sheets.WebHookUserSheet
import chat.peptide.sheets.spark.SwipeToReplySparkSheet
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.sentry.Sentry
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
    var latestChangelogRead by mutableStateOf(true)
    var latestChangelog by mutableStateOf("")
    var latestChangelogBody by mutableStateOf("")
    var showNotificationRationale by mutableStateOf(false)
    var showEarlyAccessSpark by mutableStateOf(false)
    var showSwipeToReplySpark by mutableStateOf(false)

    private val changelogs = Changelogs(context, kvStorage)

    init {
        viewModelScope.launch {
            // Check if there's a deep link to handle first
            if (DeepLinkHandler.hasDeepLink) {
                Log.d("ChatRouterViewModel", "Deep link detected in init, skipping saved destination")
                // Don't set any destination here, let the LaunchedEffect in ChatRouterScreen handle it
            } else {
                // No deep link, load the saved destination
                val current = kvStorage.get("currentDestination")
                setSaveDestination(ChatRouterDestination.fromString(current ?: ""))
            }

            latestChangelogRead = changelogs.hasSeenCurrent()
            latestChangelog = changelogs.getLatestChangelogCode()
            latestChangelogBody =
                changelogs.fetchChangelogByVersionCode(latestChangelog.toLong()).rendered
            if (!latestChangelogRead) {
                changelogs.markAsSeen()
            }

            val seenEarlyAccess = kvStorage.getBoolean("spark/earlyAccess/dismissed")
            val seenSwipeToReply = kvStorage.getBoolean("spark/swipeToReply/dismissed")
            if (seenEarlyAccess == null) {
                showEarlyAccessSpark = true
                // we don't show swipe to reply to new users,
                // as they would expect it to be working already
                kvStorage.set("spark/swipeToReply/dismissed", true)
            }

            if (seenEarlyAccess == true && seenSwipeToReply != true) {
                showSwipeToReplySpark = true
            }

            val hasNotificationPermission =
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            // right now we only show this in debug builds so Chucker can show its notification
            if (!hasNotificationPermission && BuildConfig.DEBUG) {
                showNotificationRationale = true
            }
        }
    }

    fun setSaveDestination(destination: ChatRouterDestination) {
        currentDestination = destination
    }

    fun setRegisterForNotifications() {
        showNotificationRationale = false
        FirebaseMessaging.getInstance().token.addOnCompleteListener(
            OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                    task.exception?.let { Sentry.captureException(it) }
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

    fun dismissEarlyAccessSpark() {
        showEarlyAccessSpark = false
        viewModelScope.launch {
            kvStorage.set("spark/earlyAccess/dismissed", true)
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
                RevoltAPI.channelCache[(viewModel.currentDestination as ChatRouterDestination.Channel).channelId]?.server
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
            scope.launch { RevoltAPI.connectWS() }
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

    LaunchedEffect(RevoltAPI.selfId) {
        snapshotFlow { RevoltAPI.selfId }
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
                    Log.d("ChatRouterScreen", "Setting destination to message: $messageId in channel: $channelId")
                    
                    val resolvedChannel = RevoltAPI.channelCache[channelId]
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
                    val resolvedChannel = RevoltAPI.channelCache[channelId]
                    
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
                        val resolvedChannel = RevoltAPI.channelCache[action.channelId]

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
                TextButton(onClick = {
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
                    reportServerTarget = currentServer ?: ""
                    showReportServer = true
                }
            )
        }
    }

    if (showUserContextSheet) {
        val userContextSheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            sheetState = userContextSheetState,
            onDismissRequest = {
                showUserContextSheet = false
            }
        ) {
            UserInfoSheet(
                userId = userContextSheetTarget,
                serverId = userContextSheetServer,
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
                TextButton(onClick = {
                    showChannelUnavailableAlert = false
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

    if (viewModel.showEarlyAccessSpark) {
        val earlyAccessSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            sheetState = earlyAccessSheetState,
            sheetGesturesEnabled = false,
            dragHandle = {},
            onDismissRequest = {
                // Only dismiss using button in sheet
            }
        ) {
            EarlyAccessSheet(
                onClose = {
                    scope.launch {

                        earlyAccessSheetState.hide()
                        viewModel.dismissEarlyAccessSpark()
                    }
                }
            )
        }
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
                    scope.launch { RevoltAPI.connectWS() }
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
    when (dest) {
        is ChatRouterDestination.Channel -> {
            ChannelScreen(
                channelId = dest.channelId,
                messageId = dest.messageId,
                backToChannelsScreen = {
                    currentServer?.let {
                        viewModel.setSaveDestination(
                            ChatRouterDestination.ServersChannels(
                                serverID = currentServer
                            )
                        )
                    } ?: viewModel.setSaveDestination(
                        ChatRouterDestination.Home
                    )
                },
                setDrawerGestureEnabled = setDrawerGestureEnabled,
            )
        }

        else -> {
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
                            selected = when(dest){
                                is ChatRouterDestination.ServersChannels,
                                is ChatRouterDestination.NoCurrentChannel,
                                ChatRouterDestination.Home,
                                ChatRouterDestination.Discover -> true
                                else -> false
                            } ,
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
                            enabled = true,
                            onClick = {
                                viewModel.setSaveDestination(ChatRouterDestination.Settings)
                            }
                        )
                    }
                }
            ) { innerPadding ->
                Column(modifier = Modifier.padding(innerPadding)) {
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
                        is ChatRouterDestination.Discover -> {
                            ChannelSideDrawer(
                                onDestinationChanged = viewModel::setSaveDestination,
                                currentDestination = viewModel.currentDestination,
                                currentServer = when (dest) {
                                    is ChatRouterDestination.Home -> currentServer
                                    is ChatRouterDestination.ServersChannels -> dest.serverID
                                    else -> null
                                },
                                navigateToServer = viewModel::navigateToServer,
                                onShowServerContextSheet = onShowServerContextSheet,
                                showSettingsIcon = isTouchExplorationEnabled,
                                onOpenSettings = {
                                    topNav.navigate("settings")
                                },
                                onShowAddServerSheet = onShowAddServerSheet
                            )
                        }

                        is ChatRouterDestination.Channel -> {}

                        is ChatRouterDestination.NoCurrentChannel -> {
                            NoCurrentChannelScreen(onDrawerClicked = toggleDrawer)
                        }
                    }
                }
            }
        }
    }
}
