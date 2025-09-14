package chat.zekochat.composables.screens.chat.discover

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.zekochat.R
import chat.zekochat.api.PeptideAPI
import chat.zekochat.api.PeptideError
import chat.zekochat.api.schemas.Invite
import chat.zekochat.composables.generic.IconPlaceholder
import chat.zekochat.composables.generic.PepTextButton
import chat.zekochat.composables.generic.RemoteImage
import chat.zekochat.composables.generic.SquareButton

@Composable
fun ServerInviteDialog(
    isLoading: Boolean,
    invite: Invite?,
    error: PeptideError?,
    onJoinClick: () -> Unit,
    onDismiss: () -> Unit
) {

    if (error != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Image(
                    painter = painterResource(R.drawable.link_no_valid_img),
                    contentDescription = null,
                )
            },
            title = {
                Text(
                    text = stringResource(id = R.string.invite_error_header),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column {
                    Text(
                        text = when (error.type) {
                            "NotFound" -> stringResource(id = R.string.invite_error_invalid_invite)
                            "Banned" -> stringResource(id = R.string.invite_error_banned)
                            else -> stringResource(id = R.string.invite_error_unknown)
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(32.dp))
                    SquareButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(id = R.string.spark_early_access_cta))
                    }
                }
            },
            confirmButton = {}
        )
        return
    }

    if (invite == null) {
        // This shouldn't happen, but handle it just in case
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom // Align buttons to the bottom
            ) {
                SquareButton(
                    onClick = onJoinClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(id = R.string.joining))
                        }
                    } else {
                        Text(text = stringResource(id = R.string.invite_join))
                    }
                }
                PepTextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.invite_cancel))
                }
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (invite.serverIcon != null) {
                    RemoteImage(
                        url = "${PeptideAPI.getCurrentFilesUrl()}/icons/${invite.serverIcon.id}/${invite.serverIcon.filename}",
                        allowAnimation = false,
                        description = invite.serverName ?: stringResource(id = R.string.unknown),
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                    )
                } else {
                    IconPlaceholder(
                        name = invite.serverName ?: stringResource(id = R.string.unknown),
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.invite_message),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = invite.serverName ?: stringResource(id = R.string.unknown),
                    textAlign = TextAlign.Center,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                // Invited by section
                if (invite.userName != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = MaterialTheme.shapes.large
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Invited by")
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (invite.userAvatar != null) {
                                RemoteImage(
                                    url = "${PeptideAPI.getCurrentFilesUrl()}/avatars/${invite.userAvatar.id}/${invite.userAvatar.filename}",
                                    description = invite.userName,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                IconPlaceholder(
                                    name = invite.userName, modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape), fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = invite.userName, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (invite.memberCount != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.three_person),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.6f
                                )
                            )
                        )
                        Text(
                            text = stringResource(
                                id = R.string.members_count,
                                invite.memberCount
                            ),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.6f
                            )
                        )
                    }
                }
            }
        },
    )
}


@Preview
@Composable
fun ServerInviteDialogPreviewError() {
    ServerInviteDialog(
        isLoading = false,
        invite = null,
        error = PeptideError(type = "NotFound"),
        onJoinClick = {},
        onDismiss = {}
    )
}

@Preview
@Composable
fun ServerInviteDialogPreviewSuccess() {
    val invite = Invite(
        serverName = "Peptide Test Server",
        serverIcon = null, // Replace with a real AutumnResource if needed for preview
        memberCount = 123,
        userName = "TestUser",
        userAvatar = null // Replace with a real AutumnResource if needed for preview
    )
    ServerInviteDialog(
        isLoading = false,
        invite = invite,
        error = null,
        onJoinClick = {},
        onDismiss = {}
    )
}

