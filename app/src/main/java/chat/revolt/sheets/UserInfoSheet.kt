package chat.revolt.sheets

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.BrushCompat
import chat.revolt.api.internals.ULID
import chat.revolt.api.internals.solidColor
import chat.revolt.api.routes.user.fetchUserProfile
import chat.revolt.api.schemas.Profile
import chat.revolt.api.settings.Experiments
import chat.revolt.api.settings.FeatureFlags
import chat.revolt.composables.chat.RoleListEntry
import chat.revolt.composables.chat.UserBadgeList
import chat.revolt.composables.chat.UserBadgeRow
import chat.revolt.composables.generic.NonIdealState
import chat.revolt.composables.generic.UserAvatar
import chat.revolt.composables.markdown.RichMarkdown
import chat.revolt.composables.screens.settings.RawUserOverview
import chat.revolt.composables.screens.settings.UserButtons
import chat.revolt.composables.sheets.SheetTile
import kotlinx.datetime.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoSheet(
    userId: String,
    serverId: String? = null,
    dismissSheet: suspend () -> Unit
) {
    val user = RevoltAPI.userCache[userId]

    val member = serverId?.let { RevoltAPI.members.getMember(it, userId) }

    val server = RevoltAPI.serverCache[serverId]

    var profile by remember { mutableStateOf<Profile?>(null) }
    var profileNotFound by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        try {
            user?.id?.let { fetchUserProfile(it) }?.let { profile = it }
        } catch (e: Exception) {
            if (e.message == "NotFound") {
                profileNotFound = true
            }
            e.printStackTrace()
        }
    }

    if (user == null) {
        // TODO fetch user in this scenario
        NonIdealState(
            icon = {
                Icon(
                    painter = painterResource(R.drawable.icn_error_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(it)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.user_info_sheet_user_not_found)
                )
            },
            description = {
                Text(
                    text = stringResource(R.string.user_info_sheet_user_not_found_description)
                )
            }
        )
        Spacer(Modifier.height(20.dp))
        return
    }

    var showUserCard by remember { mutableStateOf(false) }
    if (showUserCard) {
        val sheetState = rememberModalBottomSheetState(true)
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { showUserCard = false }
        ) {
            UserCardSheet(user)
        }
    }

    var showServerIdentityOptions by remember { mutableStateOf(false) }
    if (showServerIdentityOptions) {
        val sheetState = rememberModalBottomSheetState(true)
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { showServerIdentityOptions = false }
        ) {
            ServerIdentityOptionsSheet(
                userId = user.id!!
            )
        }
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalItemSpacing = 16.dp,
        modifier = Modifier.padding(16.dp)
    ) {
        item(key = "overview", span = StaggeredGridItemSpan.FullLine) {
            Box {
                RawUserOverview(user, profile, internalPadding = false)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                ) {
                    if (Experiments.enableServerIdentityOptions.isEnabled) {
                        SmallFloatingActionButton(
                            onClick = { showServerIdentityOptions = true },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.icn_psychology_alt_24dp),
                                contentDescription = null
                            )
                        }
                    }

                    if (FeatureFlags.userCardsGranted) {
                        SmallFloatingActionButton(
                            onClick = { showUserCard = true },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.icn_badge_24dp),
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }

        member?.roles?.let {
            item(key = "roles") {
                SheetTile(
                    header = {
                        Text(stringResource(R.string.user_info_sheet_category_roles))
                    },
                    contentPreview = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            it
                                .map { roleId -> server?.roles?.get(roleId) }
                                .sortedBy { it?.rank ?: 0.0 }
                                .take(3)
                                .forEach { role ->
                                    role?.let {
                                        RoleListEntry(
                                            label = role.name ?: "null",
                                            brush = role.colour?.let { BrushCompat.parseColour(it) }
                                                ?: Brush.solidColor(LocalContentColor.current)
                                        )
                                    }
                                }
                        }
                    }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        it
                            .map { roleId -> server?.roles?.get(roleId) }
                            .sortedBy { it?.rank ?: 0.0 }
                            .forEach { role ->
                                role?.let {
                                    RoleListEntry(
                                        label = role.name ?: "null",
                                        brush = role.colour?.let { BrushCompat.parseColour(it) }
                                            ?: Brush.solidColor(LocalContentColor.current)
                                    )
                                }
                            }
                    }
                }
            }
        }
        val accountAt = user.id?.let {
            DateUtils.getRelativeTimeSpanString(
                ULID.asTimestamp(user.id),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            ).toString()
        }
        val joinedAt = member?.joinedAt?.let {
            DateUtils.getRelativeTimeSpanString(
                Instant.parse(member.joinedAt).toEpochMilliseconds(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            ).toString()
        }

        item(key = "joined") {
            SheetTile(
                header = {
                    Text(stringResource(R.string.user_info_sheet_category_joined))
                },
                contentPreview = {
                    if (joinedAt != null && server?.name != null) {
                        Text(
                            text = joinedAt,
                            fontSize = 14.sp
                        )

                        Text(
                            text = server.name,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(4.dp))
                    }

                    accountAt?.let { _ ->
                        Text(
                            text = accountAt,
                            fontSize = 14.sp
                        )

                        Text(
                            text = stringResource(id = R.string.user_info_sheet_category_joined_revolt),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            ) {
                if (joinedAt != null && server?.name != null) {
                    Text(
                        text = joinedAt,
                        style = MaterialTheme.typography.displaySmall
                    )

                    Text(
                        text = server.name,
                        style = MaterialTheme.typography.labelMedium
                    )

                    Spacer(Modifier.height(8.dp))
                }

                accountAt?.let { _ ->
                    Text(
                        text = accountAt,
                        style = MaterialTheme.typography.displaySmall
                    )

                    Text(
                        text = stringResource(id = R.string.user_info_sheet_category_joined_revolt),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        if ((user.badges ?: 0) > 0) {
            item(key = "info") {
                SheetTile(
                    header = {
                        Text(stringResource(R.string.user_info_sheet_category_badges))
                    },
                    contentPreview = {
                        user.badges?.let { UserBadgeRow(badges = it) }
                    }
                ) {
                    user.badges?.let { UserBadgeList(badges = it) }
                }
            }
        }

        if (user.status?.text != null) {
            item(key = "status") {
                SheetTile(
                    header = {
                        Text(stringResource(R.string.user_info_sheet_category_status))
                    },
                    contentPreview = {
                        Text(
                            text = user.status.text,
                            fontSize = 14.sp,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                ) {
                    Text(
                        text = user.status.text,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (user.bot != null) {
            val resolvedOwner = user.bot.owner?.let { RevoltAPI.userCache[it] }

            item(key = "bot-owner") {
                SheetTile(
                    header = {
                        Text(stringResource(R.string.user_info_sheet_category_owner))
                    },
                    contentPreview = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            resolvedOwner?.let {
                                UserAvatar(
                                    username = it.displayName ?: it.username
                                    ?: stringResource(R.string.unknown),
                                    avatar = it.avatar,
                                    userId = it.id!!,
                                    size = 32.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = it.displayName ?: it.username
                                    ?: stringResource(R.string.unknown),
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } ?: run {
                                Icon(
                                    painter = painterResource(id = R.drawable.icn_error_24dp),
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.unknown),
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                ) {
                    resolvedOwner?.let {
                        RawUserOverview(it, null, internalPadding = false)
                    } ?: run {
                        NonIdealState(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.icn_error_24dp),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            title = {
                                Text(
                                    text = stringResource(R.string.user_info_sheet_owner_not_found)
                                )
                            }
                        )
                    }
                }
            }
        }

        if (profile?.content.isNullOrBlank().not()) {
            item(key = "bio", span = StaggeredGridItemSpan.FullLine) {
                SheetTile(
                    header = {
                        Text(stringResource(R.string.user_info_sheet_category_bio))
                    },
                    contentPreview = {
                        RichMarkdown(input = profile?.content!!)
                    }
                ) {
                    SelectionContainer(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        RichMarkdown(input = profile?.content!!)
                    }
                }
            }
        }

        item(key = "actions", span = StaggeredGridItemSpan.FullLine) {
            UserButtons(user, dismissSheet)
        }
    }

}