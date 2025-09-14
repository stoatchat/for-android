package chat.zekochat.sheets

import androidx.compose.runtime.Composable
import chat.zekochat.api.PeptideAPI
import chat.zekochat.composables.emoji.EmojiPicker

@Composable
fun ReactSheet(messageId: String, onSelect: (String?) -> Unit) {
    val message = PeptideAPI.messageCache[messageId]

    if (message == null) {
        onSelect(null)
        return
    }

    EmojiPicker {
        onSelect(it.removeSurrounding(":"))
    }

}