package chat.stoat.composables.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.ULID
import chat.stoat.composables.markdown.prose.ChatMarkdown
import chat.stoat.core.model.schemas.Message
import kotlinx.datetime.Instant

enum class SystemMessageType(val type: String) {
    CHANNEL_OWNERSHIP_CHANGED("channel_ownership_changed"),
    CHANNEL_ICON_CHANGED("channel_icon_changed"),
    CHANNEL_DESCRIPTION_CHANGED("channel_description_changed"),
    CHANNEL_RENAMED("channel_renamed"),
    USER_REMOVE("user_remove"),
    USER_ADDED("user_added"),
    USER_BANNED("user_banned"),
    USER_KICKED("user_kicked"),
    USER_LEFT("user_left"),
    USER_JOINED("user_joined"),
    MESSAGE_PINNED("message_pinned"),
    MESSAGE_UNPINNED("message_unpinned"),
    CALL_STARTED("call_started"),
    TEXT("text")
}

fun String?.mention(): String {
    return "<@$this>"
}

@Composable
fun SystemMessage(
    message: Message,
    onClick: (() -> Unit)? = null,
) {
    if (message.system == null) return
    val serverId = StoatAPI.channelCache[message.channel]?.server

    val systemMessageType =
        SystemMessageType.entries.firstOrNull { it.type == message.system!!.type }

    if (systemMessageType == null) {
        UnsupportedMessage(context = message.system!!.type)
        return
    }

    CompositionLocalProvider(
        LocalContentColor provides LocalContentColor.current.copy(alpha = 0.7f),
        LocalTextStyle provides LocalTextStyle.current.copy(
            fontWeight = FontWeight.Light
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .fillMaxWidth()
        ) {
            SystemMessageIconWithBackground(type = systemMessageType)

            Spacer(modifier = Modifier.width(10.dp))

            when (systemMessageType) {
                SystemMessageType.CHANNEL_OWNERSHIP_CHANGED -> {
                    ChatMarkdown(
                        stringResource(
                            R.string.system_message_ownership_changed,
                            message.system!!.from.mention(),
                            message.system!!.to.mention()
                        ),
                        serverId = serverId
                    )
                }

                SystemMessageType.CHANNEL_ICON_CHANGED -> {
                    ChatMarkdown(
                        stringResource(
                            R.string.system_message_channel_icon_changed,
                            message.system!!.by.mention()
                        ),
                        serverId = serverId
                    )
                }

                SystemMessageType.CHANNEL_DESCRIPTION_CHANGED -> {
                    ChatMarkdown(
                        stringResource(
                            R.string.system_message_channel_description_changed,
                            message.system!!.by.mention()
                        ),
                        serverId = serverId
                    )
                }

                SystemMessageType.CHANNEL_RENAMED -> {
                    ChatMarkdown(
                        stringResource(
                            R.string.system_message_channel_renamed,
                            message.system!!.by.mention(),
                            "**${message.system!!.name ?: stringResource(R.string.unknown)}**"
                        ),
                        serverId = serverId
                    )
                }

                SystemMessageType.USER_REMOVE -> {
                    ChatMarkdown(
                        stringResource(
                            R.string.system_message_user_removed,
                            message.system!!.by.mention(),
                            message.system!!.id.mention()
                        ),
                        serverId = serverId
                    )
                }

                SystemMessageType.USER_ADDED -> {
                    ChatMarkdown(
                        stringResource(
                            R.string.system_message_user_added,
                            message.system!!.by.mention(),
                            message.system!!.id.mention()
                        ),
                        serverId = serverId
                    )
                }

                SystemMessageType.USER_BANNED -> {
                    ChatMarkdown(
                        stringResource(
                            R.string.system_message_user_banned,
                            message.system!!.id.mention()
                        ),
                        serverId = serverId
                    )
                }

                SystemMessageType.USER_KICKED -> {
                    ChatMarkdown(
                        stringResource(
                            R.string.system_message_user_kicked,
                            message.system!!.id.mention()
                        ),
                        serverId = serverId
                    )
                }

                SystemMessageType.USER_LEFT -> {
                    ChatMarkdown(
                        stringResource(
                            R.string.system_message_user_left,
                            message.system!!.id.mention()
                        ),
                        serverId = serverId
                    )
                }

                SystemMessageType.USER_JOINED -> {
                    ChatMarkdown(
                        stringResource(
                            R.string.system_message_user_joined,
                            message.system!!.id.mention()
                        ),
                        serverId = serverId
                    )
                }

                SystemMessageType.MESSAGE_PINNED -> {
                    ChatMarkdown(
                        stringResource(
                            R.string.system_message_message_pinned,
                            message.system!!.by.mention()
                        ),
                        serverId = serverId
                    )
                }

                SystemMessageType.MESSAGE_UNPINNED -> {
                    ChatMarkdown(
                        stringResource(
                            R.string.system_message_message_unpinned,
                            message.system!!.by.mention()
                        ),
                        serverId = serverId
                    )
                }

                SystemMessageType.CALL_STARTED -> {
                    val callDuration = callDurationMillis(
                        messageId = message.id,
                        finishedAt = message.system!!.finishedAt
                    )
                    ChatMarkdown(
                        if (callDuration != null) {
                            stringResource(
                                R.string.system_message_call_started_with_finished_at,
                                message.system!!.by.mention(),
                                formatCallDuration(callDuration)
                            )
                        } else {
                            stringResource(
                                R.string.system_message_call_started,
                                message.system!!.by.mention()
                            )
                        },
                        serverId = serverId
                    )
                }

                SystemMessageType.TEXT -> {
                    message.system!!.content?.let { ChatMarkdown(it) }
                }
            }
        }
    }
}

internal fun callDurationMillis(messageId: String?, finishedAt: String?): Long? {
    if (messageId == null || finishedAt == null) return null

    return runCatching {
        val duration = Math.subtractExact(
            Instant.parse(finishedAt).toEpochMilliseconds(),
            ULID.asTimestamp(messageId)
        )
        duration.takeIf { it >= 0 }
    }.getOrNull()
}

internal enum class CallDurationUnit {
    FEW_SECONDS,
    MINUTES,
    HOURS,
    DAYS,
    MONTHS,
    YEARS,
}

internal data class CallDuration(
    val unit: CallDurationUnit,
    val quantity: Int = 0,
)

internal fun humanizeCallDuration(durationMillis: Long): CallDuration {
    val seconds = durationMillis.coerceAtLeast(0) / MILLIS_PER_SECOND
    return when {
        seconds < SECONDS_PER_MINUTE -> CallDuration(CallDurationUnit.FEW_SECONDS)
        seconds < SECONDS_PER_HOUR -> CallDuration(
            CallDurationUnit.MINUTES,
            (seconds / SECONDS_PER_MINUTE).asQuantity()
        )

        seconds < SECONDS_PER_DAY -> CallDuration(
            CallDurationUnit.HOURS,
            (seconds / SECONDS_PER_HOUR).asQuantity()
        )

        seconds < SECONDS_PER_MONTH -> CallDuration(
            CallDurationUnit.DAYS,
            (seconds / SECONDS_PER_DAY).asQuantity()
        )

        seconds < SECONDS_PER_YEAR -> CallDuration(
            CallDurationUnit.MONTHS,
            (seconds / SECONDS_PER_MONTH).asQuantity()
        )

        else -> CallDuration(
            CallDurationUnit.YEARS,
            (seconds / SECONDS_PER_YEAR).asQuantity()
        )
    }
}

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 60 * SECONDS_PER_MINUTE
private const val SECONDS_PER_DAY = 24 * SECONDS_PER_HOUR
private const val SECONDS_PER_MONTH = 30 * SECONDS_PER_DAY
private const val SECONDS_PER_YEAR = 365 * SECONDS_PER_DAY

private fun Long.asQuantity(): Int = coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

@Composable
private fun formatCallDuration(durationMillis: Long): String {
    val duration = humanizeCallDuration(durationMillis)
    return when (duration.unit) {
        CallDurationUnit.FEW_SECONDS -> stringResource(
            R.string.system_message_call_duration_few_seconds
        )

        CallDurationUnit.MINUTES -> pluralStringResource(
            R.plurals.system_message_call_duration_minutes,
            duration.quantity,
            duration.quantity
        )

        CallDurationUnit.HOURS -> pluralStringResource(
            R.plurals.system_message_call_duration_hours,
            duration.quantity,
            duration.quantity
        )

        CallDurationUnit.DAYS -> pluralStringResource(
            R.plurals.system_message_call_duration_days,
            duration.quantity,
            duration.quantity
        )

        CallDurationUnit.MONTHS -> pluralStringResource(
            R.plurals.system_message_call_duration_months,
            duration.quantity,
            duration.quantity
        )

        CallDurationUnit.YEARS -> pluralStringResource(
            R.plurals.system_message_call_duration_years,
            duration.quantity,
            duration.quantity
        )
    }
}

@Composable
fun SystemMessageIcon(type: SystemMessageType, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    when (type) {
        SystemMessageType.CHANNEL_OWNERSHIP_CHANGED -> {
            Icon(
                painter = painterResource(R.drawable.ic_key_24dp),
                contentDescription = stringResource(R.string.system_message_ownership_changed_alt),
                tint = LocalContentColor.current,
                modifier = modifier.size(size)
            )
        }

        SystemMessageType.CHANNEL_ICON_CHANGED -> {
            Icon(
                painter = painterResource(R.drawable.ic_landscape_2_edit_24dp),
                contentDescription = stringResource(
                    R.string.system_message_channel_icon_changed_alt
                ),
                tint = LocalContentColor.current,
                modifier = modifier.size(size)
            )
        }

        SystemMessageType.CHANNEL_DESCRIPTION_CHANGED -> {
            Icon(
                painter = painterResource(R.drawable.ic_contract_edit_24dp),
                contentDescription = stringResource(
                    R.string.system_message_channel_description_changed_alt
                ),
                tint = LocalContentColor.current,
                modifier = modifier.size(size)
            )
        }

        SystemMessageType.CHANNEL_RENAMED -> {
            Icon(
                painter = painterResource(R.drawable.ic_ink_highlighter_move_24dp),
                contentDescription = stringResource(R.string.system_message_channel_renamed_alt),
                tint = LocalContentColor.current,
                modifier = modifier.size(size)
            )
        }

        SystemMessageType.USER_REMOVE -> {
            Icon(
                painter = painterResource(R.drawable.ic_group_remove_24dp),
                contentDescription = stringResource(R.string.system_message_user_removed_alt),
                tint = LocalContentColor.current,
                modifier = modifier.size(size)
            )
        }

        SystemMessageType.USER_ADDED -> {
            Icon(
                painter = painterResource(R.drawable.ic_group_add_24dp),
                contentDescription = stringResource(R.string.system_message_user_added_alt),
                tint = LocalContentColor.current,
                modifier = modifier.size(size)
            )
        }

        SystemMessageType.USER_BANNED -> {
            Icon(
                painter = painterResource(R.drawable.ic_gavel_24dp),
                contentDescription = stringResource(R.string.system_message_user_banned_alt),
                tint = LocalContentColor.current,
                modifier = modifier.size(size)
            )
        }

        SystemMessageType.USER_KICKED -> {
            Icon(
                painter = painterResource(R.drawable.ic_sports_and_outdoors_24dp),
                contentDescription = stringResource(R.string.system_message_user_kicked_alt),
                tint = LocalContentColor.current,
                modifier = modifier.size(size)
            )
        }

        SystemMessageType.USER_LEFT -> {
            Icon(
                painter = painterResource(R.drawable.ic_door_open_24dp),
                contentDescription = stringResource(R.string.system_message_user_left_alt),
                tint = LocalContentColor.current,
                modifier = modifier.size(size)
            )
        }

        SystemMessageType.USER_JOINED -> {
            Icon(
                painter = painterResource(R.drawable.ic_waving_hand_24dp),
                contentDescription = stringResource(R.string.system_message_user_joined_alt),
                tint = LocalContentColor.current,
                modifier = modifier.size(size),
            )
        }

        SystemMessageType.MESSAGE_PINNED -> {
            Icon(
                painter = painterResource(R.drawable.ic_keep_24dp),
                contentDescription = stringResource(R.string.system_message_message_pinned_alt),
                tint = LocalContentColor.current,
                modifier = modifier.size(size)
            )
        }

        SystemMessageType.MESSAGE_UNPINNED -> {
            Icon(
                painter = painterResource(R.drawable.ic_keep_off_24dp),
                contentDescription = stringResource(R.string.system_message_message_unpinned_alt),
                tint = LocalContentColor.current,
                modifier = modifier.size(size)
            )
        }

        SystemMessageType.CALL_STARTED -> {
            Icon(
                painter = painterResource(R.drawable.ic_call_24dp__fill),
                contentDescription = stringResource(R.string.system_message_call_started_alt),
                tint = LocalContentColor.current,
                modifier = modifier.size(size)
            )
        }

        SystemMessageType.TEXT -> {
            Icon(
                painter = painterResource(R.drawable.ic_info_24dp),
                contentDescription = stringResource(R.string.system_message_text_alt),
                tint = LocalContentColor.current,
                modifier = modifier.size(size)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun shapeForType(type: SystemMessageType): Shape {
    return when (type) {
        SystemMessageType.CHANNEL_OWNERSHIP_CHANGED -> MaterialShapes.Slanted
        SystemMessageType.CHANNEL_ICON_CHANGED -> MaterialShapes.Pentagon
        SystemMessageType.CHANNEL_DESCRIPTION_CHANGED -> MaterialShapes.Gem
        SystemMessageType.CHANNEL_RENAMED -> MaterialShapes.Pill
        SystemMessageType.USER_REMOVE -> MaterialShapes.Flower
        SystemMessageType.USER_ADDED -> MaterialShapes.Sunny
        SystemMessageType.USER_BANNED -> MaterialShapes.Burst
        SystemMessageType.USER_KICKED -> MaterialShapes.SoftBurst
        SystemMessageType.USER_LEFT -> MaterialShapes.Cookie4Sided
        SystemMessageType.USER_JOINED -> MaterialShapes.Cookie9Sided
        SystemMessageType.MESSAGE_PINNED -> MaterialShapes.Clover4Leaf
        SystemMessageType.MESSAGE_UNPINNED -> MaterialShapes.Clover8Leaf
        SystemMessageType.CALL_STARTED -> MaterialShapes.Fan
        SystemMessageType.TEXT -> MaterialShapes.Square
    }.toShape()
}

// TODO - find the best colours for each type
@Composable
private fun backgroundColourForType(type: SystemMessageType): Color {
    return MaterialTheme.colorScheme.primaryContainer
}

@Composable
private fun contentColourForType(type: SystemMessageType): Color {
    return MaterialTheme.colorScheme.onPrimaryContainer
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SystemMessageIconWithBackground(
    type: SystemMessageType,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shapeForType(type))
            .background(
                color = backgroundColourForType(type),
            )
            .size(size)
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColourForType(type),
        ) {
            SystemMessageIcon(type = type)
        }
    }
}
