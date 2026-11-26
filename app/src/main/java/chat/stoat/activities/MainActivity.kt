package chat.stoat.activities

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyboardShortcutGroup
import android.view.KeyboardShortcutInfo
import android.view.Menu
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.ResultEffect
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import chat.stoat.BuildConfig
import chat.stoat.api.HitRateLimitException
import chat.stoat.api.StoatAPI
import chat.stoat.api.StoatHttp
import chat.stoat.api.api
import chat.stoat.api.routes.microservices.geo.queryGeo
import chat.stoat.api.routes.microservices.health.healthCheck
import chat.stoat.api.routes.onboard.needsOnboarding
import chat.stoat.api.settings.Experiments
import chat.stoat.api.settings.GeoStateProvider
import chat.stoat.c2dm.NotificationDeepLink
import chat.stoat.composables.generic.HealthAlert
import chat.stoat.composables.voice.VoicePermissionSwitch
import chat.stoat.composables.voice.VoiceSheet
import chat.stoat.core.designsystem.theme.NavTweenFloat
import chat.stoat.core.designsystem.theme.NavTweenInt
import chat.stoat.core.designsystem.theme.StoatTweenFloat
import chat.stoat.core.model.schemas.HealthNotice
import chat.stoat.internals.StoatWebLink
import chat.stoat.internals.extensions.getStringBlocking
import chat.stoat.internals.toStoatWebLinkOrNull
import chat.stoat.material.EasingTokens
import chat.stoat.navigation.BackHandler
import chat.stoat.navigation.LocalDetailBesideList
import chat.stoat.navigation.Navigator
import chat.stoat.navigation.Route
import chat.stoat.navigation.RouteSavedStateConfiguration
import chat.stoat.navigation.listDetail
import chat.stoat.navigation.rememberListDetailSceneStrategy
import chat.stoat.persistence.KVStorage
import chat.stoat.platform.StoatAppTheme
import chat.stoat.screens.DefaultDestinationScreen
import chat.stoat.screens.about.AboutScreen
import chat.stoat.screens.about.AttributionScreen
import chat.stoat.screens.changelogs.ReadChangelogScreen
import chat.stoat.screens.chat.ChannelMessageJump
import chat.stoat.screens.chat.ChannelPinsScreen
import chat.stoat.screens.chat.ChannelSearchScreen
import chat.stoat.screens.chat.ChatRouterScreen
import chat.stoat.screens.chat.standalone.CatchUpScreen
import chat.stoat.screens.chat.views.channel.ChannelScreen
import chat.stoat.screens.create.CreateGroupScreen
import chat.stoat.screens.labs.LabsRootScreen
import chat.stoat.screens.login.LoginGreetingScreen
import chat.stoat.screens.login.LoginScreen
import chat.stoat.screens.login.MfaScreen
import chat.stoat.screens.login2.InitScreen
import chat.stoat.screens.main.MainScreen
import chat.stoat.screens.register.OnboardingScreen
import chat.stoat.screens.register.RegisterDetailsScreen
import chat.stoat.screens.register.RegisterGreetingScreen
import chat.stoat.screens.register.RegisterVerifyScreen
import chat.stoat.screens.services.DiscoverScreen
import chat.stoat.screens.settings.AccountSettingsScreen
import chat.stoat.screens.settings.AppearanceSettingsScreen
import chat.stoat.screens.settings.ChatSettingsScreen
import chat.stoat.screens.settings.DebugSettingsScreen
import chat.stoat.screens.settings.ExperimentsSettingsScreen
import chat.stoat.screens.settings.LanguagePickerSettingsScreen
import chat.stoat.screens.settings.MfaSettingsScreen
import chat.stoat.screens.settings.NotificationsSettingsScreen
import chat.stoat.screens.settings.ProfileSettingsScreen
import chat.stoat.screens.settings.SessionSettingsScreen
import chat.stoat.screens.settings.SettingsScreen
import chat.stoat.screens.settings.channel.ChannelSettingsHome
import chat.stoat.screens.settings.channel.ChannelSettingsOverview
import chat.stoat.screens.settings.channel.ChannelSettingsPermissionEditor
import chat.stoat.screens.settings.channel.ChannelSettingsPermissions
import chat.stoat.screens.settings.server.ServerIdentitySettingsScreen
import chat.stoat.screens.settings.server.ServerSettingsBans
import chat.stoat.screens.settings.server.ServerSettingsChannels
import chat.stoat.screens.settings.server.ServerSettingsEmojis
import chat.stoat.screens.settings.server.ServerSettingsHome
import chat.stoat.screens.settings.server.ServerSettingsInvites
import chat.stoat.screens.settings.server.ServerSettingsOverview
import chat.stoat.screens.settings.server.ServerSettingsRoleEditor
import chat.stoat.screens.settings.server.ServerSettingsRoles
import chat.stoat.shared.resources.Res
import chat.stoat.shared.resources.could_not_log_in_body
import chat.stoat.shared.resources.could_not_log_in_cta_logout
import chat.stoat.shared.resources.could_not_log_in_cta_try_again
import chat.stoat.shared.resources.could_not_log_in_heading
import chat.stoat.shared.resources.keyboard_shortcut_messaging
import chat.stoat.shared.resources.keyboard_shortcut_messaging_new_line
import chat.stoat.shared.resources.keyboard_shortcut_messaging_send_message
import chat.stoat.shared.resources.rate_limit_toast
import chat.stoat.voice.VoiceCallManager
import io.ktor.client.request.get
import io.sentry.android.core.SentryAndroid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivityViewModel(
    private val kvStorage: KVStorage,
    private val context: Context
) : ViewModel() {
    val nextDestination = MutableStateFlow<Route?>(null)
    var isConnected = MutableStateFlow(false)
    val isReady = MutableStateFlow(false)
    val couldNotLogIn = MutableStateFlow(false)

    private fun hasInternetConnection(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> true
            else -> false
        }
    }

    private suspend fun canReachStoat(): Boolean {
        try {
            val res = StoatHttp.get("/".api())
            return res.status.value == 200
        } catch (e: Exception) {
            return false
        }
    }

    private suspend fun startWithDestination(destination: Route) {
        nextDestination.emit(destination)
        isReady.emit(true)
    }

    private suspend fun startWithoutDestination() {
        isReady.emit(true)
    }

    private fun doPreStartupTasks() {
        Log.d("MainActivity", "Performing pre-startup tasks")
        viewModelScope.launch {
            Log.d("MainActivity", "Hydrating Experiments from KV")
            Experiments.hydrateWithKv()
            Log.d("MainActivity", "Performing health check")
            doHealthCheck()
            Log.d("MainActivity", "Performing update geo state")
            updateGeoState()
        }
    }

    private suspend fun updateGeoState() {
        try {
            Log.d("MainActivity", "Querying geo state")
            GeoStateProvider.updateGeoState(queryGeo())
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to query geo state", e)
        }
    }

    fun checkLoggedInState() {
        viewModelScope.launch {
            Log.d("MainActivity", "Checking logged in state")

            isConnected.emit(hasInternetConnection())

            Log.d("MainActivity", "Checking if we can reach Stoat")

            if (!isConnected.value) return@launch startWithoutDestination()

            Log.d("MainActivity", "We can reach Stoat, checking if we're logged in")

            val token = kvStorage.get("sessionToken")
                ?: return@launch startWithDestination(Route.Login.Greeting)
            val id = kvStorage.get("sessionId") ?: ""

            Log.d(
                "MainActivity",
                "We have a session token, checking if it's valid and if we can still reach Stoat"
            )

            val canReachStoat = canReachStoat()
            val valid = try {
                StoatAPI.checkSessionToken(token)
            } catch (e: Throwable) {
                false
            }

            if (canReachStoat && !valid) {
                Log.d("MainActivity", "Session token is invalid, could not log in")
                couldNotLogIn.emit(true)
            } else {
                try {
                    Log.d("MainActivity", "Session token is valid, checking onboarding state")
                    val onboard = needsOnboarding(token)
                    if (onboard) {
                        Log.d("MainActivity", "Onboarding state is incomplete, starting onboarding")
                        startWithDestination(Route.Register.Onboarding)
                        return@launch
                    }
                } catch (e: HitRateLimitException) {
                    Log.e("MainActivity", "Rate limited while checking onboarding state", e)
                    Toast.makeText(
                        context,
                        getString(Res.string.rate_limit_toast),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch startWithoutDestination()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to check onboarding state, could not log in", e)
                    couldNotLogIn.emit(true)
                }

                try {
                    Log.d("MainActivity", "Onboarding state is complete, logging in")
                    StoatAPI.loginAs(token)
                    StoatAPI.setSessionId(id)
                    if (Experiments.usePolar.isEnabled) {
                        startWithDestination(Route.Main.Home)
                    } else {
                        startWithDestination(Route.Chat)
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to login, could not log in", e)
                    couldNotLogIn.emit(true)
                }
            }
        }
    }

    fun logOut() {
        viewModelScope.launch {
            kvStorage.remove("sessionToken")
            kvStorage.remove("sessionId")
            kvStorage.remove("selfId")
            kvStorage.remove("selfName")
            kvStorage.remove("selfAvatarUrl")
            startWithDestination(Route.Login.Greeting)
        }
    }

    fun finishOnboarding() {
        nextDestination.value = null
        checkLoggedInState()
    }

    val activeAlert = MutableStateFlow<HealthNotice?>(null)
    val isAlertActive = MutableStateFlow(false)

    private fun doHealthCheck() {
        viewModelScope.launch {
            try {
                val health = healthCheck()
                if (health.alert != null) {
                    activeAlert.emit(health)
                    isAlertActive.emit(true)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to perform health check", e)
            }
        }
    }

    fun onDismissHealthAlert() {
        viewModelScope.launch {
            activeAlert.emit(null)
            isAlertActive.emit(false)
        }
    }

    fun onDismissLoginError() {
        viewModelScope.launch {
            couldNotLogIn.emit(false)
        }
    }

    init {
        Log.d("MainActivity", "Starting up")
        doPreStartupTasks()
        checkLoggedInState()
    }
}

class MainActivity : AppCompatActivity() {
    private val viewModel: MainActivityViewModel by viewModel()

    // The window can lose its transparent status bar after the activity is re-shown
    // See the other one in DefaultDestinationScreen.kt
    override fun onResume() {
        super.onResume()
        @Suppress("DEPRECATION") // no Compose-side equivalent for this window flag
        window.statusBarColor = Color.Transparent.toArgb()
    }

    // Same as above for configuration changes (rotation, dark mode, etc.)
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        @Suppress("DEPRECATION") // no Compose-side equivalent for this window flag
        window.statusBarColor = Color.Transparent.toArgb()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavigationIntent(intent)
    }

    private fun handleNavigationIntent(intent: Intent) {
        val webLink = intent.data?.toStoatWebLinkOrNull()
        if (webLink != null) {
            NotificationDeepLink.pendingNavigation.value = webLink
            return
        }

        val channelId = intent.getStringExtra("channelId") ?: return
        val messageId = intent.getStringExtra("messageId")
        NotificationDeepLink.pendingNavigation.value = if (messageId != null) {
            StoatWebLink.Message(channelId, messageId)
        } else {
            StoatWebLink.Channel(channelId)
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SentryAndroid.init(this) { options ->
            options.dsn = BuildConfig.SENTRY_DSN
            options.release = BuildConfig.VERSION_NAME
        }

        @Suppress("DEPRECATION") // We are fixing a bug in the splash screen
        window.statusBarColor = Color.Transparent.toArgb()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        StoatAPI.hydrateFromPersistentCache()

        handleNavigationIntent(intent)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            AppEntrypoint(
                windowSizeClass,
                viewModel.nextDestination.collectAsState().value,
                viewModel.isConnected.collectAsState().value,
                viewModel.activeAlert.collectAsState().value,
                viewModel.isAlertActive.collectAsState().value,
                viewModel.couldNotLogIn.collectAsState().value,
                viewModel::logOut,
                viewModel::onDismissHealthAlert,
                viewModel::onDismissLoginError,
                viewModel::checkLoggedInState,
                viewModel::finishOnboarding
            )
        }

        val content: View = findViewById(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    // Check whether the initial data is ready.
                    return if (viewModel.isReady.value) {
                        // The content is ready. Start drawing.
                        content.viewTreeObserver.removeOnPreDrawListener(this)
                        true
                    } else {
                        // The content isn't ready. Suspend.
                        false
                    }
                }
            }
        )
    }

    override fun onProvideKeyboardShortcuts(
        data: MutableList<KeyboardShortcutGroup>?,
        menu: Menu?,
        deviceId: Int
    ) {
        val messaging = KeyboardShortcutGroup(
            getStringBlocking(Res.string.keyboard_shortcut_messaging),
            listOf(
                KeyboardShortcutInfo(
                    getStringBlocking(Res.string.keyboard_shortcut_messaging_new_line),
                    KeyEvent.KEYCODE_ENTER,
                    0
                ),
                KeyboardShortcutInfo(
                    getStringBlocking(Res.string.keyboard_shortcut_messaging_send_message),
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.META_CTRL_ON
                )
            )
        )

        data?.add(messaging)
    }

}


// This composable handles the main compose entrypoint of the app, provides the main navigation
// graph, and handles the animation and layout for the voice chat UI.
@Composable
fun AppEntrypoint(
    windowSizeClass: WindowSizeClass,
    nextDestination: Route?,
    isConnected: Boolean,
    healthNotice: HealthNotice?,
    isHealthAlertActive: Boolean,
    couldNotLogIn: Boolean,
    onLogout: () -> Unit = {},
    onDismissHealthAlert: () -> Unit = {},
    onDismissLoginError: () -> Unit = {},
    onRetryConnection: () -> Unit,
    onOnboardingComplete: () -> Unit = {}
) {
    val showVoiceUI = VoiceCallManager.isSheetVisible
    val hideVoiceUI = { VoiceCallManager.isSheetVisible = false }
    val disconnectVoice = { VoiceCallManager.leave() }

    val chatUIScale by animateFloatAsState(
        if (showVoiceUI) 0.8f else 1.0f,
        animationSpec = tween(
            durationMillis = 300,
            easing = EasingTokens.EmphasizedDecelerate
        )
    )
    val chatUIOpacity by animateFloatAsState(
        if (showVoiceUI) 0.8f else 1.0f,
        animationSpec = tween(
            durationMillis = 300,
            easing = EasingTokens.EmphasizedDecelerate
        )
    )

    BackHandler(showVoiceUI) {
        hideVoiceUI()
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(showVoiceUI) {
        if (showVoiceUI) keyboardController?.hide()
    }

    val backStack = rememberNavBackStack(RouteSavedStateConfiguration, Route.Default)
    val navigator = remember(backStack) { Navigator(backStack) }

    StoatAppTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(chatUIScale)
                    .alpha(chatUIOpacity),
                color = MaterialTheme.colorScheme.background
            ) {
                if (isHealthAlertActive) {
                    healthNotice?.let {
                        HealthAlert(notice = healthNotice, onDismiss = onDismissHealthAlert)
                    }
                }

                if (couldNotLogIn) {
                    AlertDialog(
                        onDismissRequest = {
                            // no-op
                        },
                        title = {
                            Text(stringResource(Res.string.could_not_log_in_heading))
                        },
                        text = {
                            Text(stringResource(Res.string.could_not_log_in_body))
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    onDismissLoginError()
                                    onRetryConnection()
                                }
                            ) {
                                Text(stringResource(Res.string.could_not_log_in_cta_try_again))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    onDismissLoginError()
                                    onLogout()
                                }
                            ) {
                                Text(stringResource(Res.string.could_not_log_in_cta_logout))
                            }
                        }
                    )
                }

                NavDisplay(
                    backStack = backStack,
                    onBack = { navigator.popBackStack() },
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                        rememberResultEventBusNavEntryDecorator(),
                    ),
                    transitionSpec = { forwardEnter() togetherWith forwardExit() },
                    popTransitionSpec = { backEnter() togetherWith backExit() },
                    predictivePopTransitionSpec = { backEnter() togetherWith backExit() },
                    sceneStrategies = listOf(rememberListDetailSceneStrategy(backStack)),
                    entryProvider = entryProvider {
                        entry<Route.Default> {
                            DefaultDestinationScreen(
                                navigator,
                                nextDestination,
                                isConnected,
                                onRetryConnection
                            )
                        }

                        entry<Route.Login.Greeting> { LoginGreetingScreen(navigator) }
                        entry<Route.Login.Credentials> { LoginScreen(navigator) }
                        entry<Route.Login.Mfa> {
                            MfaScreen(
                                navigator,
                                it.allowedAuthTypes,
                                it.ticket
                            )
                        }

                        entry<Route.Register.Greeting> { RegisterGreetingScreen(navigator) }
                        entry<Route.Register.Details> { RegisterDetailsScreen(navigator) }
                        entry<Route.Register.Verify> { RegisterVerifyScreen(navigator, it.email) }
                        entry<Route.Register.Onboarding> {
                            OnboardingScreen(
                                navigator,
                                onOnboardingComplete = onOnboardingComplete
                            )
                        }

                        entry<Route.Login2.Init> { InitScreen(navigator, windowSizeClass) }

                        // This is only used outside of Polar mode
                        // Otherwise you may be looking for Main right below
                        entry<Route.Chat>(
                            metadata = NavDisplay.transitionSpec {
                                slideInVertically(
                                    tween(400, easing = EasingTokens.EmphasizedDecelerate)
                                ) { it / 3 } + fadeIn(StoatTweenFloat) togetherWith forwardExit()
                            }
                        ) {
                            ChatRouterScreen(
                                navigator,
                                windowSizeClass,
                                disableBackHandler = showVoiceUI,
                                onNullifiedUser = {
                                    onRetryConnection()
                                    navigator.resetTo(Route.Default)
                                },
                                onEnterVoiceUI = { channelId ->
                                    VoiceCallManager.openSheet(channelId)
                                },
                            )
                        }

                        // This is only the main screen in Polar mode
                        // Otherwise you may be looking for Chat right above
                        listDetail(PaneScene.Main, navigator) {
                            list<Route.Main.Home>(
                                metadata = NavDisplay.transitionSpec {
                                    slideInVertically(
                                        tween(400, easing = EasingTokens.EmphasizedDecelerate)
                                    ) { it / 3 } + fadeIn(StoatTweenFloat) + scaleIn(
                                        tween(400, easing = EasingTokens.EmphasizedDecelerate),
                                        initialScale = 0.8f,
                                        transformOrigin = TransformOrigin.Center
                                    ) togetherWith forwardExit()
                                }
                            ) { _, nav ->
                                MainScreen(nav)
                            }
                            detail<Route.Main.Conversation>(
                                metadata = NavDisplay.transitionSpec {
                                    slideInHorizontally(
                                        tween(600, easing = EasingTokens.EmphasizedDecelerate)
                                    ) { it } + fadeIn(StoatTweenFloat) togetherWith forwardExit()
                                } + NavDisplay.popTransitionSpec {
                                    backEnter() togetherWith conversationExit()
                                } + NavDisplay.predictivePopTransitionSpec {
                                    backEnter() togetherWith conversationExit()
                                }
                            ) { key, nav ->
                                var requestedMessageId by remember { mutableStateOf<String?>(null) }
                                ResultEffect<ChannelMessageJump> {
                                    if (it.channelId == key.channelId) requestedMessageId =
                                        it.messageId
                                }
                                ChannelScreen(
                                    channelId = key.channelId,
                                    onToggleDrawer = {},
                                    useDrawer = false,
                                    useBackButton = !LocalDetailBesideList.current,
                                    backButtonAction = {
                                        nav.popBackStack()
                                    },
                                    useChatUI = true,
                                    requestedMessageId = requestedMessageId,
                                    onRequestedMessageConsumed = { requestedMessageId = null },
                                )
                            }
                        }

                        entry<Route.CatchUp> { CatchUpScreen(navigator) }

                        entry<Route.CreateGroup> { CreateGroupScreen(navigator) }

                        entry<Route.Discover> { DiscoverScreen(navigator) }

                        listDetail(PaneScene.Settings, navigator) {
                            list<Route.Settings.Home> { _, nav -> SettingsScreen(nav) }
                            detail<Route.Settings.Account> { _, nav -> AccountSettingsScreen(nav) }
                            detail<Route.Settings.Profile> { _, nav -> ProfileSettingsScreen(nav) }
                            detail<Route.Settings.Sessions> { _, nav -> SessionSettingsScreen(nav) }
                            detail<Route.Settings.Appearance> { _, nav ->
                                AppearanceSettingsScreen(
                                    nav
                                )
                            }
                            detail<Route.Settings.Language> { _, nav ->
                                LanguagePickerSettingsScreen(
                                    nav
                                )
                            }
                            detail<Route.Settings.Chat> { _, nav -> ChatSettingsScreen(nav) }
                            detail<Route.Settings.Notifications> { _, nav ->
                                NotificationsSettingsScreen(
                                    nav
                                )
                            }
                            detail<Route.About.Home> { _, nav -> AboutScreen(nav) }
                            detail<Route.Settings.Debug> { _, nav -> DebugSettingsScreen(nav) }
                            detail<Route.Settings.Experiments> { _, nav ->
                                ExperimentsSettingsScreen(
                                    nav
                                )
                            }
                            detail<Route.Settings.AccountMfa> { _, nav -> MfaSettingsScreen(nav) }
                            detail<Route.About.Oss> { _, nav -> AttributionScreen(nav) }
                        }

                        listDetail(PaneScene.ChannelSettings, navigator) {
                            list<Route.ChannelSettings.Home> { key, nav ->
                                ChannelSettingsHome(
                                    nav,
                                    key.channelId
                                )
                            }
                            detail<Route.ChannelSettings.Overview> { key, nav ->
                                ChannelSettingsOverview(
                                    nav,
                                    key.channelId
                                )
                            }
                            detail<Route.ChannelSettings.Permissions> { key, nav ->
                                ChannelSettingsPermissions(
                                    nav,
                                    key.channelId
                                )
                            }
                            detail<Route.ChannelSettings.PermissionEditor> { key, nav ->
                                ChannelSettingsPermissionEditor(nav, key.channelId, key.roleId)
                            }
                        }

                        listDetail(PaneScene.ServerSettings, navigator) {
                            list<Route.ServerSettings.Home> { key, nav ->
                                ServerSettingsHome(
                                    nav,
                                    key.serverId
                                )
                            }
                            detail<Route.ServerSettings.Overview> { key, nav ->
                                ServerSettingsOverview(
                                    nav,
                                    key.serverId
                                )
                            }
                            detail<Route.ServerSettings.Channels> { key, nav ->
                                ServerSettingsChannels(
                                    nav,
                                    key.serverId
                                )
                            }
                            detail<Route.ServerSettings.Emojis> { key, nav ->
                                ServerSettingsEmojis(
                                    nav,
                                    key.serverId
                                )
                            }
                            detail<Route.ServerSettings.Roles> { key, nav ->
                                ServerSettingsRoles(
                                    nav,
                                    key.serverId
                                )
                            }
                            detail<Route.ServerSettings.Invites> { key, nav ->
                                ServerSettingsInvites(
                                    nav,
                                    key.serverId
                                )
                            }
                            detail<Route.ServerSettings.Bans> { key, nav ->
                                ServerSettingsBans(
                                    nav,
                                    key.serverId
                                )
                            }
                            detail<Route.ServerSettings.RoleEditor> { key, nav ->
                                ServerSettingsRoleEditor(nav, key.serverId, key.roleId)
                            }
                        }

                        entry<Route.ServerSettings.Identity> { key ->
                            (key.userId ?: StoatAPI.selfId)?.let { userId ->
                                ServerIdentitySettingsScreen(navigator, key.serverId, userId)
                            }
                        }

                        entry<Route.Channel.Pins> { ChannelPinsScreen(navigator, it.channelId) }
                        entry<Route.Channel.Search> { ChannelSearchScreen(navigator, it.channelId) }

                        entry<Route.Labs> { LabsRootScreen(navigator) }

                        entry<Route.Changelog> { ReadChangelogScreen(navigator, it.id) }
                    }
                )
            }
            if (showVoiceUI) { // if tapped outside the voice UI, close it
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            hideVoiceUI()
                        }
                )
            }

            AnimatedVisibility(
                visible = showVoiceUI,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(
                    initialOffsetY = { it -> it },
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = EasingTokens.EmphasizedDecelerate
                    )
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it -> it },
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = EasingTokens.EmphasizedDecelerate
                    )
                )
            ) {
                // We need a box as applying the padding elsewhere leads to either
                // janky animation or layout
                Box(Modifier.safeDrawingPadding()) {
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .padding(8.dp)
                    ) {
                        VoicePermissionSwitch(
                            onCancel = disconnectVoice
                        ) {
                            VoiceCallManager.requestedChannelId?.let { channelId ->
                                LaunchedEffect(channelId) {
                                    VoiceCallManager.join(channelId)
                                }
                                VoiceSheet(
                                    onDisconnect = disconnectVoice
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun forwardEnter() = slideInHorizontally(NavTweenInt) { it / 3 } + fadeIn(NavTweenFloat)

private fun forwardExit() = slideOutHorizontally(NavTweenInt) { -it / 3 } + fadeOut(NavTweenFloat)

private fun backEnter() = slideInHorizontally(NavTweenInt) { -it / 3 } + fadeIn(NavTweenFloat)

private fun backExit() = slideOutHorizontally(NavTweenInt) { it / 2 } + fadeOut(NavTweenFloat)

private fun conversationExit() =
    slideOutHorizontally(tween(600, easing = EasingTokens.EmphasizedDecelerate)) { it } +
            fadeOut(StoatTweenFloat)

private enum class PaneScene { Main, Settings, ChannelSettings, ServerSettings }
