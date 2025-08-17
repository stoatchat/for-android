package chat.peptide.composables.screens.chat.drawer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.peptide.R
import chat.peptide.api.PeptideAPI
import chat.peptide.api.internals.CategorisedChannelList
import chat.peptide.api.internals.ChannelUtils
import chat.peptide.api.internals.DirectMessages
import chat.peptide.api.schemas.Category
import chat.peptide.api.schemas.Channel
import chat.peptide.api.schemas.ChannelType
import chat.peptide.api.schemas.ServerFlags
import chat.peptide.api.schemas.User
import chat.peptide.api.schemas.has
import chat.peptide.api.settings.GeoStateProvider
import chat.peptide.api.settings.NotificationSettingsProvider
import chat.peptide.api.settings.SyncedSettings
import chat.peptide.composables.generic.GroupIcon
import chat.peptide.composables.generic.IconPlaceholder
import chat.peptide.composables.generic.RemoteImage
import chat.peptide.composables.generic.SquareButton
import chat.peptide.composables.generic.UserAvatar
import chat.peptide.composables.generic.presenceFromStatus
import chat.peptide.composables.screens.chat.ChannelIcon
import chat.peptide.composables.screens.chat.discover.DiscoverServersList
import chat.peptide.screens.chat.ChatRouterDestination
import chat.peptide.sheets.ChannelContextSheet

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun ChannelSideDrawer(
    currentServer: String?,
    currentDestination: ChatRouterDestination,
    onDestinationChanged: (ChatRouterDestination) -> Unit,
    navigateToServer: (String) -> Unit,
    onShowServerContextSheet: (String) -> Unit,
    showSettingsIcon: Boolean,
    onOpenSettings: () -> Unit,
    onShowAddServerSheet: () -> Unit,
    modifier: Modifier = Modifier
) {

    val server = PeptideAPI.serverCache[currentServer]
    val categorisedChannels = server?.let {
        ChannelUtils.categoriseServerFlat(it)
    }
    val channelListState = rememberLazyListState()

    LaunchedEffect(currentDestination) {
        if ((currentDestination is ChatRouterDestination.ServersChannels) && currentServer != null) {
            val channelIndex = categorisedChannels?.indexOfFirst {
                when (it) {
                    is CategorisedChannelList.Channel -> it.channel.id == currentDestination.serverID
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

    // - Take the list of servers and filter them by the ones that are in the ordering.
    // - Sort the servers that are in the ordering using the ordering.
    // - Add the servers that aren't in the ordering to the end of the list.
    // - Sort the servers that aren't in the ordering by their ID (creation order).
    val serverList = ((PeptideAPI.serverCache.values.filter {
        SyncedSettings.ordering.servers.contains(
            it.id
        )
    }
        .sortedBy { SyncedSettings.ordering.servers.indexOf(it.id) }) + (PeptideAPI.serverCache.values.filter {
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



    Box {
        Row(modifier.fillMaxSize()) {
            LazyColumn(
                Modifier.width(64.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                stickyHeader(key = "self") {
                    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
                        Box(
                            Modifier
                                .padding(8.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    onDestinationChanged(ChatRouterDestination.Home)
                                }
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_direct_messages),
                                contentDescription = stringResource(R.string.discover_alt),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                items(
                    count = DirectMessages.unreadDMs().size,
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
                                    PeptideAPI.userCache[
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


                item(key = "discover") {
                    Box(
                        Modifier
                            .padding(8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                onDestinationChanged(
                                    ChatRouterDestination.Discover
                                )
                            }
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.icn_explore_24dp),
                            contentDescription = stringResource(R.string.discover_alt),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
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
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.icn_add_24dp),
                            contentDescription = stringResource(R.string.server_plus_alt),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                items(
                    serverList.size,
                    key = { serverList[it].id ?: it }
                ) {
                    val serverInList = serverList[it]
                    val serverHasUnread =
                        serverInList.id?.let { srvId -> PeptideAPI.unreads.serverHasUnread(srvId) }
                            ?: false
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
                                .clip(CircleShape)
                                .clickable {
                                    serverInList.id?.let { srvId -> navigateToServer(srvId) }
                                }) {
                            val icon = serverInList.icon?.id?.let { iconId ->
                                "${PeptideAPI.getCurrentFilesUrl()}/icons/$iconId"
                            }
                            if (icon != null) {
                                RemoteImage(
                                    url = icon,
                                    allowAnimation = false,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape),
                                    description = serverInList.name
                                        ?: stringResource(R.string.unknown)
                                )
                            } else {
                                IconPlaceholder(
                                    name = serverInList.name ?: stringResource(R.string.unknown),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                )
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

                if (showSettingsIcon) {
                    item(key = "settings") {
                        Box(
                            Modifier
                                .padding(8.dp)
                                .clip(CircleShape)
                                .clickable {
                                    onOpenSettings()
                                }
                                .size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.icn_settings_24dp),
                                contentDescription = stringResource(R.string.settings)
                            )
                        }
                    }
                }
            }
            Column(
                Modifier
                    .clip(shape = RoundedCornerShape(topStart = 24.dp))
                    .background(color = MaterialTheme.colorScheme.surfaceContainer)
                    .weight(weight = 1f)
                    .fillMaxHeight()
            ) {
                Box(
                    Modifier
                        .height(serverBannerHeight)
                ) {
                    if (server?.banner != null) {
                        RemoteImage(
                            url = "${PeptideAPI.getCurrentFilesUrl()}/banners/${server.banner.id}",
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
                            .padding(16.dp),
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
                                            id = R.drawable.ic_peptide_decagram_24dp
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
                                            id = R.drawable.ic_check_decagram_24dp
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
                                        null -> stringResource(if (currentDestination is ChatRouterDestination.Discover) R.string.discover_servers else R.string.direct_messages)
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
                                        painter = painterResource(R.drawable.icn_more_vert_24dp),
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
                if (currentDestination is ChatRouterDestination.Discover) {
                    DiscoverServersList(
                        onJoinToServerSuccess = navigateToServer
                    )
                } else {
                    if (currentServer == null) {
                        DirectMessagesChannelListRenderer(
                            currentDestination,
                            onDestinationChanged,
                            channelListState,
                            onOpenChannelContextSheet = { channelContextSheetTarget = it }
                        )
                    } else {
                        ServerChannelListRenderer(
                            categorisedChannels,
                            currentDestination,
                            onDestinationChanged,
                            channelListState,
                            onOpenChannelContextSheet = { channelContextSheetTarget = it },
                            serverId = currentServer
                        )
                    }
                }
            }
        }
        if (currentDestination is ChatRouterDestination.Home) {
            FloatingActionButton(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomEnd),
                onClick = {
                    onDestinationChanged(ChatRouterDestination.Friends)
                },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                shape = CircleShape
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_new_message),
                    contentDescription = stringResource(R.string.message_friends),
                    tint = MaterialTheme.colorScheme.onSecondary
                )
            }
        }
    }
}

@Preview
@Composable
private fun DirectMessagesChannelListRendererPreview() {
    val mockChannels = listOf(
        Channel(id = "1", name = "Saved Messages", channelType = ChannelType.SavedMessages),
        Channel(
            id = "2",
            name = "User One",
            channelType = ChannelType.DirectMessage,
            lastMessageID = "msg1"
        ),
        Channel(
            id = "3",
            name = "Group Chat Alpha",
            channelType = ChannelType.Group,
            icon = null,
            lastMessageID = "msg2"
        ),
        Channel(
            id = "4",
            name = "User Two",
            channelType = ChannelType.DirectMessage,
            active = false
        ), // inactive DM
        Channel(
            id = "5",
            name = "Another Group",
            channelType = ChannelType.Group,
            icon = null,
            lastMessageID = "msg3"
        )
    )
    val mockUsers = mapOf(
        "user1_id" to User(id = "user1_id", username = "User One", avatar = null, online = true),
        "user2_id" to User(id = "user2_id", username = "User Two", avatar = null, online = false)
    )

    // Simulate PeptideAPI cache for preview
    PeptideAPI.channelCache.clear()
    mockChannels.forEach { PeptideAPI.channelCache[it.id!!] = it }
    PeptideAPI.userCache.clear()
    mockUsers.forEach { (id, user) -> PeptideAPI.userCache[id] = user }


    //ColumnScope is required for DirectMessagesChannelListRenderer, let's provide a dummy one
    Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
        DirectMessagesChannelListRenderer(
            currentDestination = ChatRouterDestination.Home,
            onDestinationChanged = {},
            channelListState = rememberLazyListState(),
            onOpenChannelContextSheet = {}
        )
    }
}

@Preview
@Composable
private fun DirectMessagesChannelListRendererEmptyPreview() {
    // Simulate PeptideAPI cache for preview - empty state
    PeptideAPI.channelCache.clear()
    PeptideAPI.channelCache["saved_notes_channel_id"] = Channel(
        id = "saved_notes_channel_id",
        name = "Saved Messages",
        channelType = ChannelType.SavedMessages
    )
    PeptideAPI.userCache.clear()

    //ColumnScope is required for DirectMessagesChannelListRenderer, let's provide a dummy one
    Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
        DirectMessagesChannelListRenderer(
            currentDestination = ChatRouterDestination.Home,
            onDestinationChanged = {},
            channelListState = rememberLazyListState(),
            onOpenChannelContextSheet = {}
        )
    }
}

@Composable
private fun ColumnScope.DirectMessagesChannelListRenderer(
    currentDestination: ChatRouterDestination,
    onDestinationChanged: (ChatRouterDestination) -> Unit,
    channelListState: LazyListState,
    onOpenChannelContextSheet: (String) -> Unit,
) {
    val dmAbleChannels =
        PeptideAPI.channelCache.values
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
        item(key = "saved_messages") {
            val notesChannel = PeptideAPI.channelCache.values.firstOrNull {
                it.channelType == ChannelType.SavedMessages
            }

            if (notesChannel != null) {
                HorizontalDivider(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Column(Modifier.fillMaxWidth()) {
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
                        },
                        hasUnread = false,
                        onOpenChannelContextSheet = {},
                    )
                    HorizontalDivider(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }

        if (dmAbleChannels.isEmpty()) {
            item(key = "empty-list-state") {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        modifier = Modifier.size(100.dp),
                        painter = painterResource(R.drawable.empty_direct_messages_img),
                        contentDescription = stringResource(R.string.empty_messages_content_description)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.empty_messages_heading),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        fontSize = 24.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.empty_messages_body),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    SquareButton(
                        onClick = {
                            onDestinationChanged(ChatRouterDestination.Friends)
                        },
                    ) {
                        Text(text = stringResource(R.string.new_conversation))
                    }
                }
            }
        } else {
            items(
                dmAbleChannels.size,
                key = { dmAbleChannels[it].id ?: it }
            ) {
                val channel = dmAbleChannels.getOrNull(it) ?: return@items

                val partner =
                    if (channel.channelType == ChannelType.DirectMessage) {
                        PeptideAPI.userCache[
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
                        PeptideAPI.unreads.hasUnread(
                            channel.id!!,
                            lastMessageID,
                            serverId = null
                        )
                    } ?: false,
                    isMuted = NotificationSettingsProvider.isChannelMuted(channel.id!!, null),
                    onDestinationChanged = { dest ->
                        onDestinationChanged(dest)
                    },
                    onOpenChannelContextSheet = onOpenChannelContextSheet
                )
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


@Composable
private fun ColumnScope.ServerChannelListRenderer(
    categorisedChannels: List<CategorisedChannelList>?,
    currentDestination: ChatRouterDestination,
    onDestinationChanged: (ChatRouterDestination) -> Unit,
    channelListState: LazyListState,
    onOpenChannelContextSheet: (String) -> Unit,
    serverId: String
) {
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
                        onDestinationChanged = { dest ->
                            onDestinationChanged(dest)
                        },
                        hasUnread = channelOrCat.channel.lastMessageID?.let { lastMessageID ->
                            PeptideAPI.unreads.hasUnread(
                                channelOrCat.channel.id!!,
                                lastMessageID,
                                serverId
                            )
                        } ?: false,
                        isMuted = NotificationSettingsProvider.isChannelMuted(
                            channelOrCat.channel.id!!,
                            serverId
                        ),
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
                                painter = painterResource(R.drawable.icn_grid_3x3_off_24dp),
                                contentDescription = stringResource(R.string.geogate_channel_icon_alt),
                            )
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
                    " (${PeptideAPI.serverCache[channel.server]?.name ?: stringResource(R.string.unknown)})"
                } else {
                    ""
                },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (hasUnread && !isCurrent) {
                Spacer(Modifier.weight(1f))
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