package chat.stoat.composables.chat

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
import androidx.compose.ui.unit.dp
import chat.stoat.R
import chat.stoat.core.model.schemas.UserBadges
import chat.stoat.core.model.schemas.has
import chat.stoat.shared.resources.Res
import chat.stoat.shared.resources.user_badge_active_supporter
import chat.stoat.shared.resources.user_badge_developer
import chat.stoat.shared.resources.user_badge_early_adopter
import chat.stoat.shared.resources.user_badge_founder
import chat.stoat.shared.resources.user_badge_paw
import chat.stoat.shared.resources.user_badge_platform_moderation
import chat.stoat.shared.resources.user_badge_reserved_relevant_joke_badge_1
import chat.stoat.shared.resources.user_badge_reserved_relevant_joke_badge_2
import chat.stoat.shared.resources.user_badge_responsible_disclosure
import chat.stoat.shared.resources.user_badge_supporter
import chat.stoat.shared.resources.user_badge_translator
import org.jetbrains.compose.resources.stringResource

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
                label = stringResource(Res.string.user_badge_developer),
                icon = painterResource(R.drawable.user_badge_developer)
            )
        }

        UserBadges.Translator.value -> {
            BadgeListEntryTemplate(
                label = stringResource(Res.string.user_badge_translator),
                icon = painterResource(R.drawable.user_badge_translator)
            )
        }

        UserBadges.Supporter.value -> {
            BadgeListEntryTemplate(
                label = stringResource(Res.string.user_badge_supporter),
                icon = painterResource(R.drawable.user_badge_supporter)
            )
        }

        UserBadges.ResponsibleDisclosure.value -> {
            BadgeListEntryTemplate(
                label = stringResource(Res.string.user_badge_responsible_disclosure),
                icon = painterResource(R.drawable.user_badge_disclosure)
            )
        }

        UserBadges.Founder.value -> {
            BadgeListEntryTemplate(
                label = stringResource(Res.string.user_badge_founder),
                icon = painterResource(R.drawable.user_badge_founder)
            )
        }

        UserBadges.PlatformModeration.value -> {
            BadgeListEntryTemplate(
                label = stringResource(Res.string.user_badge_platform_moderation),
                icon = painterResource(R.drawable.user_badge_moderation)
            )
        }

        UserBadges.ActiveSupporter.value -> {
            BadgeListEntryTemplate(
                label = stringResource(Res.string.user_badge_active_supporter),
                icon = painterResource(R.drawable.ic_emoji_people_24dp)
            )
        }

        UserBadges.Paw.value -> {
            BadgeListEntryTemplate(
                label = stringResource(Res.string.user_badge_paw),
                icon = painterResource(R.drawable.user_badge_paw)
            )
        }

        UserBadges.EarlyAdopter.value -> {
            BadgeListEntryTemplate(
                label = stringResource(Res.string.user_badge_early_adopter),
                icon = painterResource(R.drawable.user_badge_early_adopter)
            )
        }

        UserBadges.ReservedRelevantJokeBadge1.value -> {
            BadgeListEntryTemplate(
                label = stringResource(Res.string.user_badge_reserved_relevant_joke_badge_1),
                icon = painterResource(R.drawable.user_badge_reserved_relevant_one)
            )
        }

        UserBadges.ReservedRelevantJokeBadge2.value -> {
            BadgeListEntryTemplate(
                label = stringResource(Res.string.user_badge_reserved_relevant_joke_badge_2),
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
                        UserBadges.ActiveSupporter -> painterResource(R.drawable.ic_emoji_people_24dp)
                        UserBadges.Paw -> painterResource(R.drawable.user_badge_paw)
                        UserBadges.EarlyAdopter -> painterResource(R.drawable.user_badge_early_adopter)
                        UserBadges.ReservedRelevantJokeBadge1 -> painterResource(R.drawable.user_badge_reserved_relevant_one)
                        UserBadges.ReservedRelevantJokeBadge2 -> painterResource(R.drawable.user_badge_reserved_relevant_two)
                    },
                    contentDescription = when (badge) {
                        UserBadges.Developer -> stringResource(Res.string.user_badge_developer)
                        UserBadges.Translator -> stringResource(Res.string.user_badge_translator)
                        UserBadges.Supporter -> stringResource(Res.string.user_badge_supporter)
                        UserBadges.ResponsibleDisclosure -> stringResource(Res.string.user_badge_responsible_disclosure)
                        UserBadges.Founder -> stringResource(Res.string.user_badge_founder)
                        UserBadges.PlatformModeration -> stringResource(Res.string.user_badge_platform_moderation)
                        UserBadges.ActiveSupporter -> stringResource(Res.string.user_badge_active_supporter)
                        UserBadges.Paw -> stringResource(Res.string.user_badge_paw)
                        UserBadges.EarlyAdopter -> stringResource(Res.string.user_badge_early_adopter)
                        UserBadges.ReservedRelevantJokeBadge1 -> stringResource(Res.string.user_badge_reserved_relevant_joke_badge_1)
                        UserBadges.ReservedRelevantJokeBadge2 -> stringResource(Res.string.user_badge_reserved_relevant_joke_badge_2)
                    },
                    modifier = Modifier
                        .size(32.dp)
                )
            }
    }
}