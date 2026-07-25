package chat.stoat.screens.chat

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DismissibleDrawerSheet
import androidx.compose.material3.DismissibleNavigationDrawer
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import chat.stoat.BuildConfig
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.DirectMessages
import chat.stoat.api.realtime.DisconnectionState
import chat.stoat.api.realtime.RealtimeSocket
import chat.stoat.api.routes.microservices.gazette.getLatestChangelog
import chat.stoat.api.routes.push.subscribePush
import chat.stoat.api.routes.user.fetchSelf
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.schemas.User
import chat.stoat.api.settings.SyncedSettings
import chat.stoat.callbacks.Action
import chat.stoat.callbacks.ActionChannel
import chat.stoat.composables.chat.DisconnectedNotice
import chat.stoat.composables.screens.chat.drawer.ChannelSideDrawer
import chat.stoat.core.model.schemas.ReleaseNotesSettings
import chat.stoat.dialogs.NotificationRationaleDialog
import chat.stoat.c2dm.NotificationDeepLink
import chat.stoat.internals.StoatWebLink
import chat.stoat.internals.extensions.zero
import chat.stoat.persistence.KVStorage
import chat.stoat.screens.chat.dialogs.safety.ReportMessageDialog
import chat.stoat.screens.chat.dialogs.safety.ReportServerDialog
import chat.stoat.screens.chat.dialogs.safety.ReportUserDialog
import chat.stoat.screens.chat.views.FriendsScreen
import chat.stoat.screens.chat.views.NoCurrentChannelScreen
import chat.stoat.screens.chat.views.OverviewScreen
import chat.stoat.screens.chat.views.channel.ChannelScreen
import chat.stoat.sheets.AddServerSheet
import chat.stoat.sheets.EarlyAccessSheet
import chat.stoat.sheets.EmoteInfoSheet
import chat.stoat.sheets.LinkInfoSheet
import chat.stoat.sheets.ReactionInfoSheet
import chat.stoat.sheets.ServerContextSheet
import chat.stoat.sheets.StatusSheet
import chat.stoat.sheets.UserInfoSheet
import chat.stoat.sheets.WebHookUserSheet
import chat.stoat.sheets.spark.SwipeToReplySparkSheet
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import io.sentry.Sentry
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import logcat.LogPriority
import logcat.logcat
import org.koin.androidx.compose.koinViewModel

sealed class ChatRouterDestination {
    data object Overview : ChatRouterDestination()
    data object Friends : ChatRouterDestination()
    data class Channel(val channelId: String) : ChatRouterDestination()
    data class NoCurrentChannel(val serverId: String?) : ChatRouterDestination()

    fun asSerialisedString(): String {
        return when (this) {
            is Overview -> "overview"
            is Friends -> "friends"
            is Channel -> "channel/$channelId"
            is NoCurrentChannel -> "no_current_channel/$serverId"
        }
    }

    companion object {
        val default = Overview
        val defaultForDMList = Overview

        fun fromString(destination: String): ChatRouterDestination {
            return when {
                destination == "home" -> Overview // previous name for overview
                destination == "overview" -> Overview
                destination == "friends" -> Friends
                destination.startsWith("no_current_channel/") -> NoCurrentChannel(
                    destination.removePrefix(
                        "no_current_channel/"
                    )
                )

                destination.startsWith("channel/") -> Channel(destination.removePrefix("channel/"))
                else -> default
            }
        }
    }
}

@SuppressLint("StaticFieldLeak")
class ChatRouterViewModel(
    private val kvStorage: KVStorage,
    val context: Context,
) : ViewModel() {
    var currentDestination by mutableStateOf<ChatRouterDestination>(ChatRouterDestination.default)
    var showNotificationRationale by mutableStateOf(false)
    var showEarlyAccessSpark by mutableStateOf(false)
    var showSwipeToReplySpark by mutableStateOf(false)
    var showChangelogScreenForId by mutableStateOf<String?>(null)
    var pendingMessageJump by mutableStateOf<ChannelMessageJump?>(null)
        private set
    private var changelogCheckDone = false

    init {
        viewModelScope.launch {
            runCatching { fetchSelf() }.getOrNull()?.let { user ->
                kvStorage.set("selfId", user.id ?: "")
                kvStorage.set("selfName", User.resolveDefaultName(user))
                kvStorage.set("selfAvatarUrl", user.avatar?.id?.let { "$STOAT_FILES/avatars/$it" } ?: "")
            }

            val pendingNavigation = NotificationDeepLink.pendingNavigation.value
            if (pendingNavigation != null) {
                consumePendingNavigation(pendingNavigation)
            } else {
                val current = kvStorage.get("currentDestination")
                setSaveDestination(ChatRouterDestination.fromString(current ?: ""))
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
            val rejectedPush = kvStorage.getBoolean("pushNotificationsRejected") == true
            if (!hasNotificationPermission && !rejectedPush) {
                showNotificationRationale = true
            }

            NotificationDeepLink.pendingNavigation.collect { navigation ->
                if (navigation != null) consumePendingNavigation(navigation)
            }
        }
    }

    private fun consumePendingNavigation(navigation: StoatWebLink) {
        if (NotificationDeepLink.pendingNavigation.value == navigation) {
            NotificationDeepLink.pendingNavigation.value = null
        }

        when (navigation) {
            is StoatWebLink.Server -> navigateToServer(navigation.serverId)
            is StoatWebLink.Channel ->
                setSaveDestination(ChatRouterDestination.Channel(navigation.channelId))

            is StoatWebLink.Message ->
                requestMessageJump(navigation.channelId, navigation.messageId)
        }
    }

    fun requestMessageJump(channelId: String, messageId: String) {
        pendingMessageJump = ChannelMessageJump(channelId, messageId)
        setSaveDestination(ChatRouterDestination.Channel(channelId))
    }

    fun consumeMessageJump(request: ChannelMessageJump) {
        if (pendingMessageJump == request) pendingMessageJump = null
    }

    fun setSaveDestination(destination: ChatRouterDestination) {
        currentDestination = destination

        viewModelScope.launch {
            kvStorage.set("currentDestination", destination.asSerialisedString())

            if (destination is ChatRouterDestination.Channel) {
                val server = StoatAPI.channelCache[destination.channelId]?.server
                if (server != null) {
                    kvStorage.set("lastChannel/$server", destination.channelId)
                }
            }
        }
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
            val savedLastChannel = kvStorage.get("lastChannel/$serverId")
            val channelId =
                savedLastChannel ?: StoatAPI.serverCache[serverId]?.channels?.firstOrNull()
            val channelExists = StoatAPI.channelCache.containsKey(channelId)

            if (channelId != null && channelExists) {
                setSaveDestination(ChatRouterDestination.Channel(channelId))
            } else {
                setSaveDestination(ChatRouterDestination.NoCurrentChannel(serverId))
            }
        }
    }

    fun maybeShowChangelog() {
        if (changelogCheckDone) return
        changelogCheckDone = true

        viewModelScope.launch {
            val latestChangelog = runCatching { getLatestChangelog() }
                .onFailure {
                    logcat(LogPriority.ERROR) { "Failed to fetch latest changelog: ${it.message}" }
                }
                .getOrNull()

            if (latestChangelog != null) {
                val isInFuture =
                    runCatching { Instant.parse(latestChangelog.publishedAt) > Clock.System.now() }.getOrNull()
                        ?: false
                if (isInFuture) {
                    logcat(LogPriority.WARN) { "Latest changelog is from the future (${latestChangelog.publishedAt} > ${Clock.System.now()}), not showing it!" }
                    return@launch
                }

                SyncedSettings.awaitFetched()
                val lastSeenChangelog = SyncedSettings.releaseNotes.lastSeenId
                if (lastSeenChangelog == null || lastSeenChangelog != latestChangelog.id) {
                    showChangelogScreenForId = latestChangelog.id
                    SyncedSettings.updateReleaseNotes(
                        ReleaseNotesSettings(
                            lastSeenId = latestChangelog.id,
                            lastSeenAt = Clock.System.now().toString()
                        )
                    )
                }
            }
        }
    }
}

val LocalIsConnected = compositionLocalOf(structuralEqualityPolicy()) { false }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRouterScreen(
    topNav: NavController,
    windowSizeClass: WindowSizeClass,
    disableBackHandler: Boolean,
    onNullifiedUser: () -> Unit,
    onEnterVoiceUI: (String) -> Unit,
    viewModel: ChatRouterViewModel = koinViewModel()
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current

    var drawerWidth by remember { mutableFloatStateOf(0.0f) }

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
                StoatAPI.channelCache[(viewModel.currentDestination as ChatRouterDestination.Channel).channelId]?.server
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
            scope.launch { StoatAPI.connectWS() }
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

    LaunchedEffect(StoatAPI.selfId) {
        snapshotFlow { StoatAPI.selfId }
            .distinctUntilChanged()
            .collect { selfId ->
                if (selfId == null) {
                    onNullifiedUser()
                }
            }
    }

    LaunchedEffect(Unit) {
        viewModel.maybeShowChangelog()
    }

    LaunchedEffect(Unit) {
        snapshotFlow { viewModel.showChangelogScreenForId }
            .distinctUntilChanged()
            .collect { changelogId ->
                if (changelogId != null) {
                    viewModel.showChangelogScreenForId = null
                    topNav.navigate("changelog/${changelogId}")
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

    LaunchedEffect(Unit) {
        while (true) {
            ActionChannel.receive().let { action ->
                when (action) {
                    is Action.OpenUserSheet -> {
                        userContextSheetTarget = action.userId
                        userContextSheetServer = action.serverId
                        showUserContextSheet = true
                    }

                    is Action.SwitchServer -> {
                        viewModel.navigateToServer(action.serverId)
                    }

                    is Action.SwitchChannel -> {
                        val resolvedChannel = StoatAPI.channelCache[action.channelId]

                        if (resolvedChannel == null) {
                            showChannelUnavailableAlert = true
                            return@let
                        }

                        viewModel.setSaveDestination(ChatRouterDestination.Channel(action.channelId))
                    }

                    is Action.JumpToMessage -> {
                        val resolvedChannel = StoatAPI.channelCache[action.channelId]

                        if (resolvedChannel == null) {
                            showChannelUnavailableAlert = true
                            return@let
                        }

                        viewModel.requestMessageJump(
                            channelId = action.channelId,
                            messageId = action.messageId,
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
                    painter = painterResource(R.drawable.ic_lock_24dp),
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
                viewModel.markNotificationsRejected()
            }
        }
    if (viewModel.showNotificationRationale) {
        NotificationRationaleDialog(
            onDismiss = {
                viewModel.markNotificationsRejected()
            },
            onSelected = { accepted ->
                if (accepted) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        askNotificationsPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
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
                    scope.launch { StoatAPI.connectWS() }
                }
            )
        }

        CompositionLocalProvider(
            LocalIsConnected provides (RealtimeSocket.disconnectionState == DisconnectionState.Connected)
        ) {
            if (useTabletAwareUI) {
                Row {
                    DismissibleDrawerSheet(
                        drawerContainerColor = Color.Transparent,
                        windowInsets = WindowInsets.zero
                    ) {
                        Sidebar(
                            viewModel = viewModel,
                            topNav = topNav,
                            currentServer = currentServer,
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
                            showSettingsButton = isTouchExplorationEnabled,
                            onOpenSettings = {
                                topNav.navigate("settings")
                            },
                        )
                    }
                    ChannelNavigator(
                        dest = viewModel.currentDestination,
                        topNav = topNav,
                        messageJump = viewModel.pendingMessageJump,
                        onMessageJumpConsumed = viewModel::consumeMessageJump,
                        useDrawer = false,
                        disableBackHandler = disableBackHandler,
                        toggleDrawer = {
                            toggleDrawerLambda()
                        },
                        onEnterVoiceUI = onEnterVoiceUI,
                    )
                }
            } else {
                var useSidebarGesture by remember { mutableStateOf(true) }
                DismissibleNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = useSidebarGesture,
                    drawerContent = {
                        DismissibleDrawerSheet(
                            drawerContainerColor = Color.Transparent,
                            windowInsets = WindowInsets.zero,
                            modifier = Modifier.onSizeChanged {
                                drawerWidth = it.width.toFloat()
                            }
                        ) {
                            Sidebar(
                                viewModel = viewModel,
                                topNav = topNav,
                                currentServer = currentServer,
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
                                showSettingsButton = isTouchExplorationEnabled,
                                onOpenSettings = {
                                    topNav.navigate("settings")
                                },
                                drawerState = drawerState
                            )
                        }
                    },
                    content = {
                        Box(Modifier.fillMaxSize()) {
                            ChannelNavigator(
                                dest = viewModel.currentDestination,
                                topNav = topNav,
                                messageJump = viewModel.pendingMessageJump,
                                onMessageJumpConsumed = viewModel::consumeMessageJump,
                                useDrawer = true,
                                disableBackHandler = disableBackHandler,
                                toggleDrawer = {
                                    toggleDrawerLambda()
                                },
                                drawerState = drawerState,
                                drawerGestureEnabled = useSidebarGesture,
                                setDrawerGestureEnabled = {
                                    useSidebarGesture = it
                                },
                                onEnterVoiceUI = onEnterVoiceUI,
                            )

                            // This is the overlay on the main content when the drawer is open
                            val interactionSource = remember { MutableInteractionSource() }
                            Box(
                                Modifier
                                    .then(
                                        if (drawerState.isOpen) {
                                            Modifier.clickable(
                                                interactionSource = interactionSource,
                                                indication = null,
                                                enabled = drawerState.isOpen,
                                                onClick = {
                                                    scope.launch {
                                                        drawerState.close()
                                                    }
                                                }
                                            )
                                        } else Modifier
                                    )
                                    .fillMaxSize()
                                    .background(
                                        MaterialTheme
                                            .colorScheme
                                            .surfaceContainerLowest
                                            .copy(
                                                alpha = (1.0f + (drawerState.currentOffset / drawerWidth)) * 0.7f
                                            )
                                    )
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun Sidebar(
    viewModel: ChatRouterViewModel,
    currentServer: String?,
    topNav: NavController,
    drawerState: DrawerState? = null,
    onShowStatusSheet: () -> Unit,
    onShowServerContextSheet: (String) -> Unit,
    onShowAddServerSheet: () -> Unit,
    showSettingsButton: Boolean,
    onOpenSettings: () -> Unit,
) {
    ChannelSideDrawer(
        onDestinationChanged = viewModel::setSaveDestination,
        currentDestination = viewModel.currentDestination,
        currentServer = currentServer,
        drawerState = drawerState,
        navigateToServer = viewModel::navigateToServer,
        onLongPressAvatar = onShowStatusSheet,
        onShowServerContextSheet = onShowServerContextSheet,
        showSettingsIcon = showSettingsButton,
        onOpenSettings = onOpenSettings,
        topNav = topNav,
        onShowAddServerSheet = onShowAddServerSheet
    )
}

@Composable
fun ChannelNavigator(
    dest: ChatRouterDestination,
    topNav: NavController,
    messageJump: ChannelMessageJump? = null,
    onMessageJumpConsumed: (ChannelMessageJump) -> Unit = {},
    useDrawer: Boolean,
    toggleDrawer: () -> Unit,
    drawerState: DrawerState? = null,
    drawerGestureEnabled: Boolean = true,
    disableBackHandler: Boolean = false,
    onEnterVoiceUI: (String) -> Unit = {},
    setDrawerGestureEnabled: (Boolean) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val currentTopEntry by topNav.currentBackStackEntryAsState()

    BackHandler(useDrawer && !disableBackHandler) {
        toggleDrawer()
    }

    Column(Modifier.fillMaxSize()) {
        when (dest) {
            is ChatRouterDestination.Overview -> {
                OverviewScreen(
                    navController = topNav,
                    useDrawer = useDrawer,
                    onDrawerClicked = toggleDrawer,
                )
            }

            is ChatRouterDestination.Friends -> {
                FriendsScreen(
                    topNav = topNav,
                    useDrawer = useDrawer,
                    onDrawerClicked = toggleDrawer,
                )
            }

            is ChatRouterDestination.Channel -> {
                val routedMessageJump = messageJump?.takeIf {
                    it.channelId == dest.channelId
                }
                val requestedChannelId =
                    currentTopEntry?.savedStateHandle?.get<String>(
                        CHANNEL_MESSAGE_JUMP_CHANNEL_KEY
                    )
                val requestedMessageId =
                    currentTopEntry?.savedStateHandle?.get<String>(
                        CHANNEL_MESSAGE_JUMP_MESSAGE_KEY
                    )?.takeIf { requestedChannelId == dest.channelId }
                val effectiveRequestedMessageId =
                    routedMessageJump?.messageId ?: requestedMessageId

                ChannelScreen(
                    channelId = dest.channelId,
                    onToggleDrawer = {
                        scope.launch {
                            if (drawerState?.isOpen == true) {
                                drawerState.close()
                            } else {
                                drawerState?.open()
                            }
                        }
                    },
                    useDrawer = useDrawer,
                    drawerGestureEnabled = drawerGestureEnabled,
                    setDrawerGestureEnabled = setDrawerGestureEnabled,
                    drawerIsOpen = drawerState?.isOpen == true,
                    requestedMessageId = effectiveRequestedMessageId,
                    onRequestedMessageConsumed = {
                        routedMessageJump?.let(onMessageJumpConsumed)
                        currentTopEntry?.savedStateHandle?.remove<String>(
                            CHANNEL_MESSAGE_JUMP_CHANNEL_KEY
                        )
                        currentTopEntry?.savedStateHandle?.remove<String>(
                            CHANNEL_MESSAGE_JUMP_MESSAGE_KEY
                        )
                    },
                )
            }

            is ChatRouterDestination.NoCurrentChannel -> {
                NoCurrentChannelScreen(useDrawer = useDrawer, onDrawerClicked = toggleDrawer)
            }
        }
    }
}
