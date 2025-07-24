package chat.revolt.composables.chat

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
import chat.revolt.R
import chat.revolt.api.schemas.UserBadges
import chat.revolt.api.schemas.has

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
                icon = painterResource(R.drawable.user_badge_developer)
            )
        }

        UserBadges.Translator.value -> {
            BadgeListEntryTemplate(
                label = stringResource(R.string.user_badge_translator),
                icon = painterResource(R.drawable.user_badge_translator)
            )
        }

        UserBadges.Supporter.value -> {
            BadgeListEntryTemplate(
                label = stringResource(R.string.user_badge_supporter),
                icon = painterResource(R.drawable.user_badge_supporter)
            )
        }

        UserBadges.ResponsibleDisclosure.value -> {
            BadgeListEntryTemplate(
                label = stringResource(R.string.user_badge_responsible_disclosure),
                icon = painterResource(R.drawable.user_badge_disclosure)
            )
        }

        UserBadges.Founder.value -> {
            BadgeListEntryTemplate(
                label = stringResource(R.string.user_badge_founder),
                icon = painterResource(R.drawable.user_badge_founder)
            )
        }

        UserBadges.PlatformModeration.value -> {
            BadgeListEntryTemplate(
                label = stringResource(R.string.user_badge_platform_moderation),
                icon = painterResource(R.drawable.user_badge_moderation)
            )
        }

        UserBadges.ActiveSupporter.value -> {
            BadgeListEntryTemplate(
                label = stringResource(R.string.user_badge_active_supporter),
                icon = painterResource(R.drawable.icn_emoji_people_24dp)
            )
        }

        UserBadges.Paw.value -> {
            BadgeListEntryTemplate(
                label = stringResource(R.string.user_badge_paw),
                icon = painterResource(R.drawable.user_badge_paw)
            )
        }

        UserBadges.EarlyAdopter.value -> {
            BadgeListEntryTemplate(
                label = stringResource(R.string.user_badge_early_adopter),
                icon = painterResource(R.drawable.user_badge_early_adopter)
            )
        }

        UserBadges.ReservedRelevantJokeBadge1.value -> {
            BadgeListEntryTemplate(
                label = stringResource(R.string.user_badge_reserved_relevant_joke_badge_1),
                icon = painterResource(R.drawable.user_badge_reserved_relevant_one)
            )
        }

        UserBadges.ReservedRelevantJokeBadge2.value -> {
            BadgeListEntryTemplate(
                label = stringResource(R.string.user_badge_reserved_relevant_joke_badge_2),
                icon = painterResource(R.drawable.user_badge_reserved_relevant_two)
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
            .filter { badges has it }
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
            .filter { badges has it }
            .forEach { badge ->
                Image(
                    painter = when (badge) {
                        UserBadges.Developer -> painterResource(R.drawable.user_badge_developer)
                        UserBadges.Translator -> painterResource(R.drawable.user_badge_translator)
                        UserBadges.Supporter -> painterResource(R.drawable.user_badge_supporter)
                        UserBadges.ResponsibleDisclosure -> painterResource(R.drawable.user_badge_disclosure)
                        UserBadges.Founder -> painterResource(R.drawable.user_badge_founder)
                        UserBadges.PlatformModeration -> painterResource(R.drawable.user_badge_moderation)
                        UserBadges.ActiveSupporter -> painterResource(R.drawable.icn_emoji_people_24dp)
                        UserBadges.Paw -> painterResource(R.drawable.user_badge_paw)
                        UserBadges.EarlyAdopter -> painterResource(R.drawable.user_badge_early_adopter)
                        UserBadges.ReservedRelevantJokeBadge1 -> painterResource(R.drawable.user_badge_reserved_relevant_one)
                        UserBadges.ReservedRelevantJokeBadge2 -> painterResource(R.drawable.user_badge_reserved_relevant_two)
                    },
                    contentDescription = when (badge) {
                        UserBadges.Developer -> stringResource(R.string.user_badge_developer)
                        UserBadges.Translator -> stringResource(R.string.user_badge_translator)
                        UserBadges.Supporter -> stringResource(R.string.user_badge_supporter)
                        UserBadges.ResponsibleDisclosure -> stringResource(R.string.user_badge_responsible_disclosure)
                        UserBadges.Founder -> stringResource(R.string.user_badge_founder)
                        UserBadges.PlatformModeration -> stringResource(R.string.user_badge_platform_moderation)
                        UserBadges.ActiveSupporter -> stringResource(R.string.user_badge_active_supporter)
                        UserBadges.Paw -> stringResource(R.string.user_badge_paw)
                        UserBadges.EarlyAdopter -> stringResource(R.string.user_badge_early_adopter)
                        UserBadges.ReservedRelevantJokeBadge1 -> stringResource(R.string.user_badge_reserved_relevant_joke_badge_1)
                        UserBadges.ReservedRelevantJokeBadge2 -> stringResource(R.string.user_badge_reserved_relevant_joke_badge_2)
                    },
                    modifier = Modifier
                        .size(32.dp)
                )
            }
    }
}