package chat.stoat.composables.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.WavyProgressIndicatorDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.SpecialUsers
import chat.stoat.api.internals.ULID
import chat.stoat.api.internals.solidColor
import chat.stoat.api.routes.user.fetchUserProfile
import chat.stoat.composables.expressive.Wave
import chat.stoat.composables.generic.RemoteImage
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.composables.generic.presenceFromStatus
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.schemas.AutumnResource
import chat.stoat.core.model.schemas.Profile
import chat.stoat.core.model.schemas.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SelfUserOverview() {
    val selfUser = StoatAPI.userCache[StoatAPI.selfId] ?: return

    UserOverview(selfUser)
}

@Composable
fun UserOverview(
    user: User,
    internalPadding: Boolean = true,
    pfpUrl: String? = null,
) {
    var profile by remember { mutableStateOf<Profile?>(null) }

    LaunchedEffect(user.id) {
        try {
            if (profile == null) {
                profile = fetchUserProfile(user.id ?: ULID.makeSpecial(0))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    RawUserOverview(
        user = user,
        profile = profile,
        pfpUrl = pfpUrl,
        internalPadding = internalPadding,
    )
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

    LaunchedEffect(user.id) {
        withContext(Dispatchers.IO) {
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
        val contentColour = if (background != null) Color.White else LocalContentColor.current
        val pronouns = user.pronouns?.trim()?.takeIf { it.isNotEmpty() }

        if (background != null) {
            RemoteImage(
                url = backgroundUrl
                    ?: "$STOAT_FILES/backgrounds/${if (background is AutumnResource) background.id else null}/${if (background is AutumnResource) background.filename else background}",
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
                    if (!user.displayName.isNullOrBlank() && user.displayName!!.trim() != user.username?.trim()) {
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
                color = contentColour,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            pronouns?.let {
                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = it,
                    color = contentColour,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.Bottom)
                        .widthIn(max = 140.dp)
                )
            }
        }
    }
}

enum class WaveInclusionPolicy {
    Include,
    Exclude,
    OnlyTeam
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RawUserOverview2(
    user: User,
    profile: Profile? = null,
    pfpUrl: String? = null,
    backgroundUrl: String? = null,
    showWave: WaveInclusionPolicy = WaveInclusionPolicy.Include,
) {
    val context = LocalContext.current
    var teamMemberFlair by remember { mutableStateOf<Brush?>(null) }

    LaunchedEffect(user.id) {
        withContext(Dispatchers.IO) {
            user.id?.let {
                teamMemberFlair = SpecialUsers.teamFlairAsBrush(
                    context,
                    it
                )
            }
        }
    }

    Column {
        val background = backgroundUrl ?: profile?.background
        val contentColour = LocalContentColor.current
        val pronouns = user.pronouns?.trim()?.takeIf { it.isNotEmpty() }

        if (background != null) {
            RemoteImage(
                url = backgroundUrl
                    ?: "$STOAT_FILES/backgrounds/${if (background is AutumnResource) background.id else null}/${if (background is AutumnResource) background.filename else background}",
                description = null,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.large)
                    .height(128.dp)
                    .fillMaxWidth(),
                contentScale = ContentScale.FillWidth
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
                size = 64.dp,
                presence = presenceFromStatus(user.status?.presence, user.online ?: false),
                presenceSize = 24.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            val noDisplayName =
                user.displayName.isNullOrBlank() || user.displayName!!.trim() == user.username?.trim()
            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (noDisplayName) {
                    Text(
                        text = "${user.username}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = contentColour,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "@${user.username}#${user.discriminator}",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = contentColour,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = user.displayName ?: "",
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = contentColour,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "@${user.username}#${user.discriminator}",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = contentColour,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            pronouns?.let {
                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = it,
                    color = contentColour,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.Bottom)
                        .widthIn(max = 140.dp)
                )
            }
        }

        val discriminator = user.discriminator?.toIntOrNull()?.coerceIn(0, 9999) ?: 5000
        val wavelengthVariance = 12.5f
        val wavelengthOffset = discriminator / 9999f * (2 * wavelengthVariance) - wavelengthVariance

        if (showWave != WaveInclusionPolicy.Exclude && (showWave != WaveInclusionPolicy.OnlyTeam || teamMemberFlair != null)) {
            Wave(
                colour = teamMemberFlair
                    ?: Brush.solidColor(MaterialTheme.colorScheme.surfaceContainerHighest),
                stroke = WavyProgressIndicatorDefaults.linearIndicatorStroke,
                amplitude = 1f,
                wavelength = 40.dp + wavelengthOffset.dp,
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(12.dp)
            )
        }
    }
}
