package chat.stoat.composables.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import chat.stoat.api.internals.BrushCompat
import chat.stoat.api.internals.Roles
import chat.stoat.api.internals.solidColor
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.composables.generic.presenceFromStatus
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.schemas.Member
import chat.stoat.core.model.schemas.User

private val NoneLambda = { }

@Composable
fun MemberListItem(
    member: Member?,
    user: User?,
    serverId: String?,
    userId: String,
    modifier: Modifier = Modifier,
    first: Boolean = false,
    last: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = NoneLambda,
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
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = modifier
            .then(
                when {
                    first && last -> Modifier.clip(MaterialTheme.shapes.large)

                    first -> Modifier.clip(
                        MaterialTheme.shapes.extraSmall.copy(
                            topStart = MaterialTheme.shapes.large.topStart,
                            topEnd = MaterialTheme.shapes.large.topEnd
                        )
                    )

                    last -> Modifier.clip(
                        MaterialTheme.shapes.extraSmall.copy(
                            bottomStart = MaterialTheme.shapes.large.bottomStart,
                            bottomEnd = MaterialTheme.shapes.large.bottomEnd
                        )
                    )

                    else -> Modifier.clip(MaterialTheme.shapes.extraSmall)
                }
            )
            .then(
                if (onLongClick != NoneLambda) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier.clickable(
                        onClick = onClick
                    )
                }
            ),
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
                rawUrl = member?.avatar?.let { "$STOAT_FILES/avatars/${it.id}" },
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