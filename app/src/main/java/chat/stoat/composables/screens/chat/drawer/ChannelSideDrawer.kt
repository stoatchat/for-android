package chat.stoat.composables.screens.chat.drawer

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.CategorisedChannelList
import chat.stoat.api.internals.ChannelUtils
import chat.stoat.api.internals.DirectMessages
import chat.stoat.api.internals.FriendRequests
import chat.stoat.api.routes.user.addUserIfUnknown
import chat.stoat.api.settings.GeoStateProvider
import chat.stoat.api.settings.NotificationSettingsProvider
import chat.stoat.api.settings.SyncedSettings
import chat.stoat.composables.generic.GroupIcon
import chat.stoat.composables.generic.IconPlaceholder
import chat.stoat.composables.generic.RemoteImage
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.composables.generic.bottomEndCircleCutout
import chat.stoat.composables.generic.presenceFromStatus
import chat.stoat.composables.screens.chat.ChannelIcon
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.schemas.Category
import chat.stoat.core.model.schemas.Channel
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.core.model.schemas.ServerFlags
import chat.stoat.core.model.schemas.User
import chat.stoat.core.model.schemas.has
import chat.stoat.core.model.util.UserVoiceState
import chat.stoat.screens.chat.ChatRouterDestination
import chat.stoat.screens.chat.LocalIsConnected
import chat.stoat.sheets.ChannelContextSheet
import chat.stoat.ui.theme.FragmentMono
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import logcat.LogPriority
import logcat.asLog
import logcat.logcat

private val ServerVoiceBadgeSize = 16.dp
private val ServerVoiceBadgeIconSize = 12.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChannelSideDrawer(
    currentServer: String?,
    currentDestination: ChatRouterDestination,
    onDestinationChanged: (ChatRouterDestination) -> Unit,
    onLongPressAvatar: () -> Unit,
    drawerState: DrawerState?,
    navigateToServer: (String) -> Unit,
    onShowServerContextSheet: (String) -> Unit,
    showSettingsIcon: Boolean,
    onOpenSettings: () -> Unit,
    topNav: NavController,
    onShowAddServerSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val server = StoatAPI.serverCache[currentServer]
    val categorisedChannels = server?.let {
        ChannelUtils.categoriseServerFlat(it)
    }
    val channelListState = rememberLazyListState()

    LaunchedEffect(currentDestination) {
        if (currentDestination is ChatRouterDestination.Channel && currentServer != null) {
            val channelIndex = categorisedChannels?.indexOfFirst {
                when (it) {
                    is CategorisedChannelList.Channel -> it.channel.id == currentDestination.channelId
                    else -> false
                }
            } ?: 0
            val firstVisibleIndex = kotlin.math.max(0, channelIndex - 2)

            // Add an offset to the scroll position so it is obvious to the user that they are not at the top.
            channelListState.animateScrollToItem(
                firstVisibleIndex,
                if (firstVisibleIndex == 0) 0 else 85
            )
        }
    }

    val isAtFirst by remember { derivedStateOf { channelListState.firstVisibleItemIndex == 0 } }
    val serverBannerHeight by animateDpAsState(
        targetValue = if (server?.banner == null) {
            76.dp // Magic number deducted by trial and error
        } else if (isAtFirst) {
            192.dp
        } else {
            128.dp
        },
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = 0
        ), label = "Server banner height"
    )

    val serverInfoOffset by animateDpAsState(
        if (LocalIsConnected.current)
            WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        else
            0.dp,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = Dp.VisibilityThreshold
        )
    )

    // - Take the list of servers and filter them by the ones that are in the ordering.
    // - Sort the servers that are in the ordering using the ordering.
    // - Add the servers that aren't in the ordering to the end of the list.
    // - Sort the servers that aren't in the ordering by their ID (creation order).
    val serverList = ((StoatAPI.serverCache.values.filter {
        SyncedSettings.ordering.servers.contains(
            it.id
        )
    }
        .sortedBy { SyncedSettings.ordering.servers.indexOf(it.id) }) + (StoatAPI.serverCache.values.filter {
        !SyncedSettings.ordering.servers.contains(
            it.id
        )
    }.sortedBy { it.id }))

    var channelContextSheetTarget by remember { mutableStateOf<String?>(null) }

    if (channelContextSheetTarget != null) {
        val channelContextSheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            sheetState = channelContextSheetState,
            onDismissRequest = {
                channelContextSheetTarget = null
            }
        ) {
            ChannelContextSheet(
                channelId = channelContextSheetTarget!!,
                onHideSheet = {
                    channelContextSheetState.hide()
                    channelContextSheetTarget = null
                }
            )
        }
    }

    val scope = rememberCoroutineScope()

    Row(modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.width(64.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            )
        ) {
            stickyHeader(key = "self") {
                Column(Modifier.background(MaterialTheme.colorScheme.background)) {
                    AnimatedVisibility(LocalIsConnected.current) {
                        Spacer(
                            Modifier
                                .height(
                                    WindowInsets.statusBars.asPaddingValues()
                                        .calculateTopPadding()
                                )
                        )
                    }
                    UserAvatar(
                        username = StoatAPI.userCache[StoatAPI.selfId]?.let {
                            User.resolveDefaultName(
                                it
                            )
                        }
                            ?: "",
                        presence = presenceFromStatus(
                            StoatAPI.userCache[StoatAPI.selfId]?.status?.presence,
                            StoatAPI.userCache[StoatAPI.selfId]?.online ?: false
                        ),
                        userId = StoatAPI.selfId ?: "",
                        avatar = StoatAPI.userCache[StoatAPI.selfId]?.avatar,
                        size = 48.dp,
                        presenceSize = 16.dp,
                        onClick = {
                            onDestinationChanged(ChatRouterDestination.defaultForDMList)
                        },
                        onLongClick = onLongPressAvatar,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(48.dp)
                    )
                }
            }

            items(
                DirectMessages.unreadDMs().size,
                key = { DirectMessages.unreadDMs()[it].id ?: it }
            ) {
                val dm = DirectMessages.unreadDMs()[it]
                when (dm.channelType) {
                    ChannelType.Group -> GroupIcon(
                        name = dm.name ?: "?",
                        size = 48.dp,
                        onClick = {
                            dm.id?.let { id ->
                                onDestinationChanged(ChatRouterDestination.Channel(id))
                            }
                        },
                        icon = dm.icon,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(48.dp)
                    )

                    else -> {
                        val partner =
                            if (dm.channelType == ChannelType.DirectMessage) {
                                StoatAPI.userCache[
                                    ChannelUtils.resolveDMPartner(
                                        dm
                                    )
                                ]
                            } else {
                                null
                            }

                        UserAvatar(
                            username = partner?.let { p ->
                                User.resolveDefaultName(
                                    p
                                )
                            } ?: dm.name ?: "?",
                            presence = presenceFromStatus(
                                partner?.status?.presence,
                                partner?.online ?: false
                            ),
                            userId = partner?.id ?: dm.id ?: "",
                            avatar = partner?.avatar ?: dm.icon,
                            size = 48.dp,
                            presenceSize = 16.dp,
                            onClick = {
                                dm.id?.let { id ->
                                    onDestinationChanged(ChatRouterDestination.Channel(id))
                                }
                            },
                            modifier = Modifier
                                .padding(8.dp)
                                .size(48.dp)
                        )
                    }
                }
            }

            item(key = "divider") {
                HorizontalDivider(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }

            items(
                serverList.size,
                key = { serverList[it].id ?: it }
            ) {
                val serverInList = serverList[it]
                val serverHasUnread =
                    serverInList.id?.let { srvId -> StoatAPI.unreads.serverHasUnread(srvId) }
                        ?: false
                val voiceParticipants = serverInList.channels.orEmpty().flatMap { channelId ->
                    StoatAPI.voiceStateCache[channelId]?.participants.orEmpty()
                }
                val hasScreenShare = voiceParticipants.any { it.screensharing }
                val voiceBadgeIcon = when {
                    hasScreenShare -> R.drawable.ic_screen_share_24dp
                    voiceParticipants.isNotEmpty() -> R.drawable.ic_volume_up_24dp
                    else -> null
                }
                val leftIndicatorHeight = animateDpAsState(
                    targetValue = if (serverInList.id == currentServer) 32.dp
                    else if (serverHasUnread) 8.dp
                    else 0.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ), label = "Left indicator width"
                )
                val leftIndicatorColour = animateColorAsState(
                    targetValue =
                        if (serverInList.id == currentServer)
                            MaterialTheme.colorScheme.primary
                        else if (serverHasUnread)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            Color.Transparent,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "Left indicator colour"
                )

                Box(Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .padding(8.dp)
                            .size(48.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        val icon = serverInList.icon?.id?.let { iconId ->
                            "$STOAT_FILES/icons/$iconId"
                        }
                        val iconModifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .then(
                                if (voiceBadgeIcon != null) {
                                    Modifier.bottomEndCircleCutout(ServerVoiceBadgeSize)
                                } else {
                                    Modifier
                                }
                            )
                            .clickable {
                                serverInList.id?.let { srvId -> navigateToServer(srvId) }
                                scope.launch {
                                    drawerState?.close()
                                }
                            }
                        if (icon != null) {
                            RemoteImage(
                                url = icon,
                                allowAnimation = false,
                                modifier = iconModifier,
                                description = serverInList.name ?: stringResource(R.string.unknown)
                            )
                        } else {
                            IconPlaceholder(
                                name = serverInList.name ?: stringResource(R.string.unknown),
                                modifier = iconModifier
                            )
                        }

                        if (voiceBadgeIcon != null) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(ServerVoiceBadgeSize)
                            ) {
                                Icon(
                                    painter = painterResource(voiceBadgeIcon),
                                    contentDescription = stringResource(
                                        if (hasScreenShare) {
                                            R.string.voice_screen_sharing
                                        } else {
                                            R.string.voice_notification_ongoing_call
                                        }
                                    ),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(ServerVoiceBadgeIconSize)
                                )
                            }
                        }
                    }

                    Box(
                        Modifier
                            .height(leftIndicatorHeight.value)
                            .width(8.dp)
                            .offset(x = (-4).dp)
                            .clip(CircleShape)
                            .background(leftIndicatorColour.value)
                            .align(Alignment.CenterStart)
                    )
                }
            }

            item(key = "add_server") {
                Box(
                    Modifier
                        .padding(8.dp)
                        .clip(CircleShape)
                        .clickable {
                            onShowAddServerSheet()
                        }
                        .size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_24dp),
                        contentDescription = stringResource(R.string.server_plus_alt)
                    )
                }
            }

            item(key = "discover") {
                Box(
                    Modifier
                        .padding(8.dp)
                        .clip(CircleShape)
                        .clickable {
                            topNav.navigate("discover")
                        }
                        .size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_explore_24dp),
                        contentDescription = stringResource(R.string.discover_alt)
                    )
                }
            }

            if (showSettingsIcon) {
                item(key = "settings") {
                    Box(
                        Modifier
                            .padding(8.dp)
                            .clip(CircleShape)
                            .clickable {
                                onOpenSettings()
                                scope.launch {
                                    drawerState?.close()
                                }
                            }
                            .size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings_24dp),
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                }
            }
        }
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .weight(1f)
                .fillMaxHeight()
        ) {
            Box(
                Modifier
                    .clip(
                        MaterialTheme.shapes.medium.copy(
                            topStart = CornerSize(0.dp),
                            topEnd = CornerSize(0.dp)
                        )
                    )
                    .height(
                        serverBannerHeight + WindowInsets.statusBars.asPaddingValues()
                            .calculateTopPadding()
                    )
                //.offset(y = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            ) {
                if (server?.banner != null) {
                    RemoteImage(
                        url = "$STOAT_FILES/banners/${server.banner!!.id}",
                        description = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                    )

                    with(MaterialTheme.colorScheme) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .drawBehind {
                                    drawRect(
                                        Brush.linearGradient(
                                            listOf(
                                                Color.Black.copy(alpha = 0.6f),
                                                Color.Transparent
                                            ),
                                            Offset.Zero,
                                            Offset.Infinite.copy(x = 0f)
                                        ),
                                    )
                                })
                    }
                }

                Row(
                    Modifier
                        .padding(16.dp)
                        .offset(y = serverInfoOffset),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides
                                if (server?.banner != null) Color.White
                                else LocalContentColor.current
                    ) {
                        Row(
                            Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (server?.flags has ServerFlags.Official) {
                                Icon(
                                    painter = painterResource(
                                        id = R.drawable.ic_workspace_premium_24dp__fill
                                    ),
                                    contentDescription = stringResource(
                                        R.string.server_flag_official
                                    ),
                                    tint = LocalContentColor.current,
                                    modifier = Modifier
                                        .size(24.dp)
                                )
                            }
                            if (server?.flags has ServerFlags.Verified) {
                                Icon(
                                    painter = painterResource(
                                        id = R.drawable.ic_verified_24dp__fill
                                    ),
                                    contentDescription = stringResource(
                                        R.string.server_flag_verified
                                    ),
                                    tint = LocalContentColor.current,
                                    modifier = Modifier
                                        .size(24.dp)
                                )
                            }

                            Text(
                                text = when (currentServer) {
                                    null -> stringResource(R.string.direct_messages)
                                    else -> server?.name ?: stringResource(R.string.unknown)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (currentServer != null) {
                            IconButton(onClick = {
                                server?.id?.let { srvId -> onShowServerContextSheet(srvId) }
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_more_vert_24dp),
                                    contentDescription = stringResource(R.string.menu),
                                    tint = LocalContentColor.current
                                )
                            }
                        } else {
                            Spacer(Modifier.height(64.dp))
                        }
                    }
                }
            }

            if (currentServer == null) {
                DirectMessagesChannelListRenderer(
                    currentDestination,
                    onDestinationChanged,
                    drawerState,
                    channelListState,
                    onOpenChannelContextSheet = { channelContextSheetTarget = it }
                )
            } else {
                ServerChannelListRenderer(
                    categorisedChannels,
                    currentDestination,
                    onDestinationChanged,
                    drawerState,
                    channelListState,
                    onOpenChannelContextSheet = { channelContextSheetTarget = it },
                    serverId = currentServer
                )
            }
        }
    }
}

@Composable
fun ColumnScope.DirectMessagesChannelListRenderer(
    currentDestination: ChatRouterDestination,
    onDestinationChanged: (ChatRouterDestination) -> Unit,
    drawerState: DrawerState?,
    channelListState: LazyListState,
    onOpenChannelContextSheet: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val dmAbleChannels =
        StoatAPI.channelCache.values
            .filter { it.channelType == ChannelType.DirectMessage || it.channelType == ChannelType.Group }
            .filter { if (it.channelType == ChannelType.DirectMessage) it.active == true else true }
            .sortedBy { it.lastMessageID ?: it.id }
            .reversed()

    LazyColumn(
        state = channelListState,
        modifier = Modifier
            .fillMaxSize()
            .weight(1f)
    ) {
        item(key = "overview") {
            ChannelItem(
                channel = Channel(
                    id = "overview",
                    name = stringResource(R.string.overview_screen_title),
                    channelType = ChannelType.TextChannel
                ),
                iconType = ChannelItemIconType.Painter(painterResource(R.drawable.ic_star_shine_24dp)),
                isCurrent = currentDestination is ChatRouterDestination.Overview,
                onDestinationChanged = {
                    onDestinationChanged(ChatRouterDestination.Overview)
                    scope.launch {
                        drawerState?.close()
                    }
                },
                hasUnread = false,
                onOpenChannelContextSheet = {}
            )
            Spacer(Modifier.height(4.dp))
        }

        item(key = "friends") {
            ChannelItem(
                channel = Channel(
                    id = "friends",
                    name = stringResource(R.string.friends),
                    channelType = ChannelType.TextChannel
                ),
                iconType = ChannelItemIconType.Painter(painterResource(R.drawable.ic_group_24dp)),
                isCurrent = currentDestination is ChatRouterDestination.Friends,
                onDestinationChanged = {
                    onDestinationChanged(ChatRouterDestination.Friends)
                    scope.launch {
                        drawerState?.close()
                    }
                },
                hasUnread = FriendRequests.getIncoming().isNotEmpty(),
                onOpenChannelContextSheet = {},
            )
            Spacer(Modifier.height(4.dp))
        }

        item(key = "saved_messages") {
            val notesChannel =
                StoatAPI.channelCache.values.firstOrNull { it.channelType == ChannelType.SavedMessages }

            if (notesChannel != null) {
                ChannelItem(
                    channel = Channel(
                        id = notesChannel.id,
                        name = stringResource(R.string.channel_notes),
                        channelType = ChannelType.SavedMessages
                    ),
                    isCurrent = currentDestination is ChatRouterDestination.Channel &&
                            currentDestination.channelId == notesChannel.id,
                    onDestinationChanged = {
                        onDestinationChanged(it)
                        scope.launch {
                            drawerState?.close()
                        }
                    },
                    hasUnread = false,
                    onOpenChannelContextSheet = {},
                )
                Spacer(Modifier.height(4.dp))
            }
        }

        item("divider") {
            HorizontalDivider(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(4.dp))
        }

        items(
            dmAbleChannels.size,
            key = { dmAbleChannels[it].id ?: it }
        ) {
            val channel = dmAbleChannels.getOrNull(it) ?: return@items

            val partner =
                if (channel.channelType == ChannelType.DirectMessage) {
                    StoatAPI.userCache[
                        ChannelUtils.resolveDMPartner(
                            channel
                        )
                    ]
                } else {
                    null
                }

            DMOrGroupItem(
                channel = channel,
                partner = partner,
                isCurrent = when (currentDestination) {
                    is ChatRouterDestination.Channel -> {
                        currentDestination.channelId == channel.id
                    }

                    else -> false
                },
                hasUnread = channel.lastMessageID?.let { lastMessageID ->
                    StoatAPI.unreads.hasUnread(
                        channel.id!!,
                        lastMessageID,
                        serverId = null
                    )
                } ?: false,
                isMuted = NotificationSettingsProvider.isChannelMuted(channel.id!!, null),
                onDestinationChanged = { dest ->
                    onDestinationChanged(dest)
                    scope.launch {
                        drawerState?.close()
                    }
                },
                onOpenChannelContextSheet = onOpenChannelContextSheet
            )
        }

        item(key = "last") {
            Spacer(
                Modifier.height(
                    WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding()
                )
            )
        }
    }
}

@Composable
fun ColumnScope.ServerChannelListRenderer(
    categorisedChannels: List<CategorisedChannelList>?,
    currentDestination: ChatRouterDestination,
    onDestinationChanged: (ChatRouterDestination) -> Unit,
    drawerState: DrawerState?,
    channelListState: LazyListState,
    onOpenChannelContextSheet: (String) -> Unit,
    serverId: String
) {
    val scope = rememberCoroutineScope()

    LazyColumn(
        state = channelListState,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(top = 8.dp),
        modifier = Modifier
            .fillMaxSize()
            .weight(1f)
    ) {
        if (categorisedChannels.isNullOrEmpty()) {
            item {
                Column(
                    Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_channels_heading),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = stringResource(R.string.no_channels_body),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        items(categorisedChannels?.size ?: 0) {
            when (val channelOrCat = categorisedChannels?.get(it)) {
                is CategorisedChannelList.Channel -> {
                    ChannelItem(
                        channel = channelOrCat.channel,
                        isCurrent = when (currentDestination) {
                            is ChatRouterDestination.Channel -> {
                                currentDestination.channelId == channelOrCat.channel.id
                            }

                            else -> false
                        },
                        onDestinationChanged = {
                            onDestinationChanged(it)
                            scope.launch {
                                drawerState?.close()
                            }
                        },
                        hasUnread = channelOrCat.channel.lastMessageID?.let { lastMessageID ->
                            StoatAPI.unreads.hasUnread(
                                channelOrCat.channel.id!!,
                                lastMessageID,
                                serverId
                            )
                        } ?: false,
                        isMuted = NotificationSettingsProvider.isChannelMuted(
                            channelOrCat.channel.id!!,
                            serverId
                        ),
                        showVoiceParticipants = true,
                        onOpenChannelContextSheet = onOpenChannelContextSheet
                    )
                }

                is CategorisedChannelList.Category -> {
                    CategoryItem(category = channelOrCat.category)
                }

                else -> {}
            }
        }
        item(key = "last") {
            Spacer(
                Modifier.height(
                    WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding()
                )
            )
        }
    }
}

sealed class ChannelItemIconType {
    data class Channel(val type: ChannelType) : ChannelItemIconType()
    data class Painter(val painter: androidx.compose.ui.graphics.painter.Painter) :
        ChannelItemIconType()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelItem(
    channel: Channel,
    isCurrent: Boolean,
    iconType: ChannelItemIconType = ChannelItemIconType.Channel(
        channel.channelType ?: ChannelType.TextChannel
    ),
    hasUnread: Boolean = false,
    isMuted: Boolean = false,
    appendServerName: Boolean = false,
    showVoiceParticipants: Boolean = false,
    onDestinationChanged: (ChatRouterDestination) -> Unit,
    onOpenChannelContextSheet: (String) -> Unit
) {
    CompositionLocalProvider(
        LocalContentColor provides if (isCurrent) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            if (hasUnread) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        }
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp)
                    .clip(
                        CircleShape
                    )
                    .combinedClickable(
                        onLongClickLabel = stringResource(R.string.channel_context_sheet_open),
                        onLongClick = {
                            channel.id?.let { chId ->
                                onOpenChannelContextSheet(chId)
                            }
                        },
                        onClick = {
                            channel.id?.let { chId ->
                                onDestinationChanged(ChatRouterDestination.Channel(chId))
                            }
                        }
                    )
                    .then(
                        if (isCurrent) {
                            Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                        } else {
                            Modifier
                        }
                    )
                    .then(
                        if (isMuted) {
                            Modifier.alpha(0.5f)
                        } else {
                            Modifier
                        }
                    )
                    .padding(16.dp)
                    .fillMaxWidth()) {
                when (iconType) {
                    is ChannelItemIconType.Channel -> {
                        when {
                            GeoStateProvider.geoState?.isAgeRestrictedGeo == true &&
                                    channel.nsfw == true -> {
                                Icon(
                                    painter = painterResource(R.drawable.ic_grid_3x3_off_24dp),
                                    contentDescription = stringResource(R.string.geogate_channel_icon_alt),
                                )
                            }

                            channel.channelType == ChannelType.TextChannel && channel.voice != null -> {
                                ChannelIcon(channel = channel)
                            }

                            else -> ChannelIcon(iconType.type)
                        }
                    }

                    is ChannelItemIconType.Painter -> {
                        Icon(painter = iconType.painter, contentDescription = null)
                    }
                }
                Text(
                    text = (ChannelUtils.resolveName(channel) ?: stringResource(R.string.unknown))
                            + if (appendServerName && channel.server != null) {
                        " (${StoatAPI.serverCache[channel.server]?.name ?: stringResource(R.string.unknown)})"
                    } else {
                        ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (hasUnread && !isCurrent) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .requiredSize(8.dp)
                    )
                }
                channel.voice?.maxUsers?.let { maxUsers ->
                    val participantCount = channel.id
                        ?.let { StoatAPI.voiceStateCache[it]?.participants?.size }
                        ?: 0
                    Text(
                        text = "$participantCount/$maxUsers",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FragmentMono
                        ),
                        color = LocalContentColor.current.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }

            if (showVoiceParticipants &&
                channel.channelType == ChannelType.TextChannel &&
                channel.voice != null
            ) {
                VoiceChannelParticipantPreview(
                    channel = channel,
                    modifier = if (isMuted) Modifier.alpha(0.5f) else Modifier
                )
            }
        }
    }
}

private const val MAX_VISIBLE_VOICE_PARTICIPANTS = 5

@Composable
private fun VoiceChannelParticipantPreview(
    channel: Channel,
    modifier: Modifier = Modifier,
) {
    val channelId = channel.id ?: return
    val participants = StoatAPI.voiceStateCache[channelId]?.participants.orEmpty()
    val participantIds = participants.map { it.id }.distinct()

    LaunchedEffect(participantIds) {
        supervisorScope {
            participantIds
                .filter { StoatAPI.userCache[it] == null }
                .forEach { userId ->
                    launch {
                        try {
                            addUserIfUnknown(userId)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            logcat(LogPriority.ERROR) {
                                "Failed to fetch voice participant $userId\n" +
                                        e.asLog()
                            }
                        }
                    }
                }
        }
    }

    AnimatedVisibility(
        visible = participants.isNotEmpty(),
        modifier = modifier
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 56.dp, top = 4.dp, end = 24.dp, bottom = 8.dp)
        ) {
            participants.take(MAX_VISIBLE_VOICE_PARTICIPANTS).forEach { participant ->
                VoiceChannelParticipantRow(
                    state = participant,
                    channel = channel
                )
            }

            val hiddenParticipantCount =
                (participants.size - MAX_VISIBLE_VOICE_PARTICIPANTS).coerceAtLeast(0)
            if (hiddenParticipantCount > 0) {
                Text(
                    text = stringResource(
                        R.string.channel_voice_participants_more,
                        hiddenParticipantCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 28.dp)
                )
            }
        }
    }
}

@Composable
private fun VoiceChannelParticipantRow(
    state: UserVoiceState,
    channel: Channel,
) {
    val user = StoatAPI.userCache[state.id]
    val displayName = channel.server
        ?.let { StoatAPI.members.getMember(it, state.id)?.nickname }
        ?: user?.let(User::resolveDefaultName)
        ?: stringResource(R.string.unknown)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        UserAvatar(
            username = displayName,
            userId = state.id,
            avatar = user?.avatar,
            size = 20.dp
        )
        Text(
            text = displayName,
            style = MaterialTheme.typography.bodySmall,
            color = LocalContentColor.current.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (state.screensharing) {
            Icon(
                painter = painterResource(R.drawable.ic_screen_share_24dp),
                contentDescription = stringResource(R.string.voice_screen_sharing),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun CategoryItem(
    category: Category
) {
    Text(
        text = category.title ?: stringResource(R.string.unknown),
        style = MaterialTheme.typography.labelLarge,
        fontSize = 16.sp,
        modifier = Modifier.padding(
            start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DMOrGroupItem(
    channel: Channel,
    partner: User?,
    isCurrent: Boolean,
    hasUnread: Boolean,
    isMuted: Boolean = false,
    onDestinationChanged: (ChatRouterDestination) -> Unit,
    onOpenChannelContextSheet: (String) -> Unit
) {
    val currentIndicatorOpacity = animateFloatAsState(
        targetValue = if (isCurrent) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "Current indicator opacity"
    )
    val currentIndicatorSize = animateDpAsState(
        targetValue = if (isCurrent) 24.dp else 0.dp,
        animationSpec = tween(durationMillis = 150),
        label = "Current indicator size"
    )

    Row(
        Modifier
            .combinedClickable(
                onLongClickLabel = stringResource(R.string.channel_context_sheet_open),
                onLongClick = {
                    channel.id?.let { chId ->
                        onOpenChannelContextSheet(chId)
                    }
                },
                onClick = {
                    channel.id?.let { chId ->
                        onDestinationChanged(ChatRouterDestination.Channel(chId))
                    }
                }
            )
            .padding(vertical = 16.dp)
            .fillMaxWidth()
            .clipToBounds()
            .then(
                if (isMuted) {
                    Modifier.alpha(0.5f)
                } else {
                    Modifier
                }
            )
    ) {
        Box(
            Modifier
                .offset(x = (-4).dp)
                .clip(
                    CircleShape
                        .copy(
                            topStart = CornerSize(0),
                            bottomStart = CornerSize(0)
                        )
                )
                .background(MaterialTheme.colorScheme.primary)
                .height(currentIndicatorSize.value)
                .width(8.dp)
                .alpha(currentIndicatorOpacity.value)
                .align(Alignment.CenterVertically)
        )

        Row(
            Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (channel.channelType) {
                ChannelType.Group -> GroupIcon(
                    name = channel.name ?: stringResource(R.string.unknown),
                    size = 28.dp,
                    icon = channel.icon
                )

                else -> UserAvatar(
                    username = partner?.let { User.resolveDefaultName(it) } ?: channel.name
                    ?: stringResource(R.string.unknown),
                    presence = presenceFromStatus(
                        partner?.status?.presence,
                        partner?.online ?: false
                    ),
                    userId = partner?.id ?: channel.id ?: "",
                    avatar = partner?.avatar ?: channel.icon,
                    size = 28.dp,
                    presenceSize = 12.dp
                )
            }

            Column(Modifier.weight(1f)) {
                Text(
                    text = partner?.let { User.resolveDefaultName(it) } ?: channel.name
                    ?: stringResource(R.string.unknown),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (hasUnread && !isCurrent) {
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .requiredSize(8.dp)
                )
            }
        }
    }
}
