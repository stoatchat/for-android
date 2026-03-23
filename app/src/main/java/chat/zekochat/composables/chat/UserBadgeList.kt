package chat.zekochat.composables.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import chat.zekochat.R
import chat.zekochat.api.schemas.UserBadges
import chat.zekochat.api.schemas.has

@Composable
fun BadgeListEntryTemplate(
    label: String,
    icon: Painter
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = icon,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = label
        )
    }
}

@Composable
fun BadgeListEntry(badge: UserBadges) {
    when (badge.value) {
        UserBadges.Developer.value -> {
            BadgeListEntryTemplate(
                label = stringResource(R.string.user_badge_developer),
                icon = painterResource(R.drawable.badge_developer)
            )
        }

        UserBadges.Translator.value -> {
            BadgeListEntryTemplate(
                label = stringResource(R.string.user_badge_first_100_members),
                icon = painterResource(R.drawable.badge_first_100_members)
            )
        }

        UserBadges.Supporter.value -> {
            BadgeListEntryTemplate(
                label = stringResource(R.string.user_badge_supporter),
                icon = painterResource(R.drawable.badge_supporter)
            )
        }

        UserBadges.ResponsibleDisclosure.value -> {
            BadgeListEntryTemplate(
                label = stringResource(R.string.user_badge_trusted_seller),
                icon = painterResource(R.drawable.badge_trusted_seller)
            )
        }

        UserBadges.Founder.value -> {
            BadgeListEntryTemplate(
                label = stringResource(R.string.user_badge_founder),
                icon = painterResource(R.drawable.badge_founder)
            )
        }

        UserBadges.PlatformModeration.value -> {
            BadgeListEntryTemplate(
                label = stringResource(R.string.user_badge_administrator),
                icon = painterResource(R.drawable.badge_administrator)
            )
        }

        UserBadges.Paw.value -> {
            BadgeListEntryTemplate(
                label = stringResource(R.string.user_badge_clown),
                icon = painterResource(R.drawable.badge_clown)
            )
        }

        UserBadges.EarlyAdopter.value -> {
            BadgeListEntryTemplate(
                label = stringResource(R.string.user_badge_top_contributor),
                icon = painterResource(R.drawable.badge_top_contributor)
            )
        }

        UserBadges.ReservedRelevantJokeBadge1.value -> {
            BadgeListEntryTemplate(
                label = stringResource(R.string.user_badge_karen),
                icon = painterResource(R.drawable.badge_karen)
            )
        }

        UserBadges.ReservedRelevantJokeBadge2.value -> {
            BadgeListEntryTemplate(
                label = stringResource(R.string.user_badge_gump),
                icon = painterResource(R.drawable.badge_gump)
            )
        }
    }
}

@Composable
fun UserBadgeList(badges: Long) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        UserBadges.entries
            .filter { it != UserBadges.ActiveSupporter && (badges has it) }
            .forEach { badge ->
                BadgeListEntry(badge)
            }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserBadgeRow(badges: Long) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        UserBadges.entries
            .filter { it != UserBadges.ActiveSupporter && (badges has it) }
            .forEach { badge ->
                Image(
                    painter = when (badge) {
                        UserBadges.Developer -> painterResource(R.drawable.badge_developer)
                        UserBadges.Translator -> painterResource(R.drawable.badge_first_100_members)
                        UserBadges.Supporter -> painterResource(R.drawable.badge_supporter)
                        UserBadges.ResponsibleDisclosure -> painterResource(R.drawable.badge_trusted_seller)
                        UserBadges.Founder -> painterResource(R.drawable.badge_founder)
                        UserBadges.PlatformModeration -> painterResource(R.drawable.badge_administrator)
                        UserBadges.ActiveSupporter -> painterResource(R.drawable.icn_emoji_people_24dp)
                        UserBadges.Paw -> painterResource(R.drawable.badge_clown)
                        UserBadges.EarlyAdopter -> painterResource(R.drawable.badge_top_contributor)
                        UserBadges.ReservedRelevantJokeBadge1 -> painterResource(R.drawable.badge_karen)
                        UserBadges.ReservedRelevantJokeBadge2 -> painterResource(R.drawable.badge_gump)
                    },
                    contentDescription = when (badge) {
                        UserBadges.Developer -> stringResource(R.string.user_badge_developer)
                        UserBadges.Translator -> stringResource(R.string.user_badge_first_100_members)
                        UserBadges.Supporter -> stringResource(R.string.user_badge_supporter)
                        UserBadges.ResponsibleDisclosure -> stringResource(R.string.user_badge_trusted_seller)
                        UserBadges.Founder -> stringResource(R.string.user_badge_founder)
                        UserBadges.PlatformModeration -> stringResource(R.string.user_badge_administrator)
                        UserBadges.ActiveSupporter -> stringResource(R.string.user_badge_active_supporter)
                        UserBadges.Paw -> stringResource(R.string.user_badge_clown)
                        UserBadges.EarlyAdopter -> stringResource(R.string.user_badge_top_contributor)
                        UserBadges.ReservedRelevantJokeBadge1 -> stringResource(R.string.user_badge_karen)
                        UserBadges.ReservedRelevantJokeBadge2 -> stringResource(R.string.user_badge_gump)
                    },
                    modifier = Modifier
                        .size(32.dp)
                )
            }
    }
}