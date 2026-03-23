package chat.zekochat.composables.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chat.zekochat.R
import chat.zekochat.api.schemas.UserBadges

/**
 * Compact "Verified Vendor" indicator shown next to usernames.
 *
 * Web mapping notes:
 * - ResponsibleDisclosure (8) and ReservedRelevantJokeBadge1 (512) both trigger the vendor badge.
 */
fun hasVerifiedVendor(badges: Long?): Boolean {
    val b = badges ?: 0L
    return (b and UserBadges.ResponsibleDisclosure.value) != 0L ||
        (b and UserBadges.ReservedRelevantJokeBadge1.value) != 0L
}

@Composable
fun UserVendorBadge(
    badges: Long?,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
) {
    if (!hasVerifiedVendor(badges)) return

    Image(
        painter = painterResource(R.drawable.badge_verified_vendor),
        contentDescription = stringResource(R.string.user_badge_verified_vendor),
        modifier = modifier.then(Modifier.size(size)),
    )
}

