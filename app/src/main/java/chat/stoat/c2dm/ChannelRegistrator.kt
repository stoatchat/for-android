package chat.stoat.c2dm

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.ContextCompat.getSystemService
import chat.stoat.internals.extensions.getStringBlocking
import chat.stoat.shared.resources.Res
import chat.stoat.shared.resources.notification_channel_friend_requests
import chat.stoat.shared.resources.notification_channel_friend_requests_description
import chat.stoat.shared.resources.notification_channel_group_conversations
import chat.stoat.shared.resources.notification_channel_group_social
import chat.stoat.shared.resources.notification_channel_group_voice
import chat.stoat.shared.resources.notification_channel_messages
import chat.stoat.shared.resources.notification_channel_messages_description
import chat.stoat.shared.resources.notification_channel_ongoing_call
import chat.stoat.shared.resources.notification_channel_ongoing_call_description

// TODO
//  * Add the remaining groups.
//  * Add the remaining channels.
//  * Find out whether every conversation should have its own channel or if they should be grouped
//    together as one channel.

class ChannelRegistrator(val context: Context) {
    companion object {
        const val CHANNEL_ID_GROUP_CONVERSATIONS = "chat.stoat.c2dm.conversations"
        const val CHANNEL_ID_GROUP_CONVERSATIONS_MESSAGES =
            "chat.stoat.c2dm.conversations.messages"

        const val CHANNEL_ID_GROUP_SOCIAL = "chat.stoat.c2dm.social"
        const val CHANNEL_ID_GROUP_SOCIAL_FRIENDREQUESTS = "chat.stoat.c2dm.social.friendrequests"

        const val CHANNEL_ID_GROUP_VOICE = "chat.stoat.voice"
        const val CHANNEL_ID_GROUP_VOICE_ONGOING = "chat.stoat.voice.ongoing"
    }

    private val notificationManager =
        getSystemService(context, NotificationManager::class.java) as NotificationManager

    private fun registerGroups() {
        notificationManager.createNotificationChannelGroup(
            NotificationChannelGroup(
                CHANNEL_ID_GROUP_CONVERSATIONS,
                getStringBlocking(Res.string.notification_channel_group_conversations)
            )
        )
        notificationManager.createNotificationChannelGroup(
            NotificationChannelGroup(
                CHANNEL_ID_GROUP_SOCIAL,
                getStringBlocking(Res.string.notification_channel_group_social)
            )
        )
        notificationManager.createNotificationChannelGroup(
            NotificationChannelGroup(
                CHANNEL_ID_GROUP_VOICE,
                getStringBlocking(Res.string.notification_channel_group_voice)
            )
        )
    }

    private fun registerChannels() {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID_GROUP_SOCIAL_FRIENDREQUESTS,
                getStringBlocking(Res.string.notification_channel_friend_requests),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                group = CHANNEL_ID_GROUP_SOCIAL
                description =
                    getStringBlocking(Res.string.notification_channel_friend_requests_description)
            }
        )
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID_GROUP_CONVERSATIONS_MESSAGES,
                getStringBlocking(Res.string.notification_channel_messages),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                group = CHANNEL_ID_GROUP_CONVERSATIONS
                description =
                    getStringBlocking(Res.string.notification_channel_messages_description)
            }
        )
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID_GROUP_VOICE_ONGOING,
                getStringBlocking(Res.string.notification_channel_ongoing_call),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                group = CHANNEL_ID_GROUP_VOICE
                description =
                    getStringBlocking(Res.string.notification_channel_ongoing_call_description)
                setSound(null, null)
                enableVibration(false)
            }
        )
    }

    fun register() {
        registerGroups()
        registerChannels()
    }
}
