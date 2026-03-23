package chat.zekochat.composables.chat

import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.zekochat.api.PeptideAPI
import chat.zekochat.api.internals.BrushCompat
import chat.zekochat.api.internals.Roles
import chat.zekochat.api.internals.solidColor
import chat.zekochat.api.schemas.Member
import chat.zekochat.api.schemas.User
import chat.zekochat.composables.generic.UserAvatar
import chat.zekochat.composables.generic.presenceFromStatus
import chat.zekochat.internals.extensions.TransparentListItemColours

@Composable
fun MemberListItem(
    member: Member?,
    user: User?,
    serverId: String?,
    userId: String,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val highestColourRole = serverId?.let {
        user?.id?.let { userId ->
            Roles.resolveHighestRole(
                it,
                userId,
                true
            )
        }
    }

    val colour = highestColourRole?.colour?.let { BrushCompat.parseColour(it) }
        ?: Brush.solidColor(LocalContentColor.current)

    ListItem(
        colors = TransparentListItemColours,
        modifier = modifier,
        headlineContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = member?.nickname
                            ?: user?.displayName
                            ?: user?.username
                            ?: user?.id
                            ?: userId,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = LocalTextStyle.current.copy(brush = colour),
                        modifier = Modifier.weight(1f)
                    )

                    if (hasVerifiedVendor(user?.badges)) {
                        Spacer(modifier = Modifier.width(4.dp))
                        UserVendorBadge(
                            badges = user?.badges,
                            size = 16.dp
                        )
                    }
                }
        },
        supportingContent = {
            user?.status?.text?.let {
                if (user.online == true) {
                    Text(
                        text = it,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        leadingContent = {
            UserAvatar(
                username = member?.nickname
                    ?: user?.displayName
                    ?: user?.username
                    ?: user?.id
                    ?: userId,
                avatar = user?.avatar,
                rawUrl = member?.avatar?.let { "${PeptideAPI.getCurrentFilesUrl()}/avatars/${it.id}" },
                userId = userId,
                presence = presenceFromStatus(
                    user?.status?.presence,
                    user?.online ?: false
                )
            )
        },
        trailingContent = trailingContent
    )
}