package chat.zekochat.c2dm

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.ContextCompat.getSystemService
import chat.zekochat.R

// TODO
//  * Add the remaining groups.
//  * Add the remaining channels.
//  * Find out whether every conversation should have its own channel or if they should be grouped
//    together as one channel.

class ChannelRegistrator(val context: Context) {
    companion object {
        const val CHANNEL_ID_GROUP_CONVERSATIONS = "chat.peptide.c2dm.conversations"

        const val CHANNEL_ID_GROUP_SOCIAL = "chat.peptide.c2dm.social"
        const val CHANNEL_ID_GROUP_SOCIAL_FRIENDREQUESTS = "chat.peptide.c2dm.social.friendrequests"
    }

    private val notificationManager =
        getSystemService(context, NotificationManager::class.java) as NotificationManager

    private fun registerGroups() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return

        notificationManager.createNotificationChannelGroup(
            NotificationChannelGroup(
                CHANNEL_ID_GROUP_CONVERSATIONS,
                context.getString(R.string.notification_channel_group_conversations)
            )
        )
        notificationManager.createNotificationChannelGroup(
            NotificationChannelGroup(
                CHANNEL_ID_GROUP_SOCIAL,
                context.getString(R.string.notification_channel_group_social)
            )
        )
    }

    private fun registerChannels() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return

        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID_GROUP_SOCIAL_FRIENDREQUESTS,
                context.getString(R.string.notification_channel_friend_requests),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                group = CHANNEL_ID_GROUP_SOCIAL
                description =
                    context.getString(R.string.notification_channel_friend_requests_description)
            }
        )
    }

    fun register() {
        registerGroups()
        registerChannels()
    }
}
