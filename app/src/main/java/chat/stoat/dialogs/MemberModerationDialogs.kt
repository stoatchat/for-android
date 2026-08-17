package chat.stoat.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.internals.Roles
import chat.stoat.api.internals.hasPermission
import chat.stoat.api.routes.server.banMember
import chat.stoat.api.routes.server.kickMember
import chat.stoat.api.routes.server.setMemberTimeout
import chat.stoat.core.model.schemas.User
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal sealed interface MemberModerationAction {
    data object Timeout : MemberModerationAction
    data object Kick : MemberModerationAction
    data object Ban : MemberModerationAction
}

private data class DurationOption(
    val seconds: Long,
)

@Composable
private fun DurationOption.label(): String = when {
    seconds == 0L -> stringResource(R.string.member_moderation_none)
    seconds % 1.days.inWholeSeconds == 0L -> {
        val days = (seconds / 1.days.inWholeSeconds).toInt()
        pluralStringResource(R.plurals.duration_days, days, days)
    }

    seconds % 1.hours.inWholeSeconds == 0L -> {
        val hours = (seconds / 1.hours.inWholeSeconds).toInt()
        pluralStringResource(R.plurals.duration_hours, hours, hours)
    }

    else -> {
        val minutes = (seconds / 1.minutes.inWholeSeconds).toInt()
        pluralStringResource(R.plurals.duration_minutes, minutes, minutes)
    }
}

internal data class MemberModerationPermissions(
    val canTimeout: Boolean,
    val canKick: Boolean,
    val canBan: Boolean,
) {
    val any get() = canTimeout || canKick || canBan
}

private fun memberRanking(serverId: String, userId: String): Double {
    val server = StoatAPI.serverCache[serverId] ?: return Double.NEGATIVE_INFINITY
    if (server.owner == userId) return Double.NEGATIVE_INFINITY
    return Roles.resolveHighestRole(serverId, userId)?.rank ?: Double.POSITIVE_INFINITY
}

internal fun memberModerationPermissions(
    serverId: String,
    targetUserId: String?,
): MemberModerationPermissions {
    val server = StoatAPI.serverCache[serverId]
        ?: return MemberModerationPermissions(canTimeout = false, canKick = false, canBan = false)
    val selfId = StoatAPI.selfId
        ?: return MemberModerationPermissions(canTimeout = false, canKick = false, canBan = false)
    if (targetUserId == null || StoatAPI.members.getMember(serverId, targetUserId) == null) {
        return MemberModerationPermissions(canTimeout = false, canKick = false, canBan = false)
    }
    val selfMember = StoatAPI.members.getMember(serverId, selfId)
        ?: return MemberModerationPermissions(canTimeout = false, canKick = false, canBan = false)

    if (selfId == targetUserId || memberRanking(serverId, selfId) >= memberRanking(
            serverId,
            targetUserId
        )
    ) {
        return MemberModerationPermissions(canTimeout = false, canKick = false, canBan = false)
    }

    val permissions = Roles.permissionFor(server, selfMember)
    return MemberModerationPermissions(
        canTimeout = permissions.hasPermission(PermissionBit.TimeoutMembers),
        canKick = permissions.hasPermission(PermissionBit.KickMembers),
        canBan = permissions.hasPermission(PermissionBit.BanMembers),
    )
}

@Composable
internal fun MemberModerationDialog(
    action: MemberModerationAction,
    serverId: String,
    user: User,
    dismissUserSheet: suspend () -> Unit,
    onDismiss: () -> Unit,
) {
    val userId = user.id ?: return

    val username = user.displayName ?: user.username ?: stringResource(R.string.unknown)
    when (action) {
        MemberModerationAction.Timeout -> TimeoutMemberDialog(
            serverId = serverId,
            userId = userId,
            username = username,
            isTimedOut = StoatAPI.members.getMember(serverId, userId)
                ?.timeoutTimestamp()
                ?.let { it > Clock.System.now() } == true,
            onDismiss = onDismiss,
        )

        MemberModerationAction.Kick -> KickMemberDialog(
            serverId = serverId,
            userId = userId,
            username = username,
            onDismiss = onDismiss,
            onKicked = dismissUserSheet,
        )

        MemberModerationAction.Ban -> BanMemberDialog(
            serverId = serverId,
            userId = userId,
            username = username,
            onDismiss = onDismiss,
            onBanned = dismissUserSheet,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationSelector(
    title: String,
    options: List<DurationOption>,
    selectedSeconds: Long,
    onSelected: (Long) -> Unit,
    enabled: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = options.first { it.seconds == selectedSeconds }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
    ) {
        OutlinedTextField(
            value = selectedOption.label(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(title) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label()) },
                    onClick = {
                        onSelected(option.seconds)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DialogConfirmButton(
    label: String,
    submitting: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = !submitting,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onError,
            containerColor = MaterialTheme.colorScheme.error
        ),
    ) {
        if (submitting) {
            LoadingIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            Text(label)
        }
    }
}

@Composable
private fun KickMemberDialog(
    serverId: String,
    userId: String,
    username: String,
    onDismiss: () -> Unit,
    onKicked: suspend () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text(stringResource(R.string.member_moderation_kick_member)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.member_moderation_kick_description, username))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            DialogConfirmButton(
                label = stringResource(R.string.member_moderation_kick),
                submitting = submitting,
                onClick = {
                    scope.launch {
                        submitting = true
                        error = null
                        runCatching { kickMember(serverId, userId) }
                            .onSuccess { onKicked() }
                            .onFailure { error = it.message }
                        submitting = false
                    }
                },
            )
        },
    )
}

@Composable
private fun BanMemberDialog(
    serverId: String,
    userId: String,
    username: String,
    onDismiss: () -> Unit,
    onBanned: suspend () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val options = listOf(
        DurationOption(0),
        DurationOption(1.hours.inWholeSeconds),
        DurationOption(6.hours.inWholeSeconds),
        DurationOption(1.days.inWholeSeconds),
        DurationOption(3.days.inWholeSeconds),
        DurationOption(7.days.inWholeSeconds),
    )
    var reason by remember { mutableStateOf("") }
    var deleteSeconds by remember { mutableLongStateOf(0L) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text(stringResource(R.string.member_moderation_ban_member)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.member_moderation_ban_description, username))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it.take(1024) },
                    enabled = !submitting,
                    label = { Text(stringResource(R.string.member_moderation_reason)) },
                    supportingText = { Text("${reason.length}/1024") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                DurationSelector(
                    title = stringResource(R.string.member_moderation_delete_history),
                    options = options,
                    selectedSeconds = deleteSeconds,
                    onSelected = { deleteSeconds = it },
                    enabled = !submitting,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            DialogConfirmButton(
                label = stringResource(R.string.member_moderation_ban),
                submitting = submitting,
                onClick = {
                    scope.launch {
                        submitting = true
                        error = null
                        runCatching { banMember(serverId, userId, reason, deleteSeconds) }
                            .onSuccess { onBanned() }
                            .onFailure { error = it.message }
                        submitting = false
                    }
                },
            )
        },
    )
}

@Composable
private fun TimeoutMemberDialog(
    serverId: String,
    userId: String,
    username: String,
    isTimedOut: Boolean,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val options = listOf(
        DurationOption(5.minutes.inWholeSeconds),
        DurationOption(15.minutes.inWholeSeconds),
        DurationOption(1.hours.inWholeSeconds),
        DurationOption(6.hours.inWholeSeconds),
        DurationOption(1.days.inWholeSeconds),
        DurationOption(7.days.inWholeSeconds),
    )
    var durationSeconds by remember { mutableLongStateOf(1.hours.inWholeSeconds) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text(stringResource(R.string.member_moderation_timeout_member)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.member_moderation_timeout_description, username))
                DurationSelector(
                    title = stringResource(R.string.member_moderation_timeout_length),
                    options = options,
                    selectedSeconds = durationSeconds,
                    onSelected = { durationSeconds = it },
                    enabled = !submitting,
                )
                if (isTimedOut) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                submitting = true
                                error = null
                                runCatching { setMemberTimeout(serverId, userId, null) }
                                    .onSuccess { onDismiss() }
                                    .onFailure { error = it.message }
                                submitting = false
                            }
                        },
                        enabled = !submitting,
                    ) {
                        Text(stringResource(R.string.member_moderation_remove_timeout))
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            DialogConfirmButton(
                label = stringResource(R.string.member_moderation_timeout),
                submitting = submitting,
                onClick = {
                    scope.launch {
                        submitting = true
                        error = null
                        val timeout = Clock.System.now().plus(durationSeconds.seconds).toString()
                        runCatching { setMemberTimeout(serverId, userId, timeout) }
                            .onSuccess { onDismiss() }
                            .onFailure { error = it.message }
                        submitting = false
                    }
                },
            )
        },
    )
}
