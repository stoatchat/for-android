package chat.revolt.composables.chat

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import chat.revolt.R

enum class InlineBadge {
    Bot,
    Bridge,
    PlatformModeration,
    TeamMember,
    Webhook
}

@Composable
fun InlineBadge(
    badge: InlineBadge,
    modifier: Modifier = Modifier,
    colour: Color = Color.Unspecified
) {
    when (badge) {
        InlineBadge.Bot -> Icon(
            painter = painterResource(id = R.drawable.icn_smart_toy_24dp),
            contentDescription = stringResource(id = R.string.badge_bot_alt),
            tint = colour,
            modifier = modifier
        )

        InlineBadge.Bridge -> Icon(
            painter = painterResource(id = R.drawable.icn_link_24dp),
            contentDescription = stringResource(id = R.string.badge_masquerade_alt),
            tint = colour,
            modifier = modifier
        )

        InlineBadge.PlatformModeration -> Icon(
            painter = painterResource(id = R.drawable.icn_brightness_alert_24dp),
            contentDescription = stringResource(id = R.string.badge_bot_alt),
            tint = colour,
            modifier = modifier
        )

        InlineBadge.TeamMember -> Icon(
            painter = painterResource(id = R.drawable.icn_hardware_24dp),
            contentDescription = stringResource(id = R.string.badge_team_member_alt),
            tint = colour,
            modifier = modifier
        )

        InlineBadge.Webhook -> Icon(
            painter = painterResource(id = R.drawable.icn_cloud_24dp),
            contentDescription = stringResource(id = R.string.badge_webhook_alt),
            tint = colour,
            modifier = modifier.padding(start = 4.dp)
        )
    }
}

@Composable
fun InlineBadges(
    modifier: Modifier = Modifier,
    bot: Boolean = false,
    bridge: Boolean = false,
    platformModeration: Boolean = false,
    teamMember: Boolean = false,
    webhook: Boolean = false,
    colour: Color = Color.Unspecified,
    precedingIfAny: @Composable () -> Unit = {},
    followingIfAny: @Composable () -> Unit = {}
) {
    val hasBadges = bot || bridge || platformModeration || teamMember

    if (hasBadges) {
        precedingIfAny()
    }

    Row {
        if (bot) {
            InlineBadge(
                badge = InlineBadge.Bot,
                modifier = modifier,
                colour = colour
            )
        }
        if (bridge) {
            InlineBadge(
                badge = InlineBadge.Bridge,
                modifier = modifier,
                colour = colour
            )
        }
        if (platformModeration) {
            InlineBadge(
                badge = InlineBadge.PlatformModeration,
                modifier = modifier,
                colour = colour
            )
        }
        if (teamMember) {
            InlineBadge(
                badge = InlineBadge.TeamMember,
                modifier = modifier,
                colour = colour
            )
        }
        if (webhook) {
            InlineBadge(
                badge = InlineBadge.Webhook,
                modifier = modifier,
                colour = colour
            )
        }
    }

    if (hasBadges) {
        followingIfAny()
    }
}
