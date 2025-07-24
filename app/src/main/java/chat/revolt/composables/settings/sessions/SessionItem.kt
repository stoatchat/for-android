package chat.revolt.composables.settings.sessions

import android.text.format.DateUtils
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import chat.revolt.R
import chat.revolt.api.internals.ULID
import chat.revolt.api.schemas.Session

@Composable
fun SessionItem(
    session: Session,
    modifier: Modifier = Modifier,
    currentSession: Boolean = false,
    onLogout: (Session) -> Unit
) {
    val decodedUlid by remember(session) { mutableLongStateOf(ULID.asTimestamp(session.id)) }
    val formattedTimestamp = remember(decodedUlid) {
        DateUtils.getRelativeTimeSpanString(
            decodedUlid,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        )
    }

    ListItem(
        headlineContent = {
            Text(
                text = session.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = stringResource(R.string.settings_sessions_first_seen, formattedTimestamp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            if (!currentSession) {
                IconButton(onClick = {
                    onLogout(session)
                }) {
                    Icon(
                        painter = painterResource(R.drawable.icn_logout_24dp),
                        contentDescription = stringResource(R.string.logout)
                    )
                }
            }
        },
        modifier = modifier
    )
}
