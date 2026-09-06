package chat.stoat.sheets

import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.stoat.R
import chat.stoat.api.HitRateLimitException
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.BrushCompat
import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.internals.Roles
import chat.stoat.api.internals.ULID
import chat.stoat.api.internals.has
import chat.stoat.api.internals.solidColor
import chat.stoat.api.routes.server.patchMember
import chat.stoat.api.routes.user.fetchUserProfile
import chat.stoat.api.settings.Experiments
import chat.stoat.api.settings.FeatureFlags
import chat.stoat.composables.chat.RoleListEntry
import chat.stoat.composables.chat.UserBadgeList
import chat.stoat.composables.chat.UserBadgeRow
import chat.stoat.composables.generic.NonIdealState
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.composables.markdown.prose.ChatMarkdown
import chat.stoat.composables.screens.settings.RawUserOverview
import chat.stoat.composables.screens.settings.UserButtons
import chat.stoat.composables.sheets.SheetTile
import chat.stoat.core.model.schemas.Profile
import chat.stoat.internals.extensions.rememberServerPermissions
import chat.stoat.internals.server.canManageServerRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import androidx.compose.ui.semantics.Role as SemanticsRole

private fun canAssignRolesToMember(serverId: String, targetUserId: String): Boolean {
    val server = StoatAPI.serverCache[serverId] ?: return false
    val selfId = StoatAPI.selfId ?: return false

    if (selfId == targetUserId) return true
    if (server.owner == selfId || StoatAPI.userCache[selfId]?.privileged == true) return true
    if (server.owner == targetUserId) return false

    val selfRank = Roles.resolveHighestRole(serverId, selfId)?.rank ?: Double.MAX_VALUE
    val targetRank =
        Roles.resolveHighestRole(serverId, targetUserId)?.rank ?: Double.MAX_VALUE
    return targetRank > selfRank
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoSheet2(
    userId: String,
    serverId: String? = null,
    dismissSheet: suspend () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = StoatAPI.userCache[userId]

    val member = serverId?.let { StoatAPI.members.getMember(it, userId) }

    val server = StoatAPI.serverCache[serverId]
    val permissions by rememberServerPermissions(serverId.orEmpty())
    var assignedRoleIds by remember(serverId, userId) {
        mutableStateOf(member?.roles.orEmpty())
    }
    var updatingRoleId by remember(serverId, userId) { mutableStateOf<String?>(null) }
    val rateLimitMessage = stringResource(R.string.rate_limit_toast)
    val roleUpdateError = stringResource(R.string.user_info_sheet_role_update_failed)

    fun updateRole(roleId: String, assigned: Boolean) {
        val currentServerId = serverId ?: return
        if (updatingRoleId != null) return

        val previousRoleIds = assignedRoleIds
        val updatedRoleIds = if (assigned) {
            (assignedRoleIds + roleId).distinct()
        } else {
            assignedRoleIds - roleId
        }
        assignedRoleIds = updatedRoleIds
        updatingRoleId = roleId

        scope.launch {
            try {
                val updatedMember = patchMember(currentServerId, userId, roles = updatedRoleIds)
                assignedRoleIds = updatedMember.roles.orEmpty()
            } catch (error: CancellationException) {
                assignedRoleIds = previousRoleIds
                throw error
            } catch (error: Exception) {
                assignedRoleIds = previousRoleIds
                Toast.makeText(
                    context,
                    if (error is HitRateLimitException) rateLimitMessage else roleUpdateError,
                    Toast.LENGTH_SHORT,
                ).show()
            } finally {
                updatingRoleId = null
            }
        }
    }

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
                    painter = painterResource(R.drawable.ic_error_24dp),
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
        item(key = "nonsense", span = StaggeredGridItemSpan.FullLine) {
            Text(
                "This is UserInfoSheet2",
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
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
                                painter = painterResource(R.drawable.ic_psychology_alt_24dp),
                                contentDescription = null
                            )
                        }
                    }

                    if (FeatureFlags.userCardsGranted) {
                        SmallFloatingActionButton(
                            onClick = { showUserCard = true },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_badge_24dp),
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }

        if (member != null) {
            item(key = "roles") {
                val assignedRoles = assignedRoleIds.mapNotNull { roleId ->
                    server?.roles?.get(roleId)?.let { role -> roleId to role }
                }.sortedBy { (_, role) -> role.rank ?: Double.MAX_VALUE }
                val canAssignRoles = permissions has PermissionBit.AssignRoles
                val canAssignToMember = serverId?.let {
                    canAssignRolesToMember(it, userId)
                } == true
                val availableRoles = server?.roles.orEmpty()
                    .map { (roleId, role) -> roleId to role }
                    .sortedBy { (_, role) -> role.rank ?: Double.MAX_VALUE }

                SheetTile(
                    header = {
                        Text(stringResource(R.string.user_info_sheet_category_roles))
                    },
                    contentPreview = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            assignedRoles
                                .take(3)
                                .forEach { (_, role) ->
                                    RoleListEntry(
                                        label = role.name ?: "null",
                                        brush = role.colour?.let { BrushCompat.parseColour(it) }
                                            ?: Brush.solidColor(LocalContentColor.current),
                                        icon = role.icon
                                    )
                                }
                        }
                    }
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    ) {
                        if (canAssignRoles) {
                            availableRoles.forEach { (roleId, role) ->
                                val checked = roleId in assignedRoleIds
                                val canManageRole = server?.let {
                                    canManageServerRole(it, role)
                                } == true
                                val enabled =
                                    canAssignToMember && canManageRole && updatingRoleId == null

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .toggleable(
                                            value = checked,
                                            enabled = enabled,
                                            role = SemanticsRole.Checkbox,
                                            onValueChange = { updateRole(roleId, it) },
                                        ),
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = null,
                                        enabled = enabled,
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    RoleListEntry(
                                        label = role.name ?: "null",
                                        brush = role.colour?.let { c -> BrushCompat.parseColour(c) }
                                            ?: Brush.solidColor(LocalContentColor.current),
                                        icon = role.icon,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        } else {
                            assignedRoles.forEach { (_, role) ->
                                RoleListEntry(
                                    label = role.name ?: "null",
                                    brush = role.colour?.let { c -> BrushCompat.parseColour(c) }
                                        ?: Brush.solidColor(LocalContentColor.current),
                                    icon = role.icon
                                )
                            }
                        }
                    }
                }
            }
        }
        val accountAt = user.id?.let {
            DateUtils.getRelativeTimeSpanString(
                ULID.asTimestamp(user.id!!),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            ).toString()
        }
        val joinedAt = member?.joinedAt?.let {
            DateUtils.getRelativeTimeSpanString(
                Instant.parse(member.joinedAt!!).toEpochMilliseconds(),
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
                            text = server.name!!,
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
                            text = stringResource(id = R.string.user_info_sheet_category_joined_stoat),
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
                        text = server.name!!,
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
                        text = stringResource(id = R.string.user_info_sheet_category_joined_stoat),
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
                            text = user.status!!.text!!,
                            fontSize = 14.sp,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                ) {
                    Text(
                        text = user.status!!.text!!,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (user.bot != null) {
            val resolvedOwner = user.bot!!.owner?.let { StoatAPI.userCache[it] }

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
                                    painter = painterResource(id = R.drawable.ic_error_24dp),
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
                                    painter = painterResource(R.drawable.ic_error_24dp),
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
                        ChatMarkdown(content = profile?.content!!, serverId = serverId)
                    }
                ) {
                    SelectionContainer(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        ChatMarkdown(content = profile?.content!!, serverId = serverId)
                    }
                }
            }
        }

        item(key = "actions", span = StaggeredGridItemSpan.FullLine) {
            UserButtons(
                user = user,
                serverId = serverId,
                dismissSheet = dismissSheet,
            )
        }
    }

}
