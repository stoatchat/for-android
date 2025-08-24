package chat.peptide.composables.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chat.peptide.R
import chat.peptide.api.PeptideAPI
import chat.peptide.api.internals.SpecialUsers
import chat.peptide.api.internals.ULID
import chat.peptide.api.internals.solidColor
import chat.peptide.api.routes.user.fetchUserProfile
import chat.peptide.api.schemas.AutumnResource
import chat.peptide.api.schemas.Profile
import chat.peptide.api.schemas.User
import chat.peptide.composables.generic.RemoteImage
import chat.peptide.composables.generic.UserAvatar
import chat.peptide.composables.generic.presenceFromStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

@Composable
fun SelfUserOverview() {
    val selfUser = PeptideAPI.userCache[PeptideAPI.selfId] ?: return

    UserOverview(selfUser)
}

@Composable
fun UserOverview(user: User, internalPadding: Boolean = true) {
    var profile by remember { mutableStateOf<Profile?>(null) }

    LaunchedEffect(user) {
        try {
            if (profile == null) {
                profile = fetchUserProfile(user.id ?: ULID.makeSpecial(0))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    RawUserOverview(user, profile, internalPadding = internalPadding)
}

@Composable
fun RawUserOverview(
    user: User,
    profile: Profile? = null,
    pfpUrl: String? = null,
    backgroundUrl: String? = null,
    internalPadding: Boolean = true
) {
    val context = LocalContext.current
    var teamMemberFlair by remember { mutableStateOf<Brush?>(null) }

    LaunchedEffect(user) {
        runBlocking(Dispatchers.IO) {
            user.id?.let {
                teamMemberFlair = SpecialUsers.teamFlairAsBrush(
                    context,
                    it
                )
            }
        }
    }

    Box(
        contentAlignment = Alignment.BottomStart,
        modifier = Modifier
            .height(128.dp)
            .padding(horizontal = if (internalPadding) 16.dp else 0.dp)
            .clip(MaterialTheme.shapes.large)
            .then(
                if (user.id in SpecialUsers.TEAM_MEMBER_FLAIRS.keys) {
                    Modifier
                        .border(
                            width = 4.dp,
                            brush = teamMemberFlair
                                ?: Brush.solidColor(Color.Transparent),
                            shape = MaterialTheme.shapes.large
                        )
                } else {
                    Modifier
                }
            )
    ) {
        val background = backgroundUrl ?: profile?.background
        if (background != null) {
            RemoteImage(
                url = backgroundUrl
                    ?: "${PeptideAPI.getCurrentFilesUrl()}/backgrounds/${if (background is AutumnResource) background.id else null}/${if (background is AutumnResource) background.filename else background}",
                description = null,
                modifier = Modifier
                    .height(128.dp)
                    .fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )

            Box(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .height(128.dp)
                    .fillMaxWidth()
            )
        } else {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .height(128.dp)
                    .fillMaxWidth()
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            UserAvatar(
                username = user.displayName ?: stringResource(id = R.string.unknown),
                rawUrl = pfpUrl,
                userId = user.id ?: ULID.makeSpecial(0),
                avatar = user.avatar,
                size = 48.dp,
                presence = presenceFromStatus(user.status?.presence, user.online ?: false)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = AnnotatedString.Builder().apply {
                    // make sure
                    // - the display name is not null or blank
                    // - the display name is not the same as the username; both trimmed
                    if (!user.displayName.isNullOrBlank() && ((user.displayName.trim() == user.username?.trim()) == false)) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(user.displayName)
                        pop()
                        append("\n")
                    }
                    append("${user.username}")
                    pushStyle(SpanStyle(fontWeight = FontWeight.ExtraLight))
                    append("#${user.discriminator}")
                    pop()
                }.toAnnotatedString(),
                color = if (profile?.background != null) Color.White else LocalContentColor.current
            )
        }
    }
}
