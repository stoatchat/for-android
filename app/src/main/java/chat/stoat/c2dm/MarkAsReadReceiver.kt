package chat.stoat.c2dm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import chat.stoat.api.StoatAPI
import chat.stoat.api.routes.channel.ackChannel
import chat.stoat.persistence.KVStorage
import kotlinx.coroutines.runBlocking
import logcat.LogPriority
import logcat.asLog
import logcat.logcat

class MarkAsReadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val channelId = intent.getStringExtra("channelId") ?: run {
            logcat(LogPriority.ERROR) { "No channel ID, aborting" }
            return
        }

        val messageId = intent.getStringExtra("messageId") ?: run {
            logcat(LogPriority.ERROR) { "No message ID, aborting" }
            return
        }

        runBlocking {
            val token = KVStorage(context).get("sessionToken") ?: run {
                logcat(LogPriority.ERROR) { "No session token, aborting" }
                return@runBlocking
            }
            StoatAPI.setSessionHeader(token)
            runCatching { ackChannel(channelId, messageId) }
                .onFailure { logcat(LogPriority.ERROR) { "Ack failed: ${it.asLog()}" } }
        }

        NotificationManagerCompat.from(context).cancel(channelId, NotificationID.NEW_MESSAGE)
    }
}
