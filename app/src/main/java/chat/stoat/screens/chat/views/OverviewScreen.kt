package chat.stoat.screens.chat.views

import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.Badge
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.activities.InviteActivity
import chat.stoat.api.StoatAPI
import chat.stoat.api.routes.user.fetchSelf
import chat.stoat.composables.generic.NonIdealState
import chat.stoat.composables.screens.settings.UserOverview
import chat.stoat.composables.skeletons.UserOverviewSkeleton
import chat.stoat.core.model.schemas.User
import chat.stoat.internals.extensions.zero
import chat.stoat.screens.chat.LocalIsConnected
import chat.stoat.sheets.UserCardSheet
import io.sentry.Sentry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    navController: NavController,
    useDrawer: Boolean,
    onDrawerClicked: () -> Unit,
    includePadding: Boolean = true
) {
    val context = LocalContext.current
    val resources = LocalResources.current

    var isLoading by rememberSaveable { mutableStateOf(true) }
    var user by rememberSaveable { mutableStateOf<User?>(null) }
    LaunchedEffect(Unit) {
        val inCache = StoatAPI.userCache[StoatAPI.selfId]
        if (inCache != null) {
            user = inCache
            isLoading = false
        } else {
            try {
                fetchSelf().let {
                    user = it
                    isLoading = false
                }
            } catch (e: Exception) {
                Log.e("OverviewScreen", "Failed to fetch self", e)
                Sentry.captureException(e)
                isLoading = false
            }
        }
    }

    var showUserCardSheet by rememberSaveable { mutableStateOf(false) }

    if (showUserCardSheet) {
        val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            sheetState = state,
            onDismissRequest = { showUserCardSheet = false },
        ) {
            UserCardSheet(user = user)
        }
    }

    var hasUnreads by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        hasUnreads = StoatAPI.unreads.hasAnyUnreads()
    }

    var countUnreads by rememberSaveable { mutableStateOf<Int?>(null) }
    LaunchedEffect(hasUnreads) {
        countUnreads = if (hasUnreads) {
            StoatAPI.unreads.countChannelsWithUnreads()
        } else {
            0
        }
    }

    val lazyStaggeredGridState = rememberLazyStaggeredGridState()
    val scrollabilityIndicatorOpacity by animateFloatAsState(
        if (!lazyStaggeredGridState.canScrollBackward && lazyStaggeredGridState.canScrollForward) 1.0f else 0.0f,
        label = "scrollabilityIndicatorOpacity"
    )

    Scaffold(
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
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.overview_screen_title)) },
                    navigationIcon = {
                        if (useDrawer) {
                            IconButton(onClick = {
                                onDrawerClicked()
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_menu_24dp),
                                    contentDescription = stringResource(id = R.string.menu)
                                )
                            }
                        }
                    },
                    windowInsets = WindowInsets.zero
                )
            }
        },
        contentWindowInsets = if (includePadding) ScaffoldDefaults.contentWindowInsets else ScaffoldDefaults.contentWindowInsets.exclude(
            NavigationBarDefaults.windowInsets
        )
    ) { pv ->
        if (user == null && !isLoading) {
            NonIdealState(
                icon = { size ->
                    Icon(
                        painter = painterResource(R.drawable.ic_error_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(size)
                    )
                },
                title = { Text(stringResource(R.string.overview_screen_error)) },
                description = { Text(stringResource(R.string.overview_screen_error_description)) }
            )
            return@Scaffold
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .then(
                    Modifier
                        .padding(pv)
                        .padding(horizontal = 16.dp)
                )
        ) {
            AnimatedContent(targetState = isLoading, label = "isLoading") { loading ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (loading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        UserOverviewSkeleton(false)
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                        user?.let { user ->
                            UserOverview(user, internalPadding = false)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(Modifier.size(48.dp))
            }

            AnimatedVisibility(
                visible = isLoading
            ) {
                Spacer(modifier = Modifier.height(48.dp))
            }

            AnimatedVisibility(
                visible = !isLoading,
                enter = fadeIn(),
            ) {
                Box {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Adaptive(300.dp),
                        verticalItemSpacing = 16.dp,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        state = lazyStaggeredGridState
                    ) {
                        if (false) { // TODO - implement catch up screen
                            item(key = "catchup") {
                                OverviewScreenLink(
                                    onClick = {
                                        if (hasUnreads) navController.navigate("catchup")
                                    },
                                    clickable = hasUnreads,
                                    backgroundColour = if (hasUnreads) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    foregroundColour = if (hasUnreads) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    title = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                painter = if (hasUnreads) painterResource(R.drawable.ic_move_to_inbox_24dp) else painterResource(
                                                    R.drawable.ic_inbox_24dp
                                                ),
                                                contentDescription = null,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Text(
                                                if (hasUnreads) stringResource(R.string.overview_screen_catch_up) else stringResource(
                                                    R.string.overview_screen_catch_up_none
                                                )
                                            )

                                            if (hasUnreads) {
                                                Spacer(Modifier.weight(1f))
                                                Badge {
                                                    Text(
                                                        countUnreads?.toString()
                                                            ?: stringResource(R.string.overview_screen_catch_up_amount_badge_loading)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    body = {
                                        Text(
                                            if (hasUnreads) stringResource(R.string.overview_screen_catch_up_description) else stringResource(
                                                R.string.overview_screen_catch_up_none_description
                                            )
                                        )
                                    }
                                )
                            }
                        }

                        item(key = "settings") {
                            OverviewScreenLink(
                                onClick = {
                                    navController.navigate("settings")
                                },
                                backgroundColour = MaterialTheme.colorScheme.primaryContainer,
                                foregroundColour = MaterialTheme.colorScheme.onPrimaryContainer,
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_settings_24dp),
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(stringResource(R.string.overview_screen_settings))
                                    }
                                },
                                body = { Text(stringResource(R.string.overview_screen_settings_description)) }
                            )
                        }
                        item(key = "shareProfile") {
                            OverviewScreenLink(
                                onClick = {
                                    showUserCardSheet = true
                                },
                                backgroundColour = MaterialTheme.colorScheme.tertiaryContainer,
                                foregroundColour = MaterialTheme.colorScheme.onTertiaryContainer,
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_ios_share_24dp),
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(stringResource(R.string.overview_screen_share_profile))
                                    }
                                },
                                body = { Text(stringResource(R.string.overview_screen_share_profile_description)) }
                            )
                        }

                        item(key = "changelog") {
                            OverviewScreenLink(
                                onClick = {
                                    navController.navigate("changelog/latest")
                                },
                                backgroundColour = MaterialTheme.colorScheme.errorContainer,
                                foregroundColour = MaterialTheme.colorScheme.onErrorContainer,
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_campaign_24dp),
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(stringResource(R.string.overview_screen_changelog))
                                    }
                                },
                                body = { Text(stringResource(R.string.overview_screen_changelog_description)) }
                            )
                        }

                        item(key = "join-lounge") {
                            OverviewScreenLink(
                                onClick = {
                                    val intent = Intent(
                                        context,
                                        InviteActivity::class.java
                                    ).setAction(Intent.ACTION_VIEW)

                                    intent.data = "https://stt.gg/Testers".toUri()
                                    context.startActivity(intent)
                                },
                                backgroundColour = MaterialTheme.colorScheme.primary,
                                foregroundColour = MaterialTheme.colorScheme.onPrimary,
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_waving_hand_24dp),
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(stringResource(R.string.overview_screen_join_lounge))
                                    }
                                },
                                body = { Text(stringResource(R.string.overview_screen_join_lounge_description)) }
                            )
                        }
                    }

                    Box(
                        Modifier
                            .alpha(scrollabilityIndicatorOpacity)
                            .background(
                                Brush.linearGradient(
                                    0.0f to MaterialTheme.colorScheme.background.copy(alpha = 0.0f),
                                    0.5f to MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                                    1.0f to MaterialTheme.colorScheme.background,
                                    end = Offset.Infinite.copy(x = .0f)
                                )
                            )
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(60.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OverviewScreenLink(
    onClick: () -> Unit,
    backgroundColour: Color,
    foregroundColour: Color,
    clickable: Boolean = true,
    title: @Composable () -> Unit,
    body: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
            .background(backgroundColour)
            .padding(vertical = 32.dp)
            .fillMaxWidth()
    ) {
        CompositionLocalProvider(LocalContentColor provides foregroundColour) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                    title()
                }
                ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                    body()
                }
            }
        }
    }
}