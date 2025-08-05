package chat.revolt.composables.chat

import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.BrushCompat
import chat.revolt.api.internals.Roles
import chat.revolt.api.internals.solidColor
import chat.revolt.api.schemas.Member
import chat.revolt.api.schemas.User
import chat.revolt.composables.generic.UserAvatar
import chat.revolt.composables.generic.presenceFromStatus
import chat.revolt.internals.extensions.TransparentListItemColours

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
            Text(
                text = member?.nickname
                    ?: user?.displayName
                    ?: user?.username
                    ?: user?.id
                    ?: userId,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = LocalTextStyle.current.copy(brush = colour),
            )
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
                rawUrl = member?.avatar?.let { "${RevoltAPI.getCurrentFilesUrl()}/avatars/${it.id}" },
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