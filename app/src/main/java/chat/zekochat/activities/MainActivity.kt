package chat.zekochat.activities

import android.annotation.SuppressLint
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
import androidx.activity.viewModels
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
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import chat.zekochat.R
import chat.zekochat.PeptideApplication
import chat.zekochat.api.ApplicationPlatform
import chat.zekochat.api.HitRateLimitException
import chat.zekochat.api.PeptideAPI
import chat.zekochat.api.PeptideAPI.setPlatform
import chat.zekochat.api.PeptideHttp
import chat.zekochat.api.UrlsStorageKeys
import chat.zekochat.api.api
import chat.zekochat.api.routes.microservices.geo.queryGeo
import chat.zekochat.api.routes.microservices.health.healthCheck
import chat.zekochat.api.routes.onboard.needsOnboarding
import chat.zekochat.api.schemas.HealthNotice
import chat.zekochat.api.settings.Experiments
import chat.zekochat.api.settings.GeoStateProvider
import chat.zekochat.api.settings.LoadedSettings
import chat.zekochat.api.settings.SyncedSettings
import chat.zekochat.composables.generic.HealthAlert
import chat.zekochat.composables.generic.PepTextButton
import chat.zekochat.composables.voice.VoicePermissionSwitch
import chat.zekochat.composables.voice.VoiceSheet
import chat.zekochat.material.EasingTokens
import chat.zekochat.ndk.NativeLibraries
import chat.zekochat.persistence.KVStorage
import chat.zekochat.screens.DefaultDestinationScreen
import chat.zekochat.screens.about.AboutScreen
import chat.zekochat.screens.about.AttributionScreen
import chat.zekochat.screens.chat.ChatRouterScreen
import chat.zekochat.screens.chat.views.channel.ChannelScreen
import chat.zekochat.screens.chooseplatform.ChoosePlatformScreen
import chat.zekochat.screens.create.CreateGroupScreen
import chat.zekochat.screens.labs.LabsRootScreen
import chat.zekochat.screens.login.LoginGreetingScreen
import chat.zekochat.screens.login.LoginScreen
import chat.zekochat.screens.login.MfaScreen
import chat.zekochat.screens.login2.InitScreen
import chat.zekochat.screens.main.MainScreen
import chat.zekochat.screens.register.OnboardingScreen
import chat.zekochat.screens.register.RegisterDetailsScreen
import chat.zekochat.screens.register.RegisterGreetingScreen
import chat.zekochat.screens.register.RegisterVerifyScreen
import chat.zekochat.screens.services.DiscoverScreen
import chat.zekochat.screens.settings.AppearanceSettingsScreen
import chat.zekochat.screens.settings.ChangelogsSettingsScreen
import chat.zekochat.screens.settings.ChatSettingsScreen
import chat.zekochat.screens.settings.DebugSettingsScreen
import chat.zekochat.screens.settings.ExperimentsSettingsScreen
import chat.zekochat.screens.settings.LanguagePickerSettingsScreen
import chat.zekochat.screens.settings.ProfileSettingsScreen
import chat.zekochat.screens.settings.SessionSettingsScreen
import chat.zekochat.screens.settings.SettingsScreen
import chat.zekochat.screens.settings.channel.ChannelSettingsHome
import chat.zekochat.screens.settings.channel.ChannelSettingsOverview
import chat.zekochat.screens.settings.channel.ChannelSettingsPermissions
import chat.zekochat.ui.theme.PeptideTheme
import chat.zekochat.utils.DeepLinkHandler
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.request.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
@SuppressLint("StaticFieldLeak")
class MainActivityViewModel @Inject constructor(
    private val kvStorage: KVStorage,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val nextDestination = MutableStateFlow<String?>(null)
    var isConnected = MutableStateFlow(false)
    val isReady = MutableStateFlow(false)
    // Controls when we allow the first UI draw.
    // Authentication/navigation readiness is represented by `isReady`.
    val isUiReady = MutableStateFlow(false)
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

    private suspend fun canReachPeptide(): Boolean {
        try {
            val res = PeptideHttp.get("/".api())
            return res.status.value == 200
        } catch (_: Exception) {
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

    private fun doPostStartupTasks() {
        Log.d("MainActivity", "Performing post-startup tasks")
        viewModelScope.launch {
            Log.d("MainActivity", "Performing health check")
            doHealthCheck()
            Log.d("MainActivity", "Performing update geo state")
            updateGeoState()
        }
    }

    /**
     * Sets the default platform on application creation.
     * It retrieves the platform from key-value storage and sets it in the PeptideAPI.
     * If the platform is not found or invalid, it logs the user out.
     */
    fun setDefaultPlatformOnCreate() {
        viewModelScope.launch {
            ApplicationPlatform.fromName(
                name = kvStorage.get(UrlsStorageKeys.PLATFORM) ?: "---"
            )?.let { platform ->
                setPlatform(platform)
            } ?: run {
                logOut()
            }
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
            Log.d("MainActivity", "Hydrating Experiments from KV")
            Experiments.hydrateWithKv()

            isConnected.emit(hasInternetConnection())

            Log.d("MainActivity", "Checking if we can reach Peptide")

            if (!isConnected.value) return@launch startWithoutDestination()

            Log.d("MainActivity", "We can reach Peptide, checking if we're logged in")

            val token = kvStorage.get("sessionToken")
                ?: return@launch startWithDestination("choose-platform")
            val id = kvStorage.get("sessionId") ?: ""

            Log.d(
                "MainActivity",
                "We have a session token, checking if it's valid and if we can still reach Peptide"
            )

            val canReachPeptide = canReachPeptide()
            val valid = try {
                PeptideAPI.checkSessionToken(token)
            } catch (_: Throwable) {
                false
            }

            if (canReachPeptide && !valid) {
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
                    PeptideAPI.loginAs(token)
                    PeptideAPI.setSessionId(id)
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
            startWithDestination("choose-platform")
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
        // Do not block initial rendering on slow network/login checks.
        isUiReady.value = true
        checkLoggedInState()
        viewModelScope.launch {
            isReady.first { it }
            doPostStartupTasks()
        }
    }
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel by viewModels<MainActivityViewModel>()

    // Fix for SDK >=31, where core-splashscreen accidentally removes dynamic colours
    // See the other one in DefaultDestinationScreen.kt
    override fun onResume() {
        super.onResume()
        DynamicColors.applyToActivityIfAvailable(this)
        DynamicColors.applyToActivitiesIfAvailable(PeptideApplication.instance)
        @Suppress("DEPRECATION") // We are fixing a bug in the splash screen
        window.statusBarColor = Color.Transparent.toArgb()
    }

    // Same as above for configuration changes (rotation, dark mode, etc.)
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        DynamicColors.applyToActivityIfAvailable(this)
        DynamicColors.applyToActivitiesIfAvailable(PeptideApplication.instance)
        @Suppress("DEPRECATION") // We are fixing a bug in the splash screen
        window.statusBarColor = Color.Transparent.toArgb()
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    // waiting for view to draw to better represent a captured error with a screenshot
    findViewById<android.view.View>(android.R.id.content).viewTreeObserver.addOnGlobalLayoutListener {
      try {
        throw Exception("This app uses Sentry! :)")
      } catch (e: Exception) {
//        Sentry.captureException(e)
      }
    }


        // Set the default platform for the Platform API.
        viewModel.setDefaultPlatformOnCreate()

//        SentryAndroid.init(this) { options ->
//            options.dsn = BuildConfig.SENTRY_DSN
//            options.release = BuildConfig.VERSION_NAME
//        }

        @Suppress("DEPRECATION") // We are fixing a bug in the splash screen
        window.statusBarColor = Color.Transparent.toArgb()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        PeptideAPI.hydrateFromPersistentCache()
        
        // Handle deep link data if available
        handleDeepLinkData(intent)

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
                    return if (viewModel.isUiReady.value) {
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLinkData(intent)
    }
    
    private fun handleDeepLinkData(intent: Intent) {
        // Check for deep link data from DeepLinkActivity
        val channelId = intent.getStringExtra(DeepLinkActivity.PATH_CHANNEL)
        val serverId = intent.getStringExtra(DeepLinkActivity.PATH_SERVER)
        val userId = intent.getStringExtra(DeepLinkActivity.PATH_USER)
        val messageId = intent.getStringExtra(DeepLinkActivity.PATH_MESSAGE)
        val nextDestination = intent.getStringExtra("next_destination")
        
        if (nextDestination != null) {
            Log.d(TAG, "Setting next destination from deep link: $nextDestination")
            viewModel.updateNextDestination(nextDestination)
            return
        }
        
        // Store deep link data in a static companion object for ChatRouterScreen to access
        DeepLinkHandler.channelId = channelId
        DeepLinkHandler.serverId = serverId
        DeepLinkHandler.userId = userId
        DeepLinkHandler.messageId = messageId
        DeepLinkHandler.hasDeepLink = channelId != null || serverId != null || userId != null || messageId != null
        
        // Wait for the app to be ready before navigating to the deep link destination
        viewModel.viewModelScope.launch {
            viewModel.isReady.collect { isReady ->
                if (isReady) {
                    when {
                        channelId != null -> {
                            Log.d(TAG, "Navigating to channel from deep link: $channelId")
                            // Check if Polar mode is enabled
                            if (Experiments.usePolar.isEnabled) {
                                viewModel.updateNextDestination("main/conversation/$channelId")
                            } else {
                                // For non-Polar mode, we'll use the DeepLinkHandler to communicate with ChatRouterScreen
                                viewModel.updateNextDestination("chat")
                            }
                        }
                        serverId != null -> {
                            Log.d(TAG, "Navigating to server from deep link: $serverId")
                            // Navigate to chat screen, the ChatRouterScreen will handle the server navigation
                            viewModel.updateNextDestination("chat")
                        }
                        userId != null -> {
                            Log.d(TAG, "Navigating to user from deep link: $userId")
                            // Navigate to chat screen, the ChatRouterScreen will handle the user navigation
                            viewModel.updateNextDestination("chat")
                        }
                        messageId != null -> {
                            Log.d(TAG, "Navigating to message from deep link: $messageId")
                            // Navigate to chat screen, the ChatRouterScreen will handle the message navigation
                            viewModel.updateNextDestination("chat")
                        }
                    }
                    // Only collect once
                    return@collect
                }
            }
        }
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

    companion object {
        private const val TAG = "MainActivity"
        init {
            NativeLibraries.init()
        }
    }
}

val PeptideTweenInt: FiniteAnimationSpec<IntOffset> = tween(400, easing = EaseInOutExpo)
val PeptideTweenFloat: FiniteAnimationSpec<Float> = tween(400, easing = EaseInOutExpo)
val PeptideTweenDp: FiniteAnimationSpec<Dp> = tween(400, easing = EaseInOutExpo)

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
    var showVoiceUI by rememberSaveable { mutableStateOf(false) }
    var voiceChannelId by rememberSaveable { mutableStateOf<String?>(null) }

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
        showVoiceUI = false
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(showVoiceUI) {
        if (showVoiceUI) keyboardController?.hide()
    }

    val navController = rememberNavController()

    PeptideTheme(
        requestedTheme = LoadedSettings.theme,
        colourOverrides = SyncedSettings.android.colourOverrides,
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
                            PepTextButton(
                                onClick = {
                                    onDismissLoginError()
                                    onRetryConnection()
                                }
                            ) {
                                Text(stringResource(R.string.could_not_log_in_cta_try_again))
                            }
                        },
                        dismissButton = {
                            PepTextButton(
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

                    composable("choose-platform") { ChoosePlatformScreen(navController) }
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
                            ) + fadeIn(animationSpec = PeptideTweenFloat)
                        }
                    ) {
                        ChatRouterScreen(
                            navController,
                            windowSizeClass,
                            onNullifiedUser = {
                                onRetryConnection()
                                navController.popBackStack(
                                    navController.graph.startDestinationRoute!!,
                                    inclusive = true
                                )
                                navController.navigate("default")
                            },
                            onEnterVoiceUI = { channelId ->
                                showVoiceUI = true
                                voiceChannelId = channelId
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
                            ) + fadeIn(animationSpec = PeptideTweenFloat) + scaleIn(
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
                            ) + fadeIn(animationSpec = PeptideTweenFloat)
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(
                                    600,
                                    easing = EasingTokens.EmphasizedDecelerate
                                ),
                                targetOffset = { it }
                            ) + fadeOut(animationSpec = PeptideTweenFloat)
                        }
                    ) { backStackEntry ->
                        val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                        ChannelScreen(
                            channelId = channelId,
                            useDrawer = false,
                            useBackButton = true,
                            backToChannelsScreen = {
                                navController.navigate("main")
                            },
                            backButtonAction = {
                                navController.popBackStack()
                            },
                            useChatUI = true
                        )
                    }

                    composable("create/group") { CreateGroupScreen(navController) }

                    composable("discover") { DiscoverScreen(navController) }

                    composable("settings") { SettingsScreen(navController) }
                    composable("settings/profile") { ProfileSettingsScreen(navController) }
                    composable("settings/sessions") { SessionSettingsScreen(navController) }
                    composable("settings/appearance") { AppearanceSettingsScreen(navController) }
                    composable("settings/chat") { ChatSettingsScreen(navController) }
                    composable("settings/debug") { DebugSettingsScreen(navController) }
                    composable("settings/experiments") { ExperimentsSettingsScreen(navController) }
                    composable("settings/changelogs") { ChangelogsSettingsScreen(navController) }
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

                    composable("about") { AboutScreen(navController) }
                    composable("about/oss") { AttributionScreen(navController) }

                    composable("labs") { LabsRootScreen(navController) }
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
                            showVoiceUI = false
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
                            onCancel = {
                                showVoiceUI = false
                            }
                        ) {
                            voiceChannelId?.let {
                                VoiceSheet(
                                    it,
                                    onDisconnect = {
                                        showVoiceUI = false
                                        voiceChannelId = null
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
