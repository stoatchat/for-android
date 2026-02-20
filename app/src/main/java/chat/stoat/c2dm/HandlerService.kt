package chat.stoat.c2dm

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
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
import chat.stoat.api.STOAT_BASE
import chat.stoat.api.StoatJson
import chat.stoat.api.internals.ULID
import chat.stoat.api.routes.push.subscribePush
import chat.stoat.core.model.schemas.Message
import chat.stoat.core.model.schemas.User
import chat.stoat.c2dm.ChannelRegistrator.Companion.CHANNEL_ID_GROUP_CONVERSATIONS_MESSAGES
import chat.stoat.persistence.Database
import chat.stoat.persistence.SqlStorage
import com.bumptech.glide.Glide
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
        /// TEMPORARY CODE, SCHEMA TO BE REPLACED
        val payloadString = fcmMessage.data["payload"]
        if (payloadString == null) {
            Log.e("HandlerService", "No payload in message, abort")
            return
        }

        Log.d("HandlerService", payloadString)

        val payload = StoatJson.parseToJsonElement(payloadString).jsonObject
        val keys = payload.keys.toList().toString()
        Log.d("HandlerService", "following keys: $keys")

        var authorIcon = payload["icon"]?.jsonPrimitive?.contentOrNull
        val message = payload["message"]?.jsonObject?.let {
            StoatJson.decodeFromJsonElement(
                Message.serializer(),
                it
            )
        } ?: run {
            Log.e("HandlerService", "No message in payload, abort")
            return
        }

        val user = payload["message"]?.jsonObject?.get("user")?.jsonObject?.let {
            StoatJson.decodeFromJsonElement(
                User.serializer(),
                it
            )
        } ?: run {
            Log.e("HandlerService", "No message->user in payload, abort")
            return
        }

        if (authorIcon == null) {
            authorIcon =
                "$STOAT_BASE/users/${message.author?.ifBlank { "0".repeat(26) }}/default_avatar"
        }

        val db = Database(SqlStorage.driver)
        val channelName = message.channel?.let {
            db.channelQueries.findById(it).executeAsOneOrNull()
        }?.let {
            when (it.channelType) {
                "DirectMessage" -> {
                    user.displayName ?: user.username
                }

                "TextChannel" -> {
                    "#${it.name}"
                }

                else -> {
                    it.name ?: getString(R.string.unknown)
                }
            }
        } ?: getString(
            R.string.unknown
        )

        val messageTimestamp = message.id?.let { ULID.asTimestamp(it) } ?: run {
            Log.e("HandlerService", "No message id in message, abort")
            return
        }

        val bitmap = Glide.with(this)
            .asBitmap()
            .load(authorIcon)
            .circleCrop()
            .submit()
            .get()

        val author =
            Person.Builder()
                .setBot(user.bot != null)
                .setKey(message.author)
                .setIcon(IconCompat.createWithBitmap(bitmap))
                .setName(user.displayName ?: user.username)
                .build()

        if (message.channel == null) {
            Log.e("HandlerService", "No channel in message, abort")
            return
        }

        val shortcutId = "${BuildConfig.APPLICATION_ID}.channel.${message.channel}"

        val conversationIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("channelId", message.channel)
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

        val action: NotificationCompat.Action =
            NotificationCompat.Action.Builder(
                R.drawable.ic_reply_24dp,
                getString(R.string.message_context_sheet_actions_reply),
                PendingIntent.getActivity(
                    this,
                    0,
                    conversationIntent,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
                .addRemoteInput(remoteInput)
                .build()

        val contentIntent = PendingIntent.getActivity(
            this,
            message.channel.hashCode(),
            conversationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID_GROUP_CONVERSATIONS_MESSAGES)
            .setSmallIcon(R.drawable.ic_chat_24dp)
            .setContentTitle(user.displayName ?: user.username)
            .setContentText(message.content)
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setStyle(
                NotificationCompat.MessagingStyle(author)
                    .setConversationTitle(channelName)
                    .addMessage(
                        message.content ?: getString(R.string.reply_message_empty_has_attachments),
                        messageTimestamp,
                        author
                    )
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
                message.channel.hashCode(),
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
            notify(message.channel, NotificationID.NEW_MESSAGE, builder.build())
        }
        /// END TEMPORARY CODE
    }
}