package chat.stoat.internals

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import chat.stoat.activities.InviteActivity
import chat.stoat.callbacks.Action
import chat.stoat.callbacks.ActionChannel
import chat.stoat.composables.markdown.prose.ChatMarkdownActions
import chat.stoat.internals.extensions.isInviteUri

class AppChatMarkdownActions(private val context: Context) : ChatMarkdownActions {
    override suspend fun openUser(userId: String, serverId: String?) {
        ActionChannel.send(Action.OpenUserSheet(userId, serverId))
    }

    override suspend fun openChannel(channelId: String) {
        ActionChannel.send(Action.SwitchChannel(channelId))
    }

    override suspend fun openEmoteInfo(emoteId: String) {
        ActionChannel.send(Action.EmoteInfo(emoteId))
    }

    override suspend fun openInAppLink(uri: String): Boolean {
        val parsed = runCatching { uri.toUri() }.getOrNull() ?: return false
        parsed.toStoatWebLinkOrNull()?.let {
            ActionChannel.send(it.toNavigationAction())
            return true
        }
        if (parsed.isInviteUri()) {
            context.startActivity(
                Intent(context, InviteActivity::class.java)
                    .setData(parsed)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return true
        }
        return false
    }
}
