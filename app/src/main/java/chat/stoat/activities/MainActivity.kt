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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutExpo
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import chat.stoat.BuildConfig
import chat.stoat.R
import chat.stoat.api.HitRateLimitException
import chat.stoat.api.StoatAPI
import chat.stoat.api.StoatHttp
import chat.stoat.api.api
import chat.stoat.api.routes.microservices.geo.queryGeo
import chat.stoat.api.routes.microservices.health.healthCheck
import chat.stoat.api.routes.onboard.needsOnboarding
import chat.stoat.api.settings.Experiments
import chat.stoat.api.settings.GeoStateProvider
import chat.stoat.api.settings.LoadedSettings
import chat.stoat.api.settings.SyncedSettings
import chat.stoat.c2dm.NotificationDeepLink
import chat.stoat.composables.generic.HealthAlert
import chat.stoat.composables.voice.VoicePermissionSwitch
import chat.stoat.composables.voice.VoiceSheet
import chat.stoat.core.model.schemas.HealthNotice
import chat.stoat.internals.StoatWebLink
import chat.stoat.internals.toStoatWebLinkOrNull
import chat.stoat.material.EasingTokens
import chat.stoat.persistence.KVStorage
import chat.stoat.screens.DefaultDestinationScreen
import chat.stoat.screens.about.AboutScreen
import chat.stoat.screens.about.AttributionScreen
import chat.stoat.screens.changelogs.ReadChangelogScreen
import chat.stoat.screens.chat.CHANNEL_MESSAGE_JUMP_CHANNEL_KEY
import chat.stoat.screens.chat.CHANNEL_MESSAGE_JUMP_MESSAGE_KEY
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
import chat.stoat.screens.settings.channel.ChannelSettingsPermissions
import chat.stoat.ui.theme.StoatTheme
import chat.stoat.voice.VoiceCallManager
import io.ktor.client.request.get
import io.sentry.android.core.SentryAndroid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivityViewModel(
    private val kvStorage: KVStorage,
    private val context: Context
) : ViewModel() {
    val nextDestination = MutableStateFlow<String?>(null)
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

    private suspend fun startWithDestination(destination: String) {
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
                ?: return@launch startWithDestination("login/greeting")
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
                        startWithDestination("register/onboarding")
                        return@launch
                    }
                } catch (e: HitRateLimitException) {
                    Log.e("MainActivity", "Rate limited while checking onboarding state", e)
                    Toast.makeText(
                        context,
                        context.getString(R.string.rate_limit_toast),
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
                        startWithDestination("main")
                    } else {
                        startWithDestination("chat")
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
            startWithDestination("login/greeting")
        }
    }

    fun updateNextDestination(destination: String) {
        viewModelScope.launch {
            nextDestination.emit(null)
            nextDestination.emit(destination)
        }
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
                viewModel::updateNextDestination
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
            getString(R.string.keyboard_shortcut_messaging),
            listOf(
                KeyboardShortcutInfo(
                    getString(R.string.keyboard_shortcut_messaging_new_line),
                    KeyEvent.KEYCODE_ENTER,
                    0
                ),
                KeyboardShortcutInfo(
                    getString(R.string.keyboard_shortcut_messaging_send_message),
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.META_CTRL_ON
                )
            )
        )

        data?.add(messaging)
    }

}

val StoatTweenInt: FiniteAnimationSpec<IntOffset> = tween(400, easing = EaseInOutExpo)
val StoatTweenSize: FiniteAnimationSpec<IntSize> = tween(400, easing = EaseInOutExpo)
val StoatTweenFloat: FiniteAnimationSpec<Float> = tween(400, easing = EaseInOutExpo)
val StoatTweenDp: FiniteAnimationSpec<Dp> = tween(400, easing = EaseInOutExpo)
val StoatTweenColour: FiniteAnimationSpec<Color> = tween(400, easing = EaseInOutExpo)

val NavTweenInt: FiniteAnimationSpec<IntOffset> = tween(350, easing = EaseInOutExpo)
val NavTweenFloat: FiniteAnimationSpec<Float> = tween(350, easing = EaseInOutExpo)

// This composable handles the main compose entrypoint of the app, provides the main navigation
// graph, and handles the animation and layout for the voice chat UI.
@Composable
fun AppEntrypoint(
    windowSizeClass: WindowSizeClass,
    nextDestination: String?,
    isConnected: Boolean,
    healthNotice: HealthNotice?,
    isHealthAlertActive: Boolean,
    couldNotLogIn: Boolean,
    onLogout: () -> Unit = {},
    onDismissHealthAlert: () -> Unit = {},
    onDismissLoginError: () -> Unit = {},
    onRetryConnection: () -> Unit,
    onUpdateNextDestination: (String) -> Unit = {}
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

    val navController = rememberNavController()

    StoatTheme(
        requestedTheme = LoadedSettings.theme,
        requestedUserInterfaceFont = LoadedSettings.font,
        colourOverrides = SyncedSettings.android.colourOverrides
    ) {
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
                            Text(stringResource(R.string.could_not_log_in_heading))
                        },
                        text = {
                            Text(stringResource(R.string.could_not_log_in_body))
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    onDismissLoginError()
                                    onRetryConnection()
                                }
                            ) {
                                Text(stringResource(R.string.could_not_log_in_cta_try_again))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    onDismissLoginError()
                                    onLogout()
                                }
                            ) {
                                Text(stringResource(R.string.could_not_log_in_cta_logout))
                            }
                        }
                    )
                }

                NavHost(
                    navController = navController,
                    startDestination = "default",
                    enterTransition = {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = NavTweenInt,
                            initialOffset = { it / 3 }
                        ) + fadeIn(animationSpec = NavTweenFloat)
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = NavTweenInt,
                            targetOffset = { it / 3 }
                        ) + fadeOut(animationSpec = NavTweenFloat)
                    },
                    popEnterTransition = {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = NavTweenInt,
                            initialOffset = { it / 3 }
                        ) + fadeIn(animationSpec = NavTweenFloat)
                    },
                    popExitTransition = {
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = NavTweenInt,
                            targetOffset = { it / 2 }
                        ) + fadeOut(animationSpec = NavTweenFloat)
                    }
                ) {
                    composable("default") {
                        DefaultDestinationScreen(
                            navController,
                            nextDestination,
                            isConnected,
                            onRetryConnection
                        )
                    }

                    composable("login/greeting") { LoginGreetingScreen(navController) }
                    composable("login/login") { LoginScreen(navController) }
                    composable("login/mfa/{mfaTicket}/{allowedAuthTypes}") { backStackEntry ->
                        val mfaTicket = backStackEntry.arguments?.getString("mfaTicket") ?: ""
                        val allowedAuthTypes =
                            backStackEntry.arguments?.getString("allowedAuthTypes") ?: ""

                        MfaScreen(navController, allowedAuthTypes, mfaTicket)
                    }

                    composable("register/greeting") { RegisterGreetingScreen(navController) }
                    composable("register/details") { RegisterDetailsScreen(navController) }
                    composable("register/verify/{email}") { backStackEntry ->
                        val email = backStackEntry.arguments?.getString("email") ?: ""

                        RegisterVerifyScreen(navController, email)
                    }
                    composable("register/onboarding") {
                        OnboardingScreen(
                            navController,
                            onOnboardingComplete = {
                                onUpdateNextDestination("chat")
                                navController.popBackStack(
                                    navController.graph.startDestinationRoute!!,
                                    inclusive = true
                                )
                                navController.navigate("default")
                            }
                        )
                    }

                    composable("login2/init") { InitScreen(navController, windowSizeClass) }

                    // This is only used outside of Polar mode
                    // Otherwise you may be looking for "main" right below
                    composable(
                        "chat",
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Up,
                                animationSpec = tween(
                                    400,
                                    easing = EasingTokens.EmphasizedDecelerate
                                ),
                                initialOffset = { it / 3 }
                            ) + fadeIn(animationSpec = StoatTweenFloat)
                        }
                    ) {
                        ChatRouterScreen(
                            navController,
                            windowSizeClass,
                            disableBackHandler = showVoiceUI,
                            onNullifiedUser = {
                                onRetryConnection()
                                navController.popBackStack(
                                    navController.graph.startDestinationRoute!!,
                                    inclusive = true
                                )
                                navController.navigate("default")
                            },
                            onEnterVoiceUI = { channelId ->
                                VoiceCallManager.openSheet(channelId)
                            },
                        )
                    }

                    // This is only the main screen in Polar mode
                    // Otherwise you may be looking for "chat" right above
                    composable(
                        "main",
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Up,
                                animationSpec = tween(
                                    400,
                                    easing = EasingTokens.EmphasizedDecelerate
                                ),
                                initialOffset = { it / 3 }
                            ) + fadeIn(animationSpec = StoatTweenFloat) + scaleIn(
                                animationSpec = tween(
                                    400,
                                    easing = EasingTokens.EmphasizedDecelerate
                                ),
                                initialScale = 0.8f,
                                transformOrigin = TransformOrigin.Center
                            )
                        }
                    ) {
                        MainScreen(navController)
                    }
                    composable(
                        "main/conversation/{channelId}",
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(
                                    600,
                                    easing = EasingTokens.EmphasizedDecelerate
                                ),
                                initialOffset = { it }
                            ) + fadeIn(animationSpec = StoatTweenFloat)
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(
                                    600,
                                    easing = EasingTokens.EmphasizedDecelerate
                                ),
                                targetOffset = { it }
                            ) + fadeOut(animationSpec = StoatTweenFloat)
                        }
                    ) { backStackEntry ->
                        val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                        val requestedMessageId =
                            backStackEntry.savedStateHandle.get<String>(
                                CHANNEL_MESSAGE_JUMP_MESSAGE_KEY
                            )?.takeIf {
                                backStackEntry.savedStateHandle.get<String>(
                                    CHANNEL_MESSAGE_JUMP_CHANNEL_KEY
                                ) == channelId
                            }
                        ChannelScreen(
                            channelId = channelId,
                            onToggleDrawer = {},
                            useDrawer = false,
                            useBackButton = true,
                            backButtonAction = {
                                navController.popBackStack()
                            },
                            useChatUI = true,
                            requestedMessageId = requestedMessageId,
                            onRequestedMessageConsumed = {
                                backStackEntry.savedStateHandle.remove<String>(
                                    CHANNEL_MESSAGE_JUMP_CHANNEL_KEY
                                )
                                backStackEntry.savedStateHandle.remove<String>(
                                    CHANNEL_MESSAGE_JUMP_MESSAGE_KEY
                                )
                            },
                        )
                    }

                    composable("catchup") { CatchUpScreen(navController) }

                    composable("create/group") { CreateGroupScreen(navController) }

                    composable("discover") { DiscoverScreen(navController) }

                    composable("settings") { SettingsScreen(navController) }
                    composable("settings/account") { AccountSettingsScreen(navController) }
                    composable("settings/account/mfa") { MfaSettingsScreen(navController) }
                    composable("settings/profile") { ProfileSettingsScreen(navController) }
                    composable("settings/sessions") { SessionSettingsScreen(navController) }
                    composable("settings/appearance") { AppearanceSettingsScreen(navController) }
                    composable("settings/chat") { ChatSettingsScreen(navController) }
                    composable("settings/notifications") { NotificationsSettingsScreen(navController) }
                    composable("settings/debug") { DebugSettingsScreen(navController) }
                    composable("settings/experiments") { ExperimentsSettingsScreen(navController) }
                    composable("settings/language") { LanguagePickerSettingsScreen(navController) }

                    composable("settings/channel/{channelId}") { backStackEntry ->
                        val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                        ChannelSettingsHome(navController, channelId)
                    }
                    composable("settings/channel/{channelId}/overview") { backStackEntry ->
                        val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                        ChannelSettingsOverview(navController, channelId)
                    }
                    composable("settings/channel/{channelId}/permissions") { backStackEntry ->
                        val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                        ChannelSettingsPermissions(navController, channelId)
                    }

                    composable("channel/{channelId}/pins") { backStackEntry ->
                        val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                        ChannelPinsScreen(navController, channelId)
                    }

                    composable("channel/{channelId}/search") { backStackEntry ->
                        val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                        ChannelSearchScreen(navController, channelId)
                    }

                    composable("about") { AboutScreen(navController) }
                    composable("about/oss") { AttributionScreen(navController) }

                    composable("labs") { LabsRootScreen(navController) }

                    composable("changelog/{id}") { ReadChangelogScreen(navController) }
                }
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
