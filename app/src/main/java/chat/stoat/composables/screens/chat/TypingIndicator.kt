package chat.stoat.composables.screens.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.stoat.R
import chat.stoat.activities.StoatTweenColour
import chat.stoat.activities.StoatTweenFloat
import chat.stoat.activities.StoatTweenInt
import chat.stoat.activities.StoatTweenSize
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.formatCompactDuration
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.schemas.User
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

@Composable
fun StackedUserAvatars(
    users: List<String>,
    amount: Int = 3,
    size: Dp = 16.dp,
    offset: Dp = 8.dp,
    serverId: String?
) {
    Box(
        modifier = Modifier
            .size(size + (offset * minOf(users.size, amount)), size)
    ) {
        users.take(amount).forEachIndexed { index, userId ->
            val user = StoatAPI.userCache[userId]
            val maybeMember = serverId?.let { StoatAPI.members.getMember(serverId, userId) }

            UserAvatar(
                avatar = user?.avatar,
                userId = userId,
                username = user?.let { User.resolveDefaultName(it) }
                    ?: stringResource(id = R.string.unknown),
                rawUrl = maybeMember?.avatar?.let { "$STOAT_FILES/avatars/${it.id}" },
                size = size,
                modifier = Modifier
                    .offset(
                        x = (index * offset.value).dp
                    )
            )
        }
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun TypingIndicator(
    users: List<String>,
    serverId: String?,
    slowmodeSeconds: Long? = null,
    slowmodeRemainingSeconds: Long = 0,
    slowmodeImmune: Boolean = false,
    hazeState: HazeState? = null,
) {
    val slowmodeEnabled = slowmodeSeconds != null && slowmodeSeconds > 0
    val slowmodeActive = !slowmodeImmune && slowmodeRemainingSeconds > 0
    var lastTypingUsers by remember { mutableStateOf(emptyList<String>()) }
    var lastSlowmodeSeconds by remember { mutableLongStateOf(0L) }
    val displayedTypingUsers = users.ifEmpty { lastTypingUsers }
    val displayedSlowmodeSeconds = slowmodeSeconds?.takeIf { it > 0 }
        ?: lastSlowmodeSeconds
    SideEffect {
        if (users.isNotEmpty()) {
            lastTypingUsers = users.toList()
        }
        if (slowmodeSeconds != null && slowmodeSeconds > 0) {
            lastSlowmodeSeconds = slowmodeSeconds
        }
    }
    val idleSlowmodeColor = LocalContentColor.current
    val slowmodeColor by animateColorAsState(
        targetValue = if (slowmodeActive) {
            MaterialTheme.colorScheme.primary
        } else {
            idleSlowmodeColor
        },
        animationSpec = StoatTweenColour,
        label = "Slowmode color",
    )
    val pillShape = RoundedCornerShape(16.dp)
    val pillContainerColor = MaterialTheme.colorScheme.surfaceContainer
    val pillHazeStyle = hazeState?.let {
        HazeMaterials.thin(containerColor = pillContainerColor)
    }

    fun Modifier.pillBackground(): Modifier =
        clip(pillShape).then(
            if (hazeState != null && pillHazeStyle != null) {
                Modifier.hazeEffect(
                    state = hazeState,
                    style = pillHazeStyle
                ) {
                    blurRadius = 20.dp
                }
            } else {
                Modifier.background(pillContainerColor.copy(alpha = 0.9f))
            }
        )

    fun typingMessageResource(userCount: Int): Int {
        return when (userCount) {
            0 -> R.string.typing_blank
            1 -> R.string.typing_one
            in 2..4 -> R.string.typing_many
            else -> R.string.typing_several
        }
    }

    AnimatedVisibility(
        visible = users.isNotEmpty() || slowmodeEnabled,
        enter = expandVertically(
            animationSpec = StoatTweenSize,
            expandFrom = Alignment.Bottom,
        ),
        exit = shrinkVertically(
            animationSpec = StoatTweenSize,
            shrinkTowards = Alignment.Bottom,
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = users.isNotEmpty(),
                    enter = slideInVertically(
                        animationSpec = StoatTweenInt,
                        initialOffsetY = { it },
                    ) + fadeIn(animationSpec = StoatTweenFloat),
                    exit = slideOutVertically(
                        animationSpec = StoatTweenInt,
                        targetOffsetY = { it },
                    ) + fadeOut(animationSpec = StoatTweenFloat),
                ) {
                    Row(
                        modifier = Modifier
                            .pillBackground()
                            .padding(vertical = 6.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StackedUserAvatars(
                            users = displayedTypingUsers,
                            serverId = serverId
                        )

                        Text(
                            text = stringResource(
                                id = typingMessageResource(displayedTypingUsers.size),
                                displayedTypingUsers.joinToString { userId ->
                                    StoatAPI.userCache[userId]?.let { u ->
                                        val maybeMember = serverId?.let {
                                            StoatAPI.members.getMember(serverId, userId)
                                        }

                                        maybeMember?.nickname ?: User.resolveDefaultName(u)
                                    } ?: userId
                                }
                            ),
                            modifier = Modifier.weight(1f, fill = false),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = slowmodeEnabled,
                enter = slideInVertically(
                    animationSpec = StoatTweenInt,
                    initialOffsetY = { it },
                ) + fadeIn(animationSpec = StoatTweenFloat),
                exit = slideOutVertically(
                    animationSpec = StoatTweenInt,
                    targetOffsetY = { it },
                ) + fadeOut(animationSpec = StoatTweenFloat),
            ) {
                Row(
                    modifier = Modifier
                        .pillBackground()
                        .padding(vertical = 6.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_timer_24dp),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = slowmodeColor,
                        )
                        if (slowmodeImmune) {
                            Text(
                                text = stringResource(R.string.slowmode_immune),
                                color = slowmodeColor,
                                fontSize = 12.sp,
                                maxLines = 1,
                            )
                        } else {
                            AnimatedSlowmodeDuration(
                                seconds = slowmodeRemainingSeconds.takeIf { it > 0 }
                                    ?: displayedSlowmodeSeconds,
                                color = slowmodeColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedSlowmodeDuration(
    seconds: Long,
    color: Color,
) {
    val formattedDuration = formatCompactDuration(seconds)

    AnimatedContent(
        targetState = seconds,
        modifier = Modifier
            .clearAndSetSemantics {
                text = AnnotatedString(formattedDuration)
            },
        contentAlignment = Alignment.CenterEnd,
        contentKey = { formatCompactDuration(it).length },
        transitionSpec = {
            val movement = if (targetState > initialState) {
                (slideInVertically(StoatTweenInt) { -it } + fadeIn(StoatTweenFloat))
                    .togetherWith(
                        slideOutVertically(StoatTweenInt) { it } +
                                fadeOut(StoatTweenFloat)
                    )
            } else {
                (slideInVertically(StoatTweenInt) { it } + fadeIn(StoatTweenFloat))
                    .togetherWith(
                        slideOutVertically(StoatTweenInt) { -it } +
                                fadeOut(StoatTweenFloat)
                    )
            }

            movement.using(
                SizeTransform(clip = false) { _, _ -> StoatTweenSize }
            )
        },
        label = "Slowmode duration width",
    ) { targetSeconds ->
        Row {
            formatCompactDuration(targetSeconds)
                .mapIndexed { index, character ->
                    SlowmodeCharacter(
                        character = character,
                        totalSeconds = targetSeconds,
                        place = index,
                    )
                }
                .forEach { character ->
                    AnimatedContent(
                        targetState = character,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInVertically(StoatTweenInt) { -it } togetherWith
                                        slideOutVertically(StoatTweenInt) { it }
                            } else {
                                slideInVertically(StoatTweenInt) { it } togetherWith
                                        slideOutVertically(StoatTweenInt) { -it }
                            }
                        },
                        label = "Slowmode character",
                    ) { target ->
                        Text(
                            text = target.character.toString(),
                            color = color,
                            maxLines = 1,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                fontFeatureSettings = "tnum",
                            ),
                        )
                    }
                }
        }
    }
}

private data class SlowmodeCharacter(
    val character: Char,
    val totalSeconds: Long,
    val place: Int,
) {
    override fun equals(other: Any?): Boolean {
        return when (other) {
            is SlowmodeCharacter -> character == other.character
            else -> super.equals(other)
        }
    }

    override fun hashCode(): Int {
        var result = character.hashCode()
        result = 31 * result + totalSeconds.hashCode()
        result = 31 * result + place
        return result
    }
}

private operator fun SlowmodeCharacter.compareTo(other: SlowmodeCharacter): Int {
    return totalSeconds.compareTo(other.totalSeconds)
}
