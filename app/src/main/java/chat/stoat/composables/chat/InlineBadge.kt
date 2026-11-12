package chat.stoat.composables.chat

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import chat.stoat.core.designsystem.icons.StoatIcons
import chat.stoat.core.designsystem.icons.painter
import chat.stoat.shared.resources.Res
import chat.stoat.shared.resources.badge_bot_alt
import chat.stoat.shared.resources.badge_masquerade_alt
import chat.stoat.shared.resources.badge_team_member_alt
import chat.stoat.shared.resources.badge_webhook_alt
import org.jetbrains.compose.resources.stringResource

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
            painter = StoatIcons.BotBadge.painter(),
            contentDescription = stringResource(Res.string.badge_bot_alt),
            tint = colour,
            modifier = modifier
        )

        InlineBadge.Bridge -> Icon(
            painter = StoatIcons.BridgedBadge.painter(),
            contentDescription = stringResource(Res.string.badge_masquerade_alt),
            tint = colour,
            modifier = modifier
        )

        InlineBadge.PlatformModeration -> Icon(
            painter = StoatIcons.ModerationBadge.painter(),
            contentDescription = stringResource(Res.string.badge_bot_alt),
            tint = colour,
            modifier = modifier
        )

        InlineBadge.TeamMember -> Icon(
            painter = StoatIcons.TeamMemberBadge.painter(),
            contentDescription = stringResource(Res.string.badge_team_member_alt),
            tint = colour,
            modifier = modifier
        )

        InlineBadge.Webhook -> Icon(
            painter = StoatIcons.WebhookBadge.painter(),
            contentDescription = stringResource(Res.string.badge_webhook_alt),
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
