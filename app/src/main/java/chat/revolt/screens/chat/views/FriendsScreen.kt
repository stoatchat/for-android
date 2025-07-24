package chat.revolt.screens.chat.views

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.api.internals.FriendRequests
import chat.revolt.api.internals.UserQR
import chat.revolt.api.internals.UserQRContents
import chat.revolt.api.routes.user.friendUser
import chat.revolt.api.routes.user.unfriendUser
import chat.revolt.api.schemas.AutumnResource
import chat.revolt.api.schemas.Metadata
import chat.revolt.api.settings.LoadedSettings
import chat.revolt.callbacks.Action
import chat.revolt.callbacks.ActionChannel
import chat.revolt.components.vectorassets.HL_TAG
import chat.revolt.components.vectorassets.HL_USERNAME
import chat.revolt.components.vectorassets.RevoltTagIntro
import chat.revolt.composables.chat.MemberListItem
import chat.revolt.composables.generic.CountableListHeader
import chat.revolt.composables.generic.UserAvatar
import chat.revolt.internals.extensions.zero
import chat.revolt.markdown.jbm.asHexString
import chat.revolt.screens.chat.LocalIsConnected
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private fun showInvalidClipboardToast(context: Context) {
    Toast.makeText(
        context,
        context.getString(R.string.friends_add_by_tag_sheet_invalid_clipboard),
        Toast.LENGTH_SHORT
    ).show()
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun FriendsScreen(topNav: NavController, useDrawer: Boolean, onDrawerClicked: () -> Unit) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    var overflowMenuShown by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val fabVisible by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var addByTagSheetVisible by rememberSaveable { mutableStateOf(false) }
    var qrResult by rememberSaveable { mutableStateOf<QRResult?>(null) }

    val scanQrCodeLauncher = rememberLauncherForActivityResult(ScanQRCode()) { result ->
        qrResult = result
    }

    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

    if (addByTagSheetVisible) {
        val addByTagSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var username by rememberSaveable { mutableStateOf("") }
        var tag by rememberSaveable { mutableStateOf("") }
        var error by rememberSaveable { mutableStateOf<String?>(null) }

        ModalBottomSheet(
            onDismissRequest = {
                addByTagSheetVisible = false
            },
            sheetGesturesEnabled = false,
            dragHandle = {},
            sheetState = addByTagSheetState,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    imageVector = RevoltTagIntro,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                )
                Text(
                    text = stringResource(R.string.friends_add_by_tag),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = AnnotatedString.fromHtml(
                        stringResource(
                            R.string.friends_add_by_tag_sheet_description,
                            "<font color=\"${Color(HL_USERNAME).asHexString(false)}\">",
                            "<font color=\"${Color(HL_TAG).asHexString(false)}\">",
                            "</font>",
                        )
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    TextField(
                        value = username,
                        onValueChange = {
                            // Cf. https://github.com/revoltchat/backend/blob/aab1734615ac3e09cd447d2c2862ae0f33f5ce5f/crates/delta/src/routes/onboard/complete.rs#L16
                            if (it.length <= 32 && it.all { char ->
                                    char.isLetterOrDigit() ||
                                            char == '_' || char == '.' || char == '-'
                                }) {
                                username = it
                            }
                        },
                        label = {
                            Text(stringResource(R.string.friends_add_by_tag_sheet_username))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        supportingText = {
                            Text(
                                text = error ?: "",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        isError = error != null,
                        modifier = Modifier.weight(1f),
                    )
                    Text("#", modifier = Modifier.padding(bottom = 36.dp))
                    TextField(
                        value = tag,
                        onValueChange = {
                            // up to 4 characters, only numbers
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                tag = it
                            }
                        },
                        label = {
                            Text(stringResource(R.string.friends_add_by_tag_sheet_tag))
                        },
                        supportingText = {
                            Text("") // Layout hack
                        },
                        modifier = Modifier.width(80.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        8.dp,
                        Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val clipboardText =
                                    clipboard.getClipEntry()?.clipData?.getItemAt(0)?.coerceToText(
                                        context
                                    ) ?: ""
                                try {
                                    if (clipboardText.isEmpty()) {
                                        showInvalidClipboardToast(context)
                                        return@launch
                                    }

                                    val parts = clipboardText.toString().replace(" ", "").split("#")
                                    if (parts.size == 2) {
                                        if (parts[0].length > 32 || parts[0].any { char ->
                                                !char.isLetterOrDigit() && char != '_' && char != '.' && char != '-'
                                            }) {
                                            showInvalidClipboardToast(context)
                                            return@launch
                                        }
                                        if (parts[1].length > 4 || parts[1].any { char -> !char.isDigit() }) {
                                            showInvalidClipboardToast(context)
                                            return@launch
                                        }

                                        username = parts[0].trim()
                                        tag = parts[1].trim()
                                    } else {
                                        showInvalidClipboardToast(context)
                                    }
                                } catch (_: Exception) {
                                    showInvalidClipboardToast(context)
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.friends_add_by_tag_sheet_paste_from_clipboard))
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                if (username.isNotBlank() && tag.isNotBlank()) {
                                    try {
                                        error = null
                                        friendUser("$username#$tag")
                                        addByTagSheetVisible = false
                                    } catch (e: Exception) {
                                        error = e.localizedMessage ?: e.toString()
                                    }
                                }
                            }
                        },
                        enabled = username.isNotBlank() && tag.isNotBlank()
                    ) {
                        Text(stringResource(R.string.friends_add_by_tag_sheet_add))
                    }
                }
            }
        }
    }

    if (qrResult != null) {
        val qrResultSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var contents by rememberSaveable { mutableStateOf<UserQRContents?>(null) }

        LaunchedEffect(qrResult) {
            when (qrResult) {
                is QRResult.QRUserCanceled -> {
                    // Silently dismiss ourselves.
                    qrResult = null
                }

                is QRResult.QRSuccess -> {
                    contents =
                        UserQR.fromUri((qrResult as QRResult.QRSuccess).content.rawValue ?: "")
                }

                else -> {
                    contents = null
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = {
                qrResult = null
                contents = null
            },
            sheetGesturesEnabled = false,
            dragHandle = {},
            sheetState = qrResultSheetState,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when {
                    qrResult is QRResult.QRMissingPermission -> {
                        Text(
                            text = stringResource(R.string.friends_scan_qr_result_sheet_missing_permission_title),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = stringResource(R.string.friends_scan_qr_result_sheet_missing_permission_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalContentColor.current.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                qrResult = null
                                contents = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.ok))
                        }
                    }

                    qrResult is QRResult.QRSuccess && contents != null -> {
                        Text(
                            text = stringResource(R.string.friends_scan_qr_result_sheet_success_title),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        UserAvatar(
                            username = contents?.username ?: stringResource(R.string.unknown),
                            userId = contents?.id ?: "",
                            allowAnimation = false,
                            shape = RoundedCornerShape(LoadedSettings.avatarRadius),
                            size = 128.dp,
                            avatar = contents?.avatar?.let {
                                AutumnResource(
                                    tag = "avatars",
                                    id = it,
                                    metadata = Metadata(
                                        type = "image/png",
                                        width = 128,
                                        height = 128,
                                    )
                                )
                            }
                        )

                        Text(
                            text = buildAnnotatedString {
                                val displayNameSameAsUsername = contents?.displayName?.trim() ==
                                        contents?.username?.trim()

                                if (!displayNameSameAsUsername) {
                                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                                    append(contents?.displayName)
                                    pop()
                                    append("\n")
                                }
                                append("${contents?.username}")
                                pushStyle(SpanStyle(fontWeight = FontWeight.ExtraLight))
                                append("#${contents?.discriminator}")
                                pop()
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 21.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                scope.launch {
                                    contents?.let { userContents ->
                                        try {
                                            friendUser("${userContents.username}#${userContents.discriminator}")
                                            qrResult = null
                                            contents = null
                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                context,
                                                e.localizedMessage ?: e.toString(),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.friends_add_by_tag_sheet_add))
                        }
                    }

                    qrResult is QRResult.QRError || contents == null -> {
                        Text(
                            text = stringResource(R.string.friends_scan_qr_result_sheet_invalid_title),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = stringResource(R.string.friends_scan_qr_result_sheet_invalid_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalContentColor.current.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                qrResult = null
                                contents = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                }
            }
        }
    }

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
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.friends),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        if (useDrawer) {
                            IconButton(onClick = {
                                onDrawerClicked()
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.icn_menu_24dp),
                                    contentDescription = stringResource(id = R.string.menu)
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            overflowMenuShown = true
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.icn_more_vert_24dp),
                                contentDescription = stringResource(R.string.menu)
                            )
                        }
                        DropdownMenu(
                            expanded = overflowMenuShown,
                            onDismissRequest = {
                                overflowMenuShown = false
                            }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(R.string.friends_deny_all_incoming))
                                },
                                onClick = {
                                    scope.launch {
                                        overflowMenuShown = false
                                    }
                                    with(Dispatchers.IO) {
                                        scope.launch {
                                            FriendRequests.getIncoming()
                                                .forEach { it.id?.let { id -> unfriendUser(id) } }
                                        }
                                    }
                                }
                            )
                        }
                    },
                    windowInsets = WindowInsets.zero
                )
            }
        },
    ) { pv ->
        Box(
            modifier = Modifier
                .padding(pv)
                .fillMaxHeight()
        ) {
            LazyColumn {
                stickyHeader(key = "incoming") {
                    CountableListHeader(
                        text = stringResource(id = R.string.friends_incoming_requests),
                        count = FriendRequests.getIncoming().size
                    )
                }

                items(FriendRequests.getIncoming().size) {
                    val item = FriendRequests.getIncoming().getOrNull(it)
                    if (item == null) return@items

                    MemberListItem(
                        member = null,
                        user = item,
                        serverId = null,
                        userId = item.id ?: "",
                        modifier = Modifier.clickable {
                            scope.launch {
                                item.id?.let { userId ->
                                    ActionChannel.send(Action.OpenUserSheet(userId, null))
                                }
                            }
                        }
                    )
                }

                stickyHeader(key = "outgoing") {
                    CountableListHeader(
                        text = stringResource(id = R.string.friends_outgoing_requests),
                        count = FriendRequests.getOutgoing().size
                    )
                }

                items(FriendRequests.getOutgoing().size) {
                    val item = FriendRequests.getOutgoing().getOrNull(it)
                    if (item == null) return@items

                    MemberListItem(
                        member = null,
                        user = item,
                        serverId = null,
                        userId = item.id ?: "",
                        modifier = Modifier.clickable {
                            scope.launch {
                                item.id?.let { userId ->
                                    ActionChannel.send(Action.OpenUserSheet(userId, null))
                                }
                            }
                        }
                    )
                }

                stickyHeader(key = "online") {
                    CountableListHeader(
                        text = stringResource(id = R.string.status_online),
                        count = FriendRequests.getOnlineFriends().size
                    )
                }

                items(FriendRequests.getOnlineFriends().size) {
                    val item = FriendRequests.getOnlineFriends().getOrNull(it)
                    if (item == null) return@items

                    MemberListItem(
                        member = null,
                        user = item,
                        serverId = null,
                        userId = item.id ?: "",
                        modifier = Modifier.clickable {
                            scope.launch {
                                item.id?.let { userId ->
                                    ActionChannel.send(Action.OpenUserSheet(userId, null))
                                }
                            }
                        }
                    )
                }

                stickyHeader(key = "not_online") {
                    CountableListHeader(
                        text = stringResource(id = R.string.friends_all),
                        count = FriendRequests.getFriends(true).size
                    )
                }

                items(FriendRequests.getFriends(true).size) {
                    val item = FriendRequests.getFriends(true).getOrNull(it)
                    if (item == null) return@items

                    MemberListItem(
                        member = null,
                        user = item,
                        serverId = null,
                        userId = item.id ?: "",
                        modifier = Modifier.clickable {
                            scope.launch {
                                item.id?.let { userId ->
                                    ActionChannel.send(Action.OpenUserSheet(userId, null))
                                }
                            }
                        }
                    )
                }

                stickyHeader(key = "blocked") {
                    CountableListHeader(
                        text = stringResource(id = R.string.friends_blocked),
                        count = FriendRequests.getBlocked().size
                    )
                }


                items(FriendRequests.getBlocked().size) {
                    val item = FriendRequests.getBlocked().getOrNull(it)
                    if (item == null) return@items

                    MemberListItem(
                        member = null,
                        user = item,
                        serverId = null,
                        userId = item.id ?: "",
                        modifier = Modifier.clickable {
                            scope.launch {
                                item.id?.let { userId ->
                                    ActionChannel.send(Action.OpenUserSheet(userId, null))
                                }
                            }
                        }
                    )
                }
            }

            FloatingActionButtonMenu(
                modifier = Modifier.align(Alignment.BottomEnd),
                expanded = fabMenuExpanded,
                button = {
                    ToggleFloatingActionButton(
                        modifier =
                            Modifier
                                .semantics {
                                    traversalIndex = -1f
                                }
                                .animateFloatingActionButton(
                                    visible = fabVisible || fabMenuExpanded,
                                    alignment = Alignment.BottomEnd
                                ),
                        checked = fabMenuExpanded,
                        onCheckedChange = { fabMenuExpanded = !fabMenuExpanded }
                    ) {
                        val imageVector by remember {
                            derivedStateOf {
                                if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.Add
                            }
                        }
                        Icon(
                            painter = rememberVectorPainter(imageVector),
                            contentDescription = null,
                            modifier = Modifier.animateIcon({ checkedProgress })
                        )
                    }
                }
            ) {
                FloatingActionButtonMenuItem(
                    modifier = Modifier.semantics { isTraversalGroup = true },
                    onClick = {
                        topNav.navigate("create/group")
                        fabMenuExpanded = false
                    },
                    icon = {
                        Icon(
                            painterResource(R.drawable.icn_group_add_24dp),
                            contentDescription = null
                        )
                    },
                    text = { Text(stringResource(R.string.friends_new_group)) },
                )
                FloatingActionButtonMenuItem(
                    modifier = Modifier.semantics { isTraversalGroup = true },
                    onClick = {
                        fabMenuExpanded = false
                        scanQrCodeLauncher.launch(null)
                    },
                    icon = {
                        Icon(
                            painterResource(R.drawable.icn_qr_code_scanner_24dp),
                            contentDescription = null
                        )
                    },
                    text = { Text(stringResource(R.string.friends_scan_qr)) },
                )
                FloatingActionButtonMenuItem(
                    modifier = Modifier.semantics { isTraversalGroup = true },
                    onClick = {
                        fabMenuExpanded = false
                        addByTagSheetVisible = true
                    },
                    icon = {
                        Icon(
                            painterResource(R.drawable.icn_tag_24dp),
                            contentDescription = null
                        )
                    },
                    text = { Text(stringResource(R.string.friends_add_by_tag)) },
                )
            }
        }
    }
}