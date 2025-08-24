package chat.peptide.composables.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.peptide.R
import chat.peptide.activities.PeptideTweenFloat
import chat.peptide.activities.PeptideTweenInt
import chat.peptide.api.PeptideAPI
import chat.peptide.api.schemas.User
import chat.peptide.composables.generic.UserAvatar

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
            val user = PeptideAPI.userCache[userId]
            val maybeMember = serverId?.let { PeptideAPI.members.getMember(serverId, userId) }

            UserAvatar(
                avatar = user?.avatar,
                userId = userId,
                username = user?.let { User.resolveDefaultName(it) }
                    ?: stringResource(id = R.string.unknown),
                rawUrl = maybeMember?.avatar?.let { "${PeptideAPI.getCurrentFilesUrl()}/avatars/${it.id}" },
                size = size,
                modifier = Modifier
                    .offset(
                        x = (index * offset.value).dp
                    )
            )
        }
    }
}

@Composable
fun TypingIndicator(users: List<String>, serverId: String?) {
    fun typingMessageResource(): Int {
        return when (users.size) {
            0 -> R.string.typing_blank
            1 -> R.string.typing_one
            in 2..4 -> R.string.typing_many
            else -> R.string.typing_several
        }
    }

    AnimatedVisibility(
        visible = users.isNotEmpty(),
        enter = slideInVertically(
            animationSpec = PeptideTweenInt,
            initialOffsetY = { it }
        ) + fadeIn(animationSpec = PeptideTweenFloat),
        exit = slideOutVertically(
            animationSpec = PeptideTweenInt,
            targetOffsetY = { it }
        ) + fadeOut(animationSpec = PeptideTweenFloat)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
                .padding(vertical = 8.dp, horizontal = 16.dp)
        ) {
            StackedUserAvatars(users = users, serverId = serverId)

            Text(
                text = stringResource(
                    id = typingMessageResource(),
                    users.joinToString { userId ->
                        PeptideAPI.userCache[userId]?.let { u ->
                            val maybeMember =
                                serverId?.let { PeptideAPI.members.getMember(serverId, userId) }

                            maybeMember?.nickname ?: User.resolveDefaultName(u)
                        } ?: userId
                    }
                ),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
