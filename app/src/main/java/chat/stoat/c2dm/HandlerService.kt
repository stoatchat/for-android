package chat.stoat.c2dm

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.LocusIdCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import chat.stoat.BuildConfig
import chat.stoat.R
import chat.stoat.activities.MainActivity
import chat.stoat.api.internals.ULID
import chat.stoat.api.routes.channel.fetchSingleChannel
import chat.stoat.api.routes.push.subscribePush
import chat.stoat.c2dm.ChannelRegistrator.Companion.CHANNEL_ID_GROUP_CONVERSATIONS_MESSAGES
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.persistence.Database
import chat.stoat.persistence.SqlStorage
import com.bumptech.glide.Glide
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking
import logcat.LogPriority
import logcat.logcat

object NotificationID {
    const val NEW_MESSAGE = 0
}

class HandlerService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        runBlocking {
            subscribePush(auth = token)
        }
    }

    override fun onMessageReceived(fcmMessage: RemoteMessage) {
        val data = fcmMessage.data

        val type = data["type"]
        if (type != "push.message") {
            logcat(LogPriority.ERROR) { "Unknown message type: $type, abort" }
            return
        }

        val authorId = data["author"] ?: run {
            logcat(LogPriority.ERROR) { "No author in message, abort" }
            return
        }

        val body = data["body"] ?: run {
            logcat(LogPriority.ERROR) { "No body in message, abort" }
            return
        }

        val image = data["image"] ?: run {
            logcat(LogPriority.WARN) { "No image in message, abort" }
            return
        }

        val authorName = data["author_name"] ?: run {
            logcat(LogPriority.ERROR) { "No author name in message, abort" }
            return
        }

        val channelId = data["channel"] ?: run {
            logcat(LogPriority.ERROR) { "No channel in message, abort" }
            return
        }

        val messageId = data["message"] ?: run {
            logcat(LogPriority.ERROR) { "No message ID in message, abort" }
            return
        }

        val messageTimestamp = ULID.asTimestamp(messageId)

        val db = Database(SqlStorage.driver)

        fun serverPrefix(serverId: String?): String? {
            if (serverId == null) return null
            return db.serverQueries.findById(serverId).executeAsOneOrNull()?.name
        }

        fun formatChannelName(type: String, name: String?, serverId: String?): String {
            val base = when (type) {
                "DirectMessage" -> return authorName
                "TextChannel" -> "#${name}"
                else -> name ?: return authorName
            }
            val prefix = serverPrefix(serverId) ?: return base
            return "$prefix · $base"
        }

        val channelName = db.channelQueries.findById(channelId).executeAsOneOrNull()?.let {
            formatChannelName(it.channelType, it.name, it.server)
        } ?: runBlocking {
            runCatching { fetchSingleChannel(channelId) }.getOrNull()?.let {
                formatChannelName(
                    it.channelType?.value ?: "",
                    it.name,
                    it.server
                )
            } ?: authorName
        }

        val bitmap = Glide.with(this)
            .asBitmap()
            .load(image)
            .circleCrop()
            .submit()
            .get()

        val author = Person.Builder()
            .setBot(false)
            .setKey(authorId)
            .setIcon(IconCompat.createWithBitmap(bitmap))
            .setName(authorName)
            .build()

        val shortcutId = "${BuildConfig.APPLICATION_ID}.channel.$channelId"

        val conversationIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("channelId", channelId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val shortcut = ShortcutInfoCompat.Builder(this, shortcutId)
            .setShortLabel(channelName)
            .setLongLabel(channelName)
            .setIcon(IconCompat.createWithBitmap(bitmap))
            .setIntent(conversationIntent)
            .setLongLived(true)
            .setPerson(author)
            .build()

        ShortcutManagerCompat.pushDynamicShortcut(this, shortcut)

        val remoteInput = RemoteInput.Builder("content").run {
            setLabel(getString(R.string.message_context_sheet_actions_reply))
            build()
        }

        val replyIntent = Intent(this, ReplyReceiver::class.java).apply {
            putExtra("channelId", channelId)
        }

        val action: NotificationCompat.Action =
            NotificationCompat.Action.Builder(
                R.drawable.ic_reply_24dp,
                getString(R.string.message_context_sheet_actions_reply),
                PendingIntent.getBroadcast(
                    this,
                    channelId.hashCode(),
                    replyIntent,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
                .addRemoteInput(remoteInput)
                .build()

        val contentIntent = PendingIntent.getActivity(
            this,
            channelId.hashCode(),
            conversationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID_GROUP_CONVERSATIONS_MESSAGES)
            .setSmallIcon(R.drawable.ic_stoat_24dp)
            .setContentTitle(authorName)
            .setContentText(body)
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setStyle(
                NotificationCompat.MessagingStyle(author)
                    .setConversationTitle(channelName)
                    .addMessage(body, messageTimestamp, author)
            )
            .addAction(action)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Android 11 bubbles
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setShortcutId(shortcutId)
            builder.setLocusId(LocusIdCompat(shortcutId))

            val bubbleIntent = PendingIntent.getActivity(
                this,
                channelId.hashCode(),
                conversationIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val bubbleMetadata = NotificationCompat.BubbleMetadata.Builder(
                bubbleIntent,
                IconCompat.createWithBitmap(bitmap)
            )
                .setDesiredHeight(600)
                .setAutoExpandBubble(false)
                .setSuppressNotification(false)
                .build()

            builder.setBubbleMetadata(bubbleMetadata)
        }

        NotificationManagerCompat.from(this).apply {
            if (ActivityCompat.checkSelfPermission(
                    this@HandlerService,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(channelId, NotificationID.NEW_MESSAGE, builder.build())
        }
    }
}
