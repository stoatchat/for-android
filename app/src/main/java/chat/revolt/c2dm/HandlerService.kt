package chat.revolt.c2dm

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import chat.revolt.R
import chat.revolt.activities.MainActivity
import chat.revolt.api.REVOLT_BASE
import chat.revolt.api.RevoltJson
import chat.revolt.api.internals.ULID
import chat.revolt.api.routes.push.subscribePush
import chat.revolt.api.schemas.Message
import chat.revolt.api.schemas.User
import chat.revolt.c2dm.ChannelRegistrator.Companion.CHANNEL_ID_GROUP_SOCIAL_FRIENDREQUESTS
import chat.revolt.persistence.Database
import chat.revolt.persistence.SqlStorage
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

        val payload = RevoltJson.parseToJsonElement(payloadString).jsonObject
        val keys = payload.keys.toList().toString()
        Log.d("HandlerService", "following keys: $keys")

        var authorIcon = payload["icon"]?.jsonPrimitive?.contentOrNull
        val message = payload["message"]?.jsonObject?.let {
            RevoltJson.decodeFromJsonElement(
                Message.serializer(),
                it
            )
        } ?: run {
            Log.e("HandlerService", "No message in payload, abort")
            return
        }

        val user = payload["message"]?.jsonObject?.get("user")?.jsonObject?.let {
            RevoltJson.decodeFromJsonElement(
                User.serializer(),
                it
            )
        } ?: run {
            Log.e("HandlerService", "No message->user in payload, abort")
            return
        }

        if (authorIcon == null) {
            authorIcon =
                "$REVOLT_BASE/users/${message.author?.ifBlank { "0".repeat(26) }}/default_avatar"
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

        val remoteInput = RemoteInput.Builder("content").run {
            setLabel(getString(R.string.message_context_sheet_actions_reply))
            build()
        }

        val action: NotificationCompat.Action =
            NotificationCompat.Action.Builder(
                R.drawable.icn_reply_24dp,
                getString(R.string.message_context_sheet_actions_reply),
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_MUTABLE
                )
            )
                .addRemoteInput(remoteInput)
                .build()

        val builder = NotificationCompat.Builder(this, CHANNEL_ID_GROUP_SOCIAL_FRIENDREQUESTS)
            .setSmallIcon(R.drawable.icn_chat_24dp)
            .setContentTitle(user.displayName ?: user.username)
            .setContentText(message.content)
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
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

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