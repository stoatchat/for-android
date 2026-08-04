package chat.stoat.composables.generic

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import chat.stoat.R
import chat.stoat.api.settings.LoadedSettings
import chat.stoat.core.model.data.STOAT_BASE
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.schemas.AutumnResource

enum class Presence {
    Online,
    Idle,
    Focus,
    Dnd,
    Offline
}

fun presenceFromStatus(status: String?, online: Boolean = true): Presence {
    if (!online) return Presence.Offline

    return when (status) {
        "Online", null -> Presence.Online
        "Idle" -> Presence.Idle
        "Busy" -> Presence.Dnd
        "Focus" -> Presence.Focus
        else -> Presence.Offline
    }
}

fun Presence.asApiName(): String {
    return when (this) {
        Presence.Online -> "Online"
        Presence.Idle -> "Idle"
        Presence.Dnd -> "Busy"
        Presence.Focus -> "Focus"
        Presence.Offline -> "Invisible"
    }
}

fun presenceColour(presence: Presence): Color {
    return when (presence) {
        Presence.Online -> Color(0xFF3ABF7E)
        Presence.Idle -> Color(0xFFF39F00)
        Presence.Dnd -> Color(0xFFF84848)
        Presence.Focus -> Color(0xFF4799F0)
        Presence.Offline -> Color(0xFFA5A5A5)
    }
}

private fun Presence.iconResource(): Int = when (this) {
    Presence.Online -> R.drawable.ic_circle_24dp__fill
    Presence.Idle -> R.drawable.ic_bedtime_24dp
    Presence.Dnd -> R.drawable.ic_do_not_disturb_on_24dp
    Presence.Focus -> R.drawable.ic_circle_circle_24dp
    Presence.Offline -> R.drawable.ic_circle_24dp
}

private fun Presence.descriptionResource(): Int = when (this) {
    Presence.Online -> R.string.status_online
    Presence.Idle -> R.string.status_idle
    Presence.Dnd -> R.string.status_dnd
    Presence.Focus -> R.string.status_focus
    Presence.Offline -> R.string.status_offline
}

@Composable
fun PresenceBadge(presence: Presence, size: Dp = 16.dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size)
    ) {
        Icon(
            painter = painterResource(presence.iconResource()),
            contentDescription = stringResource(presence.descriptionResource()),
            tint = presenceColour(presence),
            modifier = Modifier
                .size((size - PresenceCutoutPadding * 2).coerceAtLeast(0.dp))
        )
    }
}

private val PresenceCutoutPadding = 2.dp

internal fun Modifier.bottomEndCircleCutout(diameter: Dp): Modifier =
    graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
    }.drawWithCache {
        val radius = diameter.toPx() / 2
        val center = Offset(
            x = if (layoutDirection == LayoutDirection.Ltr) size.width - radius else radius,
            y = size.height - radius
        )

        onDrawWithContent {
            drawContent()
            drawCircle(
                color = Color.Transparent,
                radius = radius,
                center = center,
                blendMode = BlendMode.Clear
            )
        }
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserAvatar(
    username: String,
    userId: String,
    modifier: Modifier = Modifier,
    presence: Presence? = null,
    avatar: AutumnResource? = null,
    rawUrl: String? = null,
    size: Dp = 40.dp,
    presenceSize: Dp = 16.dp,
    shape: Shape = RoundedCornerShape(LoadedSettings.avatarRadius),
    allowAnimation: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(size),
        contentAlignment = Alignment.BottomEnd
    ) {
        if (avatar != null) {
            RemoteImage(
                url = rawUrl ?: "$STOAT_FILES/avatars/${avatar.id}",
                contentScale = ContentScale.Crop,
                description = stringResource(id = R.string.avatar_alt, username),
                allowAnimation = allowAnimation,
                modifier = Modifier
                    .clip(shape)
                    .size(size)
                    .then(
                        if (presence != null) {
                            Modifier.bottomEndCircleCutout(presenceSize)
                        } else {
                            Modifier
                        }
                    )
                    .then(
                        if (onLongClick != null || onClick != null) {
                            Modifier
                                .combinedClickable(
                                    onClick = { onClick?.invoke() },
                                    onLongClick = { onLongClick?.invoke() }
                                )
                        } else {
                            Modifier
                        }
                    )
            )
        } else {
            RemoteImage(
                url = "$STOAT_BASE/users/${userId.ifBlank { "0".repeat(26) }}/default_avatar",
                description = stringResource(id = R.string.avatar_alt, username),
                allowAnimation = allowAnimation,
                modifier = Modifier
                    .clip(shape)
                    .size(size)
                    .then(
                        if (presence != null) {
                            Modifier.bottomEndCircleCutout(presenceSize)
                        } else {
                            Modifier
                        }
                    )
                    .then(
                        if (onLongClick != null || onClick != null) {
                            Modifier
                                .combinedClickable(
                                    onClick = { onClick?.invoke() },
                                    onLongClick = { onLongClick?.invoke() }
                                )
                        } else {
                            Modifier
                        }
                    )
            )
        }

        if (presence != null) {
            PresenceBadge(presence, size = presenceSize)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupIcon(
    name: String,
    modifier: Modifier = Modifier,
    icon: AutumnResource? = null,
    rawUrl: String? = null,
    size: Dp = 40.dp,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(size),
        contentAlignment = Alignment.BottomEnd
    ) {
        if (icon?.id != null) {
            RemoteImage(
                url = rawUrl ?: "$STOAT_FILES/icons/${icon.id}",
                allowAnimation = false,
                contentScale = ContentScale.Crop,
                description = stringResource(id = R.string.avatar_alt, name),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .size(size)
                    .then(
                        if (onLongClick != null || onClick != null) {
                            Modifier
                                .combinedClickable(
                                    onClick = { onClick?.invoke() },
                                    onLongClick = { onLongClick?.invoke() }
                                )
                        } else {
                            Modifier
                        }
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .then(
                        if (onLongClick != null || onClick != null) {
                            Modifier
                                .combinedClickable(
                                    onClick = { onClick?.invoke() },
                                    onLongClick = { onLongClick?.invoke() }
                                )
                        } else {
                            Modifier
                        }
                    )
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = name.first().toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun UserAvatarWidthPlaceholder(size: Dp = 40.dp) {
    Box(
        modifier = Modifier
            .width(size)
    )
}

// Note - Preview will not render due to Glide not being able to load images in preview (NPE)
// including here anyways on the off chance that it gets fixed in the future, or we switch to Coil lol
@Preview
@Composable
fun UserAvatarWithPresencePreview() {
    UserAvatar(
        username = "infi",
        userId = "01F1WKM5TK2V6KCZWR6DGBJDTZ",
        presence = Presence.Online
    )
}
